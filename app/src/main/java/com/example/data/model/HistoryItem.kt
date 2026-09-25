package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "processing_history")
data class HistoryItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fileName: String,
    val operation: String,
    val originalSizeBytes: Long,
    val outputSizeBytes: Long,
    val originalFormat: String,
    val targetFormat: String,
    val outputPath: String,
    val status: String = "Completed",
    val details: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    val spaceSavedBytes: Long
        get() = (originalSizeBytes - outputSizeBytes).coerceAtLeast(0L)

    val savedPercentage: Int
        get() = if (originalSizeBytes > 0 && spaceSavedBytes > 0) {
            ((spaceSavedBytes.toDouble() / originalSizeBytes.toDouble()) * 100).toInt()
        } else {
            0
        }
}
