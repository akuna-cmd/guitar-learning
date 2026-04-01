package com.example.thetest1.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.thetest1.R
import com.example.thetest1.data.local.TabDao
import com.example.thetest1.data.model.Lesson
import com.example.thetest1.data.model.toDomain
import com.example.thetest1.domain.model.Difficulty
import com.example.thetest1.domain.model.Lesson as DomainLesson
import com.example.thetest1.domain.model.TabItem
import com.example.thetest1.domain.repository.TabRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.util.UUID

class TabRepositoryImpl(
    private val context: Context,
    private val tabDao: TabDao
) : TabRepository {

    private companion object {
        const val DefaultUserTabFileExtension = "gp"
        val SupportedUserTabExtensions = setOf("gp", "gp3", "gp4", "gp5", "gpx")
    }

    private val lessonsFromJson: List<Lesson> by lazy {
        try {
            val inputStream = context.assets.open("lessons.json")
            val reader = InputStreamReader(inputStream)
            val lessonListType = object : TypeToken<List<Lesson>>() {}.type
            Gson().fromJson(reader, lessonListType)
        } catch (e: Exception) {
            emptyList()
        }
    }

    override fun getTabs(): Flow<List<TabItem>> = flow {
        val tabsFromDb = tabDao.getTabs().first()
        if (tabsFromDb.isEmpty()) {
            val tabsToInsert = lessonsFromJson.mapIndexed { index, lesson ->
                TabItem(
                    id = lesson.id,
                    name = lesson.title,
                    description = lesson.description,
                    difficulty = when (lesson.level) {
                        "beginner" -> Difficulty.BEGINNER
                        "intermediate" -> Difficulty.INTERMEDIATE
                        "advanced" -> Difficulty.ADVANCED
                        else -> Difficulty.BEGINNER
                    },
                    lessonNumber = index + 1,
                    isCompleted = false,
                    isUserTab = false,
                    tagsCsv = buildString {
                        append(lesson.level)
                        append(",lesson")
                    },
                    folder = "Без папки",
                    updatedAt = 0L,
                    offlineReady = true
                )
            }
            tabDao.insertTabs(tabsToInsert)
            emit(tabsToInsert)
        } else {
            emit(tabsFromDb)
        }
    }

    override fun observeUserTabs(): Flow<List<TabItem>> = tabDao.getUserTabs()

    override suspend fun getAllTabsSync(): List<TabItem> {
        return tabDao.getTabs().first() + tabDao.getUserTabs().first()
    }

    override suspend fun getLesson(id: String): DomainLesson? {
        val tab = tabDao.getTabById(id)
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
        return lessonsFromJson.find { it.id == id }?.toDomain()
    }

    override suspend fun updateTab(tab: TabItem) {
        tabDao.updateTab(tab.copy(updatedAt = System.currentTimeMillis()))
    }

    override suspend fun upsertTabs(tabs: List<TabItem>) {
        if (tabs.isEmpty()) return
        tabDao.insertTabs(tabs)
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
        val file = File(context.filesDir, "${UUID.randomUUID()}.$extension")
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException(context.getString(R.string.user_tab_open_failed))

        inputStream.use { stream ->
            FileOutputStream(file).use { outputStream ->
                stream.copyTo(outputStream)
            }
        }

        val asciiTabs = context.getString(R.string.user_tab_ascii_placeholder, fileName)

        val tabItem = TabItem(
            id = UUID.randomUUID().toString(),
            name = fileName,
            description = context.getString(R.string.user_tab_description),
            difficulty = Difficulty.BEGINNER,
            lessonNumber = 0,
            isCompleted = false,
            isUserTab = true,
            filePath = file.absolutePath,
            asciiTabs = asciiTabs,
            tagsCsv = "custom,user",
            folder = "Без папки",
            updatedAt = System.currentTimeMillis()
        )
        tabDao.insertTab(tabItem)
    }

    override suspend fun getUserTabs(): List<TabItem> {
        return tabDao.getUserTabs().first()
    }

    override fun getUserTabsCount(): Flow<Int> {
        return tabDao.getUserTabsCount()
    }

    override suspend fun deleteUserTab(tab: TabItem) {
        tab.filePath?.let { File(it).delete() }
        tabDao.deleteTab(tab)
    }

    override suspend fun deleteTabs(tabs: List<TabItem>) {
        tabs.forEach { deleteUserTab(it) }
    }

    override suspend fun deleteAllUserTabs() {
        getUserTabs().forEach { deleteUserTab(it) }
    }

    override suspend fun renameUserTab(tab: TabItem, newName: String) {
        val updatedTab = tab.copy(name = newName, updatedAt = System.currentTimeMillis())
        tabDao.updateTab(updatedTab)
    }

    override suspend fun getTabById(id: String): TabItem? {
        return tabDao.getTabById(id)
    }

    override suspend fun markTabOpened(tabId: String, openedAt: Long) {
        val tab = tabDao.getTabById(tabId) ?: return
        tabDao.updateTab(
            tab.copy(
                openCount = tab.openCount + 1,
                lastOpenedAt = openedAt,
                updatedAt = openedAt
            )
        )
    }

    override suspend fun updateTabTags(tabId: String, tags: List<String>) {
        val tab = tabDao.getTabById(tabId) ?: return
        tabDao.updateTab(
            tab.copy(
                tagsCsv = tags
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .distinct()
                    .joinToString(","),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun updateTabFolder(tabId: String, folder: String) {
        val tab = tabDao.getTabById(tabId) ?: return
        tabDao.updateTab(tab.copy(folder = folder, updatedAt = System.currentTimeMillis()))
    }

    override suspend fun markOfflineReady(tabId: String, ready: Boolean) {
        val tab = tabDao.getTabById(tabId) ?: return
        tabDao.updateTab(tab.copy(offlineReady = ready, updatedAt = System.currentTimeMillis()))
    }

    override suspend fun clearAllTabs() {
        getUserTabs().forEach { tab ->
            tab.filePath?.let { File(it).delete() }
        }
        tabDao.deleteAllTabs()
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
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != -1) {
                if (cut != null) {
                    result = result?.substring(cut + 1)
                }
            }
        }
        return result
    }

    private fun getFileName(uri: Uri): String? {
        return getDisplayName(uri)?.substringBeforeLast('.')
    }
}
