package com.example.data.repository

import com.example.data.dao.HistoryDao
import com.example.data.model.HistoryItem
import kotlinx.coroutines.flow.Flow

class HistoryRepository(private val historyDao: HistoryDao) {
    val allHistory: Flow<List<HistoryItem>> = historyDao.getAllHistory()

    fun getRecentHistory(limit: Int = 10): Flow<List<HistoryItem>> = historyDao.getRecentHistory(limit)

    fun getHistoryFiltered(filter: String): Flow<List<HistoryItem>> = historyDao.getHistoryFiltered(filter)

    val historyCount: Flow<Int> = historyDao.getHistoryCount()

    val totalSpaceSaved: Flow<Long?> = historyDao.getTotalSpaceSaved()
    val totalCompressOriginalBytes: Flow<Long?> = historyDao.getTotalCompressOriginalBytes()
    val totalCompressOutputBytes: Flow<Long?> = historyDao.getTotalCompressOutputBytes()

    suspend fun insert(item: HistoryItem): Long = historyDao.insertHistory(item)

    suspend fun delete(id: Long) = historyDao.deleteHistory(id)

    suspend fun deleteMultiple(ids: List<Long>) = historyDao.deleteMultipleHistory(ids)

    suspend fun updateFileNameAndPath(id: Long, newName: String, newPath: String) =
        historyDao.updateFileNameAndPath(id, newName, newPath)

    suspend fun update(item: HistoryItem) = historyDao.updateHistory(item)

    suspend fun clearAll() = historyDao.clearAllHistory()
}
