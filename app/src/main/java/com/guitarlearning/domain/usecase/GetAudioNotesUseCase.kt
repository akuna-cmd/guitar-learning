package com.guitarlearning.domain.usecase

import com.guitarlearning.domain.model.AudioNote
import com.guitarlearning.domain.repository.AudioNoteRepository
import kotlinx.coroutines.flow.Flow

class GetAudioNotesUseCase(private val audioNoteRepository: AudioNoteRepository) {
    operator fun invoke(lessonId: String): Flow<List<AudioNote>> {
        return audioNoteRepository.getAudioNotes(lessonId)
    }
}
