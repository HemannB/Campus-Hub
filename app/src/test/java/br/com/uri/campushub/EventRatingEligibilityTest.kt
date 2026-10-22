package br.com.uri.campushub

import br.com.uri.campushub.model.Event
import br.com.uri.campushub.model.EventRatingEligibility
import com.google.firebase.Timestamp
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EventRatingEligibilityTest {

    private val startAt = Timestamp(1_000, 0)
    private val endAt = Timestamp(2_000, 0)

    @Test
    fun `event cannot be rated before it ends`() {
        val event = Event(startAt = startAt, endAt = endAt)

        assertFalse(EventRatingEligibility.hasEnded(event, Timestamp(1_999, 999_999_999)))
    }

    @Test
    fun `event can be rated exactly when it ends`() {
        val event = Event(startAt = startAt, endAt = endAt)

        assertTrue(EventRatingEligibility.hasEnded(event, endAt))
    }

    @Test
    fun `event can be rated after it ends`() {
        val event = Event(startAt = startAt, endAt = endAt)

        assertTrue(EventRatingEligibility.hasEnded(event, Timestamp(2_001, 0)))
    }

    @Test
    fun `event without an end date cannot be rated`() {
        val event = Event(startAt = startAt, endAt = null)

        assertFalse(EventRatingEligibility.hasEnded(event, Timestamp(3_000, 0)))
    }

    @Test
    fun `event without a start date cannot be rated`() {
        val event = Event(startAt = null, endAt = endAt)

        assertFalse(EventRatingEligibility.hasEnded(event, Timestamp(3_000, 0)))
    }

    @Test
    fun `event whose end precedes its start cannot be rated`() {
        val event = Event(startAt = endAt, endAt = startAt)

        assertFalse(EventRatingEligibility.hasEnded(event, Timestamp(3_000, 0)))
    }
}
