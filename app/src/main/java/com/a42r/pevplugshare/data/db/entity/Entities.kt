package com.a42r.pevplugshare.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "locations")
data class LocationEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String? = null,
    val latitude: Double,
    val longitude: Double,
    val socketTypes: String,
    val publicPrivate: String,
    val hasFee: Boolean = false,
    val gpoCount: Int,
    val openingHours: String? = null,
    val additionalInfo: String? = null,
    val providerName: String? = null,
    val providerWebsite: String? = null,
    val chargingCosts: String? = null,
    val color: String,
    val icon: String,
    val lastModified: Long,
    val isFavourite: Boolean = false,
    val syncStatus: String = "SYNCED",
    val imagePaths: String = "", // New field for image paths
    val descriptionHash: String? = null,
    val aiParsed: Boolean = false,
    val aiParsedAt: Long? = null,
    val businessName: String? = null,
    val streetAddress: String? = null,
    val suburb: String? = null,
    val state: String? = null,
    val postcode: String? = null,
    
    // OSMAnd sync tracking fields
    val osmandOriginalName: String? = null,      // Original name from OSMAnd server
    val osmandGpxIndex: Int? = null,             // Last known index in GPX file
    val osmandSyncSource: String = "LOCAL",      // "LOCAL" or "OSMAND"
    val osmandLastSyncedAt: Long? = null,        // Timestamp of last successful sync
    val osmandSyncedToServer: Boolean = false    // True if exists on OSMAnd server
)

@Entity(tableName = "location_photos")
data class PhotoEntity(
    @PrimaryKey
    val id: String,
    val locationId: String,
    val filePath: String,
    val timestamp: Long,
    val exifMetadata: String? = null,
    val gpxData: String? = null,
    val focusRectString: String? = null
)

@Entity(tableName = "sync_state")
data class SyncStateEntity(
    @PrimaryKey
    val id: String = "sync_state",
    val lastSyncTime: Long = 0L,
    val syncStatus: String = "IDLE",
    val pendingCount: Int = 0
)

@Entity(tableName = "user_preferences")
data class UserPreferencesEntity(
    @PrimaryKey
    val id: String = "user_prefs",
    val selectedCategory: String? = null,
    val syncFrequencyHours: Int = 24,
    val osmandAccount: String? = null
)

@Entity(tableName = "pending_locations")
data class PendingLocationEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String? = null,
    val latitude: Double,
    val longitude: Double,
    val socketTypes: String,
    val publicPrivate: String,
    val hasFee: Boolean = false,
    val gpoCount: Int,
    val openingHours: String? = null,
    val additionalInfo: String? = null,
    val providerName: String? = null,
    val providerWebsite: String? = null,
    val chargingCosts: String? = null,
    val createdAt: Long,
    val syncAttempts: Int = 0,
    val lastSyncAttempt: Long? = null
)

@Entity(tableName = "saved_for_later")
data class SavedForLaterEntity(
    @PrimaryKey
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val bearing: Float? = null,
    val photoPaths: String, // Comma-separated photo file paths
    val photoMetadata: String, // JSON-encoded map of path -> metadata
    val createdAt: Long
)
