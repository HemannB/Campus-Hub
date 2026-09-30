package br.com.uri.campushub.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import br.com.uri.campushub.model.User
import br.com.uri.campushub.repository.AuthRepository
import br.com.uri.campushub.repository.UserRepository

sealed class RegisterState {
    object Idle : RegisterState()
    object Loading : RegisterState()
    object Success : RegisterState()
    data class Error(val message: String) : RegisterState()
}

class RegisterViewModel : ViewModel() {

    private val authRepository = AuthRepository()
    private val userRepository = UserRepository()

    private val _registerState = MutableLiveData<RegisterState>(RegisterState.Idle)
    val registerState: LiveData<RegisterState> = _registerState

    fun register(
        name: String,
        email: String,
        studentId: String,
        course: String,
        password: String
    ) {
        _registerState.value = RegisterState.Loading

        authRepository.register(email, password) { authResult ->
            authResult.fold(
                onSuccess = { userId ->
                    createProfile(
                        User(
                            id = userId,
                            name = name,
                            email = email,
                            studentId = studentId,
                            course = course,
                            role = "STUDENT"
                        )
                    )
                },
                onFailure = { exception ->
                    _registerState.value = RegisterState.Error(
                        exception.message ?: "Não foi possível criar a conta."
                    )
                }
            )
        }
    }

    private fun createProfile(user: User) {
        userRepository.createProfile(user) { profileResult ->
            profileResult.fold(
                onSuccess = {
                    _registerState.value = RegisterState.Success
                },
                onFailure = { exception ->
                    authRepository.rollbackRegistration {
                        _registerState.value = RegisterState.Error(
                            exception.message ?: "Não foi possível salvar o perfil."
                        )
                    }
                }
            )
        }
    }

    fun resetState() {
        _registerState.value = RegisterState.Idle
    }
}
