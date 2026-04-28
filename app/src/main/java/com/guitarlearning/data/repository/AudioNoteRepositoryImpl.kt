package com.guitarlearning.data.repository

import com.guitarlearning.data.local.AudioNoteDao
import com.guitarlearning.domain.model.AudioNote
import com.guitarlearning.domain.repository.AudioNoteRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioNoteRepositoryImpl @Inject constructor(
    private val audioNoteDao: AudioNoteDao
) : AudioNoteRepository {

    override fun getAudioNotes(lessonId: String): Flow<List<AudioNote>> {
        return audioNoteDao.getNotesForLesson(lessonId)
    }

    override suspend fun addAudioNote(audioNote: AudioNote) {
        audioNoteDao.insert(audioNote)
    }

    override suspend fun updateAudioNote(audioNote: AudioNote) {
        audioNoteDao.update(audioNote)
    }

    override suspend fun deleteAudioNote(id: Int) {
        audioNoteDao.delete(id)
    }
}
