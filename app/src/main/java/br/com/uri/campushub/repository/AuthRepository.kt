package br.com.uri.campushub.repository

import com.google.firebase.auth.FirebaseAuth

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    fun isLoggedIn(): Boolean {
        return auth.currentUser != null
    }

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

    fun logout() {
        auth.signOut()
    }

    fun register(
        email: String,
        password: String,
        onResult: (Result<String>) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = task.result?.user?.uid

                    if (userId != null) {
                        onResult(Result.success(userId))
                    } else {
                        onResult(
                            Result.failure(
                                Exception("Não foi possível identificar o usuário criado.")
                            )
                        )
                    }
                } else {
                    val exception = task.exception
                        ?: Exception("Não foi possível criar a conta.")
                    onResult(Result.failure(exception))
                }
            }
    }

    fun rollbackRegistration(onComplete: () -> Unit) {
        val currentUser = auth.currentUser

        if (currentUser == null) {
            onComplete()
            return
        }

        currentUser.delete().addOnCompleteListener {
            auth.signOut()
            onComplete()
        }
    }
}
