package br.com.uri.campushub.organizer

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import br.com.uri.campushub.databinding.ItemOrganizerEventBinding
import br.com.uri.campushub.model.Event
import br.com.uri.campushub.model.EventStatus
import br.com.uri.campushub.model.eventStatus
import br.com.uri.campushub.ui.loadEventImage
import br.com.uri.campushub.student.EventCategoryVisuals
import java.text.SimpleDateFormat
import java.util.Locale

class OrganizerEventAdapter(private val onClick: (Event) -> Unit) :
    ListAdapter<Event, OrganizerEventAdapter.EventViewHolder>(OrganizerEventDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val binding = ItemOrganizerEventBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return EventViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        holder.bind(getItem(position), onClick)
    }

    class EventViewHolder(
        private val binding: ItemOrganizerEventBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(event: Event, onClick: (Event) -> Unit) {
            binding.root.setOnClickListener { onClick(event) }
            binding.imageEvent.loadEventImage(event.imageUrl, EventCategoryVisuals.resolve(event.category).imageRes)
            binding.textEventTitle.text = event.title
            binding.textEventStatus.text = when (event.eventStatus) {
                EventStatus.DRAFT -> "Rascunho"
                EventStatus.PUBLISHED -> "Publicado"
                EventStatus.ENDED -> "Encerrado"
            }
            binding.textEventDate.text = event.startAt?.toDate()?.let { date ->
                SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale.forLanguageTag("pt-BR"))
                    .format(date)
            } ?: "Data a definir"
            binding.textEventParticipants.text = if (event.maxParticipants > 0) {
                "${event.participantCount}/${event.maxParticipants} inscritos"
            } else {
                "${event.participantCount} inscritos"
            }
        }
    }
}

private object OrganizerEventDiffCallback : DiffUtil.ItemCallback<Event>() {
    override fun areItemsTheSame(oldItem: Event, newItem: Event) = oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: Event, newItem: Event) = oldItem == newItem
}
