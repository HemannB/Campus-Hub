package br.com.uri.campushub.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import br.com.uri.campushub.model.*
import br.com.uri.campushub.repository.*

sealed class OrganizerEventState {
    object Loading : OrganizerEventState()
    data class Ready(val event: Event, val attendees: List<Registration>) : OrganizerEventState()
    data class Error(val message: String) : OrganizerEventState()
}

class OrganizerEventViewModel(
    private val auth: CurrentUserDataSource = AuthRepository(),
    private val events: EventEditorDataSource = EventRepository(),
    private val attendance: AttendanceDataSource = AttendanceRepository()
) : ViewModel() {
    private val _state = MutableLiveData<OrganizerEventState>(OrganizerEventState.Loading)
    val state: LiveData<OrganizerEventState> = _state
    private val _message = MutableLiveData<String?>()
    val message: LiveData<String?> = _message
    private var busy = false

    fun load(eventId: String) {
        if (busy) return
        _state.value = OrganizerEventState.Loading
        events.getEventById(eventId) { result ->
            result.fold(onSuccess = { event ->
                if (auth.currentUserId() == null || event.organizerId != auth.currentUserId()) {
                    showError(IllegalStateException("Este evento não pertence a você."))
                    return@fold
                }
                attendance.getAttendees(eventId) { attendees ->
                    _state.value = attendees.fold(
                        onSuccess = { OrganizerEventState.Ready(event, it) },
                        onFailure = { OrganizerEventState.Error(it.toUserMessage("Não foi possível carregar os inscritos.")) }
                    )
                }
            }, onFailure = ::showError)
        }
    }

    fun changeStatus(eventId: String, status: EventStatus) =
        perform(eventId, "Evento atualizado.") { events.changeStatus(eventId, status, it) }

    fun checkIn(eventId: String, ticket: String) =
        perform(eventId, "Presença registrada.") { attendance.checkIn(eventId, ticket, it) }

    fun clearMessage() { _message.value = null }

    private fun perform(eventId: String, successMessage: String, action: ((Result<Unit>) -> Unit) -> Unit) {
        if (busy) return
        busy = true
        _state.value = OrganizerEventState.Loading
        action { result ->
            busy = false
            _message.value = result.fold(onSuccess = { successMessage },
                onFailure = { it.toUserMessage("Não foi possível concluir a operação.") })
            load(eventId)
        }
    }

    private fun showError(error: Throwable) {
        _state.value = OrganizerEventState.Error(error.toUserMessage("Não foi possível carregar o evento."))
    }
}
