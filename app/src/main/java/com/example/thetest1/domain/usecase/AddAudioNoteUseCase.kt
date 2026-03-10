package com.example.thetest1.domain.usecase

import com.example.thetest1.domain.model.AudioNote
import com.example.thetest1.domain.repository.AudioNoteRepository

class AddAudioNoteUseCase(private val audioNoteRepository: AudioNoteRepository) {
    suspend operator fun invoke(audioNote: AudioNote) {
        audioNoteRepository.addAudioNote(audioNote)
    }
}