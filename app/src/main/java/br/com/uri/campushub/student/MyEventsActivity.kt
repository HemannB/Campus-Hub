package br.com.uri.campushub.student

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import br.com.uri.campushub.R
import br.com.uri.campushub.model.Event
import br.com.uri.campushub.viewmodel.EventState
import br.com.uri.campushub.viewmodel.MyEventsViewModel
import com.google.android.material.button.MaterialButton

class MyEventsActivity : AppCompatActivity() {

    private lateinit var viewModel: MyEventsViewModel
    private lateinit var eventAdapter: EventAdapter

    private lateinit var recyclerMyEvents: RecyclerView
    private lateinit var progressMyEvents: ProgressBar
    private lateinit var layoutMyEventsMessage: LinearLayout
    private lateinit var textMyEventsMessage: TextView
    private lateinit var buttonRetryMyEvents: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_events)

        bindViews()
        setupRecyclerView()
        setupViewModel()
        setupListeners()
        observeEventState()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadEvents()
    }

    private fun bindViews() {
        recyclerMyEvents = findViewById(R.id.recyclerMyEvents)
        progressMyEvents = findViewById(R.id.progressMyEvents)
        layoutMyEventsMessage = findViewById(R.id.layoutMyEventsMessage)
        textMyEventsMessage = findViewById(R.id.textMyEventsMessage)
        buttonRetryMyEvents = findViewById(R.id.buttonRetryMyEvents)
    }

    private fun setupRecyclerView() {
        eventAdapter = EventAdapter { event ->
            navigateToEventDetails(event.id)
        }
        recyclerMyEvents.layoutManager = LinearLayoutManager(this)
        recyclerMyEvents.adapter = eventAdapter
    }

    private fun setupViewModel() {
        viewModel = ViewModelProvider(this)[MyEventsViewModel::class.java]
    }

    private fun setupListeners() {
        findViewById<MaterialButton>(R.id.buttonBackMyEvents).setOnClickListener {
            finish()
        }

        buttonRetryMyEvents.setOnClickListener {
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
        progressMyEvents.visibility = if (isLoading) View.VISIBLE else View.GONE

        if (isLoading) {
            recyclerMyEvents.visibility = View.GONE
            layoutMyEventsMessage.visibility = View.GONE
        }
    }

    private fun showEvents(events: List<Event>) {
        showLoading(false)

        if (events.isEmpty()) {
            showMessage("Você ainda não está inscrito em nenhum evento.", canRetry = false)
            return
        }

        eventAdapter.updateEvents(events)
        recyclerMyEvents.visibility = View.VISIBLE
        layoutMyEventsMessage.visibility = View.GONE
    }

    private fun showMessage(message: String, canRetry: Boolean) {
        showLoading(false)
        recyclerMyEvents.visibility = View.GONE
        layoutMyEventsMessage.visibility = View.VISIBLE
        textMyEventsMessage.text = message
        buttonRetryMyEvents.visibility = if (canRetry) View.VISIBLE else View.GONE
    }

    private fun navigateToEventDetails(eventId: String) {
        val intent = Intent(this, EventDetailActivity::class.java).apply {
            putExtra(EventDetailActivity.EXTRA_EVENT_ID, eventId)
        }
        startActivity(intent)
    }
}
