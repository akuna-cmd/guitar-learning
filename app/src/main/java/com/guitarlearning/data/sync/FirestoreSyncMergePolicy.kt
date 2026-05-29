package com.guitarlearning.data.sync

import com.guitarlearning.core.preferences.AppSettingsSnapshot
import com.guitarlearning.domain.model.AudioNote
import com.guitarlearning.domain.model.Goal
import com.guitarlearning.domain.model.TabItem
import com.guitarlearning.domain.model.TabPlaybackProgress
import com.guitarlearning.domain.model.TextNote
import java.io.File

internal data class MergeTabsResult(
    val tabs: List<TabItem>,
    val tabsToDelete: List<TabItem>
)

internal class FirestoreSyncMergePolicy(
    private val mapper: FirestoreSyncMapper
) {
    fun mergeSettings(
        local: AppSettingsSnapshot,
        remote: AppSettingsSnapshot?,
        preferRemote: Boolean
    ): AppSettingsSnapshot {
        if (remote == null) return local
        if (preferRemote) return remote
        return if (remote.updatedAt > local.updatedAt) remote else local
    }

    fun mergeTabCollections(
        localTabs: List<TabItem>,
        remoteTabs: List<TabItem>
    ): MergeTabsResult {
        val mergedTabs = mutableListOf<TabItem>()
        val localTabsToDelete = mutableListOf<TabItem>()
        val matchedLocalIds = mutableSetOf<String>()

        remoteTabs.forEach { remote ->
            val localById = localTabs.firstOrNull { it.id == remote.id }
            if (localById != null) {
                matchedLocalIds += localById.id
                mergedTabs += mergeTabs(localById, remote)
            } else {
                val localByKey = localTabs.firstOrNull {
                    it.id !in matchedLocalIds && canonicalTabKey(it) == canonicalTabKey(remote)
                }
                if (localByKey != null) {
                    matchedLocalIds += localByKey.id
                    if (localByKey.id != remote.id && localByKey.isUserTab) {
                        localTabsToDelete += localByKey
                    }
                    mergedTabs += mergeTabs(localByKey, remote)
                } else {
                    mergedTabs += remote
                }
            }
        }

        localTabs.forEach { local ->
            val hasRemoteById = remoteTabs.any { it.id == local.id }
            val hasRemoteByKey = remoteTabs.any { canonicalTabKey(it) == canonicalTabKey(local) }
            if (!hasRemoteById && !hasRemoteByKey && local.id !in matchedLocalIds) {
                mergedTabs += local
            }
        }

        val deduped = mergedTabs
            .groupBy(mapper::canonicalTabIdentity)
            .map { (_, variants) -> variants.reduce(::mergeTabs) }

        return MergeTabsResult(deduped, localTabsToDelete.distinctBy { it.id })
    }

    fun mergeGoals(localGoals: List<Goal>, remoteGoals: List<Goal>): List<Goal> {
        return (localGoals + remoteGoals)
            .groupBy { it.syncId.ifBlank { mapper.goalFingerprint(it) } }
            .map { (_, variants) -> variants.maxByOrNull { it.updatedAt } ?: variants.first() }
            .sortedBy { it.deadline }
    }

    fun mergeProgress(
        localProgress: List<TabPlaybackProgress>,
        remoteProgress: List<TabPlaybackProgress>
    ): List<TabPlaybackProgress> {
        return (localProgress + remoteProgress)
            .groupBy { it.tabId }
            .map { (_, variants) -> variants.maxByOrNull { it.updatedAt } ?: variants.first() }
    }

    fun mergeTextNotes(
        localNotes: List<TextNote>,
        remoteNotes: List<TextNote>
    ): List<TextNote> {
        return (localNotes + remoteNotes)
            .groupBy(mapper::textNoteDocumentId)
            .map { (_, variants) ->
                variants.reduce { current, candidate ->
                    val preferredContent = when {
                        current.content == candidate.content -> current.content
                        current.content.isBlank() -> candidate.content
                        candidate.content.isBlank() -> current.content
                        else -> current.content
                    }
                    current.copy(
                        content = preferredContent,
                        isFavorite = current.isFavorite || candidate.isFavorite,
                        createdAt = if (current.createdAt.time <= candidate.createdAt.time) {
                            current.createdAt
                        } else {
                            candidate.createdAt
                        }
                    )
                }
            }
            .sortedByDescending { it.createdAt.time }
    }

    fun mergeAudioNotes(
        localNotes: List<AudioNote>,
        remoteNotes: List<AudioNote>
    ): List<AudioNote> {
        return (localNotes + remoteNotes)
            .groupBy(mapper::audioNoteDocumentId)
            .map { (_, variants) ->
                variants.reduce { current, candidate ->
                    val currentFileExists = File(current.filePath).exists()
                    val candidateFileExists = File(candidate.filePath).exists()
                    current.copy(
                        filePath = when {
                            currentFileExists -> current.filePath
                            candidateFileExists -> candidate.filePath
                            current.filePath.isNotBlank() -> current.filePath
                            else -> candidate.filePath
                        },
                        isFavorite = current.isFavorite || candidate.isFavorite,
                        createdAt = if (current.createdAt.time <= candidate.createdAt.time) {
                            current.createdAt
                        } else {
                            candidate.createdAt
                        }
                    )
                }
            }
            .sortedByDescending { it.createdAt.time }
    }

    private fun mergeTabs(local: TabItem, remote: TabItem): TabItem {
        val newer = if (local.updatedAt >= remote.updatedAt) local else remote
        val older = if (newer === local) remote else local

        return newer.copy(
            id = remote.id.ifBlank { local.id },
            name = newer.name.ifBlank { older.name },
            description = newer.description.ifBlank { older.description },
            difficulty = newer.difficulty,
            lessonNumber = if (newer.lessonNumber != 0) newer.lessonNumber else older.lessonNumber,
            isCompleted = if (local.updatedAt == remote.updatedAt) {
                local.isCompleted || remote.isCompleted
            } else {
                newer.isCompleted
            },
            isUserTab = local.isUserTab || remote.isUserTab,
            filePath = newer.filePath ?: older.filePath,
            asciiTabs = newer.asciiTabs ?: older.asciiTabs,
            tagsCsv = mapper.mergeTags(local.tagsCsv, remote.tagsCsv),
            folder = newer.folder.takeIf { it.isNotBlank() } ?: older.folder,
            openCount = maxOf(local.openCount, remote.openCount),
            lastOpenedAt = maxOf(local.lastOpenedAt, remote.lastOpenedAt),
            createdAt = listOf(local.createdAt, remote.createdAt)
                .filter { it > 0L }
                .minOrNull()
                ?: 0L,
            updatedAt = maxOf(local.updatedAt, remote.updatedAt),
            offlineReady = local.offlineReady || remote.offlineReady
        )
    }

    private fun canonicalTabKey(tab: TabItem): String {
        return "${tab.isUserTab}|${tab.name.trim().lowercase().replace(Regex("\\s+"), " ")}|${tab.lessonNumber}"
    }
}
