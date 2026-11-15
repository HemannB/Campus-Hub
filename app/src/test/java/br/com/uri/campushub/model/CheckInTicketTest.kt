package br.com.uri.campushub.model

import org.junit.Assert.*
import org.junit.Test

class CheckInTicketTest {
    @Test fun ticketPreservesEventRegistrationAndToken() {
        val ticket = CheckInTicket("event", "registration", "random-token")
        assertEquals(ticket, CheckInTicket.parse(ticket.encode()))
    }

    @Test fun rejectsMalformedAndEmptyTickets() {
        listOf("", "other|event|registration|token", "CampusHub||id|token", "CampusHub|event|id",
            "CampusHub|event|invalid/path|token")
            .forEach { assertNull(CheckInTicket.parse(it)) }
    }
}
