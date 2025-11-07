package com.pevplugshare.data.repository

import android.util.Log
import com.pevplugshare.data.db.entity.PendingOperationEntity
import com.pevplugshare.data.db.entity.PendingOperationType
import com.pevplugshare.data.model.Location
import com.pevplugshare.domain.repository.LocationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class SyncState {
    IDLE,
    SYNCING,
    SUCCESS,
    FAILED
}

@Singleton
class SyncManager @Inject constructor(
    private val pendingOperationRepository: PendingOperationRepository,
    private val locationRepository: LocationRepository
) {

    private val _syncState = MutableStateFlow<SyncState>(SyncState.IDLE)
    val syncState: StateFlow<SyncState> = _syncState

    private val _syncProgress = MutableStateFlow(0)
    val syncProgress: StateFlow<Int> = _syncProgress

    private val TAG = "SyncManager"

    suspend fun syncPendingOperations(): Boolean {
        return try {
            _syncState.value = SyncState.SYNCING
            _syncProgress.value = 0

            val retryableOps = pendingOperationRepository.getRetryableOperations()
            if (retryableOps.isEmpty()) {
                _syncState.value = SyncState.SUCCESS
                _syncProgress.value = 100
                return true
            }

            var successCount = 0
            val totalOps = retryableOps.size

            for ((index, operation) in retryableOps.withIndex()) {
                try {
                    when (operation.operationType) {
                        PendingOperationType.ADD_LOCATION -> syncAddLocation(operation)
                        PendingOperationType.UPDATE_LOCATION -> syncUpdateLocation(operation)
                        PendingOperationType.DELETE_LOCATION -> syncDeleteLocation(operation)
                        PendingOperationType.ADD_PHOTO -> syncAddPhoto(operation)
                        PendingOperationType.DELETE_PHOTO -> syncDeletePhoto(operation)
                    }
                    pendingOperationRepository.markOperationAsSuccessful(operation)
                    successCount++
                    Log.d(TAG, "Synced operation: ${operation.operationType}")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to sync operation: ${operation.operationType}", e)
                    pendingOperationRepository.incrementRetryCount(operation, e.message)
                }

                _syncProgress.value = ((index + 1) * 100) / totalOps
            }

            val finalSuccess = successCount == totalOps
            _syncState.value = if (finalSuccess) SyncState.SUCCESS else SyncState.FAILED
            finalSuccess
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed", e)
            _syncState.value = SyncState.FAILED
            false
        }
    }

    private suspend fun syncAddLocation(operation: PendingOperationEntity) {
        // Parse location data from operation and sync
        // This would call the API to add the location
        // For now, just mark the location as synced
    }

    private suspend fun syncUpdateLocation(operation: PendingOperationEntity) {
        // Parse location data from operation and sync
        // This would call the API to update the location
        // For now, just mark the location as synced
    }

    private suspend fun syncDeleteLocation(operation: PendingOperationEntity) {
        // Call API to delete the location
        // For now, just remove the pending operation
    }

    private suspend fun syncAddPhoto(operation: PendingOperationEntity) {
        // Upload photo data
        // For now, just remove the pending operation
    }

    private suspend fun syncDeletePhoto(operation: PendingOperationEntity) {
        // Call API to delete photo
        // For now, just remove the pending operation
    }

    suspend fun queueLocationOperation(
        location: Location,
        operationType: PendingOperationType
    ) {
        try {
            val operationData = location.toJson()
            val operation = PendingOperationEntity(
                locationId = location.id,
                operationType = operationType,
                operationData = operationData
            )
            pendingOperationRepository.addOperation(operation)
            Log.d(TAG, "Queued operation: $operationType for location ${location.id}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to queue operation", e)
        }
    }

    suspend fun hasPendingOperations(): Boolean =
        pendingOperationRepository.hasRetryableOperations()

    suspend fun hasFailedOperations(): Boolean =
        pendingOperationRepository.hasFailedOperations()

    suspend fun getPendingOperationCount(): Int =
        pendingOperationRepository.getAllPendingOperations().size

    suspend fun clearSyncState() {
        _syncState.value = SyncState.IDLE
        _syncProgress.value = 0
    }

    fun resetSyncState() {
        _syncState.value = SyncState.IDLE
        _syncProgress.value = 0
    }
}

private fun Location.toJson(): String {
    return """
        {
            "id": "$id",
            "name": "$name",
            "latitude": $latitude,
            "longitude": $longitude,
            "socketTypes": "${socketTypes.joinToString(",")}",
            "publicPrivate": "${publicPrivate.name}",
            "hasFee": $hasFee,
            "gpoCount": $gpoCount,
            "description": "${description ?: ""}",
            "openingHours": "${openingHours ?: ""}",
            "additionalInfo": "${additionalInfo ?: ""}",
            "providerName": "${providerName ?: ""}",
            "providerWebsite": "${providerWebsite ?: ""}",
            "chargingCosts": "${chargingCosts ?: ""}"
        }
    """.trimIndent()
}
