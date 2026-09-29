package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LocalPolice
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SheetRecord
import com.example.ui.components.RecordCard
import com.example.ui.components.RecordDetailDialog
import com.example.ui.viewmodel.SearchScope
import com.example.ui.viewmodel.SortOrder
import com.example.ui.viewmodel.StatusFilter
import com.example.ui.viewmodel.UiState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RecordsScreen(
    uiState: UiState,
    records: List<SheetRecord>,
    starredCases: List<com.example.data.firebase.StarredCase> = emptyList(),
    onSearchQueryChange: (String) -> Unit,
    onSearchScopeChange: (SearchScope) -> Unit,
    onThanaFilter: (String?) -> Unit,
    onStatusFilter: (StatusFilter) -> Unit,
    onMonthFilter: (String?) -> Unit,
    onSortOrderChange: (SortOrder) -> Unit,
    onToggleStar: (SheetRecord) -> Unit,
    onToggleStarredOnly: (Boolean) -> Unit,
    onClearFilters: () -> Unit,
    onExportCsv: () -> String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedRecordForDialog by remember { mutableStateOf<SheetRecord?>(null) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var scopeMenuExpanded by remember { mutableStateOf(false) }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Calculate active filter count
    val activeFiltersCount = remember(
        uiState.selectedThana,
        uiState.statusFilter,
        uiState.selectedMonth,
        uiState.sortOrder,
        uiState.searchScope,
        uiState.starredOnly
    ) {
        var count = 0
        if (uiState.selectedThana != null) count++
        if (uiState.statusFilter != StatusFilter.ALL) count++
        if (uiState.selectedMonth != null) count++
        if (uiState.sortOrder != SortOrder.DEFAULT) count++
        if (uiState.searchScope != SearchScope.ALL) count++
        if (uiState.starredOnly) count++
        count
    }

    selectedRecordForDialog?.let { record ->
        val isStarred = starredCases.any { it.id == record.id.toString() || it.caseNumber.equals(record.caseNumber, ignoreCase = true) }
        RecordDetailDialog(
            record = record,
            onDismiss = { selectedRecordForDialog = null },
            isStarred = isStarred,
            onToggleStar = { onToggleStar(record) }
        )
    }

    // Filter Bottom Sheet Dialog
    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            sheetState = bottomSheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .testTag("filter_bottom_sheet"),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.FilterAlt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Filter by Column Values",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (activeFiltersCount > 0) {
                        TextButton(onClick = {
                            onClearFilters()
                            showFilterSheet = false
                        }) {
                            Text("Reset All")
                        }
                    }
                }

                HorizontalDivider()

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Column 1: Police Station (थाना)
                    item {
                        Column {
                            Text(
                                text = "Police Station (थाना)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = uiState.selectedThana == null,
                                    onClick = { onThanaFilter(null) },
                                    label = { Text("All Stations (${uiState.summary.totalStations})") }
                                )

                                uiState.summary.stationStats.forEach { stat ->
                                    val isSelected = uiState.selectedThana == stat.thana
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { onThanaFilter(if (isSelected) null else stat.thana) },
                                        label = { Text("${stat.thana} (${stat.totalCount})") },
                                        leadingIcon = if (isSelected) {
                                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                        } else null
                                    )
                                }
                            }
                        }
                    }

                    // Column 2: Status / Pendency (स्थिति)
                    item {
                        Column {
                            Text(
                                text = "SID Status (स्थिति)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = uiState.statusFilter == StatusFilter.ALL,
                                    onClick = { onStatusFilter(StatusFilter.ALL) },
                                    label = { Text("All Status") },
                                    modifier = Modifier.weight(1f)
                                )

                                FilterChip(
                                    selected = uiState.statusFilter == StatusFilter.PENDING_ONLY,
                                    onClick = { onStatusFilter(StatusFilter.PENDING_ONLY) },
                                    label = { Text("⚠️ Pending (${uiState.summary.totalPending})") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFFFEF2F2),
                                        selectedLabelColor = Color(0xFFDC2626)
                                    ),
                                    modifier = Modifier.weight(1f)
                                )

                                FilterChip(
                                    selected = uiState.statusFilter == StatusFilter.RESOLVED_ONLY,
                                    onClick = { onStatusFilter(StatusFilter.RESOLVED_ONLY) },
                                    label = { Text("✅ Active (${uiState.summary.totalProcessed})") },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Column 3: Registration Month Period (पंजीकरण माह)
                    item {
                        Column {
                            Text(
                                text = "Registration Month (पंजीकरण माह)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = uiState.selectedMonth == null,
                                    onClick = { onMonthFilter(null) },
                                    label = { Text("All Months") }
                                )

                                uiState.summary.monthlyStats.forEach { stat ->
                                    val isSelected = uiState.selectedMonth == stat.periodLabel
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { onMonthFilter(if (isSelected) null else stat.periodLabel) },
                                        label = { Text("${stat.periodLabel} (${stat.count})") },
                                        leadingIcon = {
                                            Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(14.dp))
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Column 4: Sort Order
                    item {
                        Column {
                            Text(
                                text = "Sort Records By",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                SortOrder.entries.forEach { sort ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { onSortOrderChange(sort) }
                                            .padding(vertical = 4.dp, horizontal = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = uiState.sortOrder == sort,
                                            onClick = { onSortOrderChange(sort) }
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = sort.label,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = { showFilterSheet = false },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Apply Filters (${records.size} Matches)")
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("records_screen")
    ) {
        // Top Search & Filter Control Surface
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Main Search Bar with Column Scope Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = onSearchQueryChange,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("records_search_input"),
                        placeholder = {
                            val placeholderText = when (uiState.searchScope) {
                                SearchScope.ALL -> "Search all columns..."
                                SearchScope.THANA -> "Search police station..."
                                SearchScope.CASE_NO -> "Search FIR / case (e.g. 136/26)..."
                                SearchScope.SID -> "Search SID (17414060...)..."
                                SearchScope.REG_DATE -> "Search date (09/05/2026)..."
                            }
                            Text(placeholderText, fontSize = 13.sp, maxLines = 1)
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = {
                            if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onSearchQueryChange("") }) {
                                    Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    // Filter Button with Badge
                    BadgedBox(
                        badge = {
                            if (activeFiltersCount > 0) {
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ) {
                                    Text("$activeFiltersCount")
                                }
                            }
                        }
                    ) {
                        OutlinedButton(
                            onClick = { showFilterSheet = true },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 14.dp),
                            modifier = Modifier.testTag("open_filter_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = "Filters",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Quick Scope and Column Filter Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Column Scope Dropdown
                    Box {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .clickable { scopeMenuExpanded = true }
                                .testTag("search_scope_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "In: ${uiState.searchScope.label}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = scopeMenuExpanded,
                            onDismissRequest = { scopeMenuExpanded = false }
                        ) {
                            SearchScope.entries.forEach { scope ->
                                DropdownMenuItem(
                                    text = { Text(scope.label) },
                                    onClick = {
                                        onSearchScopeChange(scope)
                                        scopeMenuExpanded = false
                                    },
                                    leadingIcon = if (uiState.searchScope == scope) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null
                                )
                            }
                        }
                    }

                    // Sort Order Dropdown
                    Box {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .clickable { sortMenuExpanded = true }
                                .testTag("sort_order_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sort,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = uiState.sortOrder.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = sortMenuExpanded,
                            onDismissRequest = { sortMenuExpanded = false }
                        ) {
                            SortOrder.entries.forEach { sort ->
                                DropdownMenuItem(
                                    text = { Text(sort.label) },
                                    onClick = {
                                        onSortOrderChange(sort)
                                        sortMenuExpanded = false
                                    },
                                    leadingIcon = if (uiState.sortOrder == sort) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null
                                )
                            }
                        }
                    }

                    // Quick Status Chips
                    FilterChip(
                        selected = uiState.statusFilter == StatusFilter.PENDING_ONLY,
                        onClick = {
                            onStatusFilter(
                                if (uiState.statusFilter == StatusFilter.PENDING_ONLY) StatusFilter.ALL
                                else StatusFilter.PENDING_ONLY
                            )
                        },
                        label = { Text("⚠️ Pending (${uiState.summary.totalPending})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFFEF2F2),
                            selectedLabelColor = Color(0xFFDC2626)
                        ),
                        modifier = Modifier.testTag("quick_filter_pending")
                    )

                    // Quick Starred Cases Chip
                    FilterChip(
                        selected = uiState.starredOnly,
                        onClick = { onToggleStarredOnly(!uiState.starredOnly) },
                        label = { Text("⭐ Starred (${starredCases.size})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFFEF3C7),
                            selectedLabelColor = Color(0xFFB45309)
                        ),
                        modifier = Modifier.testTag("quick_filter_starred")
                    )

                    // Quick Top Station Chips
                    uiState.summary.stationStats.take(4).forEach { stat ->
                        val isSelected = uiState.selectedThana == stat.thana
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                onThanaFilter(if (isSelected) null else stat.thana)
                            },
                            label = { Text(stat.thana) }
                        )
                    }
                }

                // Active Filters Row (Removable InputChips)
                AnimatedVisibility(
                    visible = activeFiltersCount > 0 || uiState.searchQuery.isNotBlank(),
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Active:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )

                        if (uiState.searchQuery.isNotBlank()) {
                            InputChip(
                                selected = true,
                                onClick = { onSearchQueryChange("") },
                                label = { Text("Query: \"${uiState.searchQuery}\"") },
                                trailingIcon = {
                                    Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(14.dp))
                                }
                            )
                        }

                        if (uiState.selectedThana != null) {
                            InputChip(
                                selected = true,
                                onClick = { onThanaFilter(null) },
                                label = { Text("Station: ${uiState.selectedThana}") },
                                trailingIcon = {
                                    Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(14.dp))
                                }
                            )
                        }

                        if (uiState.statusFilter != StatusFilter.ALL) {
                            InputChip(
                                selected = true,
                                onClick = { onStatusFilter(StatusFilter.ALL) },
                                label = {
                                    Text(if (uiState.statusFilter == StatusFilter.PENDING_ONLY) "Status: Pending" else "Status: Active")
                                },
                                trailingIcon = {
                                    Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(14.dp))
                                }
                            )
                        }

                        if (uiState.starredOnly) {
                            InputChip(
                                selected = true,
                                onClick = { onToggleStarredOnly(false) },
                                label = { Text("⭐ Starred Only") },
                                trailingIcon = {
                                    Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(14.dp))
                                }
                            )
                        }

                        if (uiState.selectedMonth != null) {
                            InputChip(
                                selected = true,
                                onClick = { onMonthFilter(null) },
                                label = { Text("Month: ${uiState.selectedMonth}") },
                                trailingIcon = {
                                    Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(14.dp))
                                }
                            )
                        }

                        if (uiState.sortOrder != SortOrder.DEFAULT) {
                            InputChip(
                                selected = true,
                                onClick = { onSortOrderChange(SortOrder.DEFAULT) },
                                label = { Text("Sort: ${uiState.sortOrder.label}") },
                                trailingIcon = {
                                    Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(14.dp))
                                }
                            )
                        }

                        TextButton(
                            onClick = onClearFilters,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("Clear All", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }

        // Results Counter Header and CSV Export
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Found ${records.size} of ${uiState.summary.totalRecords} records",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )

            OutlinedButton(
                onClick = {
                    val csv = onExportCsv()
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/csv"
                        putExtra(Intent.EXTRA_SUBJECT, "Exported Sheet Records (${records.size})")
                        putExtra(Intent.EXTRA_TEXT, csv)
                    }
                    context.startActivity(Intent.createChooser(intent, "Export / Share CSV"))
                },
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.testTag("export_csv_button")
            ) {
                Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Export CSV", fontSize = 12.sp)
            }
        }

        // List of filtered records
        if (records.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No records match your filter criteria",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Try adjusting your search query, station, or status filters.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onClearFilters,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Reset All Filters")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp, top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(records, key = { it.id }) { record ->
                    val isStarred = starredCases.any { it.id == record.id.toString() || it.caseNumber.equals(record.caseNumber, ignoreCase = true) }
                    RecordCard(
                        record = record,
                        onClick = { selectedRecordForDialog = record },
                        isStarred = isStarred,
                        onToggleStar = { onToggleStar(record) }
                    )
                }
            }
        }
    }
}
