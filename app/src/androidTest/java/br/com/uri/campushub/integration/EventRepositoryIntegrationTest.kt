package br.com.uri.campushub.integration

import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.uri.campushub.model.Event
import br.com.uri.campushub.repository.EventRepository
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class EventRepositoryIntegrationTest {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var repository: EventRepository

    @Before
    fun setUp() {
        firestore = FirebaseEmulatorConfig.firestore()
        repository = EventRepository(firestore)
    }

    @Test
    fun listAndFindEventsReturnPersistedDocuments() {
        val suffix = UUID.randomUUID().toString()
        val firstId = "event-first-$suffix"
        val secondId = "event-second-$suffix"
        val firstEvent = Event(
            id = firstId,
            title = "Evento mais cedo",
            description = "Primeiro evento da lista",
            category = "Tecnologia",
            startAt = Timestamp(1_000, 0),
            endAt = Timestamp(2_000, 0),
            location = "URI Erechim",
            maxParticipants = 30,
            participantCount = 2,
            organizer = "CampusHub"
        )
        val secondEvent = Event(
            id = secondId,
            title = "Evento mais tarde",
            description = "Segundo evento da lista",
            category = "Cultura",
            startAt = Timestamp(3_000, 0),
            endAt = Timestamp(4_000, 0),
            location = "Centro de Eventos",
            maxParticipants = 50,
            participantCount = 5,
            organizer = "CampusHub"
        )

        saveEvent(firstEvent)
        saveEvent(secondEvent)

        val events = awaitResult<List<Event>> { onResult ->
            repository.getEvents(onResult)
        }
        assertTrue(events.isSuccess)
        val createdEvents = events.getOrThrow()
            .filter { event -> event.id == firstId || event.id == secondId }
        assertEquals(
            listOf(firstId, secondId),
            createdEvents.map(Event::id)
        )

        val event = awaitResult<Event> { onResult ->
            repository.getEventById(secondId, onResult)
        }
        assertEquals(secondEvent, event.getOrThrow())
    }

    private fun saveEvent(event: Event) {
        val fields = mapOf(
            "title" to event.title,
            "description" to event.description,
            "category" to event.category,
            "startAt" to event.startAt,
            "endAt" to event.endAt,
            "location" to event.location,
            "imageUrl" to event.imageUrl,
            "maxParticipants" to event.maxParticipants,
            "participantCount" to event.participantCount,
            "organizer" to event.organizer
        )
        awaitTask(firestore.collection("events").document(event.id).set(fields))
    }
}
