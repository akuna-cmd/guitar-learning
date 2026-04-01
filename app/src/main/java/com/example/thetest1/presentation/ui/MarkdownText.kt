package com.example.thetest1.presentation.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

@Composable
fun MarkdownText(markdown: String, modifier: Modifier = Modifier) {
    val annotatedString = buildAnnotatedString {
        val boldRegex = "\\*\\*(.*?)\\*\\*".toRegex()
        var lastIndex = 0

        boldRegex.findAll(markdown).forEach { matchResult ->
            val startIndex = matchResult.range.first
            val endIndex = matchResult.range.last + 1
            val text = matchResult.groupValues[1]

            if (startIndex > lastIndex) {
                append(markdown.substring(lastIndex, startIndex))
            }

            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                append(text)
            }

            lastIndex = endIndex
        }

        if (lastIndex < markdown.length) {
            append(markdown.substring(lastIndex))
        }
    }
    Text(text = annotatedString, modifier = modifier, style = MaterialTheme.typography.bodyLarge)
}
