package br.com.uri.campushub.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import br.com.uri.campushub.model.Event
import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class EventViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val past = Timestamp(1, 0)
    private val future = Timestamp(4_102_444_800L, 0)
    private val events = listOf(
        Event(
            id = "1",
            title = "Festival de Música",
            category = "Cultura",
            endAt = past
        ),
        Event(
            id = "2",
            title = "Oficina de Kotlin",
            category = "Tecnologia",
            endAt = future
        )
    )
    private lateinit var repository: FakeEventDataSource
    private lateinit var viewModel: EventViewModel

    @Before
    fun setUp() {
        repository = FakeEventDataSource()
        viewModel = EventViewModel(repository)
    }

    @Test
    fun `load events changes from loading to success`() {
        viewModel.loadEvents()

        assertSame(EventState.Loading, viewModel.eventState.value)
        assertEquals(1, repository.getEventsCalls)

        repository.completeEvents(Result.success(events))

        assertEquals(EventState.Success(events), viewModel.eventState.value)
        assertEquals(listOf("Cultura", "Tecnologia"), viewModel.categories.value)
    }

    @Test
    fun `load events failure exposes repository message`() {
        viewModel.loadEvents()

        repository.completeEvents(Result.failure(Exception("Eventos indisponíveis.")))

        assertEquals(
            EventState.Error("Eventos indisponíveis."),
            viewModel.eventState.value
        )
    }

    @Test
    fun `view model combines loaded event filters`() {
        viewModel.loadEvents()
        repository.completeEvents(Result.success(events))

        viewModel.setSearchQuery("kotlin")
        viewModel.setCategory("Tecnologia")
        viewModel.setSituation(EventSituationFilter.UPCOMING)

        val state = viewModel.eventState.value as EventState.Success
        assertEquals(listOf("2"), state.events.map(Event::id))
        assertTrue(state.isFiltered)
        assertEquals("Tecnologia", viewModel.categoryFilter)
        assertEquals(EventSituationFilter.UPCOMING, viewModel.situationFilter)
    }

    @Test
    fun `filters configured before loading are applied after success`() {
        viewModel.setSearchQuery("kotlin")
        viewModel.setCategory("Tecnologia")

        assertSame(EventState.Idle, viewModel.eventState.value)

        viewModel.loadEvents()
        repository.completeEvents(Result.success(events))

        assertEquals(
            EventState.Success(listOf(events[1]), isFiltered = true),
            viewModel.eventState.value
        )
    }

    @Test
    fun `reload clears category that is no longer available`() {
        viewModel.loadEvents()
        repository.completeEvents(Result.success(events))
        viewModel.setCategory("Tecnologia")

        val cultureEvent = events.first()
        viewModel.loadEvents()
        repository.completeEvents(Result.success(listOf(cultureEvent)))

        assertEquals(null, viewModel.categoryFilter)
        assertEquals(
            EventState.Success(listOf(cultureEvent)),
            viewModel.eventState.value
        )
    }
}
