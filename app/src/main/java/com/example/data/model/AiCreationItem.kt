package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ai_creations")
data class AiCreationItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val prompt: String,
    val model: String = "gemini-3.1-flash-image-preview",
    val mode: String, // "CREATE" or "EDIT"
    val localImagePath: String,
    val sourceImagePath: String? = null,
    val aspectRatio: String = "1:1",
    val resolution: String = "1K",
    val timestamp: Long = System.currentTimeMillis(),
    val firestoreDocId: String? = null
)
