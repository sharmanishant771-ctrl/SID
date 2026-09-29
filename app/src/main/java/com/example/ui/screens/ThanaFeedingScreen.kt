package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.LocalPolice
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SheetRecord
import com.example.data.model.StationProgress
import com.example.ui.viewmodel.UiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThanaFeedingScreen(
    uiState: UiState,
    records: List<SheetRecord>,
    stationProgressList: List<StationProgress>,
    onSelectThana: (String) -> Unit,
    onUpdateOfficerName: (String) -> Unit,
    onSubmitSid: (caseNumber: String, thana: String, regDate: String, sidNumber: String, sidDate: String, officerName: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var thanaDropdownExpanded by remember { mutableStateOf(false) }
    var selectedRecordForSid by remember { mutableStateOf<SheetRecord?>(null) }
    var showNewCaseDialog by remember { mutableStateOf(false) }
    var filterPendingOnly by remember { mutableStateOf(true) }
    var localSearchQuery by remember { mutableStateOf("") }

    val activeThana = uiState.activeThanaForFeeding
    val thanaRecords = remember(records, activeThana, filterPendingOnly, localSearchQuery) {
        records.filter {
            it.thana.equals(activeThana, ignoreCase = true) || it.thanaEn.equals(activeThana, ignoreCase = true)
        }.filter {
            if (filterPendingOnly) it.isPending else true
        }.filter {
            if (localSearchQuery.isBlank()) true
            else it.caseNumber.contains(localSearchQuery.trim(), ignoreCase = true) ||
                    it.sidNumber.contains(localSearchQuery.trim(), ignoreCase = true)
        }
    }

    val currentThanaProgress = remember(stationProgressList, activeThana) {
        stationProgressList.find { it.thana.equals(activeThana, ignoreCase = true) }
            ?: StationProgress(activeThana, "", 0, 0, 0, 0f)
    }

    // List of standard police stations from the dataset
    val stationNames = remember(stationProgressList) {
        if (stationProgressList.isNotEmpty()) {
            stationProgressList.map { it.thana }
        } else {
            listOf("को0नगर", "सिविल लाइन्स", "कैण्ट", "सुभाषनगर", "प्रेमनगर", "किला", "बारादरी", "सीबीगंज", "इज्जतनगर")
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Station Selector Header
        Surface(
            tonalElevation = 2.dp,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "थाना SID फीडिंग डेस्क",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "थाना ऑपरेटर पोर्टल (SID प्रविष्टि)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Thana Selector Chip / Dropdown
                    Box {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier
                                .clickable { thanaDropdownExpanded = true }
                                .testTag("thana_selector_dropdown")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocalPolice,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = activeThana,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Select Thana",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = thanaDropdownExpanded,
                            onDismissRequest = { thanaDropdownExpanded = false }
                        ) {
                            stationNames.forEach { thana ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = thana,
                                            fontWeight = if (thana == activeThana) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.LocalPolice,
                                            contentDescription = null,
                                            tint = if (thana == activeThana) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                        )
                                    },
                                    onClick = {
                                        onSelectThana(thana)
                                        thanaDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Station Progress Summary Card
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (currentThanaProgress.pendingSids == 0) Color(0xFF10B981) else Color(0xFFF59E0B))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "फीडिंग प्रगति: ${currentThanaProgress.completedSids} / ${currentThanaProgress.totalCases} केस",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Text(
                                text = "${currentThanaProgress.completionPercentage.toInt()}% पूर्ण",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (currentThanaProgress.completionPercentage >= 75f) Color(0xFF047857) else MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        LinearProgressIndicator(
                            progress = { currentThanaProgress.completionPercentage / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = if (currentThanaProgress.completionPercentage >= 75f) Color(0xFF10B981) else MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "लम्बित (Pending): ${currentThanaProgress.pendingSids}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = if (currentThanaProgress.pendingSids > 0) Color(0xFFDC2626) else Color(0xFF16A34A)
                            )
                            Text(
                                text = "सफलतापूर्वक फीड: ${currentThanaProgress.completedSids}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Search & Filter Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = filterPendingOnly,
                    onClick = { filterPendingOnly = true },
                    label = { Text("⚠️ केवल लम्बित (${currentThanaProgress.pendingSids})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFFEF2F2),
                        selectedLabelColor = Color(0xFFDC2626)
                    ),
                    modifier = Modifier.testTag("filter_thana_pending")
                )

                FilterChip(
                    selected = !filterPendingOnly,
                    onClick = { filterPendingOnly = false },
                    label = { Text("समस्त केस (${currentThanaProgress.totalCases})") },
                    modifier = Modifier.testTag("filter_thana_all")
                )
            }

            Button(
                onClick = { showNewCaseDialog = true },
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.testTag("add_new_case_button")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("नया केस", fontSize = 12.sp)
            }
        }

        // Search Input within Thana
        OutlinedTextField(
            value = localSearchQuery,
            onValueChange = { localSearchQuery = it },
            placeholder = { Text("मु0अ0सं0 (FIR No.) खोजें...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp),
            shape = RoundedCornerShape(10.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Records List
        if (thanaRecords.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (filterPendingOnly) "बधाई! इस थाने में कोई लम्बित SID नहीं है" else "कोई केस नहीं मिला",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (filterPendingOnly) "सभी मामलों का SID नंबर सफलता से भरा जा चुका है।" else "नया केस जोड़ने के लिए ऊपर दिए बटन का उपयोग करें।",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp, top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(thanaRecords, key = { it.id }) { record ->
                    ThanaFeedingCard(
                        record = record,
                        onFillSid = { selectedRecordForSid = record }
                    )
                }
            }
        }
    }

    // Fill SID Dialog
    selectedRecordForSid?.let { record ->
        FillSidDialog(
            record = record,
            officerName = uiState.officerName,
            onDismiss = { selectedRecordForSid = null },
            onSubmit = { sidNumber, sidDate, officer ->
                onUpdateOfficerName(officer)
                onSubmitSid(record.caseNumber, record.thana, record.registrationDate, sidNumber, sidDate, officer)
                selectedRecordForSid = null
            }
        )
    }

    // Add New Case Dialog
    if (showNewCaseDialog) {
        AddNewCaseDialog(
            thana = activeThana,
            officerName = uiState.officerName,
            onDismiss = { showNewCaseDialog = false },
            onSubmit = { caseNo, regDate, sidNo, sidDate, officer ->
                onUpdateOfficerName(officer)
                onSubmitSid(caseNo, activeThana, regDate, sidNo, sidDate, officer)
                showNewCaseDialog = false
            }
        )
    }
}

@Composable
fun ThanaFeedingCard(
    record: SheetRecord,
    onFillSid: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("thana_card_${record.id}")
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
                Column {
                    Text(
                        text = "मु0अ0सं0: ${record.caseNumber}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "पंजीकरण दिनांक: ${record.registrationDate}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (record.isPending) Color(0xFFFEF2F2) else Color(0xFFF0FDF4),
                    border = androidx.compose.foundation.BorderStroke(
                        0.8.dp,
                        if (record.isPending) Color(0xFFFCA5A5) else Color(0xFF86EFAC)
                    )
                ) {
                    Text(
                        text = if (record.isPending) "⚠️ SID लम्बित" else "✅ SID एक्टिव",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (record.isPending) Color(0xFF991B1B) else Color(0xFF166534),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Current SID Status Row
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = null,
                            tint = if (record.isPending) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (record.isPending) "SID: अभी नहीं बनाया गया" else "SID: ${record.sidNumber}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (record.isPending) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Button(
                        onClick = onFillSid,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (record.isPending) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                        ),
                        modifier = Modifier.testTag("fill_sid_btn_${record.id}")
                    ) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = if (record.isPending) "SID भरें" else "अपडेट करें", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun FillSidDialog(
    record: SheetRecord,
    officerName: String,
    onDismiss: () -> Unit,
    onSubmit: (sidNumber: String, sidDate: String, officer: String) -> Unit
) {
    val todayFormatted = remember { SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH).format(Date()) }
    var sidInput by remember { mutableStateOf(record.sidNumber) }
    var sidDateInput by remember { mutableStateOf(if (record.sidCreationDate.isNotBlank()) record.sidCreationDate else todayFormatted) }
    var officerInput by remember { mutableStateOf(officerName) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Fingerprint, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("SID नंबर दर्ज करें (थाना फीडिंग)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Info banner
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(text = "थाना: ${record.thana}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                        Text(text = "मु0अ0सं0: ${record.caseNumber}", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                        Text(text = "पंजीकरण दिनांक: ${record.registrationDate}", style = MaterialTheme.typography.labelSmall)
                    }
                }

                // SID Number Field
                OutlinedTextField(
                    value = sidInput,
                    onValueChange = {
                        sidInput = it
                        errorMessage = null
                    },
                    label = { Text("SID संख्या (SID Number) *") },
                    placeholder = { Text("उदा. UP/2026/049182") },
                    leadingIcon = { Icon(Icons.Default.Fingerprint, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_sid_number"),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters)
                )

                // SID Date Field
                OutlinedTextField(
                    value = sidDateInput,
                    onValueChange = { sidDateInput = it },
                    label = { Text("SID बनाने की तिथि (DD/MM/YYYY) *") },
                    leadingIcon = { Icon(Icons.Default.PendingActions, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_sid_date"),
                    singleLine = true
                )

                // Officer Name Field
                OutlinedTextField(
                    value = officerInput,
                    onValueChange = { officerInput = it },
                    label = { Text("फीड करने वाले कर्मी का नाम / पद") },
                    placeholder = { Text("उदा. HC कुलदीप शर्मा / ऑपरेटर") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_officer_name"),
                    singleLine = true
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (sidInput.isBlank()) {
                        errorMessage = "कृपया वैध SID संख्या दर्ज करें"
                        return@Button
                    }
                    if (sidDateInput.isBlank()) {
                        errorMessage = "कृपया SID तिथि दर्ज करें"
                        return@Button
                    }
                    onSubmit(sidInput.trim(), sidDateInput.trim(), officerInput.trim())
                },
                modifier = Modifier.testTag("confirm_submit_sid_btn")
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("सुरक्षित करें (Save & Sync)")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("रद्द करें")
            }
        }
    )
}

@Composable
fun AddNewCaseDialog(
    thana: String,
    officerName: String,
    onDismiss: () -> Unit,
    onSubmit: (caseNo: String, regDate: String, sidNo: String, sidDate: String, officer: String) -> Unit
) {
    val todayFormatted = remember { SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH).format(Date()) }
    var caseNoInput by remember { mutableStateOf("") }
    var regDateInput by remember { mutableStateOf(todayFormatted) }
    var sidInput by remember { mutableStateOf("") }
    var sidDateInput by remember { mutableStateOf(todayFormatted) }
    var officerInput by remember { mutableStateOf(officerName) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("नया केस व SID प्रविष्टि ($thana)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = caseNoInput,
                    onValueChange = {
                        caseNoInput = it
                        errorMsg = null
                    },
                    label = { Text("मु0अ0सं0 (FIR No.) *") },
                    placeholder = { Text("उदा. 124/2026") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = regDateInput,
                    onValueChange = { regDateInput = it },
                    label = { Text("पंजीकरण दिनांक (DD/MM/YYYY) *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = sidInput,
                    onValueChange = { sidInput = it },
                    label = { Text("SID संख्या (यदि उपलब्ध हो)") },
                    placeholder = { Text("उदा. UP/2026/059124") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (sidInput.isNotBlank()) {
                    OutlinedTextField(
                        value = sidDateInput,
                        onValueChange = { sidDateInput = it },
                        label = { Text("SID बनाने की तिथि") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = officerInput,
                    onValueChange = { officerInput = it },
                    label = { Text("ऑपरेटर कर्मी का नाम") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (errorMsg != null) {
                    Text(text = errorMsg ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (caseNoInput.isBlank()) {
                        errorMsg = "कृपया मु0अ0सं0 भरें"
                        return@Button
                    }
                    onSubmit(caseNoInput.trim(), regDateInput.trim(), sidInput.trim(), sidDateInput.trim(), officerInput.trim())
                }
            ) {
                Text("जोड़ें व सुरक्षित करें")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("रद्द करें")
            }
        }
    )
}
