package com.guitarlearning.domain.usecase

import com.guitarlearning.domain.model.AudioNote
import com.guitarlearning.domain.repository.AudioNoteRepository

class AddAudioNoteUseCase(private val audioNoteRepository: AudioNoteRepository) {
    suspend operator fun invoke(audioNote: AudioNote) {
        audioNoteRepository.addAudioNote(audioNote)
    }
}
