package com.guitarlearning.presentation.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.guitarlearning.R
import com.guitarlearning.presentation.ui.HoldableIconButton
import com.guitarlearning.presentation.ui.formatScale
import com.guitarlearning.presentation.ui.formatSpeed
import com.guitarlearning.presentation.ui.stepScale
import com.guitarlearning.presentation.ui.stepSpeed
import com.guitarlearning.presentation.ui.theme.appBlockBorder
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource

@Composable
internal fun CompactSpeedScale(
    speed: Float,
    scale: Float,
    onSpeedChange: (Float) -> Unit,
    onScaleChange: (Float) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = stringResource(R.string.speed_value_format, formatSpeed(speed)))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HoldableIconButton(
                    onClick = { onSpeedChange(stepSpeed(speed, -0.1f)) },
                    contentDescription = stringResource(R.string.speed_decrease),
                    icon = Icons.Default.Remove,
                    buttonSize = 36.dp,
                    iconSize = 20.dp
                )
                HoldableIconButton(
                    onClick = { onSpeedChange(stepSpeed(speed, 0.1f)) },
                    contentDescription = stringResource(R.string.speed_increase),
                    icon = Icons.Default.Add,
                    buttonSize = 36.dp,
                    iconSize = 20.dp
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ZoomIn, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = stringResource(R.string.scale_value_format, formatScale(scale)))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HoldableIconButton(
                    onClick = { onScaleChange(stepScale(scale, -0.1f)) },
                    contentDescription = stringResource(R.string.scale_decrease),
                    icon = Icons.Default.Remove,
                    buttonSize = 36.dp,
                    iconSize = 20.dp
                )
                HoldableIconButton(
                    onClick = { onScaleChange(stepScale(scale, 0.1f)) },
                    contentDescription = stringResource(R.string.scale_increase),
                    icon = Icons.Default.Add,
                    buttonSize = 36.dp,
                    iconSize = 20.dp
                )
            }
        }
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            border = appBlockBorder()
        ) {
            Column(
                modifier = Modifier.padding(vertical = 2.dp),
                content = content
            )
        }
    }
}

@Composable
fun SettingsOptionsRow(
    label: String,
    selected: Boolean,
    icon: ImageVector,
    showTopDivider: Boolean = true,
    onClick: () -> Unit
) {
    SettingsRadioRow(label, selected, icon, showTopDivider, onClick)
}

@Composable
fun SettingsIconOptionRow(
    label: String,
    selected: Boolean,
    icon: ImageVector,
    showTopDivider: Boolean = true,
    onClick: () -> Unit
) {
    SettingsRadioRow(label, selected, icon, showTopDivider, onClick)
}

@Composable
private fun SettingsRadioRow(
    label: String,
    selected: Boolean,
    icon: ImageVector,
    showTopDivider: Boolean,
    onClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (showTopDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            RadioButton(selected = selected, onClick = null)
        }
    }
}
