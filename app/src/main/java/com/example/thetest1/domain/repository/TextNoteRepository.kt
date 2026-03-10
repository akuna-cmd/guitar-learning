package com.example.thetest1.domain.repository

import com.example.thetest1.domain.model.TextNote
import kotlinx.coroutines.flow.Flow

interface TextNoteRepository {
    fun getTextNotes(lessonId: String): Flow<List<TextNote>>
    suspend fun addTextNote(textNote: TextNote)
    suspend fun updateTextNote(textNote: TextNote)
    suspend fun deleteTextNote(textNote: TextNote)
}
