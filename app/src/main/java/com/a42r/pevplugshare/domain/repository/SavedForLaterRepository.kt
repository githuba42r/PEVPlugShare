package com.a42r.pevplugshare.domain.repository

import com.a42r.pevplugshare.data.db.entity.SavedForLaterEntity
import kotlinx.coroutines.flow.Flow

interface SavedForLaterRepository {
    suspend fun insertSavedForLater(savedForLater: SavedForLaterEntity)
    fun getAllSavedForLater(): Flow<List<SavedForLaterEntity>>
    suspend fun getAllSavedForLaterList(): List<SavedForLaterEntity>
    suspend fun getSavedForLaterCount(): Int
    fun getSavedForLaterCountFlow(): Flow<Int>
    suspend fun getSavedForLaterById(id: String): SavedForLaterEntity?
    suspend fun deleteSavedForLater(id: String)
    suspend fun deleteAllSavedForLater()
}
