package br.com.uri.campushub.student

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.ViewModelProvider
import br.com.uri.campushub.R
import br.com.uri.campushub.model.Event
import br.com.uri.campushub.viewmodel.EventDetailState
import br.com.uri.campushub.viewmodel.EventDetailViewModel
import br.com.uri.campushub.viewmodel.RegistrationState
import br.com.uri.campushub.viewmodel.RegistrationViewModel
import com.google.android.material.button.MaterialButton
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

class EventDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_EVENT_ID = "event_id"
    }

    private lateinit var eventDetailViewModel: EventDetailViewModel
    private lateinit var registrationViewModel: RegistrationViewModel
    private lateinit var eventId: String
    private var currentEvent: Event? = null
    private var currentRegistrationStatus: Boolean? = null

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
    private lateinit var buttonRegistration: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event_detail)

        eventId = intent.getStringExtra(EXTRA_EVENT_ID).orEmpty()

        bindViews()
        setupViewModel()
        setupListeners()
        observeEventDetailState()
        observeRegistrationState()

        if (eventId.isBlank()) {
            showError("Evento inválido.")
        } else {
            eventDetailViewModel.loadEvent(eventId)
            registrationViewModel.loadStatus(eventId)
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
        buttonRegistration = findViewById(R.id.buttonRegistration)
    }

    private fun setupViewModel() {
        eventDetailViewModel = ViewModelProvider(this)[EventDetailViewModel::class.java]
        registrationViewModel = ViewModelProvider(this)[RegistrationViewModel::class.java]
    }

    private fun setupListeners() {
        findViewById<MaterialButton>(R.id.buttonBack).setOnClickListener {
            finish()
        }

        findViewById<MaterialButton>(R.id.buttonBackFromError).setOnClickListener {
            finish()
        }

        findViewById<MaterialButton>(R.id.buttonRetryDetail).setOnClickListener {
            if (eventId.isNotBlank()) {
                eventDetailViewModel.loadEvent(eventId)
                registrationViewModel.loadStatus(eventId)
            }
        }

        buttonRegistration.setOnClickListener {
            registrationViewModel.toggleRegistration(eventId)
        }
    }

    private fun observeEventDetailState() {
        eventDetailViewModel.eventDetailState.observe(this) { state ->
            when (state) {
                EventDetailState.Idle -> showLoading(false)
                EventDetailState.Loading -> showLoading(true)
                is EventDetailState.Success -> showEvent(state.event)
                is EventDetailState.Error -> showError(state.message)
            }
        }
    }

    private fun observeRegistrationState() {
        registrationViewModel.registrationState.observe(this) { state ->
            when (state) {
                RegistrationState.Idle,
                RegistrationState.Loading -> {
                    buttonRegistration.visibility = View.GONE
                }

                is RegistrationState.Status -> {
                    renderRegistrationButton(state.isRegistered)
                }

                is RegistrationState.Updating -> {
                    showRegistrationLoading(state.isRegistered)
                }

                is RegistrationState.Success -> {
                    renderRegistrationButton(state.isRegistered)
                    Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                    eventDetailViewModel.loadEvent(eventId)
                }

                is RegistrationState.Error -> {
                    state.isRegistered?.let(::renderRegistrationButton)
                        ?: run { buttonRegistration.visibility = View.GONE }
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                }
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
        currentEvent = event

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
        currentRegistrationStatus?.let(::renderRegistrationButton)
    }

    private fun showError(message: String) {
        showLoading(false)
        scrollEventDetail.visibility = View.GONE
        layoutDetailError.visibility = View.VISIBLE
        textDetailError.text = message
    }

    private fun renderRegistrationButton(isRegistered: Boolean) {
        currentRegistrationStatus = isRegistered

        val event = currentEvent ?: return
        val isFull = event.maxParticipants > 0 &&
            event.participantCount >= event.maxParticipants

        buttonRegistration.visibility = View.VISIBLE
        buttonRegistration.isEnabled = isRegistered || !isFull
        buttonRegistration.text = when {
            isRegistered -> "Cancelar inscrição"
            isFull -> "Evento lotado"
            else -> "Inscrever-se"
        }
    }

    private fun showRegistrationLoading(isRegistered: Boolean) {
        buttonRegistration.visibility = View.VISIBLE
        buttonRegistration.isEnabled = false
        buttonRegistration.text = if (isRegistered) {
            "Cancelando inscrição..."
        } else {
            "Realizando inscrição..."
        }
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
