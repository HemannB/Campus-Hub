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

    fun updateComment(
        commentId: String,
        text: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        val userId = auth.currentUser?.uid

        if (userId == null) {
            onResult(Result.failure(Exception("Usuário não autenticado.")))
            return
        }

        val commentDocument = firestore.collection("comments").document(commentId)

        commentDocument.get().addOnCompleteListener { lookupTask ->
            if (!lookupTask.isSuccessful) {
                val exception = lookupTask.exception
                    ?: Exception("Não foi possível consultar o comentário.")
                onResult(Result.failure(exception))
                return@addOnCompleteListener
            }

            val snapshot = lookupTask.result

            if (snapshot == null || !snapshot.exists()) {
                onResult(Result.failure(Exception("Comentário não encontrado.")))
                return@addOnCompleteListener
            }

            if (snapshot.getString("userId") != userId) {
                onResult(Result.failure(Exception("Você não pode editar este comentário.")))
                return@addOnCompleteListener
            }

            commentDocument.update(
                mapOf(
                    "text" to text,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).addOnCompleteListener { updateTask ->
                if (updateTask.isSuccessful) {
                    onResult(Result.success(Unit))
                } else {
                    val exception = updateTask.exception
                        ?: Exception("Não foi possível editar o comentário.")
                    onResult(Result.failure(exception))
                }
            }
        }
    }

    fun deleteComment(
        commentId: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        val userId = auth.currentUser?.uid

        if (userId == null) {
            onResult(Result.failure(Exception("Usuário não autenticado.")))
            return
        }

        val commentDocument = firestore.collection("comments").document(commentId)

        commentDocument.get().addOnCompleteListener { lookupTask ->
            if (!lookupTask.isSuccessful) {
                val exception = lookupTask.exception
                    ?: Exception("Não foi possível consultar o comentário.")
                onResult(Result.failure(exception))
                return@addOnCompleteListener
            }

            val snapshot = lookupTask.result

            if (snapshot == null || !snapshot.exists()) {
                onResult(Result.failure(Exception("Comentário não encontrado.")))
                return@addOnCompleteListener
            }

            if (snapshot.getString("userId") != userId) {
                onResult(Result.failure(Exception("Você não pode excluir este comentário.")))
                return@addOnCompleteListener
            }

            commentDocument.delete().addOnCompleteListener { deleteTask ->
                if (deleteTask.isSuccessful) {
                    onResult(Result.success(Unit))
                } else {
                    val exception = deleteTask.exception
                        ?: Exception("Não foi possível excluir o comentário.")
                    onResult(Result.failure(exception))
                }
            }
        }
    }
}
