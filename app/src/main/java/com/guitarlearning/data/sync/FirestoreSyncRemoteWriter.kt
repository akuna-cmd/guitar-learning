package com.guitarlearning.data.sync

import android.util.Log
import com.guitarlearning.core.preferences.AppSettingsSnapshot
import com.guitarlearning.domain.model.AudioNote
import com.guitarlearning.domain.model.Goal
import com.guitarlearning.domain.model.Session
import com.guitarlearning.domain.model.TabItem
import com.guitarlearning.domain.model.TabPlaybackProgress
import com.guitarlearning.domain.model.TextNote
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

internal data class SyncContext(
    val previousOwnerUid: String?,
    val preferRemoteState: Boolean,
    val isAccountSwitch: Boolean,
    val pendingDeletedUserTabIds: Set<String>
)

internal data class RemoteSyncState(
    val settings: AppSettingsSnapshot?,
    val settingsDocument: DocumentReferenceSnapshotBundle,
    val tabsSnapshot: QuerySnapshot,
    val sessionsSnapshot: QuerySnapshot,
    val goalsSnapshot: QuerySnapshot,
    val progressSnapshot: QuerySnapshot,
    val textNotesSnapshot: QuerySnapshot,
    val audioNotesSnapshot: QuerySnapshot,
    val remoteStoragePathsByTabId: Map<String, String?>,
    val remoteAudioStoragePathsById: Map<String, String?>,
    val tabsImport: RemoteTabsImportResult,
    val sessionsImport: RemoteImportResult<Session>,
    val textNotesImport: RemoteImportResult<TextNote>,
    val audioNotesImport: RemoteImportResult<AudioNote>,
    val tabs: List<TabItem>,
    val sessions: List<Session>,
    val goals: List<Goal>,
    val progress: List<TabPlaybackProgress>,
    val textNotes: List<TextNote>,
    val audioNotes: List<AudioNote>,
    val usedSessionFallback: Boolean,
    val usedTextNoteFallback: Boolean,
    val usedAudioNoteFallback: Boolean,
    val remoteTabIds: Set<String>,
    val remoteUserTabIds: Set<String>,
    val remoteAudioNoteIds: Set<String>
)

internal data class DocumentReferenceSnapshotBundle(
    val data: com.google.firebase.firestore.DocumentSnapshot
)

internal data class LocalSyncState(
    val localSettings: AppSettingsSnapshot,
    val localTabs: List<TabItem>,
    val localGoals: List<Goal>,
    val localProgress: List<TabPlaybackProgress>,
    val localTextNotes: List<TextNote>,
    val localAudioNotes: List<AudioNote>,
    val mergedTextNotes: List<TextNote>,
    val mergedAudioNotes: List<AudioNote>,
    val useRemoteNotesAsSourceOfTruth: Boolean
)

internal data class UploadSyncState(
    val finalSettings: AppSettingsSnapshot,
    val finalTabs: List<TabItem>,
    val finalSessions: List<Session>,
    val finalGoals: List<Goal>,
    val finalProgress: List<TabPlaybackProgress>,
    val finalTextNotes: List<TextNote>,
    val finalAudioNotes: List<AudioNote>,
    val finalTabIds: Set<String>,
    val finalUserTabIds: Set<String>,
    val finalSessionIds: Set<String>,
    val finalTextNoteIds: Set<String>,
    val finalAudioNoteIds: Set<String>,
    val uploadedStoragePaths: Map<String, CloudTabFilePayload>,
    val uploadedAudioStoragePaths: Map<String, CloudAudioNoteFilePayload>
)

internal class FirestoreBatchWriter(
    private val firestore: FirebaseFirestore,
    private val maxBatchOps: Int
) {
    private var batch = firestore.batch()
    private var operationCount = 0

    suspend fun set(
        document: DocumentReference,
        data: Map<String, Any?>,
        merge: Boolean = true
    ) {
        if (merge) {
            batch.set(document, data, SetOptions.merge())
        } else {
            batch.set(document, data)
        }
        operationCount += 1
        commitIfNeeded()
    }

    suspend fun delete(document: DocumentReference) {
        batch.delete(document)
        operationCount += 1
        commitIfNeeded()
    }

    suspend fun deleteStaleDocuments(
        parent: CollectionReference,
        remoteIds: Set<String>,
        localIds: Set<String>
    ) {
        (remoteIds - localIds).forEach { staleId ->
            delete(parent.document(staleId))
        }
    }

    suspend fun flush() {
        if (operationCount == 0) return
        batch.commit().await()
        batch = firestore.batch()
        operationCount = 0
    }

    private suspend fun commitIfNeeded() {
        if (operationCount < maxBatchOps) return
        flush()
    }
}

internal class RemoteSyncWriter(
    private val firestore: FirebaseFirestore,
    private val mapper: FirestoreSyncMapper,
    private val fileStore: FirestoreSyncFileStore,
    private val logTag: String,
    private val maxBatchOps: Int
) {
    suspend fun writeRemoteState(
        userRef: DocumentReference,
        userUid: String,
        remoteState: RemoteSyncState,
        uploadState: UploadSyncState
    ) {
        val batchWriter = FirestoreBatchWriter(firestore, maxBatchOps)
        batchWriter.set(
            userRef.collection("settings").document("app"),
            mapper.settingsToFirestoreMap(uploadState.finalSettings) + mapOf(
                "sessionBackups" to FieldValue.delete(),
                "textNoteBackups" to FieldValue.delete(),
                "audioNoteBackups" to FieldValue.delete()
            )
        )

        writeTabs(userRef, userUid, remoteState, uploadState, batchWriter)
        writeSessions(userRef, remoteState, uploadState, batchWriter)
        writeGoals(userRef, remoteState, uploadState, batchWriter)
        writeProgress(userRef, remoteState, uploadState, batchWriter)
        writeTextNotes(userRef, remoteState, uploadState, batchWriter)
        writeAudioNotes(userRef, userUid, remoteState, uploadState, batchWriter)

        logWriteState(userUid, uploadState)
        batchWriter.flush()
    }

    private suspend fun writeTabs(
        userRef: DocumentReference,
        userUid: String,
        remoteState: RemoteSyncState,
        uploadState: UploadSyncState,
        batchWriter: FirestoreBatchWriter
    ) {
        uploadState.finalTabs.forEach { tab ->
            batchWriter.set(
                userRef.collection("tabs").document(tab.id),
                mapper.tabToFirestoreMap(tab, uploadState.uploadedStoragePaths[tab.id])
            )
        }
        fileStore.deleteStaleUserTabFiles(userUid, remoteState.remoteUserTabIds, uploadState.finalUserTabIds)
        batchWriter.deleteStaleDocuments(userRef.collection("tabs"), remoteState.remoteTabIds, uploadState.finalTabIds)
    }

    private suspend fun writeSessions(
        userRef: DocumentReference,
        remoteState: RemoteSyncState,
        uploadState: UploadSyncState,
        batchWriter: FirestoreBatchWriter
    ) {
        uploadState.finalSessions.forEach { session ->
            batchWriter.set(
                userRef.collection("sessions").document(mapper.sessionDocumentId(session)),
                mapper.sessionToFirestoreMap(session)
            )
        }
        batchWriter.deleteStaleDocuments(
            userRef.collection("sessions"),
            remoteState.sessionsSnapshot.documents.map { it.id }.toSet(),
            uploadState.finalSessionIds
        )
    }

    private suspend fun writeGoals(
        userRef: DocumentReference,
        remoteState: RemoteSyncState,
        uploadState: UploadSyncState,
        batchWriter: FirestoreBatchWriter
    ) {
        uploadState.finalGoals.forEach { goal ->
            batchWriter.set(
                userRef.collection("goals").document(goal.syncId),
                mapper.goalToFirestoreMap(goal)
            )
        }
        batchWriter.deleteStaleDocuments(
            userRef.collection("goals"),
            remoteState.goalsSnapshot.documents.map { it.id }.toSet(),
            uploadState.finalGoals.map { it.syncId }.toSet()
        )
    }

    private suspend fun writeProgress(
        userRef: DocumentReference,
        remoteState: RemoteSyncState,
        uploadState: UploadSyncState,
        batchWriter: FirestoreBatchWriter
    ) {
        uploadState.finalProgress.forEach { progress ->
            batchWriter.set(
                userRef.collection("progress").document(progress.tabId),
                mapper.progressToFirestoreMap(progress)
            )
        }
        batchWriter.deleteStaleDocuments(
            userRef.collection("progress"),
            remoteState.progressSnapshot.documents.map { it.id }.toSet(),
            uploadState.finalProgress.map { it.tabId }.toSet()
        )
    }

    private suspend fun writeTextNotes(
        userRef: DocumentReference,
        remoteState: RemoteSyncState,
        uploadState: UploadSyncState,
        batchWriter: FirestoreBatchWriter
    ) {
        uploadState.finalTextNotes.forEach { textNote ->
            val documentId = mapper.textNoteDocumentId(textNote)
            batchWriter.set(
                userRef.collection("text_notes").document(documentId),
                mapper.textNoteToFirestoreMap(textNote)
            )
        }
        batchWriter.deleteStaleDocuments(
            userRef.collection("text_notes"),
            remoteState.textNotesSnapshot.documents.map { it.id }.toSet(),
            uploadState.finalTextNoteIds
        )
    }

    private suspend fun writeAudioNotes(
        userRef: DocumentReference,
        userUid: String,
        remoteState: RemoteSyncState,
        uploadState: UploadSyncState,
        batchWriter: FirestoreBatchWriter
    ) {
        uploadState.finalAudioNotes.forEach { audioNote ->
            val documentId = mapper.audioNoteDocumentId(audioNote)
            batchWriter.set(
                userRef.collection("audio_notes").document(documentId),
                mapper.audioNoteToFirestoreMap(audioNote, uploadState.uploadedAudioStoragePaths[documentId])
            )
        }
        fileStore.deleteStaleAudioNoteFiles(
            userUid,
            remoteState.remoteAudioNoteIds,
            uploadState.finalAudioNoteIds,
            remoteState.remoteAudioStoragePathsById
        )
        batchWriter.deleteStaleDocuments(
            userRef.collection("audio_notes"),
            remoteState.remoteAudioNoteIds,
            uploadState.finalAudioNoteIds
        )
    }

    private fun logWriteState(userUid: String, uploadState: UploadSyncState) {
        Log.d(
            logTag,
            "syncData:write uid=$userUid finalTabs=${uploadState.finalTabs.size} finalSessions=${uploadState.finalSessions.size} finalGoals=${uploadState.finalGoals.size} " +
                "finalProgress=${uploadState.finalProgress.size} finalTextNotes=${uploadState.finalTextNotes.size} finalAudioNotes=${uploadState.finalAudioNotes.size}"
        )
    }
}
