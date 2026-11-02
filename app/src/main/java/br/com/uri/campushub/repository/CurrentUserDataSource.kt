package br.com.uri.campushub.repository

interface CurrentUserDataSource {
    fun currentUserId(): String?
}
