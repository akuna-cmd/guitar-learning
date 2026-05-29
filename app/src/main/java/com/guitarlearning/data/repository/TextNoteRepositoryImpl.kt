package com.guitarlearning.data.repository

import com.guitarlearning.data.local.dao.TextNoteDao
import com.guitarlearning.data.local.entity.toDomain
import com.guitarlearning.data.local.entity.toEntity
import com.guitarlearning.domain.model.TextNote
import com.guitarlearning.domain.repository.TextNoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TextNoteRepositoryImpl @Inject constructor(
    private val textNoteDao: TextNoteDao
) : TextNoteRepository {

    override fun getTextNotes(lessonId: String): Flow<List<TextNote>> {
        return textNoteDao.getTextNotesForLesson(lessonId).map { notes -> notes.map { it.toDomain() } }
    }

    override suspend fun addTextNote(textNote: TextNote) {
        textNoteDao.insertTextNote(textNote.toEntity())
    }

    override suspend fun updateTextNote(textNote: TextNote) {
        textNoteDao.updateTextNote(textNote.toEntity())
    }

    override suspend fun deleteTextNote(textNote: TextNote) {
        textNoteDao.deleteTextNote(textNote.toEntity())
    }
}
