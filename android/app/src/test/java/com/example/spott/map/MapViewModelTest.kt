package com.example.spott.map

import androidx.lifecycle.SavedStateHandle
import com.example.spott.drafts.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class MapViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = TestDraftRepository()
    private val search = TestLocationSearch()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun teardown() { Dispatchers.resetMain() }

    @Test
    fun longPressSelectsWithoutSavingAndKeepsManualDescription() = runTest(dispatcher) {
        val model = ParkingMapViewModel(repository, search, AddressResolver { null }, SavedStateHandle())
        model.selectPoint(GeoPoint(45.75, 21.23))
        advanceUntilIdle()
        assertEquals(GeoPoint(45.75, 21.23), model.state.value.selection!!.point)
        assertTrue(repository.rows.value.isEmpty())
        model.renameSelection("Side street")
        assertEquals("Side street", model.state.value.selection!!.address)
    }

    @Test
    fun markersFollowRepositoryUpdatesAndDeletions() = runTest(dispatcher) {
        val model = ParkingMapViewModel(repository, search, AddressResolver { null }, SavedStateHandle())
        val draft = ParkingReportDraft("one", "Library", 45.0, 21.0, "", 1, 1)
        repository.rows.value = listOf(draft, draft.copy(id = "text", latitude = null, longitude = null))
        runCurrent()
        assertEquals(listOf("one"), model.state.value.markers.map { it.id })
        repository.rows.value = listOf(draft.copy(latitude = 46.0))
        runCurrent()
        assertEquals(46.0, model.state.value.markers.single().latitude!!, 0.0)
        repository.delete("one")
        runCurrent()
        assertTrue(model.state.value.markers.isEmpty())
    }

    @Test
    fun searchDebouncesAndIgnoresAnOlderResponseEvenWhenProviderIgnoresCancellation() = runTest(dispatcher) {
        lateinit var oldResponse: Continuation<List<PlaceSuggestion>>
        search.find = { query, _ ->
            if (query == "old") suspendCoroutine { oldResponse = it }
            else listOf(PlaceSuggestion("new", "New place", "City"))
        }
        val model = ParkingMapViewModel(repository, search, AddressResolver { null }, SavedStateHandle())
        model.openSearch()
        model.updateQuery("old")
        advanceTimeBy(299)
        runCurrent()
        assertEquals(0, search.requests)
        advanceTimeBy(1)
        runCurrent()
        assertEquals(1, search.requests)
        model.updateQuery("new")
        advanceTimeBy(300)
        runCurrent()
        oldResponse.resume(listOf(PlaceSuggestion("old", "Outdated", "City")))
        runCurrent()
        assertEquals("new", model.search.value.suggestions.single().id)
        assertEquals(1, search.sessions)
    }

    @Test
    fun selectedSuggestionResolvesActualCoordinatesAndEndsSession() = runTest(dispatcher) {
        val model = ParkingMapViewModel(repository, search, AddressResolver { null }, SavedStateHandle())
        model.openSearch()
        model.updateQuery("University")
        advanceUntilIdle()
        model.chooseSuggestion(PlaceSuggestion("university", "University", "City"))
        advanceUntilIdle()
        assertEquals(GeoPoint(45.75, 21.23), model.state.value.selection!!.point)
        assertFalse(model.search.value.active)
        assertTrue(repository.rows.value.isEmpty())
        model.openSearch()
        model.updateQuery("Next")
        advanceUntilIdle()
        assertEquals(2, search.sessions)
    }

    @Test
    fun searchFailureIsRecoverableAndDoesNotCreateAFictionalPlace() = runTest(dispatcher) {
        search.find = { _, _ -> error("External API failed") }
        val model = ParkingMapViewModel(repository, search, AddressResolver { null }, SavedStateHandle())
        model.openSearch()
        model.updateQuery("Library")
        advanceUntilIdle()
        assertNotNull(model.search.value.error)
        assertTrue(model.search.value.suggestions.isEmpty())
        assertNull(model.state.value.selection)
        search.find = { _, _ -> listOf(PlaceSuggestion("real", "Library", "City")) }
        model.updateQuery("Library")
        advanceUntilIdle()
        assertEquals("real", model.search.value.suggestions.single().id)
        assertNull(model.search.value.error)
    }

    @Test
    fun lateAddressCannotOverwriteManualDescription() = runTest(dispatcher) {
        lateinit var addressResponse: Continuation<String?>
        val model = ParkingMapViewModel(repository, search, AddressResolver { suspendCoroutine { addressResponse = it } }, SavedStateHandle())
        model.selectPoint(GeoPoint(45.0, 21.0))
        runCurrent()
        model.renameSelection("My description")
        addressResponse.resume("Old resolved address")
        runCurrent()
        assertEquals("My description", model.state.value.selection!!.address)
    }

    @Test
    fun viewportBiasAndTemporarySelectionRestoreWithoutPersistingADraft() = runTest(dispatcher) {
        val saved = SavedStateHandle()
        val model = ParkingMapViewModel(repository, search, AddressResolver { null }, saved)
        val cluj = GeoPoint(46.77, 23.59)
        model.setSelection(SelectedLocation(cluj, "Cluj"))
        model.openSearch()
        search.find = { _, bias -> assertEquals(cluj, bias); emptyList() }
        model.updateQuery("Street")
        advanceUntilIdle()
        val restoredHandle = SavedStateHandle(saved.keys().associateWith { saved.get<Any?>(it) })
        val restored = ParkingMapViewModel(repository, search, AddressResolver { null }, restoredHandle)
        assertEquals(cluj, restored.state.value.cameraTarget)
        assertEquals(model.state.value.cameraRequest, restored.state.value.cameraRequest)
        assertEquals("Cluj", restored.state.value.selection!!.address)
        assertTrue(repository.rows.value.isEmpty())
    }

    @Test
    fun closingSearchDiscardsPendingResults() = runTest(dispatcher) {
        val model = ParkingMapViewModel(repository, search, AddressResolver { null }, SavedStateHandle())
        model.openSearch()
        model.updateQuery("Library")
        model.closeSearch()
        advanceUntilIdle()
        assertFalse(model.search.value.active)
        assertEquals(0, search.requests)
    }

    @Test
    fun typingDuringPlaceResolutionStartsAFreshTokenAndDiscardsTheOldPlace() = runTest(dispatcher) {
        lateinit var latePlace: Continuation<SelectedLocation>
        search.lookup = { suspendCoroutine { latePlace = it } }
        val model = ParkingMapViewModel(repository, search, AddressResolver { null }, SavedStateHandle())
        model.openSearch()
        model.updateQuery("First")
        advanceUntilIdle()
        model.chooseSuggestion(PlaceSuggestion("first", "First", "City"))
        runCurrent()
        model.updateQuery("Second")
        advanceTimeBy(300)
        runCurrent()
        latePlace.resume(SelectedLocation(GeoPoint(45.0, 21.0), "Outdated place"))
        runCurrent()
        assertEquals(2, search.sessions)
        assertNull(model.state.value.selection)
        assertEquals("Second", model.search.value.query)
    }
}

private class TestDraftRepository : ParkingDraftRepository {
    val rows = MutableStateFlow<List<ParkingReportDraft>>(emptyList())
    override fun observeDrafts() = rows
    override fun observeDraft(id: String) = rows.map { it.firstOrNull { row -> row.id == id } }
    override suspend fun create(input: DraftInput): String = error("Selection must not save")
    override suspend fun update(id: String, input: DraftInput): Boolean = error("Selection must not update")
    override suspend fun delete(id: String) { rows.value = rows.value.filterNot { it.id == id } }
}

private class TestLocationSearch : LocationSearch {
    override val available = true
    var sessions = 0
    var requests = 0
    var find: suspend (String, GeoPoint) -> List<PlaceSuggestion> = { _, _ -> emptyList() }
    var lookup: suspend (String) -> SelectedLocation = { SelectedLocation(GeoPoint(45.75, 21.23), "University") }
    override fun startSession(): PlaceSearchSession {
        sessions++
        return object : PlaceSearchSession {
        override suspend fun suggestions(query: String, bias: GeoPoint): List<PlaceSuggestion> {
            requests++
            return find(query, bias)
        }
        override suspend fun resolve(placeId: String) = lookup(placeId)
        }
    }
}
