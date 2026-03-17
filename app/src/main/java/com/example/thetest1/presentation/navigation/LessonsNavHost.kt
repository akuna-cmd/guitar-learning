package com.example.thetest1.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.thetest1.di.ViewModelFactory
import com.example.thetest1.presentation.main.MainViewModel
import com.example.thetest1.presentation.tab_list.TabListScreen
import com.example.thetest1.presentation.tab_viewer.TabViewerScreen
import java.net.URLDecoder
import java.net.URLEncoder

@Composable
fun LessonsNavHost(viewModelFactory: ViewModelFactory, mainViewModel: MainViewModel) {
    val lessonsNavController = rememberNavController()
    val mainUiState by mainViewModel.uiState.collectAsState()

    LaunchedEffect(mainUiState.continueLessonId) {
        val lessonId = mainUiState.continueLessonId
        if (!lessonId.isNullOrBlank()) {
            val encodedId = URLEncoder.encode(lessonId, "UTF-8")
            lessonsNavController.navigate("lesson/$encodedId") {
                launchSingleTop = true
            }
            mainViewModel.consumeContinueLesson()
        }
    }

    NavHost(lessonsNavController, startDestination = "tab_list") {
        composable("tab_list") {
            TabListScreen(viewModelFactory = viewModelFactory) { tabId ->
                val encodedId = URLEncoder.encode(tabId, "UTF-8")
                lessonsNavController.navigate("lesson/$encodedId")
            }
        }
        composable("lesson/{lessonId}") { backStackEntry ->
            val lessonId = backStackEntry.arguments?.getString("lessonId") ?: ""
            val decodedId = URLDecoder.decode(lessonId, "UTF-8")
            TabViewerScreen(
                lessonId = decodedId,
                viewModelFactory = viewModelFactory,
                mainViewModel = mainViewModel,
            ) {
                lessonsNavController.popBackStack()
            }
        }
    }
}
