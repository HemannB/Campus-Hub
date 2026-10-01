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
import br.com.uri.campushub.MainActivity
import br.com.uri.campushub.R
import br.com.uri.campushub.model.Event
import br.com.uri.campushub.viewmodel.AuthViewModel
import br.com.uri.campushub.viewmodel.EventState
import br.com.uri.campushub.viewmodel.EventViewModel
import com.google.android.material.button.MaterialButton

class HomeActivity : AppCompatActivity() {

    private lateinit var authViewModel: AuthViewModel
    private lateinit var eventViewModel: EventViewModel
    private lateinit var eventAdapter: EventAdapter

    private lateinit var recyclerEvents: RecyclerView
    private lateinit var progressEvents: ProgressBar
    private lateinit var layoutEventMessage: LinearLayout
    private lateinit var textEventMessage: TextView
    private lateinit var buttonRetryEvents: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        bindViews()
        setupRecyclerView()
        setupViewModels()
        observeEventState()
        buttonListeners()

        eventViewModel.loadEvents()
    }

    private fun bindViews() {
        recyclerEvents = findViewById(R.id.recyclerEvents)
        progressEvents = findViewById(R.id.progressEvents)
        layoutEventMessage = findViewById(R.id.layoutEventMessage)
        textEventMessage = findViewById(R.id.textEventMessage)
        buttonRetryEvents = findViewById(R.id.buttonRetryEvents)
    }

    private fun setupRecyclerView() {
        eventAdapter = EventAdapter()
        recyclerEvents.layoutManager = LinearLayoutManager(this)
        recyclerEvents.adapter = eventAdapter
    }

    private fun setupViewModels() {
        authViewModel = ViewModelProvider(this)[AuthViewModel::class.java]
        eventViewModel = ViewModelProvider(this)[EventViewModel::class.java]
    }

    private fun observeEventState() {
        eventViewModel.eventState.observe(this) { state ->
            when (state) {
                EventState.Idle -> showLoading(false)
                EventState.Loading -> showLoading(true)
                is EventState.Success -> showEvents(state.events)
                is EventState.Error -> showMessage(state.message, canRetry = true)
            }
        }
    }

    private fun buttonListeners() {
        findViewById<MaterialButton>(R.id.buttonProfile).setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }

        findViewById<MaterialButton>(R.id.buttonLogout).setOnClickListener {
            authViewModel.logout()
            navigateToMain()
        }

        buttonRetryEvents.setOnClickListener {
            eventViewModel.loadEvents()
        }
    }

    private fun showLoading(isLoading: Boolean) {
        progressEvents.visibility = if (isLoading) View.VISIBLE else View.GONE

        if (isLoading) {
            recyclerEvents.visibility = View.GONE
            layoutEventMessage.visibility = View.GONE
        }
    }

    private fun showEvents(events: List<Event>) {
        showLoading(false)

        if (events.isEmpty()) {
            showMessage("Nenhum evento disponível no momento.", canRetry = false)
            return
        }

        eventAdapter.updateEvents(events)
        recyclerEvents.visibility = View.VISIBLE
        layoutEventMessage.visibility = View.GONE
    }

    private fun showMessage(message: String, canRetry: Boolean) {
        showLoading(false)
        recyclerEvents.visibility = View.GONE
        layoutEventMessage.visibility = View.VISIBLE
        textEventMessage.text = message
        buttonRetryEvents.visibility = if (canRetry) View.VISIBLE else View.GONE
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
    }
}
