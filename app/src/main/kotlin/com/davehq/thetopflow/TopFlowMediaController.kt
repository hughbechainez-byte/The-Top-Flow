package com.davehq.thetopflow

import android.app.Application
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Immutable
data class MediaUiState(
    val songPlaying: Boolean = false,
    val songPositionMs: Int = 0,
    val songDurationMs: Int = 0,
    val recordingActive: Boolean = false,
    val recordingElapsedMs: Int = 0,
    val recordingPlaybackPath: String? = null,
    val recordingPlaybackActive: Boolean = false,
    val rapReadyAmount: Float = RapReadyProcessor.DEFAULT_AMOUNT,
    val mediaErrorMessage: String? = null
)

class TopFlowMediaController(
    private val application: Application
) {
    private val context = application.applicationContext
    private val controllerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val rapReady = RapReadyProcessor()

    private val _mediaState = MutableStateFlow(MediaUiState())
    val mediaState: StateFlow<MediaUiState> = _mediaState.asStateFlow()

    private var songPlayer: MediaPlayer? = null
    private var recordingPlayer: MediaPlayer? = null
    private var recorder: MediaRecorder? = null
    private var activeRecording: File? = null
    private var recordingStartedAtMs: Long = 0L
    private var tickerJob: Job? = null
    private var attachedSongUri: String = ""
    private var songSeekMs: Int = 0

    fun setRapReadyAmount(amount: Float) {
        val next = amount.coerceIn(0f, 100f)
        _mediaState.update { it.copy(rapReadyAmount = next) }
        recordingPlayer?.let { player ->
            if (_mediaState.value.recordingPlaybackActive) {
                rapReady.apply(player, next)
            }
        }
    }

    fun attachSong(uri: String, onPersisted: () -> Unit = {}) {
        if (_mediaState.value.recordingActive) {
            onPersisted()
            return
        }
        attachedSongUri = uri
        stopSongPlayback(resetPosition = true)
        onPersisted()
        _mediaState.update {
            it.copy(
                songDurationMs = 0,
                songPositionMs = 0,
                songPlaying = false,
                mediaErrorMessage = null
            )
        }
    }

    fun toggleSong(songUri: String) {
        if (songUri.isBlank()) {
            _mediaState.update { it.copy(mediaErrorMessage = "Attach a song first.") }
            return
        }
        if (recorder != null) {
            _mediaState.update { it.copy(mediaErrorMessage = "Stop recording first.") }
            return
        }
        stopRecordingPlayback()
        if (songPlayer != null && _mediaState.value.songPlaying && attachedSongUri == songUri) {
            pauseSong()
            return
        }
        val started = ensureSongPlayer(songUri)
        if (!started) return
        startSongWithResume()
    }

    fun seekSong(songUri: String, positionMs: Int) {
        if (songUri.isBlank() || attachedSongUri != songUri) return
        val player = songPlayer ?: return
        if (positionMs < 0) return
        val duration = player.duration.coerceAtLeast(1)
        player.seekTo(positionMs.coerceIn(0, duration))
        _mediaState.update { it.copy(songPositionMs = positionMs.coerceIn(0, duration)) }
    }

    fun startRecording(outputDir: File, optionalSongUri: String?) {
        if (recorder != null) return
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }
        stopSongPlayback(resetPosition = false)
        stopRecordingPlayback()
        val now = System.currentTimeMillis()
        val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date(now))
        val newRecording = File(outputDir, "top-flow-$stamp.m4a")
        try {
            val mr = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(newRecording.absolutePath)
                prepare()
                start()
            }
            recorder = mr
            activeRecording = newRecording
            recordingStartedAtMs = now
            _mediaState.update {
                it.copy(
                    recordingActive = true,
                    recordingElapsedMs = 0,
                    mediaErrorMessage = null
                )
            }
            optionalSongUri?.takeIf { it.isNotBlank() }?.let {
                startSongForRecording(it)
            }
            startTicker()
        } catch (e: Exception) {
            stopRecording(save = false)
            newRecording.delete()
            _mediaState.update { it.copy(mediaErrorMessage = "Could not start recording.") }
        }
    }

    fun stopRecording(save: Boolean): File? {
        val wasSongPlaying = songPlayer?.isPlaying == true
        stopTicker()
        if (recorder == null) {
            val dangling = activeRecording
            if (!save) dangling?.delete()
            activeRecording = null
            _mediaState.update {
                it.copy(recordingActive = false, recordingElapsedMs = 0)
            }
            recordingStartedAtMs = 0L
            return null
        }
        try {
            recorder?.stop()
        } catch (_: Exception) {
        } finally {
            try {
                recorder?.release()
            } finally {
                recorder = null
            }
        }
        activeRecording?.let { file ->
            if (!save) {
                file.delete()
                activeRecording = null
                if (wasSongPlaying) stopSongPlayback(resetPosition = false)
                _mediaState.update {
                    it.copy(
                        recordingActive = false,
                        recordingElapsedMs = 0,
                        mediaErrorMessage = null
                    )
                }
                recordingStartedAtMs = 0L
                return null
            }
            if (!file.exists() || file.length() <= 0L) {
                _mediaState.update { it.copy(recordingActive = false, recordingElapsedMs = 0) }
                if (wasSongPlaying) stopSongPlayback(resetPosition = false)
                recordingStartedAtMs = 0L
                return null
            }
            activeRecording = null
            if (wasSongPlaying) stopSongPlayback(resetPosition = false)
            _mediaState.update {
                it.copy(
                    recordingActive = false,
                    recordingElapsedMs = 0,
                    mediaErrorMessage = null
                )
            }
            recordingStartedAtMs = 0L
            return file
        }
        _mediaState.update {
            it.copy(recordingActive = false, recordingElapsedMs = 0)
        }
        recordingStartedAtMs = 0L
        return null
    }

    fun playRecording(path: String) {
        if (path.isBlank()) return
        if (recordingPlayer != null && _mediaState.value.recordingPlaybackPath == path && _mediaState.value.recordingPlaybackActive) {
            stopRecordingPlayback()
            return
        }
        stopSongPlayback(resetPosition = false)
        stopRecordingPlayback()
        try {
            val player = MediaPlayer().apply {
                setDataSource(context, Uri.fromFile(File(path)))
                setOnCompletionListener {
                    rapReady.release()
                    _mediaState.update {
                        it.copy(recordingPlaybackActive = false, recordingPlaybackPath = null)
                    }
                    releaseRecordingPlayer()
                }
                prepare()
            }
            recordingPlayer = player
            // Apply Rap Ready chain before start so first samples are processed
            rapReady.apply(player, _mediaState.value.rapReadyAmount)
            _mediaState.update {
                it.copy(
                    recordingPlaybackActive = true,
                    recordingPlaybackPath = path,
                    mediaErrorMessage = null
                )
            }
            player.start()
            startTicker()
        } catch (_: Exception) {
            _mediaState.update { it.copy(mediaErrorMessage = "Could not play recording.") }
            releaseRecordingPlayer()
        }
    }

    fun stopRecordingPlayback() {
        rapReady.release()
        releaseRecordingPlayer()
        _mediaState.update {
            it.copy(recordingPlaybackActive = false)
        }
    }

    fun pauseAll() {
        val saved = saveMediaStateSnapshot()
        if (_mediaState.value.recordingActive) {
            stopRecording(save = false)
        }
        if (songPlayer?.isPlaying == true) {
            pauseSong()
        }
        if (recordingPlayer != null && _mediaState.value.recordingPlaybackActive) {
            stopRecordingPlayback()
        }
        stopTicker()
        _mediaState.update {
            it.copy(mediaErrorMessage = null, songPositionMs = saved.songPositionMs)
        }
    }

    fun release() {
        pauseAll()
        stopRecording(save = false)
        stopSongPlayback(resetPosition = false)
        rapReady.release()
        controllerScope.cancel()
    }

    fun pauseSong() {
        val player = songPlayer ?: return
        if (!player.isPlaying) return
        try {
            player.pause()
            songSeekMs = player.currentPosition
            _mediaState.update {
                it.copy(
                    songPlaying = false,
                    songPositionMs = player.currentPosition
                )
            }
        } catch (_: Exception) {
        }
    }

    private fun startSongWithResume() {
        val player = songPlayer ?: return
        try {
            if (songSeekMs > 0 && !player.isPlaying) {
                player.seekTo(songSeekMs)
            }
            player.start()
            _mediaState.update {
                it.copy(
                    songPlaying = true,
                    songDurationMs = player.duration.coerceAtLeast(0),
                    songPositionMs = player.currentPosition
                )
            }
            startTicker()
        } catch (_: Exception) {
            _mediaState.update { it.copy(mediaErrorMessage = "Could not play song.") }
        }
    }

    private fun startSongForRecording(songUri: String) {
        if (!ensureSongPlayer(songUri)) return
        startSongWithResume()
    }

    private fun ensureSongPlayer(songUri: String): Boolean {
        if (songPlayer != null && attachedSongUri == songUri) return true
        stopSongPlayback(resetPosition = false)
        return try {
            songPlayer = MediaPlayer.create(context, Uri.parse(songUri)).apply {
                setOnCompletionListener {
                    _mediaState.update {
                        it.copy(songPlaying = false, songPositionMs = 0, songDurationMs = 0)
                    }
                    stopSongPlayback(resetPosition = true)
                }
                attachedSongUri = songUri
                _mediaState.update {
                    it.copy(songDurationMs = duration.coerceAtLeast(0), mediaErrorMessage = null)
                }
            }
            true
        } catch (_: Exception) {
            _mediaState.update { it.copy(mediaErrorMessage = "Could not play song.") }
            false
        }
    }

    private fun stopSongPlayback(resetPosition: Boolean) {
        if (!resetPosition) {
            songSeekMs = songPlayer?.currentPosition ?: 0
        } else {
            songSeekMs = 0
        }
        try {
            songPlayer?.stop()
        } catch (_: Exception) {
        } finally {
            try {
                songPlayer?.release()
            } catch (_: Exception) {
            } finally {
                songPlayer = null
                _mediaState.update {
                    it.copy(songPlaying = false)
                }
            }
        }
        if (resetPosition) {
            _mediaState.update {
                it.copy(songPositionMs = 0, songDurationMs = 0, mediaErrorMessage = null)
            }
        }
    }

    private fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    private fun startTicker() {
        stopTicker()
        tickerJob = controllerScope.launch {
            while (true) {
                updateMediaProgress()
                if (!_mediaState.value.songPlaying &&
                    !_mediaState.value.recordingPlaybackActive &&
                    !_mediaState.value.recordingActive
                ) {
                    break
                }
                delay(500L)
            }
            stopTicker()
        }
    }

    private fun updateMediaProgress() {
        _mediaState.update { current ->
            val nextSongPosition = if (songPlayer != null && current.songPlaying) {
                songPlayer?.currentPosition?.coerceAtLeast(0) ?: current.songPositionMs
            } else {
                current.songPositionMs
            }
            val nextSongDuration = songPlayer?.duration?.coerceAtLeast(0) ?: current.songDurationMs
            val nextRecordingElapsed = if (recordingStartedAtMs > 0L && _mediaState.value.recordingActive) {
                (System.currentTimeMillis() - recordingStartedAtMs).toInt()
            } else {
                0
            }
            current.copy(
                songPositionMs = nextSongPosition,
                songDurationMs = nextSongDuration,
                recordingElapsedMs = nextRecordingElapsed
            )
        }
    }

    private fun releaseRecordingPlayer() {
        rapReady.release()
        try {
            recordingPlayer?.stop()
        } catch (_: Exception) {
        } finally {
            try {
                recordingPlayer?.release()
            } catch (_: Exception) {
            } finally {
                recordingPlayer = null
                if (!_mediaState.value.recordingActive) {
                    _mediaState.update {
                        it.copy(
                            recordingPlaybackPath = null,
                            recordingPlaybackActive = false
                        )
                    }
                } else {
                    _mediaState.update { it.copy(recordingPlaybackActive = false) }
                }
            }
        }
    }

    private fun saveMediaStateSnapshot() = _mediaState.value
}
