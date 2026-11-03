package br.com.uri.campushub.integration

import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.uri.campushub.repository.FavoriteRepository
import com.google.firebase.auth.FirebaseAuth
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class FavoriteRepositoryIntegrationTest {

    private lateinit var auth: FirebaseAuth
    private lateinit var repository: FavoriteRepository

    @Before
    fun setUp() {
        auth = FirebaseEmulatorConfig.auth()
        auth.signOut()
        awaitTask(
            auth.createUserWithEmailAndPassword(
                "favorite-${UUID.randomUUID()}@example.com",
                "123456"
            )
        )
        repository = FavoriteRepository(
            auth,
            FirebaseEmulatorConfig.firestore()
        )
    }

    @After
    fun tearDown() {
        auth.signOut()
    }

    @Test
    fun favoriteAndUnfavoritePersistCurrentUserChoice() {
        val eventId = "event-${UUID.randomUUID()}"

        val favorite = awaitResult<Unit> { onResult ->
            repository.favorite(eventId, onResult)
        }
        assertTrue(favorite.isSuccess)

        val favoriteStatus = awaitResult<Boolean> { onResult ->
            repository.getFavoriteStatus(eventId, onResult)
        }
        val favoriteIds = awaitResult<Set<String>> { onResult ->
            repository.getFavoriteEventIds(onResult)
        }

        assertTrue(favoriteStatus.getOrThrow())
        assertEquals(setOf(eventId), favoriteIds.getOrThrow())

        val unfavorite = awaitResult<Unit> { onResult ->
            repository.unfavorite(eventId, onResult)
        }
        assertTrue(unfavorite.isSuccess)

        val statusAfterRemoval = awaitResult<Boolean> { onResult ->
            repository.getFavoriteStatus(eventId, onResult)
        }
        assertFalse(statusAfterRemoval.getOrThrow())
    }
}
