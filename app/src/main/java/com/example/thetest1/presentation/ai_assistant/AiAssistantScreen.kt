package com.example.thetest1.presentation.ai_assistant

import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.thetest1.R
import com.example.thetest1.data.model.Lesson
import com.example.thetest1.di.ViewModelFactory
import com.example.thetest1.presentation.components.MarkdownView

@Composable
fun AiAssistantScreen(
    lesson: Lesson,
    viewModelFactory: ViewModelFactory,
    asciiTab: String?,
    compactTabs: String?,
    totalMeasures: Int
) {
    val viewModel: AiAssistantViewModel = viewModel(factory = viewModelFactory)
    val uiState by viewModel.uiState.collectAsState()
    var question by remember { mutableStateOf("") }
    
    var isFullContext by remember { mutableStateOf(true) }
    var measureRange by remember { mutableStateOf(1f..totalMeasures.coerceAtLeast(1).toFloat()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            items(uiState.messages) { message ->
                ChatMessageItem(message = message)
            }
        }

        if (uiState.isLoading) {
            PulsingDotsIndicator(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 16.dp)
            )
        }

        @OptIn(ExperimentalMaterial3Api::class)
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val options = listOf("Вся пісня", "Вибрати такти")
                options.forEachIndexed { index, label ->
                    SegmentedButton(
                        selected = if (index == 0) isFullContext else !isFullContext,
                        onClick = { isFullContext = index == 0 },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size)
                    ) {
                        Text(label)
                    }
                }
            }
            if (!isFullContext && totalMeasures > 1) {
                Spacer(modifier = Modifier.height(8.dp))
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Від: ${measureRange.start.toInt()}", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        Text("До: ${measureRange.endInclusive.toInt()}", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    }
                    RangeSlider(
                        value = measureRange,
                        onValueChange = { measureRange = it },
                        valueRange = 1f..totalMeasures.toFloat(),
                        steps = if (totalMeasures > 2) totalMeasures - 2 else 0
                    )
                }
            }
        }

        Card(
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = question,
                    onValueChange = { question = it },
                    placeholder = { Text(stringResource(R.string.ask_question_hint)) },
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                    )
                )
                IconButton(
                    onClick = {
                        val mRange = if (isFullContext) null else measureRange.start.toInt()..measureRange.endInclusive.toInt()
                        
                        val tabsToSend = if (isFullContext || compactTabs == null) {
                            asciiTab ?: lesson.tabsAscii
                        } else {
                            val sliced = compactTabs.split("Measure ")
                                .filter { it.isNotBlank() }
                                .filter {
                                    val idx = it.substringBefore(":").toIntOrNull()
                                    idx != null && idx in mRange!!
                                }
                                .map { "Measure $it" }
                                .joinToString("")
                            if (sliced.isBlank()) asciiTab ?: lesson.tabsAscii else sliced
                        }
                        
                        viewModel.askQuestion(question, lesson.text, tabsToSend, mRange)
                        question = ""
                    },
                    enabled = question.isNotBlank()
                ) {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = stringResource(R.string.send),
                        tint = if (question.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}

@Composable
fun ChatMessageItem(message: ChatMessage) {
    val isUser = message.author == Author.USER
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        Row(verticalAlignment = Alignment.Top) {
            val icon = if (isUser) Icons.Filled.Person else Icons.Filled.SmartToy
            val contentDescription = if (isUser) "User Icon" else "AI Icon"

            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                MarkdownView(markdown = message.text)
            }
        }
    }
}