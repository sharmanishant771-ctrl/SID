package com.example.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalPolice
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SidFeedingEntry
import com.example.data.model.StationProgress
import com.example.ui.viewmodel.UiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class AdminProgressFilter {
    ALL,
    LAGGING_ONLY, // < 50%
    COMPLETED_ONLY // >= 80%
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminMonitoringScreen(
    uiState: UiState,
    stationProgressList: List<StationProgress>,
    recentFeedings: List<SidFeedingEntry>,
    onSelectStationToFeed: (String) -> Unit,
    onNavigateToRecordsForStation: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var filterType by remember { mutableStateOf(AdminProgressFilter.ALL) }
    var stationSearchQuery by remember { mutableStateOf("") }

    val totalDistrictCases = uiState.summary.totalRecords
    val totalDistrictPending = uiState.summary.totalPending
    val totalDistrictCompleted = totalDistrictCases - totalDistrictPending
    val overallPercentage = if (totalDistrictCases > 0) {
        (totalDistrictCompleted.toFloat() / totalDistrictCases.toFloat()) * 100f
    } else 0f

    val filteredStations = remember(stationProgressList, filterType, stationSearchQuery) {
        stationProgressList.filter { stat ->
            val matchesQuery = if (stationSearchQuery.isBlank()) true
            else stat.thana.contains(stationSearchQuery.trim(), ignoreCase = true) ||
                    stat.thanaEn.contains(stationSearchQuery.trim(), ignoreCase = true)

            val matchesFilter = when (filterType) {
                AdminProgressFilter.ALL -> true
                AdminProgressFilter.LAGGING_ONLY -> stat.completionPercentage < 50f
                AdminProgressFilter.COMPLETED_ONLY -> stat.completionPercentage >= 80f
            }
            matchesQuery && matchesFilter
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // District Master Dashboard Header Card
        Surface(
            tonalElevation = 3.dp,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Assessment,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "एडमिन कंट्रोल व थानों की प्रगति",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "जनपद बरेली - E-Sakshya SID मॉनिटरिंग",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Share Report Button
                    Button(
                        onClick = {
                            val report = generateDistrictFeedingReport(
                                totalCases = totalDistrictCases,
                                completed = totalDistrictCompleted,
                                pending = totalDistrictPending,
                                overallPct = overallPercentage,
                                stations = stationProgressList
                            )
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "E-Sakshya Feeding Daily Progress Report")
                                putExtra(Intent.EXTRA_TEXT, report)
                            }
                            context.startActivity(Intent.createChooser(intent, "Share Feeding Report"))
                        },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("admin_share_report_button")
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("रिपोर्ट शेयर करें", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Overall Progress Metric Card
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "समस्त थानों की औसत प्रगति",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "${overallPercentage.toInt()}%",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LinearProgressIndicator(
                            progress = { overallPercentage / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            MetricCounter(label = "कुल दर्ज केस", count = totalDistrictCases, color = MaterialTheme.colorScheme.onSurface)
                            MetricCounter(label = "✅ SID पूर्ण", count = totalDistrictCompleted, color = Color(0xFF16A34A))
                            MetricCounter(label = "⚠️ SID लम्बित", count = totalDistrictPending, color = Color(0xFFDC2626))
                        }
                    }
                }
            }
        }

        // Two Main Views: "थानों की प्रगति" vs "हालिया लाइव फीडिंग"
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("थानेवार प्रगति (${stationProgressList.size})", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.TrendingUp, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("लाइव फीडिंग स्ट्रीम (${recentFeedings.size})", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
        }

        if (selectedTab == 0) {
            // TAB 0: Station Progress Board
            Column(modifier = Modifier.fillMaxSize()) {
                // Filter chips & search
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = filterType == AdminProgressFilter.ALL,
                        onClick = { filterType = AdminProgressFilter.ALL },
                        label = { Text("सभी (${stationProgressList.size})") }
                    )

                    FilterChip(
                        selected = filterType == AdminProgressFilter.LAGGING_ONLY,
                        onClick = { filterType = AdminProgressFilter.LAGGING_ONLY },
                        label = { Text("🚨 ध्यान अपेक्षित (< 50%)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFFEF2F2),
                            selectedLabelColor = Color(0xFFDC2626)
                        )
                    )

                    FilterChip(
                        selected = filterType == AdminProgressFilter.COMPLETED_ONLY,
                        onClick = { filterType = AdminProgressFilter.COMPLETED_ONLY },
                        label = { Text("⭐ श्रेष्ठ (> 80%)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFF0FDF4),
                            selectedLabelColor = Color(0xFF16A34A)
                        )
                    )
                }

                // Station Search
                OutlinedTextField(
                    value = stationSearchQuery,
                    onValueChange = { stationSearchQuery = it },
                    placeholder = { Text("थाना खोजें...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(6.dp))

                // List of Station Progress Cards
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp, top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredStations, key = { it.thana }) { station ->
                        StationProgressCard(
                            station = station,
                            onFeedSid = { onSelectStationToFeed(station.thana) },
                            onViewRecords = { onNavigateToRecordsForStation(station.thana) }
                        )
                    }
                }
            }
        } else {
            // TAB 1: Live Firestore Feeding Stream
            if (recentFeedings.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "लाइव फीडिंग मॉनिटर सक्रिय",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "जैसे ही कोई थाना नया SID नंबर दर्ज करेगा, वह यहाँ लाइव दिखेगा।",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(recentFeedings, key = { it.id }) { entry ->
                        LiveFeedingItemCard(entry = entry)
                    }
                }
            }
        }
    }
}

@Composable
fun StationProgressCard(
    station: StationProgress,
    onFeedSid: () -> Unit,
    onViewRecords: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_station_card_${station.thana}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalPolice,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = station.thana,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (station.thanaEn.isNotBlank()) {
                            Text(
                                text = station.thanaEn,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = when {
                        station.completionPercentage >= 80f -> Color(0xFFF0FDF4)
                        station.completionPercentage < 50f -> Color(0xFFFEF2F2)
                        else -> Color(0xFFFFFBEB)
                    },
                    border = androidx.compose.foundation.BorderStroke(
                        0.8.dp,
                        when {
                            station.completionPercentage >= 80f -> Color(0xFF86EFAC)
                            station.completionPercentage < 50f -> Color(0xFFFCA5A5)
                            else -> Color(0xFFFCD34D)
                        }
                    )
                ) {
                    Text(
                        text = "${station.completionPercentage.toInt()}% पूर्ण",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            station.completionPercentage >= 80f -> Color(0xFF166534)
                            station.completionPercentage < 50f -> Color(0xFF991B1B)
                            else -> Color(0xFF92400E)
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { station.completionPercentage / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = when {
                    station.completionPercentage >= 80f -> Color(0xFF10B981)
                    station.completionPercentage < 50f -> Color(0xFFEF4444)
                    else -> MaterialTheme.colorScheme.primary
                },
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "कुल: ${station.totalCases}  |  पूर्ण: ${station.completedSids}  |  लम्बित: ${station.pendingSids}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onViewRecords,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("केस देखें (${station.totalCases})", fontSize = 11.sp)
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onFeedSid,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("फीडिंग खोलें", fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun LiveFeedingItemCard(entry: SidFeedingEntry) {
    val dateFormatted = remember(entry.timestamp) {
        SimpleDateFormat("hh:mm a, dd MMM", Locale.ENGLISH).format(Date(entry.timestamp))
    }

    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF0FDF4)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF16A34A),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "थाना ${entry.thana} - मु0अ0सं0 ${entry.caseNumber}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = dateFormatted,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "नया SID: ${entry.sidNumber}  (तिथि: ${entry.sidCreationDate})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )

                if (entry.officerName.isNotBlank()) {
                    Text(
                        text = "ऑपरेटर: ${entry.officerName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricCounter(label: String, count: Int, color: Color) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = count.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
    }
}

private fun generateDistrictFeedingReport(
    totalCases: Int,
    completed: Int,
    pending: Int,
    overallPct: Float,
    stations: List<StationProgress>
): String {
    val dateStr = SimpleDateFormat("dd-MM-yyyy hh:mm a", Locale.ENGLISH).format(Date())
    val sb = StringBuilder()
    sb.appendLine("📊 *E-Sakshya SID दैनिक फीडिंग प्रगति आख्या*")
    sb.appendLine("📅 दिनांक/समय: $dateStr")
    sb.appendLine("─────────────────────────")
    sb.appendLine("🔹 *जनपद की कुल प्रगति: ${overallPct.toInt()}%*")
    sb.appendLine("▪️ कुल केस: $totalCases")
    sb.appendLine("▪️ SID पूर्ण: $completed")
    sb.appendLine("▪️ SID लम्बित: $pending")
    sb.appendLine("─────────────────────────")
    sb.appendLine("🚨 *लम्बित थानों का विवरण:*")

    val lagging = stations.filter { it.pendingSids > 0 }.sortedByDescending { it.pendingSids }
    lagging.forEachIndexed { idx, st ->
        sb.appendLine("${idx + 1}. थाना ${st.thana}: लम्बित ${st.pendingSids} (कुल: ${st.totalCases}, ${st.completionPercentage.toInt()}%)")
    }

    if (lagging.isEmpty()) {
        sb.appendLine("✅ समस्त थानों का 100% कार्य पूर्ण है।")
    }

    sb.appendLine("─────────────────────────")
    sb.appendLine("द्वारा: E-Sakshya Feeding मॉनिटरिंग सिस्टम")
    return sb.toString()
}
