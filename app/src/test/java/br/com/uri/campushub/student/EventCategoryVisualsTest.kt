package br.com.uri.campushub.student

import br.com.uri.campushub.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class EventCategoryVisualsTest {

    @Test
    fun `supported categories use their corresponding images`() {
        assertEquals(
            R.drawable.event_default_career,
            EventCategoryVisuals.resolve("Carreira").imageRes
        )
        assertEquals(
            R.drawable.event_default_culture,
            EventCategoryVisuals.resolve("Cultura").imageRes
        )
        assertEquals(
            R.drawable.event_default_sports,
            EventCategoryVisuals.resolve("Esporte").imageRes
        )
        assertEquals(
            R.drawable.event_default_technology,
            EventCategoryVisuals.resolve("Tecnologia").imageRes
        )
    }

    @Test
    fun `category matching ignores case and accepts alternative names`() {
        assertEquals(
            EventCategoryVisuals.resolve("Carreira"),
            EventCategoryVisuals.resolve("CAREER FAIR")
        )
        assertEquals(
            EventCategoryVisuals.resolve("Cultura"),
            EventCategoryVisuals.resolve("Festival de Musica")
        )
        assertEquals(
            EventCategoryVisuals.resolve("Esporte"),
            EventCategoryVisuals.resolve("SPORTS DAY")
        )
        assertEquals(
            EventCategoryVisuals.resolve("Tecnologia"),
            EventCategoryVisuals.resolve("TECH WEEK")
        )
    }

    @Test
    fun `unknown category uses neutral fallback colors`() {
        val technology = EventCategoryVisuals.resolve("Tecnologia")
        val fallback = EventCategoryVisuals.resolve("Outra categoria")

        assertEquals(R.drawable.event_default_technology, fallback.imageRes)
        assertEquals(0xFFEEF2F8.toInt(), fallback.backgroundColor)
        assertEquals(0xFF475467.toInt(), fallback.textColor)
        assertNotEquals(technology.backgroundColor, fallback.backgroundColor)
    }
}
