package com.davehq.thetopflow

import android.os.Bundle
import android.util.Log
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.metrics.performance.FrameData
import androidx.metrics.performance.JankStats
import com.davehq.thetopflow.data.NotePack
import com.davehq.thetopflow.ui.NotesRoute
import com.davehq.thetopflow.ui.NotesTheme
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    private val notesViewModel: NotesViewModel by viewModels()
    private var jankStats: JankStats? = null
    private lateinit var attachSongLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var recordPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var exportNoteLauncher: ActivityResultLauncher<String>
    private lateinit var importNoteLauncher: ActivityResultLauncher<Array<String>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        attachSongLauncher = registerForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri ->
            if (uri == null) return@registerForActivityResult
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            try {
                contentResolver.takePersistableUriPermission(uri, takeFlags)
            } catch (_: Exception) {
            }
            notesViewModel.attachSong(uri.toString())
        }
        recordPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) notesViewModel.startRecording()
        }
        exportNoteLauncher = registerForActivityResult(
            ActivityResultContracts.CreateDocument(NotePack.MIME_TYPE)
        ) { uri ->
            if (uri == null) return@registerForActivityResult
            val result = notesViewModel.exportSelectedNotePack(uri)
            Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
        }
        importNoteLauncher = registerForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri ->
            if (uri == null) return@registerForActivityResult
            val result = notesViewModel.importNotePack(uri)
            Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
        }
        installJankStats()
        setContent {
            val state = notesViewModel.uiState.collectAsStateWithLifecycle().value
            NotesTheme(neonTheme = state.styleDefaults.neonTheme) {
                NotesRoute(
                    state = state,
                    onCreateNote = notesViewModel::createNote,
                    onOpenNote = notesViewModel::openNote,
                    onCloseEditor = notesViewModel::closeEditor,
                    onSearch = notesViewModel::updateSearch,
                    onTitleChange = notesViewModel::updateTitle,
                    onBodyChange = notesViewModel::updateBody,
                    onSetRhymeModeEnabled = notesViewModel::setRhymeModeEnabled,
                    onSetRhymeStrictness = notesViewModel::setRhymeStrictness,
                    onSaveCustomRhymes = notesViewModel::saveCustomRhymes,
                    onRequestMoreRhymes = notesViewModel::requestMoreRhymes,
                    onDeleteNote = notesViewModel::deleteSelected,
                    onAttachSong = { attachSongLauncher.launch(arrayOf("audio/*")) },
                    onToggleSong = notesViewModel::toggleSong,
                    onSeekSong = notesViewModel::seekSong,
                    onStartRecording = {
                        if (ContextCompat.checkSelfPermission(
                                this,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            notesViewModel.startRecording()
                        } else {
                            recordPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    onStopRecording = notesViewModel::stopRecording,
                    onPlayRecording = notesViewModel::playRecording,
                    onRenameRecording = notesViewModel::renameRecording,
                    onExportRecording = notesViewModel::exportRecording,
                    onApplyStyle = notesViewModel::updateNoteStyle,
                    onSetNeonThemeEnabled = notesViewModel::setNeonThemeEnabled,
                    onOpenRecentNote = notesViewModel::openMostRecentOrNewestNote,
                    onExportNotePack = {
                        val name = notesViewModel.selectedNotePackFileName()
                        if (name == null) {
                            Toast.makeText(this, "Open a note first.", Toast.LENGTH_SHORT).show()
                        } else {
                            exportNoteLauncher.launch(name)
                        }
                    },
                    onImportNotePack = {
                        importNoteLauncher.launch(
                            arrayOf(
                                NotePack.MIME_TYPE,
                                "application/octet-stream",
                                "*/*"
                            )
                        )
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        jankStats?.isTrackingEnabled = BuildConfig.DEBUG
    }

    override fun onPause() {
        notesViewModel.pauseMediaPlayback()
        notesViewModel.flushPendingSave()
        jankStats?.isTrackingEnabled = false
        super.onPause()
    }

    private fun installJankStats() {
        if (!BuildConfig.DEBUG) return
        jankStats = JankStats.createAndTrack(window) { frameData: FrameData ->
            if (frameData.isJank) {
                Log.d("TopFlowJank", frameData.toString())
            }
        }.apply {
            isTrackingEnabled = false
        }
    }
}
