package br.com.uri.campushub.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import br.com.uri.campushub.repository.EventRepository
import br.com.uri.campushub.repository.RegistrationRepository

class MyEventsViewModel : ViewModel() {

    private val registrationRepository = RegistrationRepository()
    private val eventRepository = EventRepository()

    private val _eventState = MutableLiveData<EventState>(EventState.Idle)
    val eventState: LiveData<EventState> = _eventState

    fun loadEvents() {
        _eventState.value = EventState.Loading

        registrationRepository.getRegisteredEventIds { registrationsResult ->
            registrationsResult.fold(
                onSuccess = { eventIds ->
                    if (eventIds.isEmpty()) {
                        _eventState.value = EventState.Success(emptyList())
                    } else {
                        loadRegisteredEvents(eventIds)
                    }
                },
                onFailure = { exception ->
                    showError(exception)
                }
            )
        }
    }

    private fun loadRegisteredEvents(eventIds: Set<String>) {
        eventRepository.getEvents { eventsResult ->
            _eventState.value = eventsResult.fold(
                onSuccess = { events ->
                    EventState.Success(
                        events.filter { event -> event.id in eventIds }
                    )
                },
                onFailure = { exception ->
                    EventState.Error(
                        exception.message
                            ?: "Não foi possível carregar seus eventos."
                    )
                }
            )
        }
    }

    private fun showError(exception: Throwable) {
        _eventState.value = EventState.Error(
            exception.message
                ?: "Não foi possível carregar seus eventos."
        )
    }
}
