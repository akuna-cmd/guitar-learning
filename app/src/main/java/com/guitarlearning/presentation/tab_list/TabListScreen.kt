package com.guitarlearning.presentation.tab_list

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.guitarlearning.R
import com.guitarlearning.presentation.ui.WebViewWarmup
import com.guitarlearning.presentation.ui.theme.appBlockBorder
import kotlinx.coroutines.delay

private val TabMimeTypes = arrayOf("application/octet-stream", "text/plain", "*/*")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabListScreen(
    onTabClick: (String) -> Unit
) {
    val viewModel: TabListViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val localeTag = context.resources.configuration.locales[0]?.toLanguageTag().orEmpty()

    val pickFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri: Uri? ->
            uri?.let(viewModel::addUserTab)
        }
    )

    LaunchedEffect(Unit) {
        delay(350)
        WebViewWarmup.warm(context)
    }

    LaunchedEffect(localeTag) {
        viewModel.refreshBuiltInTabLocalizations()
    }

    Scaffold(
        floatingActionButton = {
            if (uiState.selectedTabIndex == 0) {
                FloatingActionButton(onClick = { pickFileLauncher.launch(TabMimeTypes) }) {
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
            TabListHeader(
                selectedTabIndex = uiState.selectedTabIndex,
                totalUserTabs = uiState.totalUserTabs,
                totalCompletedLessons = uiState.totalCompletedLessons,
                totalLessons = uiState.totalLessons,
                onSelectTab = viewModel::selectTab
            )
            uiState.message?.let { message ->
                TabListMessageCard(
                    message = message,
                    onDismiss = viewModel::clearMessage
                )
            }
            when (uiState.selectedTabIndex) {
                0 -> UserTabsScreen(
                    viewModel = viewModel,
                    uiState = uiState,
                    onTabClick = onTabClick,
                    onDeleteTab = viewModel::deleteUserTab,
                    onRenameTab = viewModel::renameUserTab,
                    onAddFirstTab = { pickFileLauncher.launch(TabMimeTypes) }
                )

                1 -> LessonsScreen(
                    viewModel = viewModel,
                    uiState = uiState,
                    onTabClick = onTabClick
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TabListHeader(
    selectedTabIndex: Int,
    totalUserTabs: Int,
    totalCompletedLessons: Int,
    totalLessons: Int,
    onSelectTab: (Int) -> Unit
) {
    PrimaryTabRow(selectedTabIndex = selectedTabIndex) {
        Tab(
            selected = selectedTabIndex == 0,
            onClick = { onSelectTab(0) },
            text = { Text("${stringResource(R.string.my_tabs_tab)} ($totalUserTabs)") },
            icon = { Icon(Icons.Default.MusicNote, contentDescription = null) }
        )
        Tab(
            selected = selectedTabIndex == 1,
            onClick = { onSelectTab(1) },
            text = { Text("${stringResource(R.string.lessons_tab)} ($totalCompletedLessons/$totalLessons)") },
            icon = { Icon(Icons.Default.School, contentDescription = null) }
        )
    }
}

@Composable
private fun TabListMessageCard(
    message: String,
    onDismiss: () -> Unit
) {
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
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.ok),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}
