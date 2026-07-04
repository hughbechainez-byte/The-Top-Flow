package com.davehq.thetopflow

import android.app.Application
import android.content.ContentValues
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.davehq.thetopflow.data.NoteUi
import com.davehq.thetopflow.data.NotesRepository
import com.davehq.thetopflow.data.RecordingUi
import com.davehq.thetopflow.data.StyleDefaults
import com.davehq.thetopflow.rhyme.RhymeEngine2
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream

@Immutable
data class NotesUiState(
    val notes: List<NoteUi> = emptyList(),
    val visibleNotes: List<NoteUi> = emptyList(),
    val selectedNote: NoteUi? = null,
    val rhymeSuggestions: List<String> = emptyList(),
    val rhymeLoading: Boolean = true,
    val query: String = "",
    val isLoading: Boolean = true,
    val isCreating: Boolean = false,
    val media: MediaUiState = MediaUiState(),
    val lastOpenedNoteId: String? = null,
    val styleDefaults: StyleDefaults = StyleDefaults()
)

class NotesViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = NotesRepository(application)
    private val rhymeEngine = RhymeEngine(application)
    private val rhymeEngine2 = RhymeEngine2(application)
    private val mediaController = TopFlowMediaController(application)
    private val notes = MutableStateFlow<List<NoteUi>>(emptyList())
    private val selectedNoteId = MutableStateFlow<String?>(null)
    private val query = MutableStateFlow("")
    private val loading = MutableStateFlow(true)
    private val creating = MutableStateFlow(false)
    private val rhymeSuggestions = MutableStateFlow<List<String>>(emptyList())
    private val rhymeLoading = MutableStateFlow(true)
    private val lastOpenedNoteId = MutableStateFlow<String?>(null)
    private val styleDefaults = MutableStateFlow(repository.loadStyleDefaults())
    private var saveJob: Job? = null
    private var rhymeJob: Job? = null
    private var notesLoaded = false
    @Volatile private var rhymeEngine2Ready = false
    private val rhymeRouteMetrics = RhymeRouteMetrics()

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<NotesUiState> = combine(
        notes,
        selectedNoteId,
        query,
        loading,
        creating,
        rhymeSuggestions,
        rhymeLoading,
        mediaController.mediaState,
        lastOpenedNoteId,
        styleDefaults
    ) { values ->
        val media = values[7] as MediaUiState
        val lastOpened = values[8] as String?
        val defaults = values[9] as StyleDefaults
        RawNotesState(
            notes = values[0] as List<NoteUi>,
            selectedNoteId = values[1] as String?,
            query = values[2] as String,
            isLoading = values[3] as Boolean,
            isCreating = values[4] as Boolean,
            rhymeSuggestions = values[5] as List<String>,
            rhymeLoading = values[6] as Boolean,
            media = media,
            lastOpenedNoteId = lastOpened,
            styleDefaults = defaults
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
                isCreating = raw.isCreating,
                media = raw.media,
                lastOpenedNoteId = raw.lastOpenedNoteId,
                styleDefaults = raw.styleDefaults
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NotesUiState())

    init {
        viewModelScope.launch {
            rhymeEngine2Ready = withContext(Dispatchers.IO) { rhymeEngine2.load() }
            if (rhymeEngine2Ready) {
                rhymeLoading.value = false
                refreshRhymesForSelected()
            }
        }
        rhymeEngine.loadAsync(object : RhymeEngine.LoadCallbacks {
            override fun onFastReady() {
                viewModelScope.launch {
                    rhymeLoading.value = false
                    refreshRhymesForSelected()
                }
            }

            override fun onFullReady() {
                viewModelScope.launch {
                    rhymeLoading.value = false
                    refreshRhymesForSelected()
                }
            }
        })
        viewModelScope.launch {
            val loaded = repository.loadNotes()
            notes.value = loaded
            notesLoaded = true
            loading.value = false
            if (loaded.any { it.id.startsWith("legacy-") }) scheduleSave(delayMs = 0)
        }
    }

    override fun onCleared() {
        mediaController.release()
        super.onCleared()
    }

    fun createNote() {
        val note = NoteUi.blank(defaults = styleDefaults.value)
        notes.update { current -> listOf(note) + current }
        selectedNoteId.value = note.id
        lastOpenedNoteId.value = note.id
        creating.value = true
        query.value = ""
        mediaController.attachSong("")
        scheduleSave()
        refreshMediaForSelected()
    }

    fun openNote(id: String) {
        val note = notes.value.firstOrNull { it.id == id } ?: return
        selectedNoteId.value = id
        creating.value = false
        lastOpenedNoteId.value = id
        mediaController.attachSong(note.songUri)
        refreshRhymesForSelected()
    }

    fun openMostRecentOrNewestNote() {
        val candidateId = lastOpenedNoteId.value ?: notes.value.maxByOrNull { it.updatedAt }?.id
        candidateId ?: return
        openNote(candidateId)
    }

    fun closeEditor() {
        selectedNoteId.value = null
        creating.value = false
        rhymeSuggestions.value = emptyList()
        mediaController.pauseAll()
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
        val selected = activeSelectedNote() ?: return
        if (selected.body == value) return
        updateSelected { it.copy(body = value, updatedAt = System.currentTimeMillis()) }
        if (value.length > 16_000) {
            rhymeJob?.cancel()
            rhymeSuggestions.value = emptyList()
            rhymeLoading.value = false
            return
        }
        scheduleRhymeUpdate(value)
    }

    fun deleteSelected() {
        val id = selectedNoteId.value ?: return
        val next = notes.value.filterNot { it.id == id }
        val replacement = if (next.isNotEmpty()) next.firstOrNull()?.id else null
        notes.update { next }
        selectedNoteId.value = null
        lastOpenedNoteId.value = replacement
        creating.value = false
        refreshMediaForSelected()
        scheduleSave()
    }

    fun attachSong(uri: String) {
        if (uri.isBlank()) return
        val selected = activeSelectedNote() ?: return
        updateSelected {
            if (it.id == selected.id) {
                it.copy(songUri = uri, updatedAt = System.currentTimeMillis())
            } else {
                it
            }
        }
        mediaController.attachSong(uri)
        scheduleSave()
    }

    fun toggleSong() {
        val note = activeSelectedNote() ?: return
        mediaController.toggleSong(note.songUri)
    }

    fun seekSong(positionMs: Int) {
        val note = activeSelectedNote() ?: return
        mediaController.seekSong(note.songUri, positionMs)
    }

    fun startRecording() {
        val note = activeSelectedNote() ?: return
        val dir = File(getApplication<Application>().filesDir, "recordings")
        mediaController.startRecording(dir, note.songUri.takeIf { it.isNotBlank() })
    }

    fun stopRecording(save: Boolean) {
        val finished = mediaController.stopRecording(save) ?: return
        val note = activeSelectedNote() ?: return
        val tag = finished.name
        updateSelected {
            it.copy(
                recordings = listOf(RecordingUi(finished.absolutePath, tag)) + it.recordings,
                body = appendRecordingMarker(it.body, tag),
                updatedAt = System.currentTimeMillis()
            )
        }
        scheduleSave()
    }

    fun playRecording(path: String) {
        if (path.isBlank()) return
        mediaController.playRecording(path)
    }

    fun renameRecording(path: String, tag: String) {
        val safeTag = tag.trim()
        if (safeTag.isBlank()) return
        updateSelectedOrNull { note ->
            note.copy(
                recordings = note.recordings.map { rec ->
                    if (rec.path == path) rec.copy(tag = safeTag) else rec
                }
            )
        } ?: return
        scheduleSave()
    }

    fun exportRecording(path: String): Boolean {
        val source = File(path)
        if (source.path.isBlank() || !source.exists()) return false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
        return try {
            val values = ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, source.name)
                put(MediaStore.Audio.Media.MIME_TYPE, "audio/mp4")
                put(MediaStore.Audio.Media.RELATIVE_PATH, "Music/The Top Flow")
            }
            val uri = getApplication<Application>().contentResolver
                .insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
                ?: return false
            getApplication<Application>().contentResolver.openOutputStream(uri)?.use { out ->
                FileInputStream(source).use { input ->
                    input.copyTo(out)
                }
            } ?: return false
            true
        } catch (_: Exception) {
            false
        }
    }

    fun updateNoteStyle(
        font: String,
        fontSizeSp: Int,
        noteColor: Int,
        textColor: Int,
        accentColor: Int,
        menuColor: Int,
        menuAccentColor: Int,
        saveAsDefaults: Boolean
    ) {
        val defaults = StyleDefaults(
            font = font.ifBlank { "sans" },
            fontSizeSp = fontSizeSp.coerceIn(14, 28),
            noteColor = noteColor,
            textColor = textColor,
            accentColor = accentColor,
            menuColor = menuColor,
            menuAccentColor = menuAccentColor
        )
        if (saveAsDefaults) {
            styleDefaults.value = defaults
            repository.saveStyleDefaults(defaults)
        } else {
            val current = styleDefaults.value
            if (current.menuColor != menuColor || current.menuAccentColor != menuAccentColor) {
                val updated = current.copy(menuColor = menuColor, menuAccentColor = menuAccentColor)
                styleDefaults.value = updated
                repository.saveStyleDefaults(updated)
            }
        }
        updateSelected {
            it.copy(
                font = font.ifBlank { "sans" },
                fontSizeSp = fontSizeSp.coerceIn(14, 28),
                noteColor = noteColor,
                textColor = textColor,
                accentColor = accentColor,
                updatedAt = System.currentTimeMillis()
            )
        }
    }

    fun pauseMediaPlayback() {
        mediaController.pauseAll()
    }

    fun flushPendingSave() {
        if (!notesLoaded) return
        saveJob?.cancel()
        viewModelScope.launch { repository.saveNotes(notes.value) }
    }

    private fun activeSelectedNote(): NoteUi? {
        val id = selectedNoteId.value ?: return null
        return notes.value.firstOrNull { it.id == id }
    }

    private fun updateSelected(transform: (NoteUi) -> NoteUi) {
        notes.updateAndGet { current ->
            val id = selectedNoteId.value
            if (id == null) return@updateAndGet current
            current.map { note ->
                if (note.id == id) transform(note) else note
            }
        }
        scheduleSave()
    }

    private fun updateSelectedOrNull(transform: (NoteUi) -> NoteUi): NoteUi? {
        val id = selectedNoteId.value ?: return null
        val updated = notes.updateAndGet { current ->
            current.map { note ->
                if (note.id == id) transform(note) else note
            }
        }.firstOrNull { it.id == id }
        return updated
    }

    private fun refreshMediaForSelected() {
        val note = activeSelectedNote()
        mediaController.attachSong(note?.songUri ?: "")
    }

    private fun scheduleSave(delayMs: Long = 420) {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            kotlinx.coroutines.delay(delayMs)
            repository.saveNotes(notes.value)
        }
    }

    private fun refreshRhymesForSelected() {
        val body = notes.value.firstOrNull { it.id == selectedNoteId.value }?.body.orEmpty()
        scheduleRhymeUpdate(body, delayMs = 0)
    }

    private fun scheduleRhymeUpdate(body: String, delayMs: Long = defaultRhymeDelayMs()) {
        rhymeJob?.cancel()
        rhymeJob = viewModelScope.launch {
            kotlinx.coroutines.delay(delayMs)
            val selectedId = selectedNoteId.value
            if (selectedId == null) return@launch
            val currentBody = notes.value.firstOrNull { it.id == selectedId }?.body ?: ""
            if (body != currentBody) return@launch
            val word = withContext(Dispatchers.Default) { body.activeRhymeWord() }
            if (word.length < 2) {
                rhymeSuggestions.value = emptyList()
                rhymeLoading.value = !isAnyRhymeEngineReady()
                return@launch
            }
            if (!isAnyRhymeEngineReady()) {
                rhymeLoading.value = true
                return@launch
            }
            val suggestions = withContext(Dispatchers.Default) {
                suggestRhymes(word, body)
            }
            rhymeLoading.value = !isAnyRhymeEngineReady()
            rhymeSuggestions.value = suggestions
        }
    }

    private fun defaultRhymeDelayMs(): Long {
        return if (rhymeEngine2Ready) 20L else if (rhymeEngine.isFastReady()) 50L else 140L
    }

    private fun isAnyRhymeEngineReady(): Boolean {
        return rhymeEngine2Ready || rhymeEngine.isFastReady() || rhymeEngine.isReady()
    }

    private fun suggestRhymes(word: String, body: String): List<String> {
        val start = System.nanoTime()
        var source = "empty"
        var v2Count = 0
        var fallbackCount = 0
        var finalCount = 0
        try {
        if (rhymeEngine2Ready) {
            val v2 = runCatching {
                rhymeEngine2.suggest(word, 8)
                    .map { it.word }
                    .distinct()
            }.getOrDefault(emptyList())
            v2Count = v2.size
            if (v2.size >= 4) {
                source = "v2"
                finalCount = v2.size
                return v2
            }
            source = if (v2.isEmpty()) "v2_miss" else "v2_short"
        }
        val fallback = runCatching {
            rhymeEngine.suggest(
                word,
                8,
                360,
                RhymeEngine.Options("Balanced", false, true, emptySet(), body)
            )
        }.getOrDefault(emptyList())
        fallbackCount = fallback.size
        finalCount = fallback.size
        source = if (fallback.isNotEmpty()) {
            if (source.startsWith("v2_")) "fallback_after_$source" else "fallback"
        } else if (source == "empty") {
            "empty"
        } else {
            "${source}_empty"
        }
        return fallback
        } finally {
            val elapsedMs = (System.nanoTime() - start) / 1_000_000.0
            val snapshot = rhymeRouteMetrics.record(source, elapsedMs)
            val wordHash = word.privacyTelemetryHash()
            val wordShape = word.rhymeTelemetryShape()
            Log.d(
                TRACE_TAG,
                "rhyme_trace stage=route source=$source v2Ready=$rhymeEngine2Ready fastReady=${rhymeEngine.isFastReady()} fullReady=${rhymeEngine.isReady()} " +
                    "chars=${word.length} v2Count=$v2Count fallbackCount=$fallbackCount count=$finalCount ms=$elapsedMs " +
                    "wordHash=$wordHash wordShape=$wordShape " +
                    "routeTotal=${snapshot.total} routeV2=${snapshot.v2} routeFallback=${snapshot.fallback} routeEmpty=${snapshot.empty} " +
                    "v2Miss=${snapshot.v2Miss} v2Short=${snapshot.v2Short} fallbackAfterMiss=${snapshot.fallbackAfterMiss} fallbackAfterShort=${snapshot.fallbackAfterShort} avgMs=${snapshot.averageMs}"
            )
        }
    }

    private fun String.privacyTelemetryHash(): String {
        var hash = 0x811c9dc5.toInt()
        for (char in this) {
            val normalized = char.lowercaseChar()
            if (!isWordChar(normalized)) continue
            hash = hash xor normalized.code
            hash *= 0x01000193
        }
        return hash.toUInt().toString(16).padStart(8, '0')
    }

    private fun String.rhymeTelemetryShape(): String {
        val normalized = filter(::isWordChar).lowercase()
        val lengthBucket = when {
            normalized.length <= 2 -> "l2"
            normalized.length <= 4 -> "l4"
            normalized.length <= 7 -> "l7"
            else -> "l8p"
        }
        val suffixBucket = when {
            normalized.contains('\'') -> "apostrophe"
            normalized.endsWith("ing") -> "ing"
            normalized.endsWith("in") -> "in"
            normalized.endsWith("a") -> "a"
            normalized.endsWith("y") -> "y"
            normalized.endsWith("s") -> "s"
            else -> "other"
        }
        return "${lengthBucket}_${suffixBucket}"
    }

    private fun String.activeRhymeWord(): String {
        val end = lastIndexOfLastWordChar()
        if (end < 0) return ""
        var start = end
        while (start > 0 && isWordChar(this[start - 1])) {
            start--
        }
        return substring(start, end + 1)
    }

    private fun String.lastIndexOfLastWordChar(): Int {
        for (index in lastIndex downTo 0) {
            if (isWordChar(this[index])) return index
        }
        return -1
    }

    private fun isWordChar(char: Char): Boolean {
        return char.isLetter() || char == '\''
    }

    private fun appendRecordingMarker(body: String, tag: String): String {
        val marker = "[Voice note: $tag]"
        return if (body.isBlank()) {
            marker
        } else {
            body.trimEnd() + "\n" + marker + "\n"
        }
    }

    private data class RawNotesState(
        val notes: List<NoteUi>,
        val selectedNoteId: String?,
        val query: String,
        val isLoading: Boolean,
        val isCreating: Boolean,
        val rhymeSuggestions: List<String>,
        val rhymeLoading: Boolean,
        val media: MediaUiState,
        val lastOpenedNoteId: String?,
        val styleDefaults: StyleDefaults
    )

    private data class RhymeRouteSnapshot(
        val total: Long,
        val v2: Long,
        val fallback: Long,
        val empty: Long,
        val v2Miss: Long,
        val v2Short: Long,
        val fallbackAfterMiss: Long,
        val fallbackAfterShort: Long,
        val averageMs: Double
    )

    private class RhymeRouteMetrics {
        private var total = 0L
        private var v2 = 0L
        private var fallback = 0L
        private var empty = 0L
        private var v2Miss = 0L
        private var v2Short = 0L
        private var fallbackAfterMiss = 0L
        private var fallbackAfterShort = 0L
        private var averageMs = 0.0

        @Synchronized
        fun record(source: String, elapsedMs: Double): RhymeRouteSnapshot {
            total++
            when {
                source == "v2" -> v2++
                source.contains("fallback") -> fallback++
                else -> empty++
            }
            if (source.contains("v2_miss")) v2Miss++
            if (source.contains("v2_short")) v2Short++
            if (source == "fallback_after_v2_miss") fallbackAfterMiss++
            if (source == "fallback_after_v2_short") fallbackAfterShort++
            averageMs += (elapsedMs - averageMs) / total
            return RhymeRouteSnapshot(total, v2, fallback, empty, v2Miss, v2Short, fallbackAfterMiss, fallbackAfterShort, averageMs)
        }
    }

    private companion object {
        private const val TRACE_TAG = "rhyme_trace"
    }
}
