package br.com.uri.campushub.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import br.com.uri.campushub.model.Event
import br.com.uri.campushub.repository.EventRepository

sealed class EventDetailState {
    object Idle : EventDetailState()
    object Loading : EventDetailState()
    data class Success(val event: Event) : EventDetailState()
    data class Error(val message: String) : EventDetailState()
}

class EventDetailViewModel : ViewModel() {

    private val repository = EventRepository()

    private val _eventDetailState = MutableLiveData<EventDetailState>(EventDetailState.Idle)
    val eventDetailState: LiveData<EventDetailState> = _eventDetailState

    fun loadEvent(eventId: String) {
        _eventDetailState.value = EventDetailState.Loading

        repository.getEventById(eventId) { result ->
            _eventDetailState.value = result.fold(
                onSuccess = { event ->
                    EventDetailState.Success(event)
                },
                onFailure = { exception ->
                    EventDetailState.Error(
                        exception.toUserMessage(
                            "Não foi possível carregar o evento."
                        )
                    )
                }
            )
        }
    }
}
