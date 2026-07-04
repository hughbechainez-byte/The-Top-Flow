package com.davehq.thetopflow.data

import android.app.Application
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.UUID

private const val DEFAULT_NOTE_COLOR = 0xFF0ECDBE.toInt()
private const val DEFAULT_NOTE_TEXT_COLOR = 0xFF080C0E.toInt()
private const val DEFAULT_NOTE_ACCENT_COLOR = 0xFF84FFEE.toInt()
private const val DEFAULT_MENU_COLOR = 0xFF05070D.toInt()
private const val DEFAULT_MENU_ACCENT_COLOR = 0xFF5AD7A0.toInt()
private const val DEFAULT_EDITOR_FONT_SIZE_SP = 18
private const val MIN_EDITOR_FONT_SIZE_SP = 14
private const val MAX_EDITOR_FONT_SIZE_SP = 28

@Immutable
data class RecordingUi(
    val path: String,
    val tag: String
)

@Immutable
data class NoteUi(
    val id: String,
    val title: String,
    val body: String,
    val font: String,
    val fontSizeSp: Int,
    val noteColor: Int,
    val textColor: Int,
    val accentColor: Int,
    val noteGlow: Boolean,
    val glowStrength: Int,
    val songUri: String,
    val recordings: List<RecordingUi>,
    val createdAt: Long,
    val updatedAt: Long
) {
    val preview: String
        get() {
            if (body.isBlank()) return ""
            var index = 0
            while (index < body.length) {
                val nextNewline = body.indexOf('\n', index)
                val line = if (nextNewline < 0) {
                    body.substring(index)
                } else {
                    body.substring(index, nextNewline)
                }
                if (line.isNotBlank()) return line.trim()
                index = if (nextNewline < 0) body.length else nextNewline + 1
            }
            return ""
        }

    val wordCount: Int
        get() {
            if (body.isBlank()) return 0
            var count = 0
            var inWord = false
            for (char in body) {
                if (char.isWhitespace()) {
                    if (inWord) {
                        count++
                        inWord = false
                    }
                } else if (!inWord) {
                    inWord = true
                }
            }
            if (inWord) count++
            return count
        }

    companion object {
        fun blank(now: Long = System.currentTimeMillis(), defaults: StyleDefaults = StyleDefaults()): NoteUi = NoteUi(
            id = UUID.randomUUID().toString(),
            title = "Untitled",
            body = "",
            font = defaults.font,
            fontSizeSp = defaults.fontSizeSp,
            noteColor = defaults.noteColor,
            textColor = defaults.textColor,
            accentColor = defaults.accentColor,
            noteGlow = false,
            glowStrength = 1,
            songUri = "",
            recordings = emptyList(),
            createdAt = now,
            updatedAt = now
        )
    }
}

@Immutable
data class StyleDefaults(
    val font: String = "sans",
    val fontSizeSp: Int = DEFAULT_EDITOR_FONT_SIZE_SP,
    val noteColor: Int = DEFAULT_NOTE_COLOR,
    val textColor: Int = DEFAULT_NOTE_TEXT_COLOR,
    val accentColor: Int = DEFAULT_NOTE_ACCENT_COLOR,
    val menuColor: Int = DEFAULT_MENU_COLOR,
    val menuAccentColor: Int = DEFAULT_MENU_ACCENT_COLOR
)

class NotesRepository(
    application: Application,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val notesFile = File(application.filesDir, "notes.json")
    private val backupFile = File(application.filesDir, "notes.backup.json")
    private val prefs = application.getSharedPreferences("top_flow_style_defaults", 0)

    fun loadStyleDefaults(): StyleDefaults = StyleDefaults(
        font = prefs.getString("font", "sans") ?: "sans",
        fontSizeSp = prefs.getInt("fontSizeSp", DEFAULT_EDITOR_FONT_SIZE_SP)
            .coerceIn(MIN_EDITOR_FONT_SIZE_SP, MAX_EDITOR_FONT_SIZE_SP),
        noteColor = prefs.getInt("noteColor", DEFAULT_NOTE_COLOR),
        textColor = prefs.getInt("textColor", DEFAULT_NOTE_TEXT_COLOR),
        accentColor = prefs.getInt("accentColor", DEFAULT_NOTE_ACCENT_COLOR),
        menuColor = prefs.getInt("menuColor", DEFAULT_MENU_COLOR),
        menuAccentColor = prefs.getInt("menuAccentColor", DEFAULT_MENU_ACCENT_COLOR)
    )

    fun saveStyleDefaults(defaults: StyleDefaults) {
        prefs.edit()
            .putString("font", defaults.font.ifBlank { "sans" })
            .putInt("fontSizeSp", defaults.fontSizeSp.coerceIn(MIN_EDITOR_FONT_SIZE_SP, MAX_EDITOR_FONT_SIZE_SP))
            .putInt("noteColor", defaults.noteColor)
            .putInt("textColor", defaults.textColor)
            .putInt("accentColor", defaults.accentColor)
            .putInt("menuColor", defaults.menuColor)
            .putInt("menuAccentColor", defaults.menuAccentColor)
            .apply()
    }

    suspend fun loadNotes(): List<NoteUi> = withContext(ioDispatcher) {
        val sourceFile = when {
            notesFile.exists() && notesFile.length() > 2L -> notesFile
            backupFile.exists() && backupFile.length() > 2L -> backupFile
            else -> return@withContext emptyList()
        }
        runCatching {
            val raw = sourceFile.readText(StandardCharsets.UTF_8)
            val arr = JSONArray(raw)
            buildList {
                for (index in 0 until arr.length()) {
                    add(arr.getJSONObject(index).toNoteUi(index))
                }
            }
        }.getOrDefault(emptyList())
    }

    suspend fun saveNotes(notes: List<NoteUi>) = withContext(ioDispatcher) {
        if (notes.isEmpty() && notesFile.exists() && notesFile.length() > 2L) {
            return@withContext
        }
        if (notesFile.exists() && notesFile.length() > 2L) {
            runCatching { notesFile.copyTo(backupFile, overwrite = true) }
        }
        val arr = JSONArray()
        notes.forEach { arr.put(it.toJson()) }
        notesFile.writeText(arr.toString(2), StandardCharsets.UTF_8)
    }

    private fun JSONObject.toNoteUi(index: Int): NoteUi {
        val now = System.currentTimeMillis()
        val title = optString("title", "Untitled").ifBlank { "Untitled" }
        val body = optString("body", "")
        val createdAt = optLong("createdAt", now)
        val updatedAt = optLong("updatedAt", createdAt)
        return NoteUi(
            id = optString("id").ifBlank { stableLegacyId(index, title, body, createdAt) },
            title = title,
            body = body,
            font = optString("font", "sans"),
            fontSizeSp = optInt("fontSizeSp", DEFAULT_EDITOR_FONT_SIZE_SP)
                .coerceIn(MIN_EDITOR_FONT_SIZE_SP, MAX_EDITOR_FONT_SIZE_SP),
            noteColor = optInt("noteColor", DEFAULT_NOTE_COLOR),
            textColor = optInt("textColor", DEFAULT_NOTE_TEXT_COLOR),
            accentColor = optInt("accentColor", DEFAULT_NOTE_ACCENT_COLOR),
            noteGlow = optBoolean("noteGlow", false),
            glowStrength = optInt("glowStrength", 1).coerceIn(0, 4),
            songUri = optString("songUri", ""),
            recordings = optJSONArray("recordings").toRecordingList(),
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    private fun JSONArray?.toRecordingList(): List<RecordingUi> {
        if (this == null) return emptyList()
        return buildList {
            for (index in 0 until length()) {
                val obj = optJSONObject(index)
                val fallback = optString(index, "")
                val path = obj?.optString("path", fallback).orEmpty()
                if (path.isNotBlank()) {
                    add(RecordingUi(path = path, tag = obj?.optString("tag", File(path).name) ?: File(path).name))
                }
            }
        }
    }

    private fun NoteUi.toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("title", title)
        put("body", body)
        put("font", font)
        put("fontSizeSp", fontSizeSp.coerceIn(MIN_EDITOR_FONT_SIZE_SP, MAX_EDITOR_FONT_SIZE_SP))
        put("noteColor", noteColor)
        put("textColor", textColor)
        put("accentColor", accentColor)
        put("noteGlow", noteGlow)
        put("glowStrength", glowStrength.coerceIn(0, 4))
        put("songUri", songUri)
        put("createdAt", createdAt)
        put("updatedAt", updatedAt)
        put("recordings", JSONArray().apply {
            recordings.forEach { recording ->
                put(JSONObject().apply {
                    put("path", recording.path)
                    put("tag", recording.tag)
                })
            }
        })
    }

    private fun stableLegacyId(index: Int, title: String, body: String, createdAt: Long): String {
        val input = "$index:$createdAt:$title:$body"
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray(StandardCharsets.UTF_8))
            .take(12)
            .joinToString("") { "%02x".format(it) }
        return "legacy-$digest"
    }
}
