package br.com.uri.campushub.integration

import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.uri.campushub.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class AuthRepositoryIntegrationTest {
    private lateinit var auth: FirebaseAuth
    private lateinit var repository: AuthRepository

    @Before
    fun setUp() {
        auth = FirebaseEmulatorConfig.auth()
        repository = AuthRepository(auth)
    }

    @After
    fun tearDown() {
        auth.signOut()
    }

    @Test
    fun registerCreatesAuthenticatedUser() {
        val email = "aluno-${UUID.randomUUID()}@example.com"
        val result = awaitResult<String> { onResult ->
            repository.register(email, "123456", onResult)
        }

        assertTrue(result.isSuccess)
        assertEquals(result.getOrThrow(), auth.currentUser?.uid)
        assertEquals(email, auth.currentUser?.email)
    }

    @Test
    fun loginAuthenticatesExistingUserAndLogoutClearsSession() {
        val email = "aluno-${UUID.randomUUID()}@example.com"
        val password = "123456"
        val registration = awaitResult<String> { onResult ->
            repository.register(email, password, onResult)
        }
        assertTrue(registration.isSuccess)

        repository.logout()
        assertNull(auth.currentUser)

        val login = awaitResult<Unit> { onResult ->
            repository.login(email, password, onResult)
        }

        assertTrue(login.isSuccess)
        assertEquals(email, auth.currentUser?.email)

        repository.logout()
        assertNull(auth.currentUser)
    }

    @Test
    fun resetPasswordAcceptsExistingUserEmail() {
        val email = "aluno-${UUID.randomUUID()}@example.com"
        val registration = awaitResult<String> { onResult ->
            repository.register(email, "123456", onResult)
        }
        assertTrue(registration.isSuccess)
        repository.logout()

        val passwordReset = awaitResult<Unit> { onResult ->
            repository.resetPassword(email, onResult)
        }

        assertTrue(passwordReset.isSuccess)
        assertNull(auth.currentUser)
    }

}
