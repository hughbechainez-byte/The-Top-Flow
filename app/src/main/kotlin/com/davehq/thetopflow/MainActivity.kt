package com.davehq.thetopflow

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.metrics.performance.FrameData
import androidx.metrics.performance.JankStats
import com.davehq.thetopflow.ui.NotesRoute
import com.davehq.thetopflow.ui.NotesTheme

class MainActivity : ComponentActivity() {
    private val notesViewModel: NotesViewModel by viewModels()
    private var jankStats: JankStats? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        installJankStats()
        setContent {
            val state = notesViewModel.uiState.collectAsStateWithLifecycle().value
            NotesTheme {
                NotesRoute(
                    state = state,
                    onCreateNote = notesViewModel::createNote,
                    onOpenNote = notesViewModel::openNote,
                    onCloseEditor = notesViewModel::closeEditor,
                    onSearch = notesViewModel::updateSearch,
                    onTitleChange = notesViewModel::updateTitle,
                    onBodyChange = notesViewModel::updateBody,
                    onDeleteNote = notesViewModel::deleteSelected
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        jankStats?.isTrackingEnabled = BuildConfig.DEBUG
    }

    override fun onPause() {
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
