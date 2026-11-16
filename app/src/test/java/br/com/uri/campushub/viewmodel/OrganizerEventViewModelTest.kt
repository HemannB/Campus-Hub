package br.com.uri.campushub.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import br.com.uri.campushub.model.*
import br.com.uri.campushub.repository.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class OrganizerEventViewModelTest {
    @get:Rule val rule = InstantTaskExecutorRule()

    @Test fun foreignEventDoesNotLoadAttendees() {
        val events = FakeEditor()
        events.event = events.event.copy(organizerId = "other")
        val attendees = FakeAttendance()
        val vm = OrganizerEventViewModel(FakeCurrentUserDataSource(), events, attendees)
        vm.load("event")
        assertEquals(OrganizerEventState.Error("Este evento não pertence a você."), vm.state.value)
        assertEquals(0, attendees.loads)
    }

    @Test fun loadsAttendeesAndReloadsAfterPublishing() {
        val events = FakeEditor()
        val attendees = FakeAttendance()
        val vm = OrganizerEventViewModel(FakeCurrentUserDataSource(), events, attendees)
        vm.load("event")
        assertEquals(OrganizerEventState.Ready(events.event, attendees.items), vm.state.value)
        vm.changeStatus("event", EventStatus.PUBLISHED)
        assertEquals(EventStatus.PUBLISHED, events.event.eventStatus)
        assertEquals("Evento atualizado.", vm.message.value)
        assertEquals(2, attendees.loads)
        vm.clearMessage()
        assertNull(vm.message.value)
    }

    @Test fun checkInFailureIsReportedWithoutLosingTheList() {
        val events = FakeEditor()
        val attendees = FakeAttendance()
        attendees.checkInResult = Result.failure(Exception("Presença já registrada."))
        val vm = OrganizerEventViewModel(FakeCurrentUserDataSource(), events, attendees)
        vm.load("event")
        vm.checkIn("event", "ticket")
        assertEquals("ticket", attendees.scannedTicket)
        assertEquals("Presença já registrada.", vm.message.value)
        assertEquals(OrganizerEventState.Ready(events.event, attendees.items), vm.state.value)
    }

    private class FakeEditor : EventEditorDataSource {
        var event = Event(id = "event", organizerId = "user-1", status = "DRAFT")
        override fun getEventById(eventId: String, onResult: (Result<Event>) -> Unit) = onResult(Result.success(event))
        override fun createEvent(event: Event, onResult: (Result<Unit>) -> Unit) = Unit
        override fun updateEvent(event: Event, onResult: (Result<Unit>) -> Unit) = Unit
        override fun changeStatus(eventId: String, status: EventStatus, onResult: (Result<Unit>) -> Unit) {
            event = event.copy(status = status.name)
            onResult(Result.success(Unit))
        }
    }

    private class FakeAttendance : AttendanceDataSource {
        var loads = 0
        var scannedTicket: String? = null
        var checkInResult = Result.success(Unit)
        val items = listOf(Registration(userName = "Aluno", status = "CONFIRMED"))
        override fun getAttendees(eventId: String, onResult: (Result<List<Registration>>) -> Unit) {
            loads++
            onResult(Result.success(items))
        }
        override fun checkIn(eventId: String, ticket: String, onResult: (Result<Unit>) -> Unit) {
            scannedTicket = ticket
            onResult(checkInResult)
        }
    }
}
