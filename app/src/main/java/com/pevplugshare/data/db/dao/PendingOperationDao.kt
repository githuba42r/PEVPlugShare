package com.pevplugshare.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.pevplugshare.data.db.entity.PendingOperationEntity
import com.pevplugshare.data.db.entity.PendingOperationType
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingOperationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOperation(operation: PendingOperationEntity): Long

    @Update
    suspend fun updateOperation(operation: PendingOperationEntity)

    @Delete
    suspend fun deleteOperation(operation: PendingOperationEntity)

    @Query("DELETE FROM pending_operations WHERE id = :operationId")
    suspend fun deleteOperationById(operationId: Long)

    @Query("SELECT * FROM pending_operations WHERE id = :operationId LIMIT 1")
    suspend fun getOperationById(operationId: Long): PendingOperationEntity?

    @Query("SELECT * FROM pending_operations WHERE locationId = :locationId ORDER BY timestamp ASC")
    suspend fun getLocationOperations(locationId: String): List<PendingOperationEntity>

    @Query("SELECT * FROM pending_operations ORDER BY timestamp ASC")
    suspend fun getAllPendingOperations(): List<PendingOperationEntity>

    @Query("SELECT * FROM pending_operations WHERE retryCount < maxRetries ORDER BY timestamp ASC")
    suspend fun getRetryableOperations(): List<PendingOperationEntity>

    @Query("SELECT * FROM pending_operations WHERE retryCount >= maxRetries ORDER BY timestamp DESC")
    suspend fun getFailedOperations(): List<PendingOperationEntity>

    @Query("SELECT COUNT(*) FROM pending_operations")
    fun getPendingOperationCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM pending_operations WHERE retryCount >= maxRetries")
    fun getFailedOperationCount(): Flow<Int>

    @Query("DELETE FROM pending_operations WHERE locationId = :locationId")
    suspend fun deleteLocationOperations(locationId: String)

    @Query("DELETE FROM pending_operations WHERE operationType = :operationType AND locationId = :locationId")
    suspend fun deleteOperationsByTypeAndLocation(
        operationType: PendingOperationType,
        locationId: String
    )

    @Query("SELECT * FROM pending_operations WHERE operationType = :operationType ORDER BY timestamp ASC")
    suspend fun getOperationsByType(operationType: PendingOperationType): List<PendingOperationEntity>

    @Query("SELECT * FROM pending_operations WHERE operationType = :operationType AND retryCount < maxRetries ORDER BY timestamp ASC")
    suspend fun getRetryableOperationsByType(operationType: PendingOperationType): List<PendingOperationEntity>
}
