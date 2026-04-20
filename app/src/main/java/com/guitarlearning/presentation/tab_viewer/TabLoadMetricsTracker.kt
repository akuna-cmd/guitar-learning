package com.guitarlearning.presentation.tab_viewer

import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class TabLoadMeasurement(
    val tabId: String,
    val tabName: String,
    val tapStartedAtMs: Long,
    val loadRequestedAtMs: Long? = null,
    val scoreLoadedAtMs: Long? = null,
    val fullyVisibleAtMs: Long? = null,
    val source: String? = null
) {
    val tapToRequestMs: Long?
        get() = loadRequestedAtMs?.minus(tapStartedAtMs)

    val tapToScoreLoadedMs: Long?
        get() = scoreLoadedAtMs?.minus(tapStartedAtMs)

    val requestToScoreLoadedMs: Long?
        get() = if (loadRequestedAtMs != null && scoreLoadedAtMs != null) {
            scoreLoadedAtMs - loadRequestedAtMs
        } else {
            null
        }

    val tapToFullyVisibleMs: Long?
        get() = fullyVisibleAtMs?.minus(tapStartedAtMs)
}

data class TabLoadMetricsState(
    val activeMeasurement: TabLoadMeasurement? = null,
    val lastCompletedMeasurement: TabLoadMeasurement? = null,
    val history: List<TabLoadMeasurement> = emptyList()
)

object TabLoadMetricsTracker {
    private const val Tag = "TabLoadMetrics"

    private val _state = MutableStateFlow(TabLoadMetricsState())
    val state: StateFlow<TabLoadMetricsState> = _state.asStateFlow()

    fun start(tabId: String, tabName: String) {
        _state.value = _state.value.copy(
            activeMeasurement = TabLoadMeasurement(
                tabId = tabId,
                tabName = tabName,
                tapStartedAtMs = SystemClock.elapsedRealtime()
            )
        )
    }

    fun markLoadRequested(tabId: String, source: String) {
        updateActive(tabId) { current ->
            if (current.loadRequestedAtMs != null) {
                current
            } else {
                current.copy(
                    loadRequestedAtMs = SystemClock.elapsedRealtime(),
                    source = source
                )
            }
        }
    }

    fun markScoreLoaded(tabId: String, source: String?) {
        updateActive(tabId) { current ->
            if (current.scoreLoadedAtMs != null) {
                current
            } else {
                current.copy(
                    scoreLoadedAtMs = SystemClock.elapsedRealtime(),
                    source = source ?: current.source
                )
            }
        }
    }

    fun markFullyVisible(tabId: String) {
        val current = _state.value.activeMeasurement ?: return
        if (current.tabId != tabId || current.fullyVisibleAtMs != null) return

        val completed = current.copy(fullyVisibleAtMs = SystemClock.elapsedRealtime())
        Log.i(
            Tag,
            buildString {
                append("tab=\"${completed.tabName}\"")
                append(" source=${completed.source ?: "unknown"}")
                append(" tapToScoreLoadedMs=${completed.tapToScoreLoadedMs ?: -1}")
                append(" tapToFullyVisibleMs=${completed.tapToFullyVisibleMs ?: -1}")
                append(" requestToScoreLoadedMs=${completed.requestToScoreLoadedMs ?: -1}")
            }
        )
        _state.value = _state.value.copy(
            activeMeasurement = null,
            lastCompletedMeasurement = completed,
            history = (listOf(completed) + _state.value.history).take(10)
        )
    }

    internal fun resetForTests() {
        _state.value = TabLoadMetricsState()
    }

    private inline fun updateActive(tabId: String, transform: (TabLoadMeasurement) -> TabLoadMeasurement) {
        val current = _state.value.activeMeasurement ?: return
        if (current.tabId != tabId) return
        _state.value = _state.value.copy(activeMeasurement = transform(current))
    }
}
