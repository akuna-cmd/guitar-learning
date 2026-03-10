package com.example.thetest1.data.repository

import com.example.thetest1.data.local.AudioNoteDao
import com.example.thetest1.domain.model.AudioNote
import com.example.thetest1.domain.repository.AudioNoteRepository
import kotlinx.coroutines.flow.Flow

class AudioNoteRepositoryImpl(
    private val audioNoteDao: AudioNoteDao
) : AudioNoteRepository {

    override fun getAudioNotes(lessonId: String): Flow<List<AudioNote>> {
        return audioNoteDao.getNotesForLesson(lessonId)
    }

    override suspend fun addAudioNote(audioNote: AudioNote) {
        audioNoteDao.insert(audioNote)
    }

    override suspend fun deleteAudioNote(id: Int) {
        audioNoteDao.delete(id)
    }
}