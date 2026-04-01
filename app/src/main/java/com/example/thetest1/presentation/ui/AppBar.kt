package com.example.thetest1.presentation.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.thetest1.R
import com.example.thetest1.presentation.ui.formatDuration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBar(sessionDuration: Long, onStopSession: () -> Unit) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(formatDuration(sessionDuration))
                Spacer(modifier = Modifier.width(16.dp))
                Button(onClick = onStopSession) { Text(stringResource(id = R.string.stop_session)) }
            }
        }
    )
}
