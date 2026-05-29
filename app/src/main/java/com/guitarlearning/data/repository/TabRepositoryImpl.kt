package com.guitarlearning.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.guitarlearning.R
import com.guitarlearning.core.locale.AppLocaleManager
import com.guitarlearning.data.local.dao.TabDao
import com.guitarlearning.data.local.entity.toDomain
import com.guitarlearning.data.local.entity.toEntity
import com.guitarlearning.data.model.LessonDto
import com.guitarlearning.data.model.toDomain
import com.guitarlearning.data.tabs.DefaultUserTabFileExtension
import com.guitarlearning.data.tabs.SupportedUserTabExtensions
import com.guitarlearning.data.tabs.buildBuiltInTabs
import com.guitarlearning.data.tabs.fallbackDisplayNameFromPath
import com.guitarlearning.data.tabs.localizeBuiltInTabs
import com.guitarlearning.data.tabs.normalizeTags
import com.guitarlearning.domain.model.Difficulty
import com.guitarlearning.domain.model.DEFAULT_TAB_FOLDER_KEY
import com.guitarlearning.domain.model.Lesson as DomainLesson
import com.guitarlearning.domain.model.TabItem
import com.guitarlearning.domain.repository.AppSettingsRepository
import com.guitarlearning.domain.repository.TabPlaybackProgressRepository
import com.guitarlearning.domain.repository.TabRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.util.UUID
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TabRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val tabDao: TabDao,
    private val appSettingsRepository: AppSettingsRepository,
    private val tabPlaybackProgressRepository: TabPlaybackProgressRepository
) : TabRepository {
    private val lessonsFromJson: List<LessonDto> by lazy {
        try {
            val inputStream = context.assets.open("lessons/lessons.json")
            val reader = InputStreamReader(inputStream)
            val lessonListType = object : TypeToken<List<LessonDto>>() {}.type
            Gson().fromJson(reader, lessonListType)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun shouldUseEnglishDescriptions(): Boolean {
        return AppLocaleManager.getSavedLanguageTag(context).startsWith("en", ignoreCase = true)
    }

    private fun localizedLessonDescriptionMap(): Map<String, String> {
        val useEnglishDescriptions = shouldUseEnglishDescriptions()
        return lessonsFromJson.associate { lesson ->
            lesson.id to lesson.localizedDescription(useEnglishDescriptions)
        }
    }

    override fun getTabs(): Flow<List<TabItem>> = flow {
        seedBuiltInTabsIfNeeded()
        emitAll(
            tabDao.getTabs().map { storedTabs ->
                localizeStoredBuiltInTabs(storedTabs.map { it.toDomain() })
            }
        )
    }

    override suspend fun refreshBuiltInTabLocalizations() {
        localizeStoredBuiltInTabs(tabDao.getTabs().first().map { it.toDomain() })
    }

    override fun observeUserTabs(): Flow<List<TabItem>> =
        tabDao.getUserTabs().map { tabs -> tabs.map { it.toDomain() } }

    override suspend fun getAllTabsSync(): List<TabItem> {
        return tabDao.getTabs().first().map { it.toDomain() } +
            tabDao.getUserTabs().first().map { it.toDomain() }
    }

    override suspend fun getLesson(id: String): DomainLesson? {
        val useEnglishDescriptions = shouldUseEnglishDescriptions()
        val tab = tabDao.getTabById(id)?.toDomain()
        if (tab?.isUserTab == true) {
            return tab.filePath
                ?.takeIf { path -> File(path).exists() }
                ?.let {
                DomainLesson(
                    id = tab.id,
                    level = tab.difficulty.name.lowercase(),
                    order = tab.lessonNumber,
                    title = tab.name,
                    description = tab.description,
                    text = tab.asciiTabs ?: "",
                    tabsAscii = tab.asciiTabs ?: "",
                    tabsGpPath = it
                )
            }
        }
        return lessonsFromJson.find { it.id == id }?.toDomain(useEnglishDescriptions)
    }

    override suspend fun updateTab(tab: TabItem) {
        tabDao.updateTab(tab.copy(updatedAt = System.currentTimeMillis()).toEntity())
    }

    override suspend fun upsertTabs(tabs: List<TabItem>) {
        if (tabs.isEmpty()) return
        tabDao.insertTabs(tabs.map { it.toEntity() })
    }

    override fun getCompletedLessonsCount(): Flow<Int> {
        return tabDao.getTabs().map { tabs -> tabs.count { it.isCompleted } }
    }

    override fun getTotalLessonsCount(): Flow<Int> {
        return tabDao.getTabs().map { it.size }
    }

    override suspend fun addUserTab(uriString: String) {
        val uri = Uri.parse(uriString)
        val displayName = getDisplayName(uri)
        val extension = displayName
            ?.substringAfterLast('.', "")
            ?.lowercase()
            ?.takeIf { it.isNotBlank() }
            ?: DefaultUserTabFileExtension

        require(extension in SupportedUserTabExtensions) {
            context.getString(
                R.string.user_tab_invalid_format,
                SupportedUserTabExtensions.joinToString(", ") { ".$it" }
            )
        }

        val fileName = getFileName(uri) ?: context.getString(
            R.string.user_tab_default_name,
            UUID.randomUUID().toString()
        )
        val file = copyUserTabToInternalStorage(uri, extension)

        val asciiTabs = context.getString(R.string.user_tab_ascii_placeholder, fileName)
        tabDao.insertTab(
            createUserTab(fileName = fileName, filePath = file.absolutePath, asciiTabs = asciiTabs).toEntity()
        )
    }

    override suspend fun getUserTabs(): List<TabItem> {
        return tabDao.getUserTabs().first().map { it.toDomain() }
    }

    override fun getUserTabsCount(): Flow<Int> {
        return tabDao.getUserTabsCount()
    }

    override suspend fun deleteUserTab(tab: TabItem) {
        if (tab.isUserTab) {
            appSettingsRepository.markUserTabPendingDeletion(tab.id)
        }
        tab.filePath?.let { File(it).delete() }
        tabDao.deleteTab(tab.toEntity())
        tabPlaybackProgressRepository.removeByTabId(tab.id)
    }

    override suspend fun deleteTabs(tabs: List<TabItem>) {
        tabs.forEach { deleteUserTab(it) }
    }

    override suspend fun deleteAllUserTabs() {
        getUserTabs().forEach { deleteUserTab(it) }
    }

    override suspend fun renameUserTab(tab: TabItem, newName: String) {
        val updatedTab = tab.copy(name = newName, updatedAt = System.currentTimeMillis())
        tabDao.updateTab(updatedTab.toEntity())
    }

    override suspend fun getTabById(id: String): TabItem? {
        return tabDao.getTabById(id)?.toDomain()
    }

    override suspend fun markTabOpened(tabId: String, openedAt: Long) {
        val tab = tabDao.getTabById(tabId)?.toDomain() ?: return
        tabDao.updateTab(
            tab.copy(
                openCount = tab.openCount + 1,
                lastOpenedAt = openedAt,
                updatedAt = openedAt
            ).toEntity()
        )
    }

    override suspend fun updateTabTags(tabId: String, tags: List<String>) {
        val tab = tabDao.getTabById(tabId)?.toDomain() ?: return
        tabDao.updateTab(
            tab.copy(
                tagsCsv = normalizeTags(tags),
                updatedAt = System.currentTimeMillis()
            ).toEntity()
        )
    }

    override suspend fun updateTabFolder(tabId: String, folder: String) {
        val tab = tabDao.getTabById(tabId)?.toDomain() ?: return
        tabDao.updateTab(tab.copy(folder = folder, updatedAt = System.currentTimeMillis()).toEntity())
    }

    override suspend fun markOfflineReady(tabId: String, ready: Boolean) {
        val tab = tabDao.getTabById(tabId)?.toDomain() ?: return
        tabDao.updateTab(tab.copy(offlineReady = ready, updatedAt = System.currentTimeMillis()).toEntity())
    }

    override suspend fun clearAllTabs() {
        getUserTabs().forEach { tab ->
            tab.filePath?.let { File(it).delete() }
        }
        tabDao.deleteAllTabs()
        appSettingsRepository.clearAllPendingDeletedUserTabIds()
        tabPlaybackProgressRepository.clearAll()
    }

    private fun getDisplayName(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    val displayNameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (displayNameIndex != -1) {
                        result = cursor.getString(displayNameIndex)
                    }
                }
            } finally {
                cursor?.close()
            }
        }
        if (result == null) {
            result = fallbackDisplayNameFromPath(uri.path)
        }
        return result
    }

    private fun getFileName(uri: Uri): String? {
        return getDisplayName(uri)?.substringBeforeLast('.')
    }

    private suspend fun seedBuiltInTabsIfNeeded() {
        if (tabDao.getTabs().first().isNotEmpty()) return
        tabDao.insertTabs(
            buildBuiltInTabs(lessonsFromJson, shouldUseEnglishDescriptions()).map { it.toEntity() }
        )
    }

    private suspend fun localizeStoredBuiltInTabs(storedTabs: List<TabItem>): List<TabItem> {
        val localizedTabs = localizeBuiltInTabs(storedTabs, localizedLessonDescriptionMap())
        val changedTabs = localizedTabs.filterIndexed { index, tab -> tab != storedTabs[index] }
        if (changedTabs.isNotEmpty()) {
            tabDao.insertTabs(changedTabs.map { it.toEntity() })
        }
        return localizedTabs
    }

    private fun copyUserTabToInternalStorage(uri: Uri, extension: String): File {
        val file = File(context.filesDir, "${UUID.randomUUID()}.$extension")
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException(context.getString(R.string.user_tab_open_failed))

        inputStream.use { stream ->
            FileOutputStream(file).use { outputStream ->
                stream.copyTo(outputStream)
            }
        }
        return file
    }

    private fun createUserTab(
        fileName: String,
        filePath: String,
        asciiTabs: String
    ): TabItem {
        val createdAt = System.currentTimeMillis()
        return TabItem(
            id = UUID.randomUUID().toString(),
            name = fileName,
            description = context.getString(R.string.user_tab_description),
            difficulty = Difficulty.BEGINNER,
            lessonNumber = 0,
            isCompleted = false,
            isUserTab = true,
            filePath = filePath,
            asciiTabs = asciiTabs,
            tagsCsv = "custom,user",
            folder = DEFAULT_TAB_FOLDER_KEY,
            createdAt = createdAt,
            updatedAt = createdAt
        )
    }
}
