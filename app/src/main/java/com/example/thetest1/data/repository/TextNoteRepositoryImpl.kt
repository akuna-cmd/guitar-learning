package com.example.thetest1.data.repository

import com.example.thetest1.data.local.TextNoteDao
import com.example.thetest1.domain.model.TextNote
import com.example.thetest1.domain.repository.TextNoteRepository
import kotlinx.coroutines.flow.Flow

class TextNoteRepositoryImpl(
    private val textNoteDao: TextNoteDao
) : TextNoteRepository {

    override fun getTextNotes(lessonId: String): Flow<List<TextNote>> {
        return textNoteDao.getTextNotesForLesson(lessonId)
    }

    override suspend fun addTextNote(textNote: TextNote) {
        textNoteDao.insertTextNote(textNote)
    }

    override suspend fun updateTextNote(textNote: TextNote) {
        textNoteDao.updateTextNote(textNote)
    }

    override suspend fun deleteTextNote(textNote: TextNote) {
        textNoteDao.deleteTextNote(textNote)
    }
}
