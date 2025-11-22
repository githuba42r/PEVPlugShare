# OSMAnd Sync Tracking - Database Schema Enhancement

## Problem Statement

The current database schema lacks critical fields needed to properly track OSMAnd sync state, causing:

1. **Lost Original Name**: When users rename locations, we lose the original OSMAnd name needed for updates
2. **Inefficient Sync**: Must download entire GPX file to determine if location exists on OSMAnd
3. **No Source Tracking**: Can't distinguish between locations created locally vs synced from OSMAnd
4. **No Index Tracking**: Must recalculate GPX index every time

## Proposed Solution

Add the following fields to `LocationEntity`:

```kotlin
@Entity(tableName = "locations")
data class LocationEntity(
    // ... existing fields ...
    
    // NEW: OSMAnd sync tracking fields
    val osmandOriginalName: String? = null,      // Original name from OSMAnd (for updates)
    val osmandGpxIndex: Int? = null,             // Last known index in GPX file
    val osmandSyncSource: String = "LOCAL",      // "LOCAL" or "OSMAND"
    val osmandLastSyncedAt: Long? = null,        // Timestamp of last successful sync
    val osmandSyncedToServer: Boolean = false    // True if exists on OSMAnd server
)
```

### Field Descriptions

#### `osmandOriginalName: String?`
- **Purpose**: Stores the EXACT name as it appears on OSMAnd server
- **Usage**: Required for `updateFavorite()` API call (wptName parameter)
- **Lifecycle**:
  - Set when downloading from OSMAnd GPX
  - Updated after successful `addFavorite()` or `updateFavorite()`
  - NOT changed when user edits location name in app
- **Example**: User renames "Station A" to "My Favorite Station" in app
  - `name` = "My Favorite Station" (new name)
  - `osmandOriginalName` = "Station A" (server name for updates)

#### `osmandGpxIndex: Int?`
- **Purpose**: Caches the zero-based index in the GPX file
- **Usage**: Required for `updateFavorite()` API call (ind parameter)
- **Lifecycle**:
  - Set when downloading from OSMAnd GPX
  - Updated after successful sync
  - Can become stale if other clients modify the GPX
- **Note**: Should be refreshed periodically or cleared after failed updates

#### `osmandSyncSource: String`
- **Purpose**: Indicates where the location originated
- **Values**:
  - `"LOCAL"` - Created in your app, not yet on OSMAnd
  - `"OSMAND"` - Downloaded from OSMAnd
- **Usage**: Determines whether to call `addFavorite()` or `updateFavorite()`
- **Lifecycle**:
  - Set to `"OSMAND"` when downloading GPX
  - Set to `"LOCAL"` when user creates new location
  - Changed to `"OSMAND"` after first successful `addFavorite()`

#### `osmandLastSyncedAt: Long?`
- **Purpose**: Timestamp of last successful sync to OSMAnd
- **Usage**: 
  - Detect stale data that needs re-sync
  - Show "last synced" info to user
  - Determine if local changes are newer than server
- **Lifecycle**: Updated after every successful `addFavorite()` or `updateFavorite()`

#### `osmandSyncedToServer: Boolean`
- **Purpose**: Quick flag to check if location exists on OSMAnd server
- **Usage**: 
  - Skip GPX download to determine add vs update
  - Filter locations for sync operations
  - UI indicators (show sync icon)
- **Lifecycle**:
  - `false` for new local locations
  - `true` after first successful sync
  - Set to `true` when downloading from GPX

## Implementation Changes

### 1. Update LocationEntity (Entities.kt)

```kotlin
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
    val imagePaths: String = "",
    val descriptionHash: String? = null,
    val aiParsed: Boolean = false,
    val aiParsedAt: Long? = null,
    val businessName: String? = null,
    val streetAddress: String? = null,
    val suburb: String? = null,
    val state: String? = null,
    val postcode: String? = null,
    
    // NEW: OSMAnd sync tracking
    val osmandOriginalName: String? = null,
    val osmandGpxIndex: Int? = null,
    val osmandSyncSource: String = "LOCAL",
    val osmandLastSyncedAt: Long? = null,
    val osmandSyncedToServer: Boolean = false
)
```

### 2. Update Location Model (Models.kt)

```kotlin
data class Location(
    val id: String,
    val name: String,
    // ... existing fields ...
    
    // NEW: OSMAnd sync tracking
    val osmandOriginalName: String? = null,
    val osmandGpxIndex: Int? = null,
    val osmandSyncSource: String = "LOCAL",
    val osmandLastSyncedAt: Long? = null,
    val osmandSyncedToServer: Boolean = false
)
```

### 3. Update GpxParser to Set Metadata

```kotlin
class GpxParser {
    fun parse(inputStream: InputStream): List<Location> {
        val locations = mutableListOf<Location>()
        var index = 0  // Track GPX index
        
        // ... parsing logic ...
        
        while (/* parsing */) {
            if (/* found waypoint */) {
                val location = LocationBuilder(lat, lon).apply {
                    // ... parse other fields ...
                }.build()?.copy(
                    // Set OSMAnd metadata
                    osmandOriginalName = location.name,  // Store original server name
                    osmandGpxIndex = index,              // Store index
                    osmandSyncSource = "OSMAND",         // Came from OSMAnd
                    osmandSyncedToServer = true,         // Exists on server
                    osmandLastSyncedAt = System.currentTimeMillis()
                )
                
                if (location != null) {
                    locations.add(location)
                    index++  // Increment for next waypoint
                }
            }
        }
        
        return locations
    }
}
```

### 4. Update addFavorite() to Set Metadata

```kotlin
// In OsmandLocationSyncService.kt
suspend fun addFavorite(
    location: Location,
    categoryName: String
): Result<Unit> {
    // ... existing code ...
    
    when (apiResult) {
        is ApiResult.Success -> {
            Log.d(TAG, "✅ Successfully added favorite to OSMAnd")
            
            // Update local database with OSMAnd metadata
            val syncedLocation = location.copy(
                syncStatus = SyncStatus.SYNCED,
                osmandOriginalName = location.name,      // Server now knows this name
                osmandSyncSource = "OSMAND",             // Now on OSMAnd
                osmandSyncedToServer = true,             // Confirmed on server
                osmandLastSyncedAt = System.currentTimeMillis(),
                osmandGpxIndex = null  // Index unknown until next GPX download
            )
            localRepository.updateLocation(syncedLocation, categoryName)
            
            return Result.success(Unit)
        }
        // ... error handling ...
    }
}
```

### 5. Update updateFavorite() to Update Metadata

```kotlin
suspend fun updateFavorite(
    location: Location,
    categoryName: String,
    currentName: String,
    index: Int
): Result<Unit> {
    // ... existing code ...
    
    when (apiResult) {
        is ApiResult.Success -> {
            Log.d(TAG, "✅ Successfully updated favorite on OSMAnd")
            
            // Update local database with new OSMAnd metadata
            val syncedLocation = location.copy(
                syncStatus = SyncStatus.SYNCED,
                osmandOriginalName = location.name,  // Update to new server name
                osmandGpxIndex = index,              // Confirm index
                osmandLastSyncedAt = System.currentTimeMillis()
            )
            localRepository.updateLocation(syncedLocation, categoryName)
            
            return Result.success(Unit)
        }
        // ... error handling ...
    }
}
```

### 6. Update EditLocationViewModel Sync Logic

```kotlin
fun syncToOsmand(locationId: String) {
    viewModelScope.launch {
        try {
            val category = tokenManager.getSelectedCategory() ?: "Charging Locations"
            val result = locationRepository.getLocationsByCategory(category)

            result.onSuccess { locations ->
                val location = locations.find { it.id == locationId }
                if (location != null) {
                    
                    // NEW: Use metadata to determine sync strategy
                    if (location.osmandSyncedToServer && location.osmandOriginalName != null) {
                        // Location exists on server - UPDATE
                        val currentName = location.osmandOriginalName!!
                        var index = location.osmandGpxIndex
                        
                        // If index is stale or missing, download GPX to refresh
                        if (index == null) {
                            val gpxResult = authRepository.downloadFavoritesGpx(category)
                            index = gpxResult.getOrNull()
                                ?.indexOfFirst { it.name == currentName }
                                ?: 0
                        }
                        
                        osmandSyncService.updateFavorite(location, category, currentName, index ?: 0)
                            .onSuccess {
                                _editState.value = EditLocationState.UpdateSuccess
                            }
                            .onFailure { e ->
                                _editState.value = EditLocationState.Error("Sync failed: ${e.message}")
                            }
                    } else {
                        // Location is LOCAL-only - ADD to OSMAnd
                        osmandSyncService.addFavorite(location, category)
                            .onSuccess {
                                _editState.value = EditLocationState.UpdateSuccess
                            }
                            .onFailure { e ->
                                _editState.value = EditLocationState.Error("Sync failed: ${e.message}")
                            }
                    }
                }
            }
        } catch (e: Exception) {
            _editState.value = EditLocationState.Error("Sync failed: ${e.message}")
        }
    }
}
```

## Migration Strategy

### Database Migration

```kotlin
// In your database migration
val MIGRATION_X_Y = object : Migration(X, Y) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""
            ALTER TABLE locations 
            ADD COLUMN osmandOriginalName TEXT DEFAULT NULL
        """)
        database.execSQL("""
            ALTER TABLE locations 
            ADD COLUMN osmandGpxIndex INTEGER DEFAULT NULL
        """)
        database.execSQL("""
            ALTER TABLE locations 
            ADD COLUMN osmandSyncSource TEXT DEFAULT 'LOCAL'
        """)
        database.execSQL("""
            ALTER TABLE locations 
            ADD COLUMN osmandLastSyncedAt INTEGER DEFAULT NULL
        """)
        database.execSQL("""
            ALTER TABLE locations 
            ADD COLUMN osmandSyncedToServer INTEGER DEFAULT 0
        """)
        
        // Set existing synced locations as OSMAND source
        database.execSQL("""
            UPDATE locations 
            SET osmandSyncSource = 'OSMAND',
                osmandSyncedToServer = 1,
                osmandOriginalName = name
            WHERE syncStatus = 'SYNCED'
        """)
    }
}
```

## Benefits

### ✅ Performance Improvements
- **No GPX Download for Updates**: Can use cached `osmandOriginalName` and `osmandGpxIndex`
- **Fast Add vs Update Decision**: Check `osmandSyncedToServer` flag instead of downloading GPX
- **Reduced Network Traffic**: Only download GPX when cache is stale

### ✅ Reliability Improvements
- **Survive Name Changes**: Original server name preserved even after user renames
- **Proper Update Tracking**: Know exactly when location was last synced
- **Source Attribution**: Clear distinction between local and OSMAnd locations

### ✅ User Experience Improvements
- **Faster Sync**: No need to download GPX for every sync operation
- **Sync Indicators**: Show users which locations are synced vs local-only
- **Conflict Resolution**: Can detect when local changes are newer than server

## Edge Cases Handled

### User Renames Location
- ✅ `osmandOriginalName` preserves server name for updates
- ✅ `name` field shows user's preferred name in UI
- ✅ Next update uses `osmandOriginalName` for wptName parameter

### Multiple Devices
- ⚠️ `osmandGpxIndex` may become stale if other device adds/removes favorites
- ✅ Solution: Refresh index on error 106, or periodically re-download GPX

### Location Created Locally
- ✅ `osmandSyncSource = "LOCAL"`
- ✅ `osmandSyncedToServer = false`
- ✅ First sync uses `addFavorite()`
- ✅ After success, fields updated to reflect server presence

### Location Deleted from OSMAnd Web
- ⚠️ Local DB still shows `osmandSyncedToServer = true`
- ✅ Solution: Update will fail, can catch error and re-add, or periodic full sync

## Testing Plan

1. **Test New Location Sync**
   - Create location in app
   - Verify `osmandSyncSource = "LOCAL"`
   - Sync to OSMAnd
   - Verify fields updated after success

2. **Test Existing Location Update**
   - Download location from OSMAnd
   - Verify metadata populated correctly
   - Edit location
   - Sync update
   - Verify correct name/index used

3. **Test Name Change**
   - Download location "Original Name"
   - Rename to "New Name" in app
   - Sync
   - Verify update uses "Original Name" for wptName
   - Verify `osmandOriginalName` updated to "New Name"

4. **Test Stale Index**
   - Location has `osmandGpxIndex = 5`
   - Other client modifies GPX
   - Update fails with error 106
   - Verify GPX re-downloaded and index refreshed

## Conclusion

Adding these 5 fields to the database schema will:
- ✅ Enable efficient OSMAnd sync without constant GPX downloads
- ✅ Properly track location origin and sync state
- ✅ Support name changes while maintaining server reference
- ✅ Provide foundation for future sync enhancements (conflict resolution, batch sync, etc.)

This is a **recommended enhancement** that should be implemented to make OSMAnd sync reliable and efficient.
