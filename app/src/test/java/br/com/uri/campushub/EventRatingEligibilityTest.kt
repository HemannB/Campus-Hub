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
    fun `event can be rated when or after it ends`() {
        val event = Event(startAt = startAt, endAt = endAt)

        assertTrue(EventRatingEligibility.hasEnded(event, endAt))
        assertTrue(EventRatingEligibility.hasEnded(event, Timestamp(2_001, 0)))
    }

    @Test
    fun `event without complete dates cannot be rated`() {
        val withoutEnd = Event(startAt = startAt, endAt = null)
        val withoutStart = Event(startAt = null, endAt = endAt)

        assertFalse(EventRatingEligibility.hasEnded(withoutEnd, Timestamp(3_000, 0)))
        assertFalse(EventRatingEligibility.hasEnded(withoutStart, Timestamp(3_000, 0)))
    }

    @Test
    fun `event whose end precedes its start cannot be rated`() {
        val event = Event(startAt = endAt, endAt = startAt)

        assertFalse(EventRatingEligibility.hasEnded(event, Timestamp(3_000, 0)))
    }
}
