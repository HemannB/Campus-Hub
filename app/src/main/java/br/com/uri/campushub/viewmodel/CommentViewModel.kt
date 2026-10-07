package br.com.uri.campushub.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import br.com.uri.campushub.model.Comment
import br.com.uri.campushub.repository.AuthRepository
import br.com.uri.campushub.repository.CommentRepository
import br.com.uri.campushub.repository.UserRepository

sealed class CommentListState {
    object Idle : CommentListState()
    object Loading : CommentListState()
    data class Success(val comments: List<Comment>) : CommentListState()
    data class Error(val message: String) : CommentListState()
}

sealed class CommentActionState {
    object Idle : CommentActionState()
    object Loading : CommentActionState()
    data class Success(val message: String) : CommentActionState()
    data class Error(val message: String) : CommentActionState()
}

class CommentViewModel : ViewModel() {

    private val authRepository = AuthRepository()
    private val commentRepository = CommentRepository()
    private val userRepository = UserRepository()

    private val _commentListState =
        MutableLiveData<CommentListState>(CommentListState.Idle)
    val commentListState: LiveData<CommentListState> = _commentListState

    private val _commentActionState =
        MutableLiveData<CommentActionState>(CommentActionState.Idle)
    val commentActionState: LiveData<CommentActionState> = _commentActionState

    fun currentUserId(): String? {
        return authRepository.currentUserId()
    }

    fun loadComments(eventId: String) {
        _commentListState.value = CommentListState.Loading

        commentRepository.getComments(eventId) { result ->
            _commentListState.value = result.fold(
                onSuccess = { comments ->
                    CommentListState.Success(comments)
                },
                onFailure = { exception ->
                    CommentListState.Error(
                        exception.toUserMessage(
                            "Não foi possível carregar os comentários."
                        )
                    )
                }
            )
        }
    }

    fun createComment(eventId: String, text: String) {
        val userId = authRepository.currentUserId()

        if (userId == null) {
            _commentActionState.value = CommentActionState.Error(
                "Usuário não autenticado."
            )
            return
        }

        _commentActionState.value = CommentActionState.Loading

        userRepository.getProfile(userId) { profileResult ->
            profileResult.fold(
                onSuccess = { user ->
                    commentRepository.createComment(
                        eventId = eventId,
                        authorName = user.name,
                        text = text
                    ) { result ->
                        handleActionResult(
                            eventId = eventId,
                            successMessage = "Comentário publicado.",
                            result = result
                        )
                    }
                },
                onFailure = { exception ->
                    _commentActionState.value = CommentActionState.Error(
                        exception.toUserMessage(
                            "Não foi possível identificar o autor."
                        )
                    )
                }
            )
        }
    }

    fun updateComment(eventId: String, commentId: String, text: String) {
        _commentActionState.value = CommentActionState.Loading

        commentRepository.updateComment(commentId, text) { result ->
            handleActionResult(
                eventId = eventId,
                successMessage = "Comentário atualizado.",
                result = result
            )
        }
    }

    fun deleteComment(eventId: String, commentId: String) {
        _commentActionState.value = CommentActionState.Loading

        commentRepository.deleteComment(commentId) { result ->
            handleActionResult(
                eventId = eventId,
                successMessage = "Comentário excluído.",
                result = result
            )
        }
    }

    fun clearActionState() {
        _commentActionState.value = CommentActionState.Idle
    }

    private fun handleActionResult(
        eventId: String,
        successMessage: String,
        result: Result<Unit>
    ) {
        _commentActionState.value = result.fold(
            onSuccess = {
                loadComments(eventId)
                CommentActionState.Success(successMessage)
            },
            onFailure = { exception ->
                CommentActionState.Error(
                    exception.toUserMessage(
                        "Não foi possível atualizar o comentário."
                    )
                )
            }
        )
    }
}
