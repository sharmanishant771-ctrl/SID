package com.example.data.model

enum class UserRoleType(val label: String, val hindiLabel: String) {
    ADMIN("Admin / Headquarters", "मुख्यालय / एडमिन (समस्त थाने)"),
    THANA_OFFICER("Station In-Charge", "थाना ऑपरेटर (SID फीडिंग)")
}

data class StationProgress(
    val thana: String,
    val thanaEn: String,
    val totalCases: Int,
    val completedSids: Int,
    val pendingSids: Int,
    val completionPercentage: Float,
    val lastUpdated: Long = System.currentTimeMillis()
)

data class SidFeedingEntry(
    val id: String = "",
    val thana: String = "",
    val caseNumber: String = "",
    val registrationDate: String = "",
    val sidNumber: String = "",
    val sidCreationDate: String = "",
    val officerName: String = "",
    val officerRole: String = "थाना पैरोकार / कंप्यूटर ऑपरेटर",
    val timestamp: Long = System.currentTimeMillis()
)
