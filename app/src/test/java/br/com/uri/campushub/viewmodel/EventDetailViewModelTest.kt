package br.com.uri.campushub.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import br.com.uri.campushub.model.Event
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class EventDetailViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var repository: FakeEventDataSource
    private lateinit var viewModel: EventDetailViewModel

    @Before
    fun setUp() {
        repository = FakeEventDataSource()
        viewModel = EventDetailViewModel(repository)
    }

    @Test
    fun `load event changes from loading to success`() {
        val event = Event(id = "event-1", title = "Semana Acadêmica")

        viewModel.loadEvent("event-1")

        assertSame(EventDetailState.Loading, viewModel.eventDetailState.value)
        assertEquals("event-1", repository.requestedEventId)

        repository.completeEvent(Result.success(event))

        assertEquals(EventDetailState.Success(event), viewModel.eventDetailState.value)
    }

    @Test
    fun `load event failure exposes repository message`() {
        viewModel.loadEvent("event-1")

        repository.completeEvent(Result.failure(Exception("Evento indisponível.")))

        assertEquals(
            EventDetailState.Error("Evento indisponível."),
            viewModel.eventDetailState.value
        )
    }
}
