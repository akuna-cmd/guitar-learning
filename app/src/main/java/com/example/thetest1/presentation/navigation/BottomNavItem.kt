package com.example.thetest1.presentation.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.thetest1.R

sealed class BottomNavItem(
    val route: String,
    @StringRes val titleResId: Int,
    val icon: ImageVector
) {
    object Main : BottomNavItem(
        route = "main",
        titleResId = R.string.main,
        icon = Icons.Default.Home
    )

    object GuitarTabs : BottomNavItem(
        route = "guitar_tabs",
        titleResId = R.string.learning,
        icon = Icons.Default.MusicNote
    )

    object Goals : BottomNavItem(
        route = "goals",
        titleResId = R.string.goals,
        icon = Icons.Default.Check
    )

    object Settings : BottomNavItem(
        route = "settings",
        titleResId = R.string.settings,
        icon = Icons.Default.Settings
    )
}
