package br.com.uri.campushub.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import br.com.uri.campushub.model.Comment
import br.com.uri.campushub.model.User
import br.com.uri.campushub.repository.CommentDataSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class CommentViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var authRepository: FakeCurrentUserDataSource
    private lateinit var commentRepository: FakeCommentDataSource
    private lateinit var userRepository: FakeUserDataSource
    private lateinit var viewModel: CommentViewModel

    @Before
    fun setUp() {
        authRepository = FakeCurrentUserDataSource()
        commentRepository = FakeCommentDataSource()
        userRepository = FakeUserDataSource()
        viewModel = CommentViewModel(authRepository, commentRepository, userRepository)
    }

    @Test
    fun `current user id comes from authentication`() {
        assertEquals("user-1", viewModel.currentUserId())
    }

    @Test
    fun `load comments exposes success and failure`() {
        val comments = listOf(Comment(id = "comment-1", text = "Muito bom"))

        viewModel.loadComments("event-1")
        assertSame(CommentListState.Loading, viewModel.commentListState.value)
        commentRepository.completeComments(Result.success(comments))
        assertEquals(CommentListState.Success(comments), viewModel.commentListState.value)

        viewModel.loadComments("event-1")
        commentRepository.completeComments(Result.failure(Exception("Falha ao carregar.")))
        assertEquals(
            CommentListState.Error("Falha ao carregar."),
            viewModel.commentListState.value
        )
    }

    @Test
    fun `create comment rejects unauthenticated user`() {
        authRepository.userId = null

        viewModel.createComment("event-1", "Comentário")

        assertEquals(
            CommentActionState.Error("Usuário não autenticado."),
            viewModel.commentActionState.value
        )
        assertEquals(null, userRepository.requestedUserId)
    }

    @Test
    fun `create comment uses profile name and reloads list`() {
        viewModel.createComment("event-1", "Comentário")

        assertSame(CommentActionState.Loading, viewModel.commentActionState.value)
        userRepository.completeProfile(Result.success(User(id = "user-1", name = "Bruno")))
        assertEquals(
            FakeCommentDataSource.CreateRequest("event-1", "Bruno", "Comentário"),
            commentRepository.createRequest
        )

        commentRepository.completeAction(Result.success(Unit))

        assertEquals(
            CommentActionState.Success("Comentário publicado."),
            viewModel.commentActionState.value
        )
        assertEquals("event-1", commentRepository.commentsEventId)
    }

    @Test
    fun `update comment succeeds and reloads list`() {
        viewModel.updateComment("event-1", "comment-1", "Texto atualizado")

        assertEquals("comment-1", commentRepository.updatedCommentId)
        assertEquals("Texto atualizado", commentRepository.updatedText)
        commentRepository.completeAction(Result.success(Unit))

        assertEquals(
            CommentActionState.Success("Comentário atualizado."),
            viewModel.commentActionState.value
        )
        assertEquals("event-1", commentRepository.commentsEventId)
    }

    @Test
    fun `delete failure is exposed and action can be cleared`() {
        viewModel.deleteComment("event-1", "comment-1")

        assertEquals("comment-1", commentRepository.deletedCommentId)
        commentRepository.completeAction(Result.failure(Exception("Exclusão negada.")))
        assertEquals(
            CommentActionState.Error("Exclusão negada."),
            viewModel.commentActionState.value
        )

        viewModel.clearActionState()

        assertSame(CommentActionState.Idle, viewModel.commentActionState.value)
    }

    private class FakeCommentDataSource : CommentDataSource {
        data class CreateRequest(
            val eventId: String,
            val authorName: String,
            val text: String
        )

        var commentsEventId: String? = null
        var createRequest: CreateRequest? = null
        var updatedCommentId: String? = null
        var updatedText: String? = null
        var deletedCommentId: String? = null
        private var commentsResult: ((Result<List<Comment>>) -> Unit)? = null
        private var actionResult: ((Result<Unit>) -> Unit)? = null

        override fun getComments(
            eventId: String,
            onResult: (Result<List<Comment>>) -> Unit
        ) {
            commentsEventId = eventId
            commentsResult = onResult
        }

        override fun createComment(
            eventId: String,
            authorName: String,
            text: String,
            onResult: (Result<Unit>) -> Unit
        ) {
            createRequest = CreateRequest(eventId, authorName, text)
            actionResult = onResult
        }

        override fun updateComment(
            commentId: String,
            text: String,
            onResult: (Result<Unit>) -> Unit
        ) {
            updatedCommentId = commentId
            updatedText = text
            actionResult = onResult
        }

        override fun deleteComment(
            commentId: String,
            onResult: (Result<Unit>) -> Unit
        ) {
            deletedCommentId = commentId
            actionResult = onResult
        }

        fun completeComments(result: Result<List<Comment>>) {
            checkNotNull(commentsResult)(result)
        }

        fun completeAction(result: Result<Unit>) {
            checkNotNull(actionResult)(result)
        }
    }
}
