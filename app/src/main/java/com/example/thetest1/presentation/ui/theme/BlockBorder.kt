package com.example.thetest1.presentation.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

@Composable
fun appBlockBorder(): BorderStroke {
    return BorderStroke(
        width = 0.8.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.78f)
    )
}
