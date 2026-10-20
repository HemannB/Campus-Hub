package br.com.uri.campushub.model

enum class PublicationType {
    PHOTO,
    AUDIO,
    VIDEO
}

data class EventPublication(
    val id: String = "",
    val eventId: String = "",
    val userId: String = "",
    val authorName: String = "",
    val type: PublicationType = PublicationType.PHOTO,
    val localPath: String = "",
    val createdAt: Long = 0L
)
