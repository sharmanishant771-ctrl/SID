package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SheetRecordDao {
    @Query("SELECT * FROM sheet_records WHERE feedId = :feedId ORDER BY id ASC")
    fun getAllRecords(feedId: String): Flow<List<SheetRecordEntity>>

    @Query("SELECT * FROM sheet_records WHERE feedId = :feedId AND (thana LIKE '%' || :query || '%' OR thanaEn LIKE '%' || :query || '%' OR caseNumber LIKE '%' || :query || '%' OR sidNumber LIKE '%' || :query || '%') ORDER BY id ASC")
    fun searchRecords(feedId: String, query: String): Flow<List<SheetRecordEntity>>

    @Query("SELECT * FROM sheet_records WHERE feedId = :feedId AND thana = :thana ORDER BY id ASC")
    fun getRecordsByThana(feedId: String, thana: String): Flow<List<SheetRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<SheetRecordEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: SheetRecordEntity): Long

    @Query("DELETE FROM sheet_records WHERE feedId = :feedId AND isUserSubmitted = 0")
    suspend fun clearFetchedRecords(feedId: String)

    @Query("DELETE FROM sheet_records WHERE feedId = :feedId")
    suspend fun clearAll(feedId: String)

    @Query("SELECT COUNT(*) FROM sheet_records WHERE feedId = :feedId")
    suspend fun getCount(feedId: String): Int

    @Query("UPDATE sheet_records SET sidNumber = :sidNumber, sidCreationDate = :sidDate, isPending = 0, remarks = :remarks WHERE (caseNumber = :caseNumber OR caseNumber LIKE '%' || :caseNumber || '%') AND (thana = :thana OR thanaEn = :thana OR thana LIKE '%' || :thana || '%')")
    suspend fun updateSidForCase(thana: String, caseNumber: String, sidNumber: String, sidDate: String, remarks: String): Int

    @Query("UPDATE sheet_records SET sidNumber = :sidNumber, sidCreationDate = :sidDate, isPending = 0 WHERE id = :id")
    suspend fun updateSidById(id: Long, sidNumber: String, sidDate: String): Int
}

@Dao
interface SavedFeedDao {
    @Query("SELECT * FROM saved_feeds ORDER BY isDefault DESC, lastSyncTime DESC")
    fun getAllFeeds(): Flow<List<SavedFeedEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeed(feed: SavedFeedEntity)

    @Query("DELETE FROM saved_feeds WHERE id = :id")
    suspend fun deleteFeed(id: String)
}
