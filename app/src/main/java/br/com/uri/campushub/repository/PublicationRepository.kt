package br.com.uri.campushub.repository

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.webkit.MimeTypeMap
import br.com.uri.campushub.model.EventPublication
import br.com.uri.campushub.model.PublicationType
import com.google.firebase.auth.FirebaseAuth
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

class PublicationRepository(
    context: Context,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    companion object {
        private const val PREFERENCES_NAME = "event_publications"
        private const val PUBLICATIONS_KEY = "publications"
        private const val PUBLICATIONS_DIRECTORY = "event_publications"
        private const val MAX_VIDEO_DURATION_MS = 30_000L
    }

    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    fun getPublications(eventId: String): Result<List<EventPublication>> = runCatching {
        require(eventId.isNotBlank()) { "Evento inválido." }

        readAll()
            .filter { publication -> publication.eventId == eventId }
            .sortedByDescending { publication -> publication.createdAt }
    }

    fun publishFromUri(
        eventId: String,
        authorName: String,
        type: PublicationType,
        sourceUri: Uri
    ): Result<EventPublication> = runCatching {
        val userId = requireAuthenticatedUser()
        require(eventId.isNotBlank()) { "Evento inválido." }
        require(authorName.isNotBlank()) { "Autor não identificado." }

        if (type == PublicationType.VIDEO) validateVideoDuration(sourceUri)

        val publicationId = UUID.randomUUID().toString()
        val extension = resolveExtension(sourceUri, type)
        val destination = createDestinationFile(eventId, publicationId, extension)

        try {
            appContext.contentResolver.openInputStream(sourceUri).use { input ->
                requireNotNull(input) { "Não foi possível abrir o arquivo selecionado." }
                destination.outputStream().use { output -> input.copyTo(output) }
            }

            val publication = EventPublication(
                id = publicationId,
                eventId = eventId,
                userId = userId,
                authorName = authorName,
                type = type,
                localPath = destination.absolutePath,
                createdAt = System.currentTimeMillis()
            )

            writeAll(readAll() + publication)
            publication
        } catch (exception: Exception) {
            destination.delete()
            throw exception
        }
    }

    fun deletePublication(publicationId: String): Result<Unit> = runCatching {
        val userId = requireAuthenticatedUser()
        val publications = readAll()
        val publication = publications.firstOrNull { item -> item.id == publicationId }
            ?: error("Publicação não encontrada.")

        check(publication.userId == userId) {
            "Você não pode excluir a publicação de outro usuário."
        }

        val mediaFile = File(publication.localPath)
        check(!mediaFile.exists() || mediaFile.delete()) {
            "Não foi possível excluir o arquivo da publicação."
        }

        writeAll(publications.filterNot { item -> item.id == publicationId })
    }

    private fun requireAuthenticatedUser(): String {
        return auth.currentUser?.uid ?: error("Usuário não autenticado.")
    }

    private fun createDestinationFile(
        eventId: String,
        publicationId: String,
        extension: String
    ): File {
        val safeEventId = eventId.replace(Regex("[^A-Za-z0-9_-]"), "_")
        val eventDirectory = File(appContext.filesDir, "$PUBLICATIONS_DIRECTORY/$safeEventId")
        check(eventDirectory.exists() || eventDirectory.mkdirs()) {
            "Não foi possível preparar o armazenamento da publicação."
        }

        return File(eventDirectory, "$publicationId.$extension")
    }

    private fun resolveExtension(sourceUri: Uri, type: PublicationType): String {
        val mimeType = appContext.contentResolver.getType(sourceUri)
        return MimeTypeMap.getSingleton()
            .getExtensionFromMimeType(mimeType)
            ?.takeIf { extension -> extension.isNotBlank() }
            ?: when (type) {
                PublicationType.PHOTO -> "jpg"
                PublicationType.AUDIO -> "m4a"
                PublicationType.VIDEO -> "mp4"
            }
    }

    private fun validateVideoDuration(sourceUri: Uri) {
        val retriever = MediaMetadataRetriever()

        try {
            retriever.setDataSource(appContext, sourceUri)
            val duration = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
                ?: error("Não foi possível verificar a duração do vídeo.")

            require(duration <= MAX_VIDEO_DURATION_MS) {
                "Escolha um vídeo de até 30 segundos."
            }
        } finally {
            retriever.release()
        }
    }

    private fun readAll(): List<EventPublication> {
        val storedJson = preferences.getString(PUBLICATIONS_KEY, null) ?: return emptyList()
        val jsonArray = JSONArray(storedJson)

        return buildList {
            for (index in 0 until jsonArray.length()) {
                val publication = runCatching {
                    jsonArray.getJSONObject(index).toPublication()
                }.getOrNull()

                if (publication != null) add(publication)
            }
        }
    }

    private fun writeAll(publications: List<EventPublication>) {
        val jsonArray = JSONArray()
        publications.forEach { publication ->
            jsonArray.put(publication.toJson())
        }

        check(
            preferences.edit()
                .putString(PUBLICATIONS_KEY, jsonArray.toString())
                .commit()
        ) {
            "Não foi possível salvar os dados da publicação."
        }
    }

    private fun EventPublication.toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("eventId", eventId)
            put("userId", userId)
            put("authorName", authorName)
            put("type", type.name)
            put("localPath", localPath)
            put("createdAt", createdAt)
        }
    }

    private fun JSONObject.toPublication(): EventPublication {
        return EventPublication(
            id = getString("id"),
            eventId = getString("eventId"),
            userId = getString("userId"),
            authorName = getString("authorName"),
            type = PublicationType.valueOf(getString("type")),
            localPath = getString("localPath"),
            createdAt = getLong("createdAt")
        )
    }
}
