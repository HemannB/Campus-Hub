package br.com.uri.campushub.viewmodel

import br.com.uri.campushub.model.Event
import br.com.uri.campushub.repository.EventDataSource

class FakeEventDataSource : EventDataSource {
    var requestedEventId: String? = null
    var getEventsCalls = 0
    private var eventResult: ((Result<Event>) -> Unit)? = null
    private var eventsResult: ((Result<List<Event>>) -> Unit)? = null

    override fun getEventById(
        eventId: String,
        onResult: (Result<Event>) -> Unit
    ) {
        requestedEventId = eventId
        eventResult = onResult
    }

    override fun getEvents(onResult: (Result<List<Event>>) -> Unit) {
        getEventsCalls++
        eventsResult = onResult
    }

    fun completeEvent(result: Result<Event>) {
        checkNotNull(eventResult)(result)
    }

    fun completeEvents(result: Result<List<Event>>) {
        checkNotNull(eventsResult)(result)
    }
}
