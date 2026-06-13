package com.guitarlearning.presentation.tab_list

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.guitarlearning.R
import com.guitarlearning.domain.model.Difficulty
import com.guitarlearning.presentation.tab_viewer.TabLoadMetricsTracker
import com.guitarlearning.presentation.ui.theme.appBlockBorder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LessonsScreen(
    viewModel: TabListViewModel,
    uiState: TabListUiState,
    onTabClick: (String) -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Difficulty.entries.forEach { difficulty ->
                FilterChip(
                    selected = uiState.selectedDifficulty == difficulty,
                    onClick = { viewModel.selectDifficulty(difficulty) },
                    label = { Text(difficultyChipLabel(difficulty, uiState)) }
                )
            }
        }
        Column(modifier = Modifier.padding(16.dp)) {
            if (uiState.isTabsLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
                return@Column
            }
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(uiState.filteredTabs, key = { it.id }) { tab ->
                    val progress = if (uiState.areMetricsLoading) null else (uiState.progressByTabId[tab.id] ?: 0)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                TabLoadMetricsTracker.start(tab.id, tab.name)
                                onTabClick(tab.id)
                            },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.elevatedCardColors(),
                        elevation = CardDefaults.elevatedCardElevation(),
                        border = appBlockBorder()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${tab.lessonNumber}. ${tab.name}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    textDecoration = if (tab.isCompleted) {
                                        TextDecoration.LineThrough
                                    } else {
                                        TextDecoration.None
                                    }
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = tab.description,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                TabProgressLine(
                                    progressPercent = progress,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                IconButton(onClick = { viewModel.toggleCompleted(tab.id) }) {
                                    Icon(
                                        imageVector = if (tab.isCompleted) {
                                            Icons.Filled.CheckCircle
                                        } else {
                                            Icons.Outlined.RadioButtonUnchecked
                                        },
                                        contentDescription = if (tab.isCompleted) {
                                            stringResource(R.string.completed)
                                        } else {
                                            stringResource(R.string.mark_as_completed)
                                        },
                                        tint = if (tab.isCompleted) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

internal fun difficultyChipLabel(difficulty: Difficulty, uiState: TabListUiState): String {
    val stars = when (difficulty) {
        Difficulty.BEGINNER -> "★"
        Difficulty.INTERMEDIATE -> "★★"
        Difficulty.ADVANCED -> "★★★"
    }
    return "$stars (${uiState.completedLessons(difficulty)}/${uiState.totalLessons(difficulty)})"
}

@Composable
internal fun TabProgressBadge(
    progressPercent: Int?,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = stringResource(R.string.tab_progress_label),
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Text(
                text = progressPercent?.let { "$it%" } ?: "--",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}

@Composable
internal fun TabProgressLine(
    progressPercent: Int?,
    modifier: Modifier = Modifier
) {
    val value = ((progressPercent ?: 0).coerceIn(0, 100)) / 100f
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        LinearProgressIndicator(
            progress = { value },
            modifier = Modifier
                .weight(1f)
                .height(8.dp),
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            color = MaterialTheme.colorScheme.primary
        )
        TabProgressBadge(progressPercent = progressPercent)
    }
}

@Composable
internal fun EmptyTabsState(onAddFirstTab: () -> Unit) {
    val context = LocalContext.current
    val googleQuery = stringResource(R.string.empty_tabs_google_query)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 28.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.elevatedCardColors(),
            elevation = CardDefaults.elevatedCardElevation(),
            border = appBlockBorder()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LibraryMusic,
                    contentDescription = null,
                    modifier = Modifier.size(42.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(R.string.empty_tabs_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(R.string.empty_tabs_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    border = appBlockBorder()
                ) {
                    Text(
                        text = googleQuery,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedButton(
                    onClick = {
                        val url = "https://www.google.com/search?q=${Uri.encode(googleQuery)}"
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.search_tabs_in_google))
                }
                Button(onClick = onAddFirstTab) {
                    Text(stringResource(R.string.upload_first_gp))
                }
            }
        }
    }
}
