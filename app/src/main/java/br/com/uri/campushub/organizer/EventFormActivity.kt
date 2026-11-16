package br.com.uri.campushub.organizer

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import br.com.uri.campushub.databinding.ActivityCreateEventBinding
import br.com.uri.campushub.model.Event
import br.com.uri.campushub.viewmodel.EventFormState
import br.com.uri.campushub.viewmodel.EventFormViewModel
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class EventFormActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCreateEventBinding
    private lateinit var viewModel: EventFormViewModel
    private var startMillis: Long? = null
    private var endMillis: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateEventBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewModel = ViewModelProvider(this)[EventFormViewModel::class.java]

        startMillis = savedInstanceState?.takeIf { it.containsKey("startMillis") }?.getLong("startMillis")
        endMillis = savedInstanceState?.takeIf { it.containsKey("endMillis") }?.getLong("endMillis")
        showDates()
        binding.buttonBack.setOnClickListener { finish() }
        binding.inputStart.setOnClickListener {
            pickDateTime(startMillis) { startMillis = it; showDates() }
        }
        binding.inputEnd.setOnClickListener {
            pickDateTime(endMillis ?: startMillis?.plus(3_600_000)) { endMillis = it; showDates() }
        }
        binding.buttonSaveDraft.setOnClickListener { saveEvent() }
        observeState()
        if (intent.hasExtra("event_id")) {
            binding.textFormTitle.text = "Editar evento"
            binding.textFormDescription.text = "Atualize os dados. A situação de publicação será mantida."
            binding.buttonSaveDraft.text = "Salvar alterações"
            if (savedInstanceState == null || viewModel.state.value == EventFormState.Idle) {
                viewModel.loadEvent(intent.getStringExtra("event_id").orEmpty())
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        startMillis?.let { outState.putLong("startMillis", it) }
        endMillis?.let { outState.putLong("endMillis", it) }
        super.onSaveInstanceState(outState)
    }

    private fun saveEvent() {
        viewModel.saveEvent(Event(
            title = binding.inputTitle.text.toString(),
            description = binding.inputDescription.text.toString(),
            category = binding.inputCategory.text.toString(),
            location = binding.inputLocation.text.toString(),
            startAt = startMillis?.let { Timestamp(Date(it)) },
            endAt = endMillis?.let { Timestamp(Date(it)) },
            maxParticipants = binding.inputCapacity.text.toString().toIntOrNull() ?: 0,
            imageUrl = binding.inputImageUrl.text.toString()
        ))
    }

    private fun observeState() {
        viewModel.state.observe(this) { state ->
            val saving = state == EventFormState.Saving
            binding.progressSave.visibility = if (saving) View.VISIBLE else View.GONE
            binding.textError.visibility = View.GONE
            listOf(
                binding.buttonSaveDraft, binding.buttonBack, binding.inputTitle,
                binding.inputDescription, binding.inputCategory, binding.inputLocation,
                binding.inputStart, binding.inputEnd, binding.inputCapacity, binding.inputImageUrl
            ).forEach { it.isEnabled = !saving }

            when (state) {
                is EventFormState.Ready -> {
                    if (binding.inputTitle.text.isNullOrEmpty()) {
                        val event = state.event
                        binding.inputTitle.setText(event.title)
                        binding.inputDescription.setText(event.description)
                        binding.inputCategory.setText(event.category)
                        binding.inputLocation.setText(event.location)
                        binding.inputCapacity.setText(event.maxParticipants.toString())
                        binding.inputImageUrl.setText(event.imageUrl)
                        startMillis = event.startAt?.toDate()?.time
                        endMillis = event.endAt?.toDate()?.time
                        showDates()
                    }
                }
                EventFormState.Idle, EventFormState.Saving -> Unit
                EventFormState.Success -> {
                    Toast.makeText(this, "Evento salvo.", Toast.LENGTH_SHORT).show()
                    finish()
                }
                is EventFormState.Error -> {
                    binding.textError.text = state.message
                    binding.textError.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun pickDateTime(initialMillis: Long?, onSelected: (Long) -> Unit) {
        val calendar = Calendar.getInstance()
        initialMillis?.let { calendar.timeInMillis = it }
        DatePickerDialog(this, { _, year, month, day ->
            calendar.set(year, month, day)
            TimePickerDialog(this, { _, hour, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hour)
                calendar.set(Calendar.MINUTE, minute)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                onSelected(calendar.timeInMillis)
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun showDates() {
        val format = SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale.forLanguageTag("pt-BR"))
        binding.inputStart.setText(startMillis?.let { format.format(Date(it)) }.orEmpty())
        binding.inputEnd.setText(endMillis?.let { format.format(Date(it)) }.orEmpty())
    }
}
