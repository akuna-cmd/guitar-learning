package com.example.thetest1.domain.repository

import com.example.thetest1.domain.model.AudioNote
import kotlinx.coroutines.flow.Flow

interface AudioNoteRepository {
    fun getAudioNotes(lessonId: String): Flow<List<AudioNote>>
    suspend fun addAudioNote(audioNote: AudioNote)
    suspend fun deleteAudioNote(id: Int)
}