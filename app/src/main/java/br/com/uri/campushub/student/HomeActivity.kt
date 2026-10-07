package br.com.uri.campushub.student

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import br.com.uri.campushub.MainActivity
import br.com.uri.campushub.databinding.ActivityHomeBinding
import br.com.uri.campushub.model.Event
import br.com.uri.campushub.viewmodel.AuthViewModel
import br.com.uri.campushub.viewmodel.EventState
import br.com.uri.campushub.viewmodel.EventViewModel

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var authViewModel: AuthViewModel
    private lateinit var eventViewModel: EventViewModel
    private lateinit var eventAdapter: EventAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupViewModels()
        observeEventState()
        buttonListeners()
    }

    override fun onResume() {
        super.onResume()
        eventViewModel.loadEvents()
    }

    private fun setupRecyclerView() {
        eventAdapter = EventAdapter { event ->
            navigateToEventDetails(event.id)
        }
        binding.recyclerEvents.layoutManager = LinearLayoutManager(this)
        binding.recyclerEvents.adapter = eventAdapter
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
        binding.buttonMyEvents.setOnClickListener {
            val intent = Intent(this, MyEventsActivity::class.java)
            startActivity(intent)
        }

        binding.buttonMyFavorites.setOnClickListener {
            val intent = Intent(this, MyFavoritesActivity::class.java)
            startActivity(intent)
        }

        binding.buttonProfile.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }

        binding.buttonLogout.setOnClickListener {
            authViewModel.logout()
            navigateToMain()
        }

        binding.buttonRetryEvents.setOnClickListener {
            eventViewModel.loadEvents()
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressEvents.visibility = if (isLoading) View.VISIBLE else View.GONE

        if (isLoading) {
            binding.recyclerEvents.visibility = View.GONE
            binding.layoutEventMessage.visibility = View.GONE
        }
    }

    private fun showEvents(events: List<Event>) {
        showLoading(false)

        if (events.isEmpty()) {
            showMessage("Nenhum evento disponível no momento.", canRetry = false)
            return
        }

        eventAdapter.updateEvents(events)
        binding.recyclerEvents.visibility = View.VISIBLE
        binding.layoutEventMessage.visibility = View.GONE
    }

    private fun showMessage(message: String, canRetry: Boolean) {
        showLoading(false)
        binding.recyclerEvents.visibility = View.GONE
        binding.layoutEventMessage.visibility = View.VISIBLE
        binding.textEventMessage.text = message
        binding.buttonRetryEvents.visibility = if (canRetry) View.VISIBLE else View.GONE
    }

    private fun navigateToEventDetails(eventId: String) {
        val intent = Intent(this, EventDetailActivity::class.java).apply {
            putExtra(EventDetailActivity.EXTRA_EVENT_ID, eventId)
        }
        startActivity(intent)
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
    }
}
