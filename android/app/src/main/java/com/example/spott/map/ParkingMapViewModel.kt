package com.example.spott.map

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.spott.drafts.ParkingDraftRepository
import com.example.spott.drafts.ParkingReportDraft
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

data class MapUiState(
    val drafts: List<ParkingReportDraft> = emptyList(),
    val loading: Boolean = true,
    val storageError: Boolean = false,
    val selection: SelectedLocation? = null,
    val resolvingAddress: Boolean = false,
    val selectedDraftId: String? = null,
    val cameraTarget: GeoPoint = GeoPoint.Timisoara,
    val cameraRequest: Int = 0
) {
    val markers: List<ParkingReportDraft> get() = drafts.filter { it.mapPointOrNull() != null }
    val selectedDraft: ParkingReportDraft? get() = drafts.firstOrNull { it.id == selectedDraftId }
}

data class PlaceSearchState(
    val active: Boolean = false,
    val query: String = "",
    val busy: Boolean = false,
    val suggestions: List<PlaceSuggestion> = emptyList(),
    val error: String? = null
)

class ParkingMapViewModel(
    private val repository: ParkingDraftRepository,
    private val locationSearch: LocationSearch,
    private val addressResolver: AddressResolver,
    private val savedState: SavedStateHandle
) : ViewModel() {
    private val _state = MutableStateFlow(MapUiState(cameraRequest = savedState["cameraRequest"] ?: 0))
    val state = _state.asStateFlow()
    private var addressJob: Job? = null
    private var selectionVersion = 0
    private var observationJob: Job? = null
    private val _search = MutableStateFlow(PlaceSearchState())
    val search = _search.asStateFlow()
    val searchAvailable get() = locationSearch.available
    private var session: PlaceSearchSession? = null
    private var searchJob: Job? = null
    private var searchVersion = 0
    private var viewport = GeoPoint.Timisoara

    fun openSearch() {
        closeSearch()
        _search.value = PlaceSearchState(active = true,
            error = if (searchAvailable) null else "Search is unavailable. You can still add a draft manually.")
    }

    fun closeSearch() {
        searchJob?.cancel()
        searchVersion++
        session = null
        _search.value = PlaceSearchState()
    }

    fun updateQuery(query: String) {
        searchJob?.cancel()
        val version = ++searchVersion
        _search.value = PlaceSearchState(active = true, query = query, busy = query.isNotBlank() && searchAvailable)
        if (!searchAvailable || query.isBlank()) return
        searchJob = viewModelScope.launch {
            delay(300)
            try {
                val interaction = session ?: locationSearch.startSession().also { session = it }
                val results = interaction.suggestions(query.trim(), viewport)
                if (version == searchVersion) _search.update { it.copy(busy = false, suggestions = results) }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                if (version == searchVersion) _search.update { it.copy(busy = false, error = "Could not search. Check your connection and try again.") }
            }
        }
    }

    fun chooseSuggestion(suggestion: PlaceSuggestion) {
        val interaction = session ?: return
        session = null // Details consumes the token even if a new query cancels our coroutine.
        searchJob?.cancel()
        val version = ++searchVersion
        _search.update { it.copy(busy = true, suggestions = emptyList(), error = null) }
        searchJob = viewModelScope.launch {
            try {
                val location = interaction.resolve(suggestion.id)
                if (version == searchVersion) {
                    setSelection(location)
                    closeSearch()
                }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                if (version == searchVersion) {
                    session = null // A details request ends this session, including failed attempts.
                    _search.update { it.copy(busy = false, error = "Could not open this place. Try searching again.") }
                }
            }
        }
    }

    fun setSelection(location: SelectedLocation) {
        addressJob?.cancel()
        selectionVersion++
        savedState["selectionLat"] = location.point.latitude
        savedState["selectionLon"] = location.point.longitude
        savedState["selectionAddress"] = location.address
        _state.update { it.copy(selection = location, selectedDraftId = null, resolvingAddress = false) }
        centerOn(location.point)
    }

    fun centerOn(point: GeoPoint) {
        cameraMoved(point)
        val request = _state.value.cameraRequest + 1
        savedState["cameraRequest"] = request
        _state.update { it.copy(cameraTarget = point, cameraRequest = request) }
    }

    fun cameraMoved(point: GeoPoint) {
        viewport = point
        savedState["cameraLat"] = point.latitude
        savedState["cameraLon"] = point.longitude
    }

    fun selectDraft(id: String) {
        val draft = _state.value.drafts.firstOrNull { it.id == id } ?: return
        val point = draft.mapPointOrNull() ?: return
        clearSelection()
        _state.update { it.copy(selectedDraftId = id) }
        centerOn(point)
    }

    fun clearSelection() {
        addressJob?.cancel()
        selectionVersion++
        savedState["selectionLat"] = null
        savedState["selectionLon"] = null
        savedState["selectionAddress"] = null
        _state.update { it.copy(selection = null, selectedDraftId = null, resolvingAddress = false) }
    }

    init {
        val lat = savedState.get<Double>("cameraLat")
        val lon = savedState.get<Double>("cameraLon")
        if (lat != null && lon != null) {
            viewport = GeoPoint(lat, lon)
            _state.update { it.copy(cameraTarget = viewport) }
        }
        val selectedLat = savedState.get<Double>("selectionLat")
        val selectedLon = savedState.get<Double>("selectionLon")
        if (selectedLat != null && selectedLon != null) {
            _state.update { it.copy(selection = SelectedLocation(GeoPoint(selectedLat, selectedLon), savedState["selectionAddress"] ?: "")) }
        }
        retryDrafts()
    }

    fun retryDrafts() {
        observationJob?.cancel()
        _state.update { it.copy(loading = true, storageError = false) }
        observationJob = viewModelScope.launch {
            repository.observeDrafts().catch { error ->
                if (error is CancellationException) throw error
                _state.update { it.copy(loading = false, storageError = true) }
            }.collect { drafts ->
                _state.update { it.copy(drafts = drafts, loading = false,
                    selectedDraftId = it.selectedDraftId?.takeIf { id -> drafts.any { draft -> draft.id == id } }) }
            }
        }
    }

    fun selectPoint(point: GeoPoint) {
        setSelection(SelectedLocation(point))
        val currentVersion = selectionVersion
        _state.update { it.copy(resolvingAddress = true) }
        addressJob = viewModelScope.launch {
            val address = try {
                addressResolver.address(point)
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                null
            }
            if (currentVersion == selectionVersion) {
                savedState["selectionAddress"] = address.orEmpty()
                _state.update { it.copy(selection = SelectedLocation(point, address.orEmpty()), resolvingAddress = false) }
            }
        }
    }

    fun renameSelection(address: String) {
        addressJob?.cancel()
        selectionVersion++
        savedState["selectionAddress"] = address
        _state.update { it.copy(selection = it.selection?.copy(address = address), resolvingAddress = false) }
    }
}
