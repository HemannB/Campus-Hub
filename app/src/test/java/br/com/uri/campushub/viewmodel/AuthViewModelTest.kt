package br.com.uri.campushub.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import br.com.uri.campushub.repository.AuthDataSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class AuthViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var repository: FakeAuthDataSource
    private lateinit var viewModel: AuthViewModel

    @Before
    fun setUp() {
        repository = FakeAuthDataSource()
        viewModel = AuthViewModel(repository)
    }

    @Test
    fun `initial state is idle`() {
        assertSame(AuthState.Idle, viewModel.authState.value)
    }

    @Test
    fun `login changes from loading to success`() {
        viewModel.login("aluno@uri.edu.br", "123456")

        assertSame(AuthState.Loading, viewModel.authState.value)
        assertEquals("aluno@uri.edu.br", repository.receivedEmail)
        assertEquals("123456", repository.receivedPassword)

        repository.completeLogin(Result.success(Unit))

        assertSame(AuthState.Success, viewModel.authState.value)
    }

    @Test
    fun `failed login exposes error message`() {
        viewModel.login("aluno@uri.edu.br", "senha-errada")

        repository.completeLogin(Result.failure(Exception("Login recusado.")))

        val state = viewModel.authState.value as AuthState.Error
        assertEquals("Login recusado.", state.message)
    }

    @Test
    fun `is logged in returns repository state`() {
        assertFalse(viewModel.isLoggedIn())

        repository.loggedIn = true

        assertTrue(viewModel.isLoggedIn())
    }

    @Test
    fun `logout delegates to repository`() {
        viewModel.logout()

        assertTrue(repository.logoutCalled)
    }

    private class FakeAuthDataSource : AuthDataSource {
        var loggedIn = false
        var logoutCalled = false
        var receivedEmail: String? = null
        var receivedPassword: String? = null
        private var loginResult: ((Result<Unit>) -> Unit)? = null

        override fun isLoggedIn(): Boolean = loggedIn

        override fun login(
            email: String,
            password: String,
            onResult: (Result<Unit>) -> Unit
        ) {
            receivedEmail = email
            receivedPassword = password
            loginResult = onResult
        }

        override fun logout() {
            logoutCalled = true
        }

        fun completeLogin(result: Result<Unit>) {
            checkNotNull(loginResult)(result)
        }
    }
}
