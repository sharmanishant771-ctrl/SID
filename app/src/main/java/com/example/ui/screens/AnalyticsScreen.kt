package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LocalPolice
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChartType
import com.example.ui.components.DistributionDonutChart
import com.example.ui.components.StationBarChart
import com.example.ui.components.TimelineTrendChart
import com.example.ui.theme.ChartColors
import com.example.ui.viewmodel.UiState

@Composable
fun AnalyticsScreen(
    uiState: UiState,
    onThanaFilter: (String?) -> Unit,
    onNavigateToRecords: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedChart by remember { mutableStateOf(ChartType.BAR_STATION_CASES) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("analytics_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Page Title & Subtitle
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Data Visualizations & Analytics",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Graphical analysis of ${uiState.summary.totalRecords} records across ${uiState.summary.totalStations} stations",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Horizontal Chart Selector Chips
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedChart == ChartType.BAR_STATION_CASES,
                    onClick = { selectedChart = ChartType.BAR_STATION_CASES },
                    label = { Text("Bar: Station Cases") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.BarChart, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )

                FilterChip(
                    selected = selectedChart == ChartType.BAR_STATION_PENDENCY,
                    onClick = { selectedChart = ChartType.BAR_STATION_PENDENCY },
                    label = { Text("Bar: Pendency Alert") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFFEF4444))
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFFEF2F2)
                    )
                )

                FilterChip(
                    selected = selectedChart == ChartType.DONUT_DISTRIBUTION,
                    onClick = { selectedChart = ChartType.DONUT_DISTRIBUTION },
                    label = { Text("Pie/Donut: Share") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.PieChart, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                )

                FilterChip(
                    selected = selectedChart == ChartType.TIMELINE_TREND,
                    onClick = { selectedChart = ChartType.TIMELINE_TREND },
                    label = { Text("Trend: Timeline") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Timeline, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }
        }

        // Selected Chart Render
        item {
            when (selectedChart) {
                ChartType.BAR_STATION_CASES -> {
                    StationBarChart(
                        stats = uiState.summary.stationStats,
                        selectedThana = uiState.selectedThana,
                        onThanaSelected = { thana ->
                            onThanaFilter(thana)
                            if (thana != null) onNavigateToRecords()
                        },
                        showPendingMetric = false
                    )
                }
                ChartType.BAR_STATION_PENDENCY -> {
                    StationBarChart(
                        stats = uiState.summary.stationStats,
                        selectedThana = uiState.selectedThana,
                        onThanaSelected = { thana ->
                            onThanaFilter(thana)
                            if (thana != null) onNavigateToRecords()
                        },
                        showPendingMetric = true
                    )
                }
                ChartType.DONUT_DISTRIBUTION -> {
                    DistributionDonutChart(
                        stats = uiState.summary.stationStats,
                        selectedThana = uiState.selectedThana,
                        onThanaSelected = { thana ->
                            onThanaFilter(thana)
                            if (thana != null) onNavigateToRecords()
                        }
                    )
                }
                else -> {
                    TimelineTrendChart(
                        timeline = uiState.summary.monthlyStats
                    )
                }
            }
        }

        // Second Chart: Always display complementary chart below for rich visual dashboard
        item {
            if (selectedChart != ChartType.DONUT_DISTRIBUTION) {
                DistributionDonutChart(
                    stats = uiState.summary.stationStats,
                    selectedThana = uiState.selectedThana,
                    onThanaSelected = { thana ->
                        onThanaFilter(thana)
                        if (thana != null) onNavigateToRecords()
                    }
                )
            } else {
                StationBarChart(
                    stats = uiState.summary.stationStats,
                    selectedThana = uiState.selectedThana,
                    onThanaSelected = { thana ->
                        onThanaFilter(thana)
                        if (thana != null) onNavigateToRecords()
                    },
                    showPendingMetric = true
                )
            }
        }

        // Station Leaderboard / Ranking Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
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
                        text = "Complete Police Station Index",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Ranked by case volume • Tap station to view all records",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    val maxStationCount = uiState.summary.stationStats.firstOrNull()?.totalCount ?: 1

                    uiState.summary.stationStats.forEachIndexed { index, stat ->
                        val ratio = stat.totalCount.toFloat() / maxStationCount
                        val color = ChartColors[index % ChartColors.size]

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    onThanaFilter(stat.thana)
                                    onNavigateToRecords()
                                }
                                .padding(vertical = 8.dp, horizontal = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = CircleShape,
                                        color = color.copy(alpha = 0.2f),
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "${index + 1}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = color,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${stat.thana} (${stat.thanaEn})",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (stat.pendingCount > 0) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color(0xFFFEF2F2),
                                            modifier = Modifier.padding(end = 8.dp)
                                        ) {
                                            Text(
                                                text = "${stat.pendingCount} pending",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFFDC2626),
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                    Text(
                                        text = "${stat.totalCount} cases",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            LinearProgressIndicator(
                                progress = { ratio },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(CircleShape),
                                color = color,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
