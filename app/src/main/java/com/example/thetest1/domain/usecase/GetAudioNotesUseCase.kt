package com.example.thetest1.domain.usecase

import com.example.thetest1.domain.model.AudioNote
import com.example.thetest1.domain.repository.AudioNoteRepository
import kotlinx.coroutines.flow.Flow

class GetAudioNotesUseCase(private val audioNoteRepository: AudioNoteRepository) {
    operator fun invoke(lessonId: String): Flow<List<AudioNote>> {
        return audioNoteRepository.getAudioNotes(lessonId)
    }
}
