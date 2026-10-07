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
        val availableCategories = events
            .map { event -> event.category.trim() }
            .filter { category -> category.isNotBlank() }
            .distinct()
            .sorted()

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
        val filteredEvents = allEvents.filter { event ->
            matchesSearch(event) &&
                matchesCategory(event) &&
                matchesSituation(event, now)
        }

        _eventState.value = EventState.Success(
            events = filteredEvents,
            isFiltered = searchQuery.isNotBlank() ||
                selectedCategory != null ||
                selectedSituation != EventSituationFilter.ALL
        )
    }

    private fun matchesSearch(event: Event): Boolean {
        return searchQuery.isBlank() ||
            event.title.contains(searchQuery, ignoreCase = true)
    }

    private fun matchesCategory(event: Event): Boolean {
        val category = selectedCategory ?: return true
        return event.category.equals(category, ignoreCase = true)
    }

    private fun matchesSituation(event: Event, now: Timestamp): Boolean {
        val isFinished = event.endAt?.compareTo(now)?.let { it < 0 } ?: false

        return when (selectedSituation) {
            EventSituationFilter.ALL -> true
            EventSituationFilter.UPCOMING -> !isFinished
            EventSituationFilter.FINISHED -> isFinished
        }
    }
}
