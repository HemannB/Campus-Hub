package br.com.uri.campushub.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import br.com.uri.campushub.repository.RegistrationDataSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class RegistrationViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var repository: FakeRegistrationDataSource
    private lateinit var viewModel: RegistrationViewModel

    @Before
    fun setUp() {
        repository = FakeRegistrationDataSource()
        viewModel = RegistrationViewModel(repository)
    }

    @Test
    fun `load status changes from loading to status`() {
        viewModel.loadStatus("event-1")

        assertSame(RegistrationState.Loading, viewModel.registrationState.value)
        assertEquals("event-1", repository.statusEventId)

        repository.completeStatus(Result.success(true))

        assertEquals(RegistrationState.Status(true), viewModel.registrationState.value)
    }

    @Test
    fun `load status failure exposes repository message`() {
        viewModel.loadStatus("event-1")

        repository.completeStatus(Result.failure(Exception("Consulta indisponível.")))

        assertEquals(
            RegistrationState.Error("Consulta indisponível.", null),
            viewModel.registrationState.value
        )
    }

    @Test
    fun `toggle registers and then cancels registration`() {
        loadStatus(false)

        viewModel.toggleRegistration("event-1")

        assertEquals(RegistrationState.Updating(false), viewModel.registrationState.value)
        assertEquals("event-1", repository.registeredEventId)
        repository.completeOperation(Result.success(Unit))
        assertEquals(
            RegistrationState.Success(true, "Inscrição realizada com sucesso."),
            viewModel.registrationState.value
        )

        viewModel.toggleRegistration("event-1")

        assertEquals("event-1", repository.cancelledEventId)
        repository.completeOperation(Result.success(Unit))
        assertEquals(
            RegistrationState.Success(false, "Inscrição cancelada."),
            viewModel.registrationState.value
        )
    }

    @Test
    fun `toggle failure preserves current status`() {
        loadStatus(false)

        viewModel.toggleRegistration("event-1")
        repository.completeOperation(Result.failure(Exception("Evento lotado.")))

        assertEquals(
            RegistrationState.Error("Evento lotado.", false),
            viewModel.registrationState.value
        )
    }

    private fun loadStatus(isRegistered: Boolean) {
        viewModel.loadStatus("event-1")
        repository.completeStatus(Result.success(isRegistered))
    }

    private class FakeRegistrationDataSource : RegistrationDataSource {
        var statusEventId: String? = null
        var registeredEventId: String? = null
        var cancelledEventId: String? = null
        private var statusResult: ((Result<Boolean>) -> Unit)? = null
        private var operationResult: ((Result<Unit>) -> Unit)? = null

        override fun getRegistrationStatus(
            eventId: String,
            onResult: (Result<Boolean>) -> Unit
        ) {
            statusEventId = eventId
            statusResult = onResult
        }

        override fun register(
            eventId: String,
            onResult: (Result<Unit>) -> Unit
        ) {
            registeredEventId = eventId
            operationResult = onResult
        }

        override fun cancel(
            eventId: String,
            onResult: (Result<Unit>) -> Unit
        ) {
            cancelledEventId = eventId
            operationResult = onResult
        }

        fun completeStatus(result: Result<Boolean>) {
            checkNotNull(statusResult)(result)
        }

        fun completeOperation(result: Result<Unit>) {
            checkNotNull(operationResult)(result)
        }
    }
}
