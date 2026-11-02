package br.com.uri.campushub.repository

interface FavoriteDataSource {
    fun getFavoriteStatus(
        eventId: String,
        onResult: (Result<Boolean>) -> Unit
    )

    fun favorite(
        eventId: String,
        onResult: (Result<Unit>) -> Unit
    )

    fun unfavorite(
        eventId: String,
        onResult: (Result<Unit>) -> Unit
    )
}
