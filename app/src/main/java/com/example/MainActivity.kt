package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DynamicFeed
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.LocalPolice
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.LocalPolice
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.UserRoleType
import com.example.ui.screens.AdminMonitoringScreen
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.FeedManagerScreen
import com.example.ui.screens.RecordsScreen
import com.example.ui.screens.ThanaFeedingScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.SheetViewModel

sealed class AppDestination(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    data object AdminMonitor : AppDestination("एडमिन निगरानी", Icons.Filled.Assessment, Icons.Outlined.Assessment, "nav_admin")
    data object ThanaFeeding : AppDestination("थाना फीडिंग", Icons.Filled.LocalPolice, Icons.Outlined.LocalPolice, "nav_thana_feeding")
    data object Records : AppDestination("केस रजिस्ट्री", Icons.Filled.TableChart, Icons.Outlined.TableChart, "nav_records")
    data object Analytics : AppDestination("सांख्यिकी", Icons.Filled.BarChart, Icons.Outlined.BarChart, "nav_analytics")
}

class MainActivity : ComponentActivity() {

    private val viewModel: SheetViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainApp(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(viewModel: SheetViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val filteredRecords by viewModel.filteredRecords.collectAsStateWithLifecycle()
    val starredCases by viewModel.starredCases.collectAsStateWithLifecycle()
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val stationProgressList by viewModel.stationProgressList.collectAsStateWithLifecycle()
    val recentFeedings by viewModel.recentFeedings.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var currentNavIndex by remember { mutableIntStateOf(0) }
    var showAccountDialog by remember { mutableStateOf(false) }
    var showRoleSwitchDialog by remember { mutableStateOf(false) }

    val destinations = remember {
        listOf(
            AppDestination.AdminMonitor,
            AppDestination.ThanaFeeding,
            AppDestination.Records,
            AppDestination.Analytics
        )
    }

    // BackHandler: Return to Admin (index 0) if on other screens
    BackHandler(enabled = currentNavIndex != 0) {
        currentNavIndex = 0
    }

    // Show Snackbar on userMessage change
    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearUserMessage()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.DynamicFeed,
                                    contentDescription = "Logo",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "E-Sakshya Feeding",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF10B981))
                                )
                            }
                            Text(
                                text = uiState.currentFeed.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                actions = {
                    // Role Switcher Badge
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (uiState.currentRole == UserRoleType.ADMIN) Color(0xFFFEF3C7) else Color(0xFFE0F2FE),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (uiState.currentRole == UserRoleType.ADMIN) Color(0xFFF59E0B) else Color(0xFF38BDF8)
                        ),
                        modifier = Modifier
                            .clickable { showRoleSwitchDialog = true }
                            .padding(end = 4.dp)
                            .testTag("role_badge_switcher")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (uiState.currentRole == UserRoleType.ADMIN) "👑 एडमिन" else "👮 ${uiState.activeThanaForFeeding}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (uiState.currentRole == UserRoleType.ADMIN) Color(0xFFB45309) else Color(0xFF0369A1),
                                fontSize = 11.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = { viewModel.refreshActiveFeed() },
                        enabled = !uiState.isRefreshing,
                        modifier = Modifier.testTag("app_bar_refresh_button")
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
                                contentDescription = "Refresh Sheet",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Account & Firebase Cloud Sync button
                    IconButton(
                        onClick = { showAccountDialog = true },
                        modifier = Modifier.testTag("app_bar_account_button")
                    ) {
                        if (uiState.currentUserProfile != null) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(30.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = (uiState.currentUserProfile?.displayName?.take(1) ?: "U").uppercase(),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = "Firebase Account & Cloud Sync",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp
            ) {
                destinations.forEachIndexed { index, destination ->
                    val selected = currentNavIndex == index
                    NavigationBarItem(
                        selected = selected,
                        onClick = { currentNavIndex = index },
                        icon = {
                            Icon(
                                imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
                                contentDescription = destination.title
                            )
                        },
                        label = {
                            Text(
                                text = destination.title,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 11.sp
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag(destination.testTag)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentNavIndex,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "ScreenTransition"
            ) { targetIndex ->
                when (targetIndex) {
                    0 -> AdminMonitoringScreen(
                        uiState = uiState,
                        stationProgressList = stationProgressList,
                        recentFeedings = recentFeedings,
                        onSelectStationToFeed = { thana ->
                            viewModel.setActiveThanaForFeeding(thana)
                            viewModel.setRole(UserRoleType.THANA_OFFICER)
                            currentNavIndex = 1
                        },
                        onNavigateToRecordsForStation = { thana ->
                            viewModel.setSelectedThana(thana)
                            currentNavIndex = 2
                        }
                    )

                    1 -> ThanaFeedingScreen(
                        uiState = uiState,
                        records = filteredRecords,
                        stationProgressList = stationProgressList,
                        onSelectThana = { thana ->
                            viewModel.setActiveThanaForFeeding(thana)
                        },
                        onUpdateOfficerName = { name ->
                            viewModel.setOfficerName(name)
                        },
                        onSubmitSid = { caseNo, thana, regDate, sidNo, sidDate, officer ->
                            viewModel.updateCaseSidByThana(caseNo, thana, regDate, sidNo, sidDate, officer)
                        }
                    )

                    2 -> RecordsScreen(
                        uiState = uiState,
                        records = filteredRecords,
                        starredCases = starredCases,
                        onSearchQueryChange = { viewModel.setSearchQuery(it) },
                        onSearchScopeChange = { viewModel.setSearchScope(it) },
                        onThanaFilter = { viewModel.setSelectedThana(it) },
                        onStatusFilter = { viewModel.setStatusFilter(it) },
                        onMonthFilter = { viewModel.setSelectedMonth(it) },
                        onSortOrderChange = { viewModel.setSortOrder(it) },
                        onToggleStar = { viewModel.toggleStarCase(it) },
                        onToggleStarredOnly = { viewModel.setStarredOnly(it) },
                        onClearFilters = { viewModel.clearFilters() },
                        onExportCsv = { viewModel.getExportCsv() }
                    )

                    3 -> AnalyticsScreen(
                        uiState = uiState,
                        onThanaFilter = { thana ->
                            viewModel.setSelectedThana(thana)
                        },
                        onNavigateToRecords = { currentNavIndex = 2 }
                    )
                }
            }
        }
    }

    if (showAccountDialog) {
        AccountSyncDialog(
            currentUser = uiState.currentUserProfile,
            starredCount = starredCases.size,
            onSignInGoogle = { viewModel.signInWithGoogle() },
            onSignInAnonymous = { viewModel.signInAnonymously() },
            onSignOut = { viewModel.signOut() },
            onDismiss = { showAccountDialog = false }
        )
    }

    if (showRoleSwitchDialog) {
        RoleSwitchDialog(
            currentRole = uiState.currentRole,
            activeThana = uiState.activeThanaForFeeding,
            stationList = stationProgressList.map { it.thana },
            onSelectAdmin = {
                viewModel.setRole(UserRoleType.ADMIN)
                currentNavIndex = 0
                showRoleSwitchDialog = false
            },
            onSelectThana = { thana ->
                viewModel.setActiveThanaForFeeding(thana)
                viewModel.setRole(UserRoleType.THANA_OFFICER)
                currentNavIndex = 1
                showRoleSwitchDialog = false
            },
            onDismiss = { showRoleSwitchDialog = false }
        )
    }
}

@Composable
fun AccountSyncDialog(
    currentUser: com.example.data.firebase.AppUserProfile?,
    starredCount: Int,
    onSignInGoogle: () -> Unit,
    onSignInAnonymous: () -> Unit,
    onSignOut: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CloudDone,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Firebase Auth & Cloud Sync",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (currentUser != null) {
                    // Authenticated state
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = (currentUser.displayName?.take(1) ?: "U").uppercase(),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = currentUser.displayName ?: "Authenticated User",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = currentUser.email ?: if (currentUser.isAnonymous) "Guest Session" else "Cloud Identified",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }

                    // Firestore sync status
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = Color(0xFFF59E0B),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Firestore Starred Cases: $starredCount synced",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Text(
                                text = "Real-time bidirectional synchronization with Cloud Firestore is active.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            onSignOut()
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sign Out")
                    }
                } else {
                    // Unauthenticated state
                    Text(
                        text = "Sign in to securely identify your session and synchronize starred cases and FIR submissions with Cloud Firestore.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = {
                            onSignInGoogle()
                            onDismiss()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sign in with Google")
                    }

                    OutlinedButton(
                        onClick = {
                            onSignInAnonymous()
                            onDismiss()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Continue as Guest (Anonymous)")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun RoleSwitchDialog(
    currentRole: UserRoleType,
    activeThana: String,
    stationList: List<String>,
    onSelectAdmin: () -> Unit,
    onSelectThana: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val defaultStations = remember(stationList) {
        if (stationList.isNotEmpty()) stationList
        else listOf("को0नगर", "सिविल लाइन्स", "कैण्ट", "सुभाषनगर", "प्रेमनगर", "किला", "बारादरी", "सीबीगंज", "इज्जतनगर")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("भूमिका चुनें (Role Selection)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "आप एडमिन के रूप में सभी थानों की प्रगति देख सकते हैं या किसी भी थाने का ऑपरेटर बनकर SID दर्ज कर सकते हैं:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Admin Option Card
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (currentRole == UserRoleType.ADMIN) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectAdmin() }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "👑", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "मुख्यालय / एडमिन (समस्त थाने)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "सभी थानों की लाइव फीडिंग, प्रगति रिपोर्ट व नियंत्रण",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Text(
                    text = "या थाना ऑपरेटर बनें (SID फीडिंग):",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )

                // Quick List of Stations to pick
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(defaultStations, key = { it }) { thana ->
                        val isSelected = currentRole == UserRoleType.THANA_OFFICER && activeThana == thana
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectThana(thana) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.LocalPolice,
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "थाना $thana",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                                Text("फीड करें ➔", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("बंद करें")
            }
        }
    )
}
