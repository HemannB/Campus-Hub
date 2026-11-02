package br.com.uri.campushub.repository

interface FavoriteEventIdsDataSource {
    fun getFavoriteEventIds(
        onResult: (Result<Set<String>>) -> Unit
    )
}
