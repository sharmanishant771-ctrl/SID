package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.SavedFeedEntity
import com.example.data.local.SheetRecordEntity
import com.example.data.model.SheetFeedInfo
import com.example.data.model.SheetRecord
import com.example.data.model.SheetSummary
import com.example.data.parser.CsvParser
import com.example.data.remote.SheetRemoteDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.InputStream

class SheetRepository(
    private val context: Context,
    private val database: AppDatabase = AppDatabase.getDatabase(context),
    private val remoteDataSource: SheetRemoteDataSource = SheetRemoteDataSource()
) {
    companion object {
        const val DEFAULT_FEED_ID = "default_sheet"
        const val DEFAULT_SHEET_URL = "https://docs.google.com/spreadsheets/d/1SIFbfWSvXX7LcSrXZoNtOFn49OJIExcr-QyZFAD25WM/edit?usp=drivesdk"
    }

    private val recordDao = database.sheetRecordDao()
    private val feedDao = database.savedFeedDao()

    fun getRecordsFlow(feedId: String): Flow<List<SheetRecord>> {
        return recordDao.getAllRecords(feedId).map { entities ->
            entities.map { entity ->
                SheetRecord(
                    id = entity.id,
                    serialNo = entity.serialNo,
                    thana = entity.thana,
                    thanaEn = entity.thanaEn,
                    caseNumber = entity.caseNumber,
                    registrationDate = entity.registrationDate,
                    sidNumber = entity.sidNumber,
                    sidCreationDate = entity.sidCreationDate,
                    isPending = entity.isPending,
                    remarks = entity.remarks,
                    isUserSubmitted = entity.isUserSubmitted
                )
            }
        }
    }

    fun getSavedFeeds(): Flow<List<SheetFeedInfo>> {
        return feedDao.getAllFeeds().map { entities ->
            entities.map {
                SheetFeedInfo(
                    id = it.id,
                    name = it.name,
                    originalUrl = it.originalUrl,
                    csvExportUrl = it.csvExportUrl,
                    isDefault = it.isDefault,
                    rowCount = it.rowCount,
                    lastSyncTime = it.lastSyncTime
                )
            }
        }
    }

    /**
     * Initializes the default sheet from local asset if database is empty,
     * ensuring immediate offline readiness and zero delay on first launch.
     */
    suspend fun initializeDefaultDataIfNeeded(): SheetSummary = withContext(Dispatchers.IO) {
        val count = recordDao.getCount(DEFAULT_FEED_ID)
        if (count == 0) {
            try {
                context.assets.open("default_sheet.csv").use { inputStream ->
                    return@withContext importCsvToDatabase(DEFAULT_FEED_ID, "District Police SID Registry", DEFAULT_SHEET_URL, inputStream, isDefault = true)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        // If already initialized, compute current summary from stored rows
        return@withContext computeCurrentSummary(DEFAULT_FEED_ID)
    }

    /**
     * Fetches live data from Google Sheets for the given feed URL and saves into Room.
     */
    suspend fun refreshFeedFromNetwork(
        feedId: String,
        feedName: String,
        url: String
    ): Result<SheetSummary> = withContext(Dispatchers.IO) {
        try {
            val streamResult = remoteDataSource.fetchCsvStream(url)
            if (streamResult.isFailure) {
                return@withContext Result.failure(streamResult.exceptionOrNull() ?: Exception("Unknown network error"))
            }

            val stream = streamResult.getOrThrow()
            stream.use { inputStream ->
                val summary = importCsvToDatabase(
                    feedId = feedId,
                    feedName = feedName,
                    sheetUrl = url,
                    inputStream = inputStream,
                    isDefault = feedId == DEFAULT_FEED_ID
                )
                Result.success(summary)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Inserts a user-fed/submitted public record directly.
     */
    suspend fun addCustomSubmission(
        feedId: String,
        thana: String,
        caseNumber: String,
        regDate: String,
        sidNumber: String,
        sidDate: String,
        remarks: String
    ): Long = withContext(Dispatchers.IO) {
        val isPending = sidNumber.isBlank() || sidNumber.equals("pending", ignoreCase = true)
        val entity = SheetRecordEntity(
            feedId = feedId,
            serialNo = "NEW-${System.currentTimeMillis() % 10000}",
            thana = thana.trim(),
            thanaEn = CsvParser.getThanaEnglish(thana),
            caseNumber = caseNumber.trim(),
            registrationDate = regDate.trim(),
            sidNumber = sidNumber.trim(),
            sidCreationDate = sidDate.trim(),
            isPending = isPending,
            remarks = remarks.trim(),
            rawCsvLine = "",
            isUserSubmitted = true
        )
        recordDao.insert(entity)
    }

    suspend fun updateSidForCase(
        thana: String,
        caseNumber: String,
        sidNumber: String,
        sidDate: String,
        remarks: String
    ): Int = withContext(Dispatchers.IO) {
        recordDao.updateSidForCase(
            thana = thana.trim(),
            caseNumber = caseNumber.trim(),
            sidNumber = sidNumber.trim(),
            sidDate = sidDate.trim(),
            remarks = remarks.trim()
        )
    }

    suspend fun updateSidById(
        id: Long,
        sidNumber: String,
        sidDate: String
    ): Int = withContext(Dispatchers.IO) {
        recordDao.updateSidById(id, sidNumber.trim(), sidDate.trim())
    }

    suspend fun deleteFeed(feedId: String) = withContext(Dispatchers.IO) {
        recordDao.clearAll(feedId)
        feedDao.deleteFeed(feedId)
    }

    private suspend fun importCsvToDatabase(
        feedId: String,
        feedName: String,
        sheetUrl: String,
        inputStream: InputStream,
        isDefault: Boolean
    ): SheetSummary {
        val rawMatrix = CsvParser.parseCsvStream(inputStream)
        val (summary, records) = CsvParser.parseRecords(rawMatrix)

        // Clear previous fetched records for this feed (keep user-submitted entries if any)
        recordDao.clearFetchedRecords(feedId)

        // Convert to Room entities
        val entities = records.map { record ->
            SheetRecordEntity(
                feedId = feedId,
                serialNo = record.serialNo,
                thana = record.thana,
                thanaEn = record.thanaEn,
                caseNumber = record.caseNumber,
                registrationDate = record.registrationDate,
                sidNumber = record.sidNumber,
                sidCreationDate = record.sidCreationDate,
                isPending = record.isPending,
                remarks = record.remarks,
                rawCsvLine = record.rawColumns.joinToString(","),
                isUserSubmitted = false
            )
        }

        // Chunked insert into Room to keep memory efficient
        entities.chunked(300).forEach { chunk ->
            recordDao.insertAll(chunk)
        }

        // Update saved feeds list
        val exportUrl = SheetRemoteDataSource.normalizeToCsvUrl(sheetUrl)
        feedDao.insertFeed(
            SavedFeedEntity(
                id = feedId,
                name = feedName,
                originalUrl = sheetUrl,
                csvExportUrl = exportUrl,
                isDefault = isDefault,
                rowCount = records.size,
                lastSyncTime = System.currentTimeMillis()
            )
        )

        return summary
    }

    suspend fun computeCurrentSummary(feedId: String): SheetSummary = withContext(Dispatchers.IO) {
        // Fallback if needed from asset
        try {
            context.assets.open("default_sheet.csv").use { inputStream ->
                val rawMatrix = CsvParser.parseCsvStream(inputStream)
                val (summary, _) = CsvParser.parseRecords(rawMatrix)
                return@withContext summary
            }
        } catch (e: Exception) {
            return@withContext SheetSummary()
        }
    }

    /**
     * Generates a shareable CSV representation of currently filtered records.
     */
    fun exportRecordsToCsv(records: List<SheetRecord>): String {
        val sb = StringBuilder()
        sb.append("S.No,Police Station (थाना),Case No (मु0अ0सं0),Reg Date (पंजीकरण),SID Number,SID Date\n")
        records.forEach { r ->
            sb.append("\"${r.serialNo}\",")
            sb.append("\"${r.thana}\",")
            sb.append("\"${r.caseNumber}\",")
            sb.append("\"${r.registrationDate}\",")
            sb.append("\"${r.sidNumber}\",")
            sb.append("\"${r.sidCreationDate}\"\n")
        }
        return sb.toString()
    }
}
