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
        updatedAt = updatedAt,
        isCompleted = isCompleted,
        isOverdue = isOverdue
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
        updatedAt = updatedAt,
        isCompleted = isCompleted,
        isOverdue = isOverdue
    )

fun TabEntity.toDomain(): TabItem =
    TabItem(
        id = id,
        name = name,
        description = description,
        difficulty = difficulty,
        lessonNumber = lessonNumber,
        isCompleted = isCompleted,
        isUserTab = isUserTab,
        filePath = filePath,
        asciiTabs = asciiTabs,
        tagsCsv = tagsCsv,
        folder = folder,
        openCount = openCount,
        lastOpenedAt = lastOpenedAt,
        createdAt = createdAt,
        updatedAt = updatedAt,
        offlineReady = offlineReady
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
        tagsCsv = tagsCsv,
        folder = folder,
        openCount = openCount,
        lastOpenedAt = lastOpenedAt,
        createdAt = createdAt,
        updatedAt = updatedAt,
        offlineReady = offlineReady
    )
