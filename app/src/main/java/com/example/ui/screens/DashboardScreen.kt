package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.LocalPolice
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SheetRecord
import com.example.ui.components.DistributionDonutChart
import com.example.ui.components.KpiStatCard
import com.example.ui.components.RecordCard
import com.example.ui.components.RecordDetailDialog
import com.example.ui.components.StationBarChart
import com.example.ui.components.TimelineTrendChart
import com.example.ui.components.WarningNoticeCard
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SecondaryTeal
import com.example.ui.theme.WarningRed
import com.example.ui.viewmodel.StatusFilter
import com.example.ui.viewmodel.UiState

@Composable
fun DashboardScreen(
    uiState: UiState,
    filteredRecords: List<SheetRecord>,
    onRefresh: () -> Unit,
    onNavigateToRecords: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToFeeder: () -> Unit,
    onThanaFilter: (String?) -> Unit,
    onStatusFilter: (StatusFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedRecordForDialog by remember { mutableStateOf<SheetRecord?>(null) }
    var activeChartTab by remember { mutableStateOf(0) } // 0: Bar Chart, 1: Donut/Pie Chart, 2: Timeline

    selectedRecordForDialog?.let { record ->
        RecordDetailDialog(
            record = record,
            onDismiss = { selectedRecordForDialog = null }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Feed Header Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF10B981))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "LIVE PUBLIC FEED",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = uiState.currentFeed.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Amethi District Police • Google Sheets Live Integration",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }

                    IconButton(
                        onClick = onRefresh,
                        enabled = !uiState.isRefreshing
                    ) {
                        if (uiState.isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh Feed",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        // Warning Alert Banner from Google Sheet
        if (uiState.summary.warningNotice.isNotBlank()) {
            item {
                WarningNoticeCard(
                    notice = uiState.summary.warningNotice,
                    onClickPending = {
                        onStatusFilter(StatusFilter.PENDING_ONLY)
                        onNavigateToRecords()
                    }
                )
            }
        }

        // KPI Stat Cards Grid (2x2)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    KpiStatCard(
                        title = "Total Registered",
                        value = "${uiState.summary.totalRecords}",
                        subtitle = "100%",
                        icon = Icons.Default.FolderShared,
                        accentColor = PrimaryBlue,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            onStatusFilter(StatusFilter.ALL)
                            onNavigateToRecords()
                        }
                    )

                    KpiStatCard(
                        title = "Pending SIDs",
                        value = "${uiState.summary.totalPending}",
                        subtitle = "Alert",
                        icon = Icons.Default.ErrorOutline,
                        accentColor = WarningRed,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            onStatusFilter(StatusFilter.PENDING_ONLY)
                            onNavigateToRecords()
                        }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    KpiStatCard(
                        title = "Active Stations",
                        value = "${uiState.summary.totalStations}",
                        subtitle = "Amethi Dist.",
                        icon = Icons.Default.LocalPolice,
                        accentColor = SecondaryTeal,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToAnalytics
                    )

                    KpiStatCard(
                        title = "Top Pendency",
                        value = "${uiState.summary.highestPendencyCount}",
                        subtitle = uiState.summary.highestPendencyStation.take(8),
                        icon = Icons.Default.Warning,
                        accentColor = AccentAmber,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            onThanaFilter(uiState.summary.highestPendencyStation)
                            onStatusFilter(StatusFilter.PENDING_ONLY)
                            onNavigateToRecords()
                        }
                    )
                }
            }
        }

        // Data Visualization Section with Chart Switcher
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Data Visualizations",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    TextButton(onClick = onNavigateToAnalytics) {
                        Text("All Charts")
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Chart toggle tabs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = activeChartTab == 0,
                        onClick = { activeChartTab = 0 },
                        label = { Text("Bar Chart") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.BarChart, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )

                    FilterChip(
                        selected = activeChartTab == 1,
                        onClick = { activeChartTab = 1 },
                        label = { Text("Pie / Donut") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.PieChart, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )

                    FilterChip(
                        selected = activeChartTab == 2,
                        onClick = { activeChartTab = 2 },
                        label = { Text("Trend") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Timeline, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                when (activeChartTab) {
                    0 -> {
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
                    1 -> {
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
        }

        // Quick Feeder Action Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Feed Google Sheet / Submit",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Paste any public sheet URL or submit records to registry",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                        )
                    }

                    Button(
                        onClick = onNavigateToFeeder,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Feed")
                    }
                }
            }
        }

        // Recent / Priority Pending Records section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "High Priority Records",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                TextButton(onClick = onNavigateToRecords) {
                    Text("View All (${filteredRecords.size})")
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        // Show sample of priority records (pending first)
        val previewRecords = filteredRecords
            .sortedByDescending { it.isPending }
            .take(5)

        if (previewRecords.isEmpty()) {
            item {
                Text(
                    text = "No records found matching criteria.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            items(previewRecords, key = { it.id }) { record ->
                RecordCard(
                    record = record,
                    onClick = { selectedRecordForDialog = record }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
