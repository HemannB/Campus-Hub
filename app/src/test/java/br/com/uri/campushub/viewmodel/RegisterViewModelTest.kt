package br.com.uri.campushub.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import br.com.uri.campushub.model.User
import br.com.uri.campushub.repository.AccountRegistrationDataSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class RegisterViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var authRepository: FakeAccountRegistrationDataSource
    private lateinit var userRepository: FakeUserDataSource
    private lateinit var viewModel: RegisterViewModel

    @Before
    fun setUp() {
        authRepository = FakeAccountRegistrationDataSource()
        userRepository = FakeUserDataSource()
        viewModel = RegisterViewModel(authRepository, userRepository)
    }

    @Test
    fun `register creates account profile and succeeds`() {
        registerUser()

        assertSame(RegisterState.Loading, viewModel.registerState.value)
        assertEquals("bruno@uri.edu.br", authRepository.receivedEmail)
        assertEquals("123456", authRepository.receivedPassword)

        authRepository.completeRegistration(Result.success("user-1"))

        assertEquals(
            User(
                id = "user-1",
                name = "Bruno",
                email = "bruno@uri.edu.br",
                studentId = "123",
                course = "Computação"
            ),
            userRepository.createdUser
        )

        userRepository.completeCreate(Result.success(Unit))

        assertSame(RegisterState.Success, viewModel.registerState.value)
    }

    @Test
    fun `account creation failure exposes repository message`() {
        registerUser()

        authRepository.completeRegistration(Result.failure(Exception("E-mail já cadastrado.")))

        assertEquals(
            RegisterState.Error("E-mail já cadastrado."),
            viewModel.registerState.value
        )
        assertEquals(null, userRepository.createdUser)
    }

    @Test
    fun `profile creation failure rolls account back`() {
        registerUser()
        authRepository.completeRegistration(Result.success("user-1"))

        userRepository.completeCreate(Result.failure(Exception("Falha ao salvar perfil.")))

        assertTrue(authRepository.rollbackCalled)
        assertSame(RegisterState.Loading, viewModel.registerState.value)

        authRepository.completeRollback()

        assertEquals(
            RegisterState.Error("Falha ao salvar perfil."),
            viewModel.registerState.value
        )
    }

    @Test
    fun `reset state returns to idle`() {
        registerUser()

        viewModel.resetState()

        assertSame(RegisterState.Idle, viewModel.registerState.value)
    }

    private fun registerUser() {
        viewModel.register(
            name = "Bruno",
            email = "bruno@uri.edu.br",
            studentId = "123",
            course = "Computação",
            password = "123456"
        )
    }

    private class FakeAccountRegistrationDataSource : AccountRegistrationDataSource {
        var receivedEmail: String? = null
        var receivedPassword: String? = null
        var rollbackCalled = false
        private var registrationResult: ((Result<String>) -> Unit)? = null
        private var rollbackComplete: (() -> Unit)? = null

        override fun register(
            email: String,
            password: String,
            onResult: (Result<String>) -> Unit
        ) {
            receivedEmail = email
            receivedPassword = password
            registrationResult = onResult
        }

        override fun rollbackRegistration(onComplete: () -> Unit) {
            rollbackCalled = true
            rollbackComplete = onComplete
        }

        fun completeRegistration(result: Result<String>) {
            checkNotNull(registrationResult)(result)
        }

        fun completeRollback() {
            checkNotNull(rollbackComplete)()
        }
    }
}
