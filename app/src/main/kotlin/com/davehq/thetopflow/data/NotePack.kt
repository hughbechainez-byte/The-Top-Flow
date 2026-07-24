package com.davehq.thetopflow.data

import android.content.Context
import android.net.Uri
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Portable note archive (.topflow = zip).
 *
 * Layout:
 *   note.json              — title, body, full per-note style, recording metadata
 *   recordings/<file>      — embedded voice takes (when present on disk)
 *
 * Linked song URIs are preserved as strings but are not re-hosted (they often
 * point at external SAF content the user already owns).
 */
object NotePack {
    private const val TAG = "TopFlowNotePack"
    const val FORMAT_VERSION = 1
    const val MIME_TYPE = "application/zip"
    const val FILE_EXTENSION = "topflow"
    private const val NOTE_JSON = "note.json"
    private const val RECORDINGS_PREFIX = "recordings/"

    data class Result(
        val ok: Boolean,
        val message: String,
        val note: NoteUi? = null
    )

    fun suggestedFileName(note: NoteUi): String {
        val safeTitle = note.title
            .trim()
            .ifBlank { "Untitled" }
            .replace(Regex("[^A-Za-z0-9._-]+"), "_")
            .take(40)
            .trim('_')
            .ifBlank { "Untitled" }
        return "TopFlow-${safeTitle}.${FILE_EXTENSION}"
    }

    fun export(context: Context, note: NoteUi, outputUri: Uri): Result {
        return try {
            context.contentResolver.openOutputStream(outputUri)?.use { out ->
                export(note, out)
            } ?: return Result(false, "Could not open destination.")
        } catch (error: Exception) {
            Log.e(TAG, "export_failed", error)
            Result(false, error.message ?: "Export failed.")
        }
    }

    fun export(note: NoteUi, output: OutputStream): Result {
        return try {
            ZipOutputStream(BufferedOutputStream(output)).use { zip ->
                val recordingEntries = mutableListOf<JSONObject>()
                note.recordings.forEachIndexed { index, recording ->
                    val source = File(recording.path)
                    if (!source.exists() || !source.isFile) {
                        Log.w(TAG, "skip_missing_recording path=${recording.path}")
                        return@forEachIndexed
                    }
                    val ext = source.extension.ifBlank { "m4a" }
                    val zipName = "${RECORDINGS_PREFIX}rec_${index.toString().padStart(2, '0')}.$ext"
                    zip.putNextEntry(ZipEntry(zipName))
                    FileInputStream(source).use { input -> input.copyTo(zip) }
                    zip.closeEntry()
                    recordingEntries += JSONObject().apply {
                        put("zipPath", zipName)
                        put("tag", recording.tag.ifBlank { source.nameWithoutExtension })
                        put("originalName", source.name)
                    }
                }

                val payload = JSONObject().apply {
                    put("formatVersion", FORMAT_VERSION)
                    put("id", note.id)
                    put("title", note.title)
                    put("body", note.body)
                    put("font", note.font)
                    put("fontSizeSp", note.fontSizeSp)
                    put("noteColor", note.noteColor)
                    put("textColor", note.textColor)
                    put("accentColor", note.accentColor)
                    put("noteGlow", note.noteGlow)
                    put("glowStrength", note.glowStrength)
                    put("songUri", note.songUri)
                    put("createdAt", note.createdAt)
                    put("updatedAt", note.updatedAt)
                    put("recordings", JSONArray(recordingEntries))
                }
                zip.putNextEntry(ZipEntry(NOTE_JSON))
                zip.write(payload.toString(2).toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
            val count = note.recordings.count { File(it.path).exists() }
            Result(true, "Saved note with style${if (count > 0) " + $count recording(s)" else ""}.")
        } catch (error: Exception) {
            Log.e(TAG, "export_stream_failed", error)
            Result(false, error.message ?: "Export failed.")
        }
    }

    fun import(context: Context, inputUri: Uri): Result {
        return try {
            context.contentResolver.openInputStream(inputUri)?.use { input ->
                import(context, input)
            } ?: Result(false, "Could not open selected file.")
        } catch (error: Exception) {
            Log.e(TAG, "import_failed", error)
            Result(false, error.message ?: "Import failed.")
        }
    }

    fun import(context: Context, input: InputStream): Result {
        val recordingsDir = File(context.filesDir, "recordings").apply { mkdirs() }
        val extracted = linkedMapOf<String, File>()
        var noteJson: String? = null

        try {
            ZipInputStream(BufferedInputStream(input)).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    val name = entry.name.trimStart('/').replace('\\', '/')
                    if (!entry.isDirectory && isSafeZipPath(name)) {
                        when {
                            name == NOTE_JSON -> {
                                noteJson = zip.readBytes().toString(Charsets.UTF_8)
                            }
                            name.startsWith(RECORDINGS_PREFIX) -> {
                                val dest = File(
                                    recordingsDir,
                                    "import_${UUID.randomUUID().toString().take(12)}_${File(name).name}"
                                )
                                FileOutputStream(dest).use { out -> zip.copyTo(out) }
                                extracted[name] = dest
                            }
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        } catch (error: Exception) {
            extracted.values.forEach { runCatching { it.delete() } }
            Log.e(TAG, "import_unzip_failed", error)
            return Result(false, "Not a valid Top Flow note pack.")
        }

        val jsonText = noteJson
            ?: run {
                extracted.values.forEach { runCatching { it.delete() } }
                return Result(false, "Missing note.json in pack.")
            }

        return try {
            val obj = JSONObject(jsonText)
            val version = obj.optInt("formatVersion", 1)
            if (version > FORMAT_VERSION) {
                Log.w(TAG, "newer_format version=$version")
            }
            val now = System.currentTimeMillis()
            val recordingsJson = obj.optJSONArray("recordings") ?: JSONArray()
            val recordings = buildList {
                for (i in 0 until recordingsJson.length()) {
                    val item = recordingsJson.optJSONObject(i) ?: continue
                    val zipPath = item.optString("zipPath").ifBlank {
                        item.optString("path")
                    }
                    val file = extracted[zipPath] ?: continue
                    val tag = item.optString("tag").ifBlank {
                        item.optString("originalName").ifBlank { file.nameWithoutExtension }
                    }
                    add(RecordingUi(path = file.absolutePath, tag = tag))
                }
            }
            val used = recordings.map { it.path }.toSet()
            extracted.values.filter { it.absolutePath !in used }.forEach { runCatching { it.delete() } }

            val note = NoteUi(
                id = UUID.randomUUID().toString(),
                title = obj.optString("title", "Untitled").ifBlank { "Untitled" },
                body = obj.optString("body", ""),
                font = obj.optString("font", "sans").ifBlank { "sans" },
                fontSizeSp = obj.optInt("fontSizeSp", 18).coerceIn(14, 28),
                noteColor = obj.optInt("noteColor", 0xFF0ECDBE.toInt()),
                textColor = obj.optInt("textColor", 0xFF080C0E.toInt()),
                accentColor = obj.optInt("accentColor", 0xFF84FFEE.toInt()),
                noteGlow = obj.optBoolean("noteGlow", false),
                glowStrength = obj.optInt("glowStrength", 1).coerceIn(0, 4),
                songUri = obj.optString("songUri", ""),
                recordings = recordings,
                createdAt = obj.optLong("createdAt", now),
                updatedAt = now
            )
            val msg = if (recordings.isEmpty()) {
                "Loaded note with style."
            } else {
                "Loaded note with style + ${recordings.size} recording(s)."
            }
            Result(true, msg, note)
        } catch (error: Exception) {
            extracted.values.forEach { runCatching { it.delete() } }
            Log.e(TAG, "import_parse_failed", error)
            Result(false, error.message ?: "Could not parse note pack.")
        }
    }

    private fun isSafeZipPath(name: String): Boolean {
        if (name.isBlank() || name.startsWith("../") || name.contains("/../")) return false
        if (name.contains("..")) return false
        return true
    }
}
