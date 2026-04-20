package com.guitarlearning.presentation.tab_viewer

import java.time.Duration
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowSystemClock

@RunWith(RobolectricTestRunner::class)
class TabLoadMetricsTrackerTest {

    @After
    fun tearDown() {
        TabLoadMetricsTracker.resetForTests()
    }

    @Test
    fun recordsEndToEndMetricsAcrossLoadingStages() {
        TabLoadMetricsTracker.start("tab-1", "Canon Rock")

        ShadowSystemClock.advanceBy(Duration.ofMillis(120))
        TabLoadMetricsTracker.markLoadRequested("tab-1", "base64")

        ShadowSystemClock.advanceBy(Duration.ofMillis(340))
        TabLoadMetricsTracker.markScoreLoaded("tab-1", "base64")

        ShadowSystemClock.advanceBy(Duration.ofMillis(180))
        TabLoadMetricsTracker.markFullyVisible("tab-1")

        val completed = TabLoadMetricsTracker.state.value.lastCompletedMeasurement
        assertEquals(120L, completed?.tapToRequestMs)
        assertEquals(460L, completed?.tapToScoreLoadedMs)
        assertEquals(340L, completed?.requestToScoreLoadedMs)
        assertEquals(640L, completed?.tapToFullyVisibleMs)
        assertEquals("base64", completed?.source)
        assertNull(TabLoadMetricsTracker.state.value.activeMeasurement)
    }

    @Test
    fun ignoresSignalsForAnotherTab() {
        TabLoadMetricsTracker.start("tab-1", "Song A")

        ShadowSystemClock.advanceBy(Duration.ofMillis(50))
        TabLoadMetricsTracker.markLoadRequested("tab-2", "asset-url")
        TabLoadMetricsTracker.markScoreLoaded("tab-2", "asset-url")
        TabLoadMetricsTracker.markFullyVisible("tab-2")

        val active = TabLoadMetricsTracker.state.value.activeMeasurement
        assertEquals("tab-1", active?.tabId)
        assertNull(active?.loadRequestedAtMs)
        assertNull(TabLoadMetricsTracker.state.value.lastCompletedMeasurement)
    }
}
