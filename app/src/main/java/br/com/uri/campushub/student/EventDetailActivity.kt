package br.com.uri.campushub.student

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import br.com.uri.campushub.databinding.ActivityEventDetailBinding
import br.com.uri.campushub.model.Comment
import br.com.uri.campushub.model.Event
import br.com.uri.campushub.viewmodel.CommentActionState
import br.com.uri.campushub.viewmodel.CommentListState
import br.com.uri.campushub.viewmodel.CommentViewModel
import br.com.uri.campushub.viewmodel.EventDetailState
import br.com.uri.campushub.viewmodel.EventDetailViewModel
import br.com.uri.campushub.viewmodel.FavoriteState
import br.com.uri.campushub.viewmodel.FavoriteViewModel
import br.com.uri.campushub.viewmodel.RegistrationState
import br.com.uri.campushub.viewmodel.RegistrationViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

class EventDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_EVENT_ID = "event_id"
    }

    private lateinit var binding: ActivityEventDetailBinding
    private lateinit var eventDetailViewModel: EventDetailViewModel
    private lateinit var registrationViewModel: RegistrationViewModel
    private lateinit var favoriteViewModel: FavoriteViewModel
    private lateinit var commentViewModel: CommentViewModel
    private lateinit var commentAdapter: CommentAdapter
    private lateinit var eventId: String
    private var editingCommentId: String? = null
    private var currentEvent: Event? = null
    private var currentRegistrationStatus: Boolean? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEventDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        eventId = intent.getStringExtra(EXTRA_EVENT_ID).orEmpty()

        setupViewModel()
        setupCommentRecyclerView()
        setupListeners()
        observeEventDetailState()
        observeRegistrationState()
        observeFavoriteState()
        observeCommentListState()
        observeCommentActionState()

        if (eventId.isBlank()) {
            showError("Evento inválido.")
        } else {
            eventDetailViewModel.loadEvent(eventId)
            registrationViewModel.loadStatus(eventId)
            favoriteViewModel.loadStatus(eventId)
            commentViewModel.loadComments(eventId)
        }
    }

    private fun setupViewModel() {
        eventDetailViewModel = ViewModelProvider(this)[EventDetailViewModel::class.java]
        registrationViewModel = ViewModelProvider(this)[RegistrationViewModel::class.java]
        favoriteViewModel = ViewModelProvider(this)[FavoriteViewModel::class.java]
        commentViewModel = ViewModelProvider(this)[CommentViewModel::class.java]
    }

    private fun setupCommentRecyclerView() {
        commentAdapter = CommentAdapter(
            currentUserId = commentViewModel.currentUserId(),
            onEditComment = ::startEditingComment,
            onDeleteComment = ::confirmCommentDeletion
        )
        binding.recyclerComments.layoutManager = LinearLayoutManager(this)
        binding.recyclerComments.adapter = commentAdapter
    }

    private fun setupListeners() {
        binding.buttonBack.setOnClickListener {
            finish()
        }

        binding.buttonBackFromError.setOnClickListener {
            finish()
        }

        binding.buttonRetryDetail.setOnClickListener {
            if (eventId.isNotBlank()) {
                eventDetailViewModel.loadEvent(eventId)
                registrationViewModel.loadStatus(eventId)
                favoriteViewModel.loadStatus(eventId)
                commentViewModel.loadComments(eventId)
            }
        }

        binding.buttonRegistration.setOnClickListener {
            registrationViewModel.toggleRegistration(eventId)
        }

        binding.buttonFavorite.setOnClickListener {
            favoriteViewModel.toggleFavorite(eventId)
        }

        binding.buttonSubmitComment.setOnClickListener {
            submitComment()
        }

        binding.buttonCancelCommentEdit.setOnClickListener {
            resetCommentEditor()
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
                    binding.buttonRegistration.visibility = View.GONE
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
                        ?: run { binding.buttonRegistration.visibility = View.GONE }
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun observeFavoriteState() {
        favoriteViewModel.favoriteState.observe(this) { state ->
            when (state) {
                FavoriteState.Idle,
                FavoriteState.Loading -> {
                    binding.buttonFavorite.visibility = View.GONE
                }

                is FavoriteState.Status -> {
                    renderFavoriteButton(state.isFavorite)
                }

                is FavoriteState.Updating -> {
                    showFavoriteLoading(state.isFavorite)
                }

                is FavoriteState.Success -> {
                    renderFavoriteButton(state.isFavorite)
                    Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                }

                is FavoriteState.Error -> {
                    state.isFavorite?.let(::renderFavoriteButton)
                        ?: run { binding.buttonFavorite.visibility = View.GONE }
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun observeCommentListState() {
        commentViewModel.commentListState.observe(this) { state ->
            when (state) {
                CommentListState.Idle -> showCommentsLoading(false)
                CommentListState.Loading -> showCommentsLoading(true)
                is CommentListState.Success -> showComments(state.comments)
                is CommentListState.Error -> showCommentsError(state.message)
            }
        }
    }

    private fun observeCommentActionState() {
        commentViewModel.commentActionState.observe(this) { state ->
            when (state) {
                CommentActionState.Idle -> setCommentActionLoading(false)
                CommentActionState.Loading -> setCommentActionLoading(true)
                is CommentActionState.Success -> {
                    setCommentActionLoading(false)
                    resetCommentEditor()
                    Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                    commentViewModel.clearActionState()
                }
                is CommentActionState.Error -> {
                    setCommentActionLoading(false)
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                    commentViewModel.clearActionState()
                }
            }
        }
    }

    private fun showCommentsLoading(isLoading: Boolean) {
        binding.progressComments.visibility = if (isLoading) View.VISIBLE else View.GONE

        if (isLoading) {
            binding.recyclerComments.visibility = View.GONE
            binding.textCommentsMessage.visibility = View.GONE
        }
    }

    private fun showComments(comments: List<Comment>) {
        showCommentsLoading(false)
        commentAdapter.submitList(comments)

        if (comments.isEmpty()) {
            binding.recyclerComments.visibility = View.GONE
            binding.textCommentsMessage.visibility = View.VISIBLE
            binding.textCommentsMessage.text = "Nenhum comentário publicado."
        } else {
            binding.recyclerComments.visibility = View.VISIBLE
            binding.textCommentsMessage.visibility = View.GONE
        }
    }

    private fun showCommentsError(message: String) {
        showCommentsLoading(false)
        binding.recyclerComments.visibility = View.GONE
        binding.textCommentsMessage.visibility = View.VISIBLE
        binding.textCommentsMessage.text = message
    }

    private fun submitComment() {
        binding.layoutCommentInput.error = null

        val text = binding.inputComment.text?.toString()?.trim().orEmpty()

        if (text.isBlank()) {
            binding.layoutCommentInput.error = "Escreva um comentário."
            return
        }

        val commentId = editingCommentId

        if (commentId == null) {
            commentViewModel.createComment(eventId, text)
        } else {
            commentViewModel.updateComment(eventId, commentId, text)
        }
    }

    private fun startEditingComment(comment: Comment) {
        editingCommentId = comment.id
        binding.layoutCommentInput.error = null
        binding.inputComment.setText(comment.text)
        binding.inputComment.setSelection(comment.text.length)
        binding.buttonSubmitComment.text = "Salvar alteração"
        binding.buttonCancelCommentEdit.visibility = View.VISIBLE
        binding.inputComment.requestFocus()
    }

    private fun resetCommentEditor() {
        editingCommentId = null
        binding.layoutCommentInput.error = null
        binding.inputComment.text?.clear()
        binding.buttonSubmitComment.text = "Publicar comentário"
        binding.buttonCancelCommentEdit.visibility = View.GONE
    }

    private fun confirmCommentDeletion(comment: Comment) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Excluir comentário?")
            .setMessage("Esta ação não poderá ser desfeita.")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Excluir") { _, _ ->
                commentViewModel.deleteComment(eventId, comment.id)
            }
            .show()
    }

    private fun setCommentActionLoading(isLoading: Boolean) {
        binding.buttonSubmitComment.isEnabled = !isLoading
        binding.buttonCancelCommentEdit.isEnabled = !isLoading
        binding.inputComment.isEnabled = !isLoading
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressEventDetail.visibility = if (isLoading) View.VISIBLE else View.GONE

        if (isLoading) {
            binding.scrollEventDetail.visibility = View.GONE
            binding.layoutDetailError.visibility = View.GONE
        }
    }

    private fun showEvent(event: Event) {
        showLoading(false)
        currentEvent = event

        binding.textDetailCategory.text = event.category.ifBlank { "Evento" }
        binding.textDetailTitle.text = event.title
        binding.textDetailDescription.text = event.description.ifBlank { "Evento sem descrição." }
        binding.textDetailStart.text = formatDate(event.startAt)
        binding.textDetailEnd.text = formatDate(event.endAt)
        binding.textDetailLocation.text = event.location.ifBlank { "Local a definir" }
        binding.textDetailOrganizer.text = event.organizer.ifBlank { "Organizador não informado" }
        binding.textDetailParticipants.text = formatParticipants(event)

        binding.layoutDetailError.visibility = View.GONE
        binding.scrollEventDetail.visibility = View.VISIBLE
        currentRegistrationStatus?.let(::renderRegistrationButton)
    }

    private fun showError(message: String) {
        showLoading(false)
        binding.scrollEventDetail.visibility = View.GONE
        binding.layoutDetailError.visibility = View.VISIBLE
        binding.textDetailError.text = message
    }

    private fun renderRegistrationButton(isRegistered: Boolean) {
        currentRegistrationStatus = isRegistered

        val event = currentEvent ?: return
        val isFull = event.maxParticipants > 0 &&
            event.participantCount >= event.maxParticipants

        binding.buttonRegistration.visibility = View.VISIBLE
        binding.buttonRegistration.isEnabled = isRegistered || !isFull
        binding.buttonRegistration.text = when {
            isRegistered -> "Cancelar inscrição"
            isFull -> "Evento lotado"
            else -> "Inscrever-se"
        }
    }

    private fun showRegistrationLoading(isRegistered: Boolean) {
        binding.buttonRegistration.visibility = View.VISIBLE
        binding.buttonRegistration.isEnabled = false
        binding.buttonRegistration.text = if (isRegistered) {
            "Cancelando inscrição..."
        } else {
            "Realizando inscrição..."
        }
    }

    private fun renderFavoriteButton(isFavorite: Boolean) {
        binding.buttonFavorite.visibility = View.VISIBLE
        binding.buttonFavorite.isEnabled = true
        binding.buttonFavorite.text = if (isFavorite) {
            "Remover dos favoritos"
        } else {
            "Favoritar evento"
        }
    }

    private fun showFavoriteLoading(isFavorite: Boolean) {
        binding.buttonFavorite.visibility = View.VISIBLE
        binding.buttonFavorite.isEnabled = false
        binding.buttonFavorite.text = if (isFavorite) {
            "Removendo dos favoritos..."
        } else {
            "Favoritando evento..."
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
