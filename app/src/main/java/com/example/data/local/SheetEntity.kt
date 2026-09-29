package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sheet_records")
data class SheetRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val feedId: String,
    val serialNo: String,
    val thana: String,
    val thanaEn: String,
    val caseNumber: String,
    val registrationDate: String,
    val sidNumber: String,
    val sidCreationDate: String,
    val isPending: Boolean,
    val remarks: String,
    val rawCsvLine: String,
    val isUserSubmitted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "saved_feeds")
data class SavedFeedEntity(
    @PrimaryKey val id: String,
    val name: String,
    val originalUrl: String,
    val csvExportUrl: String,
    val isDefault: Boolean,
    val rowCount: Int,
    val lastSyncTime: Long
)
