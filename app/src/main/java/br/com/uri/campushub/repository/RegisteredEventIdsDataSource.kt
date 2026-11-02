package br.com.uri.campushub.repository

interface RegisteredEventIdsDataSource {
    fun getRegisteredEventIds(
        onResult: (Result<Set<String>>) -> Unit
    )
}
