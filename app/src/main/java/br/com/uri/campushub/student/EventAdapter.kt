package br.com.uri.campushub.student

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import br.com.uri.campushub.databinding.ItemEventBinding
import br.com.uri.campushub.model.Event
import java.text.SimpleDateFormat
import java.util.Locale

class EventAdapter(
    private val onEventClick: (Event) -> Unit
) : ListAdapter<Event, EventAdapter.EventViewHolder>(EventDiffCallback) {

    fun updateEvents(newEvents: List<Event>) {
        submitList(newEvents)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val binding = ItemEventBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return EventViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val event = getItem(position)
        holder.bind(event, onEventClick)
    }

    class EventViewHolder(
        private val binding: ItemEventBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(event: Event, onEventClick: (Event) -> Unit) {
            binding.textEventCategory.text = event.category.ifBlank { "Evento" }
            applyCategoryStyle(event.category)
            binding.imageEvent.setImageResource(eventImage(event.category))
            binding.imageEvent.contentDescription = "Imagem do evento ${event.title}"
            binding.textEventTitle.text = event.title
            binding.textEventDescription.text = event.description.ifBlank {
                "Confira os detalhes e participe deste evento."
            }
            binding.textEventDate.text = formatDate(event)
            binding.textEventLocation.text = event.location.ifBlank { "Local a definir" }
            binding.textEventParticipants.text = formatParticipants(event)
            binding.root.setOnClickListener { onEventClick(event) }
            binding.textViewDetails.setOnClickListener { onEventClick(event) }
        }

        private fun formatDate(event: Event): String {
            val startDate = event.startAt?.toDate() ?: return "Data a definir"
            val locale = Locale.forLanguageTag("pt-BR")
            val dateFormatter = SimpleDateFormat("dd/MM/yyyy", locale)
            val timeFormatter = SimpleDateFormat("HH:mm", locale)
            val start = "${dateFormatter.format(startDate)} • ${timeFormatter.format(startDate)}"
            val endDate = event.endAt?.toDate() ?: return start

            return if (dateFormatter.format(startDate) == dateFormatter.format(endDate)) {
                "$start–${timeFormatter.format(endDate)}"
            } else {
                start
            }
        }

        private fun formatParticipants(event: Event): String {
            return if (event.maxParticipants > 0) {
                "${event.participantCount}/${event.maxParticipants} inscritos"
            } else {
                "${event.participantCount} inscritos"
            }
        }

        private fun eventImage(category: String): Int {
            return when {
                category.contains("carreira", ignoreCase = true) ||
                    category.contains("career", ignoreCase = true) ->
                    br.com.uri.campushub.R.drawable.event_default_career

                category.contains("cultura", ignoreCase = true) ||
                    category.contains("música", ignoreCase = true) ||
                    category.contains("musica", ignoreCase = true) ->
                    br.com.uri.campushub.R.drawable.event_default_culture

                category.contains("esporte", ignoreCase = true) ||
                    category.contains("sport", ignoreCase = true) ->
                    br.com.uri.campushub.R.drawable.event_default_sports

                else -> br.com.uri.campushub.R.drawable.event_default_technology
            }
        }

        private fun applyCategoryStyle(category: String) {
            val (backgroundColor, textColor) = when {
                category.contains("carreira", ignoreCase = true) ||
                    category.contains("career", ignoreCase = true) ->
                    "#DFF7F1" to "#08776D"

                category.contains("cultura", ignoreCase = true) ||
                    category.contains("música", ignoreCase = true) ||
                    category.contains("musica", ignoreCase = true) ->
                    "#F1E8FF" to "#6C2BD9"

                category.contains("esporte", ignoreCase = true) ||
                    category.contains("sport", ignoreCase = true) ->
                    "#E5F5E9" to "#237A40"

                category.contains("tecnologia", ignoreCase = true) ||
                    category.contains("tech", ignoreCase = true) ->
                    "#E8F0FF" to "#1557D6"

                else -> "#EEF2F8" to "#475467"
            }

            binding.textEventCategory.backgroundTintList =
                ColorStateList.valueOf(Color.parseColor(backgroundColor))
            binding.textEventCategory.setTextColor(Color.parseColor(textColor))
        }
    }
}

private object EventDiffCallback : DiffUtil.ItemCallback<Event>() {
    override fun areItemsTheSame(oldItem: Event, newItem: Event): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Event, newItem: Event): Boolean {
        return oldItem == newItem
    }
}
