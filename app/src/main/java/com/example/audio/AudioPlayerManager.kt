package com.example.audio

import android.content.Context
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

data class PlayerState(
    val isLoaded: Boolean = false,
    val isPlaying: Boolean = false,
    val currentPositionMs: Int = 0,
    val durationMs: Int = 0,
    val currentFilePath: String? = null,
    val playbackSpeed: Float = 1.0f,
    val volume: Float = 1.0f
)

class AudioPlayerManager(
    private val context: Context,
    private val coroutineScope: CoroutineScope
) {
    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    fun loadAndPlay(filePath: String, speed: Float = 1.0f, autoPlay: Boolean = true) {
        val file = File(filePath)
        if (!file.exists()) {
            Log.e("AudioPlayerManager", "File does not exist: $filePath")
            return
        }

        stop()

        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                setOnPreparedListener { mp ->
                    val duration = mp.duration.coerceAtLeast(0)
                    applySpeed(speed)
                    val vol = _playerState.value.volume
                    mp.setVolume(vol, vol)
                    _playerState.update {
                        it.copy(
                            isLoaded = true,
                            isPlaying = autoPlay,
                            currentPositionMs = 0,
                            durationMs = duration,
                            currentFilePath = filePath,
                            playbackSpeed = speed
                        )
                    }
                    if (autoPlay) {
                        mp.start()
                        startProgressTicker()
                    }
                }
                setOnCompletionListener {
                    _playerState.update { it.copy(isPlaying = false, currentPositionMs = it.durationMs) }
                    stopProgressTicker()
                }
                setOnErrorListener { _, what, extra ->
                    Log.e("AudioPlayerManager", "MediaPlayer error: what=$what extra=$extra")
                    _playerState.update { it.copy(isPlaying = false) }
                    stopProgressTicker()
                    true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e("AudioPlayerManager", "Failed to initialize MediaPlayer", e)
        }
    }

    fun togglePlayPause() {
        val mp = mediaPlayer ?: return
        if (_playerState.value.isPlaying) {
            mp.pause()
            stopProgressTicker()
            _playerState.update { it.copy(isPlaying = false) }
        } else {
            // If finished, seek to beginning
            if (_playerState.value.currentPositionMs >= _playerState.value.durationMs) {
                mp.seekTo(0)
            }
            mp.start()
            startProgressTicker()
            _playerState.update { it.copy(isPlaying = true) }
        }
    }

    fun seekTo(positionMs: Int) {
        val mp = mediaPlayer ?: return
        val clamped = positionMs.coerceIn(0, _playerState.value.durationMs)
        mp.seekTo(clamped)
        _playerState.update { it.copy(currentPositionMs = clamped) }
    }

    fun setSpeed(speed: Float) {
        _playerState.update { it.copy(playbackSpeed = speed) }
        applySpeed(speed)
    }

    fun setVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        _playerState.update { it.copy(volume = clamped) }
        mediaPlayer?.setVolume(clamped, clamped)
    }

    private fun applySpeed(speed: Float) {
        val mp = mediaPlayer ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val params = mp.playbackParams ?: PlaybackParams()
                params.speed = speed
                mp.playbackParams = params
            }
        } catch (e: Exception) {
            Log.w("AudioPlayerManager", "Could not set playback speed: ${e.message}")
        }
    }

    private fun startProgressTicker() {
        progressJob?.cancel()
        progressJob = coroutineScope.launch(Dispatchers.Main) {
            while (isActive) {
                val mp = mediaPlayer
                if (mp != null && _playerState.value.isPlaying) {
                    try {
                        val current = mp.currentPosition
                        val dur = mp.duration.coerceAtLeast(_playerState.value.durationMs)
                        _playerState.update {
                            it.copy(
                                currentPositionMs = current,
                                durationMs = dur
                            )
                        }
                    } catch (_: Exception) {
                    }
                }
                delay(80)
            }
        }
    }

    private fun stopProgressTicker() {
        progressJob?.cancel()
        progressJob = null
    }

    fun stop() {
        stopProgressTicker()
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {
        }
        mediaPlayer = null
        _playerState.update {
            it.copy(isPlaying = false, isLoaded = false, currentPositionMs = 0)
        }
    }

    fun release() {
        stop()
    }
}
