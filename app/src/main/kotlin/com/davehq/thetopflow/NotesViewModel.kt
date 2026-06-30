package com.davehq.thetopflow

import android.app.Application
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.davehq.thetopflow.data.NoteUi
import com.davehq.thetopflow.data.NotesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Immutable
data class NotesUiState(
    val notes: List<NoteUi> = emptyList(),
    val visibleNotes: List<NoteUi> = emptyList(),
    val selectedNote: NoteUi? = null,
    val rhymeSuggestions: List<String> = emptyList(),
    val rhymeLoading: Boolean = true,
    val query: String = "",
    val isLoading: Boolean = true,
    val isCreating: Boolean = false
)

class NotesViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = NotesRepository(application)
    private val rhymeEngine = RhymeEngine(application)
    private val notes = MutableStateFlow<List<NoteUi>>(emptyList())
    private val selectedNoteId = MutableStateFlow<String?>(null)
    private val query = MutableStateFlow("")
    private val loading = MutableStateFlow(true)
    private val creating = MutableStateFlow(false)
    private val rhymeSuggestions = MutableStateFlow<List<String>>(emptyList())
    private val rhymeLoading = MutableStateFlow(true)
    private var saveJob: Job? = null
    private var rhymeJob: Job? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<NotesUiState> = combine(
        notes,
        selectedNoteId,
        query,
        loading,
        creating,
        rhymeSuggestions,
        rhymeLoading
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        RawNotesState(
            notes = values[0] as List<NoteUi>,
            selectedNoteId = values[1] as String?,
            query = values[2] as String,
            isLoading = values[3] as Boolean,
            isCreating = values[4] as Boolean,
            rhymeSuggestions = values[5] as List<String>,
            rhymeLoading = values[6] as Boolean
        )
    }.mapLatest { raw ->
        withContext(Dispatchers.Default) {
            val sorted = raw.notes.sortedByDescending { it.updatedAt }
            val filtered = if (raw.query.isBlank()) {
                sorted
            } else {
                val needle = raw.query.trim()
                sorted.filter {
                    it.title.contains(needle, ignoreCase = true) ||
                        it.body.contains(needle, ignoreCase = true)
                }
            }
            NotesUiState(
                notes = sorted,
                visibleNotes = filtered,
                selectedNote = sorted.firstOrNull { it.id == raw.selectedNoteId },
                rhymeSuggestions = raw.rhymeSuggestions,
                rhymeLoading = raw.rhymeLoading,
                query = raw.query,
                isLoading = raw.isLoading,
                isCreating = raw.isCreating
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NotesUiState())

    init {
        rhymeEngine.loadAsync {
            rhymeLoading.value = false
            refreshRhymesForSelected()
        }
        viewModelScope.launch {
            val loaded = repository.loadNotes()
            notes.value = loaded
            loading.value = false
            if (loaded.any { it.id.startsWith("legacy-") }) scheduleSave(delayMs = 0)
        }
    }

    fun createNote() {
        val note = NoteUi.blank()
        notes.update { current -> listOf(note) + current }
        selectedNoteId.value = note.id
        creating.value = true
        query.value = ""
        scheduleSave()
    }

    fun openNote(id: String) {
        selectedNoteId.value = id
        creating.value = false
        refreshRhymesForSelected()
    }

    fun closeEditor() {
        selectedNoteId.value = null
        creating.value = false
        rhymeSuggestions.value = emptyList()
    }

    fun updateSearch(value: String) {
        query.value = value
        if (value.isNotBlank()) {
            selectedNoteId.value = null
            creating.value = false
        }
    }

    fun updateTitle(value: String) {
        updateSelected { it.copy(title = value.ifBlank { "Untitled" }, updatedAt = System.currentTimeMillis()) }
    }

    fun updateBody(value: String) {
        updateSelected { it.copy(body = value, updatedAt = System.currentTimeMillis()) }
        scheduleRhymeUpdate(value)
    }

    fun deleteSelected() {
        val id = selectedNoteId.value ?: return
        notes.update { current -> current.filterNot { it.id == id } }
        selectedNoteId.value = null
        creating.value = false
        scheduleSave()
    }

    fun flushPendingSave() {
        saveJob?.cancel()
        viewModelScope.launch { repository.saveNotes(notes.value) }
    }

    private fun updateSelected(transform: (NoteUi) -> NoteUi) {
        val id = selectedNoteId.value ?: return
        notes.update { current -> current.map { if (it.id == id) transform(it) else it } }
        scheduleSave()
    }

    private fun scheduleSave(delayMs: Long = 420) {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(delayMs)
            repository.saveNotes(notes.value)
        }
    }

    private fun refreshRhymesForSelected() {
        val body = notes.value.firstOrNull { it.id == selectedNoteId.value }?.body.orEmpty()
        scheduleRhymeUpdate(body, delayMs = 0)
    }

    private fun scheduleRhymeUpdate(body: String, delayMs: Long = 140) {
        rhymeJob?.cancel()
        rhymeJob = viewModelScope.launch {
            delay(delayMs)
            val word = withContext(Dispatchers.Default) { body.activeRhymeWord() }
            if (word.isBlank()) {
                rhymeSuggestions.value = emptyList()
                rhymeLoading.value = !rhymeEngine.isReady()
                return@launch
            }
            val suggestions = withContext(Dispatchers.Default) {
                if (!rhymeEngine.isReady()) {
                    emptyList()
                } else {
                    rhymeEngine.suggest(
                        word,
                        8,
                        360,
                        RhymeEngine.Options("Balanced", false, true, emptySet(), body)
                    )
                }
            }
            rhymeLoading.value = !rhymeEngine.isReady()
            rhymeSuggestions.value = suggestions
        }
    }

    private fun String.activeRhymeWord(): String {
        return trim().split(Regex("[^A-Za-z']+")).lastOrNull { it.isNotBlank() }.orEmpty()
    }

    private data class RawNotesState(
        val notes: List<NoteUi>,
        val selectedNoteId: String?,
        val query: String,
        val isLoading: Boolean,
        val isCreating: Boolean,
        val rhymeSuggestions: List<String>,
        val rhymeLoading: Boolean
    )
}
