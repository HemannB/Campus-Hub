package br.com.uri.campushub.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import br.com.uri.campushub.repository.FavoriteDataSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class FavoriteViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var repository: FakeFavoriteDataSource
    private lateinit var viewModel: FavoriteViewModel

    @Before
    fun setUp() {
        repository = FakeFavoriteDataSource()
        viewModel = FavoriteViewModel(repository)
    }

    @Test
    fun `load status changes from loading to status`() {
        viewModel.loadStatus("event-1")

        assertSame(FavoriteState.Loading, viewModel.favoriteState.value)
        assertEquals("event-1", repository.statusEventId)

        repository.completeStatus(Result.success(true))

        assertEquals(FavoriteState.Status(true), viewModel.favoriteState.value)
    }

    @Test
    fun `load status failure exposes repository message`() {
        viewModel.loadStatus("event-1")

        repository.completeStatus(Result.failure(Exception("Consulta indisponível.")))

        assertEquals(
            FavoriteState.Error("Consulta indisponível.", null),
            viewModel.favoriteState.value
        )
    }

    @Test
    fun `toggle favorites and then removes favorite`() {
        loadStatus(false)

        viewModel.toggleFavorite("event-1")

        assertEquals(FavoriteState.Updating(false), viewModel.favoriteState.value)
        assertEquals("event-1", repository.favoritedEventId)
        repository.completeOperation(Result.success(Unit))
        assertEquals(
            FavoriteState.Success(true, "Evento adicionado aos favoritos."),
            viewModel.favoriteState.value
        )

        viewModel.toggleFavorite("event-1")

        assertEquals("event-1", repository.unfavoritedEventId)
        repository.completeOperation(Result.success(Unit))
        assertEquals(
            FavoriteState.Success(false, "Evento removido dos favoritos."),
            viewModel.favoriteState.value
        )
    }

    @Test
    fun `toggle failure preserves current status`() {
        loadStatus(false)

        viewModel.toggleFavorite("event-1")
        repository.completeOperation(Result.failure(Exception("Falha ao favoritar.")))

        assertEquals(
            FavoriteState.Error("Falha ao favoritar.", false),
            viewModel.favoriteState.value
        )
    }

    @Test
    fun `toggle before status load does nothing`() {
        viewModel.toggleFavorite("event-1")

        assertSame(FavoriteState.Idle, viewModel.favoriteState.value)
        assertEquals(null, repository.favoritedEventId)
        assertEquals(null, repository.unfavoritedEventId)
    }

    private fun loadStatus(isFavorite: Boolean) {
        viewModel.loadStatus("event-1")
        repository.completeStatus(Result.success(isFavorite))
    }

    private class FakeFavoriteDataSource : FavoriteDataSource {
        var statusEventId: String? = null
        var favoritedEventId: String? = null
        var unfavoritedEventId: String? = null
        private var statusResult: ((Result<Boolean>) -> Unit)? = null
        private var operationResult: ((Result<Unit>) -> Unit)? = null

        override fun getFavoriteStatus(
            eventId: String,
            onResult: (Result<Boolean>) -> Unit
        ) {
            statusEventId = eventId
            statusResult = onResult
        }

        override fun favorite(
            eventId: String,
            onResult: (Result<Unit>) -> Unit
        ) {
            favoritedEventId = eventId
            operationResult = onResult
        }

        override fun unfavorite(
            eventId: String,
            onResult: (Result<Unit>) -> Unit
        ) {
            unfavoritedEventId = eventId
            operationResult = onResult
        }

        fun completeStatus(result: Result<Boolean>) {
            checkNotNull(statusResult)(result)
        }

        fun completeOperation(result: Result<Unit>) {
            checkNotNull(operationResult)(result)
        }
    }
}
