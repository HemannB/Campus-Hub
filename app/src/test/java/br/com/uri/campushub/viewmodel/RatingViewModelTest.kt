package br.com.uri.campushub.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import br.com.uri.campushub.model.RatingSummary
import br.com.uri.campushub.repository.RatingDataSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class RatingViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var repository: FakeRatingDataSource
    private lateinit var viewModel: RatingViewModel

    @Before
    fun setUp() {
        repository = FakeRatingDataSource()
        viewModel = RatingViewModel(repository)
    }

    @Test
    fun `load summary changes from loading to success`() {
        val summary = RatingSummary(average = 4.5, count = 2, currentUserScore = 5)

        viewModel.loadRatingSummary("event-1")

        assertSame(RatingSummaryState.Loading, viewModel.summaryState.value)
        assertEquals("event-1", repository.summaryEventId)

        repository.completeSummary(Result.success(summary))

        assertEquals(RatingSummaryState.Success(summary), viewModel.summaryState.value)
    }

    @Test
    fun `load summary failure exposes repository message`() {
        viewModel.loadRatingSummary("event-1")

        repository.completeSummary(Result.failure(Exception("Avaliações indisponíveis.")))

        assertEquals(
            RatingSummaryState.Error("Avaliações indisponíveis."),
            viewModel.summaryState.value
        )
    }

    @Test
    fun `invalid score is rejected without repository call`() {
        viewModel.saveRating("event-1", 0)
        assertEquals(
            RatingActionState.Error("Escolha uma nota de 1 a 5."),
            viewModel.actionState.value
        )

        viewModel.saveRating("event-1", 6)

        assertEquals(
            RatingActionState.Error("Escolha uma nota de 1 a 5."),
            viewModel.actionState.value
        )
        assertEquals(0, repository.saveCalls)
    }

    @Test
    fun `successful save identifies new and updated rating`() {
        viewModel.saveRating("event-1", 4)
        repository.completeSave(Result.success(Unit))

        assertEquals(
            RatingActionState.Success("Avaliação registrada."),
            viewModel.actionState.value
        )
        assertEquals("event-1", repository.summaryEventId)

        val existingRepository = FakeRatingDataSource()
        val existingViewModel = RatingViewModel(existingRepository)
        existingViewModel.loadRatingSummary("event-2")
        existingRepository.completeSummary(
            Result.success(RatingSummary(currentUserScore = 3))
        )

        existingViewModel.saveRating("event-2", 5)
        existingRepository.completeSave(Result.success(Unit))

        assertEquals(
            RatingActionState.Success("Avaliação atualizada."),
            existingViewModel.actionState.value
        )
    }

    @Test
    fun `failed save exposes repository message`() {
        viewModel.saveRating("event-1", 3)

        assertSame(RatingActionState.Loading, viewModel.actionState.value)

        repository.completeSave(Result.failure(Exception("Falha ao salvar.")))

        val state = viewModel.actionState.value as RatingActionState.Error
        assertEquals("Falha ao salvar.", state.message)
        assertEquals(1, repository.saveCalls)

        viewModel.clearActionState()

        assertSame(RatingActionState.Idle, viewModel.actionState.value)
    }

    private class FakeRatingDataSource : RatingDataSource {
        var summaryEventId: String? = null
        var saveCalls = 0
        private var summaryResult: ((Result<RatingSummary>) -> Unit)? = null
        private var saveResult: ((Result<Unit>) -> Unit)? = null

        override fun getRatingSummary(
            eventId: String,
            onResult: (Result<RatingSummary>) -> Unit
        ) {
            summaryEventId = eventId
            summaryResult = onResult
        }

        override fun saveRating(
            eventId: String,
            score: Int,
            onResult: (Result<Unit>) -> Unit
        ) {
            saveCalls++
            saveResult = onResult
        }

        fun completeSummary(result: Result<RatingSummary>) {
            checkNotNull(summaryResult)(result)
        }

        fun completeSave(result: Result<Unit>) {
            checkNotNull(saveResult)(result)
        }
    }
}
