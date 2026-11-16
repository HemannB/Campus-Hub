package br.com.uri.campushub.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import br.com.uri.campushub.model.Registration
import br.com.uri.campushub.repository.EnrollmentDataSource
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class EnrollmentViewModelTest {
    @get:Rule val rule = InstantTaskExecutorRule()

    @Test fun loadExposesRegistrationIncludingWaitlistAndError() {
        val repo = FakeEnrollment()
        val vm = EnrollmentViewModel(repo)
        vm.load("event")
        assertSame(EnrollmentState.Loading, vm.state.value)
        val registration = Registration(status = "WAITLISTED")
        repo.read(Result.success(registration))
        assertEquals(EnrollmentState.Ready(registration), vm.state.value)
        vm.load("event")
        repo.read(Result.failure(Exception("Sem conexão.")))
        assertEquals(EnrollmentState.Error("Sem conexão."), vm.state.value)
    }

    @Test fun joinIgnoresDuplicateClickAndReloadsActualStatus() {
        val repo = FakeEnrollment()
        val vm = EnrollmentViewModel(repo)
        vm.join("event")
        vm.join("event")
        assertEquals(1, repo.joinCalls)
        repo.operation(Result.success(Unit))
        repo.read(Result.success(Registration(status = "WAITLISTED")))
        assertEquals(EnrollmentState.Ready(Registration(status = "WAITLISTED")), vm.state.value)
    }

    @Test fun confirmationAndCancellationUseTheirOperationsAndAllowRetry() {
        val repo = FakeEnrollment()
        val vm = EnrollmentViewModel(repo)
        vm.confirm("event")
        assertEquals("confirm", repo.lastOperation)
        repo.operation(Result.failure(Exception("Sem reserva.")))
        assertEquals(EnrollmentState.Error("Sem reserva."), vm.state.value)
        vm.cancel("event")
        assertEquals("cancel", repo.lastOperation)
        repo.operation(Result.success(Unit))
        repo.read(Result.success(null))
        assertEquals(EnrollmentState.Ready(null), vm.state.value)
    }

    private class FakeEnrollment : EnrollmentDataSource {
        lateinit var read: (Result<Registration?>) -> Unit
        lateinit var operation: (Result<Unit>) -> Unit
        var joinCalls = 0
        var lastOperation = ""
        override fun getRegistration(eventId: String, onResult: (Result<Registration?>) -> Unit) { read = onResult }
        override fun register(eventId: String, onResult: (Result<Unit>) -> Unit) {
            joinCalls++; operation = onResult
        }
        override fun cancel(eventId: String, onResult: (Result<Unit>) -> Unit) {
            lastOperation = "cancel"; operation = onResult
        }
        override fun confirmInvitation(eventId: String, onResult: (Result<Unit>) -> Unit) {
            lastOperation = "confirm"; operation = onResult
        }
    }
}
