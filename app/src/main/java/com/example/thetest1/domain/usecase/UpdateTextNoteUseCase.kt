package com.example.thetest1.domain.usecase

import com.example.thetest1.domain.model.TextNote
import com.example.thetest1.domain.repository.TextNoteRepository

class UpdateTextNoteUseCase(private val textNoteRepository: TextNoteRepository) {
    suspend operator fun invoke(textNote: TextNote) {
        textNoteRepository.updateTextNote(textNote)
    }
}
