package br.com.uri.campushub.viewmodel

import br.com.uri.campushub.model.Event
import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Test

class EventFilterTest {

    private val now = Timestamp(2_000, 0)
    private val events = listOf(
        Event(
            id = "1",
            title = "Semana Acadêmica",
            category = "Tecnologia",
            endAt = Timestamp(1_000, 0)
        ),
        Event(
            id = "2",
            title = "Festival de Música",
            category = "Cultura",
            endAt = Timestamp(3_000, 0)
        ),
        Event(
            id = "3",
            title = "Oficina de Kotlin",
            category = "Tecnologia",
            endAt = now
        )
    )

    @Test
    fun `search matches title ignoring case and spaces`() {
        val result = filter(searchQuery = "  kotlin  ")

        assertEquals(listOf("3"), result.map(Event::id))
    }

    @Test
    fun `category filter ignores case`() {
        val result = filter(category = "tecnologia")

        assertEquals(listOf("1", "3"), result.map(Event::id))
    }

    @Test
    fun `situation separates upcoming and finished events`() {
        val upcoming = filter(situation = EventSituationFilter.UPCOMING)
        val finished = filter(situation = EventSituationFilter.FINISHED)

        assertEquals(listOf("2"), upcoming.map(Event::id))
        assertEquals(listOf("1", "3"), finished.map(Event::id))
    }

    @Test
    fun `filters can be combined`() {
        val result = filter(
            searchQuery = "semana",
            category = "Tecnologia",
            situation = EventSituationFilter.FINISHED
        )

        assertEquals(listOf("1"), result.map(Event::id))
    }

    @Test
    fun `event without end date is upcoming and blank category is ignored`() {
        val eventWithoutEnd = Event(id = "4", title = "Evento sem encerramento")

        val result = EventFilter.apply(
            events = listOf(eventWithoutEnd),
            searchQuery = "",
            category = "   ",
            situation = EventSituationFilter.UPCOMING,
            now = now
        )

        assertEquals(listOf(eventWithoutEnd), result)
    }

    @Test
    fun `categories are trimmed unique and sorted`() {
        val source = listOf(
            Event(category = " Tecnologia "),
            Event(category = "Cultura"),
            Event(category = "Tecnologia"),
            Event(category = " ")
        )

        assertEquals(listOf("Cultura", "Tecnologia"), EventFilter.categories(source))
    }

    private fun filter(
        searchQuery: String = "",
        category: String? = null,
        situation: EventSituationFilter = EventSituationFilter.ALL
    ): List<Event> {
        return EventFilter.apply(events, searchQuery, category, situation, now)
    }
}
