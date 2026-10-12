package br.com.uri.campushub.student

import android.graphics.Color
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
                    backgroundColor = Color.parseColor("#DFF7F1"),
                    textColor = Color.parseColor("#08776D")
                )

            category.contains("cultura", ignoreCase = true) ||
                category.contains("música", ignoreCase = true) ||
                category.contains("musica", ignoreCase = true) ->
                EventCategoryStyle(
                    imageRes = R.drawable.event_default_culture,
                    backgroundColor = Color.parseColor("#F1E8FF"),
                    textColor = Color.parseColor("#6C2BD9")
                )

            category.contains("esporte", ignoreCase = true) ||
                category.contains("sport", ignoreCase = true) ->
                EventCategoryStyle(
                    imageRes = R.drawable.event_default_sports,
                    backgroundColor = Color.parseColor("#E5F5E9"),
                    textColor = Color.parseColor("#237A40")
                )

            category.contains("tecnologia", ignoreCase = true) ||
                category.contains("tech", ignoreCase = true) ->
                EventCategoryStyle(
                    imageRes = R.drawable.event_default_technology,
                    backgroundColor = Color.parseColor("#E8F0FF"),
                    textColor = Color.parseColor("#1557D6")
                )

            else -> EventCategoryStyle(
                imageRes = R.drawable.event_default_technology,
                backgroundColor = Color.parseColor("#EEF2F8"),
                textColor = Color.parseColor("#475467")
            )
        }
    }
}
