package br.com.uri.campushub.repository

interface PasswordResetDataSource {
    fun resetPassword(
        email: String,
        onResult: (Result<Unit>) -> Unit
    )
}
