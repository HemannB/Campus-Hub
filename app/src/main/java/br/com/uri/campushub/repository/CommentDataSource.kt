package br.com.uri.campushub.repository

import br.com.uri.campushub.model.Comment

interface CommentDataSource {
    fun getComments(
        eventId: String,
        onResult: (Result<List<Comment>>) -> Unit
    )

    fun createComment(
        eventId: String,
        authorName: String,
        text: String,
        onResult: (Result<Unit>) -> Unit
    )

    fun updateComment(
        commentId: String,
        text: String,
        onResult: (Result<Unit>) -> Unit
    )

    fun deleteComment(
        commentId: String,
        onResult: (Result<Unit>) -> Unit
    )
}
