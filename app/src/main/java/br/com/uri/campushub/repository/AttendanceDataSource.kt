package br.com.uri.campushub.repository

import br.com.uri.campushub.model.Registration

interface AttendanceDataSource {
    fun getAttendees(eventId: String, onResult: (Result<List<Registration>>) -> Unit)
    fun checkIn(eventId: String, ticket: String, onResult: (Result<Unit>) -> Unit)
}
