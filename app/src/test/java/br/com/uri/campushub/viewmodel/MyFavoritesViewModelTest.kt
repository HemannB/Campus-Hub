package br.com.uri.campushub.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import br.com.uri.campushub.model.Event
import br.com.uri.campushub.repository.FavoriteEventIdsDataSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class MyFavoritesViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val events = listOf(Event(id = "1"), Event(id = "2"))
    private lateinit var favorites: FakeFavoriteEventIdsDataSource
    private lateinit var eventRepository: FakeEventDataSource
    private lateinit var viewModel: MyFavoritesViewModel

    @Before
    fun setUp() {
        favorites = FakeFavoriteEventIdsDataSource()
        eventRepository = FakeEventDataSource()
        viewModel = MyFavoritesViewModel(favorites, eventRepository)
    }

    @Test
    fun `load events exposes favorites failure`() {
        viewModel.loadEvents()

        assertSame(EventState.Loading, viewModel.eventState.value)
        favorites.complete(Result.failure(Exception("Favoritos indisponíveis.")))

        assertEquals(
            EventState.Error("Favoritos indisponíveis."),
            viewModel.eventState.value
        )
    }

    @Test
    fun `empty favorites return empty list without loading events`() {
        viewModel.loadEvents()

        favorites.complete(Result.success(emptySet()))

        assertEquals(EventState.Success(emptyList()), viewModel.eventState.value)
        assertEquals(0, eventRepository.getEventsCalls)
    }

    @Test
    fun `favorite event ids filter loaded events`() {
        viewModel.loadEvents()
        favorites.complete(Result.success(setOf("2")))

        eventRepository.completeEvents(Result.success(events))

        assertEquals(
            EventState.Success(listOf(Event(id = "2"))),
            viewModel.eventState.value
        )
    }

    @Test
    fun `event loading failure is exposed`() {
        viewModel.loadEvents()
        favorites.complete(Result.success(setOf("1")))

        eventRepository.completeEvents(Result.failure(Exception("Eventos indisponíveis.")))

        assertEquals(
            EventState.Error("Eventos indisponíveis."),
            viewModel.eventState.value
        )
    }

    private class FakeFavoriteEventIdsDataSource : FavoriteEventIdsDataSource {
        private var result: ((Result<Set<String>>) -> Unit)? = null

        override fun getFavoriteEventIds(onResult: (Result<Set<String>>) -> Unit) {
            result = onResult
        }

        fun complete(value: Result<Set<String>>) {
            checkNotNull(result)(value)
        }
    }
}
