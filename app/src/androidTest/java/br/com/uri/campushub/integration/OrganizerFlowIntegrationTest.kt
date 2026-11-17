package br.com.uri.campushub.integration

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import br.com.uri.campushub.model.*
import br.com.uri.campushub.repository.*
import com.google.firebase.Timestamp
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject
import org.json.JSONArray

@RunWith(AndroidJUnit4::class)
class OrganizerFlowIntegrationTest {
    private val auth get() = FirebaseEmulatorConfig.auth()
    private val firestore get() = FirebaseEmulatorConfig.firestore()

    private fun account(role: String): Pair<String, String> {
        val email = "flow-${UUID.randomUUID()}@example.com"
        awaitTask(auth.createUserWithEmailAndPassword(email, "123456"))
        val id = auth.currentUser!!.uid
        awaitTask(firestore.collection("users").document(id).set(User(id = id, name = email)))
        if (role == "ORGANIZER") assignOrganizerInEmulator(id)
        return id to email
    }

    private fun assignOrganizerInEmulator(userId: String) {
        val host = InstrumentationRegistry.getArguments().getString("firebaseHost") ?: "10.0.2.2"
        val root = "projects/demo-campushub/databases/(default)/documents"
        val connection = URL("http://$host:8080/v1/$root:commit").openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Authorization", "Bearer owner")
        connection.setRequestProperty("Content-Type", "application/json")
        connection.connectTimeout = 5000
        connection.readTimeout = 5000
        connection.doOutput = true
        val update = JSONObject().put("name", "$root/users/$userId")
            .put("fields", JSONObject().put("role", JSONObject().put("stringValue", "ORGANIZER")))
        val write = JSONObject().put("update", update)
            .put("updateMask", JSONObject().put("fieldPaths", JSONArray().put("role")))
        try {
            connection.outputStream.use { it.write(JSONObject().put("writes", JSONArray().put(write)).toString().toByteArray()) }
            assertEquals(200, connection.responseCode)
        } finally {
            connection.disconnect()
        }
    }

    private fun login(account: Pair<String, String>) {
        awaitTask(auth.signInWithEmailAndPassword(account.second, "123456"))
    }

    @Test fun organizerCreatesEditsPublishesAndEndsOnlyOwnEvent() {
        val owner = account("ORGANIZER")
        val repository = EventRepository(firestore, auth)
        val draft = Event(title = "Oficina", description = "Kotlin", category = "Tecnologia",
            location = "URI Erechim", organizerId = owner.first, status = "DRAFT", maxParticipants = 2,
            startAt = Timestamp(Timestamp.now().seconds - 60, 0),
            endAt = Timestamp(Timestamp.now().seconds + 3600, 0))
        awaitResult<Unit> { repository.createEvent(draft, it) }.getOrThrow()
        val created = awaitResult<List<Event>> { repository.getOrganizerEvents(owner.first, it) }.getOrThrow().single()
        awaitResult<Unit> { repository.updateEvent(created.copy(title = "Oficina atualizada"), it) }.getOrThrow()
        awaitResult<Unit> { repository.changeStatus(created.id, EventStatus.PUBLISHED, it) }.getOrThrow()
        account("ORGANIZER")
        assertTrue(awaitResult<Unit> { repository.updateEvent(created, it) }.isFailure)
        login(owner)
        awaitResult<Unit> { repository.changeStatus(created.id, EventStatus.ENDED, it) }.getOrThrow()
        val ended = awaitResult<Event> { repository.getEventById(created.id, it) }.getOrThrow()
        assertEquals("Oficina atualizada", ended.title)
        assertEquals(EventStatus.ENDED, ended.eventStatus)
        assertTrue(awaitResult<Unit> { repository.updateEvent(ended, it) }.isFailure)
    }

    @Test fun waitlistPromotionConfirmationAndSingleCheckInWorkTogether() {
        val owner = account("ORGANIZER")
        val eventId = UUID.randomUUID().toString()
        awaitTask(firestore.collection("events").document(eventId).set(Event(
            id = eventId, title = "Evento", description = "Oficina", category = "Tecnologia",
            location = "URI Erechim", organizerId = owner.first, maxParticipants = 1, status = "DRAFT",
            startAt = Timestamp(Timestamp.now().seconds - 60, 0),
            endAt = Timestamp(Timestamp.now().seconds + 3600, 0)
        )))
        awaitResult<Unit> { EventRepository(firestore, auth).changeStatus(eventId, EventStatus.PUBLISHED, it) }.getOrThrow()
        val first = account("STUDENT")
        val second = account("STUDENT")
        val third = account("STUDENT")
        val repo = RegistrationRepository(auth, firestore)
        login(first)
        awaitResult<Unit> { repo.register(eventId, it) }.getOrThrow()
        login(second)
        awaitResult<Unit> { repo.register(eventId, it) }.getOrThrow()
        assertEquals(RegistrationStatus.WAITLISTED,
            awaitResult<Registration?> { repo.getRegistration(eventId, it) }.getOrThrow()!!.registrationStatus)
        login(third)
        awaitResult<Unit> { repo.register(eventId, it) }.getOrThrow()
        assertTrue(awaitResult<Unit> { repo.confirmInvitation(eventId, it) }.isFailure)
        login(first)
        awaitResult<Unit> { repo.cancel(eventId, it) }.getOrThrow()
        login(second)
        assertEquals(RegistrationStatus.INVITED,
            awaitResult<Registration?> { repo.getRegistration(eventId, it) }.getOrThrow()!!.registrationStatus)
        awaitResult<Unit> { repo.confirmInvitation(eventId, it) }.getOrThrow()
        val registration = awaitResult<Registration?> { repo.getRegistration(eventId, it) }.getOrThrow()!!
        assertEquals(RegistrationStatus.CONFIRMED, registration.registrationStatus)
        val event = awaitTaskResult(firestore.collection("events").document(eventId).get())
        assertEquals(1L, event.getLong("participantCount"))
        login(owner)
        val attendance = AttendanceRepository(auth, firestore)
        val code = CheckInTicket(eventId, registration.id, registration.qrToken).encode()
        assertTrue(awaitResult<Unit> { attendance.checkIn(eventId, code + "invalid", it) }.isFailure)
        awaitResult<Unit> { attendance.checkIn(eventId, code, it) }.getOrThrow()
        assertTrue(awaitResult<Unit> { attendance.checkIn(eventId, code, it) }.isFailure)
        val attendees = awaitResult<List<Registration>> { attendance.getAttendees(eventId, it) }.getOrThrow()
        assertNotNull(attendees.single { it.userId == second.first }.checkedInAt)
        login(second)
        assertTrue(awaitResult<Unit> { repo.cancel(eventId, it) }.isFailure)
    }
}
