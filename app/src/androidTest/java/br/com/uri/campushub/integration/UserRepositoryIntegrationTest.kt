package br.com.uri.campushub.integration

import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.uri.campushub.model.User
import br.com.uri.campushub.repository.UserRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class UserRepositoryIntegrationTest {

    private lateinit var repository: UserRepository

    @Before
    fun setUp() {
        repository = UserRepository(FirebaseEmulatorConfig.firestore())
    }

    @Test
    fun createAndReadProfilePersistsAllFields() {
        val user = User(
            id = "user-${UUID.randomUUID()}",
            name = "Bruno",
            email = "bruno@uri.edu.br",
            studentId = "12345",
            course = "Ciência da Computação",
            semester = "6",
            role = "STUDENT"
        )

        val creation = awaitResult<Unit> { onResult ->
            repository.createProfile(user, onResult)
        }
        assertTrue(creation.isSuccess)

        val profile = awaitResult<User> { onResult ->
            repository.getProfile(user.id, onResult)
        }

        assertTrue(profile.isSuccess)
        assertEquals(user, profile.getOrThrow())
    }

    @Test
    fun updateProfilePersistsChangedFields() {
        val originalUser = User(
            id = "user-${UUID.randomUUID()}",
            name = "Nome antigo",
            email = "bruno@uri.edu.br",
            studentId = "11111",
            course = "Sistemas de Informação",
            semester = "5",
            role = "STUDENT"
        )
        val creation = awaitResult<Unit> { onResult ->
            repository.createProfile(originalUser, onResult)
        }
        assertTrue(creation.isSuccess)

        val update = awaitResult<Unit> { onResult ->
            repository.updateProfile(
                userId = originalUser.id,
                name = "Bruno",
                studentId = "12345",
                course = "Ciência da Computação",
                semester = "6",
                onResult = onResult
            )
        }
        assertTrue(update.isSuccess)

        val profile = awaitResult<User> { onResult ->
            repository.getProfile(originalUser.id, onResult)
        }

        assertEquals(
            originalUser.copy(
                name = "Bruno",
                studentId = "12345",
                course = "Ciência da Computação",
                semester = "6"
            ),
            profile.getOrThrow()
        )
    }
}
