package com.example.thetest1.presentation.audio_notes

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class TrackState(
    val id: String,
    val position: Int = 0,
    val duration: Int = 0,
    val progress: Float = 0f
)

data class PlayerState(
    val currentPlayingId: String? = null,
    val isPlaying: Boolean = false,
    val trackStates: Map<String, TrackState> = emptyMap()
)

class AudioPlayer(
    private val context: Context,
    mainDispatcher: CoroutineDispatcher
) {
    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState = _playerState.asStateFlow()

    private val scope = CoroutineScope(mainDispatcher)

    fun onPlay(id: String, filePath: String) {
        val currentState = _playerState.value
        val isSameTrack = currentState.currentPlayingId == id

        if (isSameTrack) {
            if (currentState.isPlaying) {
                pause()
            } else {
                resume()
            }
        } else {
            stopCurrentTrack()
            startNew(id, filePath)
        }
    }

    private fun stopCurrentTrack() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
            }
            val oldId = _playerState.value.currentPlayingId
            if (oldId != null) {
                updateTrackState(oldId) {
                    it.copy(
                        position = mediaPlayer?.currentPosition ?: it.position
                    )
                }
            }
            it.release()
        }
        mediaPlayer = null
        progressJob?.cancel()
    }

    private fun startNew(id: String, filePath: String) {
        try {
            val fileUri = Uri.fromFile(File(filePath))
            val trackState = getOrCreateTrackState(id)

            mediaPlayer = MediaPlayer.create(context, fileUri).apply {
                setOnCompletionListener { onPlaybackCompleted() }
                val newDuration =
                    if (trackState.duration == 0) this.duration else trackState.duration
                updateTrackState(id) { it.copy(duration = newDuration) }

                seekTo(trackState.position)
                start()
            }

            _playerState.update { it.copy(currentPlayingId = id, isPlaying = true) }
            startProgressUpdates()
        } catch (e: Exception) {
            release()
        }
    }

    private fun pause() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                updateTrackState(_playerState.value.currentPlayingId) {
                    it.copy(
                        position = mediaPlayer?.currentPosition ?: it.position
                    )
                }
            }
        }
        progressJob?.cancel()
        _playerState.update { it.copy(isPlaying = false) }
    }

    private fun resume() {
        mediaPlayer?.let {
            val trackState = _playerState.value.trackStates[_playerState.value.currentPlayingId]
            if (trackState != null && trackState.position >= trackState.duration - 100) {
                it.seekTo(0)
                updateTrackState(_playerState.value.currentPlayingId) {
                    it.copy(
                        position = 0,
                        progress = 0f
                    )
                }
            }
            it.start()
            _playerState.update { it.copy(isPlaying = true) }
            startProgressUpdates()
        }
    }

    private fun onPlaybackCompleted() {
        progressJob?.cancel()
        val currentId = _playerState.value.currentPlayingId
        if (currentId != null) {
            updateTrackState(currentId) { it.copy(position = 0, progress = 1f) }
        }
        _playerState.update { it.copy(isPlaying = false) }
    }

    fun seek(trackId: String, newProgress: Float) {
        val trackState = getOrCreateTrackState(trackId)
        if (trackState.duration > 0) {
            val newPosition = (trackState.duration * newProgress).toInt()
            if (_playerState.value.currentPlayingId == trackId) {
                mediaPlayer?.seekTo(newPosition)
            }
            updateTrackState(trackId) { it.copy(position = newPosition, progress = newProgress) }
        }
    }

    private fun getOrCreateTrackState(id: String): TrackState {
        return _playerState.value.trackStates[id] ?: TrackState(id)
    }

    private fun updateTrackState(id: String?, operation: (TrackState) -> TrackState) {
        if (id == null) return
        val currentTrackState = getOrCreateTrackState(id)
        val newTrackState = operation(currentTrackState)
        val newStates = _playerState.value.trackStates.toMutableMap()
        newStates[id] = newTrackState
        _playerState.update { it.copy(trackStates = newStates) }
    }

    private fun startProgressUpdates() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (_playerState.value.isPlaying) {
                val currentId = _playerState.value.currentPlayingId
                mediaPlayer?.let { player ->
                    if (player.isPlaying) {
                        val duration = player.duration.takeIf { it > 0 } ?: 1
                        val progress = player.currentPosition.toFloat() / duration.toFloat()
                        updateTrackState(currentId) {
                            it.copy(
                                position = player.currentPosition,
                                progress = progress
                            )
                        }
                    }
                }
                delay(100)
            }
        }
    }

    fun release() {
        stopCurrentTrack()
        _playerState.value = PlayerState()
    }
}
