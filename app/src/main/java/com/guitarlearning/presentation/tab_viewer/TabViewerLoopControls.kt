package com.guitarlearning.presentation.tab_viewer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.guitarlearning.R
import com.guitarlearning.presentation.ui.HoldableIconButton
import androidx.compose.ui.res.stringResource
import kotlin.math.roundToInt

private fun formatSpeedPercent(speed: Float): String = "${(speed * 100f).roundToInt()}%"

private fun stepLoopSpeedPercent(value: Float, deltaPercent: Int): Float {
    val stepped = value + (deltaPercent / 100f)
    return ((stepped * 100f).roundToInt() / 100f).coerceIn(0.05f, 2.5f)
}

private fun stepRepeatCount(value: Int, delta: Int): Int = (value + delta).coerceIn(1, 32)

@Composable
internal fun LoopConfigurator(
    totalMeasures: Int,
    startMeasure: Int,
    endMeasure: Int,
    isLoopEnabled: Boolean,
    onStartChange: (Int) -> Unit,
    onEndChange: (Int) -> Unit,
    onToggleLoop: (Boolean) -> Unit,
    isLoopAccelerationEnabled: Boolean,
    loopAccelerationStartSpeed: Float,
    loopAccelerationEndSpeed: Float,
    loopAccelerationStep: Float,
    loopAccelerationRepeats: Int,
    loopAccelerationCompletedRepeats: Int,
    onToggleLoopAcceleration: (Boolean) -> Unit,
    onLoopAccelerationStartSpeedChange: (Float) -> Unit,
    onLoopAccelerationEndSpeedChange: (Float) -> Unit,
    onLoopAccelerationStepChange: (Float) -> Unit,
    onLoopAccelerationRepeatsChange: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.loop_section),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        SheetSection(
            contentPadding = 0.dp,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.Repeat, contentDescription = null)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.loop_section))
                        Text(
                            text = stringResource(R.string.loop_enable),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = isLoopEnabled, onCheckedChange = onToggleLoop)
                }
                if (isLoopEnabled) {
                    LoopValueRow(
                        label = stringResource(R.string.loop_start_measure),
                        value = startMeasure.toString(),
                        onDecrement = { onStartChange((startMeasure - 1).coerceAtLeast(1).coerceAtMost(endMeasure)) },
                        onIncrement = { onStartChange((startMeasure + 1).coerceAtMost(endMeasure)) }
                    )
                    LoopValueRow(
                        label = stringResource(R.string.loop_end_measure),
                        value = endMeasure.toString(),
                        onDecrement = { onEndChange((endMeasure - 1).coerceAtLeast(startMeasure)) },
                        onIncrement = { onEndChange((endMeasure + 1).coerceAtMost(totalMeasures.coerceAtLeast(startMeasure))) }
                    )
                }
            }
        }

        if (isLoopEnabled) {
            SheetSection(
                contentPadding = 0.dp,
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.Speed, contentDescription = null)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.loop_gradual_speedup_title))
                            Text(
                                text = stringResource(R.string.loop_gradual_speedup_description),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isLoopAccelerationEnabled,
                            onCheckedChange = onToggleLoopAcceleration
                        )
                    }
                    if (isLoopAccelerationEnabled) {
                        LoopValueRow(
                            label = stringResource(R.string.loop_gradual_start_tempo),
                            value = formatSpeedPercent(loopAccelerationStartSpeed),
                            onDecrement = { onLoopAccelerationStartSpeedChange(stepLoopSpeedPercent(loopAccelerationStartSpeed, -5)) },
                            onIncrement = { onLoopAccelerationStartSpeedChange(stepLoopSpeedPercent(loopAccelerationStartSpeed, 5)) }
                        )
                        LoopValueRow(
                            label = stringResource(R.string.loop_gradual_end_tempo),
                            value = formatSpeedPercent(loopAccelerationEndSpeed),
                            onDecrement = { onLoopAccelerationEndSpeedChange(stepLoopSpeedPercent(loopAccelerationEndSpeed, -5)) },
                            onIncrement = { onLoopAccelerationEndSpeedChange(stepLoopSpeedPercent(loopAccelerationEndSpeed, 5)) }
                        )
                        LoopValueRow(
                            label = stringResource(R.string.loop_gradual_step),
                            value = formatSpeedPercent(loopAccelerationStep),
                            onDecrement = { onLoopAccelerationStepChange(stepLoopSpeedPercent(loopAccelerationStep, -5)) },
                            onIncrement = { onLoopAccelerationStepChange(stepLoopSpeedPercent(loopAccelerationStep, 5)) }
                        )
                        LoopValueRow(
                            label = stringResource(R.string.loop_gradual_repeats),
                            value = stringResource(R.string.loop_gradual_repeats_value, loopAccelerationRepeats),
                            supporting = stringResource(
                                R.string.loop_gradual_progress,
                                loopAccelerationCompletedRepeats,
                                loopAccelerationRepeats
                            ),
                            onDecrement = { onLoopAccelerationRepeatsChange(stepRepeatCount(loopAccelerationRepeats, -1)) },
                            onIncrement = { onLoopAccelerationRepeatsChange(stepRepeatCount(loopAccelerationRepeats, 1)) }
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun LoopValueRow(
    label: String,
    value: String,
    supporting: String? = null,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label)
            if (!supporting.isNullOrBlank()) {
                Text(
                    text = supporting,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        HoldableIconButton(
            onClick = onDecrement,
            contentDescription = stringResource(R.string.speed_decrease),
            icon = Icons.Default.Remove
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.width(8.dp))
        HoldableIconButton(
            onClick = onIncrement,
            contentDescription = stringResource(R.string.speed_increase),
            icon = Icons.Default.Add
        )
    }
}
