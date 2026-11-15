package br.com.uri.campushub.viewmodel

import br.com.uri.campushub.model.Event
import br.com.uri.campushub.model.EventValidator
import com.google.firebase.Timestamp
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class EventValidatorTest {
    private val event = Event(
        title = "Oficina", description = "Aprenda Kotlin", category = "Tecnologia",
        location = "Rua A, 123, Centro, Erechim - RS",
        startAt = Timestamp(100, 0), endAt = Timestamp(200, 0), maxParticipants = 20
    )

    @Test
    fun `required text fields cannot be blank`() {
        listOf(
            event.copy(title = " "), event.copy(description = ""),
            event.copy(category = ""), event.copy(location = "")
        ).forEach { assertNotNull(EventValidator.validate(it)) }
    }

    @Test
    fun `both dates are required and end must be later`() {
        listOf(
            event.copy(startAt = null), event.copy(endAt = null),
            event.copy(endAt = event.startAt), event.copy(endAt = Timestamp(50, 0))
        ).forEach { assertNotNull(EventValidator.validate(it)) }
        assertNull(EventValidator.validate(event))
    }

    @Test
    fun `capacity must be positive`() {
        assertNotNull(EventValidator.validate(event.copy(maxParticipants = 0)))
        assertNotNull(EventValidator.validate(event.copy(maxParticipants = -1)))
        assertNull(EventValidator.validate(event.copy(maxParticipants = 1)))
    }

    @Test
    fun `image is optional but must be a web url when provided`() {
        assertNull(EventValidator.validate(event))
        assertNull(EventValidator.validate(event.copy(imageUrl = "https://example.com/photo.jpg")))
        assertNotNull(EventValidator.validate(event.copy(imageUrl = "not a url")))
        assertNotNull(EventValidator.validate(event.copy(imageUrl = "file:///photo.jpg")))
    }
}
