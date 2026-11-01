package br.com.uri.campushub.viewmodel

object RatingValidator {
    fun isValid(score: Int): Boolean = score in 1..5
}
