package br.com.uri.campushub.model

enum class RegistrationStatus {
    CONFIRMED,
    WAITLISTED,
    INVITED;

    companion object {
        fun from(value: String?): RegistrationStatus {
            return entries.firstOrNull { status ->
                status.name.equals(value?.trim(), ignoreCase = true)
            } ?: CONFIRMED
        }
    }
}
