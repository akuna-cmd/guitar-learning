package com.guitarlearning.presentation.tab_viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.guitarlearning.presentation.ui.theme.appBlockBorder

@Composable
internal fun RoundControlButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
    backgroundColor: Color,
    iconTint: Color
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .border(appBlockBorder(), CircleShape)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconTint,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
internal fun SheetSection(
    modifier: Modifier = Modifier,
    contentPadding: androidx.compose.ui.unit.Dp = 12.dp,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = containerColor,
        border = appBlockBorder()
    ) {
        Box(modifier = Modifier.padding(contentPadding)) {
            content()
        }
    }
}

@Composable
internal fun LearningSheetItem(
    onClick: () -> Unit,
    leadingIcon: ImageVector,
    headline: String
) {
    SheetSection(
        contentPadding = 0.dp,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        ListItem(
            headlineContent = { Text(headline) },
            leadingContent = { Icon(leadingIcon, contentDescription = null) },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() },
            colors = androidx.compose.material3.ListItemDefaults.colors(
                containerColor = Color.Transparent
            )
        )
    }
}

@Composable
internal fun TrackOptionRow(
    track: TabTrackOption,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer
                else Color.Transparent
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.MusicNote,
            contentDescription = null,
            tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = track.name,
            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp)
        )
    }
}
