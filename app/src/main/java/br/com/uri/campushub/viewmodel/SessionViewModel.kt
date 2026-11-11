package br.com.uri.campushub.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import br.com.uri.campushub.model.UserRole
import br.com.uri.campushub.model.userRole
import br.com.uri.campushub.repository.AuthRepository
import br.com.uri.campushub.repository.CurrentUserDataSource
import br.com.uri.campushub.repository.UserDataSource
import br.com.uri.campushub.repository.UserRepository

sealed class SessionState {
    object Idle : SessionState()
    object Loading : SessionState()
    object Unauthenticated : SessionState()
    data class Authenticated(val role: UserRole) : SessionState()
    data class Error(val message: String) : SessionState()
}

class SessionViewModel(
    private val authRepository: CurrentUserDataSource = AuthRepository(),
    private val userRepository: UserDataSource = UserRepository()
) : ViewModel() {

    private val _sessionState = MutableLiveData<SessionState>(SessionState.Idle)
    val sessionState: LiveData<SessionState> = _sessionState

    fun loadSession() {
        val userId = authRepository.currentUserId()

        if (userId == null) {
            _sessionState.value = SessionState.Unauthenticated
            return
        }

        _sessionState.value = SessionState.Loading
        userRepository.getProfile(userId) { result ->
            _sessionState.value = result.fold(
                onSuccess = { user ->
                    SessionState.Authenticated(user.userRole)
                },
                onFailure = { exception ->
                    SessionState.Error(
                        exception.toUserMessage(
                            "Não foi possível carregar seu perfil."
                        )
                    )
                }
            )
        }
    }
}
