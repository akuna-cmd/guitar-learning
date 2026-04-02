package com.guitarlearning.domain.usecase

import com.guitarlearning.domain.repository.AudioNoteRepository

class DeleteAudioNoteUseCase(private val audioNoteRepository: AudioNoteRepository) {
    suspend operator fun invoke(id: Int) {
        audioNoteRepository.deleteAudioNote(id)
    }
}
