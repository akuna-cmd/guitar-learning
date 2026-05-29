package com.guitarlearning.data.audio_notes.media

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
