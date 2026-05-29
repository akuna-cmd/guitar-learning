package com.guitarlearning.domain.usecase

import com.guitarlearning.domain.model.TabPlaybackProgress
import com.guitarlearning.domain.repository.TabPlaybackProgressRepository
import com.guitarlearning.domain.repository.TabRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

class ObserveContinueLearningUseCase @Inject constructor(
    private val progressRepository: TabPlaybackProgressRepository,
    private val tabRepository: TabRepository
) {
    operator fun invoke(): Flow<TabPlaybackProgress?> {
        return progressRepository.observeAll()
            .combine(tabRepository.getTabs().combine(tabRepository.observeUserTabs()) { tabs, userTabs ->
                tabs + userTabs
            }) { progressList, allTabs ->
                val latestOpenedTab = allTabs.maxByOrNull { it.lastOpenedAt }
                latestOpenedTab?.let { tab ->
                    progressList.firstOrNull { it.tabId == tab.id } ?: TabPlaybackProgress(
                        tabId = tab.id,
                        tabName = tab.name,
                        lastTick = 0L,
                        lastBarIndex = 0,
                        totalBars = 0,
                        updatedAt = tab.lastOpenedAt
                    )
                }
            }
            .distinctUntilChanged()
    }
}
