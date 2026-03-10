package com.example.thetest1.presentation.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.thetest1.R
import com.example.thetest1.presentation.util.formatDuration

@Composable
fun ProfileScreen(
    totalSessionTime: Long,
    lessonsCompleted: Int,
    totalLessons: Int,
    userTabsCount: Int
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            imageVector = Icons.Default.Person,
            contentDescription = stringResource(R.string.profile_picture_description),
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(id = R.string.user_name),
            style = androidx.compose.material3.MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(32.dp))
        ProfileStats(totalSessionTime, lessonsCompleted, totalLessons, userTabsCount)
    }
}

@Composable
fun ProfileStats(
    totalSessionTime: Long,
    lessonsCompleted: Int,
    totalLessons: Int,
    userTabsCount: Int
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        StatCard(
            title = stringResource(id = R.string.total_practice_time),
            value = formatDuration(totalSessionTime)
        )
        StatCard(
            title = stringResource(id = R.string.lessons_unlocked),
            value = "$lessonsCompleted/$totalLessons"
        )
        StatCard(title = stringResource(id = R.string.my_tabs), value = userTabsCount.toString())
    }
}

@Composable
fun StatCard(title: String, value: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium
            )
            Text(
                text = value,
                style = androidx.compose.material3.MaterialTheme.typography.bodyLarge
            )
        }
    }
}
