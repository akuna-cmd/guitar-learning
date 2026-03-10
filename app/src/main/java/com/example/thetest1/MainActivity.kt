package com.example.thetest1

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.thetest1.di.ViewModelFactory
import com.example.thetest1.presentation.common.AppBar
import com.example.thetest1.presentation.goals.GoalsScreen
import com.example.thetest1.presentation.main.HomeScreen
import com.example.thetest1.presentation.main.MainViewModel
import com.example.thetest1.presentation.main.ThemeViewModel
import com.example.thetest1.presentation.navigation.BottomNavItem
import com.example.thetest1.presentation.navigation.LessonsNavHost
import com.example.thetest1.presentation.settings.SettingsScreen
import com.example.thetest1.presentation.ui.theme.TheTest1Theme

class MainActivity : ComponentActivity() {
    private lateinit var viewModelFactory: ViewModelFactory

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        viewModelFactory = (application as MainApplication).appContainer.viewModelFactory

        setContent {
            val themeViewModel: ThemeViewModel = viewModel(factory = viewModelFactory)
            val themeUiState by themeViewModel.uiState.collectAsState()

            splashScreen.setKeepOnScreenCondition { themeUiState.isLoading }

            TheTest1Theme(darkTheme = themeUiState.isDarkTheme) {
                MainScreen(
                    viewModelFactory = viewModelFactory,
                    themeViewModel = themeViewModel,
                    isDarkTheme = themeUiState.isDarkTheme
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
    val mainViewModel: MainViewModel = viewModel(factory = viewModelFactory)
    val mainUiState by mainViewModel.uiState.collectAsState()

    val navController = rememberNavController()
    Scaffold(
        topBar = {
            if (mainUiState.isSessionActive) {
                AppBar(
                    sessionDuration = mainUiState.sessionDuration,
                    onStopSession = mainViewModel::stopSession
                )
            }
        },
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                val items = listOf(
                    BottomNavItem.Main,
                    BottomNavItem.GuitarTabs,
                    BottomNavItem.Goals,
                    BottomNavItem.Settings
                )
                items.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = null) },
                        label = {
                            Text(
                                text = stringResource(id = screen.titleResId),
                                fontSize = 10.sp,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                        },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
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
                HomeScreen(
                    sessions = mainUiState.sessions,
                    onStartSession = mainViewModel::startSession,
                    isSessionActive = mainUiState.isSessionActive,
                    totalSessionTime = mainUiState.totalSessionTime,
                    lessonsCompleted = mainUiState.lessonsCompleted,
                    totalLessons = mainUiState.totalLessons,
                    userTabsCount = mainUiState.userTabsCount,
                    viewModelFactory = viewModelFactory,
                    onToggleTheme = themeViewModel::toggleTheme,
                    isDarkTheme = isDarkTheme
                )
            }
            composable(BottomNavItem.GuitarTabs.route) {
                LessonsNavHost(viewModelFactory = viewModelFactory, mainViewModel = mainViewModel)
            }
            composable(BottomNavItem.Goals.route) {
                GoalsScreen(viewModelFactory = viewModelFactory)
            }
            composable(BottomNavItem.Settings.route) {
                SettingsScreen()
            }
        }
    }
}
