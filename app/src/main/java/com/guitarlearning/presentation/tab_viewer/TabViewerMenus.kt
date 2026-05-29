package com.guitarlearning.presentation.tab_viewer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.guitarlearning.R
import com.guitarlearning.domain.settings.TabDisplayMode
import com.guitarlearning.presentation.ui.HoldableIconButton

@Composable
internal fun TabDisplayModeMenu(
    currentMode: TabDisplayMode,
    onModeChange: (TabDisplayMode) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .clickable { expanded = true }
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.QueueMusic,
                contentDescription = stringResource(R.string.tab_display_mode_toggle),
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = when (currentMode) {
                    TabDisplayMode.TAB_AND_NOTES -> stringResource(R.string.tab_display_mode_tab_and_notes)
                    TabDisplayMode.NOTES_ONLY -> stringResource(R.string.tab_display_mode_notes_only)
                    TabDisplayMode.TAB_ONLY -> stringResource(R.string.tab_display_mode_tab_only)
                },
                style = MaterialTheme.typography.labelLarge
            )
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.tab_display_mode_tab_and_notes)) },
                onClick = { expanded = false; onModeChange(TabDisplayMode.TAB_AND_NOTES) }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.tab_display_mode_notes_only)) },
                onClick = { expanded = false; onModeChange(TabDisplayMode.NOTES_ONLY) }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.tab_display_mode_tab_only)) },
                onClick = { expanded = false; onModeChange(TabDisplayMode.TAB_ONLY) }
            )
        }
    }
}

@Composable
internal fun SpeedScaleMenu(
    icon: ImageVector,
    valueText: String,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    decrementContentDescription: String,
    incrementContentDescription: String
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable { expanded = true }
                .padding(horizontal = 6.dp, vertical = 4.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(16.dp))
            Text(text = valueText, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HoldableIconButton(
                    onClick = onDecrement,
                    contentDescription = decrementContentDescription,
                    icon = Icons.Default.Remove
                )
                Text(text = valueText, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                HoldableIconButton(
                    onClick = onIncrement,
                    contentDescription = incrementContentDescription,
                    icon = Icons.Default.Add
                )
            }
        }
    }
}
