package br.com.uri.campushub.repository

import br.com.uri.campushub.model.Event

interface OrganizerEventDataSource {
    fun getOrganizerEvents(
        organizerId: String,
        onResult: (Result<List<Event>>) -> Unit
    )
}
