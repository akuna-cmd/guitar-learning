package com.example.thetest1.domain.usecase

import com.example.thetest1.domain.model.Lesson
import com.example.thetest1.domain.repository.TabRepository

class GetLessonUseCase(private val tabRepository: TabRepository) {
    suspend operator fun invoke(id: String): Lesson? {
        return tabRepository.getLesson(id)
    }
}
