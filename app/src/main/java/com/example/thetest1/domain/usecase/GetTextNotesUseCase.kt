package com.example.thetest1.domain.usecase

import com.example.thetest1.domain.model.TextNote
import com.example.thetest1.domain.repository.TextNoteRepository
import kotlinx.coroutines.flow.Flow

class GetTextNotesUseCase(private val textNoteRepository: TextNoteRepository) {
    operator fun invoke(lessonId: String): Flow<List<TextNote>> {
        return textNoteRepository.getTextNotes(lessonId)
    }
}