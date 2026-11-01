package br.com.uri.campushub.viewmodel

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RatingValidatorTest {

    @Test
    fun `minimum and maximum scores are valid`() {
        assertTrue(RatingValidator.isValid(1))
        assertTrue(RatingValidator.isValid(5))
    }

    @Test
    fun `score below minimum is invalid`() {
        assertFalse(RatingValidator.isValid(0))
    }

    @Test
    fun `score above maximum is invalid`() {
        assertFalse(RatingValidator.isValid(6))
    }
}
