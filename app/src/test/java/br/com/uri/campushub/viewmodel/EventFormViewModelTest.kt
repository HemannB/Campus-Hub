package br.com.uri.campushub.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import br.com.uri.campushub.model.Event
import br.com.uri.campushub.model.User
import br.com.uri.campushub.repository.EventEditorDataSource
import br.com.uri.campushub.model.EventStatus
import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class EventFormViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var auth: FakeCurrentUserDataSource
    private lateinit var users: FakeUserDataSource
    private lateinit var events: FakeEventCreationDataSource
    private lateinit var viewModel: EventFormViewModel
    private val organizer = User(name = "Bruno", role = "ORGANIZER")
    private val event = Event(
        title = " Oficina ", description = "Kotlin", category = "Tecnologia",
        location = "Rua A, 123", startAt = Timestamp(100, 0),
        endAt = Timestamp(200, 0), maxParticipants = 20
    )

    @Before
    fun setUp() {
        auth = FakeCurrentUserDataSource()
        users = FakeUserDataSource()
        events = FakeEventCreationDataSource()
        viewModel = EventFormViewModel(auth, users, events)
    }

    @Test
    fun editsLoadedEventKeepingIdAndPublishedState() {
        events.loadedEvent = event.copy(id = "event-1", organizerId = "user-1", status = "PUBLISHED")
        viewModel.loadEvent("event-1")
        assertEquals(EventFormState.Ready(events.loadedEvent!!), viewModel.state.value)
        viewModel.saveEvent(event.copy(title = "Novo título"))
        users.completeProfile(Result.success(organizer))
        assertEquals("event-1", events.savedEvent?.id)
        assertEquals("PUBLISHED", events.savedEvent?.status)
        assertEquals("Novo título", events.savedEvent?.title)
    }

    @Test
    fun rejectsForeignEventWithoutCreatingAnotherOne() {
        events.loadedEvent = event.copy(id = "foreign", organizerId = "other")
        viewModel.loadEvent("foreign")
        assertEquals(EventFormState.Error("Você não pode editar este evento."), viewModel.state.value)
        viewModel.saveEvent(event)
        assertEquals(EventFormState.Error("Carregue o evento antes de salvar."), viewModel.state.value)
        assertNull(events.savedEvent)
    }

    @Test
    fun `missing session or invalid fields prevent saving`() {
        auth.userId = null
        viewModel.saveEvent(event)
        assertEquals(EventFormState.Error("Usuário não autenticado."), viewModel.state.value)

        auth.userId = "user-1"
        viewModel.saveEvent(event.copy(title = ""))
        assertEquals(EventFormState.Error("Informe o título."), viewModel.state.value)
        assertNull(users.requestedUserId)
        assertNull(events.savedEvent)
    }

    @Test
    fun `student cannot create event`() {
        viewModel.saveEvent(event)
        users.completeProfile(Result.success(User()))

        assertEquals(
            EventFormState.Error("Somente organizadores podem criar eventos."),
            viewModel.state.value
        )
        assertNull(events.savedEvent)
    }

    @Test
    fun `organizer saves own draft without duplicating pending requests`() {
        viewModel.saveEvent(event.copy(id = "old", organizerId = "other", status = "PUBLISHED", participantCount = 5))
        assertSame(EventFormState.Saving, viewModel.state.value)
        assertEquals("user-1", users.requestedUserId)
        users.completeProfile(Result.success(organizer))

        assertEquals(event.copy(title = "Oficina", organizerId = "user-1", organizer = "Bruno", status = "DRAFT"), events.savedEvent)
        viewModel.saveEvent(event)
        assertEquals(1, events.calls)

        events.complete(Result.success(Unit))
        assertSame(EventFormState.Success, viewModel.state.value)
        viewModel.saveEvent(event)
        assertEquals(1, events.calls)
    }

    @Test
    fun `profile and persistence errors allow retry`() {
        viewModel.saveEvent(event)
        users.completeProfile(Result.failure(Exception("Falha no perfil.")))
        assertEquals(EventFormState.Error("Falha no perfil."), viewModel.state.value)

        viewModel.saveEvent(event)
        users.completeProfile(Result.success(organizer))
        events.complete(Result.failure(Exception("Falha ao salvar.")))
        assertEquals(EventFormState.Error("Falha ao salvar."), viewModel.state.value)

        viewModel.saveEvent(event)
        users.completeProfile(Result.success(organizer))
        events.complete(Result.success(Unit))
        assertSame(EventFormState.Success, viewModel.state.value)
    }

    private class FakeEventCreationDataSource : EventEditorDataSource {
        var loadedEvent: Event? = null
        override fun getEventById(eventId: String, onResult: (Result<Event>) -> Unit) {
            onResult(Result.success(checkNotNull(loadedEvent)))
        }
        override fun updateEvent(event: Event, onResult: (Result<Unit>) -> Unit) = createEvent(event, onResult)
        override fun changeStatus(eventId: String, status: EventStatus, onResult: (Result<Unit>) -> Unit) = Unit
        var savedEvent: Event? = null
        var calls = 0
        private var callback: ((Result<Unit>) -> Unit)? = null

        override fun createEvent(event: Event, onResult: (Result<Unit>) -> Unit) {
            savedEvent = event
            calls++
            callback = onResult
        }

        fun complete(result: Result<Unit>) = checkNotNull(callback)(result)
    }
}
