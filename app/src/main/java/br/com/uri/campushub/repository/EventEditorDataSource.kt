package br.com.uri.campushub.repository

import br.com.uri.campushub.model.Event
import br.com.uri.campushub.model.EventStatus

interface EventEditorDataSource {
    fun createEvent(event: Event, onResult: (Result<Unit>) -> Unit)
    fun getEventById(eventId: String, onResult: (Result<Event>) -> Unit)
    fun updateEvent(event: Event, onResult: (Result<Unit>) -> Unit)
    fun changeStatus(eventId: String, status: EventStatus, onResult: (Result<Unit>) -> Unit)
}
