package com.example.thetest1.presentation.tab_viewer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max

private data class FretNote(
    val stringIndex: Int,
    val fret: Int?,
    val isMuted: Boolean,
    val finger: String,
    val color: Color
)

@OptIn(ExperimentalTextApi::class)
@Composable
fun GuitarFretboard(
    analysis: TabAnalysis?,
    isPlaying: Boolean = true,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val scheme = MaterialTheme.colorScheme
    val allInstructions = analysis?.instructions?.filter { it.isNotBlank() } ?: emptyList()

    val notes = remember(analysis) {
        (analysis?.leftHand ?: emptyList()).mapNotNull { note ->
            val stringIndex = (note.stringIndex?.let { it - 1 } ?: stringToIndex(note.string))
                ?.coerceIn(0, 5) ?: return@mapNotNull null
            val rawFret = note.fret?.trim()
            val isMuted = rawFret.equals("x", ignoreCase = true)
            val fretInt = rawFret?.toIntOrNull()
            val dotColor = runCatching {
                Color(android.graphics.Color.parseColor(note.color))
            }.getOrElse { scheme.primary }
            FretNote(
                stringIndex = stringIndex,
                fret = fretInt,
                isMuted = isMuted,
                finger = note.finger.ifBlank { "?" },
                color = dotColor
            )
        }
    }

    val fretted = notes.mapNotNull { it.fret }.filter { it > 0 }
    val startFret = if (fretted.isEmpty()) 0 else max(0, (fretted.minOrNull() ?: 1) - 1)
    val endFret = max(startFret + 5, (fretted.maxOrNull() ?: 0) + 1)
    val hintBackground = scheme.secondaryContainer
    val hintTextColor = scheme.onSecondaryContainer
    val hintAccent = scheme.primary

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .background(scheme.surfaceContainerHigh, RoundedCornerShape(14.dp))
                .border(1.dp, scheme.outlineVariant, RoundedCornerShape(14.dp))
                .padding(8.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val left = 66f
                val right = w - 12f
                val top = 18f
                val bottom = h - 18f
                val stringCount = 6
                val fretCount = (endFret - startFret).coerceAtLeast(1)
                val fretStep = (right - left) / fretCount
                val lineColor = scheme.onSurface.copy(alpha = 0.38f)
                val fretColor = scheme.outline
                val boardTop = top - 6f
                val boardBottom = bottom + 6f
                val nutX = left + 2f

                drawRoundRect(
                    color = Color(0xFF2B1D17),
                    topLeft = Offset(left - 8f, boardTop),
                    size = Size((right - left) + 16f, boardBottom - boardTop),
                    cornerRadius = CornerRadius(12f, 12f)
                )

                fun yForString(index: Int): Float {
                    return top + (bottom - top) * (index / (stringCount - 1).toFloat())
                }

                // Strings: 1st string is TOP, 6th is BOTTOM.
                repeat(stringCount) { i ->
                    val y = yForString(i)
                    val stroke = 1.6f + (i * 0.5f)
                    drawLine(
                        color = lineColor,
                        start = Offset(left, y),
                        end = Offset(right, y),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )
                }

                for (fret in startFret..endFret) {
                    val x = left + (fret - startFret) * fretStep
                    val width = if (fret == 0) 5f else 1.8f
                    drawLine(
                        color = if (fret == 0) scheme.onSurface else fretColor,
                        start = Offset(x, top),
                        end = Offset(x, bottom),
                        strokeWidth = width
                    )
                    if (fret > 0 && fret <= endFret) {
                        val txt = textMeasurer.measure(
                            text = fret.toString(),
                            style = TextStyle(
                                color = scheme.onSurfaceVariant,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        drawText(txt, topLeft = Offset(x + 4f, top - txt.size.height - 2f))
                    }
                }

                val stringLabels = listOf("1 e", "2 b", "3 g", "4 d", "5 A", "6 E")
                stringLabels.forEachIndexed { i, label ->
                    val labelText = textMeasurer.measure(
                        text = label,
                        style = TextStyle(
                            color = scheme.onPrimaryContainer,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    val labelY = yForString(i) - labelText.size.height / 2f
                    drawRoundRect(
                        color = scheme.primaryContainer.copy(alpha = 0.92f),
                        topLeft = Offset(6f, labelY - 1f),
                        size = Size(48f, labelText.size.height + 2f),
                        cornerRadius = CornerRadius(8f, 8f)
                    )
                    drawText(labelText, topLeft = Offset(10f, yForString(i) - labelText.size.height / 2f))
                }

                notes.forEach { note ->
                    val y = yForString(note.stringIndex)
                    if (note.isMuted) {
                        val x = nutX + 8f
                        drawLine(
                            color = scheme.onSurfaceVariant,
                            start = Offset(x - 6f, y - 6f),
                            end = Offset(x + 6f, y + 6f),
                            strokeWidth = 2.4f,
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            color = scheme.onSurfaceVariant,
                            start = Offset(x - 6f, y + 6f),
                            end = Offset(x + 6f, y - 6f),
                            strokeWidth = 2.4f,
                            cap = StrokeCap.Round
                        )
                        return@forEach
                    }

                    val fret = note.fret ?: return@forEach
                    if (fret == 0) {
                        val x = nutX + 10f
                        drawCircle(
                            color = scheme.surface,
                            radius = 11f,
                            center = Offset(x, y),
                            style = Stroke(width = 2.6f)
                        )
                        val txt = textMeasurer.measure(
                            text = "0",
                            style = TextStyle(color = scheme.onSurface, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        )
                        drawText(txt, topLeft = Offset(x - txt.size.width / 2f, y - txt.size.height / 2f))
                        return@forEach
                    }

                    if (fret < startFret || fret > endFret) return@forEach
                    val x = left + (fret - startFret + 0.5f) * fretStep
                    drawCircle(
                        color = note.color,
                        radius = 15f,
                        center = Offset(x, y)
                    )
                    drawCircle(
                        color = Color.Black.copy(alpha = 0.28f),
                        radius = 15f,
                        center = Offset(x, y),
                        style = Stroke(width = 1.8f)
                    )
                    val txt = textMeasurer.measure(
                        text = note.finger,
                        style = TextStyle(color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                    )
                    drawText(txt, topLeft = Offset(x - txt.size.width / 2f, y - txt.size.height / 2f))

                    val fretText = textMeasurer.measure(
                        text = "L$fret",
                        style = TextStyle(color = scheme.onPrimaryContainer, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    )
                    val badgeX = x + 13f
                    val badgeY = y - 20f
                    drawRoundRect(
                        color = scheme.primaryContainer,
                        topLeft = Offset(badgeX - 2f, badgeY - 1f),
                        size = Size(fretText.size.width + 6f, fretText.size.height + 4f),
                        cornerRadius = CornerRadius(6f, 6f)
                    )
                    drawText(fretText, topLeft = Offset(badgeX + 1f, badgeY + 1f))
                }
            }
        }

        if (allInstructions.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 190.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                userScrollEnabled = true
            ) {
                items(allInstructions) { text ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(hintBackground, RoundedCornerShape(10.dp))
                            .border(1.dp, scheme.outlineVariant, RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Lightbulb,
                                contentDescription = null,
                                tint = hintAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = text,
                                color = hintTextColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun stringToIndex(raw: String): Int? {
    val s = raw.trim()
    val numeric = s.toIntOrNull()
    if (numeric != null && numeric in 1..6) return numeric - 1
    val inlineIndex = Regex("""\(([1-6])\)""").find(s)?.groupValues?.getOrNull(1)?.toIntOrNull()
    if (inlineIndex != null) return inlineIndex - 1
    val n = s.lowercase()
    return when {
        s == "E" || n == "low e" || n == "e6" || n == "ebass" -> 5
        n == "e" -> 0
        n == "b" -> 1
        n == "g" -> 2
        n == "d" -> 3
        n == "a" -> 4
        else -> null
    }
}



