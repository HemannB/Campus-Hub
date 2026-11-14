package br.com.uri.campushub.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import br.com.uri.campushub.model.Event
import br.com.uri.campushub.model.EventStatus
import br.com.uri.campushub.repository.OrganizerEventDataSource
import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class OrganizerEventsViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var authRepository: FakeCurrentUserDataSource
    private lateinit var eventRepository: FakeOrganizerEventDataSource
    private lateinit var viewModel: OrganizerEventsViewModel

    @Before
    fun setUp() {
        authRepository = FakeCurrentUserDataSource()
        eventRepository = FakeOrganizerEventDataSource()
        viewModel = OrganizerEventsViewModel(authRepository, eventRepository)
    }

    @Test
    fun `missing session does not query events`() {
        authRepository.userId = null

        viewModel.loadEvents()

        assertEquals(EventState.Error("Usuário não autenticado."), viewModel.eventState.value)
        assertNull(eventRepository.requestedOrganizerId)
    }

    @Test
    fun `loads current organizer events ordered by date including drafts`() {
        val draft = Event(id = "draft", status = EventStatus.DRAFT.name)
        val later = Event(id = "later", startAt = Timestamp(200, 0))
        val earlier = Event(id = "earlier", startAt = Timestamp(100, 0))

        viewModel.loadEvents()

        assertSame(EventState.Loading, viewModel.eventState.value)
        assertEquals("user-1", eventRepository.requestedOrganizerId)

        eventRepository.complete(Result.success(listOf(draft, later, earlier)))

        assertEquals(
            EventState.Success(listOf(earlier, later, draft)),
            viewModel.eventState.value
        )
    }

    @Test
    fun `organizer without events receives empty list`() {
        viewModel.loadEvents()
        eventRepository.complete(Result.success(emptyList()))

        assertEquals(EventState.Success(emptyList()), viewModel.eventState.value)
    }

    @Test
    fun `query failure is shown and can be retried`() {
        viewModel.loadEvents()
        eventRepository.complete(Result.failure(Exception("Consulta indisponível.")))

        assertEquals(EventState.Error("Consulta indisponível."), viewModel.eventState.value)

        viewModel.loadEvents()
        assertSame(EventState.Loading, viewModel.eventState.value)
        eventRepository.complete(Result.success(emptyList()))
        assertEquals(EventState.Success(emptyList()), viewModel.eventState.value)
    }

    private class FakeOrganizerEventDataSource : OrganizerEventDataSource {
        var requestedOrganizerId: String? = null
        private var callback: ((Result<List<Event>>) -> Unit)? = null

        override fun getOrganizerEvents(
            organizerId: String,
            onResult: (Result<List<Event>>) -> Unit
        ) {
            requestedOrganizerId = organizerId
            callback = onResult
        }

        fun complete(result: Result<List<Event>>) {
            checkNotNull(callback)(result)
        }
    }
}
