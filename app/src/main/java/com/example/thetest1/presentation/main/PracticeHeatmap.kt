package com.example.thetest1.presentation.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.thetest1.R
import com.example.thetest1.di.ViewModelFactory
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun PracticeHeatmap(
    viewModelFactory: ViewModelFactory
) {
    val viewModel: PracticeHeatmapViewModel = viewModel(factory = viewModelFactory)
    val uiState by viewModel.uiState.collectAsState()

    Heatmap(activityData = uiState.activityData)

}


@Composable
fun Heatmap(activityData: Map<Date, Int>) {
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.DAY_OF_YEAR, -6)

    val days = (0..6).map {
        calendar.time.also { calendar.add(Calendar.DAY_OF_YEAR, 1) }
    }

    val maxActivity = activityData.values.maxOrNull() ?: 1

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            val weekDays = stringArrayResource(R.array.week_days)
            days.forEach { day ->
                val cal = Calendar.getInstance().apply { time = day }
                val dayOfWeek = weekDays[cal.get(Calendar.DAY_OF_WEEK) - 1]
                Text(text = dayOfWeek, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            days.forEach { day ->
                val activity = activityData[getStartOfDay(day)] ?: 0
                val color = getColorForActivity(activity, maxActivity)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(4.dp))
                        .background(color),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = activity.toString(),
                        color = if (activity > 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            days.forEach { day ->
                Text(
                    text = SimpleDateFormat("d", Locale.getDefault()).format(day),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

private fun getStartOfDay(date: Date): Date {
    val calendar = Calendar.getInstance()
    calendar.time = date
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.time
}

@Composable
private fun getColorForActivity(activity: Int, maxActivity: Int): Color {
    val primaryColor = MaterialTheme.colorScheme.primary
    return when {
        activity == 0 -> MaterialTheme.colorScheme.surfaceVariant
        activity <= maxActivity * 0.25 -> primaryColor.copy(alpha = 0.4f)
        activity <= maxActivity * 0.5 -> primaryColor.copy(alpha = 0.6f)
        activity <= maxActivity * 0.75 -> primaryColor.copy(alpha = 0.8f)
        else -> primaryColor
    }
}
