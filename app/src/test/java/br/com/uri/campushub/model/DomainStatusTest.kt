package br.com.uri.campushub.model

import org.junit.Assert.assertEquals
import org.junit.Test

class DomainStatusTest {

    @Test
    fun `user role accepts stored values and defaults to student`() {
        assertEquals(UserRole.ORGANIZER, UserRole.from(" organizer "))
        assertEquals(UserRole.STUDENT, UserRole.from("STUDENT"))
        assertEquals(UserRole.STUDENT, UserRole.from(null))
        assertEquals(UserRole.STUDENT, UserRole.from("UNKNOWN"))
    }

    @Test
    fun `event status accepts stored values and defaults old events to published`() {
        assertEquals(EventStatus.DRAFT, EventStatus.from("draft"))
        assertEquals(EventStatus.ENDED, EventStatus.from(" ENDED "))
        assertEquals(EventStatus.PUBLISHED, EventStatus.from(null))
        assertEquals(EventStatus.PUBLISHED, EventStatus.from("UNKNOWN"))
    }

    @Test
    fun `registration status accepts queue states and defaults old records to confirmed`() {
        assertEquals(RegistrationStatus.WAITLISTED, RegistrationStatus.from("waitlisted"))
        assertEquals(RegistrationStatus.INVITED, RegistrationStatus.from(" INVITED "))
        assertEquals(RegistrationStatus.CONFIRMED, RegistrationStatus.from(null))
        assertEquals(RegistrationStatus.CONFIRMED, RegistrationStatus.from("UNKNOWN"))
    }

    @Test
    fun `models expose typed role and event status`() {
        val organizer = User(role = "ORGANIZER")
        val draft = Event(organizerId = "organizer-1", status = "DRAFT")
        val legacyEvent = Event()

        assertEquals(UserRole.ORGANIZER, organizer.userRole)
        assertEquals("organizer-1", draft.organizerId)
        assertEquals(EventStatus.DRAFT, draft.eventStatus)
        assertEquals(EventStatus.PUBLISHED, legacyEvent.eventStatus)
    }
}
