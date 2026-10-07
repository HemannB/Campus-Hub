package br.com.uri.campushub.student

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import br.com.uri.campushub.MainActivity
import br.com.uri.campushub.databinding.ActivityHomeBinding
import br.com.uri.campushub.model.Event
import br.com.uri.campushub.viewmodel.AuthViewModel
import br.com.uri.campushub.viewmodel.EventState
import br.com.uri.campushub.viewmodel.EventSituationFilter
import br.com.uri.campushub.viewmodel.EventViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var authViewModel: AuthViewModel
    private lateinit var eventViewModel: EventViewModel
    private lateinit var eventAdapter: EventAdapter
    private var availableCategories: List<String> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupViewModels()
        observeEventState()
        observeCategories()
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
                is EventState.Success -> showEvents(state.events, state.isFiltered)
                is EventState.Error -> showMessage(state.message, canRetry = true)
            }
        }
    }

    private fun observeCategories() {
        eventViewModel.categories.observe(this) { categories ->
            availableCategories = categories
            updateCategoryFilterButton(eventViewModel.categoryFilter)
        }
    }

    private fun buttonListeners() {
        binding.inputSearchEvents.doAfterTextChanged { text ->
            eventViewModel.setSearchQuery(text?.toString().orEmpty())
        }

        binding.buttonCategoryFilter.setOnClickListener {
            showCategoryFilterDialog()
        }

        binding.buttonSituationFilter.setOnClickListener {
            showSituationFilterDialog()
        }

        updateSituationFilterButton(eventViewModel.situationFilter)

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

    private fun showCategoryFilterDialog() {
        val allCategoriesLabel = "Todas as categorias"
        val options = listOf(allCategoriesLabel) + availableCategories
        val selectedIndex = eventViewModel.categoryFilter
            ?.let(options::indexOf)
            ?.takeIf { index -> index >= 0 }
            ?: 0

        MaterialAlertDialogBuilder(this)
            .setTitle("Filtrar por categoria")
            .setSingleChoiceItems(options.toTypedArray(), selectedIndex) { dialog, index ->
                val category = options[index].takeUnless { index == 0 }
                eventViewModel.setCategory(category)
                updateCategoryFilterButton(category)
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showSituationFilterDialog() {
        val options = arrayOf("Todos os eventos", "Próximos", "Encerrados")
        val selectedIndex = when (eventViewModel.situationFilter) {
            EventSituationFilter.ALL -> 0
            EventSituationFilter.UPCOMING -> 1
            EventSituationFilter.FINISHED -> 2
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Filtrar por situação")
            .setSingleChoiceItems(options, selectedIndex) { dialog, index ->
                val situation = when (index) {
                    1 -> EventSituationFilter.UPCOMING
                    2 -> EventSituationFilter.FINISHED
                    else -> EventSituationFilter.ALL
                }

                eventViewModel.setSituation(situation)
                updateSituationFilterButton(situation)
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun updateCategoryFilterButton(category: String?) {
        binding.buttonCategoryFilter.text = category
            ?.let { selectedCategory -> "Categoria: $selectedCategory" }
            ?: "Categoria: todas"
    }

    private fun updateSituationFilterButton(situation: EventSituationFilter) {
        binding.buttonSituationFilter.text = when (situation) {
            EventSituationFilter.ALL -> "Situação: todas"
            EventSituationFilter.UPCOMING -> "Situação: próximos"
            EventSituationFilter.FINISHED -> "Situação: encerrados"
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressEvents.visibility = if (isLoading) View.VISIBLE else View.GONE

        if (isLoading) {
            binding.recyclerEvents.visibility = View.GONE
            binding.layoutEventMessage.visibility = View.GONE
        }
    }

    private fun showEvents(events: List<Event>, isFiltered: Boolean) {
        showLoading(false)

        if (events.isEmpty()) {
            val message = if (isFiltered) {
                "Nenhum evento corresponde à busca e aos filtros."
            } else {
                "Nenhum evento disponível no momento."
            }
            showMessage(message, canRetry = false)
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
