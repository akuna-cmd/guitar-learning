package com.guitarlearning.presentation.tab_viewer

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.guitarlearning.R
import com.guitarlearning.domain.settings.TabDisplayMode
import com.guitarlearning.presentation.ui.HoldableIconButton
import com.guitarlearning.presentation.ui.formatScale
import com.guitarlearning.presentation.ui.formatSpeed
import com.guitarlearning.presentation.ui.stepScale
import com.guitarlearning.presentation.ui.stepSpeed

private fun stepBpm(value: Int, delta: Int): Int {
    return (value + delta).coerceIn(40, 240)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TabViewerSheets(
    showDisplaySheet: Boolean,
    onDismissDisplaySheet: () -> Unit,
    displaySheetState: androidx.compose.material3.SheetState,
    currentSpeed: Float,
    currentScale: Float,
    tabDisplayMode: TabDisplayMode,
    onSpeedChange: (Float) -> Unit,
    onScaleChange: (Float) -> Unit,
    onTabDisplayModeChange: (TabDisplayMode) -> Unit,
    silentMode: Boolean,
    onSilentModeChange: (Boolean) -> Unit,
    showLearningSheet: Boolean,
    onDismissLearningSheet: () -> Unit,
    learningSheetState: androidx.compose.material3.SheetState,
    onOpenAiAssistant: () -> Unit,
    onOpenNotes: () -> Unit,
    onOpenLoop: () -> Unit,
    metronomeEnabled: Boolean,
    metronomeBpm: Int,
    onMetronomeEnabledChange: (Boolean) -> Unit,
    onMetronomeBpmChange: (Int) -> Unit,
    trackOptions: List<TabTrackOption>,
    selectedTrackIndex: Int,
    transposeSemitones: Int,
    onTrackSelected: (Int) -> Unit,
    onTransposeChange: (Int) -> Unit
) {
    if (showDisplaySheet) {
        ModalBottomSheet(
            onDismissRequest = onDismissDisplaySheet,
            sheetState = displaySheetState
        ) {
            DisplayControlsSheet(
                currentSpeed = currentSpeed,
                currentScale = currentScale,
                tabDisplayMode = tabDisplayMode,
                onSpeedChange = onSpeedChange,
                onScaleChange = onScaleChange,
                onTabDisplayModeChange = onTabDisplayModeChange,
                silentMode = silentMode,
                onSilentModeChange = onSilentModeChange
            )
        }
    }

    if (showLearningSheet) {
        ModalBottomSheet(
            onDismissRequest = onDismissLearningSheet,
            sheetState = learningSheetState
        ) {
            LearningControlsSheet(
                onOpenAiAssistant = {
                    onDismissLearningSheet()
                    onOpenAiAssistant()
                },
                onOpenNotes = {
                    onDismissLearningSheet()
                    onOpenNotes()
                },
                onOpenLoop = {
                    onDismissLearningSheet()
                    onOpenLoop()
                },
                metronomeEnabled = metronomeEnabled,
                metronomeBpm = metronomeBpm,
                onMetronomeEnabledChange = onMetronomeEnabledChange,
                onMetronomeBpmChange = onMetronomeBpmChange,
                trackOptions = trackOptions,
                selectedTrackIndex = selectedTrackIndex,
                transposeSemitones = transposeSemitones,
                onTrackSelected = onTrackSelected,
                onTransposeChange = onTransposeChange
            )
        }
    }
}

@Composable
internal fun DisplayControlsSheet(
    currentSpeed: Float,
    currentScale: Float,
    tabDisplayMode: TabDisplayMode,
    onSpeedChange: (Float) -> Unit,
    onScaleChange: (Float) -> Unit,
    onTabDisplayModeChange: (TabDisplayMode) -> Unit,
    silentMode: Boolean,
    onSilentModeChange: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.display_controls),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        SheetSection {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Speed, contentDescription = null)
                Text(
                    text = stringResource(R.string.speed_value_format, formatSpeed(currentSpeed)),
                    modifier = Modifier.weight(1f)
                )
                HoldableIconButton(
                    onClick = { onSpeedChange(stepSpeed(currentSpeed, -0.1f)) },
                    contentDescription = stringResource(R.string.speed_decrease),
                    icon = Icons.Default.Remove
                )
                HoldableIconButton(
                    onClick = { onSpeedChange(stepSpeed(currentSpeed, 0.1f)) },
                    contentDescription = stringResource(R.string.speed_increase),
                    icon = Icons.Default.Add
                )
            }
        }
        SheetSection {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.ZoomIn, contentDescription = null)
                Text(
                    text = stringResource(R.string.scale_value_format, formatScale(currentScale)),
                    modifier = Modifier.weight(1f)
                )
                HoldableIconButton(
                    onClick = { onScaleChange(stepScale(currentScale, -0.1f)) },
                    contentDescription = stringResource(R.string.scale_decrease),
                    icon = Icons.Default.Remove
                )
                HoldableIconButton(
                    onClick = { onScaleChange(stepScale(currentScale, 0.1f)) },
                    contentDescription = stringResource(R.string.scale_increase),
                    icon = Icons.Default.Add
                )
            }
        }
        Text(
            text = stringResource(R.string.display_mode),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        SheetSection {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(
                    onClick = { onTabDisplayModeChange(TabDisplayMode.TAB_AND_NOTES) },
                    label = { Text(stringResource(R.string.tab_display_mode_tab_and_notes)) },
                    leadingIcon = { Icon(Icons.Default.LibraryMusic, contentDescription = null) }
                )
                AssistChip(
                    onClick = { onTabDisplayModeChange(TabDisplayMode.NOTES_ONLY) },
                    label = { Text(stringResource(R.string.tab_display_mode_notes_only)) },
                    leadingIcon = { Icon(Icons.Default.MusicNote, contentDescription = null) }
                )
                AssistChip(
                    onClick = { onTabDisplayModeChange(TabDisplayMode.TAB_ONLY) },
                    label = { Text(stringResource(R.string.tab_display_mode_tab_only)) },
                    leadingIcon = { Icon(Icons.Default.QueueMusic, contentDescription = null) }
                )
            }
        }
        SheetSection(
            contentPadding = 0.dp,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.silent_mode)) },
                supportingContent = { Text(stringResource(R.string.silent_mode_desc)) },
                trailingContent = {
                    Switch(
                        checked = silentMode,
                        onCheckedChange = onSilentModeChange
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.ListItemDefaults.colors(
                    containerColor = Color.Transparent
                )
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
internal fun LearningControlsSheet(
    onOpenAiAssistant: () -> Unit,
    onOpenNotes: () -> Unit,
    onOpenLoop: () -> Unit,
    metronomeEnabled: Boolean,
    metronomeBpm: Int,
    onMetronomeEnabledChange: (Boolean) -> Unit,
    onMetronomeBpmChange: (Int) -> Unit,
    trackOptions: List<TabTrackOption>,
    selectedTrackIndex: Int,
    transposeSemitones: Int,
    onTrackSelected: (Int) -> Unit,
    onTransposeChange: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.learning_controls),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        LearningSheetItem(
            onClick = onOpenNotes,
            leadingIcon = Icons.Filled.NoteAdd,
            headline = stringResource(R.string.notes)
        )
        LearningSheetItem(
            onClick = onOpenLoop,
            leadingIcon = Icons.Filled.Repeat,
            headline = stringResource(R.string.loop_section)
        )
        SheetSection(
            contentPadding = 0.dp,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.track_selection)) },
                    supportingContent = {
                        Text(
                            text = trackOptions.firstOrNull { it.index == selectedTrackIndex }?.name
                                ?: stringResource(R.string.track_selection_empty)
                        )
                    },
                    leadingContent = { Icon(Icons.Filled.QueueMusic, contentDescription = null) },
                    colors = androidx.compose.material3.ListItemDefaults.colors(
                        containerColor = Color.Transparent
                    )
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val fallbackTrackName = stringResource(R.string.track_selection_empty)
                    val visibleTracks = trackOptions.ifEmpty {
                        listOf(TabTrackOption(index = selectedTrackIndex, name = fallbackTrackName))
                    }
                    visibleTracks.forEach { track ->
                        TrackOptionRow(
                            track = track,
                            selected = track.index == selectedTrackIndex,
                            onClick = { onTrackSelected(track.index) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
        SheetSection(
            contentPadding = 0.dp,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.transpose_track)) },
                supportingContent = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.transpose_value, transposeSemitones),
                            modifier = Modifier.weight(1f)
                        )
                        HoldableIconButton(
                            onClick = { onTransposeChange((transposeSemitones - 1).coerceAtLeast(-36)) },
                            contentDescription = stringResource(R.string.transpose_decrease),
                            icon = Icons.Default.Remove
                        )
                        HoldableIconButton(
                            onClick = { onTransposeChange((transposeSemitones + 1).coerceAtMost(36)) },
                            contentDescription = stringResource(R.string.transpose_increase),
                            icon = Icons.Default.Add
                        )
                    }
                },
                leadingContent = { Icon(Icons.Filled.Tune, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.ListItemDefaults.colors(
                    containerColor = Color.Transparent
                )
            )
        }
        SheetSection(
            contentPadding = 0.dp,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.metronome)) },
                supportingContent = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.metronome_bpm, metronomeBpm),
                            modifier = Modifier.weight(1f)
                        )
                        HoldableIconButton(
                            onClick = { onMetronomeBpmChange(stepBpm(metronomeBpm, -5)) },
                            contentDescription = stringResource(R.string.metronome_decrease),
                            icon = Icons.Default.Remove
                        )
                        HoldableIconButton(
                            onClick = { onMetronomeBpmChange(stepBpm(metronomeBpm, 5)) },
                            contentDescription = stringResource(R.string.metronome_increase),
                            icon = Icons.Default.Add
                        )
                    }
                },
                leadingContent = { Icon(Icons.Filled.MusicNote, contentDescription = null) },
                trailingContent = {
                    Switch(
                        checked = metronomeEnabled,
                        onCheckedChange = onMetronomeEnabledChange
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.ListItemDefaults.colors(
                    containerColor = Color.Transparent
                )
            )
        }
        LearningSheetItem(
            onClick = onOpenAiAssistant,
            leadingIcon = Icons.Filled.AutoAwesome,
            headline = stringResource(R.string.ai_assistant)
        )
        Spacer(modifier = Modifier.height(12.dp))
    }
}


