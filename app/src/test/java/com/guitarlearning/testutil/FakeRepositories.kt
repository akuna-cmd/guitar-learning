package com.guitarlearning.testutil

import com.guitarlearning.domain.model.Difficulty
import com.guitarlearning.domain.model.Lesson
import com.guitarlearning.domain.model.Session
import com.guitarlearning.domain.model.TabItem
import com.guitarlearning.domain.model.TabPlaybackProgress
import com.guitarlearning.domain.repository.SessionRepository
import com.guitarlearning.domain.repository.SoundFontRepository
import com.guitarlearning.domain.repository.TabFileRepository
import com.guitarlearning.domain.repository.TabPlaybackProgressRepository
import com.guitarlearning.domain.repository.TabRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.util.Date

class FakeSessionRepository(
    initialSessions: List<Session> = emptyList()
) : SessionRepository {
    val sessionsFlow = MutableStateFlow(initialSessions)
    val addedSessions = mutableListOf<Session>()

    override fun getAllSessions(): Flow<List<Session>> = sessionsFlow

    override suspend fun getAllSessionsSync(): List<Session> = sessionsFlow.value

    override fun getSessionsSince(since: Date): Flow<List<Session>> {
        return sessionsFlow.map { sessions ->
            sessions.filter { !it.startTime.before(since) }
        }
    }

    override suspend fun addSession(session: Session) {
        addedSessions += session
        sessionsFlow.value = sessionsFlow.value + session
    }

    override suspend fun importHistory(sessions: List<Session>) {
        sessionsFlow.value = sessions
    }

    override suspend fun clearHistory() {
        sessionsFlow.value = emptyList()
    }
}

class FakeTabRepository(
    initialTabs: List<TabItem> = emptyList(),
    initialUserTabs: List<TabItem> = emptyList()
) : TabRepository {
    val tabsFlow = MutableStateFlow(initialTabs)
    val userTabsFlow = MutableStateFlow(initialUserTabs)
    val folderUpdates = mutableListOf<Pair<String, String>>()
    val openedTabIds = mutableListOf<String>()

    override fun getTabs(): Flow<List<TabItem>> = tabsFlow

    override suspend fun refreshBuiltInTabLocalizations() = Unit

    override fun observeUserTabs(): Flow<List<TabItem>> = userTabsFlow

    override suspend fun getAllTabsSync(): List<TabItem> = tabsFlow.value + userTabsFlow.value

    override suspend fun getLesson(id: String): Lesson? = null

    override suspend fun updateTab(tab: TabItem) {
        tabsFlow.update { tabs -> tabs.map { if (it.id == tab.id) tab else it } }
        userTabsFlow.update { tabs -> tabs.map { if (it.id == tab.id) tab else it } }
    }

    override suspend fun upsertTabs(tabs: List<TabItem>) {
        tabsFlow.value = tabs
    }

    override fun getCompletedLessonsCount(): Flow<Int> {
        return tabsFlow.map { tabs -> tabs.count { it.isCompleted } }
    }

    override fun getTotalLessonsCount(): Flow<Int> {
        return tabsFlow.map { it.size }
    }

    override suspend fun addUserTab(uriString: String) = Unit

    override suspend fun getUserTabs(): List<TabItem> = userTabsFlow.value

    override fun getUserTabsCount(): Flow<Int> {
        return userTabsFlow.map { it.size }
    }

    override suspend fun deleteUserTab(tab: TabItem) {
        userTabsFlow.update { tabs -> tabs.filterNot { it.id == tab.id } }
    }

    override suspend fun deleteTabs(tabs: List<TabItem>) {
        val ids = tabs.map { it.id }.toSet()
        userTabsFlow.update { current -> current.filterNot { it.id in ids } }
    }

    override suspend fun deleteAllUserTabs() {
        userTabsFlow.value = emptyList()
    }

    override suspend fun clearAllTabs() {
        tabsFlow.value = emptyList()
        userTabsFlow.value = emptyList()
    }

    override suspend fun renameUserTab(tab: TabItem, newName: String) {
        updateTab(tab.copy(name = newName))
    }

    override suspend fun getTabById(id: String): TabItem? {
        return (tabsFlow.value + userTabsFlow.value).firstOrNull { it.id == id }
    }

    override suspend fun markTabOpened(tabId: String, openedAt: Long) {
        openedTabIds += tabId
    }

    override suspend fun updateTabTags(tabId: String, tags: List<String>) {
        val updatedTags = tags.joinToString(",")
        val current = getTabById(tabId) ?: return
        updateTab(current.copy(tagsCsv = updatedTags))
    }

    override suspend fun updateTabFolder(tabId: String, folder: String) {
        folderUpdates += tabId to folder
        val current = getTabById(tabId) ?: return
        updateTab(current.copy(folder = folder))
    }

    override suspend fun markOfflineReady(tabId: String, ready: Boolean) {
        val current = getTabById(tabId) ?: return
        updateTab(current.copy(offlineReady = ready))
    }
}

class FakeTabPlaybackProgressRepository(
    initialProgress: List<TabPlaybackProgress> = emptyList()
) : TabPlaybackProgressRepository {
    val progressFlow = MutableStateFlow(initialProgress)

    override fun observeAll(): Flow<List<TabPlaybackProgress>> = progressFlow

    override suspend fun getByTabId(tabId: String): TabPlaybackProgress? {
        return progressFlow.value.firstOrNull { it.tabId == tabId }
    }

    override suspend fun upsert(progress: TabPlaybackProgress) {
        progressFlow.update { list ->
            list.filterNot { it.tabId == progress.tabId } + progress
        }
    }

    override suspend fun replaceAll(progressList: List<TabPlaybackProgress>) {
        progressFlow.value = progressList
    }

    override suspend fun clearAll() {
        progressFlow.value = emptyList()
    }
}

class FakeTabFileRepository(
    private val bytes: ByteArray = byteArrayOf(1, 2, 3)
) : TabFileRepository {
    override suspend fun readTabBytes(path: String): ByteArray = bytes
}

class FakeSoundFontRepository(
    private val bytes: ByteArray = byteArrayOf(4, 5, 6)
) : SoundFontRepository {
    override suspend fun readSoundFontBytes(): ByteArray = bytes
}

fun testTabItem(
    id: String,
    name: String = id,
    difficulty: Difficulty = Difficulty.BEGINNER,
    folder: String = "Без папки",
    isCompleted: Boolean = false,
    isUserTab: Boolean = false
): TabItem {
    return TabItem(
        id = id,
        name = name,
        description = "desc-$id",
        difficulty = difficulty,
        lessonNumber = 1,
        isCompleted = isCompleted,
        isUserTab = isUserTab,
        folder = folder
    )
}
