package br.com.uri.campushub.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import br.com.uri.campushub.model.User
import br.com.uri.campushub.model.UserRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class SessionViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var authRepository: FakeCurrentUserDataSource
    private lateinit var userRepository: FakeUserDataSource
    private lateinit var viewModel: SessionViewModel

    @Before
    fun setUp() {
        authRepository = FakeCurrentUserDataSource()
        userRepository = FakeUserDataSource()
        viewModel = SessionViewModel(authRepository, userRepository)
    }

    @Test
    fun `user without session is unauthenticated`() {
        authRepository.userId = null

        viewModel.loadSession()

        assertSame(SessionState.Unauthenticated, viewModel.sessionState.value)
    }

    @Test
    fun `student profile returns student role`() {
        viewModel.loadSession()

        assertSame(SessionState.Loading, viewModel.sessionState.value)
        assertEquals("user-1", userRepository.requestedUserId)

        userRepository.completeProfile(
            Result.success(User(id = "user-1"))
        )

        val state = viewModel.sessionState.value as SessionState.Authenticated
        assertEquals(UserRole.STUDENT, state.role)
    }

    @Test
    fun `organizer profile returns organizer role`() {
        viewModel.loadSession()
        userRepository.completeProfile(
            Result.success(
                User(id = "user-1", role = UserRole.ORGANIZER.name)
            )
        )

        val state = viewModel.sessionState.value as SessionState.Authenticated
        assertEquals(UserRole.ORGANIZER, state.role)
    }

    @Test
    fun `profile failure returns error`() {
        viewModel.loadSession()
        userRepository.completeProfile(
            Result.failure(Exception("Perfil indisponível."))
        )

        val state = viewModel.sessionState.value as SessionState.Error
        assertEquals("Perfil indisponível.", state.message)
    }
}
