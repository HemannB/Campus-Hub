package br.com.uri.campushub.model

import com.google.firebase.Timestamp

data class Comment(
    val id: String = "",
    val eventId: String = "",
    val userId: String = "",
    val authorName: String = "",
    val text: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)
