package com.guitarlearning.presentation.ai_assistant

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun PulsingDotsIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    dotSize: Dp = 12.dp,
    spacing: Dp = 8.dp,
    pulseDelay: Int = 300
) {
    val dots = List(3) { remember { Animatable(0f) } }

    dots.forEachIndexed { index, animatable ->
        LaunchedEffect(animatable) {
            delay(index * 100L)
            animatable.animateTo(
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = keyframes {
                        durationMillis = pulseDelay * 2
                        0.0f at 0 with LinearEasing
                        1.0f at pulseDelay / 2 with LinearEasing
                        0.0f at pulseDelay
                        0.0f at pulseDelay * 2
                    },
                    repeatMode = RepeatMode.Restart
                )
            )
        }
    }

    val yOffset = with(LocalDensity.current) { (dotSize / 2).toPx() }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(spacing)
    ) {
        dots.forEach { animatable ->
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .graphicsLayer {
                        translationY = -animatable.value * yOffset
                    }
                    .background(
                        color = color,
                        shape = CircleShape
                    )
            )
        }
    }
}
