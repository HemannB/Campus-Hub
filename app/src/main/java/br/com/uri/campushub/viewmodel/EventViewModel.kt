package br.com.uri.campushub.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import br.com.uri.campushub.model.Event
import br.com.uri.campushub.repository.EventRepository

sealed class EventState {
    object Idle : EventState()
    object Loading : EventState()
    data class Success(val events: List<Event>) : EventState()
    data class Error(val message: String) : EventState()
}

class EventViewModel : ViewModel() {

    private val repository = EventRepository()

    private val _eventState = MutableLiveData<EventState>(EventState.Idle)
    val eventState: LiveData<EventState> = _eventState

    fun loadEvents() {
        _eventState.value = EventState.Loading

        repository.getEvents { result ->
            _eventState.value = result.fold(
                onSuccess = { events ->
                    EventState.Success(events)
                },
                onFailure = { exception ->
                    EventState.Error(
                        exception.toUserMessage(
                            "Não foi possível carregar os eventos."
                        )
                    )
                }
            )
        }
    }
}
