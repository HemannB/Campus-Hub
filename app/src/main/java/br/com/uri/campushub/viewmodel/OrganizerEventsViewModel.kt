package br.com.uri.campushub.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import br.com.uri.campushub.repository.AuthRepository
import br.com.uri.campushub.repository.CurrentUserDataSource
import br.com.uri.campushub.repository.EventRepository
import br.com.uri.campushub.repository.OrganizerEventDataSource

class OrganizerEventsViewModel(
    private val authRepository: CurrentUserDataSource = AuthRepository(),
    private val eventRepository: OrganizerEventDataSource = EventRepository()
) : ViewModel() {

    private val _eventState = MutableLiveData<EventState>(EventState.Idle)
    val eventState: LiveData<EventState> = _eventState

    fun loadEvents() {
        val organizerId = authRepository.currentUserId()
        if (organizerId == null) {
            _eventState.value = EventState.Error("Usuário não autenticado.")
            return
        }

        _eventState.value = EventState.Loading
        eventRepository.getOrganizerEvents(organizerId) { result ->
            _eventState.value = result.fold(
                onSuccess = { events ->
                    EventState.Success(
                        events.sortedWith(compareBy(nullsLast()) { it.startAt })
                    )
                },
                onFailure = { exception ->
                    EventState.Error(
                        exception.toUserMessage("Não foi possível carregar seus eventos.")
                    )
                }
            )
        }
    }
}
