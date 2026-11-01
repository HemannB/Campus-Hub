package br.com.uri.campushub.repository

import br.com.uri.campushub.model.Event

interface EventDataSource {
    fun getEventById(
        eventId: String,
        onResult: (Result<Event>) -> Unit
    )

    fun getEvents(
        onResult: (Result<List<Event>>) -> Unit
    )
}
