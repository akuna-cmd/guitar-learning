package com.example.thetest1.presentation.tab_viewer

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.animation.Crossfade
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max
import kotlin.math.min

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

    // Parse active notes
    val activeNotes = analysis?.leftHand?.filter { it.fret != null } ?: emptyList()
    
    // Determine target fret range
    val frets = activeNotes.mapNotNull { it.fret?.toIntOrNull() }.filter { it > 0 }
    
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
        // HUD for full text instructions overlay (Ensuring it stays ABOVE the fretboard)
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
                .height(210.dp) // Fixed height for the fretboard part
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
            val stringSpacing = height / (totalStrings + 1)
            
            // Adjust fretboard wood and strings to leave space on the right for the Right Hand Zone
            val rightHandZoneWidth = 100f // Made wider for text
            val fretboardWidth = width - rightHandZoneWidth
            
            // Draw fretboard wood
            drawRect(
                color = woodColor,
                topLeft = Offset(0f, stringSpacing / 2f),
                size = Size(fretboardWidth, height - stringSpacing)
            )

            // Right Hand Zone Background
            drawRect(
                color = Color(0x33000000),
                topLeft = Offset(fretboardWidth, stringSpacing / 2f),
                size = Size(rightHandZoneWidth, height - stringSpacing)
            )
            // Vertical separator for right hand zone
            drawLine(
                color = Color.DarkGray,
                start = Offset(fretboardWidth, stringSpacing / 2f),
                end = Offset(fretboardWidth, height - stringSpacing / 2f),
                strokeWidth = 4f
            )

            // Calculate fret positioning based on fretboardWidth
            val windowRange = animatedEndFret - animatedStartFret
            val pxPerFret = fretboardWidth / windowRange

            // Draw Inlays (Dots)
            val singleDots = listOf(3, 5, 7, 9, 15, 17, 19, 21)
            val doubleDots = listOf(12, 24)
            for (fret in 1..maxFretsOnGuitar) {
                if (fret > animatedStartFret && fret <= animatedEndFret) {
                    val relativeFretCenter = fret - animatedStartFret - 0.5f
                    val centerX = relativeFretCenter * pxPerFret
                    if (fret in singleDots) {
                        drawCircle(
                            color = inlayColor,
                            radius = 12f,
                            center = Offset(centerX, height / 2f)
                        )
                    } else if (fret in doubleDots) {
                        drawCircle(
                            color = inlayColor,
                            radius = 10f,
                            center = Offset(centerX, stringSpacing * 2.5f)
                        )
                        drawCircle(
                            color = inlayColor,
                            radius = 10f,
                            center = Offset(centerX, stringSpacing * 4.5f)
                        )
                    }
                }
            }

            // Draw Frets (Vertical lines)
            for (i in 0..maxFretsOnGuitar) {
                val fretX = (i - animatedStartFret) * pxPerFret
                if (fretX in 0f..fretboardWidth) {
                    if (i == 0) {
                        // Nut
                        drawLine(
                            color = nutColor,
                            start = Offset(fretX + 4f, stringSpacing / 2f),
                            end = Offset(fretX + 4f, height - stringSpacing / 2f),
                            strokeWidth = 12f
                        )
                    } else {
                        // Normal fret wire
                        drawLine(
                            color = fretWireColor,
                            start = Offset(fretX, stringSpacing / 2f),
                            end = Offset(fretX, height - stringSpacing / 2f),
                            strokeWidth = 4f
                        )
                    }
                    
                    // Draw fret numbers at the bottom
                    if (i > 0 && i % 2 != 0 || i in doubleDots) {
                        val textStr = i.toString()
                        val textRes = textMeasurer.measure(
                            text = textStr,
                            style = TextStyle(color = fretWireColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        )
                        drawText(
                            textLayoutResult = textRes,
                            topLeft = Offset(fretX - pxPerFret / 2f - textRes.size.width / 2f, height - 10f)
                        )
                    }
                }
            }

            // Draw Strings (Horizontal lines)
            // 6th string (E2) is now index 0 at the top, 1st string (E4) is index 5 at the bottom
            for (i in 0 until totalStrings) {
                val y = stringSpacing * (i + 1)
                drawLine(
                    color = stringColor,
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 2f + ((5 - i) * 0.8f) // thicker strings (Low E) at the top (i=0)
                )
            }

            // Helper for string index (String 6/Low E = 0, String 1/High E = 5)
            fun getStringIndex(stringName: String?): Int {
                return when (stringName) {
                    "E", "E4", "1" -> 5 // High E (String 1) bottom
                    "B", "B3", "2" -> 4
                    "G", "G3", "3" -> 3
                    "D", "D3", "4" -> 2
                    "A", "A2", "5" -> 1
                    "E2", "6" -> 0      // Low E (String 6) top
                    else -> 0
                }
            }

            // Draw Barre
            analysis?.barreFret?.let { barreFret ->
                val fretX = (barreFret - animatedStartFret - 0.5f) * pxPerFret
                if (fretX in -50f..(fretboardWidth + 50f)) {
                    val yTop = stringSpacing * 1 - 24f
                    val yBottom = stringSpacing * 6 + 24f
                    drawRoundRect(
                        color = Color(0x554CAF50), // semi-transparent green
                        topLeft = Offset(fretX - 16f, yTop),
                        size = Size(32f, yBottom - yTop),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f, 16f)
                    )
                }
            }

            // Draw Ghost Notes (Next chord preview)
            val ghostNotes = analysis?.nextLeftHand?.filter { it.fret != null && it.fret != "x" } ?: emptyList()
            ghostNotes.forEach { note ->
                val stringIndex = getStringIndex(note.string)
                val fretNum = note.fret?.toIntOrNull()
                if (fretNum != null && fretNum > 0) {
                    val y = stringSpacing * (stringIndex + 1)
                    val fretX = (fretNum - animatedStartFret - 0.5f) * pxPerFret
                    if (fretX in -50f..(fretboardWidth + 50f)) {
                        drawCircle(
                            color = Color.White.copy(alpha = 0.4f),
                            radius = 18f,
                            center = Offset(fretX, y),
                            style = Stroke(width = 4f)
                        )
                    }
                }
            }

            // Draw Active Notes (Fingers)
            activeNotes.forEach { note ->
                val stringIndex = getStringIndex(note.string)
                
                val fretNum = note.fret?.toIntOrNull()
                val y = stringSpacing * (stringIndex + 1)
                
                val dotColor = try {
                    Color(android.graphics.Color.parseColor(note.color))
                } catch (e: Exception) {
                    Color.Red
                }

                if (fretNum != null && fretNum > 0) {
                    // Played on a fret
                    val fretX = (fretNum - animatedStartFret - 0.5f) * pxPerFret
                    if (fretX in -50f..(fretboardWidth + 50f)) {
                        drawCircle(
                            color = dotColor,
                            radius = 24f,
                            center = Offset(fretX, y)
                        )
                        // Draw finger number inside
                        val textRes = textMeasurer.measure(
                            text = note.finger,
                            style = TextStyle(color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        )
                        drawText(
                            textLayoutResult = textRes,
                            topLeft = Offset(fretX - textRes.size.width / 2f, y - textRes.size.height / 2f)
                        )

                        // Draw Articulations
                        if (note.isHammer) {
                            val txt = textMeasurer.measure("H", TextStyle(color = Color(0xFF81C784), fontSize = 12.sp, fontWeight = FontWeight.Bold))
                            drawText(txt, topLeft = Offset(fretX - txt.size.width/2f, y - 36f))
                        }
                        if (note.isSlide) {
                            val txt = textMeasurer.measure("sl.", TextStyle(color = Color(0xFF64B5F6), fontSize = 12.sp, fontWeight = FontWeight.Bold))
                            drawText(txt, topLeft = Offset(fretX - txt.size.width/2f, y - 36f))
                        }
                        if (note.isVibrato) {
                            val txt = textMeasurer.measure("~~~", TextStyle(color = Color(0xFFE57373), fontSize = 12.sp, fontWeight = FontWeight.Bold))
                            drawText(txt, topLeft = Offset(fretX + 26f, y - txt.size.height/2f))
                        }
                    }
                } else if (fretNum == 0) {
                    // Open string
                    val startX = 20f
                    drawCircle(
                        color = dotColor,
                        radius = 16f,
                        center = Offset(startX, y),
                        style = Stroke(width = 6f)
                    )
                } else if (note.fret == "x" || note.fret?.lowercase() == "x") {
                    // Muted string (Dead note)
                    val fretX = if (targetStartFret > 0) 40f else ((1 - animatedStartFret - 0.5f) * pxPerFret).coerceAtLeast(30f)
                    val size = 12f
                    drawLine(
                        color = Color.Gray,
                        start = Offset(fretX - size, y - size),
                        end = Offset(fretX + size, y + size),
                        strokeWidth = 6f,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = Color.Gray,
                        start = Offset(fretX - size, y + size),
                        end = Offset(fretX + size, y - size),
                        strokeWidth = 6f,
                        cap = StrokeCap.Round
                    )
                }
            }

            // Draw Right Hand indicators
            analysis?.rightHand?.forEach { rhNote ->
                val stringIndex = getStringIndex(rhNote.string)
                val y = stringSpacing * (stringIndex + 1)
                val indicatorX = fretboardWidth + rightHandZoneWidth / 2f
                
                // Draw circle background
                drawCircle(
                    color = Color(android.graphics.Color.parseColor(rhNote.color)),
                    radius = 20f,
                    center = Offset(indicatorX, y)
                )
                
                // Draw explicit text: just use the finger letter directly (p, i, m, a)
                val indicatorText = rhNote.finger
                val textRes = textMeasurer.measure(
                    text = indicatorText,
                    style = TextStyle(color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                )
                drawText(
                    textLayoutResult = textRes,
                    topLeft = Offset(indicatorX - textRes.size.width / 2f, y - textRes.size.height / 2f)
                )
            }

            // Draw Contextual Hint Bubble just above the active strings
            analysis?.contextHint?.let { hint ->
                val hintRes = textMeasurer.measure(
                    text = hint,
                    style = TextStyle(color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                )
                val paddingX = 16f
                val paddingY = 8f
                val hintWidth = hintRes.size.width + paddingX * 2
                val hintHeight = hintRes.size.height + paddingY * 2
                
                // Align to top-center of the fretboard
                val hintX = fretboardWidth / 2f - hintWidth / 2f
                val hintY = 16f
                
                drawRoundRect(
                    color = Color(0xDD2C2C2C), // Dark pill background
                    topLeft = Offset(hintX, hintY),
                    size = Size(hintWidth, hintHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(hintHeight/2f, hintHeight/2f)
                )
                drawText(
                    textLayoutResult = hintRes,
                    topLeft = Offset(hintX + paddingX, hintY + paddingY)
                )
            }
        }
    }
    }
}
