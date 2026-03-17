package com.example.thetest1.presentation.tab_viewer

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.ui.res.stringResource
import com.example.thetest1.R
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max

@OptIn(ExperimentalTextApi::class)
@Composable
fun GuitarFretboard(
    analysis: TabAnalysis?,
    isPlaying: Boolean = true,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()

    // Configuration
    val totalStrings = 6
    val minFretCount = 5
    val maxFretsOnGuitar = 24

    // Parse active notes and pre-calculate values to avoid work in DrawScope
    val activeNotes = remember(analysis) {
        analysis?.leftHand?.filter { it.fret != null }?.map { note ->
            val color = try {
                Color(android.graphics.Color.parseColor(note.color))
            } catch (e: Exception) {
                Color.Red
            }
            note to color
        } ?: emptyList()
    }
    
    val frets = remember(activeNotes) {
        activeNotes.mapNotNull { it.first.fret?.toIntOrNull() }.filter { it > 0 }
    }
    
    val targetStartFret = if (frets.isEmpty()) 0 else max(0, frets.minOrNull()!! - 1)
    val targetEndFret = max(targetStartFret + minFretCount, (frets.maxOrNull() ?: 0) + 1).coerceAtMost(maxFretsOnGuitar)
    val safeStartFret = if (targetEndFret - targetStartFret < minFretCount) max(0, targetEndFret - minFretCount) else targetStartFret

    // Animate viewport for smooth panning effect
    val animatedStartFret by animateFloatAsState(
        targetValue = safeStartFret.toFloat(),
        animationSpec = spring(stiffness = 100f, dampingRatio = 0.8f),
        label = "StartFret"
    )
    val animatedEndFret by animateFloatAsState(
        targetValue = targetEndFret.toFloat(),
        animationSpec = spring(stiffness = 100f, dampingRatio = 0.8f),
        label = "EndFret"
    )

    // Pre-measure constant text elements
    val fretNumberStyle = TextStyle(color = Color(0xFFB0BEC5), fontSize = 12.sp, fontWeight = FontWeight.Bold)
    val fretNumberLayouts = remember(textMeasurer) {
        (1..maxFretsOnGuitar).associateWith { 
            textMeasurer.measure(it.toString(), fretNumberStyle)
        }
    }

    val fingerNumberStyle = TextStyle(color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    val fingerNumberLayouts = remember(textMeasurer, analysis) {
        val uniqueFingers = analysis?.leftHand?.map { it.finger }?.distinct() ?: emptyList()
        uniqueFingers.associateWith { textMeasurer.measure(it, fingerNumberStyle) }
    }

    val rhFingerStyle = TextStyle(color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    val rhFingerLayouts = remember(textMeasurer, analysis) {
        val uniqueRhFingers = analysis?.rightHand?.map { it.finger }?.distinct() ?: emptyList()
        uniqueRhFingers.associateWith { textMeasurer.measure(it, rhFingerStyle) }
    }

    // Colors
    val woodColor = Color(0xFF2B1D17)
    val rightHandZoneColor = Color(0xFF211A24)
    val fretWireColor = MaterialTheme.colorScheme.outlineVariant
    val stringColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
    val inlayColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
    val nutColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
    val outlineColor = MaterialTheme.colorScheme.outline
    val instructionsList = analysis?.instructions ?: emptyList()
    val hintBackground = MaterialTheme.colorScheme.secondaryContainer
    val hintTextColor = MaterialTheme.colorScheme.onSecondaryContainer
    val hintAccent = MaterialTheme.colorScheme.primary

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Transparent)
            .padding(vertical = 4.dp)
    ) {
        if (instructionsList.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                instructionsList.forEach { instructionText ->
                    if (instructionText.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(hintBackground)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Lightbulb,
                                    contentDescription = stringResource(R.string.hint_icon_desc),
                                    tint = hintAccent
                                    ,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = instructionText,
                                    color = hintTextColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val stringSpacing = height / (totalStrings + 1)
                
                val rightHandZoneWidth = 100f
                val fretboardWidth = width - rightHandZoneWidth
                
                // 1. Draw Background & Wood
                drawRect(
                    color = woodColor,
                    topLeft = Offset(0f, stringSpacing / 2f),
                    size = Size(fretboardWidth, height - stringSpacing)
                )
                drawRect(
                    color = rightHandZoneColor,
                    topLeft = Offset(fretboardWidth, stringSpacing / 2f),
                    size = Size(rightHandZoneWidth, height - stringSpacing)
                )
                drawLine(
                    color = outlineColor,
                    start = Offset(fretboardWidth, stringSpacing / 2f),
                    end = Offset(fretboardWidth, height - stringSpacing / 2f),
                    strokeWidth = 4f
                )

                val windowRange = animatedEndFret - animatedStartFret
                val pxPerFret = fretboardWidth / windowRange
                val nutX = 8f

                // 2. Draw Inlays
                val singleDots = listOf(3, 5, 7, 9, 15, 17, 19, 21)
                val doubleDots = listOf(12, 24)
                for (fret in 1..maxFretsOnGuitar) {
                    if (fret > animatedStartFret && fret <= animatedEndFret) {
                        val relativeFretCenter = fret - animatedStartFret - 0.5f
                        val centerX = relativeFretCenter * pxPerFret
                        if (fret in singleDots) {
                            drawCircle(color = inlayColor, radius = 12f, center = Offset(centerX, height / 2f))
                        } else if (fret in doubleDots) {
                            drawCircle(color = inlayColor, radius = 10f, center = Offset(centerX, stringSpacing * 2.5f))
                            drawCircle(color = inlayColor, radius = 10f, center = Offset(centerX, stringSpacing * 4.5f))
                        }
                    }
                }

                // 3. Draw Frets & Numbers
                for (i in 0..maxFretsOnGuitar) {
                    val fretX = (i - animatedStartFret) * pxPerFret
                    if (fretX in 0f..fretboardWidth) {
                        if (i == 0) {
                            drawLine(color = nutColor, start = Offset(fretX + nutX, stringSpacing / 2f), end = Offset(fretX + nutX, height - stringSpacing / 2f), strokeWidth = 12f)
                        } else {
                            drawLine(color = fretWireColor, start = Offset(fretX, stringSpacing / 2f), end = Offset(fretX, height - stringSpacing / 2f), strokeWidth = 4f)
                        }
                        
                        if (i > 0 && (i % 2 != 0 || i in doubleDots)) {
                            fretNumberLayouts[i]?.let { textRes ->
                                drawText(
                                    textLayoutResult = textRes,
                                    topLeft = Offset(fretX - pxPerFret / 2f - textRes.size.width / 2f, height - 10f)
                                )
                            }
                        }
                    }
                }

                val stringOrder = listOf("e", "b", "g", "d", "A", "E")
                for (i in 0 until totalStrings) {
                    val y = stringSpacing * (i + 1)
                    drawLine(
                        color = stringColor,
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 2f + (i * 0.9f)
                    )
                }

                fun getStringIndex(stringName: String?): Int {
                    if (stringName == "E") return 5
                    val normalized = stringName?.lowercase()
                    return when (normalized) {
                        "e", "1" -> 5
                        "b", "2" -> 4
                        "g", "3" -> 3
                        "d", "4" -> 2
                        "a", "5" -> 1
                        "e2", "6" -> 0
                        else -> 0
                    }
                }

                fun getStringY(stringName: String?): Float {
                    val idx = getStringIndex(stringName)
                    return stringSpacing * (idx + 1)
                }

                // 5. Draw Barre
                analysis?.barreFret?.let { barreFret ->
                    val fretX = (barreFret - animatedStartFret - 0.5f) * pxPerFret
                    if (fretX in -50f..(fretboardWidth + 50f)) {
                        drawRoundRect(
                            color = Color(0x554CAF50),
                            topLeft = Offset(fretX - 16f, stringSpacing * 1 - 24f),
                            size = Size(32f, stringSpacing * 5 + 48f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f, 16f)
                        )
                    }
                }

                // 6. Draw Ghost Notes
                analysis?.nextLeftHand?.forEach { note ->
                    if (note.fret != null && note.fret != "x") {
                        val fretNum = note.fret.toIntOrNull()
                        if (fretNum != null && fretNum > 0) {
                            val y = getStringY(note.string)
                            val fretX = (fretNum - animatedStartFret - 0.5f) * pxPerFret
                            if (fretX in -50f..(fretboardWidth + 50f)) {
                                drawCircle(color = Color.White.copy(alpha = 0.4f), radius = 18f, center = Offset(fretX, y), style = Stroke(width = 4f))
                            }
                        }
                    }
                }

                val fretLabelStyle = TextStyle(color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                val openTextStyle = TextStyle(color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)

                // 7. Draw Active Notes
                activeNotes.forEach { (note, dotColor) ->
                    val fretNum = note.fret?.toIntOrNull()
                    val stringIndex = getStringIndex(note.string)
                    val y = getStringY(note.string)

                    if (fretNum != null && fretNum > 0) {
                        val fretX = (fretNum - animatedStartFret - 0.5f) * pxPerFret
                        if (fretX in -50f..(fretboardWidth + 50f)) {
                            drawCircle(color = dotColor, radius = 24f, center = Offset(fretX, y))
                            drawCircle(color = Color.Black.copy(alpha = 0.6f), radius = 24f, center = Offset(fretX, y), style = Stroke(width = 2f))
                            val fingerLabel = note.finger
                            val fingerText = textMeasurer.measure(fingerLabel, fingerNumberStyle)
                            drawText(
                                textLayoutResult = fingerText,
                                topLeft = Offset(fretX - fingerText.size.width / 2f, y - fingerText.size.height / 2f)
                            )
                            val fretLabel = fretNum.toString()
                            val fretText = textMeasurer.measure(fretLabel, fretLabelStyle)
                            val badgeX = fretX + 18f
                            drawRoundRect(
                                color = Color.White,
                                topLeft = Offset(badgeX - fretText.size.width / 2f - 4f, y - 34f),
                                size = Size(fretText.size.width + 8f, fretText.size.height + 4f),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
                            )
                            drawText(
                                textLayoutResult = fretText,
                                topLeft = Offset(badgeX - fretText.size.width / 2f, y - 32f)
                            )

                            // Articulations - these are small and rare, keep simple or cache if needed
                            if (note.isHammer) drawText(textMeasurer.measure("H", TextStyle(color = Color(0xFF81C784), fontSize = 12.sp, fontWeight = FontWeight.Bold)), topLeft = Offset(fretX - 10f, y - 36f))
                            if (note.isSlide) drawText(textMeasurer.measure("sl.", TextStyle(color = Color(0xFF64B5F6), fontSize = 12.sp, fontWeight = FontWeight.Bold)), topLeft = Offset(fretX - 15f, y - 36f))
                            if (note.isVibrato) drawText(textMeasurer.measure("~~~", TextStyle(color = Color(0xFFE57373), fontSize = 12.sp, fontWeight = FontWeight.Bold)), topLeft = Offset(fretX + 26f, y - 10f))
                        }
                    } else if (fretNum == 0) {
                        val openX = nutX + 10f
                        drawCircle(color = Color.White, radius = 16f, center = Offset(openX, y))
                        drawCircle(color = dotColor, radius = 16f, center = Offset(openX, y), style = Stroke(width = 4f))
                        drawCircle(color = Color.Black.copy(alpha = 0.7f), radius = 16f, center = Offset(openX, y), style = Stroke(width = 1.5f))
                        val openText = textMeasurer.measure("0", openTextStyle)
                        drawText(textLayoutResult = openText, topLeft = Offset(openX - openText.size.width / 2f, y - openText.size.height / 2f))
                    } else if (note.fret?.lowercase() == "x") {
                        val fretX = nutX + 22f
                        drawLine(color = Color.Gray, start = Offset(fretX - 12f, y - 12f), end = Offset(fretX + 12f, y + 12f), strokeWidth = 6f, cap = StrokeCap.Round)
                        drawLine(color = Color.Gray, start = Offset(fretX - 12f, y + 12f), end = Offset(fretX + 12f, y - 12f), strokeWidth = 6f, cap = StrokeCap.Round)
                    }
                }

                // 8. Draw Right Hand
                analysis?.rightHand?.forEach { rhNote ->
                    val y = getStringY(rhNote.string)
                    val indicatorX = fretboardWidth + rightHandZoneWidth / 2f
                    val color = try { Color(android.graphics.Color.parseColor(rhNote.color)) } catch (e: Exception) { Color.Gray }
                    
                    drawCircle(color = color, radius = 20f, center = Offset(indicatorX, y))
                    rhFingerLayouts[rhNote.finger]?.let { textRes ->
                        drawText(textLayoutResult = textRes, topLeft = Offset(indicatorX - textRes.size.width / 2f, y - textRes.size.height / 2f))
                    }
                }

                // 9. Contextual Hint
                analysis?.contextHint?.let { hint ->
                    val hintRes = textMeasurer.measure(hint, TextStyle(color = hintTextColor, fontSize = 14.sp, fontWeight = FontWeight.Bold))
                    val paddingX = 16f
                    val paddingY = 8f
                    val hX = fretboardWidth / 2f - (hintRes.size.width + paddingX * 2) / 2f
                    drawRoundRect(
                        color = hintBackground.copy(alpha = 0.95f),
                        topLeft = Offset(hX, 16f),
                        size = Size(hintRes.size.width + paddingX * 2, hintRes.size.height + paddingY * 2),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(20f, 20f)
                    )
                    drawText(hintRes, topLeft = Offset(hX + paddingX, 16f + paddingY))
                }
            }
        }
    }
}
