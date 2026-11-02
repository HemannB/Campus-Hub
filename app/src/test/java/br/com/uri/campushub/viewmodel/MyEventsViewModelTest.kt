package br.com.uri.campushub.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import br.com.uri.campushub.model.Event
import br.com.uri.campushub.repository.RegisteredEventIdsDataSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class MyEventsViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val events = listOf(Event(id = "1"), Event(id = "2"))
    private lateinit var registrations: FakeRegisteredEventIdsDataSource
    private lateinit var eventRepository: FakeEventDataSource
    private lateinit var viewModel: MyEventsViewModel

    @Before
    fun setUp() {
        registrations = FakeRegisteredEventIdsDataSource()
        eventRepository = FakeEventDataSource()
        viewModel = MyEventsViewModel(registrations, eventRepository)
    }

    @Test
    fun `load events exposes registration failure`() {
        viewModel.loadEvents()

        assertSame(EventState.Loading, viewModel.eventState.value)
        registrations.complete(Result.failure(Exception("Inscrições indisponíveis.")))

        assertEquals(
            EventState.Error("Inscrições indisponíveis."),
            viewModel.eventState.value
        )
    }

    @Test
    fun `empty registrations return empty list without loading events`() {
        viewModel.loadEvents()

        registrations.complete(Result.success(emptySet()))

        assertEquals(EventState.Success(emptyList()), viewModel.eventState.value)
        assertEquals(0, eventRepository.getEventsCalls)
    }

    @Test
    fun `registered event ids filter loaded events`() {
        viewModel.loadEvents()
        registrations.complete(Result.success(setOf("2")))

        eventRepository.completeEvents(Result.success(events))

        assertEquals(
            EventState.Success(listOf(Event(id = "2"))),
            viewModel.eventState.value
        )
    }

    @Test
    fun `event loading failure is exposed`() {
        viewModel.loadEvents()
        registrations.complete(Result.success(setOf("1")))

        eventRepository.completeEvents(Result.failure(Exception("Eventos indisponíveis.")))

        assertEquals(
            EventState.Error("Eventos indisponíveis."),
            viewModel.eventState.value
        )
    }

    private class FakeRegisteredEventIdsDataSource : RegisteredEventIdsDataSource {
        private var result: ((Result<Set<String>>) -> Unit)? = null

        override fun getRegisteredEventIds(onResult: (Result<Set<String>>) -> Unit) {
            result = onResult
        }

        fun complete(value: Result<Set<String>>) {
            checkNotNull(result)(value)
        }
    }
}
