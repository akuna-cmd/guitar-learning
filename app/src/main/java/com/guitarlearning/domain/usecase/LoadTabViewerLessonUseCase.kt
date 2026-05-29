package com.guitarlearning.domain.usecase

import com.guitarlearning.domain.model.Lesson
import com.guitarlearning.domain.model.TabItem
import com.guitarlearning.domain.model.TabPlaybackProgress
import com.guitarlearning.domain.repository.TabPlaybackProgressRepository
import com.guitarlearning.domain.repository.TabRepository
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

data class TabViewerLessonLoadResult(
    val lesson: Lesson?,
    val tabItem: TabItem?,
    val savedProgress: TabPlaybackProgress?,
    val shouldRestore: Boolean,
    val tabPath: String?
)

class LoadTabViewerLessonUseCase @Inject constructor(
    private val tabRepository: TabRepository,
    private val tabPlaybackProgressRepository: TabPlaybackProgressRepository
) {
    suspend operator fun invoke(id: String): TabViewerLessonLoadResult {
        tabRepository.markTabOpened(id)

        val (lesson, tabItem, savedProgress) = coroutineScope {
            val lessonDeferred = async { tabRepository.getLesson(id) }
            val tabItemDeferred = async { tabRepository.getTabById(id) }
            val progressDeferred = async { tabPlaybackProgressRepository.getByTabId(id) }
            Triple(
                lessonDeferred.await(),
                tabItemDeferred.await(),
                progressDeferred.await()
            )
        }

        val savedBar = savedProgress?.lastBarIndex ?: 0
        val savedTick = savedProgress?.lastTick ?: 0L
        return TabViewerLessonLoadResult(
            lesson = lesson,
            tabItem = tabItem,
            savedProgress = savedProgress,
            shouldRestore = savedProgress != null && (savedBar > 0 || savedTick > 0L),
            tabPath = lesson?.tabsGpPath ?: tabItem?.filePath
        )
    }
}
