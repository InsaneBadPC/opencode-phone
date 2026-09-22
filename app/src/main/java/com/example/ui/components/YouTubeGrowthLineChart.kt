package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlin.math.roundToInt

/**
 * High-fidelity Recharts/D3 styled line graph component for the YouTube Agent dashboard.
 * Visualizes Subscribers and View Count trends over the last 30 days with:
 * - Smooth cubic Bézier spline lines
 * - Semi-transparent gradient area fills
 * - Interactive scrubber / crosshair tooltip with touch detection
 * - Metric toggles (Both, Views, Subscribers)
 * - Video release milestone markers
 */

data class DayTrendData(
    val dayNumber: Int,
    val dayLabel: String,
    val views: Int,
    val totalSubscribers: Int,
    val subGain: Int,
    val videoPublishedTitle: String? = null
)

enum class ChartMetricMode(val label: String) {
    COMBINED("Kombinovaný graf"),
    VIEWS("Pouze zhlédnutí"),
    SUBSCRIBERS("Pouze odběratelé")
}

@Composable
fun YouTubeGrowthLineChart(
    modifier: Modifier = Modifier
) {
    var selectedMetricMode by remember { mutableStateOf(ChartMetricMode.COMBINED) }
    var touchedIndex by remember { mutableStateOf<Int?>(29) } // Default to last day (Day 30)

    val youtubeRed = Color(0xFFFF0033)
    val subsColor = CyanBright
    val gridColor = Slate800.copy(alpha = 0.7f)

    // 30 days dataset with realistic algorithm growth spikes on Thursdays and Sundays
    val trendData = remember {
        generate30DaysGrowthData()
    }

    val selectedDay = touchedIndex?.let { trendData.getOrNull(it) } ?: trendData.last()

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Slate950),
        border = BorderStroke(1.dp, Slate800),
        modifier = modifier
            .fillMaxWidth()
            .testTag("youtube_growth_line_chart")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header with title and Recharts-style aesthetic badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(youtubeRed.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShowChart,
                            contentDescription = null,
                            tint = youtubeRed,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Trend zhlédnutí & odběratelů (30 dní)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Slate100
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Slate800
                            ) {
                                Text(
                                    text = "D3 / Recharts Engine",
                                    fontSize = 8.sp,
                                    color = CyanBright,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(
                            text = "Interaktivní časová osa s detekcí algoritmických špiček",
                            fontSize = 10.sp,
                            color = Slate400
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Metric Toggle Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ChartMetricMode.values().forEach { mode ->
                    FilterChip(
                        selected = selectedMetricMode == mode,
                        onClick = { selectedMetricMode = mode },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val indicatorColor = when (mode) {
                                    ChartMetricMode.COMBINED -> AmberWarning
                                    ChartMetricMode.VIEWS -> youtubeRed
                                    ChartMetricMode.SUBSCRIBERS -> subsColor
                                }
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(indicatorColor)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(mode.label, fontSize = 10.sp)
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Slate800,
                            selectedLabelColor = Color.White,
                            containerColor = Slate900,
                            labelColor = Slate400
                        ),
                        border = if (selectedMetricMode == mode) BorderStroke(1.dp, Slate700) else null
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Interactive Tooltip Card (Recharts Scrubber Inspector)
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Slate900,
                border = BorderStroke(1.dp, Slate800),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = selectedDay.dayLabel,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate100
                            )
                            if (selectedDay.videoPublishedTitle != null) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = youtubeRed.copy(alpha = 0.2f),
                                    border = BorderStroke(0.5.dp, youtubeRed)
                                ) {
                                    Text(
                                        text = "🎬 Video vydáno",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFF4D4D),
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }

                        if (selectedDay.videoPublishedTitle != null) {
                            Text(
                                text = "„${selectedDay.videoPublishedTitle}“",
                                fontSize = 9.sp,
                                color = Slate400,
                                maxLines = 1
                            )
                        } else {
                            Text(
                                text = "Dotkněte se grafu pro detail konkrétního dne",
                                fontSize = 9.sp,
                                color = Slate500
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (selectedMetricMode != ChartMetricMode.SUBSCRIBERS) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${selectedDay.views} zhlédnutí",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = youtubeRed,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text("Denní sledovanost", fontSize = 8.sp, color = Slate400)
                            }
                        }

                        if (selectedMetricMode != ChartMetricMode.VIEWS) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${selectedDay.totalSubscribers} (+${selectedDay.subGain})",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = subsColor,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text("Stav odběratelů", fontSize = 8.sp, color = Slate400)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // THE NATIVE COMPOSE CANVAS D3/RECHARTS GRAPH
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                val pointSpacing = size.width / (trendData.size - 1).toFloat()
                                val rawIndex = (offset.x / pointSpacing).roundToInt()
                                touchedIndex = rawIndex.coerceIn(0, trendData.size - 1)
                            }
                        }
                        .pointerInput(Unit) {
                            detectDragGestures { change, _ ->
                                change.consume()
                                val pointSpacing = size.width / (trendData.size - 1).toFloat()
                                val rawIndex = (change.position.x / pointSpacing).roundToInt()
                                touchedIndex = rawIndex.coerceIn(0, trendData.size - 1)
                            }
                        }
                ) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height

                    // 1. Draw horizontal gridlines (Recharts CartisianGrid style)
                    val gridLines = 4
                    for (i in 0..gridLines) {
                        val y = (canvasHeight / gridLines) * i
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, y),
                            end = Offset(canvasWidth, y),
                            strokeWidth = 1f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                        )
                    }

                    val maxViews = trendData.maxOf { it.views }.toFloat() * 1.15f
                    val minViews = trendData.minOf { it.views }.toFloat() * 0.85f
                    val viewsRange = (maxViews - minViews).coerceAtLeast(1f)

                    val maxSubs = trendData.maxOf { it.totalSubscribers }.toFloat() * 1.02f
                    val minSubs = trendData.minOf { it.totalSubscribers }.toFloat() * 0.98f
                    val subsRange = (maxSubs - minSubs).coerceAtLeast(1f)

                    val stepX = canvasWidth / (trendData.size - 1).toFloat()

                    // Compute points for Views
                    val viewsOffsets = trendData.mapIndexed { index, data ->
                        val x = index * stepX
                        val normalizedY = (data.views - minViews) / viewsRange
                        val y = canvasHeight - (normalizedY * (canvasHeight - 30f)) - 10f
                        Offset(x, y)
                    }

                    // Compute points for Subscribers
                    val subsOffsets = trendData.mapIndexed { index, data ->
                        val x = index * stepX
                        val normalizedY = (data.totalSubscribers - minSubs) / subsRange
                        val y = canvasHeight - (normalizedY * (canvasHeight - 30f)) - 10f
                        Offset(x, y)
                    }

                    // DRAW VIEWS AREA + CURVE
                    if (selectedMetricMode != ChartMetricMode.SUBSCRIBERS) {
                        drawSmoothCurve(
                            offsets = viewsOffsets,
                            lineColor = youtubeRed,
                            fillBrush = Brush.verticalGradient(
                                colors = listOf(youtubeRed.copy(alpha = 0.35f), Color.Transparent),
                                startY = 0f,
                                endY = canvasHeight
                            ),
                            canvasHeight = canvasHeight
                        )
                    }

                    // DRAW SUBSCRIBERS AREA + CURVE
                    if (selectedMetricMode != ChartMetricMode.VIEWS) {
                        drawSmoothCurve(
                            offsets = subsOffsets,
                            lineColor = subsColor,
                            fillBrush = Brush.verticalGradient(
                                colors = listOf(subsColor.copy(alpha = 0.25f), Color.Transparent),
                                startY = 0f,
                                endY = canvasHeight
                            ),
                            canvasHeight = canvasHeight
                        )
                    }

                    // DRAW PUBLISH MILESTONE ICONS (Dots with outer glow on publish days)
                    trendData.forEachIndexed { index, data ->
                        if (data.videoPublishedTitle != null) {
                            val targetOffset = if (selectedMetricMode == ChartMetricMode.SUBSCRIBERS) subsOffsets[index] else viewsOffsets[index]
                            drawCircle(
                                color = youtubeRed.copy(alpha = 0.4f),
                                radius = 9f,
                                center = targetOffset
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 4f,
                                center = targetOffset
                            )
                        }
                    }

                    // DRAW INTERACTIVE TOUCH SCRUBBER / CROSSHAIR
                    touchedIndex?.let { index ->
                        if (index in trendData.indices) {
                            val activeX = index * stepX

                            // Vertical crosshair line
                            drawLine(
                                color = Slate400.copy(alpha = 0.6f),
                                start = Offset(activeX, 0f),
                                end = Offset(activeX, canvasHeight),
                                strokeWidth = 1.5f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
                            )

                            // Views Dot
                            if (selectedMetricMode != ChartMetricMode.SUBSCRIBERS) {
                                val vOffset = viewsOffsets[index]
                                drawCircle(
                                    color = youtubeRed.copy(alpha = 0.3f),
                                    radius = 12f,
                                    center = vOffset
                                )
                                drawCircle(
                                    color = youtubeRed,
                                    radius = 5f,
                                    center = vOffset
                                )
                                drawCircle(
                                    color = Color.White,
                                    radius = 2.5f,
                                    center = vOffset
                                )
                            }

                            // Subs Dot
                            if (selectedMetricMode != ChartMetricMode.VIEWS) {
                                val sOffset = subsOffsets[index]
                                drawCircle(
                                    color = subsColor.copy(alpha = 0.3f),
                                    radius = 12f,
                                    center = sOffset
                                )
                                drawCircle(
                                    color = subsColor,
                                    radius = 5f,
                                    center = sOffset
                                )
                                drawCircle(
                                    color = Color.White,
                                    radius = 2.5f,
                                    center = sOffset
                                )
                            }
                        }
                    }
                }
            }

            // X-AXIS TIMELINE MILESTONES (Recharts XAxis labels)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Den 1", fontSize = 9.sp, color = Slate500)
                Text("Den 7", fontSize = 9.sp, color = Slate500)
                Text("Den 14", fontSize = 9.sp, color = Slate500)
                Text("Den 21", fontSize = 9.sp, color = Slate500)
                Text("Dnes (Den 30)", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Slate300)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // LEGEND & PERFORMANCE SUMMARY
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    LegendItem(color = youtubeRed, label = "Zhlédnutí (Views)")
                    LegendItem(color = subsColor, label = "Odběratelé (Subs)")
                    LegendItem(color = Color.White, label = "Vydaná videa")
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.TrendingUp, contentDescription = null, tint = EmeraldBright, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "+29.4 % 30d",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldBright
                    )
                }
            }
        }
    }
}

// Draw smooth Cubic Bézier line with area fill
private fun DrawScope.drawSmoothCurve(
    offsets: List<Offset>,
    lineColor: Color,
    fillBrush: Brush,
    canvasHeight: Float
) {
    if (offsets.size < 2) return

    val strokePath = Path()
    val fillPath = Path()

    strokePath.moveTo(offsets[0].x, offsets[0].y)
    fillPath.moveTo(offsets[0].x, canvasHeight)
    fillPath.lineTo(offsets[0].x, offsets[0].y)

    for (i in 0 until offsets.size - 1) {
        val p0 = offsets[i]
        val p1 = offsets[i + 1]

        val controlPoint1 = Offset(p0.x + (p1.x - p0.x) / 2f, p0.y)
        val controlPoint2 = Offset(p0.x + (p1.x - p0.x) / 2f, p1.y)

        strokePath.cubicTo(
            controlPoint1.x, controlPoint1.y,
            controlPoint2.x, controlPoint2.y,
            p1.x, p1.y
        )

        fillPath.cubicTo(
            controlPoint1.x, controlPoint1.y,
            controlPoint2.x, controlPoint2.y,
            p1.x, p1.y
        )
    }

    fillPath.lineTo(offsets.last().x, canvasHeight)
    fillPath.close()

    // Draw gradient area below curve
    drawPath(path = fillPath, brush = fillBrush)

    // Draw main spline line
    drawPath(
        path = strokePath,
        color = lineColor,
        style = Stroke(
            width = 2.5f,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, fontSize = 9.sp, color = Slate400)
    }
}

// Helper to generate 30 days of data with realistic peaks on Thursdays (Day 3, 10, 17, 24) and Sundays (Day 6, 13, 20, 27)
private fun generate30DaysGrowthData(): List<DayTrendData> {
    val list = mutableListOf<DayTrendData>()
    var currentSubs = 13400

    val videoReleases = mapOf(
        3 to "Jak postavit AI agenta v Androidu",
        10 to "Docker tutoriál pro začátečníky",
        17 to "3 zkratky v Termuxu",
        24 to "Budoucnost programování 2027"
    )

    for (day in 1..30) {
        val isVideoDay = videoReleases.containsKey(day)
        val isSunday = (day % 7 == 6)
        val isThursday = (day % 7 == 3)

        val views = when {
            isVideoDay -> 8200 + (day * 40)
            isSunday -> 6800 + (day * 35)
            isThursday -> 5900 + (day * 30)
            else -> 2800 + (day * 30) + ((day * 97) % 650)
        }

        val subGain = when {
            isVideoDay -> 180 + (day * 2)
            isSunday -> 120 + day
            else -> 28 + (day % 15)
        }

        currentSubs += subGain

        list.add(
            DayTrendData(
                dayNumber = day,
                dayLabel = "Den $day (${getDayOfWeekName(day)})",
                views = views,
                totalSubscribers = currentSubs,
                subGain = subGain,
                videoPublishedTitle = videoReleases[day]
            )
        )
    }

    return list
}

private fun getDayOfWeekName(day: Int): String {
    val days = listOf("Po", "Út", "St", "Čt", "Pá", "So", "Ne")
    return days[(day - 1) % 7]
}
