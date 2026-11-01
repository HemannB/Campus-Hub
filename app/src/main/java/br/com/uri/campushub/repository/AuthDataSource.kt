package br.com.uri.campushub.repository

interface AuthDataSource {
    fun isLoggedIn(): Boolean

    fun login(
        email: String,
        password: String,
        onResult: (Result<Unit>) -> Unit
    )

    fun logout()
}
