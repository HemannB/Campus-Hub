package br.com.uri.campushub.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue

class FavoriteRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    fun getFavoriteEventIds(
        onResult: (Result<Set<String>>) -> Unit
    ) {
        val userId = auth.currentUser?.uid

        if (userId == null) {
            onResult(Result.failure(Exception("Usuário não autenticado.")))
            return
        }

        firestore.collection("favorites")
            .whereEqualTo("userId", userId)
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val eventIds = task.result
                        ?.documents
                        ?.mapNotNull { document ->
                            document.getString("eventId")
                        }
                        ?.toSet()
                        .orEmpty()

                    onResult(Result.success(eventIds))
                } else {
                    val exception = task.exception
                        ?: Exception("Não foi possível carregar seus favoritos.")
                    onResult(Result.failure(exception))
                }
            }
    }

    fun getFavoriteStatus(
        eventId: String,
        onResult: (Result<Boolean>) -> Unit
    ) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            onResult(Result.failure(Exception("Usuário não autenticado!")))
            return
        }

        firestore.collection("favorites")
            .document("${userId}_$eventId")
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val isFavorite = task.result?.exists() == true
                    onResult(Result.success(isFavorite))
                } else {
                    val exception = task.exception
                        ?: Exception("Não foi possivel consultar o favorito.")
                    onResult(Result.failure((exception)))
                }
            }
    }

    fun favorite(
        eventId: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        val userId = auth.currentUser?.uid

        if (userId == null) {
            onResult(Result.failure(Exception("Usuário não atenticado")))
            return
        }

        val favorite = hashMapOf(
            "userId" to userId,
            "eventId" to eventId,
            "createdAt" to FieldValue.serverTimestamp()
        )

        firestore.collection("favorites")
            .document("${userId}_$eventId")
            .set(favorite)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onResult(Result.success(Unit))
                } else {
                    val exception = task.exception
                        ?: Exception("Não foi possivel favoritar o evento...")
                    onResult(Result.failure((exception)))
                }
            }
    }

    fun unfavorite(
        eventId: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        val userId = auth.currentUser?.uid

        if (userId == null) {
            onResult(Result.failure(Exception("Usuário não autenticado.")))
            return
        }

        firestore.collection("favorites")
            .document("${userId}_$eventId")
            .delete()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onResult(Result.success(Unit))
                } else {
                    val exception = task.exception
                        ?: Exception("Não foi possível remover o favorito.")

                    onResult(Result.failure(exception))
                }
            }
    }
}
