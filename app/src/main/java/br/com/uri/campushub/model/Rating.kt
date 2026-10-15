package br.com.uri.campushub.model

import com.google.firebase.Timestamp

data class Rating(
    val id: String = "",
    val eventId: String = "",
    val userId: String = "",
    val score: Int = 0,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

data class RatingSummary(
    val average: Double = 0.0,
    val count: Int = 0,
    val currentUserScore: Int? = null
)
