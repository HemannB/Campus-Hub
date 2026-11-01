package br.com.uri.campushub.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import br.com.uri.campushub.model.Event
import br.com.uri.campushub.repository.EventRepository
import com.google.firebase.Timestamp

sealed class EventState {
    object Idle : EventState()
    object Loading : EventState()
    data class Success(
        val events: List<Event>,
        val isFiltered: Boolean = false
    ) : EventState()
    data class Error(val message: String) : EventState()
}

enum class EventSituationFilter {
    ALL,
    UPCOMING,
    FINISHED
}

class EventViewModel : ViewModel() {

    private val repository = EventRepository()
    private var allEvents: List<Event> = emptyList()
    private var searchQuery = ""
    private var selectedCategory: String? = null
    private var selectedSituation = EventSituationFilter.ALL
    private var hasLoadedEvents = false

    val categoryFilter: String?
        get() = selectedCategory

    val situationFilter: EventSituationFilter
        get() = selectedSituation

    private val _eventState = MutableLiveData<EventState>(EventState.Idle)
    val eventState: LiveData<EventState> = _eventState

    private val _categories = MutableLiveData<List<String>>(emptyList())
    val categories: LiveData<List<String>> = _categories

    fun loadEvents() {
        _eventState.value = EventState.Loading
        hasLoadedEvents = false

        repository.getEvents { result ->
            result.fold(
                onSuccess = { events ->
                    allEvents = events
                    hasLoadedEvents = true
                    updateCategories(events)
                    applyFilters()
                },
                onFailure = { exception ->
                    _eventState.value = EventState.Error(
                        exception.toUserMessage(
                            "Não foi possível carregar os eventos."
                        )
                    )
                }
            )
        }
    }

    fun setSearchQuery(query: String) {
        searchQuery = query.trim()
        applyFiltersIfLoaded()
    }

    fun setCategory(category: String?) {
        selectedCategory = category
        applyFiltersIfLoaded()
    }

    fun setSituation(situation: EventSituationFilter) {
        selectedSituation = situation
        applyFiltersIfLoaded()
    }

    private fun updateCategories(events: List<Event>) {
        val availableCategories = EventFilter.categories(events)

        if (selectedCategory !in availableCategories) {
            selectedCategory = null
        }

        _categories.value = availableCategories
    }

    private fun applyFiltersIfLoaded() {
        if (hasLoadedEvents) {
            applyFilters()
        }
    }

    private fun applyFilters() {
        val now = Timestamp.now()
        val filteredEvents = EventFilter.apply(
            events = allEvents,
            searchQuery = searchQuery,
            category = selectedCategory,
            situation = selectedSituation,
            now = now
        )

        _eventState.value = EventState.Success(
            events = filteredEvents,
            isFiltered = searchQuery.isNotBlank() ||
                selectedCategory != null ||
                selectedSituation != EventSituationFilter.ALL
        )
    }
}
