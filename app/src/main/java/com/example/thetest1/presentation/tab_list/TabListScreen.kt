package com.example.thetest1.presentation.tab_list

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.thetest1.R
import com.example.thetest1.domain.model.Difficulty
import com.example.thetest1.domain.model.TabItem
import com.example.thetest1.presentation.ui.WebViewWarmup
import com.example.thetest1.presentation.ui.theme.appBlockBorder
import com.example.thetest1.presentation.ui.formatDuration
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabListScreen(
    onTabClick: (String) -> Unit
) {
    val viewModel: TabListViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showAddMenuDialog by remember { mutableStateOf(false) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var newFolderFromFab by remember { mutableStateOf("") }

    val pickFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri: Uri? ->
            uri?.let { viewModel.addUserTab(it) }
        }
    )

    LaunchedEffect(Unit) {
        delay(350)
        WebViewWarmup.warm(context)
    }

    Scaffold(
        floatingActionButton = {
            if (uiState.selectedTabIndex == 0) {
                FloatingActionButton(onClick = { showAddMenuDialog = true }) {
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
            uiState.message?.let { message ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    border = appBlockBorder(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = message,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        TextButton(onClick = viewModel::clearMessage) {
                            Text(
                                text = stringResource(R.string.ok),
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
            when (uiState.selectedTabIndex) {
                0 -> UserTabsScreen(
                    viewModel = viewModel,
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

    if (showAddMenuDialog) {
        AlertDialog(
            onDismissRequest = { showAddMenuDialog = false },
            title = { Text("Обери дію") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilledTonalButton(
                        onClick = {
                            showAddMenuDialog = false
                            showCreateFolderDialog = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Folder, contentDescription = null)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Нова папка", style = MaterialTheme.typography.titleMedium)
                    }
                    FilledTonalButton(
                        onClick = {
                            showAddMenuDialog = false
                            pickFileLauncher.launch(arrayOf("application/octet-stream", "text/plain", "*/*"))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.LibraryMusic, contentDescription = null)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Табулатура", style = MaterialTheme.typography.titleMedium)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAddMenuDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showCreateFolderDialog) {
        AlertDialog(
            onDismissRequest = { showCreateFolderDialog = false },
            title = { Text("Створити папку") },
            text = {
                OutlinedTextField(
                    value = newFolderFromFab,
                    onValueChange = { newFolderFromFab = it },
                    label = { Text("Назва папки") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.createFolder(newFolderFromFab)
                        newFolderFromFab = ""
                        showCreateFolderDialog = false
                    }
                ) { Text("Створити") }
            },
            dismissButton = {
                TextButton(onClick = {
                    newFolderFromFab = ""
                    showCreateFolderDialog = false
                }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserTabsScreen(
    viewModel: TabListViewModel,
    uiState: TabListUiState,
    onTabClick: (String) -> Unit,
    onDeleteTab: (TabItem) -> Unit,
    onRenameTab: (TabItem, String) -> Unit,
    onAddFirstTab: () -> Unit
) {
    var showMenu by remember { mutableStateOf<String?>(null) }
    var showRenameDialog by remember { mutableStateOf<TabItem?>(null) }
    var tabToDelete by remember { mutableStateOf<TabItem?>(null) }
    var tabToMove by remember { mutableStateOf<TabItem?>(null) }
    var newTabName by remember { mutableStateOf("") }
    var newFolderName by remember { mutableStateOf("") }
    var folderMenuExpanded by remember { mutableStateOf(false) }
    var folderActionsFor by remember { mutableStateOf<String?>(null) }
    var folderToRename by remember { mutableStateOf<String?>(null) }
    var renameFolderValue by remember { mutableStateOf("") }
    var folderToDelete by remember { mutableStateOf<String?>(null) }

    showRenameDialog?.let { tab ->
        AlertDialog(
            onDismissRequest = { showRenameDialog = null },
            title = { Text(stringResource(R.string.rename_tab)) },
            text = {
                OutlinedTextField(
                    value = newTabName,
                    onValueChange = { newTabName = it },
                    label = { Text(stringResource(R.string.new_name)) },
                    shape = RoundedCornerShape(16.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRenameTab(tab, newTabName)
                        showRenameDialog = null
                    }
                ) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    tabToDelete?.let { tab ->
        AlertDialog(
            onDismissRequest = { tabToDelete = null },
            title = { Text(stringResource(R.string.delete_tab_title)) },
            text = { Text(stringResource(R.string.delete_tab_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteTab(tab)
                        tabToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { tabToDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    tabToMove?.let { tab ->
        AlertDialog(
            onDismissRequest = { tabToMove = null },
            title = { Text("Перемістити у") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (uiState.availableFolders.isEmpty()) {
                        Text(
                            text = "Ще немає папок. Створи першу папку нижче.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    LazyColumn(modifier = Modifier.heightIn(max = 180.dp)) {
                        items(uiState.availableFolders) { folder ->
                            AssistChip(
                                onClick = {
                                    viewModel.moveToFolder(tab, folder)
                                    tabToMove = null
                                },
                                label = { Text(folder) },
                                leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null) }
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newFolderName,
                        onValueChange = { newFolderName = it },
                        label = { Text("Нова папка") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val folder = newFolderName.trim()
                        if (folder.isNotEmpty()) {
                            viewModel.moveToFolder(tab, folder)
                            newFolderName = ""
                            tabToMove = null
                        }
                    }
                ) {
                    Text("Створити і перемістити")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    newFolderName = ""
                    tabToMove = null
                }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    folderActionsFor?.let { folder ->
        AlertDialog(
            onDismissRequest = { folderActionsFor = null },
            title = { Text("Папка: $folder") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilledTonalButton(
                        onClick = {
                            folderToRename = folder
                            renameFolderValue = folder
                            folderActionsFor = null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Перейменувати", style = MaterialTheme.typography.titleMedium)
                    }
                    FilledTonalButton(
                        onClick = {
                            folderToDelete = folder
                            folderActionsFor = null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Видалити", style = MaterialTheme.typography.titleMedium)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { folderActionsFor = null }) { Text(stringResource(R.string.cancel)) }
            },
            dismissButton = {}
        )
    }

    folderToRename?.let { folder ->
        AlertDialog(
            onDismissRequest = { folderToRename = null },
            title = { Text("Перейменувати папку") },
            text = {
                OutlinedTextField(
                    value = renameFolderValue,
                    onValueChange = { renameFolderValue = it },
                    label = { Text("Нова назва папки") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.renameFolder(folder, renameFolderValue)
                        folderToRename = null
                    }
                ) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { folderToRename = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    folderToDelete?.let { folder ->
        AlertDialog(
            onDismissRequest = { folderToDelete = null },
            title = { Text("Видалити папку") },
            text = { Text("Таби з цієї папки будуть переміщені в \"Без папки\".") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteFolder(folder)
                        folderToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { folderToDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.total_tabs, uiState.totalUserTabs),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (uiState.isUserTabsLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 28.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Column
        }

        if (uiState.userTabs.isEmpty()) {
            EmptyTabsState(onAddFirstTab = onAddFirstTab)
            return@Column
        }

        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            item {
                ExposedDropdownMenuBox(
                    expanded = folderMenuExpanded,
                    onExpandedChange = { folderMenuExpanded = !folderMenuExpanded }
                ) {
                    OutlinedTextField(
                        value = uiState.selectedFolder ?: "Усі папки",
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = folderMenuExpanded) }
                    )
                    ExposedDropdownMenu(
                        expanded = folderMenuExpanded,
                        onDismissRequest = { folderMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Усі папки") },
                            onClick = {
                                viewModel.selectFolder(null)
                                folderMenuExpanded = false
                            }
                        )
                        uiState.availableFolders.forEach { folder ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(folder)
                                        if (folder != "Без папки") {
                                            Text(
                                                text = "⋮",
                                                style = MaterialTheme.typography.headlineSmall,
                                                fontSize = 30.sp,
                                                modifier = Modifier.clickable {
                                                    folderActionsFor = folder
                                                }
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    viewModel.selectFolder(folder)
                                    folderMenuExpanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(uiState.filteredUserTabs, key = { it.id }) { tab ->
                val progress = if (uiState.areMetricsLoading) null else (uiState.progressByTabId[tab.id] ?: 0)
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .border(appBlockBorder(), RoundedCornerShape(16.dp))
                        .clickable {
                            viewModel.markTabOpened(tab.id)
                            onTabClick(tab.id)
                        }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
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
                                        text = { Text("Перемістити у") },
                                        leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null) },
                                        onClick = {
                                            tabToMove = tab
                                            showMenu = null
                                        }
                                    )
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier.weight(1f, fill = false),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Surface(
                                    modifier = Modifier.wrapContentWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Folder,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                        Text(
                                            text = uiState.displayFolder(tab),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                            TabProgressBadge(progressPercent = progress)
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
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .border(appBlockBorder(), RoundedCornerShape(16.dp))
                            .clickable {
                                viewModel.markTabOpened(tab.id)
                                onTabClick(tab.id)
                            }
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
                                TabProgressBadge(progressPercent = progress)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
}

@Composable
private fun TabProgressBadge(
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
private fun EmptyTabsState(onAddFirstTab: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 28.dp),
        contentAlignment = Alignment.Center
    ) {
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .border(appBlockBorder(), RoundedCornerShape(16.dp))
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
