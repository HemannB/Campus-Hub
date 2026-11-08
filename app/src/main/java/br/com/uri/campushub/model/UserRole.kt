package br.com.uri.campushub.model

enum class UserRole {
    STUDENT,
    ORGANIZER;

    companion object {
        fun from(value: String?): UserRole {
            return entries.firstOrNull { role ->
                role.name.equals(value?.trim(), ignoreCase = true)
            } ?: STUDENT
        }
    }
}
