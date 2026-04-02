package com.guitarlearning.domain.usecase

import com.guitarlearning.domain.model.TextNote
import com.guitarlearning.domain.repository.TextNoteRepository
import kotlinx.coroutines.flow.Flow

class GetTextNotesUseCase(private val textNoteRepository: TextNoteRepository) {
    operator fun invoke(lessonId: String): Flow<List<TextNote>> {
        return textNoteRepository.getTextNotes(lessonId)
    }
}
