package com.guitarlearning.presentation.tab_list

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.guitarlearning.R
import com.guitarlearning.domain.model.TabItem
import com.guitarlearning.presentation.tab_viewer.TabLoadMetricsTracker
import com.guitarlearning.presentation.ui.displayTabFolder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun UserTabsScreen(
    viewModel: TabListViewModel,
    uiState: TabListUiState,
    onTabClick: (String) -> Unit,
    onDeleteTab: (TabItem) -> Unit,
    onRenameTab: (TabItem, String) -> Unit,
    onAddFirstTab: () -> Unit
) {
    val context = LocalContext.current
    val allFoldersLabel = stringResource(R.string.tab_list_all_folders)
    val noFolderLabel = stringResource(R.string.tab_folder_default)
    var showMenu by remember { mutableStateOf<String?>(null) }
    var showRenameDialog by remember { mutableStateOf<TabItem?>(null) }
    var tabToDelete by remember { mutableStateOf<TabItem?>(null) }
    var tabToMove by remember { mutableStateOf<TabItem?>(null) }
    var newTabName by remember { mutableStateOf("") }
    var newFolderName by remember { mutableStateOf("") }
    var folderActionsExpanded by remember { mutableStateOf(false) }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var newFolderValue by remember { mutableStateOf("") }
    var folderToRename by remember { mutableStateOf<String?>(null) }
    var renameFolderValue by remember { mutableStateOf("") }
    var folderToDelete by remember { mutableStateOf<String?>(null) }

    RenameTabDialog(
        tab = showRenameDialog,
        newName = newTabName,
        onNameChange = { newTabName = it },
        onDismiss = { showRenameDialog = null },
        onConfirm = { tab ->
            onRenameTab(tab, newTabName)
            showRenameDialog = null
        }
    )
    DeleteTabDialog(
        tab = tabToDelete,
        onDismiss = { tabToDelete = null },
        onConfirm = { tab ->
            onDeleteTab(tab)
            tabToDelete = null
        }
    )
    MoveTabDialog(
        tab = tabToMove,
        availableFolders = uiState.availableFolders,
        newFolderName = newFolderName,
        onFolderNameChange = { newFolderName = it },
        onDismiss = {
            newFolderName = ""
            tabToMove = null
        },
        onMoveToFolder = { tab, folder ->
            viewModel.moveToFolder(tab, folder)
            tabToMove = null
        },
        onCreateAndMove = { tab ->
            val folder = newFolderName.trim()
            if (folder.isNotEmpty()) {
                viewModel.moveToFolder(tab, folder)
                newFolderName = ""
                tabToMove = null
            }
        }
    )
    FolderDialogs(
        showCreateFolderDialog = showCreateFolderDialog,
        newFolderValue = newFolderValue,
        onNewFolderValueChange = { newFolderValue = it },
        onDismissCreate = {
            newFolderValue = ""
            showCreateFolderDialog = false
        },
        onCreate = {
            viewModel.createFolder(newFolderValue)
            newFolderValue = ""
            showCreateFolderDialog = false
        },
        folderToRename = folderToRename,
        renameFolderValue = renameFolderValue,
        onRenameFolderValueChange = { renameFolderValue = it },
        onDismissRename = { folderToRename = null },
        onRename = { folder ->
            viewModel.renameFolder(folder, renameFolderValue)
            folderToRename = null
        },
        folderToDelete = folderToDelete,
        noFolderLabel = noFolderLabel,
        onDismissDelete = { folderToDelete = null },
        onDelete = { folder ->
            viewModel.deleteFolder(folder)
            folderToDelete = null
        }
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
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
                UserTabsToolbar(
                    uiState = uiState,
                    allFoldersLabel = allFoldersLabel,
                    onQueryChange = viewModel::updateUserTabsQuery,
                    onSelectFolder = viewModel::selectFolder,
                    folderActionsExpanded = folderActionsExpanded,
                    onFolderActionsExpandedChange = { folderActionsExpanded = it },
                    onShowCreateFolderDialog = { showCreateFolderDialog = true },
                    onShowRenameFolderDialog = {
                        folderToRename = uiState.selectedFolder
                        renameFolderValue = uiState.selectedFolder.orEmpty()
                    },
                    onShowDeleteFolderDialog = { folderToDelete = uiState.selectedFolder },
                    sortMenuExpanded = sortMenuExpanded,
                    onSortMenuExpandedChange = { sortMenuExpanded = it },
                    onSelectSortMode = viewModel::updateUserTabsSortMode
                )
            }

            items(uiState.filteredUserTabs, key = { it.id }) { tab ->
                UserTabCard(
                    tab = tab,
                    progress = if (uiState.areMetricsLoading) null else (uiState.progressByTabId[tab.id] ?: 0),
                    folderLabel = displayTabFolder(context, uiState.displayFolder(tab)),
                    onOpen = {
                        TabLoadMetricsTracker.start(tab.id, tab.name)
                        onTabClick(tab.id)
                    },
                    onMenuExpanded = { showMenu = tab.id },
                    isMenuExpanded = showMenu == tab.id,
                    onMenuDismiss = { showMenu = null },
                    onMove = {
                        tabToMove = tab
                        showMenu = null
                    },
                    onRename = {
                        newTabName = tab.name
                        showRenameDialog = tab
                        showMenu = null
                    },
                    onDelete = {
                        tabToDelete = tab
                        showMenu = null
                    }
                )
            }
        }
    }
}
