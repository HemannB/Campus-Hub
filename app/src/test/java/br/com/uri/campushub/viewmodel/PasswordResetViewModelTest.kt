package br.com.uri.campushub.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import br.com.uri.campushub.repository.PasswordResetDataSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class PasswordResetViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var repository: FakePasswordResetDataSource
    private lateinit var viewModel: PasswordResetViewModel

    @Before
    fun setUp() {
        repository = FakePasswordResetDataSource()
        viewModel = PasswordResetViewModel(repository)
    }

    @Test
    fun `reset password changes from loading to success`() {
        viewModel.resetPassword("aluno@uri.edu.br")

        assertSame(PasswordResetState.Loading, viewModel.resetState.value)
        assertEquals("aluno@uri.edu.br", repository.receivedEmail)

        repository.complete(Result.success(Unit))

        assertSame(PasswordResetState.Success, viewModel.resetState.value)
    }

    @Test
    fun `reset password failure exposes repository message`() {
        viewModel.resetPassword("aluno@uri.edu.br")

        repository.complete(Result.failure(Exception("E-mail não encontrado.")))

        assertEquals(
            PasswordResetState.Error("E-mail não encontrado."),
            viewModel.resetState.value
        )
    }

    @Test
    fun `clear state returns to idle`() {
        viewModel.resetPassword("aluno@uri.edu.br")

        viewModel.clearState()

        assertSame(PasswordResetState.Idle, viewModel.resetState.value)
    }

    private class FakePasswordResetDataSource : PasswordResetDataSource {
        var receivedEmail: String? = null
        private var resetResult: ((Result<Unit>) -> Unit)? = null

        override fun resetPassword(
            email: String,
            onResult: (Result<Unit>) -> Unit
        ) {
            receivedEmail = email
            resetResult = onResult
        }

        fun complete(result: Result<Unit>) {
            checkNotNull(resetResult)(result)
        }
    }
}
