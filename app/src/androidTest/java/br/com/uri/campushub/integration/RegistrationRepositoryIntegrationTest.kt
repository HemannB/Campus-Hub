package br.com.uri.campushub.integration

import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.uri.campushub.repository.RegistrationRepository
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class RegistrationRepositoryIntegrationTest {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var repository: RegistrationRepository

    @Before
    fun setUp() {
        auth = FirebaseEmulatorConfig.auth()
        auth.signOut()
        awaitTask(
            auth.createUserWithEmailAndPassword(
                "registration-${UUID.randomUUID()}@example.com",
                "123456"
            )
        )
        firestore = FirebaseEmulatorConfig.firestore()
        repository = RegistrationRepository(auth, firestore)
    }

    @After
    fun tearDown() {
        auth.signOut()
    }

    @Test
    fun registerAndCancelUpdateStatusAndParticipantCount() {
        val eventId = "event-${UUID.randomUUID()}"
        val eventDocument = firestore.collection("events").document(eventId)
        awaitTask(
            eventDocument.set(
                mapOf(
                    "title" to "Evento de integração",
                    "startAt" to Timestamp(2_000, 0),
                    "endAt" to Timestamp(3_000, 0),
                    "participantCount" to 0,
                    "maxParticipants" to 20
                )
            )
        )

        val registration = awaitResult<Unit> { onResult ->
            repository.register(eventId, onResult)
        }
        assertTrue(registration.isSuccess)

        val registeredStatus = awaitResult<Boolean> { onResult ->
            repository.getRegistrationStatus(eventId, onResult)
        }
        val registeredIds = awaitResult<Set<String>> { onResult ->
            repository.getRegisteredEventIds(onResult)
        }
        val eventAfterRegistration = awaitTaskResult(eventDocument.get())

        assertTrue(registeredStatus.getOrThrow())
        assertEquals(setOf(eventId), registeredIds.getOrThrow())
        assertEquals(1L, eventAfterRegistration.getLong("participantCount"))

        val cancellation = awaitResult<Unit> { onResult ->
            repository.cancel(eventId, onResult)
        }
        assertTrue(cancellation.isSuccess)

        val statusAfterCancellation = awaitResult<Boolean> { onResult ->
            repository.getRegistrationStatus(eventId, onResult)
        }
        val eventAfterCancellation = awaitTaskResult(eventDocument.get())

        assertFalse(statusAfterCancellation.getOrThrow())
        assertEquals(0L, eventAfterCancellation.getLong("participantCount"))
    }
}
