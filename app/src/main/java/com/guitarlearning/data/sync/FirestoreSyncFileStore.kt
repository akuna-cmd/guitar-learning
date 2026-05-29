package com.guitarlearning.data.sync

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.guitarlearning.R
import com.guitarlearning.domain.model.AudioNote
import com.guitarlearning.domain.model.TabItem
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import java.io.File
import javax.inject.Inject

internal class FirestoreSyncFileStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val storage: FirebaseStorage
) {
    private companion object {
        const val MaxInlineUserTabBytes = 512 * 1024L
        const val MaxInlineAudioNoteBytes = 512 * 1024L
    }

    suspend fun restoreUserTabFile(
        tabId: String,
        tabName: String,
        originalPath: String?,
        storagePath: String?,
        fileBase64: String?
    ): String? {
        val existing = originalPath?.takeIf { path ->
            runCatching { File(path).exists() }.getOrDefault(false)
        }
        if (existing != null) return existing

        val safeName = tabName
            .ifBlank { tabId }
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
            .take(48)
        val targetDir = File(context.filesDir, "synced_tabs").apply { mkdirs() }
        val targetFile = File(targetDir, "${tabId}_$safeName.gp")

        if (!storagePath.isNullOrBlank()) {
            val restoredFromStorage = runCatching {
                if (!targetFile.exists() || targetFile.length() == 0L) {
                    storage.reference.child(storagePath).getFile(targetFile).await()
                }
                targetFile.absolutePath
            }.getOrNull()
            if (restoredFromStorage != null) return restoredFromStorage
        }

        if (fileBase64.isNullOrBlank()) return null

        return runCatching {
            if (!targetFile.exists() || targetFile.length() == 0L) {
                targetFile.writeBytes(Base64.decode(fileBase64, Base64.DEFAULT))
            }
            targetFile.absolutePath
        }.getOrNull()
    }

    suspend fun restoreAudioNoteFile(
        documentId: String,
        fileName: String?,
        storagePath: String?,
        fileBase64: String?
    ): String? {
        val safeName = fileName
            ?.ifBlank { null }
            ?.replace(Regex("[^A-Za-z0-9._-]"), "_")
            ?.take(64)
            ?: documentId
        val extension = safeName.substringAfterLast('.', "").takeIf { it.isNotBlank() }
        val targetDir = File(context.filesDir, "synced_audio_notes").apply { mkdirs() }
        val targetFile = if (extension != null) {
            File(targetDir, "${documentId}_$safeName")
        } else {
            File(targetDir, documentId)
        }

        if (!storagePath.isNullOrBlank()) {
            val restoredFromStorage = runCatching {
                if (!targetFile.exists() || targetFile.length() == 0L) {
                    storage.reference.child(storagePath).getFile(targetFile).await()
                }
                targetFile.absolutePath
            }.getOrNull()
            if (restoredFromStorage != null) return restoredFromStorage
        }

        if (fileBase64.isNullOrBlank()) return null

        return runCatching {
            if (!targetFile.exists() || targetFile.length() == 0L) {
                targetFile.writeBytes(Base64.decode(fileBase64, Base64.DEFAULT))
            }
            targetFile.absolutePath
        }.getOrNull()
    }

    suspend fun prepareCloudTabFilePayload(
        userId: String,
        tab: TabItem,
        existingStoragePath: String?
    ): CloudTabFilePayload {
        if (!tab.isUserTab) return CloudTabFilePayload(storagePath = null, fileBase64 = null)

        val uploadedStoragePath = uploadUserTabFile(userId, tab)
        if (!uploadedStoragePath.isNullOrBlank()) {
            return CloudTabFilePayload(storagePath = uploadedStoragePath, fileBase64 = null)
        }

        val verifiedExistingStoragePath = existingStoragePath?.takeIf { path ->
            path.isNotBlank() && isStorageObjectAvailable(path)
        }
        val inlineFileBase64 = exportUserTabFileBase64(tab)

        if (!inlineFileBase64.isNullOrBlank()) {
            return CloudTabFilePayload(
                storagePath = verifiedExistingStoragePath,
                fileBase64 = inlineFileBase64
            )
        }

        if (!verifiedExistingStoragePath.isNullOrBlank()) {
            return CloudTabFilePayload(storagePath = verifiedExistingStoragePath, fileBase64 = null)
        }

        throw IllegalStateException(context.getString(R.string.sync_error_tab_file_failed, tab.name))
    }

    suspend fun prepareCloudAudioNoteFilePayload(
        userId: String,
        documentId: String,
        audioNote: AudioNote,
        existingStoragePath: String?
    ): CloudAudioNoteFilePayload {
        val uploadedStoragePath = uploadAudioNoteFile(userId, documentId, audioNote)
        if (!uploadedStoragePath.isNullOrBlank()) {
            return CloudAudioNoteFilePayload(storagePath = uploadedStoragePath, fileBase64 = null)
        }

        val verifiedExistingStoragePath = existingStoragePath?.takeIf { path ->
            path.isNotBlank() && isStorageObjectAvailable(path)
        }
        val inlineFileBase64 = exportAudioNoteFileBase64(audioNote)

        if (!inlineFileBase64.isNullOrBlank()) {
            return CloudAudioNoteFilePayload(
                storagePath = verifiedExistingStoragePath,
                fileBase64 = inlineFileBase64
            )
        }

        if (!verifiedExistingStoragePath.isNullOrBlank()) {
            return CloudAudioNoteFilePayload(storagePath = verifiedExistingStoragePath, fileBase64 = null)
        }

        throw IllegalStateException("Failed to sync audio note file for lesson ${audioNote.lessonId}")
    }

    suspend fun deleteUserTabFile(userId: String, tabId: String) {
        runCatching {
            storage.reference.child(storagePathForTab(userId, tabId)).delete().await()
        }
    }

    suspend fun deleteStaleUserTabFiles(
        userId: String,
        remoteIds: Set<String>,
        localIds: Set<String>
    ) {
        (remoteIds - localIds).forEach { staleId ->
            deleteUserTabFile(userId, staleId)
        }
    }

    suspend fun deleteAllUserTabFiles(userId: String) {
        runCatching {
            val listResult = storage.reference.child("users/$userId/tabs").listAll().await()
            listResult.items.forEach { item ->
                runCatching { item.delete().await() }
            }
        }
    }

    suspend fun deleteStaleAudioNoteFiles(
        userId: String,
        remoteIds: Set<String>,
        localIds: Set<String>,
        remoteStoragePathsById: Map<String, String?>
    ) {
        (remoteIds - localIds).forEach { staleId ->
            val storagePath = remoteStoragePathsById[staleId]
                ?: storagePathForAudioNote(userId, staleId, extension = null)
            deleteAudioNoteFile(storagePath)
        }
    }

    suspend fun deleteAllAudioNoteFiles(userId: String) {
        runCatching {
            val listResult = storage.reference.child("users/$userId/audio_notes").listAll().await()
            listResult.items.forEach { item ->
                runCatching { item.delete().await() }
            }
        }
    }

    fun deleteLocalAudioNoteFiles(audioNotes: List<AudioNote>) {
        audioNotes.forEach { audioNote ->
            runCatching {
                File(audioNote.filePath).takeIf { it.exists() }?.delete()
            }
        }
        runCatching {
            File(context.filesDir, "synced_audio_notes")
                .takeIf { it.exists() }
                ?.listFiles()
                ?.forEach { it.delete() }
        }
    }

    private suspend fun uploadUserTabFile(userId: String, tab: TabItem): String? {
        if (!tab.isUserTab) return null
        val path = tab.filePath ?: return null
        val file = File(path)
        if (!file.exists() || !file.isFile) return null

        val storagePath = storagePathForTab(userId, tab.id)
        return runCatching {
            storage.reference.child(storagePath)
                .putFile(Uri.fromFile(file))
                .await()
            storagePath
        }.getOrNull()
    }

    private suspend fun uploadAudioNoteFile(
        userId: String,
        documentId: String,
        audioNote: AudioNote
    ): String? {
        val file = File(audioNote.filePath)
        if (!file.exists() || !file.isFile) return null

        val storagePath = storagePathForAudioNote(
            userId = userId,
            documentId = documentId,
            extension = file.extension
        )
        return runCatching {
            storage.reference.child(storagePath)
                .putFile(Uri.fromFile(file))
                .await()
            storagePath
        }.getOrNull()
    }

    private fun exportUserTabFileBase64(tab: TabItem): String? {
        if (!tab.isUserTab) return null
        val path = tab.filePath ?: return null
        val file = File(path)
        if (!file.exists() || !file.isFile) return null
        if (file.length() > MaxInlineUserTabBytes) return null
        return runCatching {
            Base64.encodeToString(file.readBytes(), Base64.NO_WRAP)
        }.getOrNull()
    }

    private fun exportAudioNoteFileBase64(audioNote: AudioNote): String? {
        val file = File(audioNote.filePath)
        if (!file.exists() || !file.isFile) return null
        if (file.length() > MaxInlineAudioNoteBytes) return null
        return runCatching {
            Base64.encodeToString(file.readBytes(), Base64.NO_WRAP)
        }.getOrNull()
    }

    private suspend fun isStorageObjectAvailable(storagePath: String): Boolean {
        return runCatching {
            storage.reference.child(storagePath).metadata.await()
            true
        }.getOrDefault(false)
    }

    private suspend fun deleteAudioNoteFile(storagePath: String?) {
        if (storagePath.isNullOrBlank()) return
        runCatching {
            storage.reference.child(storagePath).delete().await()
        }
    }

    private fun storagePathForTab(userId: String, tabId: String): String {
        return "users/$userId/tabs/$tabId.gp"
    }

    private fun storagePathForAudioNote(
        userId: String,
        documentId: String,
        extension: String?
    ): String {
        val normalizedExtension = extension
            ?.trim()
            ?.trimStart('.')
            ?.takeIf { it.isNotBlank() }
            ?.let { ".$it" }
            .orEmpty()
        return "users/$userId/audio_notes/$documentId$normalizedExtension"
    }
}
