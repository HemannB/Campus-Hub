package br.com.uri.campushub.viewmodel

import android.app.Application
import android.net.Uri
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import br.com.uri.campushub.model.EventPublication
import br.com.uri.campushub.model.PublicationType
import br.com.uri.campushub.repository.PublicationDataSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.AbstractExecutorService
import java.util.concurrent.TimeUnit

class PublicationViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var authRepository: FakeCurrentUserDataSource
    private lateinit var publicationRepository: FakePublicationDataSource
    private lateinit var viewModel: PublicationViewModel

    @Before
    fun setUp() {
        authRepository = FakeCurrentUserDataSource()
        publicationRepository = FakePublicationDataSource()
        viewModel = PublicationViewModel(
            application = Application(),
            authRepository = authRepository,
            userRepository = FakeUserDataSource(),
            publicationRepository = publicationRepository,
            fileExecutor = DirectExecutorService()
        )
    }

    @Test
    fun `initial states are idle and current user comes from authentication`() {
        assertSame(PublicationListState.Idle, viewModel.listState.value)
        assertSame(PublicationActionState.Idle, viewModel.actionState.value)
        assertEquals("user-1", viewModel.currentUserId())

        authRepository.userId = null

        assertEquals(null, viewModel.currentUserId())
    }

    @Test
    fun `load publications changes from loading to success`() {
        val publications = listOf(publication(id = "publication-1"))
        publicationRepository.publicationsResult = Result.success(publications)

        viewModel.loadPublications("event-1")

        assertEquals("event-1", publicationRepository.requestedEventId)
        assertEquals(PublicationListState.Success(publications), viewModel.listState.value)
    }

    @Test
    fun `load publications failure exposes repository message`() {
        publicationRepository.publicationsResult = Result.failure(
            Exception("Arquivos indisponíveis.")
        )

        viewModel.loadPublications("event-1")

        assertEquals(
            PublicationListState.Error("Arquivos indisponíveis."),
            viewModel.listState.value
        )
    }

    @Test
    fun `successful deletion reloads publications`() {
        val remaining = listOf(publication(id = "publication-2"))
        publicationRepository.publicationsResult = Result.success(remaining)
        publicationRepository.deleteResult = Result.success(Unit)

        viewModel.deletePublication("event-1", "publication-1")

        assertEquals("publication-1", publicationRepository.deletedPublicationId)
        assertEquals(1, publicationRepository.getPublicationsCalls)
        assertEquals(PublicationListState.Success(remaining), viewModel.listState.value)
        assertEquals(
            PublicationActionState.Success("Publicação excluída."),
            viewModel.actionState.value
        )
    }

    @Test
    fun `failed deletion exposes error and action can be cleared`() {
        publicationRepository.deleteResult = Result.failure(
            Exception("Publicação não encontrada.")
        )

        viewModel.deletePublication("event-1", "publication-1")

        assertEquals(0, publicationRepository.getPublicationsCalls)
        assertEquals(
            PublicationActionState.Error("Publicação não encontrada."),
            viewModel.actionState.value
        )

        viewModel.clearActionState()

        assertSame(PublicationActionState.Idle, viewModel.actionState.value)
    }

    private fun publication(id: String) = EventPublication(
        id = id,
        eventId = "event-1",
        userId = "user-1",
        authorName = "Bruno",
        type = PublicationType.PHOTO,
        localPath = "/tmp/$id.jpg",
        createdAt = 1L
    )

    private class FakePublicationDataSource : PublicationDataSource {
        var publicationsResult: Result<List<EventPublication>> = Result.success(emptyList())
        var deleteResult: Result<Unit> = Result.success(Unit)
        var requestedEventId: String? = null
        var deletedPublicationId: String? = null
        var getPublicationsCalls = 0

        override fun getPublications(eventId: String): Result<List<EventPublication>> {
            requestedEventId = eventId
            getPublicationsCalls++
            return publicationsResult
        }

        override fun publishFromUri(
            eventId: String,
            authorName: String,
            type: PublicationType,
            sourceUri: Uri
        ): Result<EventPublication> {
            return Result.failure(UnsupportedOperationException())
        }

        override fun deletePublication(publicationId: String): Result<Unit> {
            deletedPublicationId = publicationId
            return deleteResult
        }
    }

    private class DirectExecutorService : AbstractExecutorService() {
        private var shutdown = false

        override fun execute(command: Runnable) = command.run()

        override fun shutdown() {
            shutdown = true
        }

        override fun shutdownNow(): List<Runnable> {
            shutdown = true
            return emptyList()
        }

        override fun isShutdown(): Boolean = shutdown

        override fun isTerminated(): Boolean = shutdown

        override fun awaitTermination(timeout: Long, unit: TimeUnit): Boolean = true
    }
}
