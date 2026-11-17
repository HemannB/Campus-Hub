package br.com.uri.campushub.organizer

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import br.com.uri.campushub.databinding.ItemAttendeeBinding
import br.com.uri.campushub.model.*
import java.text.SimpleDateFormat
import java.util.Locale

class AttendeeAdapter : RecyclerView.Adapter<AttendeeAdapter.Holder>() {
    private var attendees: List<Registration> = emptyList()

    fun update(items: List<Registration>) {
        attendees = items
        notifyDataSetChanged()
    }

    override fun getItemCount() = attendees.size
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemAttendeeBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(attendees[position])

    class Holder(private val binding: ItemAttendeeBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(registration: Registration) {
            binding.textName.text = registration.userName.ifBlank { registration.userId }
            binding.textStatus.text = when {
                registration.checkedInAt != null -> "Presença confirmada • " +
                    SimpleDateFormat("dd/MM HH:mm", Locale.forLanguageTag("pt-BR")).format(registration.checkedInAt.toDate())
                registration.registrationStatus == RegistrationStatus.CONFIRMED -> "Inscrição confirmada"
                registration.registrationStatus == RegistrationStatus.INVITED -> "Vaga reservada • aguardando confirmação"
                else -> "Na lista de espera"
            }
        }
    }
}
