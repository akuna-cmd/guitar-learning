package com.guitarlearning.presentation.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.guitarlearning.R
import com.guitarlearning.domain.model.Session
import java.util.concurrent.TimeUnit

private enum class SessionDateFilter {
    ALL,
    TODAY,
    WEEK,
    MONTH
}

private enum class SessionDurationFilter {
    ALL,
    SHORT,
    MEDIUM,
    LONG
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionHistoryScreen(
    sessions: List<Session>,
    onBack: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var dateFilter by remember { mutableStateOf(SessionDateFilter.ALL) }
    var durationFilter by remember { mutableStateOf(SessionDurationFilter.ALL) }

    val now = remember { System.currentTimeMillis() }
    val filteredSessions = remember(sessions, query, dateFilter, durationFilter, now) {
        val normalizedQuery = query.trim().lowercase()
        sessions.filter { session ->
            val matchesQuery = normalizedQuery.isBlank() || session.practicedTabs.any {
                it.tabName.lowercase().contains(normalizedQuery)
            }
            val matchesDate = when (dateFilter) {
                SessionDateFilter.ALL -> true
                SessionDateFilter.TODAY -> session.startTime.time >= now - TimeUnit.DAYS.toMillis(1)
                SessionDateFilter.WEEK -> session.startTime.time >= now - TimeUnit.DAYS.toMillis(7)
                SessionDateFilter.MONTH -> session.startTime.time >= now - TimeUnit.DAYS.toMillis(30)
            }
            val durationMinutes = TimeUnit.MILLISECONDS.toMinutes(session.duration)
            val matchesDuration = when (durationFilter) {
                SessionDurationFilter.ALL -> true
                SessionDurationFilter.SHORT -> durationMinutes < 15
                SessionDurationFilter.MEDIUM -> durationMinutes in 15..30
                SessionDurationFilter.LONG -> durationMinutes > 30
            }
            matchesQuery && matchesDate && matchesDuration
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.session_history_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back_arrow))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                placeholder = { Text(stringResource(R.string.session_history_search_placeholder)) },
                shape = MaterialTheme.shapes.extraLarge
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SessionDateFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = dateFilter == filter,
                        onClick = { dateFilter = filter },
                        label = { Text(filter.label()) }
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SessionDurationFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = durationFilter == filter,
                        onClick = { durationFilter = filter },
                        label = { Text(filter.label()) }
                    )
                }
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (filteredSessions.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.session_history_empty),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                    }
                } else {
                    items(filteredSessions, key = { it.id }) { session ->
                        SessionItem(session)
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionDateFilter.label(): String {
    return when (this) {
        SessionDateFilter.ALL -> stringResource(R.string.filter_all)
        SessionDateFilter.TODAY -> stringResource(R.string.filter_today)
        SessionDateFilter.WEEK -> stringResource(R.string.filter_week)
        SessionDateFilter.MONTH -> stringResource(R.string.filter_month)
    }
}

@Composable
private fun SessionDurationFilter.label(): String {
    return when (this) {
        SessionDurationFilter.ALL -> stringResource(R.string.filter_all_duration)
        SessionDurationFilter.SHORT -> stringResource(R.string.filter_duration_short)
        SessionDurationFilter.MEDIUM -> stringResource(R.string.filter_duration_medium)
        SessionDurationFilter.LONG -> stringResource(R.string.filter_duration_long)
    }
}
