package br.com.uri.campushub.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import br.com.uri.campushub.model.User
import br.com.uri.campushub.repository.AuthRepository
import br.com.uri.campushub.repository.UserRepository

sealed class EditProfileState {
    object Idle : EditProfileState()
    object Loading : EditProfileState()
    data class Ready(val user: User) : EditProfileState()
    object Saving : EditProfileState()
    object Success : EditProfileState()
    data class Error(val message: String) : EditProfileState()
}

class EditProfileViewModel : ViewModel() {

    private val authRepository = AuthRepository()
    private val userRepository = UserRepository()
    private var currentUser: User? = null

    private val _editProfileState =
        MutableLiveData<EditProfileState>(EditProfileState.Idle)
    val editProfileState: LiveData<EditProfileState> = _editProfileState

    fun loadProfile() {
        val userId = authRepository.currentUserId()

        if (userId == null) {
            _editProfileState.value = EditProfileState.Error(
                "Usuário não autenticado."
            )
            return
        }

        _editProfileState.value = EditProfileState.Loading

        userRepository.getProfile(userId) { result ->
            _editProfileState.value = result.fold(
                onSuccess = { user ->
                    currentUser = user
                    EditProfileState.Ready(user)
                },
                onFailure = { exception ->
                    EditProfileState.Error(
                        exception.message
                            ?: "Não foi possível carregar o perfil."
                    )
                }
            )
        }
    }

    fun updateProfile(
        name: String,
        studentId: String,
        course: String,
        semester: String
    ) {
        val user = currentUser

        if (user == null) {
            _editProfileState.value = EditProfileState.Error(
                "Carregue o perfil antes de salvar."
            )
            return
        }

        _editProfileState.value = EditProfileState.Saving

        userRepository.updateProfile(
            userId = user.id,
            name = name,
            studentId = studentId,
            course = course,
            semester = semester
        ) { result ->
            _editProfileState.value = result.fold(
                onSuccess = {
                    currentUser = user.copy(
                        name = name,
                        studentId = studentId,
                        course = course,
                        semester = semester
                    )
                    EditProfileState.Success
                },
                onFailure = { exception ->
                    EditProfileState.Error(
                        exception.message
                            ?: "Não foi possível atualizar o perfil."
                    )
                }
            )
        }
    }
}
