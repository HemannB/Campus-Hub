package br.com.uri.campushub.model

data class EventSeats(
    val capacity: Int,
    val confirmed: Int,
    val waiting: List<String> = emptyList(),
    val invited: List<String> = emptyList()
) {
    fun join(userId: String): Pair<EventSeats, RegistrationStatus> {
        check(userId !in waiting && userId !in invited) { "Você já está na fila." }
        return if ((capacity <= 0 || confirmed + invited.size < capacity) && waiting.isEmpty()) {
            copy(confirmed = confirmed + 1) to RegistrationStatus.CONFIRMED
        } else {
            copy(waiting = waiting + userId) to RegistrationStatus.WAITLISTED
        }
    }

    fun confirm(userId: String): EventSeats {
        check(userId in invited) { "Você não possui uma vaga reservada." }
        return copy(confirmed = confirmed + 1, invited = invited - userId)
    }

    fun cancel(userId: String, status: RegistrationStatus): Pair<EventSeats, String?> {
        val updated = copy(
            confirmed = if (status == RegistrationStatus.CONFIRMED) (confirmed - 1).coerceAtLeast(0) else confirmed,
            waiting = waiting - userId,
            invited = invited - userId
        )
        return updated.offerNext()
    }

    fun offerNext(): Pair<EventSeats, String?> {
        val next = waiting.firstOrNull()
        return if (next != null && (capacity <= 0 || confirmed + invited.size < capacity)) {
            copy(waiting = waiting.drop(1), invited = invited + next) to next
        } else this to null
    }
}
