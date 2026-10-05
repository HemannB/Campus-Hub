package br.com.uri.campushub.student

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import br.com.uri.campushub.databinding.ItemEventBinding
import br.com.uri.campushub.model.Event
import java.text.SimpleDateFormat
import java.util.Locale

class EventAdapter(
    private val onEventClick: (Event) -> Unit
) : RecyclerView.Adapter<EventAdapter.EventViewHolder>() {

    private val events = mutableListOf<Event>()

    fun updateEvents(newEvents: List<Event>) {
        events.clear()
        events.addAll(newEvents)
        notifyDataSetChanged()
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
        val event = events[position]
        holder.bind(event, onEventClick)
    }

    override fun getItemCount(): Int = events.size

    class EventViewHolder(
        private val binding: ItemEventBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(event: Event, onEventClick: (Event) -> Unit) {
            binding.textEventCategory.text = event.category.ifBlank { "Evento" }
            binding.textEventTitle.text = event.title
            binding.textEventDate.text = formatDate(event)
            binding.textEventLocation.text = event.location.ifBlank { "Local a definir" }
            binding.textEventParticipants.text = formatParticipants(event)
            binding.root.setOnClickListener { onEventClick(event) }
        }

        private fun formatDate(event: Event): String {
            val startDate = event.startAt?.toDate() ?: return "Data a definir"
            val formatter = SimpleDateFormat(
                "dd/MM/yyyy 'às' HH:mm",
                Locale.forLanguageTag("pt-BR")
            )

            return formatter.format(startDate)
        }

        private fun formatParticipants(event: Event): String {
            return if (event.maxParticipants > 0) {
                "${event.participantCount}/${event.maxParticipants} inscritos"
            } else {
                "${event.participantCount} inscritos"
            }
        }
    }
}
