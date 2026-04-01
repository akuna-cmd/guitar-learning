package com.example.thetest1.presentation.ui

import android.util.TypedValue
import android.widget.TextView
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import io.noties.markwon.Markwon

@Composable
fun MarkdownView(markdown: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val markwon = Markwon.create(context)
    val textColor = LocalContentColor.current.toArgb()

    AndroidView(
        factory = { ctx -> TextView(ctx) },
        update = { view ->
            markwon.setMarkdown(view, markdown)
            view.setTextColor(textColor)
            view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        },
        modifier = modifier
    )
}
