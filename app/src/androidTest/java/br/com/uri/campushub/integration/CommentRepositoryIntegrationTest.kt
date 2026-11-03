package br.com.uri.campushub.integration

import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.uri.campushub.repository.CommentRepository
import com.google.firebase.auth.FirebaseAuth
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class CommentRepositoryIntegrationTest {

    private lateinit var auth: FirebaseAuth
    private lateinit var repository: CommentRepository
    private lateinit var ownerEmail: String

    @Before
    fun setUp() {
        auth = FirebaseEmulatorConfig.auth()
        auth.signOut()
        ownerEmail = "comment-owner-${UUID.randomUUID()}@example.com"
        awaitTask(auth.createUserWithEmailAndPassword(ownerEmail, "123456"))
        repository = CommentRepository(
            auth,
            FirebaseEmulatorConfig.firestore()
        )
    }

    @After
    fun tearDown() {
        auth.signOut()
    }

    @Test
    fun commentCrudPreservesAuthorAndPreventsAnotherUserFromDeleting() {
        val eventId = "event-${UUID.randomUUID()}"
        val creation = awaitResult<Unit> { onResult ->
            repository.createComment(
                eventId,
                "Bruno",
                "Comentário inicial",
                onResult
            )
        }
        assertTrue(creation.isSuccess)

        val createdComment = awaitResult { onResult ->
            repository.getComments(eventId, onResult)
        }.getOrThrow().single()

        assertEquals("Bruno", createdComment.authorName)
        assertEquals("Comentário inicial", createdComment.text)
        assertEquals(auth.currentUser?.uid, createdComment.userId)
        assertNotNull(createdComment.createdAt)

        val update = awaitResult<Unit> { onResult ->
            repository.updateComment(
                createdComment.id,
                "Comentário editado",
                onResult
            )
        }
        assertTrue(update.isSuccess)

        val updatedComment = awaitResult { onResult ->
            repository.getComments(eventId, onResult)
        }.getOrThrow().single()
        assertEquals("Comentário editado", updatedComment.text)
        assertNotNull(updatedComment.updatedAt)

        auth.signOut()
        awaitTask(
            auth.createUserWithEmailAndPassword(
                "comment-other-${UUID.randomUUID()}@example.com",
                "123456"
            )
        )
        val unauthorizedDeletion = awaitResult<Unit> { onResult ->
            repository.deleteComment(createdComment.id, onResult)
        }
        assertTrue(unauthorizedDeletion.isFailure)

        auth.signOut()
        awaitTask(auth.signInWithEmailAndPassword(ownerEmail, "123456"))
        val deletion = awaitResult<Unit> { onResult ->
            repository.deleteComment(createdComment.id, onResult)
        }
        assertTrue(deletion.isSuccess)

        val remainingComments = awaitResult { onResult ->
            repository.getComments(eventId, onResult)
        }.getOrThrow()
        assertTrue(remainingComments.isEmpty())
    }
}
