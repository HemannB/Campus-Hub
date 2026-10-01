package br.com.uri.campushub.student

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.ViewModelProvider
import br.com.uri.campushub.R
import br.com.uri.campushub.model.Event
import br.com.uri.campushub.viewmodel.EventDetailState
import br.com.uri.campushub.viewmodel.EventDetailViewModel
import com.google.android.material.button.MaterialButton
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

class EventDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_EVENT_ID = "event_id"
    }

    private lateinit var viewModel: EventDetailViewModel
    private lateinit var eventId: String

    private lateinit var scrollEventDetail: NestedScrollView
    private lateinit var progressEventDetail: ProgressBar
    private lateinit var layoutDetailError: LinearLayout
    private lateinit var textDetailError: TextView

    private lateinit var textCategory: TextView
    private lateinit var textTitle: TextView
    private lateinit var textDescription: TextView
    private lateinit var textStart: TextView
    private lateinit var textEnd: TextView
    private lateinit var textLocation: TextView
    private lateinit var textOrganizer: TextView
    private lateinit var textParticipants: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event_detail)

        eventId = intent.getStringExtra(EXTRA_EVENT_ID).orEmpty()

        bindViews()
        setupViewModel()
        setupListeners()
        observeEventDetailState()

        if (eventId.isBlank()) {
            showError("Evento inválido.")
        } else {
            viewModel.loadEvent(eventId)
        }
    }

    private fun bindViews() {
        scrollEventDetail = findViewById(R.id.scrollEventDetail)
        progressEventDetail = findViewById(R.id.progressEventDetail)
        layoutDetailError = findViewById(R.id.layoutDetailError)
        textDetailError = findViewById(R.id.textDetailError)

        textCategory = findViewById(R.id.textDetailCategory)
        textTitle = findViewById(R.id.textDetailTitle)
        textDescription = findViewById(R.id.textDetailDescription)
        textStart = findViewById(R.id.textDetailStart)
        textEnd = findViewById(R.id.textDetailEnd)
        textLocation = findViewById(R.id.textDetailLocation)
        textOrganizer = findViewById(R.id.textDetailOrganizer)
        textParticipants = findViewById(R.id.textDetailParticipants)
    }

    private fun setupViewModel() {
        viewModel = ViewModelProvider(this)[EventDetailViewModel::class.java]
    }

    private fun setupListeners() {
        findViewById<MaterialButton>(R.id.buttonBack).setOnClickListener {
            finish()
        }

        findViewById<MaterialButton>(R.id.buttonBackFromError).setOnClickListener {
            finish()
        }

        findViewById<MaterialButton>(R.id.buttonRetryDetail).setOnClickListener {
            viewModel.loadEvent(eventId)
        }
    }

    private fun observeEventDetailState() {
        viewModel.eventDetailState.observe(this) { state ->
            when (state) {
                EventDetailState.Idle -> showLoading(false)
                EventDetailState.Loading -> showLoading(true)
                is EventDetailState.Success -> showEvent(state.event)
                is EventDetailState.Error -> showError(state.message)
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        progressEventDetail.visibility = if (isLoading) View.VISIBLE else View.GONE

        if (isLoading) {
            scrollEventDetail.visibility = View.GONE
            layoutDetailError.visibility = View.GONE
        }
    }

    private fun showEvent(event: Event) {
        showLoading(false)

        textCategory.text = event.category.ifBlank { "Evento" }
        textTitle.text = event.title
        textDescription.text = event.description.ifBlank { "Evento sem descrição." }
        textStart.text = formatDate(event.startAt)
        textEnd.text = formatDate(event.endAt)
        textLocation.text = event.location.ifBlank { "Local a definir" }
        textOrganizer.text = event.organizer.ifBlank { "Organizador não informado" }
        textParticipants.text = formatParticipants(event)

        layoutDetailError.visibility = View.GONE
        scrollEventDetail.visibility = View.VISIBLE
    }

    private fun showError(message: String) {
        showLoading(false)
        scrollEventDetail.visibility = View.GONE
        layoutDetailError.visibility = View.VISIBLE
        textDetailError.text = message
    }

    private fun formatDate(timestamp: Timestamp?): String {
        val date = timestamp?.toDate() ?: return "Data a definir"
        val formatter = SimpleDateFormat(
            "dd/MM/yyyy 'às' HH:mm",
            Locale.forLanguageTag("pt-BR")
        )

        return formatter.format(date)
    }

    private fun formatParticipants(event: Event): String {
        return if (event.maxParticipants > 0) {
            "${event.participantCount} de ${event.maxParticipants} vagas preenchidas"
        } else {
            "${event.participantCount} inscritos"
        }
    }
}
