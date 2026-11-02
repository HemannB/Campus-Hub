package br.com.uri.campushub.repository

interface AccountRegistrationDataSource {
    fun register(
        email: String,
        password: String,
        onResult: (Result<String>) -> Unit
    )

    fun rollbackRegistration(onComplete: () -> Unit)
}
