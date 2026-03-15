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
import androidx.compose.material3.Text
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
    val woodColor = Color(0xFF3E2723)
    val fretWireColor = Color(0xFFB0BEC5)
    val stringColor = Color(0xFFE0E0E0)
    val inlayColor = Color(0x66FFFFFF)
    val nutColor = Color(0xFFEEEEEE)
    val instructionsList = analysis?.instructions ?: emptyList()

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
                                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.95f))
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "💡 $instructionText",
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                lineHeight = 14.sp
                            )
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
                    color = Color(0x33000000),
                    topLeft = Offset(fretboardWidth, stringSpacing / 2f),
                    size = Size(rightHandZoneWidth, height - stringSpacing)
                )
                drawLine(
                    color = Color.DarkGray,
                    start = Offset(fretboardWidth, stringSpacing / 2f),
                    end = Offset(fretboardWidth, height - stringSpacing / 2f),
                    strokeWidth = 4f
                )

                val windowRange = animatedEndFret - animatedStartFret
                val pxPerFret = fretboardWidth / windowRange

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
                            drawLine(color = nutColor, start = Offset(fretX + 4f, stringSpacing / 2f), end = Offset(fretX + 4f, height - stringSpacing / 2f), strokeWidth = 12f)
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

                // 4. Draw Strings
                for (i in 0 until totalStrings) {
                    val y = stringSpacing * (i + 1)
                    drawLine(
                        color = stringColor,
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 2f + ((5 - i) * 0.8f)
                    )
                }

                fun getStringY(stringName: String?): Float {
                    val idx = when (stringName) {
                        "E", "E4", "1" -> 5
                        "B", "B3", "2" -> 4
                        "G", "G3", "3" -> 3
                        "D", "D3", "4" -> 2
                        "A", "A2", "5" -> 1
                        "E2", "6" -> 0
                        else -> 0
                    }
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

                // 7. Draw Active Notes
                activeNotes.forEach { (note, dotColor) ->
                    val fretNum = note.fret?.toIntOrNull()
                    val y = getStringY(note.string)

                    if (fretNum != null && fretNum > 0) {
                        val fretX = (fretNum - animatedStartFret - 0.5f) * pxPerFret
                        if (fretX in -50f..(fretboardWidth + 50f)) {
                            drawCircle(color = dotColor, radius = 24f, center = Offset(fretX, y))
                            fingerNumberLayouts[note.finger]?.let { textRes ->
                                drawText(textLayoutResult = textRes, topLeft = Offset(fretX - textRes.size.width / 2f, y - textRes.size.height / 2f))
                            }

                            // Articulations - these are small and rare, keep simple or cache if needed
                            if (note.isHammer) drawText(textMeasurer.measure("H", TextStyle(color = Color(0xFF81C784), fontSize = 12.sp, fontWeight = FontWeight.Bold)), topLeft = Offset(fretX - 10f, y - 36f))
                            if (note.isSlide) drawText(textMeasurer.measure("sl.", TextStyle(color = Color(0xFF64B5F6), fontSize = 12.sp, fontWeight = FontWeight.Bold)), topLeft = Offset(fretX - 15f, y - 36f))
                            if (note.isVibrato) drawText(textMeasurer.measure("~~~", TextStyle(color = Color(0xFFE57373), fontSize = 12.sp, fontWeight = FontWeight.Bold)), topLeft = Offset(fretX + 26f, y - 10f))
                        }
                    } else if (fretNum == 0) {
                        drawCircle(color = dotColor, radius = 16f, center = Offset(20f, y), style = Stroke(width = 6f))
                    } else if (note.fret?.lowercase() == "x") {
                        val fretX = if (targetStartFret > 0) 40f else ((1 - animatedStartFret - 0.5f) * pxPerFret).coerceAtLeast(30f)
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
                    val hintRes = textMeasurer.measure(hint, TextStyle(color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold))
                    val paddingX = 16f
                    val paddingY = 8f
                    val hX = fretboardWidth / 2f - (hintRes.size.width + paddingX * 2) / 2f
                    drawRoundRect(
                        color = Color(0xDD2C2C2C),
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