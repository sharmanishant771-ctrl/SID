package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ThanaStat
import com.example.data.model.TimelineStat
import com.example.ui.theme.ChartColors
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * 1. Interactive Bar Chart representing station-wise cases or pending SIDs.
 */
@Composable
fun StationBarChart(
    stats: List<ThanaStat>,
    selectedThana: String?,
    onThanaSelected: (String?) -> Unit,
    showPendingMetric: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (stats.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(180.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No station data available to visualize",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val displayList = remember(stats, showPendingMetric) {
        if (showPendingMetric) {
            stats.sortedByDescending { it.pendingCount }.take(12)
        } else {
            stats.sortedByDescending { it.totalCount }.take(12)
        }
    }

    val maxVal = remember(displayList, showPendingMetric) {
        val max = displayList.maxOfOrNull { if (showPendingMetric) it.pendingCount else it.totalCount } ?: 1
        if (max == 0) 1 else max
    }

    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(showPendingMetric, stats) {
        animProgress.snapTo(0f)
        animProgress.animateTo(1f, animationSpec = tween(650, easing = FastOutSlowInEasing))
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("station_bar_chart_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (showPendingMetric) "Pending SIDs by Station (थाना)" else "Case Distribution by Station (थाना)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Top ${displayList.size} Police Stations • Tap a bar to filter records",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (selectedThana != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier
                            .clickable { onThanaSelected(null) }
                            .padding(4.dp)
                    ) {
                        Text(
                            text = "Reset Filter",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Horizontal bars for clear Hindi/English station name readability
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                displayList.forEachIndexed { index, stat ->
                    val value = if (showPendingMetric) stat.pendingCount else stat.totalCount
                    val fraction = (value.toFloat() / maxVal.toFloat()).coerceIn(0.05f, 1f) * animProgress.value
                    val isSelected = selectedThana == stat.thana
                    val isHighAlert = showPendingMetric && stat.pendingCount >= 10

                    val barColor = when {
                        isSelected -> MaterialTheme.colorScheme.primary
                        isHighAlert -> Color(0xFFEF4444)
                        showPendingMetric -> Color(0xFFF59E0B)
                        else -> ChartColors[index % ChartColors.size]
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                else Color.Transparent
                            )
                            .clickable {
                                if (isSelected) onThanaSelected(null) else onThanaSelected(stat.thana)
                            }
                            .padding(vertical = 4.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Station Name Labels (Hindi + Roman)
                        Column(modifier = Modifier.width(108.dp)) {
                            Text(
                                text = stat.thana,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stat.thanaEn,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Animated Bar Box
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(22.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction)
                                    .height(22.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(barColor.copy(alpha = 0.75f), barColor)
                                        )
                                    )
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // Value badge
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.width(52.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            if (isHighAlert) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Alert",
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier
                                        .size(14.dp)
                                        .padding(end = 2.dp)
                                )
                            }
                            Text(
                                text = value.toString(),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isHighAlert) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 2. Interactive Pie / Donut Chart visualizing proportional distribution.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DistributionDonutChart(
    stats: List<ThanaStat>,
    selectedThana: String?,
    onThanaSelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    if (stats.isEmpty()) return

    // Prepare slices: Top 6 stations + "Others"
    val slices = remember(stats) {
        val top = stats.take(6)
        val othersCount = stats.drop(6).sumOf { it.totalCount }
        val list = top.toMutableList()
        if (othersCount > 0) {
            val total = stats.sumOf { it.totalCount }
            list.add(
                ThanaStat(
                    thana = "अन्य (Others)",
                    thanaEn = "Others",
                    totalCount = othersCount,
                    pendingCount = stats.drop(6).sumOf { it.pendingCount },
                    percentage = (othersCount.toFloat() / total) * 100f
                )
            )
        }
        list
    }

    val totalRecords = remember(stats) { stats.sumOf { it.totalCount } }
    var activeSliceIndex by remember { mutableStateOf<Int?>(null) }

    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(stats) {
        animProgress.snapTo(0f)
        animProgress.animateTo(1f, animationSpec = tween(700, easing = FastOutSlowInEasing))
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("donut_pie_chart_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Station Share (Pie/Donut)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Distribution breakdown across Amethi District",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = "$totalRecords Cases",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Canvas for Donut Chart
            Box(
                modifier = Modifier
                    .size(220.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .size(200.dp)
                        .pointerInput(slices) {
                            detectTapGestures { offset ->
                                val center = Offset(size.width / 2f, size.height / 2f)
                                val dx = offset.x - center.x
                                val dy = offset.y - center.y
                                var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                if (angle < 0) angle += 360f

                                // Match tapped angle to slice
                                var currentAngle = 0f
                                for (i in slices.indices) {
                                    val sweep = (slices[i].totalCount.toFloat() / totalRecords) * 360f
                                    if (angle >= currentAngle && angle <= currentAngle + sweep) {
                                        activeSliceIndex = if (activeSliceIndex == i) null else i
                                        val clickedThana = slices[i].thana
                                        if (clickedThana != "अन्य (Others)") {
                                            onThanaSelected(if (selectedThana == clickedThana) null else clickedThana)
                                        }
                                        break
                                    }
                                    currentAngle += sweep
                                }
                            }
                        }
                ) {
                    val strokeWidth = 36.dp.toPx()
                    val radius = (size.minDimension - strokeWidth) / 2
                    val center = Offset(size.width / 2, size.height / 2)
                    var startAngle = -90f

                    slices.forEachIndexed { index, slice ->
                        val sweep = ((slice.totalCount.toFloat() / totalRecords) * 360f) * animProgress.value
                        val isHighlighted = activeSliceIndex == index || selectedThana == slice.thana
                        val color = ChartColors[index % ChartColors.size]

                        drawArc(
                            color = if (isHighlighted) color else color.copy(alpha = 0.85f),
                            startAngle = startAngle,
                            sweepAngle = sweep - 1.5f, // Tiny gap between slices
                            useCenter = false,
                            topLeft = Offset(center.x - radius, center.y - radius),
                            size = Size(radius * 2, radius * 2),
                            style = Stroke(
                                width = if (isHighlighted) strokeWidth + 6.dp.toPx() else strokeWidth,
                                cap = StrokeCap.Round
                            )
                        )
                        startAngle += (slice.totalCount.toFloat() / totalRecords) * 360f * animProgress.value
                    }
                }

                // Center Label
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(24.dp)
                ) {
                    val displaySlice = activeSliceIndex?.let { slices.getOrNull(it) }
                    if (displaySlice != null) {
                        Text(
                            text = displaySlice.thana,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${displaySlice.totalCount}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = String.format("%.1f%%", displaySlice.percentage),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = "TOTAL",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$totalRecords",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Cases",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Interactive Color Legends
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                slices.forEachIndexed { index, slice ->
                    val color = ChartColors[index % ChartColors.size]
                    val isSelected = selectedThana == slice.thana || activeSliceIndex == index

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) color.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, color) else null,
                        modifier = Modifier
                            .clickable {
                                activeSliceIndex = if (activeSliceIndex == index) null else index
                                if (slice.thana != "अन्य (Others)") {
                                    onThanaSelected(if (selectedThana == slice.thana) null else slice.thana)
                                }
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(color)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = slice.thana,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${String.format("%.1f", slice.percentage)}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 3. Timeline Trend Chart for Case Registrations over months.
 */
@Composable
fun TimelineTrendChart(
    timeline: List<TimelineStat>,
    modifier: Modifier = Modifier
) {
    if (timeline.isEmpty()) return

    val maxVal = remember(timeline) { (timeline.maxOfOrNull { it.count } ?: 1).coerceAtLeast(1) }
    val animProgress = remember { Animatable(0f) }

    LaunchedEffect(timeline) {
        animProgress.snapTo(0f)
        animProgress.animateTo(1f, animationSpec = tween(650, easing = FastOutSlowInEasing))
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("timeline_trend_chart_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Registration Velocity by Month",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Monthly trend of FIRs / SIDs registered across 2026",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Vertical column chart with connecting baseline
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                timeline.forEach { stat ->
                    val fraction = (stat.count.toFloat() / maxVal.toFloat()).coerceIn(0.08f, 1f) * animProgress.value
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(48.dp)
                    ) {
                        Text(
                            text = "${stat.count}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .width(28.dp)
                                .height((100 * fraction).dp)
                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                        )
                                    )
                                )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stat.periodLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}
