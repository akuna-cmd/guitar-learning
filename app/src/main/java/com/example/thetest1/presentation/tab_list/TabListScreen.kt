package com.example.thetest1.presentation.tab_list

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.School
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
                    onRenameTab = { tab, newName -> viewModel.renameUserTab(tab, newName) }
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
    onRenameTab: (TabItem, String) -> Unit
) {
    var showMenu by remember { mutableStateOf<String?>(null) }
    var showRenameDialog by remember { mutableStateOf<TabItem?>(null) }
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
                Button(onClick = { showRenameDialog = null }) {
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
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(uiState.userTabs) { tab ->
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onTabClick(tab.id) },
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
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
                                    onClick = {
                                        newTabName = tab.name
                                        showRenameDialog = tab
                                        showMenu = null
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.delete)) },
                                    onClick = {
                                        onDeleteTab(tab)
                                        showMenu = null
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
