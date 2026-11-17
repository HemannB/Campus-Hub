package br.com.uri.campushub.organizer

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import br.com.uri.campushub.databinding.ActivityOrganizerEventBinding
import br.com.uri.campushub.model.*
import br.com.uri.campushub.viewmodel.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

class OrganizerEventActivity : AppCompatActivity() {
    private lateinit var binding: ActivityOrganizerEventBinding
    private lateinit var viewModel: OrganizerEventViewModel
    private lateinit var eventId: String
    private val adapter = AttendeeAdapter()
    private val scanner = registerForActivityResult(ScanContract()) { result ->
        result.contents?.let { viewModel.checkIn(eventId, it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOrganizerEventBinding.inflate(layoutInflater)
        setContentView(binding.root)
        eventId = intent.getStringExtra("event_id").orEmpty()
        viewModel = ViewModelProvider(this)[OrganizerEventViewModel::class.java]
        binding.recyclerAttendees.layoutManager = LinearLayoutManager(this)
        binding.recyclerAttendees.adapter = adapter
        binding.buttonBack.setOnClickListener { finish() }
        binding.buttonRetry.setOnClickListener { viewModel.load(eventId) }
        binding.buttonEdit.setOnClickListener {
            startActivity(Intent(this, EventFormActivity::class.java).putExtra("event_id", eventId))
        }
        binding.buttonPublish.setOnClickListener { confirmStatus(EventStatus.PUBLISHED, "Publicar este evento?") }
        binding.buttonEnd.setOnClickListener { confirmStatus(EventStatus.ENDED, "Encerrar este evento?") }
        binding.buttonScan.setOnClickListener {
            scanner.launch(ScanOptions().setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                .setPrompt("Escaneie o QR Code da inscrição").setBeepEnabled(false))
        }
        viewModel.message.observe(this) { message ->
            if (message != null) {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                viewModel.clearMessage()
            }
        }
        viewModel.state.observe(this) { state ->
            binding.progress.visibility = if (state == OrganizerEventState.Loading) View.VISIBLE else View.GONE
            binding.content.visibility = if (state is OrganizerEventState.Ready) View.VISIBLE else View.GONE
            binding.textError.visibility = if (state is OrganizerEventState.Error) View.VISIBLE else View.GONE
            binding.buttonRetry.visibility = if (state is OrganizerEventState.Error) View.VISIBLE else View.GONE
            when (state) {
                OrganizerEventState.Loading -> Unit
                is OrganizerEventState.Error -> binding.textError.text = state.message
                is OrganizerEventState.Ready -> {
                    val event = state.event
                    binding.textTitle.text = event.title
                    val checked = state.attendees.count { it.checkedInAt != null }
                    binding.textSummary.text = "${event.participantCount}/${event.maxParticipants} inscritos • " +
                        "${event.invitedUserIds.size} vagas reservadas • ${event.waitingUserIds.size} na fila • $checked presentes"
                    binding.textEmpty.visibility = if (state.attendees.isEmpty()) View.VISIBLE else View.GONE
                    binding.buttonEdit.visibility = if (event.eventStatus == EventStatus.ENDED) View.GONE else View.VISIBLE
                    binding.buttonPublish.visibility = if (event.eventStatus == EventStatus.DRAFT) View.VISIBLE else View.GONE
                    binding.buttonEnd.visibility = if (event.eventStatus == EventStatus.PUBLISHED) View.VISIBLE else View.GONE
                    binding.buttonScan.visibility = if (event.eventStatus == EventStatus.PUBLISHED &&
                        !EventRatingEligibility.hasEnded(event)) View.VISIBLE else View.GONE
                    adapter.update(state.attendees)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.load(eventId)
    }

    private fun confirmStatus(status: EventStatus, title: String) {
        MaterialAlertDialogBuilder(this).setTitle(title)
            .setNegativeButton("Voltar", null)
            .setPositiveButton("Confirmar") { _, _ -> viewModel.changeStatus(eventId, status) }.show()
    }
}
