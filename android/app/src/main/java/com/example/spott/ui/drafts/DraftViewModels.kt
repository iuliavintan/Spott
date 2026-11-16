package com.example.spott.ui.drafts

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.spott.drafts.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface LoadState<out T> {
    data object Loading : LoadState<Nothing>
    data class Ready<T>(val value: T) : LoadState<T>
    data object Error : LoadState<Nothing>
}

/** Each retry resubscribes after a failed read; Room remains the single source of stored data. */
@OptIn(ExperimentalCoroutinesApi::class)
private fun <T> observedState(retry: StateFlow<Int>, source: () -> Flow<T>): Flow<LoadState<T>> =
    retry.flatMapLatest {
        source().map<T, LoadState<T>> { LoadState.Ready(it) }
            .onStart { emit(LoadState.Loading) }
            .catch { error ->
                if (error is CancellationException) throw error
                emit(LoadState.Error)
            }
    }

class DraftListViewModel(repository: ParkingDraftRepository) : ViewModel() {
    private val retryCount = MutableStateFlow(0)
    val state = observedState(retryCount, repository::observeDrafts)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LoadState.Loading)
    fun retry() { retryCount.value++ }
}

data class DeleteState(val busy: Boolean = false, val error: Boolean = false, val completed: Boolean = false)

class DraftDetailsViewModel(private val repository: ParkingDraftRepository, private val id: String) : ViewModel() {
    private val retryCount = MutableStateFlow(0)
    val state = observedState(retryCount) { repository.observeDraft(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LoadState.Loading)
    private val _deletion = MutableStateFlow(DeleteState())
    val deletion = _deletion.asStateFlow()
    fun retry() { retryCount.value++ }
    fun delete() {
        if (_deletion.value.busy || _deletion.value.completed) return
        _deletion.value = DeleteState(busy = true)
        viewModelScope.launch {
            try {
                repository.delete(id)
                _deletion.value = DeleteState(completed = true)
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                _deletion.value = DeleteState(error = true)
            }
        }
    }
    fun acknowledgeDeletion() { _deletion.value = DeleteState() }
}

data class EditorState(
    val input: DraftInput = DraftInput(),
    val errors: DraftErrors = DraftErrors(),
    val loading: Boolean = false,
    val loadFailed: Boolean = false,
    val missing: Boolean = false,
    val busy: Boolean = false,
    val storageError: Boolean = false,
    val savedId: String? = null
)

class DraftEditorViewModel(
    private val repository: ParkingDraftRepository,
    private val savedState: SavedStateHandle,
    private val id: String?
) : ViewModel() {
    private val _state = MutableStateFlow(EditorState(
        input = DraftInput(savedState["location"] ?: "", savedState["latitude"] ?: "",
            savedState["longitude"] ?: "", savedState["notes"] ?: ""),
        loading = id != null && savedState.get<Boolean>("initialized") != true,
        savedId = savedState["savedId"]
    ))
    val state = _state.asStateFlow()

    init { if (_state.value.loading) load() }

    fun load() {
        _state.update { it.copy(loading = true, loadFailed = false) }
        viewModelScope.launch {
            try {
                val draft = repository.observeDraft(requireNotNull(id)).first()
                if (draft == null) {
                    _state.update { it.copy(loading = false, missing = true) }
                } else {
                    changeInput(DraftInput(draft.location, draft.latitude?.toString() ?: "",
                        draft.longitude?.toString() ?: "", draft.notes))
                    savedState["initialized"] = true
                    _state.update { it.copy(loading = false) }
                }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                _state.update { it.copy(loading = false, loadFailed = true) }
            }
        }
    }

    fun changeInput(input: DraftInput) {
        if (_state.value.busy) return
        savedState["location"] = input.location
        savedState["latitude"] = input.latitude
        savedState["longitude"] = input.longitude
        savedState["notes"] = input.notes
        _state.update { it.copy(input = input, errors = DraftErrors(), storageError = false) }
    }

    fun save() {
        val current = _state.value
        if (current.busy || current.loading || current.loadFailed || current.missing || current.savedId != null) return
        val validation = DraftValidator.validate(current.input)
        if (validation.value == null) {
            _state.update { it.copy(errors = validation.errors) }
            return
        }
        _state.update { it.copy(busy = true, storageError = false) }
        viewModelScope.launch {
            try {
                val resultId = if (id == null) repository.create(current.input) else {
                    if (!repository.update(id, current.input)) {
                        _state.update { it.copy(busy = false, missing = true) }
                        return@launch
                    }
                    id
                }
                savedState["savedId"] = resultId
                _state.update { it.copy(busy = false, savedId = resultId) }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                _state.update { it.copy(busy = false, storageError = true) }
            }
        }
    }

    fun acknowledgeSave() {
        savedState["savedId"] = null
        _state.update { it.copy(savedId = null) }
    }
}
