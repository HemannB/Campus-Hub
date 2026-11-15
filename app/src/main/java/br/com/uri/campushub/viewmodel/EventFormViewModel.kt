package br.com.uri.campushub.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import br.com.uri.campushub.model.Event
import br.com.uri.campushub.model.EventValidator
import br.com.uri.campushub.model.EventStatus
import br.com.uri.campushub.model.UserRole
import br.com.uri.campushub.model.userRole
import br.com.uri.campushub.repository.AuthRepository
import br.com.uri.campushub.repository.CurrentUserDataSource
import br.com.uri.campushub.repository.EventEditorDataSource
import br.com.uri.campushub.repository.EventRepository
import br.com.uri.campushub.repository.UserDataSource
import br.com.uri.campushub.repository.UserRepository

sealed class EventFormState {
    object Idle : EventFormState()
    object Saving : EventFormState()
    data class Ready(val event: Event) : EventFormState()
    object Success : EventFormState()
    data class Error(val message: String) : EventFormState()
}

class EventFormViewModel(
    private val authRepository: CurrentUserDataSource = AuthRepository(),
    private val userRepository: UserDataSource = UserRepository(),
    private val eventRepository: EventEditorDataSource = EventRepository()
) : ViewModel() {

    private val _state = MutableLiveData<EventFormState>(EventFormState.Idle)
    val state: LiveData<EventFormState> = _state
    private var editingEvent: Event? = null
    private var editingRequested = false

    fun loadEvent(eventId: String) {
        editingRequested = true
        _state.value = EventFormState.Saving
        eventRepository.getEventById(eventId) { result ->
            result.fold(onSuccess = { event ->
                if (event.organizerId != authRepository.currentUserId() || event.status == EventStatus.ENDED.name) {
                    showError(IllegalStateException("Você não pode editar este evento."))
                } else {
                    editingEvent = event
                    _state.value = EventFormState.Ready(event)
                }
            }, onFailure = ::showError)
        }
    }

    fun saveEvent(event: Event) {
        if (_state.value == EventFormState.Saving || _state.value == EventFormState.Success) return
        if (editingRequested && editingEvent == null) {
            showError(IllegalStateException("Carregue o evento antes de salvar."))
            return
        }

        val userId = authRepository.currentUserId()
        if (userId == null) {
            _state.value = EventFormState.Error("Usuário não autenticado.")
            return
        }

        val validationError = EventValidator.validate(event)
        if (validationError != null) {
            _state.value = EventFormState.Error(validationError)
            return
        }

        _state.value = EventFormState.Saving
        userRepository.getProfile(userId) { profileResult ->
            profileResult.fold(
                onSuccess = { user ->
                    if (user.userRole != UserRole.ORGANIZER) {
                        _state.value = EventFormState.Error("Somente organizadores podem criar eventos.")
                    } else {
                        val draft = event.copy(
                            id = editingEvent?.id.orEmpty(),
                            title = event.title.trim(),
                            description = event.description.trim(),
                            category = event.category.trim(),
                            location = event.location.trim(),
                            imageUrl = event.imageUrl.trim(),
                            participantCount = 0,
                            organizer = user.name,
                            organizerId = userId,
                            status = editingEvent?.status ?: EventStatus.DRAFT.name
                        )
                        val onSaved: (Result<Unit>) -> Unit = { result ->
                            result.fold(
                                onSuccess = { _state.value = EventFormState.Success },
                                onFailure = ::showError
                            )
                        }
                        if (editingEvent == null) eventRepository.createEvent(draft, onSaved)
                        else eventRepository.updateEvent(draft, onSaved)
                    }
                },
                onFailure = ::showError
            )
        }
    }

    private fun showError(exception: Throwable) {
        _state.value = EventFormState.Error(
            exception.toUserMessage("Não foi possível salvar o rascunho.")
        )
    }
}
