package br.com.uri.campushub.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import br.com.uri.campushub.repository.FavoriteRepository

sealed class FavoriteState {
    object Idle : FavoriteState()
    object Loading : FavoriteState()
    data class Status(val isFavorite: Boolean) : FavoriteState()
    data class Updating(val isFavorite: Boolean) : FavoriteState()
    data class Success(
        val isFavorite: Boolean,
        val message: String
    ) : FavoriteState()
    data class Error(
        val message: String,
        val isFavorite: Boolean?
    ) : FavoriteState()
}

class FavoriteViewModel : ViewModel() {

    private val repository = FavoriteRepository()
    private var currentStatus: Boolean? = null

    private val _favoriteState =
        MutableLiveData<FavoriteState>(FavoriteState.Idle)
    val favoriteState: LiveData<FavoriteState> = _favoriteState

    fun loadStatus(eventId: String) {
        _favoriteState.value = FavoriteState.Loading

        repository.getFavoriteStatus(eventId) { result ->
            _favoriteState.value = result.fold(
                onSuccess = { isFavorite ->
                    currentStatus = isFavorite
                    FavoriteState.Status(isFavorite)
                },
                onFailure = { exception ->
                    FavoriteState.Error(
                        message = exception.toUserMessage(
                            "Não foi possível consultar o favorito."
                        ),
                        isFavorite = currentStatus
                    )
                }
            )
        }
    }

    fun toggleFavorite(eventId: String) {
        val isFavorite = currentStatus ?: return
        _favoriteState.value = FavoriteState.Updating(isFavorite)

        val onResult: (Result<Unit>) -> Unit = { result ->
            _favoriteState.value = result.fold(
                onSuccess = {
                    currentStatus = !isFavorite
                    FavoriteState.Success(
                        isFavorite = !isFavorite,
                        message = if (isFavorite) {
                            "Evento removido dos favoritos."
                        } else {
                            "Evento adicionado aos favoritos."
                        }
                    )
                },
                onFailure = { exception ->
                    FavoriteState.Error(
                        message = exception.toUserMessage(
                            "Não foi possível atualizar o favorito."
                        ),
                        isFavorite = isFavorite
                    )
                }
            )
        }

        if (isFavorite) {
            repository.unfavorite(eventId, onResult)
        } else {
            repository.favorite(eventId, onResult)
        }
    }
}
