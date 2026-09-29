package com.example.ui.screens

import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocalPolice
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SheetFeedInfo
import com.example.ui.theme.PrimaryBlue
import com.example.ui.viewmodel.UiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun FeedManagerScreen(
    uiState: UiState,
    onFeedNewSheet: (String, String) -> Unit,
    onSelectFeed: (SheetFeedInfo) -> Unit,
    onSubmitNewRecord: (thana: String, caseNo: String, regDate: String, sid: String, sidDate: String, remarks: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Feed Google Sheet, 1: Submit Entry

    // Feed Sheet Form State
    var sheetUrlInput by remember { mutableStateOf("") }
    var sheetNameInput by remember { mutableStateOf("") }

    // Entry Form State
    var thanaInput by remember { mutableStateOf("अमेठी") }
    var caseNoInput by remember { mutableStateOf("") }
    val currentDateStr = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()) }
    var regDateInput by remember { mutableStateOf(currentDateStr) }
    var sidInput by remember { mutableStateOf("") }
    var sidDateInput by remember { mutableStateOf("") }
    var remarksInput by remember { mutableStateOf("") }
    var stationDropdownOpen by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("feed_manager_screen")
    ) {
        PrimaryTabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Feed Google Sheet") },
                icon = { Icon(Icons.Default.CloudDownload, contentDescription = null) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Submit Public Entry") },
                icon = { Icon(Icons.Default.AddCircle, contentDescription = null) }
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (selectedTab == 0) {
                // Tab 1: Connect & Feed Google Sheets

                // Active Connected Sheet Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
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
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF10B981))
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Active Connected Sheet",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = "${uiState.summary.totalRecords} Records",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = uiState.currentFeed.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = uiState.currentFeed.originalUrl,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }

                // Add / Feed New Sheet Form Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
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
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Feed New Public Google Sheet",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Feed in any public Google Sheet link or Sheet ID. The app will fetch, cache, and build interactive charts automatically.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Quick preset shortcuts
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        sheetUrlInput = "https://docs.google.com/spreadsheets/d/1SIFbfWSvXX7LcSrXZoNtOFn49OJIExcr-QyZFAD25WM/edit?usp=drivesdk"
                                        sheetNameInput = "Amethi District Police Registry"
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Preset: Amethi Registry", fontSize = 11.sp, maxLines = 1)
                                }

                                OutlinedButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = clipboard.primaryClip?.getItemAt(0)?.text?.toString()
                                        if (!clip.isNullOrBlank()) {
                                            sheetUrlInput = clip
                                            Toast.makeText(context, "Pasted from clipboard", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Paste", fontSize = 11.sp)
                                }
                            }

                            OutlinedTextField(
                                value = sheetUrlInput,
                                onValueChange = { sheetUrlInput = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("feed_url_input"),
                                label = { Text("Google Sheet URL or ID") },
                                placeholder = { Text("https://docs.google.com/spreadsheets/d/...") },
                                leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )

                            OutlinedTextField(
                                value = sheetNameInput,
                                onValueChange = { sheetNameInput = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("feed_name_input"),
                                label = { Text("Feed Name / Label (Optional)") },
                                placeholder = { Text("e.g. Police Registry 2026") },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )

                            Button(
                                onClick = {
                                    onFeedNewSheet(sheetUrlInput, sheetNameInput)
                                },
                                enabled = sheetUrlInput.isNotBlank() && !uiState.isLoading,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("connect_feed_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                if (uiState.isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Fetching & Parsing Sheet...")
                                } else {
                                    Icon(imageVector = Icons.Default.CloudDownload, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Connect & Feed Sheet")
                                }
                            }
                        }
                    }
                }

                // Public Sharing Guide Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Public,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "How to share a Google Sheet publicly",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Text(
                                text = "1. Open your spreadsheet on Google Sheets.\n" +
                                        "2. Click the 'Share' button in the top-right corner.\n" +
                                        "3. Under 'General access', change from 'Restricted' to 'Anyone with the link'.\n" +
                                        "4. Copy the link and paste it into the input above.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                // Saved Feeds History
                if (uiState.savedFeeds.isNotEmpty()) {
                    item {
                        Text(
                            text = "Saved Feeds",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    items(uiState.savedFeeds) { feed ->
                        val isCurrent = feed.id == uiState.currentFeed.id
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectFeed(feed) },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                else MaterialTheme.colorScheme.surface
                            ),
                            border = if (isCurrent) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = feed.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${feed.rowCount} rows cached",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (isCurrent) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.primary
                                    ) {
                                        Text(
                                            text = "Active",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                } else {
                                    OutlinedButton(
                                        onClick = { onSelectFeed(feed) },
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Select")
                                    }
                                }
                            }
                        }
                    }
                }

            } else {
                // Tab 2: Public Entry Submission Form
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
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
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = "Submit Public Record Entry",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Feed a new FIR or SID record to the public registry. It will be stored locally and update all visualizations immediately.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Police Station Dropdown Selector
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = thanaInput,
                                    onValueChange = { thanaInput = it },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("entry_thana_input"),
                                    label = { Text("थाना (Police Station)") },
                                    trailingIcon = {
                                        IconButton(onClick = { stationDropdownOpen = true }) {
                                            Icon(Icons.Default.LocalPolice, contentDescription = "Pick Station")
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                )

                                DropdownMenu(
                                    expanded = stationDropdownOpen,
                                    onDismissRequest = { stationDropdownOpen = false }
                                ) {
                                    val stations = listOf(
                                        "अमेठी", "गौरीगंज", "मोहनगंज", "जगदीशपुर", "मुसाफिरखाना",
                                        "जामो", "बाजारशुक्ल", "कमरौली", "संग्रामपुर", "जायस",
                                        "पीपरपुर", "इन्हौना", "फुरसतगंज", "मुंशीगंज", "रामगंज", "भाले सुल्तान"
                                    )
                                    stations.forEach { stn ->
                                        DropdownMenuItem(
                                            text = { Text(stn) },
                                            onClick = {
                                                thanaInput = stn
                                                stationDropdownOpen = false
                                            }
                                        )
                                    }
                                }
                            }

                            // Case / FIR number
                            OutlinedTextField(
                                value = caseNoInput,
                                onValueChange = { caseNoInput = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("entry_case_input"),
                                label = { Text("मु0अ0सं0 (FIR / Crime Number)") },
                                placeholder = { Text("e.g. 136/26") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )

                            // Registration Date
                            OutlinedTextField(
                                value = regDateInput,
                                onValueChange = { regDateInput = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("entry_date_input"),
                                label = { Text("पंजीकरण का दिनांक (Registration Date)") },
                                placeholder = { Text("DD/MM/YYYY") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )

                            // SID Number
                            OutlinedTextField(
                                value = sidInput,
                                onValueChange = { sidInput = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("entry_sid_input"),
                                label = { Text("SID संख्या (Leave blank if pending)") },
                                placeholder = { Text("e.g. 1741406002558338") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )

                            // SID Creation Date
                            OutlinedTextField(
                                value = sidDateInput,
                                onValueChange = { sidDateInput = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("SID बनाने तिथि (Creation Date)") },
                                placeholder = { Text("DD/MM/YYYY or pending") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )

                            // Remarks
                            OutlinedTextField(
                                value = remarksInput,
                                onValueChange = { remarksInput = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Remarks / Notes (Optional)") },
                                maxLines = 3,
                                shape = RoundedCornerShape(12.dp)
                            )

                            Button(
                                onClick = {
                                    if (thanaInput.isBlank() || caseNoInput.isBlank()) {
                                        Toast.makeText(context, "Please enter Station and FIR number", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }

                                    onSubmitNewRecord(
                                        thanaInput,
                                        caseNoInput,
                                        regDateInput,
                                        sidInput,
                                        sidDateInput,
                                        remarksInput
                                    )

                                    caseNoInput = ""
                                    sidInput = ""
                                    sidDateInput = ""
                                    remarksInput = ""
                                    Toast.makeText(context, "Record Submitted Successfully!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("submit_entry_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Submit Record to Registry")
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
}
