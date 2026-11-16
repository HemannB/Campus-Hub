package br.com.uri.campushub.repository

import br.com.uri.campushub.model.Registration

interface EnrollmentDataSource {
    fun getRegistration(eventId: String, onResult: (Result<Registration?>) -> Unit)
    fun register(eventId: String, onResult: (Result<Unit>) -> Unit)
    fun cancel(eventId: String, onResult: (Result<Unit>) -> Unit)
    fun confirmInvitation(eventId: String, onResult: (Result<Unit>) -> Unit)
}
