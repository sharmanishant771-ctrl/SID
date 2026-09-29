package com.example.data.firebase

data class AppUserProfile(
    val uid: String = "",
    val email: String? = null,
    val displayName: String? = null,
    val photoUrl: String? = null,
    val isAnonymous: Boolean = false,
    val isCloudConnected: Boolean = true
)

data class StarredCase(
    val id: String = "",
    val caseNumber: String = "",
    val thana: String = "",
    val thanaEn: String = "",
    val sidNumber: String = "",
    val registrationDate: String = "",
    val isPending: Boolean = false,
    val savedAt: Long = System.currentTimeMillis()
)

data class CloudSubmission(
    val id: String = "",
    val userId: String = "",
    val userEmail: String? = null,
    val thana: String = "",
    val caseNumber: String = "",
    val registrationDate: String = "",
    val sidNumber: String = "",
    val sidCreationDate: String = "",
    val remarks: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
