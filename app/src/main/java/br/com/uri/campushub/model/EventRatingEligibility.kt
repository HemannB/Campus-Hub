package br.com.uri.campushub.model

import com.google.firebase.Timestamp

object EventRatingEligibility {

    fun hasEnded(event: Event, now: Timestamp = Timestamp.now()): Boolean {
        val startAt = event.startAt ?: return false
        val endAt = event.endAt ?: return false

        if (endAt.compareTo(startAt) < 0) {
            return false
        }

        return endAt.compareTo(now) <= 0
    }
}
