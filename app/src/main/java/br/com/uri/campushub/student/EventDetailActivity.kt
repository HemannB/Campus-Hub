package br.com.uri.campushub.student

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import br.com.uri.campushub.databinding.ActivityEventDetailBinding
import br.com.uri.campushub.model.Comment
import br.com.uri.campushub.model.Event
import br.com.uri.campushub.model.EventPublication
import br.com.uri.campushub.model.PublicationType
import br.com.uri.campushub.model.RatingSummary
import br.com.uri.campushub.viewmodel.CommentActionState
import br.com.uri.campushub.viewmodel.CommentListState
import br.com.uri.campushub.viewmodel.CommentViewModel
import br.com.uri.campushub.viewmodel.EventDetailState
import br.com.uri.campushub.viewmodel.EventDetailViewModel
import br.com.uri.campushub.viewmodel.FavoriteState
import br.com.uri.campushub.viewmodel.FavoriteViewModel
import br.com.uri.campushub.viewmodel.PublicationActionState
import br.com.uri.campushub.viewmodel.PublicationListState
import br.com.uri.campushub.viewmodel.PublicationViewModel
import br.com.uri.campushub.viewmodel.RatingActionState
import br.com.uri.campushub.viewmodel.RatingSummaryState
import br.com.uri.campushub.viewmodel.RatingViewModel
import br.com.uri.campushub.viewmodel.RegistrationState
import br.com.uri.campushub.viewmodel.RegistrationViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.Timestamp
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID

class EventDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_EVENT_ID = "event_id"
        private const val STATE_PENDING_PHOTO_PATH = "pending_photo_path"
        private const val STATE_PENDING_AUDIO_PATH = "pending_audio_path"
    }

    private lateinit var binding: ActivityEventDetailBinding
    private lateinit var eventDetailViewModel: EventDetailViewModel
    private lateinit var registrationViewModel: RegistrationViewModel
    private lateinit var favoriteViewModel: FavoriteViewModel
    private lateinit var commentViewModel: CommentViewModel
    private lateinit var ratingViewModel: RatingViewModel
    private lateinit var publicationViewModel: PublicationViewModel
    private lateinit var commentAdapter: CommentAdapter
    private lateinit var publicationAdapter: PublicationAdapter
    private lateinit var eventId: String
    private var editingCommentId: String? = null
    private var currentEvent: Event? = null
    private var currentRegistrationStatus: Boolean? = null
    private var currentRatingSummary = RatingSummary()
    private var isRatingSummaryLoaded = false
    private var ratingSummaryError: String? = null
    private var isRatingActionLoading = false
    private var pendingPhotoFile: File? = null
    private var pendingAudioFile: File? = null
    private var audioRecorder: MediaRecorder? = null
    private var isRecordingAudio = false
    private var isPublicationActionLoading = false

    private val photoPicker = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null && ::eventId.isInitialized) {
            publicationViewModel.publish(eventId, PublicationType.PHOTO, uri)
        }
    }

    private val takePhoto = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { wasCaptured ->
        val photoFile = pendingPhotoFile

        if (wasCaptured && photoFile != null && photoFile.exists()) {
            val photoUri = FileProvider.getUriForFile(
                this,
                "$packageName.fileprovider",
                photoFile
            )
            publicationViewModel.publish(eventId, PublicationType.PHOTO, photoUri)
        } else {
            clearPendingPhoto()
        }
    }

    private val requestAudioPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { wasGranted ->
        if (wasGranted) {
            startAudioRecording()
        } else {
            Toast.makeText(
                this,
                "A permissão do microfone é necessária para gravar áudio.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEventDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        eventId = intent.getStringExtra(EXTRA_EVENT_ID).orEmpty()
        pendingPhotoFile = savedInstanceState
            ?.getString(STATE_PENDING_PHOTO_PATH)
            ?.let(::File)
        pendingAudioFile = savedInstanceState
            ?.getString(STATE_PENDING_AUDIO_PATH)
            ?.let(::File)

        setupViewModel()
        setupCommentRecyclerView()
        setupPublicationRecyclerView()
        setupListeners()
        observeEventDetailState()
        observeRegistrationState()
        observeFavoriteState()
        observeCommentListState()
        observeCommentActionState()
        observeRatingSummaryState()
        observeRatingActionState()
        observePublicationListState()
        observePublicationActionState()

        if (eventId.isBlank()) {
            showError("Evento inválido.")
        } else {
            eventDetailViewModel.loadEvent(eventId)
            registrationViewModel.loadStatus(eventId)
            favoriteViewModel.loadStatus(eventId)
            commentViewModel.loadComments(eventId)
            ratingViewModel.loadRatingSummary(eventId)
            publicationViewModel.loadPublications(eventId)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        pendingPhotoFile?.let { file ->
            outState.putString(STATE_PENDING_PHOTO_PATH, file.absolutePath)
        }
        pendingAudioFile?.let { file ->
            outState.putString(STATE_PENDING_AUDIO_PATH, file.absolutePath)
        }
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        if (isRecordingAudio) cancelAudioRecording()
        super.onDestroy()
    }

    private fun setupViewModel() {
        eventDetailViewModel = ViewModelProvider(this)[EventDetailViewModel::class.java]
        registrationViewModel = ViewModelProvider(this)[RegistrationViewModel::class.java]
        favoriteViewModel = ViewModelProvider(this)[FavoriteViewModel::class.java]
        commentViewModel = ViewModelProvider(this)[CommentViewModel::class.java]
        ratingViewModel = ViewModelProvider(this)[RatingViewModel::class.java]
        publicationViewModel = ViewModelProvider(this)[PublicationViewModel::class.java]
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

    private fun setupPublicationRecyclerView() {
        publicationAdapter = PublicationAdapter(
            currentUserId = publicationViewModel.currentUserId(),
            onDeletePublication = ::confirmPublicationDeletion
        )
        binding.recyclerPublications.layoutManager = LinearLayoutManager(this)
        binding.recyclerPublications.adapter = publicationAdapter
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
                ratingViewModel.loadRatingSummary(eventId)
                publicationViewModel.loadPublications(eventId)
            }
        }

        binding.buttonRegistration.setOnClickListener {
            registrationViewModel.toggleRegistration(eventId)
        }

        binding.buttonFavorite.setOnClickListener {
            favoriteViewModel.toggleFavorite(eventId)
        }

        binding.buttonViewMap.setOnClickListener {
            val event = currentEvent ?: return@setOnClickListener

            if (event.location.isBlank()) {
                Toast.makeText(this, "Este evento ainda não possui endereço.", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            val intent = Intent(this, EventMapActivity::class.java).apply {
                putExtra(EventMapActivity.EXTRA_EVENT_TITLE, event.title)
                putExtra(EventMapActivity.EXTRA_EVENT_ADDRESS, event.location)
            }
            startActivity(intent)
        }

        binding.buttonChoosePhoto.setOnClickListener {
            photoPicker.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }

        binding.buttonTakePhoto.setOnClickListener {
            launchCamera()
        }

        binding.buttonRecordAudio.setOnClickListener {
            if (isRecordingAudio) {
                stopAndPublishAudio()
            } else {
                checkAudioPermissionAndRecord()
            }
        }

        binding.buttonSubmitComment.setOnClickListener {
            submitComment()
        }

        binding.buttonCancelCommentEdit.setOnClickListener {
            resetCommentEditor()
        }

        binding.buttonSaveRating.setOnClickListener {
            ratingViewModel.saveRating(
                eventId = eventId,
                score = binding.ratingBarEvent.rating.toInt()
            )
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
                    currentRegistrationStatus = null
                    binding.buttonRegistration.visibility = View.GONE
                    renderRatingAvailability()
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
                        ?: run {
                            currentRegistrationStatus = null
                            binding.buttonRegistration.visibility = View.GONE
                            renderRatingAvailability()
                        }
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

    private fun observeRatingSummaryState() {
        ratingViewModel.summaryState.observe(this) { state ->
            when (state) {
                RatingSummaryState.Idle -> {
                    showRatingLoading(false)
                    renderRatingAvailability()
                }

                RatingSummaryState.Loading -> {
                    isRatingSummaryLoaded = false
                    ratingSummaryError = null
                    showRatingLoading(true)
                    renderRatingAvailability()
                }

                is RatingSummaryState.Success -> {
                    currentRatingSummary = state.summary
                    isRatingSummaryLoaded = true
                    ratingSummaryError = null
                    showRatingLoading(false)
                    renderRatingSummary(state.summary)
                    renderRatingAvailability()
                }

                is RatingSummaryState.Error -> {
                    isRatingSummaryLoaded = false
                    ratingSummaryError = state.message
                    showRatingLoading(false)
                    renderRatingAvailability()
                }
            }
        }
    }

    private fun observeRatingActionState() {
        ratingViewModel.actionState.observe(this) { state ->
            when (state) {
                RatingActionState.Idle -> setRatingActionLoading(false)
                RatingActionState.Loading -> setRatingActionLoading(true)
                is RatingActionState.Success -> {
                    setRatingActionLoading(false)
                    Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                    ratingViewModel.clearActionState()
                }

                is RatingActionState.Error -> {
                    setRatingActionLoading(false)
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                    ratingViewModel.clearActionState()
                }
            }
        }
    }

    private fun observePublicationListState() {
        publicationViewModel.listState.observe(this) { state ->
            when (state) {
                PublicationListState.Idle -> showPublicationsLoading(false)
                PublicationListState.Loading -> showPublicationsLoading(true)
                is PublicationListState.Success -> showPublications(state.publications)
                is PublicationListState.Error -> showPublicationsError(state.message)
            }
        }
    }

    private fun observePublicationActionState() {
        publicationViewModel.actionState.observe(this) { state ->
            when (state) {
                PublicationActionState.Idle -> setPublicationActionLoading(false)
                PublicationActionState.Loading -> setPublicationActionLoading(true)
                is PublicationActionState.Success -> {
                    setPublicationActionLoading(false)
                    clearPendingPhoto()
                    clearPendingAudio()
                    Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                    publicationViewModel.clearActionState()
                }

                is PublicationActionState.Error -> {
                    setPublicationActionLoading(false)
                    clearPendingPhoto()
                    clearPendingAudio()
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                    publicationViewModel.clearActionState()
                }
            }
        }
    }

    private fun showPublicationsLoading(isLoading: Boolean) {
        binding.progressPublications.visibility = if (isLoading) View.VISIBLE else View.GONE

        if (isLoading) {
            binding.recyclerPublications.visibility = View.GONE
            binding.textPublicationsMessage.visibility = View.GONE
        }
    }

    private fun showPublications(publications: List<EventPublication>) {
        showPublicationsLoading(false)
        publicationAdapter.submitList(publications)

        if (publications.isEmpty()) {
            binding.recyclerPublications.visibility = View.GONE
            binding.textPublicationsMessage.visibility = View.VISIBLE
            binding.textPublicationsMessage.text = "Nenhuma publicação neste evento."
        } else {
            binding.recyclerPublications.visibility = View.VISIBLE
            binding.textPublicationsMessage.visibility = View.GONE
        }
    }

    private fun showPublicationsError(message: String) {
        showPublicationsLoading(false)
        binding.recyclerPublications.visibility = View.GONE
        binding.textPublicationsMessage.visibility = View.VISIBLE
        binding.textPublicationsMessage.text = message
    }

    private fun confirmPublicationDeletion(publication: EventPublication) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Excluir publicação?")
            .setMessage("O arquivo será removido deste dispositivo.")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Excluir") { _, _ ->
                publicationViewModel.deletePublication(eventId, publication.id)
            }
            .show()
    }

    private fun setPublicationActionLoading(isLoading: Boolean) {
        isPublicationActionLoading = isLoading
        renderPublicationButtons()
        binding.buttonChoosePhoto.text = if (isLoading) {
            "Salvando publicação..."
        } else {
            "Escolher foto da galeria"
        }
    }

    private fun renderPublicationButtons() {
        binding.buttonTakePhoto.isEnabled = !isPublicationActionLoading && !isRecordingAudio
        binding.buttonChoosePhoto.isEnabled = !isPublicationActionLoading && !isRecordingAudio
        binding.buttonRecordAudio.isEnabled = !isPublicationActionLoading
        binding.buttonRecordAudio.text = if (isRecordingAudio) {
            "Parar e publicar áudio"
        } else {
            "Gravar áudio (até 30s)"
        }
    }

    private fun checkAudioPermissionAndRecord() {
        if (
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            startAudioRecording()
        } else {
            requestAudioPermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun startAudioRecording() {
        clearPendingAudio()

        val pendingDirectory = File(cacheDir, "pending_publication_audio")
        if (!pendingDirectory.exists() && !pendingDirectory.mkdirs()) {
            Toast.makeText(this, "Não foi possível preparar a gravação.", Toast.LENGTH_LONG).show()
            return
        }

        val audioFile = File(pendingDirectory, "${UUID.randomUUID()}.m4a")
        val recorder = createMediaRecorder()
        pendingAudioFile = audioFile
        audioRecorder = recorder

        val result = runCatching {
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            recorder.setAudioEncodingBitRate(128_000)
            recorder.setAudioSamplingRate(44_100)
            recorder.setMaxDuration(30_000)
            recorder.setOutputFile(audioFile.absolutePath)
            recorder.setOnInfoListener { _, what, _ ->
                if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                    runOnUiThread { stopAndPublishAudio() }
                }
            }
            recorder.prepare()
            recorder.start()
        }

        result.onSuccess {
            isRecordingAudio = true
            renderPublicationButtons()
            Toast.makeText(this, "Gravação iniciada.", Toast.LENGTH_SHORT).show()
        }.onFailure {
            recorder.release()
            audioRecorder = null
            clearPendingAudio()
            Toast.makeText(this, "Não foi possível iniciar a gravação.", Toast.LENGTH_LONG).show()
        }
    }

    private fun stopAndPublishAudio() {
        val recorder = audioRecorder ?: return
        val audioFile = pendingAudioFile
        audioRecorder = null

        val wasStopped = runCatching { recorder.stop() }.isSuccess
        recorder.release()
        isRecordingAudio = false
        renderPublicationButtons()

        if (!wasStopped || audioFile == null || !audioFile.exists() || audioFile.length() == 0L) {
            clearPendingAudio()
            Toast.makeText(this, "O áudio ficou muito curto. Tente novamente.", Toast.LENGTH_LONG)
                .show()
            return
        }

        val audioUri = FileProvider.getUriForFile(
            this,
            "$packageName.fileprovider",
            audioFile
        )
        publicationViewModel.publish(eventId, PublicationType.AUDIO, audioUri)
    }

    private fun cancelAudioRecording() {
        audioRecorder?.let { recorder ->
            runCatching { recorder.stop() }
            recorder.release()
        }
        audioRecorder = null
        isRecordingAudio = false
        clearPendingAudio()
    }

    private fun clearPendingAudio() {
        pendingAudioFile?.delete()
        pendingAudioFile = null
    }

    @Suppress("DEPRECATION")
    private fun createMediaRecorder(): MediaRecorder {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(this)
        } else {
            MediaRecorder()
        }
    }

    private fun launchCamera() {
        clearPendingPhoto()

        val pendingDirectory = File(cacheDir, "pending_publication_photos")
        if (!pendingDirectory.exists() && !pendingDirectory.mkdirs()) {
            Toast.makeText(this, "Não foi possível preparar a câmera.", Toast.LENGTH_LONG).show()
            return
        }

        val photoFile = File(pendingDirectory, "${UUID.randomUUID()}.jpg")
        pendingPhotoFile = photoFile
        val photoUri = FileProvider.getUriForFile(
            this,
            "$packageName.fileprovider",
            photoFile
        )

        runCatching { takePhoto.launch(photoUri) }
            .onFailure {
                clearPendingPhoto()
                Toast.makeText(
                    this,
                    "Nenhum aplicativo de câmera foi encontrado.",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun clearPendingPhoto() {
        pendingPhotoFile?.delete()
        pendingPhotoFile = null
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

    private fun showRatingLoading(isLoading: Boolean) {
        binding.progressRating.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun renderRatingSummary(summary: RatingSummary) {
        if (summary.count == 0) {
            binding.textRatingAverage.text = "—"
            binding.textRatingCount.text = "Nenhuma avaliação"
        } else {
            binding.textRatingAverage.text = String.format(
                Locale.forLanguageTag("pt-BR"),
                "%.1f",
                summary.average
            )
            binding.textRatingCount.text = if (summary.count == 1) {
                "1 avaliação"
            } else {
                "${summary.count} avaliações"
            }
        }

        binding.ratingBarEvent.rating = (summary.currentUserScore ?: 0).toFloat()
        binding.textRatingPrompt.text = if (summary.currentUserScore == null) {
            "Como foi sua experiência?"
        } else {
            "Sua avaliação"
        }
        updateRatingButtonText()
    }

    private fun renderRatingAvailability() {
        val event = currentEvent
        val canRate = event != null &&
            isEventFinished(event) &&
            currentRegistrationStatus == true &&
            isRatingSummaryLoaded

        binding.layoutRatingInput.visibility = if (canRate) View.VISIBLE else View.GONE

        val message = when {
            event == null -> null
            ratingSummaryError != null -> ratingSummaryError
            event.endAt == null ->
                "A avaliação ficará disponível quando o término for informado."
            !isEventFinished(event) ->
                "As avaliações serão liberadas depois que o evento terminar."
            currentRegistrationStatus == null -> "Verificando sua inscrição..."
            currentRegistrationStatus == false ->
                "Somente alunos inscritos podem avaliar este evento."
            !isRatingSummaryLoaded -> "Carregando avaliações..."
            else -> null
        }

        binding.textRatingMessage.text = message
        binding.textRatingMessage.visibility = if (message == null) {
            View.GONE
        } else {
            View.VISIBLE
        }

        binding.ratingBarEvent.isEnabled = canRate && !isRatingActionLoading
        binding.buttonSaveRating.isEnabled = canRate && !isRatingActionLoading
    }

    private fun setRatingActionLoading(isLoading: Boolean) {
        isRatingActionLoading = isLoading
        binding.ratingBarEvent.isEnabled = !isLoading
        binding.buttonSaveRating.isEnabled = !isLoading

        if (isLoading) {
            binding.buttonSaveRating.text = "Salvando avaliação..."
        } else {
            updateRatingButtonText()
            renderRatingAvailability()
        }
    }

    private fun updateRatingButtonText() {
        binding.buttonSaveRating.text = if (currentRatingSummary.currentUserScore == null) {
            "Salvar avaliação"
        } else {
            "Atualizar avaliação"
        }
    }

    private fun isEventFinished(event: Event): Boolean {
        return event.endAt?.compareTo(Timestamp.now())?.let { comparison ->
            comparison <= 0
        } ?: false
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
        binding.buttonViewMap.visibility = if (event.location.isBlank()) View.GONE else View.VISIBLE
        binding.textDetailOrganizer.text = event.organizer.ifBlank { "Organizador não informado" }
        binding.textDetailParticipants.text = formatParticipants(event)

        binding.layoutDetailError.visibility = View.GONE
        binding.scrollEventDetail.visibility = View.VISIBLE
        currentRegistrationStatus?.let(::renderRegistrationButton)
        renderRatingAvailability()
    }

    private fun showError(message: String) {
        showLoading(false)
        binding.scrollEventDetail.visibility = View.GONE
        binding.layoutDetailError.visibility = View.VISIBLE
        binding.textDetailError.text = message
    }

    private fun renderRegistrationButton(isRegistered: Boolean) {
        currentRegistrationStatus = isRegistered
        renderRatingAvailability()

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
