package com.guitarlearning.domain.usecase

import com.guitarlearning.domain.model.TextNote
import com.guitarlearning.domain.repository.TextNoteRepository

class AddTextNoteUseCase(private val textNoteRepository: TextNoteRepository) {
    suspend operator fun invoke(textNote: TextNote) {
        textNoteRepository.addTextNote(textNote)
    }
}
