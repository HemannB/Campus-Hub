package br.com.uri.campushub.integration

import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.uri.campushub.repository.RegistrationRepository
import br.com.uri.campushub.model.Registration
import br.com.uri.campushub.model.RegistrationStatus
import br.com.uri.campushub.model.registrationStatus
import br.com.uri.campushub.model.User
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
        val userId = auth.currentUser!!.uid
        awaitTask(firestore.collection("users").document(userId).set(User(id = userId, name = "Aluno")))
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
                    "startAt" to Timestamp.now(),
                    "endAt" to Timestamp(Timestamp.now().seconds + 3_600, 0),
                    "participantCount" to 0,
                    "maxParticipants" to 20
                )
            )
        )

        val registration = awaitResult<Unit> { onResult ->
            repository.register(eventId, onResult)
        }
        assertTrue(registration.isSuccess)

        val registeredStatus = awaitResult<Registration?> { onResult ->
            repository.getRegistration(eventId, onResult)
        }
        val registeredIds = awaitResult<Set<String>> { onResult ->
            repository.getRegisteredEventIds(onResult)
        }
        val eventAfterRegistration = awaitTaskResult(eventDocument.get())

        assertEquals(RegistrationStatus.CONFIRMED, registeredStatus.getOrThrow()?.registrationStatus)
        assertEquals(setOf(eventId), registeredIds.getOrThrow())
        assertEquals(1L, eventAfterRegistration.getLong("participantCount"))

        val cancellation = awaitResult<Unit> { onResult ->
            repository.cancel(eventId, onResult)
        }
        assertTrue(cancellation.isSuccess)

        val statusAfterCancellation = awaitResult<Registration?> { onResult ->
            repository.getRegistration(eventId, onResult)
        }
        val eventAfterCancellation = awaitTaskResult(eventDocument.get())

        assertNull(statusAfterCancellation.getOrThrow())
        assertEquals(0L, eventAfterCancellation.getLong("participantCount"))
    }
}
