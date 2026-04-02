package com.guitarlearning.domain.usecase

import com.guitarlearning.domain.model.TextNote
import com.guitarlearning.domain.repository.TextNoteRepository

class DeleteTextNoteUseCase(private val textNoteRepository: TextNoteRepository) {
    suspend operator fun invoke(textNote: TextNote) {
        textNoteRepository.deleteTextNote(textNote)
    }
}
