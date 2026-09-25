package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.AiCreationItem
import kotlinx.coroutines.flow.Flow

@Dao
interface AiCreationDao {
    @Query("SELECT * FROM ai_creations ORDER BY timestamp DESC")
    fun getAllCreations(): Flow<List<AiCreationItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCreation(item: AiCreationItem): Long

    @Query("DELETE FROM ai_creations WHERE id = :id")
    suspend fun deleteCreation(id: Long)

    @Query("DELETE FROM ai_creations")
    suspend fun clearAllCreations()
}
