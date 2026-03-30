package com.example.thetest1

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.thetest1.di.ViewModelFactory
import com.example.thetest1.presentation.common.AppBar
import com.example.thetest1.presentation.common.WebViewWarmup
import com.example.thetest1.presentation.goals.GoalsScreen
import com.example.thetest1.presentation.main.HomeScreen
import com.example.thetest1.presentation.main.MainViewModel
import com.example.thetest1.presentation.main.ThemeViewModel
import com.example.thetest1.presentation.navigation.BottomNavItem
import com.example.thetest1.presentation.navigation.lessonsNavGraph
import com.example.thetest1.presentation.settings.SettingsScreen
import com.example.thetest1.presentation.ui.theme.TheTest1Theme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private lateinit var viewModelFactory: ViewModelFactory

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        viewModelFactory = (application as MainApplication).appContainer.viewModelFactory

        setContent {
            val themeViewModel: ThemeViewModel = viewModel(factory = viewModelFactory)
            val themeUiState by themeViewModel.uiState.collectAsStateWithLifecycle()

            splashScreen.setKeepOnScreenCondition { false }

            val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
            val isDarkTheme = when (themeUiState.themeMode) {
                com.example.thetest1.presentation.main.ThemeMode.DARK -> true
                com.example.thetest1.presentation.main.ThemeMode.LIGHT -> false
                com.example.thetest1.presentation.main.ThemeMode.SYSTEM -> isSystemDark
            }

            TheTest1Theme(darkTheme = isDarkTheme) {
                MainScreen(
                    viewModelFactory = viewModelFactory,
                    themeViewModel = themeViewModel,
                    isDarkTheme = isDarkTheme
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModelFactory: ViewModelFactory,
    themeViewModel: ThemeViewModel,
    isDarkTheme: Boolean
) {
    val context = LocalContext.current
    val mainViewModel: MainViewModel = viewModel(factory = viewModelFactory)
    val shellUiState by mainViewModel.shellUiState.collectAsStateWithLifecycle()

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val bottomRoutes = remember {
        setOf(
            BottomNavItem.Main.route,
            BottomNavItem.GuitarTabs.route,
            BottomNavItem.Goals.route,
            BottomNavItem.Settings.route
        )
    }
    val isBottomBarVisible = currentDestination
        ?.hierarchy
        ?.any { destination -> destination.route in bottomRoutes } == true &&
        currentDestination?.route != "lesson/{lessonId}"
    LaunchedEffect(shellUiState.continueLessonId) {
        val lessonId = shellUiState.continueLessonId
        if (!lessonId.isNullOrBlank()) {
            val encodedId = java.net.URLEncoder.encode(lessonId, "UTF-8")
            navController.navigate("lesson/$encodedId") {
                launchSingleTop = true
            }
            mainViewModel.consumeContinueLesson()
        }
    }

    LaunchedEffect(Unit) {
        delay(900)
        WebViewWarmup.warm(context)
    }

    Scaffold(
        topBar = {
            if (shellUiState.isSessionActive) {
                AppBar(
                    sessionDuration = shellUiState.sessionDuration,
                    onStopSession = mainViewModel::stopSession
                )
            }
        },
        bottomBar = {
            if (isBottomBarVisible) {
                NavigationBar(
                    modifier = Modifier.navigationBarsPadding(),
                    tonalElevation = 3.dp
                ) {
                    val items = listOf(
                        BottomNavItem.Main,
                        BottomNavItem.GuitarTabs,
                        BottomNavItem.Goals,
                        BottomNavItem.Settings
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(72.dp)
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items.forEach { screen ->
                            val isSelected = currentDestination
                                ?.hierarchy
                                ?.any { destination -> destination.route == screen.route } == true
                            BottomBarItem(
                                title = stringResource(id = screen.titleResId),
                                icon = screen.icon,
                                selected = isSelected,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    if (!isSelected) {
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            restoreState = true
                                            launchSingleTop = true
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Main.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomNavItem.Main.route) {
                val mainUiState by mainViewModel.uiState.collectAsStateWithLifecycle()
                HomeScreen(
                    sessions = mainUiState.sessions,
                    onStartSession = mainViewModel::startSession,
                    onContinueLesson = { tabId ->
                        mainViewModel.requestContinueLesson(tabId)
                        navController.navigate(BottomNavItem.GuitarTabs.route) {
                            popUpTo(navController.graph.findStartDestination().id)
                            launchSingleTop = true
                        }
                    },
                    isSessionActive = mainUiState.isSessionActive,
                    totalSessionTime = mainUiState.totalSessionTime,
                    lessonsCompleted = mainUiState.lessonsCompleted,
                    totalLessons = mainUiState.totalLessons,
                    userTabsCount = mainUiState.userTabsCount,
                    viewModelFactory = viewModelFactory,
                    lastPlaybackProgressFlow = mainViewModel.lastPlaybackProgress
                )
            }
            lessonsNavGraph(
                navController = navController,
                viewModelFactory = viewModelFactory,
                mainViewModel = mainViewModel,
                themeViewModel = themeViewModel,
                route = BottomNavItem.GuitarTabs.route
            )
            composable(BottomNavItem.Goals.route) {
                GoalsScreen(viewModelFactory = viewModelFactory)
            }
            composable(BottomNavItem.Settings.route) {
                SettingsScreen(
                    viewModelFactory = viewModelFactory,
                    themeViewModel = themeViewModel
                )
            }
        }
    }
}

@Composable
private fun BottomBarItem(
    title: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val backgroundColor = if (selected) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f)
    } else {
        Color.Transparent
    }

    Box(
        modifier = modifier
            .height(64.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                modifier = Modifier.width(74.dp),
                color = contentColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                softWrap = true,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}
