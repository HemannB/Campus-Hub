package br.com.uri.campushub.repository

interface RegistrationDataSource {
    fun getRegistrationStatus(
        eventId: String,
        onResult: (Result<Boolean>) -> Unit
    )

    fun register(
        eventId: String,
        onResult: (Result<Unit>) -> Unit
    )

    fun cancel(
        eventId: String,
        onResult: (Result<Unit>) -> Unit
    )
}
