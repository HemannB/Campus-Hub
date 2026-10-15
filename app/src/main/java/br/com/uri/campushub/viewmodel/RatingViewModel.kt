package br.com.uri.campushub.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import br.com.uri.campushub.model.RatingSummary
import br.com.uri.campushub.repository.RatingRepository

sealed class RatingSummaryState {
    object Idle : RatingSummaryState()
    object Loading : RatingSummaryState()
    data class Success(val summary: RatingSummary) : RatingSummaryState()
    data class Error(val message: String) : RatingSummaryState()
}

sealed class RatingActionState {
    object Idle : RatingActionState()
    object Loading : RatingActionState()
    data class Success(val message: String) : RatingActionState()
    data class Error(val message: String) : RatingActionState()
}

class RatingViewModel : ViewModel() {

    private val repository = RatingRepository()
    private var currentSummary = RatingSummary()

    private val _summaryState =
        MutableLiveData<RatingSummaryState>(RatingSummaryState.Idle)
    val summaryState: LiveData<RatingSummaryState> = _summaryState

    private val _actionState =
        MutableLiveData<RatingActionState>(RatingActionState.Idle)
    val actionState: LiveData<RatingActionState> = _actionState

    fun loadRatingSummary(eventId: String) {
        _summaryState.value = RatingSummaryState.Loading

        repository.getRatingSummary(eventId) { result ->
            _summaryState.value = result.fold(
                onSuccess = { summary ->
                    currentSummary = summary
                    RatingSummaryState.Success(summary)
                },
                onFailure = { exception ->
                    RatingSummaryState.Error(
                        exception.toUserMessage(
                            "Não foi possível carregar as avaliações."
                        )
                    )
                }
            )
        }
    }

    fun saveRating(eventId: String, score: Int) {
        if (score !in 1..5) {
            _actionState.value = RatingActionState.Error(
                "Escolha uma nota de 1 a 5."
            )
            return
        }

        val isUpdating = currentSummary.currentUserScore != null
        _actionState.value = RatingActionState.Loading

        repository.saveRating(eventId, score) { result ->
            _actionState.value = result.fold(
                onSuccess = {
                    loadRatingSummary(eventId)
                    RatingActionState.Success(
                        if (isUpdating) {
                            "Avaliação atualizada."
                        } else {
                            "Avaliação registrada."
                        }
                    )
                },
                onFailure = { exception ->
                    RatingActionState.Error(
                        exception.toUserMessage(
                            "Não foi possível salvar a avaliação."
                        )
                    )
                }
            )
        }
    }

    fun clearActionState() {
        _actionState.value = RatingActionState.Idle
    }
}
