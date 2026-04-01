package com.example.thetest1.presentation.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.thetest1.R
import com.example.thetest1.di.ViewModelFactory
import com.example.thetest1.domain.model.Session
import com.example.thetest1.presentation.ui.theme.appBlockBorder
import com.example.thetest1.presentation.ui.formatDuration
import kotlinx.coroutines.flow.StateFlow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    sessions: List<Session>,
    onStartSession: () -> Unit,
    onContinueLesson: (String) -> Unit,
    isSessionActive: Boolean,
    totalSessionTime: Long,
    lessonsCompleted: Int,
    totalLessons: Int,
    userTabsCount: Int,
    viewModelFactory: ViewModelFactory,
    lastPlaybackProgressFlow: StateFlow<com.example.thetest1.domain.model.TabPlaybackProgress?>
) {
    val lastPlaybackProgress by lastPlaybackProgressFlow.collectAsStateWithLifecycle()
    val recentSessions = remember(sessions) { sessions.take(20) }
    val progress = lastPlaybackProgress
    val lastTabName = progress?.tabName
    val progressValue = if (progress != null && progress.totalBars > 0) {
        progress.lastBarIndex.toFloat() / progress.totalBars.toFloat()
    } else {
        0f
    }
    val progressPercent = (progressValue * 100).roundToInt().coerceIn(0, 100)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize(),
        contentPadding = PaddingValues(top = 32.dp, start = 16.dp, end = 16.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            ContinueLearningCard(
                lastTabName = lastTabName,
                progressPercent = progressPercent,
                progressValue = progressValue,
                onContinue = {
                    val progress = lastPlaybackProgressFlow.value
                    val tabId = progress?.tabId
                    if (tabId != null) {
                        onContinueLesson(tabId)
                    } else {
                        onStartSession()
                    }
                },
                isSessionActive = isSessionActive,
                isEnabled = lastPlaybackProgress != null
            )
        }
        item {
            PracticeHeatmap(viewModelFactory = viewModelFactory)
        }
        item {
            StatsRow(
                totalSessionTime = totalSessionTime,
                lessonsCompleted = lessonsCompleted,
                totalLessons = totalLessons
            )
        }
        item {
            MyTabsSummaryCard(userTabsCount = userTabsCount)
        }
        item {
            Button(
                onClick = onStartSession,
                enabled = !isSessionActive,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(id = R.string.start_practice))
            }
        }
        items(recentSessions, key = { it.id }) { session ->
            SessionItem(session)
        }
    }
}

@Composable
fun ContinueLearningCard(
    lastTabName: String?,
    progressPercent: Int,
    progressValue: Float,
    onContinue: () -> Unit,
    isSessionActive: Boolean,
    isEnabled: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(),
        colors = CardDefaults.elevatedCardColors(),
        shape = RoundedCornerShape(12.dp),
        border = appBlockBorder()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(id = R.string.continue_learning_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(id = R.string.continue_learning_last_song),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
            )
            Text(
                text = lastTabName ?: stringResource(id = R.string.continue_learning_no_song),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(id = R.string.continue_learning_progress_label),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = stringResource(id = R.string.progress_percent_format, progressPercent),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            LinearProgressIndicator(
                progress = { progressValue },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedButton(
                onClick = onContinue,
                enabled = !isSessionActive && isEnabled,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(id = R.string.continue_learning_button))
            }
        }
    }
}

@Composable
fun StatsRow(totalSessionTime: Long, lessonsCompleted: Int, totalLessons: Int) {
    val progressValue = if (totalLessons > 0) lessonsCompleted.toFloat() / totalLessons.toFloat() else 0f
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatTimeCard(
            totalSessionTime = totalSessionTime,
            modifier = Modifier.weight(1f)
        )
        LessonsProgressCard(
            lessonsCompleted = lessonsCompleted,
            totalLessons = totalLessons,
            progressValue = progressValue,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatTimeCard(totalSessionTime: Long, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.elevatedCardElevation(),
        colors = CardDefaults.elevatedCardColors(),
        shape = RoundedCornerShape(12.dp),
        border = appBlockBorder()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = formatDuration(totalSessionTime),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(id = R.string.total_session_time),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
            )
        }
    }
}

@Composable
private fun LessonsProgressCard(
    lessonsCompleted: Int,
    totalLessons: Int,
    progressValue: Float,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.elevatedCardElevation(),
        colors = CardDefaults.elevatedCardColors(),
        shape = RoundedCornerShape(12.dp),
        border = appBlockBorder()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { progressValue },
                    strokeWidth = 7.dp,
                    modifier = Modifier.size(86.dp)
                )
                Text(
                    text = stringResource(id = R.string.lessons_progress_format, lessonsCompleted, totalLessons),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(id = R.string.lessons_progress_title),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
            )
        }
    }
}

@Composable
fun MyTabsSummaryCard(userTabsCount: Int) {
    Card(
        elevation = CardDefaults.elevatedCardElevation(),
        colors = CardDefaults.elevatedCardColors(),
        shape = RoundedCornerShape(12.dp),
        border = appBlockBorder()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = userTabsCount.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(id = R.string.my_tabs),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@Composable
fun SessionItem(session: Session) {
    val dateFormat = SimpleDateFormat(
        stringResource(id = R.string.session_date_format),
        Locale.getDefault()
    )

    Card(
        modifier = Modifier
            .fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(),
        colors = CardDefaults.elevatedCardColors(),
        shape = RoundedCornerShape(12.dp),
        border = appBlockBorder()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = stringResource(id = R.string.start_time),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                Text(dateFormat.format(session.startTime))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Stop,
                    contentDescription = stringResource(id = R.string.end_time),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                Text(dateFormat.format(session.endTime))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Timer,
                    contentDescription = stringResource(id = R.string.duration),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                Text(formatDuration(session.duration))
            }
            if (session.practicedTabs.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    session.practicedTabs.forEach { tab ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = tab.tabName,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.Filled.Timer,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(text = formatDuration(tab.duration))
                        }
                    }
                }
            }
        }
    }
}

