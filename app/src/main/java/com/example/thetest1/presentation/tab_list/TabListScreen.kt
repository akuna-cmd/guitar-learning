package com.example.thetest1.presentation.tab_list

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.thetest1.R
import com.example.thetest1.di.ViewModelFactory
import com.example.thetest1.domain.model.Difficulty
import com.example.thetest1.domain.model.TabItem
import com.example.thetest1.presentation.util.formatDuration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabListScreen(
    viewModelFactory: ViewModelFactory,
    onTabClick: (String) -> Unit
) {
    val viewModel: TabListViewModel = viewModel(factory = viewModelFactory)
    val uiState by viewModel.uiState.collectAsState()

    val pickFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri: Uri? ->
            uri?.let { viewModel.addUserTab(it) }
        }
    )

    Scaffold(
        floatingActionButton = {
            if (uiState.selectedTabIndex == 0) {
                FloatingActionButton(onClick = { pickFileLauncher.launch(arrayOf("application/octet-stream", "text/plain", "*/*")) }) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_tab))
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            PrimaryTabRow(selectedTabIndex = uiState.selectedTabIndex) {
                Tab(
                    selected = uiState.selectedTabIndex == 0,
                    onClick = { viewModel.selectTab(0) },
                    text = { Text(stringResource(R.string.my_tabs_tab)) },
                    icon = { Icon(Icons.Default.MusicNote, contentDescription = null) }
                )
                Tab(
                    selected = uiState.selectedTabIndex == 1,
                    onClick = { viewModel.selectTab(1) },
                    text = { Text(stringResource(R.string.lessons_tab)) },
                    icon = { Icon(Icons.Default.School, contentDescription = null) }
                )
            }
            when (uiState.selectedTabIndex) {
                0 -> UserTabsScreen(
                    uiState = uiState,
                    onTabClick = onTabClick,
                    onDeleteTab = { viewModel.deleteUserTab(it) },
                    onRenameTab = { tab, newName -> viewModel.renameUserTab(tab, newName) },
                    onAddFirstTab = { pickFileLauncher.launch(arrayOf("application/octet-stream", "text/plain", "*/*")) }
                )
                1 -> LessonsScreen(viewModel, uiState, onTabClick)
            }
        }
    }
}

@Composable
private fun UserTabsScreen(
    uiState: TabListUiState,
    onTabClick: (String) -> Unit,
    onDeleteTab: (TabItem) -> Unit,
    onRenameTab: (TabItem, String) -> Unit,
    onAddFirstTab: () -> Unit
) {
    var showMenu by remember { mutableStateOf<String?>(null) }
    var showRenameDialog by remember { mutableStateOf<TabItem?>(null) }
    var tabToDelete by remember { mutableStateOf<TabItem?>(null) }
    var newTabName by remember { mutableStateOf("") }

    if (showRenameDialog != null) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = null },
            title = { Text(stringResource(R.string.rename_tab)) },
            text = {
                OutlinedTextField(
                    value = newTabName,
                    onValueChange = { newTabName = it },
                    label = { Text(stringResource(R.string.new_name)) }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRenameDialog?.let { onRenameTab(it, newTabName) }
                        showRenameDialog = null
                    }
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (tabToDelete != null) {
        AlertDialog(
            onDismissRequest = { tabToDelete = null },
            title = { Text(stringResource(R.string.delete_tab_title)) },
            text = { Text(stringResource(R.string.delete_tab_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        tabToDelete?.let { onDeleteTab(it) }
                        tabToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { tabToDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)) {
        Text(
            text = stringResource(R.string.total_tabs, uiState.totalUserTabs),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (uiState.userTabs.isEmpty()) {
            EmptyTabsState(onAddFirstTab = onAddFirstTab)
            return@Column
        }
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(uiState.userTabs) { tab ->
                val progress = uiState.progressByTabId[tab.id] ?: 0
                val lastDuration = uiState.lastSessionDurationByTabId[tab.id]
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onTabClick(tab.id) },
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = tab.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            Box {
                                IconButton(onClick = { showMenu = tab.id }) {
                                    Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.menu))
                                }
                                DropdownMenu(
                                    expanded = showMenu == tab.id,
                                    onDismissRequest = { showMenu = null }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.rename)) },
                                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                        onClick = {
                                            newTabName = tab.name
                                            showRenameDialog = tab
                                            showMenu = null
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.delete)) },
                                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                                        onClick = {
                                            tabToDelete = tab
                                            showMenu = null
                                        }
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        TabMetricsRow(
                            progressPercent = progress,
                            lastSessionDuration = lastDuration
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LessonsScreen(
    viewModel: TabListViewModel,
    uiState: TabListUiState,
    onTabClick: (String) -> Unit
) {
    Column {
        SecondaryTabRow(selectedTabIndex = uiState.selectedDifficulty.ordinal) {
            Difficulty.values().forEach { difficulty ->
                Tab(
                    selected = uiState.selectedDifficulty == difficulty,
                    onClick = { viewModel.selectDifficulty(difficulty) },
                    text = {
                        val text = when (difficulty) {
                            Difficulty.BEGINNER -> stringResource(id = R.string.beginner)
                            Difficulty.INTERMEDIATE -> stringResource(id = R.string.intermediate)
                            Difficulty.ADVANCED -> stringResource(id = R.string.advanced)
                        }
                        Text(text)
                    }
                )
            }
        }
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(
                    R.string.completed_this_level,
                    uiState.completedLessonsInSelectedDifficulty,
                    uiState.totalLessonsInSelectedDifficulty
                ),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(
                    R.string.total_progress,
                    uiState.totalCompletedLessons,
                    uiState.totalLessons
                ),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(uiState.filteredTabs) { tab ->
                    val progress = uiState.progressByTabId[tab.id] ?: 0
                    val lastDuration = uiState.lastSessionDurationByTabId[tab.id]
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { onTabClick(tab.id) },
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
                                    textDecoration = if (tab.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = tab.description,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                TabMetricsRow(
                                    progressPercent = progress,
                                    lastSessionDuration = lastDuration
                                )
                            }
                            IconButton(onClick = { viewModel.toggleCompleted(tab.id) }) {
                                Icon(
                                    imageVector = if (tab.isCompleted) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                                    contentDescription = if (tab.isCompleted) stringResource(R.string.completed) else stringResource(R.string.mark_as_completed),
                                    tint = if (tab.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TabMetricsRow(
    progressPercent: Int,
    lastSessionDuration: Long?
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Timer,
                contentDescription = stringResource(R.string.last_session_label),
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(
                    R.string.last_session_value,
                    stringResource(R.string.last_session_label),
                    lastSessionDuration?.let { formatDuration(it) } ?: stringResource(R.string.no_data)
                ),
                style = MaterialTheme.typography.labelMedium
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = stringResource(R.string.tab_progress_label),
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(
                    R.string.tab_progress_value,
                    stringResource(R.string.tab_progress_label),
                    progressPercent
                ),
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Composable
private fun EmptyTabsState(onAddFirstTab: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 28.dp),
        contentAlignment = Alignment.Center
    ) {
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
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
                Button(onClick = onAddFirstTab) {
                    Text(stringResource(R.string.upload_first_gp))
                }
            }
        }
    }
}
