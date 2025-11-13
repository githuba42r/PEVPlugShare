package com.a42r.pevplugshare.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

enum class PendingOperationType {
    ADD_LOCATION,
    UPDATE_LOCATION,
    DELETE_LOCATION,
    ADD_PHOTO,
    DELETE_PHOTO
}

@Entity(
    tableName = "pending_operations",
    foreignKeys = [
        ForeignKey(
            entity = LocationEntity::class,
            parentColumns = ["id"],
            childColumns = ["locationId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class PendingOperationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val locationId: String,
    val operationType: PendingOperationType,
    val operationData: String,
    val timestamp: Long = System.currentTimeMillis(),
    val retryCount: Int = 0,
    val maxRetries: Int = 3,
    val lastRetryTime: Long? = null,
    val errorMessage: String? = null
)
