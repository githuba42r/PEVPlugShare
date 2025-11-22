package com.a42r.pevplugshare.data.model

enum class SocketType(val displayName: String) {
    AS3112("AS3112 (AU GPO)"),
    TYPE2("Type2 EV"),
    TESLA("Tesla Charger"),
    CCS2("CCS2/Combo"),
    CHADEMO("CHAdeMO"),
    TYPE1("Type 1 (J1772)"),
    WALL_SOCKET("Wall Socket (15A)"),
    CARAVAN("Caravan Plug"),
    OTHER("Other EV Charger")
}

enum class LocationAccessType(val displayName: String) {
    PUBLIC("Public"),
    PRIVATE("Private")
}

enum class SyncStatus(val value: String) {
    SYNCED("SYNCED"),
    PENDING("PENDING"),
    FAILED("FAILED")
}

enum class OsmandSyncStatus(val value: String, val displayName: String) {
    LOCAL_ONLY("LOCAL_ONLY", "Local Only"),          // Never synced to OSMand
    SYNCED("SYNCED", "Synced"),                      // Up to date with OSMand
    MODIFIED("MODIFIED", "Modified")                 // Local changes not pushed to OSMand
}

data class Location(
    val id: String,
    val name: String,
    val description: String? = null,
    val latitude: Double,
    val longitude: Double,
    val socketTypes: List<SocketType>,
    val publicPrivate: LocationAccessType,
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
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
    val photos: List<Photo> = emptyList(),
    val imagePaths: List<String> = emptyList(),
    val distanceMeters: Double? = null,
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
    val osmandSyncedToServer: Boolean = false,   // True if exists on OSMAnd server
    val osmandSyncedUsernames: String? = null,   // Comma-separated list of OSMand usernames this location has been synced with
    val osmandSyncStatus: OsmandSyncStatus = OsmandSyncStatus.LOCAL_ONLY  // Current sync status relative to OSMand
)

data class Photo(
    val id: String,
    val locationId: String,
    val filePath: String,
    val timestamp: Long,
    val exifMetadata: String? = null,
    val gpxData: String? = null
)

data class SyncState(
    val lastSyncTime: Long = 0L,
    val syncStatus: String = "IDLE",
    val pendingCount: Int = 0
)

data class UserPreferences(
    val selectedCategory: String? = null,
    val syncFrequencyHours: Int = 24,
    val osmandAccount: String? = null
)
