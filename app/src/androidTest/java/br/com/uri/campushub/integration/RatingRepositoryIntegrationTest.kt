package br.com.uri.campushub.integration

import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.uri.campushub.repository.RatingRepository
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class RatingRepositoryIntegrationTest {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var repository: RatingRepository

    @Before
    fun setUp() {
        auth = FirebaseEmulatorConfig.auth()
        auth.signOut()
        firestore = FirebaseEmulatorConfig.firestore()
        repository = RatingRepository(auth, firestore)
    }

    @After
    fun tearDown() {
        auth.signOut()
    }

    @Test
    fun createAndUpdateRatingKeepOneScorePerUserAndCalculateAverage() {
        val eventId = "event-${UUID.randomUUID()}"
        awaitTask(
            firestore.collection("events").document(eventId).set(
                mapOf(
                    "title" to "Evento encerrado",
                    "startAt" to Timestamp(1_000, 0),
                    "endAt" to Timestamp(2_000, 0)
                )
            )
        )

        createUserAndRegistration(eventId, "rating-first")
        val firstRating = awaitResult<Unit> { onResult ->
            repository.saveRating(eventId, 5, onResult)
        }
        assertTrue(firstRating.isSuccess)

        val ratingUpdate = awaitResult<Unit> { onResult ->
            repository.saveRating(eventId, 3, onResult)
        }
        assertTrue(ratingUpdate.isSuccess)

        val firstUserId = requireNotNull(auth.currentUser?.uid)
        val firstUserRatings = awaitTaskResult(
            firestore.collection("ratings")
                .whereEqualTo("userId", firstUserId)
                .whereEqualTo("eventId", eventId)
                .get()
        )
        assertEquals(1, firstUserRatings.size())

        auth.signOut()
        createUserAndRegistration(eventId, "rating-second")
        val secondRating = awaitResult<Unit> { onResult ->
            repository.saveRating(eventId, 5, onResult)
        }
        assertTrue(secondRating.isSuccess)

        val summary = awaitResult { onResult ->
            repository.getRatingSummary(eventId, onResult)
        }.getOrThrow()

        assertEquals(4.0, summary.average, 0.0)
        assertEquals(2, summary.count)
        assertEquals(5, summary.currentUserScore)
    }

    private fun createUserAndRegistration(eventId: String, prefix: String) {
        awaitTask(
            auth.createUserWithEmailAndPassword(
                "$prefix-${UUID.randomUUID()}@example.com",
                "123456"
            )
        )
        val userId = requireNotNull(auth.currentUser?.uid)
        awaitTask(
            firestore.collection("registrations")
                .document("${userId}_$eventId")
                .set(
                    mapOf(
                        "userId" to userId,
                        "eventId" to eventId,
                        "registeredAt" to Timestamp.now()
                    )
                )
        )
    }
}
