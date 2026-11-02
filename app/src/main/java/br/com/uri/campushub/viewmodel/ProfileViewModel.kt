package br.com.uri.campushub.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import br.com.uri.campushub.model.User
import br.com.uri.campushub.repository.AuthRepository
import br.com.uri.campushub.repository.CurrentUserDataSource
import br.com.uri.campushub.repository.UserDataSource
import br.com.uri.campushub.repository.UserRepository

sealed class ProfileState {
    object Idle : ProfileState()
    object Loading : ProfileState()
    data class Success(val user: User) : ProfileState()
    data class Error(val message: String) : ProfileState()
}

class ProfileViewModel(
    private val authRepository: CurrentUserDataSource = AuthRepository(),
    private val userRepository: UserDataSource = UserRepository()
) : ViewModel() {

    private val _profileState =
        MutableLiveData<ProfileState>(ProfileState.Idle)

    val profileState: LiveData<ProfileState> = _profileState

    fun loadProfile() {
        _profileState.value = ProfileState.Loading
        val userId = authRepository.currentUserId()
        if (userId == null) {
            _profileState.value = ProfileState.Error(
                "Usuário não autenticado."
            )
            return
        }

        userRepository.getProfile(userId) { result ->
            _profileState.value = result.fold(
                onSuccess = { user ->
                    ProfileState.Success(user)
                },
                onFailure = { exception ->
                    ProfileState.Error(
                        exception.toUserMessage(
                            "Não foi possível carregar o perfil."
                        )
                    )
                }
            )
        }
    }

}
