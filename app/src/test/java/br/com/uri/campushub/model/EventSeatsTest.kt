package br.com.uri.campushub.model

import org.junit.Assert.*
import org.junit.Test

class EventSeatsTest {
    @Test fun joinsUntilCapacityThenWaits() {
        val (first, confirmed) = EventSeats(1, 0).join("a")
        val (full, waiting) = first.join("b")
        assertEquals(RegistrationStatus.CONFIRMED, confirmed)
        assertEquals(RegistrationStatus.WAITLISTED, waiting)
        assertEquals(1, full.confirmed)
        assertEquals(listOf("b"), full.waiting)
    }

    @Test fun cancellationReservesForFirstWithoutAllowingOverbooking() {
        val (seats, next) = EventSeats(1, 1, listOf("b", "c")).cancel("a", RegistrationStatus.CONFIRMED)
        assertEquals("b", next)
        assertEquals(listOf("b"), seats.invited)
        assertEquals(listOf("c"), seats.waiting)
        val (afterJoin, status) = seats.join("d")
        assertEquals(RegistrationStatus.WAITLISTED, status)
        assertEquals(listOf("c", "d"), afterJoin.waiting)
        val confirmed = seats.confirm("b")
        assertEquals(1, confirmed.confirmed)
        assertTrue(confirmed.invited.isEmpty())
    }

    @Test fun leavingQueueDoesNotReduceCountAndDecliningOffersToNext() {
        val (waiting, _) = EventSeats(1, 1, listOf("b", "c")).cancel("b", RegistrationStatus.WAITLISTED)
        assertEquals(1, waiting.confirmed)
        assertEquals(listOf("c"), waiting.waiting)
        val (declined, next) = EventSeats(1, 0, listOf("c"), listOf("b")).cancel("b", RegistrationStatus.INVITED)
        assertEquals("c", next)
        assertEquals(listOf("c"), declined.invited)
        assertTrue(declined.waiting.isEmpty())
    }

    @Test fun increasedCapacityOffersToHeadAndOnlyInviteeCanConfirm() {
        val (seats, next) = EventSeats(2, 1, listOf("b", "c")).offerNext()
        assertEquals("b", next)
        assertEquals(2, seats.confirm("b").confirmed)
        assertThrows(IllegalStateException::class.java) { seats.confirm("c") }
    }
}
