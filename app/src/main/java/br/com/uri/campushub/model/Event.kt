package br.com.uri.campushub.model

import com.google.firebase.Timestamp

data class Event(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "",
    val startAt: Timestamp? = null,
    val endAt: Timestamp? = null,
    val location: String = "",
    val imageUrl: String = "",
    val maxParticipants: Int = 0,
    val participantCount: Int = 0,
    val organizer: String = ""
)
