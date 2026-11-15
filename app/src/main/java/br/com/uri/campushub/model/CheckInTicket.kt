package br.com.uri.campushub.model

data class CheckInTicket(val eventId: String, val registrationId: String, val token: String) {
    fun encode() = "CampusHub|$eventId|$registrationId|$token"

    companion object {
        fun parse(value: String): CheckInTicket? {
            val parts = value.split('|')
            if (parts.size != 4 || parts[0] != "CampusHub" ||
                parts.drop(1).any { it.isBlank() || '/' in it }) return null
            return CheckInTicket(parts[1], parts[2], parts[3])
        }
    }
}
