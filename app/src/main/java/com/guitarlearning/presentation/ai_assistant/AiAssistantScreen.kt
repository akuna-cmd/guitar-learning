package com.guitarlearning.presentation.ai_assistant

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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.AssistChip
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.guitarlearning.R
import com.guitarlearning.domain.model.Lesson
import com.guitarlearning.presentation.ui.MarkdownView
import com.guitarlearning.presentation.ui.asString
import com.guitarlearning.presentation.ui.theme.appBlockBorder

private const val MeasurePrefix = "Measure "

private fun sliceCompactTabs(compactTabs: String, selectedRange: IntRange?): String {
    if (selectedRange == null) return compactTabs
    return compactTabs.split(MeasurePrefix)
        .filter { it.isNotBlank() }
        .filter {
            val idx = it.substringBefore(":").toIntOrNull()
            idx != null && idx in selectedRange
        }
        .map { "$MeasurePrefix$it" }
        .joinToString("")
}

private fun buildAiTabsContext(
    asciiTab: String?,
    fallbackAscii: String?,
    compactTabs: String?,
    selectedRange: IntRange?,
    isFullContext: Boolean
): String {
    val rawAscii = asciiTab ?: fallbackAscii
    val compactSlice = compactTabs?.let {
        if (isFullContext) it else sliceCompactTabs(it, selectedRange)
    }?.trim().orEmpty()

    val sections = mutableListOf<String>()
    if (compactSlice.isNotBlank()) {
        sections += "Structured measure context:\n$compactSlice"
    }
    if (rawAscii != null && (compactSlice.isBlank() || isFullContext)) {
        sections += "Raw ASCII tab:\n$rawAscii"
    }

    return sections.joinToString("\n\n").ifBlank { rawAscii.orEmpty() }
}

@Composable
fun AiAssistantScreen(
    lesson: Lesson,
    asciiTab: String?,
    compactTabs: String?,
    isAnalysisLoading: Boolean,
    totalMeasures: Int,
    initialMeasureRange: IntRange? = null
) {
    val viewModel: AiAssistantViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()
    var question by remember { mutableStateOf("") }

    val defaultMeasureRange = remember(initialMeasureRange, totalMeasures) {
        val safeTotalMeasures = totalMeasures.coerceAtLeast(1)
        val range = initialMeasureRange
        if (range != null) {
            range.first.coerceIn(1, safeTotalMeasures)..range.last.coerceIn(1, safeTotalMeasures)
        } else {
            1..1
        }
    }

    var isFullContext by remember(defaultMeasureRange, totalMeasures) {
        mutableStateOf(false)
    }
    var measureRange by remember(defaultMeasureRange, totalMeasures) {
        mutableStateOf(
            defaultMeasureRange.first.toFloat()..defaultMeasureRange.last.toFloat()
        )
    }
    var showExampleQuestions by rememberSaveable { mutableStateOf(true) }
    val exampleQuestions = listOf(
        stringResource(R.string.ai_example_question_measure),
        stringResource(R.string.ai_example_question_mistakes),
        stringResource(R.string.ai_example_question_difficult_parts)
    )

    fun submitQuestion(rawQuestion: String) {
        val trimmedQuestion = rawQuestion.trim()
        if (trimmedQuestion.isBlank()) return

        val selectedRange = if (isFullContext) {
            null
        } else {
            measureRange.start.toInt()..measureRange.endInclusive.toInt()
        }

        val tabsToSend = buildAiTabsContext(
            asciiTab = asciiTab,
            fallbackAscii = lesson.tabsAscii,
            compactTabs = compactTabs,
            selectedRange = selectedRange,
            isFullContext = isFullContext
        )

        viewModel.askQuestion(
            question = trimmedQuestion,
            theory = lesson.text,
            tabs = tabsToSend,
            measureRange = selectedRange
        )
        showExampleQuestions = false
        question = ""
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (isAnalysisLoading) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                border = appBlockBorder()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.ai_analysis_pending_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(R.string.ai_analysis_pending_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            items(uiState.messages, key = { it.id }) { message ->
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = appBlockBorder()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = stringResource(R.string.ai_context_section_title),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        val options = listOf(R.string.ai_full_context, R.string.ai_select_measures)
                        options.forEachIndexed { index, labelRes ->
                            SegmentedButton(
                                selected = if (index == 0) isFullContext else !isFullContext,
                                onClick = { isFullContext = index == 0 },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size)
                            ) {
                                Text(stringResource(labelRes))
                            }
                        }
                    }

                    if (isFullContext) {
                        Text(
                            text = stringResource(R.string.ai_full_context_warning),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }

                    if (!isFullContext && totalMeasures > 1) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    stringResource(R.string.measure_from, measureRange.start.toInt()),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                )
                                Text(
                                    stringResource(R.string.measure_to, measureRange.endInclusive.toInt()),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                )
                            }
                            RangeSlider(
                                value = measureRange,
                                onValueChange = { measureRange = it },
                                valueRange = 1f..totalMeasures.toFloat(),
                                steps = if (totalMeasures > 2) totalMeasures - 2 else 0
                            )
                        }
                    } else if (!isFullContext) {
                        Text(
                            text = stringResource(R.string.ai_single_measure_only),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (showExampleQuestions) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = appBlockBorder()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.ai_example_questions_title),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(R.string.ai_example_questions_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        ExampleQuestionChips(
                            questions = exampleQuestions,
                            onQuestionClick = { submitQuestion(it) }
                        )
                    }
                }
            }
        }

        Card(
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(4.dp),
            border = appBlockBorder(),
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
                        submitQuestion(question)
                    },
                    enabled = question.isNotBlank()
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
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
            val contentDescription = if (isUser) {
                stringResource(R.string.user_icon_description)
            } else {
                stringResource(R.string.ai_icon_description)
            }

            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                if (!isUser && message.sourceLabel != null) {
                    Text(
                        text = message.sourceLabel.asString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                MarkdownView(markdown = message.text.asString())
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExampleQuestionChips(
    questions: List<String>,
    onQuestionClick: (String) -> Unit
) {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        questions.forEach { question ->
            AssistChip(
                onClick = { onQuestionClick(question) },
                shape = RoundedCornerShape(14.dp),
                label = {
                    Text(
                        text = question,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            )
        }
    }
}
