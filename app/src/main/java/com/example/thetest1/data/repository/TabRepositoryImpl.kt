package com.example.thetest1.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.thetest1.data.local.TabDao
import com.example.thetest1.data.model.Lesson
import com.example.thetest1.domain.model.Difficulty
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
                    isUserTab = false
                )
            }
            tabDao.insertTabs(tabsToInsert)
            emit(tabsToInsert)
        } else {
            emit(tabsFromDb)
        }
    }

    override suspend fun getLesson(id: String): Lesson? {
        val tab = tabDao.getTabById(id)
        if (tab?.isUserTab == true) {
            return tab.filePath?.let {
                Lesson(
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
        return lessonsFromJson.find { it.id == id }
    }

    override suspend fun updateTab(tab: TabItem) {
        tabDao.updateTab(tab)
    }

    override fun getCompletedLessonsCount(): Flow<Int> {
        return tabDao.getTabs().map { tabs -> tabs.count { it.isCompleted } }
    }

    override fun getTotalLessonsCount(): Flow<Int> {
        return tabDao.getTabs().map { it.size }
    }

    override suspend fun addUserTab(uri: Uri) {
        val fileName = getFileName(uri) ?: "user_tab_${UUID.randomUUID()}"
        val file = File(context.filesDir, "${UUID.randomUUID()}.gp")
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            FileOutputStream(file).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }

        val asciiTabs = "Parsed ASCII tabs for $fileName"

        val tabItem = TabItem(
            id = UUID.randomUUID().toString(),
            name = fileName,
            description = "User Tab",
            difficulty = Difficulty.BEGINNER,
            lessonNumber = 0,
            isCompleted = false,
            isUserTab = true,
            filePath = file.absolutePath,
            asciiTabs = asciiTabs
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

    override suspend fun renameUserTab(tab: TabItem, newName: String) {
        val updatedTab = tab.copy(name = newName)
        tabDao.updateTab(updatedTab)
    }

    override suspend fun getTabById(id: String): TabItem? {
        return tabDao.getTabById(id)
    }

    private fun getFileName(uri: Uri): String? {
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
        return result?.substringBeforeLast('.')
    }
}