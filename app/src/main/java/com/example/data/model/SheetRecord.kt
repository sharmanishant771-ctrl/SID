package com.example.data.model

data class SheetRecord(
    val id: Long = 0,
    val serialNo: String = "",
    val thana: String = "",
    val thanaEn: String = "",
    val caseNumber: String = "",
    val registrationDate: String = "",
    val sidNumber: String = "",
    val sidCreationDate: String = "",
    val isPending: Boolean = false,
    val remarks: String = "",
    val rawColumns: List<String> = emptyList(),
    val isUserSubmitted: Boolean = false
)

data class SheetSummary(
    val title: String = "Amethi District Police SID Registry",
    val warningNotice: String = "",
    val totalRecords: Int = 0,
    val totalPending: Int = 0,
    val totalProcessed: Int = 0,
    val totalStations: Int = 0,
    val highestPendencyStation: String = "",
    val highestPendencyCount: Int = 0,
    val stationStats: List<ThanaStat> = emptyList(),
    val monthlyStats: List<TimelineStat> = emptyList(),
    val headers: List<String> = emptyList(),
    val lastUpdated: Long = System.currentTimeMillis()
)

data class ThanaStat(
    val thana: String,
    val thanaEn: String,
    val totalCount: Int,
    val pendingCount: Int,
    val percentage: Float
)

data class TimelineStat(
    val periodLabel: String,
    val count: Int
)

data class SheetFeedInfo(
    val id: String = "default_sheet",
    val name: String = "District Police SID Registry",
    val originalUrl: String = "https://docs.google.com/spreadsheets/d/1SIFbfWSvXX7LcSrXZoNtOFn49OJIExcr-QyZFAD25WM/edit?usp=drivesdk",
    val csvExportUrl: String = "https://docs.google.com/spreadsheets/d/1SIFbfWSvXX7LcSrXZoNtOFn49OJIExcr-QyZFAD25WM/export?format=csv",
    val isDefault: Boolean = true,
    val rowCount: Int = 0,
    val lastSyncTime: Long = 0L
)

enum class ChartType {
    BAR_STATION_CASES,
    BAR_STATION_PENDENCY,
    DONUT_DISTRIBUTION,
    DONUT_PENDENCY_STATUS,
    TIMELINE_TREND
}
