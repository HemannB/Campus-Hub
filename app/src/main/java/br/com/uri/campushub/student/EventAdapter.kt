package br.com.uri.campushub.student

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import br.com.uri.campushub.R
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
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_event, parent, false)

        return EventViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val event = events[position]
        holder.bind(event)
        holder.itemView.setOnClickListener {
            onEventClick(event)
        }
    }

    override fun getItemCount(): Int = events.size

    class EventViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val textCategory: TextView = itemView.findViewById(R.id.textEventCategory)
        private val textTitle: TextView = itemView.findViewById(R.id.textEventTitle)
        private val textDate: TextView = itemView.findViewById(R.id.textEventDate)
        private val textLocation: TextView = itemView.findViewById(R.id.textEventLocation)
        private val textParticipants: TextView = itemView.findViewById(R.id.textEventParticipants)

        fun bind(event: Event) {
            textCategory.text = event.category.ifBlank { "Evento" }
            textTitle.text = event.title
            textDate.text = formatDate(event)
            textLocation.text = event.location.ifBlank { "Local a definir" }
            textParticipants.text = formatParticipants(event)
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
