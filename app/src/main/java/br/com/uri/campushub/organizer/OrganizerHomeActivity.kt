package br.com.uri.campushub.organizer

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import br.com.uri.campushub.MainActivity
import br.com.uri.campushub.databinding.ActivityOrganizerHomeBinding
import br.com.uri.campushub.viewmodel.AuthViewModel
import br.com.uri.campushub.viewmodel.EventState
import br.com.uri.campushub.viewmodel.OrganizerEventsViewModel

class OrganizerHomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOrganizerHomeBinding
    private lateinit var authViewModel: AuthViewModel
    private lateinit var eventsViewModel: OrganizerEventsViewModel
    private val eventAdapter = OrganizerEventAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOrganizerHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authViewModel = ViewModelProvider(this)[AuthViewModel::class.java]
        eventsViewModel = ViewModelProvider(this)[OrganizerEventsViewModel::class.java]
        binding.recyclerOrganizerEvents.layoutManager = LinearLayoutManager(this)
        binding.recyclerOrganizerEvents.adapter = eventAdapter
        binding.buttonRefreshEvents.setOnClickListener { eventsViewModel.loadEvents() }
        observeEvents()

        binding.buttonLogout.setOnClickListener {
            authViewModel.logout()
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        eventsViewModel.loadEvents()
    }

    private fun observeEvents() {
        eventsViewModel.eventState.observe(this) { state ->
            val isLoading = state is EventState.Loading
            binding.progressEvents.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.buttonRefreshEvents.isEnabled = !isLoading
            binding.recyclerOrganizerEvents.visibility = View.GONE
            binding.textEventsMessage.visibility = View.GONE

            when (state) {
                EventState.Idle, EventState.Loading -> Unit
                is EventState.Success -> {
                    eventAdapter.submitList(state.events)
                    if (state.events.isEmpty()) {
                        binding.textEventsMessage.text = "Você ainda não tem eventos."
                        binding.textEventsMessage.visibility = View.VISIBLE
                    } else {
                        binding.recyclerOrganizerEvents.visibility = View.VISIBLE
                    }
                }
                is EventState.Error -> {
                    binding.textEventsMessage.text = state.message
                    binding.textEventsMessage.visibility = View.VISIBLE
                }
            }
        }
    }
}
