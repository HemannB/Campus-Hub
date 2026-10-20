package br.com.uri.campushub.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import br.com.uri.campushub.model.EventPublication
import br.com.uri.campushub.model.PublicationType
import br.com.uri.campushub.repository.AuthRepository
import br.com.uri.campushub.repository.PublicationRepository
import br.com.uri.campushub.repository.UserRepository
import java.util.concurrent.Executors

sealed class PublicationListState {
    object Idle : PublicationListState()
    object Loading : PublicationListState()
    data class Success(val publications: List<EventPublication>) : PublicationListState()
    data class Error(val message: String) : PublicationListState()
}

sealed class PublicationActionState {
    object Idle : PublicationActionState()
    object Loading : PublicationActionState()
    data class Success(val message: String) : PublicationActionState()
    data class Error(val message: String) : PublicationActionState()
}

class PublicationViewModel(application: Application) : AndroidViewModel(application) {
    private val authRepository = AuthRepository()
    private val userRepository = UserRepository()
    private val publicationRepository = PublicationRepository(application)
    private val fileExecutor = Executors.newSingleThreadExecutor()

    private val _listState = MutableLiveData<PublicationListState>(PublicationListState.Idle)
    val listState: LiveData<PublicationListState> = _listState

    private val _actionState = MutableLiveData<PublicationActionState>(PublicationActionState.Idle)
    val actionState: LiveData<PublicationActionState> = _actionState

    fun currentUserId(): String? = authRepository.currentUserId()

    fun loadPublications(eventId: String) {
        _listState.value = PublicationListState.Loading

        fileExecutor.execute {
            val state = publicationRepository.getPublications(eventId).fold(
                onSuccess = { publications -> PublicationListState.Success(publications) },
                onFailure = { exception ->
                    PublicationListState.Error(
                        exception.toUserMessage("Não foi possível carregar as publicações.")
                    )
                }
            )
            _listState.postValue(state)
        }
    }

    fun publish(
        eventId: String,
        type: PublicationType,
        sourceUri: Uri
    ) {
        val userId = authRepository.currentUserId()

        if (userId == null) {
            _actionState.value = PublicationActionState.Error("Usuário não autenticado.")
            return
        }

        _actionState.value = PublicationActionState.Loading
        userRepository.getProfile(userId) { profileResult ->
            profileResult.fold(
                onSuccess = { user ->
                    fileExecutor.execute {
                        val result = publicationRepository.publishFromUri(
                            eventId = eventId,
                            authorName = user.name,
                            type = type,
                            sourceUri = sourceUri
                        )
                        handleActionResult(eventId, "Publicação adicionada.", result.map { Unit })
                    }
                },
                onFailure = { exception ->
                    _actionState.value = PublicationActionState.Error(
                        exception.toUserMessage("Não foi possível identificar o autor.")
                    )
                }
            )
        }
    }

    fun deletePublication(eventId: String, publicationId: String) {
        _actionState.value = PublicationActionState.Loading

        fileExecutor.execute {
            handleActionResult(
                eventId = eventId,
                successMessage = "Publicação excluída.",
                result = publicationRepository.deletePublication(publicationId)
            )
        }
    }

    fun clearActionState() {
        _actionState.value = PublicationActionState.Idle
    }

    override fun onCleared() {
        fileExecutor.shutdownNow()
        super.onCleared()
    }

    private fun handleActionResult(
        eventId: String,
        successMessage: String,
        result: Result<Unit>
    ) {
        result.fold(
            onSuccess = {
                loadPublicationsFromWorker(eventId)
                _actionState.postValue(PublicationActionState.Success(successMessage))
            },
            onFailure = { exception ->
                _actionState.postValue(
                    PublicationActionState.Error(
                        exception.toUserMessage("Não foi possível atualizar a publicação.")
                    )
                )
            }
        )
    }

    private fun loadPublicationsFromWorker(eventId: String) {
        val state = publicationRepository.getPublications(eventId).fold(
            onSuccess = { publications -> PublicationListState.Success(publications) },
            onFailure = { exception ->
                PublicationListState.Error(
                    exception.toUserMessage("Não foi possível carregar as publicações.")
                )
            }
        )
        _listState.postValue(state)
    }
}
