package br.com.uri.campushub.repository

import com.google.firebase.auth.FirebaseAuth

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    fun login(
        email: String,
        password: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onResult(Result.success(Unit))
                } else {
                    val exception = task.exception ?: Exception("Não foi possivel fazer login...")
                    onResult(Result.failure(exception))
                }
            }
    }
}