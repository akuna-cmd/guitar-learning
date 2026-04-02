package com.guitarlearning.domain.usecase

import com.guitarlearning.domain.model.Lesson
import com.guitarlearning.domain.repository.TabRepository

class GetLessonUseCase(private val tabRepository: TabRepository) {
    suspend operator fun invoke(id: String): Lesson? {
        return tabRepository.getLesson(id)
    }
}
