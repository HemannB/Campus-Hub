package br.com.uri.campushub.model

import com.google.firebase.Timestamp

data class Registration(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val eventId: String = "",
    val status: String = RegistrationStatus.CONFIRMED.name,
    val registeredAt: Timestamp? = null,
    val qrToken: String = "",
    val checkedInAt: Timestamp? = null
)

val Registration.registrationStatus: RegistrationStatus
    get() = RegistrationStatus.from(status)
