package com.a42r.pevplugshare.data.repository

import com.a42r.pevplugshare.data.db.dao.PendingOperationDao
import com.a42r.pevplugshare.data.db.entity.PendingOperationEntity
import com.a42r.pevplugshare.data.db.entity.PendingOperationType
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class PendingOperationRepository @Inject constructor(
    private val pendingOperationDao: PendingOperationDao
) {

    suspend fun addOperation(operation: PendingOperationEntity): Long =
        pendingOperationDao.insertOperation(operation)

    suspend fun updateOperation(operation: PendingOperationEntity) =
        pendingOperationDao.updateOperation(operation)

    suspend fun deleteOperation(operation: PendingOperationEntity) =
        pendingOperationDao.deleteOperation(operation)

    suspend fun deleteOperationById(operationId: Long) =
        pendingOperationDao.deleteOperationById(operationId)

    suspend fun getOperationById(operationId: Long): PendingOperationEntity? =
        pendingOperationDao.getOperationById(operationId)

    suspend fun getLocationOperations(locationId: String): List<PendingOperationEntity> =
        pendingOperationDao.getLocationOperations(locationId)

    suspend fun getAllPendingOperations(): List<PendingOperationEntity> =
        pendingOperationDao.getAllPendingOperations()

    suspend fun getRetryableOperations(): List<PendingOperationEntity> =
        pendingOperationDao.getRetryableOperations()

    suspend fun getFailedOperations(): List<PendingOperationEntity> =
        pendingOperationDao.getFailedOperations()

    fun getPendingOperationCount(): Flow<Int> =
        pendingOperationDao.getPendingOperationCount()

    fun getFailedOperationCount(): Flow<Int> =
        pendingOperationDao.getFailedOperationCount()

    suspend fun deleteLocationOperations(locationId: String) =
        pendingOperationDao.deleteLocationOperations(locationId)

    suspend fun deleteOperationsByTypeAndLocation(
        operationType: PendingOperationType,
        locationId: String
    ) = pendingOperationDao.deleteOperationsByTypeAndLocation(operationType, locationId)

    suspend fun getOperationsByType(operationType: PendingOperationType): List<PendingOperationEntity> =
        pendingOperationDao.getOperationsByType(operationType)

    suspend fun getRetryableOperationsByType(operationType: PendingOperationType): List<PendingOperationEntity> =
        pendingOperationDao.getRetryableOperationsByType(operationType)

    suspend fun incrementRetryCount(operation: PendingOperationEntity, errorMessage: String? = null) {
        val updatedOperation = operation.copy(
            retryCount = operation.retryCount + 1,
            lastRetryTime = System.currentTimeMillis(),
            errorMessage = errorMessage
        )
        updateOperation(updatedOperation)
    }

    suspend fun markOperationAsSuccessful(operation: PendingOperationEntity) {
        deleteOperation(operation)
    }

    suspend fun hasFailedOperations(): Boolean =
        getFailedOperations().isNotEmpty()

    suspend fun hasRetryableOperations(): Boolean =
        getRetryableOperations().isNotEmpty()
}
