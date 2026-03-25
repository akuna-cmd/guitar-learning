package com.example.thetest1.presentation.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.example.thetest1.di.ViewModelFactory
import com.example.thetest1.presentation.main.MainViewModel
import com.example.thetest1.presentation.main.ThemeViewModel
import com.example.thetest1.presentation.tab_list.TabListScreen
import com.example.thetest1.presentation.tab_viewer.TabViewerScreen
import java.net.URLDecoder
import java.net.URLEncoder

fun NavGraphBuilder.lessonsNavGraph(
    navController: NavHostController,
    viewModelFactory: ViewModelFactory,
    mainViewModel: MainViewModel,
    themeViewModel: ThemeViewModel,
    route: String
) {
    navigation(startDestination = "tab_list", route = route) {
        composable("tab_list") {
            TabListScreen(viewModelFactory = viewModelFactory) { tabId ->
                val encodedId = URLEncoder.encode(tabId, "UTF-8")
                navController.navigate("lesson/$encodedId")
            }
        }
        composable("lesson/{lessonId}") { backStackEntry ->
            val lessonId = backStackEntry.arguments?.getString("lessonId") ?: ""
            val decodedId = URLDecoder.decode(lessonId, "UTF-8")
            TabViewerScreen(
                lessonId = decodedId,
                viewModelFactory = viewModelFactory,
                mainViewModel = mainViewModel,
                themeViewModel = themeViewModel,
            ) {
                navController.popBackStack()
            }
        }
    }
}
