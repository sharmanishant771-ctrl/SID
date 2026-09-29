package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.AppUserProfile
import com.example.data.firebase.AuthUiState
import com.example.data.firebase.CloudSubmission
import com.example.data.firebase.FirebaseManager
import com.example.data.firebase.StarredCase
import com.example.data.model.ChartType
import com.example.data.model.SheetFeedInfo
import com.example.data.model.SheetRecord
import com.example.data.model.SheetSummary
import com.example.data.model.SidFeedingEntry
import com.example.data.model.StationProgress
import com.example.data.model.ThanaStat
import com.example.data.model.UserRoleType
import com.example.data.parser.CsvParser
import com.example.data.repository.SheetRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class StatusFilter {
    ALL,
    PENDING_ONLY,
    RESOLVED_ONLY
}

enum class SearchScope(val label: String) {
    ALL("All Columns"),
    THANA("Station (थाना)"),
    CASE_NO("FIR (मु0अ0सं0)"),
    SID("SID Number"),
    REG_DATE("Reg. Date")
}

enum class SortOrder(val label: String) {
    DEFAULT("S.No (Default)"),
    PENDING_FIRST("⚠️ Pending First"),
    DATE_DESC("Date (Newest)"),
    DATE_ASC("Date (Oldest)"),
    THANA_ASC("Station (A-Z)")
}

data class UiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val summary: SheetSummary = SheetSummary(),
    val currentFeed: SheetFeedInfo = SheetFeedInfo(),
    val savedFeeds: List<SheetFeedInfo> = emptyList(),
    val searchQuery: String = "",
    val searchScope: SearchScope = SearchScope.ALL,
    val selectedThana: String? = null,
    val selectedMonth: String? = null,
    val statusFilter: StatusFilter = StatusFilter.ALL,
    val sortOrder: SortOrder = SortOrder.DEFAULT,
    val starredOnly: Boolean = false,
    val selectedChartType: ChartType = ChartType.BAR_STATION_CASES,
    val userMessage: String? = null,
    val currentUserProfile: AppUserProfile? = null,
    val currentRole: UserRoleType = UserRoleType.ADMIN,
    val activeThanaForFeeding: String = "को0नगर",
    val officerName: String = "थाना ऑपरेटर"
)

class SheetViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SheetRepository(application.applicationContext)
    private val firebaseManager = FirebaseManager(application.applicationContext)

    val authState: StateFlow<AuthUiState> = firebaseManager.authState
    val starredCases: StateFlow<List<StarredCase>> = firebaseManager.starredCases
    val cloudSubmissions: StateFlow<List<CloudSubmission>> = firebaseManager.listenToPublicSubmissions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentFeedings: StateFlow<List<SidFeedingEntry>> = firebaseManager.listenToSidFeedings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _rawRecords = MutableStateFlow<List<SheetRecord>>(emptyList())

    val stationProgressList: StateFlow<List<StationProgress>> = _rawRecords.map { records ->
        records.groupBy { it.thana }
            .filterKeys { it.isNotBlank() }
            .map { (thana, list) ->
                val total = list.size
                val resolved = list.count { !it.isPending }
                val pending = list.count { it.isPending }
                val pct = if (total > 0) (resolved.toFloat() / total.toFloat()) * 100f else 0f
                val thanaEn = list.firstOrNull()?.thanaEn?.ifBlank { CsvParser.getThanaEnglish(thana) }
                    ?: CsvParser.getThanaEnglish(thana)
                StationProgress(
                    thana = thana,
                    thanaEn = thanaEn,
                    totalCases = total,
                    completedSids = resolved,
                    pendingSids = pending,
                    completionPercentage = pct
                )
            }
            .sortedWith(compareByDescending<StationProgress> { it.pendingSids }.thenBy { it.completionPercentage })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // Filtered and sorted records flow based on query, column scope, station filter, month filter, status, starred filter, and sorting
    val filteredRecords: StateFlow<List<SheetRecord>> = combine(
        _rawRecords,
        _uiState,
        starredCases
    ) { records, state, starred ->
        val starredIds = starred.map { it.id }.toSet()
        val starredCasesSet = starred.map { it.caseNumber.trim().lowercase() }.toSet()

        val filtered = records.filter { record ->
            val matchesStarred = if (state.starredOnly) {
                starredIds.contains(record.id.toString()) || starredCasesSet.contains(record.caseNumber.trim().lowercase())
            } else true

            val matchesQuery = if (state.searchQuery.isBlank()) {
                true
            } else {
                val q = state.searchQuery.trim().lowercase()
                when (state.searchScope) {
                    SearchScope.ALL -> record.thana.lowercase().contains(q) ||
                            record.thanaEn.lowercase().contains(q) ||
                            record.caseNumber.lowercase().contains(q) ||
                            record.sidNumber.lowercase().contains(q) ||
                            record.registrationDate.lowercase().contains(q)
                    SearchScope.THANA -> record.thana.lowercase().contains(q) || record.thanaEn.lowercase().contains(q)
                    SearchScope.CASE_NO -> record.caseNumber.lowercase().contains(q)
                    SearchScope.SID -> record.sidNumber.lowercase().contains(q)
                    SearchScope.REG_DATE -> record.registrationDate.lowercase().contains(q)
                }
            }

            val matchesThana = state.selectedThana == null ||
                    record.thana.equals(state.selectedThana, ignoreCase = true) ||
                    record.thanaEn.equals(state.selectedThana, ignoreCase = true)

            val matchesStatus = when (state.statusFilter) {
                StatusFilter.ALL -> true
                StatusFilter.PENDING_ONLY -> record.isPending
                StatusFilter.RESOLVED_ONLY -> !record.isPending
            }

            val matchesMonth = if (state.selectedMonth == null) {
                true
            } else {
                val parts = record.registrationDate.split("/", "-", ".")
                if (parts.size >= 2) {
                    val m = parts[1].trim().toIntOrNull() ?: 0
                    val y = parts.getOrNull(2) ?: "26"
                    val yr = if (y.length == 4) y.takeLast(2) else y
                    val monthLabel = when (m) {
                        1 -> "Jan"; 2 -> "Feb"; 3 -> "Mar"; 4 -> "Apr"; 5 -> "May"; 6 -> "Jun"
                        7 -> "Jul"; 8 -> "Aug"; 9 -> "Sep"; 10 -> "Oct"; 11 -> "Nov"; 12 -> "Dec"
                        else -> "M$m"
                    } + " '$yr"
                    monthLabel.equals(state.selectedMonth, ignoreCase = true)
                } else false
            }

            matchesStarred && matchesQuery && matchesThana && matchesStatus && matchesMonth
        }

        when (state.sortOrder) {
            SortOrder.DEFAULT -> filtered
            SortOrder.PENDING_FIRST -> filtered.sortedByDescending { it.isPending }
            SortOrder.THANA_ASC -> filtered.sortedBy { it.thanaEn }
            SortOrder.DATE_DESC -> filtered.sortedByDescending { parseDateToLong(it.registrationDate) }
            SortOrder.DATE_ASC -> filtered.sortedBy { parseDateToLong(it.registrationDate) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadInitialData()
        observeSavedFeeds()
        observeAuthStateChanges()
    }

    private fun observeAuthStateChanges() {
        viewModelScope.launch {
            authState.collect { authState ->
                when (authState) {
                    is AuthUiState.Authenticated -> {
                        _uiState.value = _uiState.value.copy(
                            currentUserProfile = authState.profile,
                            userMessage = "Signed in as ${authState.profile.displayName ?: "User"}"
                        )
                    }
                    is AuthUiState.Unauthenticated -> {
                        _uiState.value = _uiState.value.copy(currentUserProfile = null)
                    }
                    is AuthUiState.Error -> {
                        _uiState.value = _uiState.value.copy(userMessage = "Auth Notice: ${authState.message}")
                    }
                    else -> {}
                }
            }
        }
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            // Initialize default sheet from asset if database empty
            val initialSummary = repository.initializeDefaultDataIfNeeded()
            _uiState.value = _uiState.value.copy(
                summary = initialSummary,
                isLoading = false
            )

            // Collect records for default feed
            observeFeedRecords(SheetRepository.DEFAULT_FEED_ID)

            // Attempt silent background network refresh to get latest Google Sheet edits
            refreshActiveFeed(showRefreshingIndicator = false)
        }
    }

    private fun observeSavedFeeds() {
        viewModelScope.launch {
            repository.getSavedFeeds().collect { feeds ->
                _uiState.value = _uiState.value.copy(savedFeeds = feeds)
            }
        }
    }

    private fun observeFeedRecords(feedId: String) {
        viewModelScope.launch {
            repository.getRecordsFlow(feedId).collect { records ->
                _rawRecords.value = records
                // Recompute summary dynamically whenever records change
                recomputeSummaryFromRecords(records)
            }
        }
    }

    private fun recomputeSummaryFromRecords(records: List<SheetRecord>) {
        if (records.isEmpty()) return

        val total = records.size
        val pendingCount = records.count { it.isPending }
        val processedCount = total - pendingCount

        val stationGroups = records.groupBy { it.thana }
        val stationStats = stationGroups.map { (thana, list) ->
            val pCount = list.count { it.isPending }
            val pct = if (total > 0) (list.size.toFloat() / total) * 100f else 0f
            ThanaStat(
                thana = thana,
                thanaEn = CsvParser.getThanaEnglish(thana),
                totalCount = list.size,
                pendingCount = pCount,
                percentage = pct
            )
        }.sortedByDescending { it.totalCount }

        val highestPendency = stationStats.maxByOrNull { it.pendingCount }
        val highestPendencyStation = highestPendency?.thana ?: "बाजारशुक्ल"
        val highestPendencyVal = highestPendency?.pendingCount ?: 14

        val existingNotice = _uiState.value.summary.warningNotice
        val updatedNotice = if (existingNotice.isNotBlank() && existingNotice.contains("⚠️")) {
            existingNotice
        } else {
            "⚠️ WARNING: Total Pending SIDs: $pendingCount | Highest Pendency at $highestPendencyStation ($highestPendencyVal pending SIDs)"
        }

        // Timeline stats by month
        val timelineStats = records
            .filter { it.registrationDate.isNotBlank() }
            .groupBy { record ->
                val parts = record.registrationDate.split("/", "-", ".")
                if (parts.size >= 2) {
                    val m = parts[1].trim().toIntOrNull() ?: 1
                    val y = parts.getOrNull(2) ?: "26"
                    val yr = if (y.length == 4) y.takeLast(2) else y
                    val monthName = when(m) {
                        1 -> "Jan"; 2 -> "Feb"; 3 -> "Mar"; 4 -> "Apr"; 5 -> "May"; 6 -> "Jun"
                        7 -> "Jul"; 8 -> "Aug"; 9 -> "Sep"; 10 -> "Oct"; 11 -> "Nov"; else -> "Dec"
                    }
                    "$monthName '$yr"
                } else {
                    "Other"
                }
            }
            .map { (period, rList) -> com.example.data.model.TimelineStat(period, rList.size) }

        _uiState.value = _uiState.value.copy(
            summary = _uiState.value.summary.copy(
                totalRecords = total,
                totalPending = pendingCount,
                totalProcessed = processedCount,
                totalStations = stationStats.size,
                highestPendencyStation = highestPendencyStation,
                highestPendencyCount = highestPendencyVal,
                stationStats = stationStats,
                monthlyStats = timelineStats,
                warningNotice = updatedNotice
            )
        )
    }

    fun refreshActiveFeed(showRefreshingIndicator: Boolean = true) {
        viewModelScope.launch {
            if (showRefreshingIndicator) {
                _uiState.value = _uiState.value.copy(isRefreshing = true)
            }

            val current = _uiState.value.currentFeed
            val result = repository.refreshFeedFromNetwork(
                feedId = current.id,
                feedName = current.name,
                url = current.originalUrl
            )

            if (result.isSuccess) {
                val newSummary = result.getOrThrow()
                _uiState.value = _uiState.value.copy(
                    isRefreshing = false,
                    summary = newSummary,
                    userMessage = "Sheet refreshed successfully (${newSummary.totalRecords} records)"
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isRefreshing = false,
                    userMessage = if (showRefreshingIndicator) {
                        "Notice: ${result.exceptionOrNull()?.localizedMessage ?: "Sync issue, cached data active"}"
                    } else null
                )
            }
        }
    }

    /**
     * User feeds in any new public Google Sheet URL or ID.
     */
    fun feedNewGoogleSheet(url: String, sheetName: String) {
        val cleanUrl = url.trim()
        if (cleanUrl.isBlank()) {
            _uiState.value = _uiState.value.copy(userMessage = "Please enter a valid Google Sheet URL or ID")
            return
        }

        val name = if (sheetName.isNotBlank()) sheetName.trim() else "Custom Sheet Feed"
        val newFeedId = "feed_${System.currentTimeMillis()}"

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, userMessage = "Connecting to Google Sheet...")
            val result = repository.refreshFeedFromNetwork(newFeedId, name, cleanUrl)

            if (result.isSuccess) {
                val summary = result.getOrThrow()
                val newFeed = SheetFeedInfo(
                    id = newFeedId,
                    name = name,
                    originalUrl = cleanUrl,
                    csvExportUrl = cleanUrl,
                    isDefault = false,
                    rowCount = summary.totalRecords,
                    lastSyncTime = System.currentTimeMillis()
                )

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    currentFeed = newFeed,
                    summary = summary,
                    userMessage = "Loaded $name (${summary.totalRecords} records)!"
                )
                observeFeedRecords(newFeedId)
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    userMessage = "Failed: ${result.exceptionOrNull()?.message ?: "Check sheet sharing permissions"}"
                )
            }
        }
    }

    /**
     * Switch active feed back to default or another saved sheet.
     */
    fun selectFeed(feed: SheetFeedInfo) {
        _uiState.value = _uiState.value.copy(currentFeed = feed, isLoading = true)
        observeFeedRecords(feed.id)
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    fun submitNewRecord(
        thana: String,
        caseNumber: String,
        registrationDate: String,
        sidNumber: String,
        sidDate: String,
        remarks: String
    ) {
        viewModelScope.launch {
            val feedId = _uiState.value.currentFeed.id
            repository.addCustomSubmission(
                feedId = feedId,
                thana = thana,
                caseNumber = caseNumber,
                regDate = registrationDate,
                sidNumber = sidNumber,
                sidDate = sidDate,
                remarks = remarks
            )

            // Also persist to Cloud Firestore for cross-device sync
            val cloudResult = firebaseManager.submitRecordToCloud(
                thana = thana,
                caseNumber = caseNumber,
                regDate = registrationDate,
                sidNumber = sidNumber,
                sidDate = sidDate,
                remarks = remarks
            )

            val syncMsg = if (cloudResult.isSuccess) " & synced to Firestore!" else " (Stored locally)"
            _uiState.value = _uiState.value.copy(userMessage = "Record submitted successfully$syncMsg")
        }
    }

    fun toggleStarCase(record: SheetRecord) {
        viewModelScope.launch {
            val isCurrentlyStarred = starredCases.value.any {
                it.id == record.id.toString() || it.caseNumber.equals(record.caseNumber, ignoreCase = true)
            }

            if (isCurrentlyStarred) {
                val res = firebaseManager.unstarCase(record)
                if (res.isSuccess) {
                    _uiState.value = _uiState.value.copy(userMessage = "Removed ${record.caseNumber} from starred cases")
                }
            } else {
                val res = firebaseManager.starCase(record)
                if (res.isSuccess) {
                    _uiState.value = _uiState.value.copy(userMessage = "Starred ${record.caseNumber} to Firestore")
                } else {
                    _uiState.value = _uiState.value.copy(userMessage = res.exceptionOrNull()?.message ?: "Please sign in to save cases")
                }
            }
        }
    }

    fun setStarredOnly(starredOnly: Boolean) {
        _uiState.value = _uiState.value.copy(starredOnly = starredOnly)
    }

    fun signInWithGoogle() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(userMessage = "Connecting with Google Sign-In...")
            val result = firebaseManager.signInWithGoogle()
            if (result.isFailure) {
                _uiState.value = _uiState.value.copy(
                    userMessage = result.exceptionOrNull()?.localizedMessage ?: "Google Sign-In cancelled"
                )
            }
        }
    }

    fun signInAnonymously() {
        viewModelScope.launch {
            val result = firebaseManager.signInAnonymously()
            if (result.isFailure) {
                _uiState.value = _uiState.value.copy(
                    userMessage = result.exceptionOrNull()?.localizedMessage ?: "Sign-in failed"
                )
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            firebaseManager.signOut()
            _uiState.value = _uiState.value.copy(userMessage = "Signed out successfully")
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun setSelectedThana(thana: String?) {
        _uiState.value = _uiState.value.copy(selectedThana = thana)
    }

    fun setStatusFilter(filter: StatusFilter) {
        _uiState.value = _uiState.value.copy(statusFilter = filter)
    }

    fun setChartType(chartType: ChartType) {
        _uiState.value = _uiState.value.copy(selectedChartType = chartType)
    }

    fun setSearchScope(scope: SearchScope) {
        _uiState.value = _uiState.value.copy(searchScope = scope)
    }

    fun setSelectedMonth(month: String?) {
        _uiState.value = _uiState.value.copy(selectedMonth = month)
    }

    fun setSortOrder(sortOrder: SortOrder) {
        _uiState.value = _uiState.value.copy(sortOrder = sortOrder)
    }

    fun clearFilters() {
        _uiState.value = _uiState.value.copy(
            searchQuery = "",
            searchScope = SearchScope.ALL,
            selectedThana = null,
            selectedMonth = null,
            statusFilter = StatusFilter.ALL,
            sortOrder = SortOrder.DEFAULT
        )
    }

    private fun parseDateToLong(dateStr: String): Long {
        return try {
            val parts = dateStr.split("/", "-", ".")
            if (parts.size >= 3) {
                val day = parts[0].trim().toIntOrNull() ?: 1
                val month = parts[1].trim().toIntOrNull() ?: 1
                var year = parts[2].trim().toIntOrNull() ?: 2026
                if (year < 100) year += 2000
                year * 10000L + month * 100L + day
            } else 0L
        } catch (e: Exception) {
            0L
        }
    }

    fun updateCaseSidByThana(
        caseNumber: String,
        thana: String,
        registrationDate: String,
        sidNumber: String,
        sidDate: String,
        officerName: String
    ) {
        viewModelScope.launch {
            val rowsUpdated = repository.updateSidForCase(
                thana = thana,
                caseNumber = caseNumber,
                sidNumber = sidNumber,
                sidDate = sidDate,
                remarks = "SID Fed by $officerName"
            )

            // Record in Firestore for Admin Live Monitor
            val feedingEntry = SidFeedingEntry(
                thana = thana,
                caseNumber = caseNumber,
                registrationDate = registrationDate,
                sidNumber = sidNumber,
                sidCreationDate = sidDate,
                officerName = officerName.ifBlank { "थाना ऑपरेटर" },
                timestamp = System.currentTimeMillis()
            )
            firebaseManager.recordSidFeeding(feedingEntry)

            // Update in-memory records immediately for instant UI feedback
            val currentList = _rawRecords.value.toMutableList()
            val index = currentList.indexOfFirst {
                it.thana.trim().equals(thana.trim(), ignoreCase = true) &&
                        it.caseNumber.trim().equals(caseNumber.trim(), ignoreCase = true)
            }
            if (index != -1) {
                val old = currentList[index]
                currentList[index] = old.copy(
                    sidNumber = sidNumber,
                    sidCreationDate = sidDate,
                    isPending = false,
                    remarks = "Updated by $officerName"
                )
                _rawRecords.value = currentList
                recomputeSummaryFromRecords(currentList)
            }

            _uiState.value = _uiState.value.copy(
                userMessage = "✅ थाना $thana: मु0अ0सं0 $caseNumber का SID $sidNumber दर्ज व सिंक हो गया!"
            )
        }
    }

    fun setRole(role: UserRoleType) {
        _uiState.value = _uiState.value.copy(currentRole = role)
    }

    fun setActiveThanaForFeeding(thana: String) {
        _uiState.value = _uiState.value.copy(activeThanaForFeeding = thana)
    }

    fun setOfficerName(name: String) {
        _uiState.value = _uiState.value.copy(officerName = name)
    }

    fun clearUserMessage() {
        _uiState.value = _uiState.value.copy(userMessage = null)
    }

    fun getExportCsv(): String {
        return repository.exportRecordsToCsv(filteredRecords.value)
    }
}
