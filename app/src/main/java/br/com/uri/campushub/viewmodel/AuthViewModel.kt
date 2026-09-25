package br.com.uri.campushub.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import br.com.uri.campushub.repository.AuthRepository

sealed class AuthState {
    object Idle: AuthState()
    object Loading: AuthState()
    object Success: AuthState()
    data class Error(val message: String): AuthState()
}

class AuthViewModel: ViewModel() {
    private val repository = AuthRepository()

    private val _authState = MutableLiveData<AuthState>(AuthState.Idle)
    val authState: LiveData<AuthState> = _authState

    fun login(email: String, password: String) {
        _authState.value = AuthState.Loading

        repository.login(email, password) { result ->
            _authState.value = result.fold(
                onSuccess = {
                    AuthState.Success
                },
                onFailure = { exception ->
                    AuthState.Error(
                        exception.message
                            ?: "Não foi possível realizar o login."
                    )
                }
            )
        }
    }
}