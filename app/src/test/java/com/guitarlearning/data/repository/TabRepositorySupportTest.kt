package com.guitarlearning.data.repository

import com.guitarlearning.data.model.LessonDto
import com.guitarlearning.domain.model.Difficulty
import com.guitarlearning.domain.model.TabItem
import org.junit.Assert.assertEquals
import org.junit.Test

class TabRepositorySupportTest {

    @Test
    fun buildBuiltInTabs_mapsLessonLevelsAndMetadata() {
        val lessons = listOf(
            lesson(id = "b", level = "beginner"),
            lesson(id = "i", level = "intermediate"),
            lesson(id = "a", level = "advanced"),
            lesson(id = "u", level = "unknown")
        )

        val tabs = buildBuiltInTabs(lessons, useEnglishDescriptions = true)

        assertEquals(
            listOf(
                Difficulty.BEGINNER,
                Difficulty.INTERMEDIATE,
                Difficulty.ADVANCED,
                Difficulty.BEGINNER
            ),
            tabs.map(TabItem::difficulty)
        )
        assertEquals(listOf(1, 2, 3, 4), tabs.map(TabItem::lessonNumber))
        assertEquals(listOf("beginner,lesson", "intermediate,lesson", "advanced,lesson", "unknown,lesson"), tabs.map(TabItem::tagsCsv))
        assertEquals(listOf("EN-b", "EN-i", "EN-a", "EN-u"), tabs.map(TabItem::description))
    }

    @Test
    fun localizeBuiltInTabs_updatesOnlyTabsWithKnownDescriptions() {
        val storedTabs = listOf(
            TabItem(id = "1", name = "One", description = "old", difficulty = Difficulty.BEGINNER, lessonNumber = 1),
            TabItem(id = "2", name = "Two", description = "same", difficulty = Difficulty.BEGINNER, lessonNumber = 2),
            TabItem(id = "3", name = "Three", description = "keep", difficulty = Difficulty.BEGINNER, lessonNumber = 3)
        )

        val localized = localizeBuiltInTabs(
            storedTabs = storedTabs,
            lessonDescriptionsById = mapOf(
                "1" to "new",
                "2" to "same"
            )
        )

        assertEquals("new", localized[0].description)
        assertEquals(storedTabs[1], localized[1])
        assertEquals(storedTabs[2], localized[2])
    }

    @Test
    fun normalizeTags_trimsRemovesBlanksAndDuplicates() {
        assertEquals("rock,metal,jazz", normalizeTags(listOf(" rock ", "", "metal", "rock", "jazz ")))
    }

    @Test
    fun fallbackDisplayNameFromPath_extractsLastSegment() {
        assertEquals("song.gp5", fallbackDisplayNameFromPath("/storage/emulated/0/Music/song.gp5"))
        assertEquals("solo", fallbackDisplayNameFromPath("solo"))
        assertEquals(null, fallbackDisplayNameFromPath(""))
    }

    private fun lesson(id: String, level: String): LessonDto {
        return LessonDto(
            id = id,
            level = level,
            order = 1,
            title = "Title-$id",
            description = "UA-$id",
            descriptionEn = "EN-$id",
            text = "",
            tabsAscii = "",
            tabsGpPath = ""
        )
    }
}
