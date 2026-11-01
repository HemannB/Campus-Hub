package br.com.uri.campushub.repository

import br.com.uri.campushub.model.RatingSummary

interface RatingDataSource {
    fun getRatingSummary(
        eventId: String,
        onResult: (Result<RatingSummary>) -> Unit
    )

    fun saveRating(
        eventId: String,
        score: Int,
        onResult: (Result<Unit>) -> Unit
    )
}
