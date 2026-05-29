package com.guitarlearning.data.repository

import com.guitarlearning.data.local.dao.AudioNoteDao
import com.guitarlearning.data.local.entity.toDomain
import com.guitarlearning.data.local.entity.toEntity
import com.guitarlearning.domain.model.AudioNote
import com.guitarlearning.domain.repository.AudioNoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioNoteRepositoryImpl @Inject constructor(
    private val audioNoteDao: AudioNoteDao
) : AudioNoteRepository {

    override fun getAudioNotes(lessonId: String): Flow<List<AudioNote>> {
        return audioNoteDao.getNotesForLesson(lessonId).map { notes -> notes.map { it.toDomain() } }
    }

    override suspend fun addAudioNote(audioNote: AudioNote) {
        audioNoteDao.insert(audioNote.toEntity())
    }

    override suspend fun updateAudioNote(audioNote: AudioNote) {
        audioNoteDao.update(audioNote.toEntity())
    }

    override suspend fun deleteAudioNote(id: Int) {
        audioNoteDao.delete(id)
    }
}
