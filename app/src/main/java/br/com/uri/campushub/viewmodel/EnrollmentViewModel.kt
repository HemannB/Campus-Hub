package br.com.uri.campushub.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import br.com.uri.campushub.model.Registration
import br.com.uri.campushub.repository.EnrollmentDataSource
import br.com.uri.campushub.repository.RegistrationRepository

sealed class EnrollmentState {
    object Loading : EnrollmentState()
    data class Ready(val registration: Registration?) : EnrollmentState()
    data class Error(val message: String) : EnrollmentState()
}

class EnrollmentViewModel(
    private val repository: EnrollmentDataSource = RegistrationRepository()
) : ViewModel() {
    private val _state = MutableLiveData<EnrollmentState>(EnrollmentState.Loading)
    val state: LiveData<EnrollmentState> = _state
    private var updating = false

    fun load(eventId: String) {
        _state.value = EnrollmentState.Loading
        repository.getRegistration(eventId) { result ->
            _state.value = result.fold(
                onSuccess = { EnrollmentState.Ready(it) },
                onFailure = { EnrollmentState.Error(it.toUserMessage("Não foi possível consultar a inscrição.")) }
            )
        }
    }

    fun join(eventId: String) = update(eventId) { repository.register(eventId, it) }
    fun cancel(eventId: String) = update(eventId) { repository.cancel(eventId, it) }
    fun confirm(eventId: String) = update(eventId) { repository.confirmInvitation(eventId, it) }

    private fun update(eventId: String, operation: ((Result<Unit>) -> Unit) -> Unit) {
        if (updating) return
        updating = true
        _state.value = EnrollmentState.Loading
        operation { result ->
            updating = false
            result.fold(onSuccess = { load(eventId) },
                onFailure = { _state.value = EnrollmentState.Error(it.toUserMessage("Não foi possível atualizar a inscrição.")) })
        }
    }
}
