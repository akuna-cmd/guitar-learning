package com.guitarlearning

import android.content.Context
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.guitarlearning.core.webview.WebViewWarmup
import com.guitarlearning.domain.settings.ThemeMode
import com.guitarlearning.presentation.goals.GoalsScreen
import com.guitarlearning.presentation.main.HomeScreen
import com.guitarlearning.presentation.main.MainViewModel
import com.guitarlearning.presentation.main.SessionHistoryScreen
import com.guitarlearning.presentation.navigation.BottomNavItem
import com.guitarlearning.presentation.navigation.lessonsNavGraph
import com.guitarlearning.presentation.settings.SettingsScreen
import com.guitarlearning.presentation.ui.AppBar
import com.guitarlearning.presentation.ui.theme.GuitarLearningTheme
import com.guitarlearning.core.locale.AppLocaleManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLocaleManager.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        setContent {
            val themeViewModel = hiltViewModel<com.guitarlearning.presentation.main.ThemeViewModel>()
            val themeUiState by themeViewModel
                .uiState
                .collectAsStateWithLifecycle()
            val mainViewModel = hiltViewModel<com.guitarlearning.presentation.main.MainViewModel>()
            val mainUiState by mainViewModel.uiState.collectAsStateWithLifecycle()

            splashScreen.setKeepOnScreenCondition { themeUiState.isLoading || mainUiState.isLoading }

            val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
            val isDarkTheme = when (themeUiState.themeMode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> isSystemDark
            }

            GuitarLearningTheme(darkTheme = isDarkTheme) {
                MainScreen(mainViewModel = mainViewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    mainViewModel: MainViewModel
) {
    val context = LocalContext.current
    val uiState by mainViewModel.uiState.collectAsStateWithLifecycle()

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
    LaunchedEffect(uiState.continueLessonId) {
        val lessonId = uiState.continueLessonId
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
            if (uiState.isSessionActive) {
                AppBar(
                    sessionDuration = uiState.sessionDuration,
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
                    items.forEach { screen ->
                        val isSelected = currentDestination
                            ?.hierarchy
                            ?.any { destination -> destination.route == screen.route } == true
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                if (!isSelected) {
                                    if (screen.route == BottomNavItem.Main.route) {
                                        mainViewModel.consumeContinueLesson()
                                        val poppedToMain = navController.popBackStack(
                                            BottomNavItem.Main.route,
                                            inclusive = false
                                        )
                                        if (!poppedToMain) {
                                            navController.navigate(screen.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                restoreState = true
                                                launchSingleTop = true
                                            }
                                        }
                                    } else {
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            restoreState = true
                                            launchSingleTop = true
                                        }
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = screen.icon,
                                    contentDescription = null
                                )
                            },
                            label = {
                                Text(
                                    text = stringResource(id = screen.titleResId),
                                    modifier = Modifier.fillMaxWidth(),
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 9.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Clip,
                                    textAlign = TextAlign.Center
                                )
                            },
                            alwaysShowLabel = true,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onSurface,
                                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                indicatorColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f),
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
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
            composable(route = BottomNavItem.Main.route) {
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
                    lastPlaybackProgressFlow = mainViewModel.lastPlaybackProgress
                )
            }
            lessonsNavGraph(
                navController = navController,
                route = BottomNavItem.GuitarTabs.route
            )
            composable(route = BottomNavItem.Goals.route) {
                GoalsScreen()
            }
            composable(route = BottomNavItem.Settings.route) {
                SettingsScreen()
            }
            composable(route = "session_history") {
                val mainUiState by mainViewModel.uiState.collectAsStateWithLifecycle()
                SessionHistoryScreen(
                    sessions = mainUiState.sessions,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

