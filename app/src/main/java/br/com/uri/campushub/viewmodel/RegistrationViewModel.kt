package br.com.uri.campushub.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import br.com.uri.campushub.repository.RegistrationRepository

sealed class RegistrationState {
    object Idle : RegistrationState()
    object Loading : RegistrationState()
    data class Status(val isRegistered: Boolean) : RegistrationState()
    data class Updating(val isRegistered: Boolean) : RegistrationState()
    data class Success(
        val isRegistered: Boolean,
        val message: String
    ) : RegistrationState()
    data class Error(
        val message: String,
        val isRegistered: Boolean?
    ) : RegistrationState()
}

class RegistrationViewModel : ViewModel() {

    private val repository = RegistrationRepository()
    private var currentStatus: Boolean? = null

    private val _registrationState =
        MutableLiveData<RegistrationState>(RegistrationState.Idle)
    val registrationState: LiveData<RegistrationState> = _registrationState

    fun loadStatus(eventId: String) {
        _registrationState.value = RegistrationState.Loading

        repository.getRegistrationStatus(eventId) { result ->
            _registrationState.value = result.fold(
                onSuccess = { isRegistered ->
                    currentStatus = isRegistered
                    RegistrationState.Status(isRegistered)
                },
                onFailure = { exception ->
                    RegistrationState.Error(
                        message = exception.toUserMessage(
                            "Não foi possível consultar a inscrição."
                        ),
                        isRegistered = currentStatus
                    )
                }
            )
        }
    }

    fun toggleRegistration(eventId: String) {
        val isRegistered = currentStatus ?: return
        _registrationState.value = RegistrationState.Updating(isRegistered)

        val onResult: (Result<Unit>) -> Unit = { result ->
            _registrationState.value = result.fold(
                onSuccess = {
                    currentStatus = !isRegistered
                    RegistrationState.Success(
                        isRegistered = !isRegistered,
                        message = if (isRegistered) {
                            "Inscrição cancelada."
                        } else {
                            "Inscrição realizada com sucesso."
                        }
                    )
                },
                onFailure = { exception ->
                    RegistrationState.Error(
                        message = exception.toUserMessage(
                            "Não foi possível atualizar a inscrição."
                        ),
                        isRegistered = isRegistered
                    )
                }
            )
        }

        if (isRegistered) {
            repository.cancel(eventId, onResult)
        } else {
            repository.register(eventId, onResult)
        }
    }
}
