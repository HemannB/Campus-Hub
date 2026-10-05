package br.com.uri.campushub.student

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import br.com.uri.campushub.databinding.ActivityMyEventsBinding
import br.com.uri.campushub.model.Event
import br.com.uri.campushub.viewmodel.EventState
import br.com.uri.campushub.viewmodel.MyEventsViewModel

class MyEventsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMyEventsBinding
    private lateinit var viewModel: MyEventsViewModel
    private lateinit var eventAdapter: EventAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyEventsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupViewModel()
        setupListeners()
        observeEventState()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadEvents()
    }

    private fun setupRecyclerView() {
        eventAdapter = EventAdapter { event ->
            navigateToEventDetails(event.id)
        }
        binding.recyclerMyEvents.layoutManager = LinearLayoutManager(this)
        binding.recyclerMyEvents.adapter = eventAdapter
    }

    private fun setupViewModel() {
        viewModel = ViewModelProvider(this)[MyEventsViewModel::class.java]
    }

    private fun setupListeners() {
        binding.buttonBackMyEvents.setOnClickListener {
            finish()
        }

        binding.buttonRetryMyEvents.setOnClickListener {
            viewModel.loadEvents()
        }
    }

    private fun observeEventState() {
        viewModel.eventState.observe(this) { state ->
            when (state) {
                EventState.Idle -> showLoading(false)
                EventState.Loading -> showLoading(true)
                is EventState.Success -> showEvents(state.events)
                is EventState.Error -> showMessage(state.message, canRetry = true)
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressMyEvents.visibility = if (isLoading) View.VISIBLE else View.GONE

        if (isLoading) {
            binding.recyclerMyEvents.visibility = View.GONE
            binding.layoutMyEventsMessage.visibility = View.GONE
        }
    }

    private fun showEvents(events: List<Event>) {
        showLoading(false)

        if (events.isEmpty()) {
            showMessage("Você ainda não está inscrito em nenhum evento.", canRetry = false)
            return
        }

        eventAdapter.updateEvents(events)
        binding.recyclerMyEvents.visibility = View.VISIBLE
        binding.layoutMyEventsMessage.visibility = View.GONE
    }

    private fun showMessage(message: String, canRetry: Boolean) {
        showLoading(false)
        binding.recyclerMyEvents.visibility = View.GONE
        binding.layoutMyEventsMessage.visibility = View.VISIBLE
        binding.textMyEventsMessage.text = message
        binding.buttonRetryMyEvents.visibility = if (canRetry) View.VISIBLE else View.GONE
    }

    private fun navigateToEventDetails(eventId: String) {
        val intent = Intent(this, EventDetailActivity::class.java).apply {
            putExtra(EventDetailActivity.EXTRA_EVENT_ID, eventId)
        }
        startActivity(intent)
    }
}
