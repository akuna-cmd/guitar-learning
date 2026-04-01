package com.example.thetest1.domain.usecase

import com.example.thetest1.domain.repository.AudioNoteRepository

class DeleteAudioNoteUseCase(private val audioNoteRepository: AudioNoteRepository) {
    suspend operator fun invoke(id: Int) {
        audioNoteRepository.deleteAudioNote(id)
    }
}
