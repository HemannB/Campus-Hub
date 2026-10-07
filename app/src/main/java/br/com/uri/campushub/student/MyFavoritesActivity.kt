package br.com.uri.campushub.student

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import br.com.uri.campushub.databinding.ActivityMyFavoritesBinding
import br.com.uri.campushub.model.Event
import br.com.uri.campushub.viewmodel.EventState
import br.com.uri.campushub.viewmodel.MyFavoritesViewModel

class MyFavoritesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMyFavoritesBinding
    private lateinit var viewModel: MyFavoritesViewModel
    private lateinit var eventAdapter: EventAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyFavoritesBinding.inflate(layoutInflater)
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
        binding.recyclerFavorites.layoutManager = LinearLayoutManager(this)
        binding.recyclerFavorites.adapter = eventAdapter
    }

    private fun setupViewModel() {
        viewModel = ViewModelProvider(this)[MyFavoritesViewModel::class.java]
    }

    private fun setupListeners() {
        binding.buttonBackFavorites.setOnClickListener {
            finish()
        }

        binding.buttonRetryFavorites.setOnClickListener {
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
        binding.progressFavorites.visibility = if (isLoading) View.VISIBLE else View.GONE

        if (isLoading) {
            binding.recyclerFavorites.visibility = View.GONE
            binding.layoutFavoritesMessage.visibility = View.GONE
        }
    }

    private fun showEvents(events: List<Event>) {
        showLoading(false)

        if (events.isEmpty()) {
            showMessage("Você ainda não favoritou nenhum evento.", canRetry = false)
            return
        }

        eventAdapter.updateEvents(events)
        binding.recyclerFavorites.visibility = View.VISIBLE
        binding.layoutFavoritesMessage.visibility = View.GONE
    }

    private fun showMessage(message: String, canRetry: Boolean) {
        showLoading(false)
        binding.recyclerFavorites.visibility = View.GONE
        binding.layoutFavoritesMessage.visibility = View.VISIBLE
        binding.textFavoritesMessage.text = message
        binding.buttonRetryFavorites.visibility = if (canRetry) View.VISIBLE else View.GONE
    }

    private fun navigateToEventDetails(eventId: String) {
        val intent = Intent(this, EventDetailActivity::class.java).apply {
            putExtra(EventDetailActivity.EXTRA_EVENT_ID, eventId)
        }
        startActivity(intent)
    }
}
