package com.guitarlearning.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.guitarlearning.domain.model.TabPlaybackProgress
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class TabPlaybackProgressRepositoryImplTest {
    private lateinit var repository: TabPlaybackProgressRepositoryImpl
    private lateinit var scope: CoroutineScope
    private lateinit var storeFile: File

    @Before
    fun setUp() {
        storeFile = Files.createTempFile("tab-progress", ".preferences_pb").toFile()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val dataStore = PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { storeFile }
        )
        repository = TabPlaybackProgressRepositoryImpl(dataStore)
    }

    @After
    fun tearDown() {
        scope.cancel()
        storeFile.delete()
    }

    @Test
    fun upsertAndGetByTabId_preservesPlaybackRestoreState() = runTest {
        val progress = TabPlaybackProgress(
            tabId = "tab-42",
            tabName = "River Flows In You",
            lastTick = 1280L,
            lastBarIndex = 24,
            totalBars = 48,
            updatedAt = 5_000L
        )

        repository.upsert(progress)

        assertEquals(progress, repository.getByTabId("tab-42"))
    }

    @Test
    fun replaceAll_overwritesOldSnapshotAndClearsRemovedTabs() = runTest {
        val oldProgress = TabPlaybackProgress("old-tab", "Old", 120L, 3, 12, 1_000L)
        val newProgress = TabPlaybackProgress("new-tab", "New", 960L, 12, 32, 2_000L)

        repository.upsert(oldProgress)
        repository.replaceAll(listOf(newProgress))

        val stored = repository.observeAll().first().associateBy { it.tabId }
        assertEquals(setOf("new-tab"), stored.keys)
        assertEquals(newProgress, stored["new-tab"])
        assertNull(repository.getByTabId("old-tab"))
    }
}
