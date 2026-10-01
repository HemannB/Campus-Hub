package br.com.uri.campushub.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import br.com.uri.campushub.repository.AuthRepository

sealed class PasswordResetState {
    object Idle : PasswordResetState()
    object Loading : PasswordResetState()
    object Success : PasswordResetState()
    data class Error(val message: String) : PasswordResetState()
}

class PasswordResetViewModel : ViewModel() {

    private val repository = AuthRepository()

    private val _resetState = MutableLiveData<PasswordResetState>(PasswordResetState.Idle)
    val resetState: LiveData<PasswordResetState> = _resetState

    fun resetPassword(email: String) {
        _resetState.value = PasswordResetState.Loading

        repository.resetPassword(email) { result ->
            _resetState.value = result.fold(
                onSuccess = {
                    PasswordResetState.Success
                },
                onFailure = {
                    PasswordResetState.Error(
                        "Não foi possível enviar o e-mail. Tente novamente."
                    )
                }
            )
        }
    }

    fun clearState() {
        _resetState.value = PasswordResetState.Idle
    }
}
