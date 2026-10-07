package br.com.uri.campushub.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class FavoriteRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
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
}