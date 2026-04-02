package com.guitarlearning.presentation.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.guitarlearning.presentation.tab_list.TabListScreen
import com.guitarlearning.presentation.tab_viewer.TabViewerScreen
import java.net.URLDecoder
import java.net.URLEncoder

fun NavGraphBuilder.lessonsNavGraph(
    navController: NavHostController,
    route: String
) {
    navigation(startDestination = "tab_list", route = route) {
        composable("tab_list") {
            TabListScreen { tabId ->
                val encodedId = URLEncoder.encode(tabId, "UTF-8")
                navController.navigate("lesson/$encodedId")
            }
        }
        composable("lesson/{lessonId}") { backStackEntry ->
            val lessonId = backStackEntry.arguments?.getString("lessonId") ?: ""
            val decodedId = URLDecoder.decode(lessonId, "UTF-8")
            TabViewerScreen(
                lessonId = decodedId,
            ) {
                navController.popBackStack()
            }
        }
    }
}
