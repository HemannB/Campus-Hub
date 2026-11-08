package br.com.uri.campushub.model

enum class EventStatus {
    DRAFT,
    PUBLISHED,
    ENDED;

    companion object {
        fun from(value: String?): EventStatus {
            return entries.firstOrNull { status ->
                status.name.equals(value?.trim(), ignoreCase = true)
            } ?: PUBLISHED
        }
    }
}
