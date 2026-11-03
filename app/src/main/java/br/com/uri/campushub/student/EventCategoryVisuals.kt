package br.com.uri.campushub.student

import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import br.com.uri.campushub.R

data class EventCategoryStyle(
    @param:DrawableRes val imageRes: Int,
    @param:ColorInt val backgroundColor: Int,
    @param:ColorInt val textColor: Int
)

object EventCategoryVisuals {

    fun resolve(category: String): EventCategoryStyle {
        return when {
            category.contains("carreira", ignoreCase = true) ||
                category.contains("career", ignoreCase = true) ->
                EventCategoryStyle(
                    imageRes = R.drawable.event_default_career,
                    backgroundColor = 0xFFDFF7F1.toInt(),
                    textColor = 0xFF08776D.toInt()
                )

            category.contains("cultura", ignoreCase = true) ||
                category.contains("música", ignoreCase = true) ||
                category.contains("musica", ignoreCase = true) ->
                EventCategoryStyle(
                    imageRes = R.drawable.event_default_culture,
                    backgroundColor = 0xFFF1E8FF.toInt(),
                    textColor = 0xFF6C2BD9.toInt()
                )

            category.contains("esporte", ignoreCase = true) ||
                category.contains("sport", ignoreCase = true) ->
                EventCategoryStyle(
                    imageRes = R.drawable.event_default_sports,
                    backgroundColor = 0xFFE5F5E9.toInt(),
                    textColor = 0xFF237A40.toInt()
                )

            category.contains("tecnologia", ignoreCase = true) ||
                category.contains("tech", ignoreCase = true) ->
                EventCategoryStyle(
                    imageRes = R.drawable.event_default_technology,
                    backgroundColor = 0xFFE8F0FF.toInt(),
                    textColor = 0xFF1557D6.toInt()
                )

            else -> EventCategoryStyle(
                imageRes = R.drawable.event_default_technology,
                backgroundColor = 0xFFEEF2F8.toInt(),
                textColor = 0xFF475467.toInt()
            )
        }
    }
}
