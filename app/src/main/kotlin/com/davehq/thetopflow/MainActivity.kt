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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
            val state by notesViewModel.uiState.collectAsStateWithLifecycle()
            NotesTheme(neonTheme = state.styleDefaults.neonTheme) {
                Box(modifier = Modifier.fillMaxSize()) {
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
                                    this@MainActivity,
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
                        onOpenRecentNote = notesViewModel::openMostRecentOrNewestNote
                    )
                    // Portable note pack controls — save includes style + recordings.
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (state.selectedNote != null) {
                            FilledTonalButton(
                                onClick = {
                                    val name = notesViewModel.selectedNotePackFileName()
                                    if (name == null) {
                                        Toast.makeText(
                                            this@MainActivity,
                                            "Open a note first.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        exportNoteLauncher.launch(name)
                                    }
                                }
                            ) {
                                Text("Save note")
                            }
                        }
                        OutlinedButton(
                            onClick = {
                                importNoteLauncher.launch(
                                    arrayOf(
                                        NotePack.MIME_TYPE,
                                        "application/octet-stream",
                                        "*/*"
                                    )
                                )
                            }
                        ) {
                            Text("Load note")
                        }
                    }
                }
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
