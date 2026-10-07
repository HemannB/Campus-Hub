package br.com.uri.campushub.repository

import br.com.uri.campushub.model.Comment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class CommentRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    fun getComments(
        eventId: String,
        onResult: (Result<List<Comment>>) -> Unit
    ) {
        firestore.collection("comments")
            .whereEqualTo("eventId", eventId)
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val comments = task.result
                        ?.documents
                        ?.mapNotNull { document ->
                            document.toObject(Comment::class.java)
                                ?.copy(id = document.id)
                        }
                        ?.sortedByDescending { comment ->
                            comment.createdAt?.seconds ?: Long.MIN_VALUE
                        }
                        .orEmpty()

                    onResult(Result.success(comments))
                } else {
                    val exception = task.exception
                        ?: Exception("Não foi possível carregar os comentários.")
                    onResult(Result.failure(exception))
                }
            }
    }

    fun createComment(
        eventId: String,
        authorName: String,
        text: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        val userId = auth.currentUser?.uid

        if (userId == null) {
            onResult(Result.failure(Exception("Usuário não autenticado.")))
            return
        }

        val comment = hashMapOf(
            "eventId" to eventId,
            "userId" to userId,
            "authorName" to authorName,
            "text" to text,
            "createdAt" to FieldValue.serverTimestamp()
        )

        firestore.collection("comments")
            .add(comment)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onResult(Result.success(Unit))
                } else {
                    val exception = task.exception
                        ?: Exception("Não foi possível publicar o comentário.")
                    onResult(Result.failure(exception))
                }
            }
    }
}
