package br.com.uri.campushub.viewmodel

import br.com.uri.campushub.model.Event
import com.google.firebase.Timestamp

object EventFilter {

    fun apply(
        events: List<Event>,
        searchQuery: String,
        category: String?,
        situation: EventSituationFilter,
        now: Timestamp
    ): List<Event> {
        val normalizedQuery = searchQuery.trim()
        val normalizedCategory = category?.trim()?.takeIf(String::isNotBlank)

        return events.filter { event ->
            val matchesSearch = normalizedQuery.isBlank() ||
                event.title.contains(normalizedQuery, ignoreCase = true)
            val matchesCategory = normalizedCategory == null ||
                event.category.equals(normalizedCategory, ignoreCase = true)
            val isFinished = event.endAt?.compareTo(now)?.let { comparison ->
                comparison <= 0
            } ?: false
            val matchesSituation = when (situation) {
                EventSituationFilter.ALL -> true
                EventSituationFilter.UPCOMING -> !isFinished
                EventSituationFilter.FINISHED -> isFinished
            }

            matchesSearch && matchesCategory && matchesSituation
        }
    }

    fun categories(events: List<Event>): List<String> {
        return events
            .map { event -> event.category.trim() }
            .filter(String::isNotBlank)
            .distinct()
            .sorted()
    }
}
