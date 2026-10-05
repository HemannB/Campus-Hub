package br.com.uri.campushub.student

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

private object EventDiffCallback : DiffUtil.ItemCallback<Event>() {
    override fun areItemsTheSame(oldItem: Event, newItem: Event): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Event, newItem: Event): Boolean {
        return oldItem == newItem
    }
}
