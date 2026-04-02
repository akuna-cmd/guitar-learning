package com.guitarlearning.presentation.ui

import java.util.Locale
import java.util.concurrent.TimeUnit

fun formatDuration(millis: Long): String {
    val hours = TimeUnit.MILLISECONDS.toHours(millis)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
    return String.format("%02d:%02d:%02d", hours, minutes, seconds)
}

fun formatDurationShort(millis: Long): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
    return String.format("%02d:%02d", minutes, seconds)
}

fun formatSpeed(speed: Float): String {
    return String.format(Locale.US, "%.1f", speed)
}

fun stepSpeed(value: Float, delta: Float): Float {
    val stepped = ((value + delta) * 10f).toInt() / 10f
    return stepped.coerceIn(0.1f, 2.5f)
}

fun formatScale(scale: Float): String {
    return String.format(Locale.US, "%.1f", scale)
}

fun stepScale(value: Float, delta: Float): Float {
    val stepped = ((value + delta) * 10f).toInt() / 10f
    return stepped.coerceIn(0.5f, 2.0f)
}
