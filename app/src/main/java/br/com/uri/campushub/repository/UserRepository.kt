package br.com.uri.campushub.repository

import br.com.uri.campushub.model.User
import com.google.firebase.firestore.FirebaseFirestore

class UserRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    fun createProfile(
        user: User,
        onResult: (Result<Unit>) -> Unit
    ) {
        firestore.collection("users")
            .document(user.id)
            .set(user)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onResult(Result.success(Unit))
                } else {
                    val exception = task.exception
                        ?: Exception("Não foi possível salvar o perfil.")
                    onResult(Result.failure(exception))
                }
            }
    }
}
