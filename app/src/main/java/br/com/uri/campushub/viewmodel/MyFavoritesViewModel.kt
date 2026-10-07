package br.com.uri.campushub.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import br.com.uri.campushub.repository.EventRepository
import br.com.uri.campushub.repository.FavoriteRepository

class MyFavoritesViewModel : ViewModel() {

    private val favoriteRepository = FavoriteRepository()
    private val eventRepository = EventRepository()

    private val _eventState = MutableLiveData<EventState>(EventState.Idle)
    val eventState: LiveData<EventState> = _eventState

    fun loadEvents() {
        _eventState.value = EventState.Loading

        favoriteRepository.getFavoriteEventIds { favoritesResult ->
            favoritesResult.fold(
                onSuccess = { eventIds ->
                    if (eventIds.isEmpty()) {
                        _eventState.value = EventState.Success(emptyList())
                    } else {
                        loadFavoriteEvents(eventIds)
                    }
                },
                onFailure = { exception ->
                    showError(exception)
                }
            )
        }
    }

    private fun loadFavoriteEvents(eventIds: Set<String>) {
        eventRepository.getEvents { eventsResult ->
            _eventState.value = eventsResult.fold(
                onSuccess = { events ->
                    EventState.Success(
                        events.filter { event -> event.id in eventIds }
                    )
                },
                onFailure = { exception ->
                    EventState.Error(
                        exception.toUserMessage(
                            "Não foi possível carregar seus favoritos."
                        )
                    )
                }
            )
        }
    }

    private fun showError(exception: Throwable) {
        _eventState.value = EventState.Error(
            exception.toUserMessage(
                "Não foi possível carregar seus favoritos."
            )
        )
    }
}
