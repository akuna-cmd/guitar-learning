package com.guitarlearning.data.local.entity

import com.guitarlearning.domain.model.AudioNote
import com.guitarlearning.domain.model.Goal
import com.guitarlearning.domain.model.TabItem
import com.guitarlearning.domain.model.TextNote

fun AudioNoteEntity.toDomain(): AudioNote =
    AudioNote(
        id = id,
        lessonId = lessonId,
        filePath = filePath,
        createdAt = createdAt,
        isFavorite = isFavorite
    )

fun AudioNote.toEntity(): AudioNoteEntity =
    AudioNoteEntity(
        id = id,
        lessonId = lessonId,
        filePath = filePath,
        createdAt = createdAt,
        isFavorite = isFavorite
    )

fun TextNoteEntity.toDomain(): TextNote =
    TextNote(
        id = id,
        lessonId = lessonId,
        content = content,
        createdAt = createdAt,
        isFavorite = isFavorite
    )

fun TextNote.toEntity(): TextNoteEntity =
    TextNoteEntity(
        id = id,
        lessonId = lessonId,
        content = content,
        createdAt = createdAt,
        isFavorite = isFavorite
    )

fun GoalEntity.toDomain(): Goal =
    Goal(
        id = id,
        syncId = syncId,
        type = type,
        description = description,
        target = target,
        progress = progress,
        deadline = deadline,
        updatedAt = updatedAt
    )

fun Goal.toEntity(): GoalEntity =
    GoalEntity(
        id = id,
        syncId = syncId,
        type = type,
        description = description,
        target = target,
        progress = progress,
        deadline = deadline,
        updatedAt = updatedAt
    )

fun TabWithTags.toDomain(): TabItem =
    TabItem(
        id = tab.id,
        name = tab.name,
        description = tab.description,
        difficulty = tab.difficulty,
        lessonNumber = tab.lessonNumber,
        isCompleted = tab.isCompleted,
        isUserTab = tab.isUserTab,
        filePath = tab.filePath,
        asciiTabs = tab.asciiTabs,
        tags = tags.map(TagEntity::name),
        folder = tab.folder,
        openCount = tab.openCount,
        lastOpenedAt = tab.lastOpenedAt,
        createdAt = tab.createdAt,
        updatedAt = tab.updatedAt,
        offlineReady = tab.offlineReady
    )

fun TabItem.toEntity(): TabEntity =
    TabEntity(
        id = id,
        name = name,
        description = description,
        difficulty = difficulty,
        lessonNumber = lessonNumber,
        isCompleted = isCompleted,
        isUserTab = isUserTab,
        filePath = filePath,
        asciiTabs = asciiTabs,
        folder = folder,
        openCount = openCount,
        lastOpenedAt = lastOpenedAt,
        createdAt = createdAt,
        updatedAt = updatedAt,
        offlineReady = offlineReady
    )
