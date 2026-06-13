package com.guitarlearning.presentation.tab_viewer

import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.guitarlearning.R
import org.json.JSONArray
import org.json.JSONTokener

@Composable
internal fun TabViewerLoadingScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
internal fun TabViewerViewport(
    modifier: Modifier,
    webView: WebView,
    isDark: Boolean,
    showLoadingOverlay: Boolean,
    showScoreContent: Boolean,
    restorePending: Boolean,
    isReusedSession: Boolean,
    isPlaying: Boolean,
    controlsVisible: Boolean,
    playbackBlocked: Boolean,
    showAnalysisOverlay: Boolean,
    onPlayPause: () -> Unit,
    onOpenDisplaySheet: () -> Unit,
    onOpenLearningSheet: () -> Unit,
    onWebYChanged: (Int) -> Unit,
    onControlsYChanged: (Int) -> Unit
) {
    Box(modifier = modifier) {
        val shouldShowOverlay = showLoadingOverlay || (restorePending && !isReusedSession)

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (showScoreContent) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    AndroidView(
                        factory = {
                            (webView.parent as? ViewGroup)?.removeView(webView)
                            webView.animate().cancel()
                            webView.alpha = if (shouldShowOverlay) 0f else 1f
                            webView
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .onGloballyPositioned { coords ->
                                onWebYChanged(coords.boundsInRoot().top.toInt())
                            },
                        update = { view ->
                            view.setBackgroundColor(
                                if (isDark) android.graphics.Color.parseColor(TAB_VIEWER_DARK_BACKGROUND) else android.graphics.Color.WHITE
                            )
                            val targetVisible = !shouldShowOverlay
                            if (targetVisible) {
                                if (view.alpha != 1f) {
                                    view.animate().cancel()
                                    view.animate().alpha(1f).setDuration(140L).start()
                                }
                            } else {
                                view.animate().cancel()
                                view.alpha = 0f
                            }
                        }
                    )

                    if (showAnalysisOverlay && !shouldShowOverlay) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.padding(horizontal = 24.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp),
                                    strokeWidth = 2.5.dp
                                )
                                Text(
                                    text = stringResource(R.string.practice_analysis_loading_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = stringResource(R.string.practice_analysis_loading_body),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(0.dp)
                        .onGloballyPositioned { coords ->
                            onWebYChanged(coords.boundsInRoot().top.toInt())
                        }
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                if (controlsVisible) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { coords ->
                                onControlsYChanged(coords.boundsInRoot().top.toInt())
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        RoundControlButton(
                            onClick = onPlayPause,
                            icon = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = stringResource(if (isPlaying) R.string.pause else R.string.play),
                            backgroundColor = MaterialTheme.colorScheme.primaryContainer,
                            iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
                            enabled = !playbackBlocked
                        )

                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            RoundControlButton(
                                onClick = onOpenDisplaySheet,
                                icon = Icons.Filled.Visibility,
                                contentDescription = stringResource(R.string.display_controls),
                                backgroundColor = MaterialTheme.colorScheme.secondaryContainer,
                                iconTint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            RoundControlButton(
                                onClick = onOpenLearningSheet,
                                icon = Icons.Filled.School,
                                contentDescription = stringResource(R.string.learning_controls),
                                backgroundColor = MaterialTheme.colorScheme.tertiaryContainer,
                                iconTint = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

internal fun parseTrackOptionsFromJs(raw: String?): List<TabTrackOption> {
    if (raw.isNullOrBlank() || raw == "null") return emptyList()
    return runCatching {
        val decoded = JSONTokener(raw).nextValue()
        val payload = decoded as? String ?: raw
        val array = JSONArray(payload)
        buildList {
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                val index = item.optInt("index", i)
                val name = item.optString("name").ifBlank { "Track ${i + 1}" }
                add(TabTrackOption(index = index, name = name))
            }
        }
    }.getOrDefault(emptyList())
}
