package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.HistoryItem
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query("SELECT * FROM processing_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<HistoryItem>>

    @Query("SELECT * FROM processing_history ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentHistory(limit: Int): Flow<List<HistoryItem>>

    @Query("SELECT * FROM processing_history WHERE operation LIKE '%' || :filter || '%' ORDER BY timestamp DESC")
    fun getHistoryFiltered(filter: String): Flow<List<HistoryItem>>

    @Query("SELECT COUNT(*) FROM processing_history")
    fun getHistoryCount(): Flow<Int>

    @Query("SELECT SUM(CASE WHEN originalSizeBytes > outputSizeBytes THEN originalSizeBytes - outputSizeBytes ELSE 0 END) FROM processing_history")
    fun getTotalSpaceSaved(): Flow<Long?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(item: HistoryItem): Long

    @Query("DELETE FROM processing_history WHERE id = :id")
    suspend fun deleteHistory(id: Long)

    @Query("DELETE FROM processing_history WHERE id IN (:ids)")
    suspend fun deleteMultipleHistory(ids: List<Long>)

    @Query("DELETE FROM processing_history")
    suspend fun clearAllHistory()

    @androidx.room.Update
    suspend fun updateHistory(item: HistoryItem)

    @Query("UPDATE processing_history SET fileName = :newName, outputPath = :newPath WHERE id = :id")
    suspend fun updateFileNameAndPath(id: Long, newName: String, newPath: String)

    @Query("SELECT SUM(originalSizeBytes) FROM processing_history WHERE operation LIKE '%Compress%'")
    fun getTotalCompressOriginalBytes(): Flow<Long?>

    @Query("SELECT SUM(outputSizeBytes) FROM processing_history WHERE operation LIKE '%Compress%'")
    fun getTotalCompressOutputBytes(): Flow<Long?>
}
