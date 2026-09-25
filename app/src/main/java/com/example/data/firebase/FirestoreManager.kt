package com.example.data.firebase

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.model.AiCreationItem
import com.example.data.model.HistoryItem
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class FirestoreManager(private val context: Context) {

    private val firestore: FirebaseFirestore by lazy {
        try {
            val dbId = context.getString(R.string.firestore_database_id)
            if (dbId.isNotBlank() && dbId != "(default)") {
                FirebaseFirestore.getInstance(FirebaseApp.getInstance(), dbId)
            } else {
                FirebaseFirestore.getInstance()
            }
        } catch (e: Exception) {
            Log.w("FirestoreManager", "Fallback to default Firestore instance: ${e.message}")
            FirebaseFirestore.getInstance()
        }
    }

    private val _cloudHistoryCount = MutableStateFlow(0)
    val cloudHistoryCount: StateFlow<Int> = _cloudHistoryCount.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private var historyListener: ListenerRegistration? = null

    fun attachUserListeners(userId: String, onHistoryUpdate: (List<HistoryItem>) -> Unit = {}) {
        historyListener?.remove()
        try {
            historyListener = firestore.collection("users")
                .document(userId)
                .collection("history")
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Log.w("FirestoreManager", "Listen failed.", e)
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        _cloudHistoryCount.value = snapshot.size()
                        val items = snapshot.documents.mapNotNull { doc ->
                            try {
                                val data = doc.data ?: return@mapNotNull null
                                HistoryItem(
                                    id = (data["localId"] as? Long) ?: doc.id.hashCode().toLong(),
                                    fileName = data["fileName"] as? String ?: "File",
                                    operation = data["operation"] as? String ?: "Processed",
                                    originalSizeBytes = (data["originalSizeBytes"] as? Long) ?: 0L,
                                    outputSizeBytes = (data["outputSizeBytes"] as? Long) ?: 0L,
                                    originalFormat = data["originalFormat"] as? String ?: "",
                                    targetFormat = data["targetFormat"] as? String ?: "",
                                    outputPath = data["outputPath"] as? String ?: "",
                                    status = data["status"] as? String ?: "Completed",
                                    details = data["details"] as? String ?: "",
                                    timestamp = (data["timestamp"] as? Long) ?: System.currentTimeMillis()
                                )
                            } catch (parseEx: Exception) {
                                null
                            }
                        }
                        onHistoryUpdate(items)
                    }
                }
        } catch (e: Exception) {
            Log.e("FirestoreManager", "Failed to attach listener: ${e.message}", e)
        }
    }

    fun detachUserListeners() {
        historyListener?.remove()
        historyListener = null
        _cloudHistoryCount.value = 0
    }

    suspend fun saveHistoryToCloud(userId: String, item: HistoryItem) = withContext(Dispatchers.IO) {
        try {
            _isSyncing.value = true
            val docId = "${item.timestamp}_${item.id}"
            val map = hashMapOf(
                "localId" to item.id,
                "fileName" to item.fileName,
                "operation" to item.operation,
                "originalSizeBytes" to item.originalSizeBytes,
                "outputSizeBytes" to item.outputSizeBytes,
                "originalFormat" to item.originalFormat,
                "targetFormat" to item.targetFormat,
                "outputPath" to item.outputPath,
                "status" to item.status,
                "details" to item.details,
                "timestamp" to item.timestamp,
                "savedBytes" to item.spaceSavedBytes,
                "savedPercentage" to item.savedPercentage
            )

            firestore.collection("users")
                .document(userId)
                .collection("history")
                .document(docId)
                .set(map, SetOptions.merge())
                .await()
        } catch (e: Exception) {
            Log.e("FirestoreManager", "Failed to save history item to Firestore: ${e.message}", e)
        } finally {
            _isSyncing.value = false
        }
    }

    suspend fun saveAiCreationToCloud(userId: String, item: AiCreationItem): String? = withContext(Dispatchers.IO) {
        try {
            _isSyncing.value = true
            val docRef = firestore.collection("users")
                .document(userId)
                .collection("ai_creations")
                .document()

            val map = hashMapOf(
                "prompt" to item.prompt,
                "model" to item.model,
                "mode" to item.mode,
                "aspectRatio" to item.aspectRatio,
                "resolution" to item.resolution,
                "timestamp" to item.timestamp,
                "localImagePath" to item.localImagePath
            )

            docRef.set(map).await()
            docRef.id
        } catch (e: Exception) {
            Log.e("FirestoreManager", "Failed to save AI creation to Firestore: ${e.message}", e)
            null
        } finally {
            _isSyncing.value = false
        }
    }

    suspend fun syncBatchHistoryToCloud(userId: String, items: List<HistoryItem>) = withContext(Dispatchers.IO) {
        if (items.isEmpty()) return@withContext
        try {
            _isSyncing.value = true
            val batch = firestore.batch()
            val collection = firestore.collection("users").document(userId).collection("history")
            for (item in items.take(50)) {
                val docId = "${item.timestamp}_${item.id}"
                val docRef = collection.document(docId)
                val map = hashMapOf(
                    "localId" to item.id,
                    "fileName" to item.fileName,
                    "operation" to item.operation,
                    "originalSizeBytes" to item.originalSizeBytes,
                    "outputSizeBytes" to item.outputSizeBytes,
                    "originalFormat" to item.originalFormat,
                    "targetFormat" to item.targetFormat,
                    "outputPath" to item.outputPath,
                    "status" to item.status,
                    "details" to item.details,
                    "timestamp" to item.timestamp,
                    "savedBytes" to item.spaceSavedBytes,
                    "savedPercentage" to item.savedPercentage
                )
                batch.set(docRef, map, SetOptions.merge())
            }
            batch.commit().await()
        } catch (e: Exception) {
            Log.e("FirestoreManager", "Batch sync failed: ${e.message}", e)
        } finally {
            _isSyncing.value = false
        }
    }

    suspend fun saveUserPreferences(userId: String, prefs: Map<String, Any>) = withContext(Dispatchers.IO) {
        try {
            firestore.collection("users")
                .document(userId)
                .collection("profile")
                .document("settings")
                .set(prefs, SetOptions.merge())
                .await()
        } catch (e: Exception) {
            Log.e("FirestoreManager", "Failed to save user settings: ${e.message}", e)
        }
    }
}
