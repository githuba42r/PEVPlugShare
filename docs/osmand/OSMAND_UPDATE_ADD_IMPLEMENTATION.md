# OSMAnd Favorites Update & Add Implementation

## Overview

This document describes the implementation of UPDATE and ADD functionality for OSMAnd favorites using the official web API. The implementation follows the exact requirements of the OSMAnd server to avoid concurrency errors (error 106: "File was changed!").

## Implementation Summary

### Files Modified

1. **OsmandApiModels.kt** - Added new data models:
   - `OsmandFavListResponse` - Response from `/mapapi/fav/list`
   - `OsmandFavGroup` - Group info with updatetime
   - `OsmandFavUpdateErrorResponse` - Error response structure
   - `OsmandFavUpdateError` - Error details with errorCode

2. **OsmandApiService.kt** - Added new API endpoints:
   - `getFavList()` - GET `/mapapi/fav/list` to fetch group updatetimes
   - `addFavorite()` - POST `/mapapi/fav/update` with ind=-1 for new favorites

3. **OsmandLocationSyncService.kt** - Added new public functions:
   - `updateFavorite()` - Update an existing favorite
   - `addFavorite()` - Add a new favorite
   - `getCurrentGroupUpdatetime()` - Helper to fetch current updatetime (private)

## Critical Concurrency Rules

Every favorites group file (e.g., `favorites-Charging Locations.gpx`) has an **updatetime** field (milliseconds since epoch). The server rejects any update with **error 106** "File was changed!" unless the query parameters `oldGroupUpdatetime` and `newGroupUpdatetime` exactly match the current updatetime on the server.

### The Required Flow

1. **Always** call `/mapapi/fav/list` immediately before any update/add operation
2. Extract the exact `updatetime` value for your target group
3. Use this value for both `oldGroupUpdatetime` and `newGroupUpdatetime` parameters
4. If you get error 106, refresh the updatetime and retry once
5. After successful update/add, the server automatically increments the updatetime

## API Usage

### Update an Existing Favorite

```kotlin
// Inject the service
@Inject lateinit var osmandSyncService: OsmandLocationSyncService

// Update an existing favorite
val result = osmandSyncService.updateFavorite(
    location = updatedLocation,
    categoryName = "Charging Locations",
    currentName = "Old Location Name",  // The EXACT current name on server
    index = 5  // Zero-based index in the GPX file
)

when {
    result.isSuccess -> {
        Log.d(TAG, "Successfully updated favorite!")
    }
    result.isFailure -> {
        Log.e(TAG, "Failed to update: ${result.exceptionOrNull()?.message}")
    }
}
```

### Add a New Favorite

```kotlin
// Inject the service
@Inject lateinit var osmandSyncService: OsmandLocationSyncService

// Add a new favorite
val result = osmandSyncService.addFavorite(
    location = newLocation,
    categoryName = "Charging Locations"
)

when {
    result.isSuccess -> {
        Log.d(TAG, "Successfully added favorite!")
    }
    result.isFailure -> {
        Log.e(TAG, "Failed to add: ${result.exceptionOrNull()?.message}")
    }
}
```

## Function Details

### `updateFavorite()`

**Purpose**: Update an existing favorite point on osmand.net

**Parameters**:
- `location: Location` - The location with updated data
- `categoryName: String` - The category/group name (e.g., "Charging Locations")
- `currentName: String` - The EXACT current name of the waypoint on the server
- `index: Int` - The zero-based index of the waypoint in the GPX file

**Returns**: `Result<Unit>` - Success or failure with error message

**How it works**:
1. Gets current updatetime from `/mapapi/fav/list`
2. Sends POST to `/mapapi/fav/update` with:
   - `wptName` = current name
   - `oldGroupName` = `favorites-{categoryName}.gpx`
   - `newGroupName` = same as oldGroupName
   - `oldGroupUpdatetime` = current updatetime from step 1
   - `newGroupUpdatetime` = same value
   - `ind` = zero-based index
   - JSON body with new data
3. If error 106 is received, refreshes updatetime and retries once
4. Updates local database on success

### `addFavorite()`

**Purpose**: Add a new favorite point to osmand.net

**Parameters**:
- `location: Location` - The new location to add
- `categoryName: String` - The category/group name (e.g., "Charging Locations")

**Returns**: `Result<Unit>` - Success or failure with error message

**How it works**:
1. Gets current updatetime from `/mapapi/fav/list` (or uses current time if group doesn't exist)
2. Sends POST to `/mapapi/fav/update` with:
   - `oldGroupName` = `favorites-{categoryName}.gpx`
   - `newGroupName` = same as oldGroupName
   - `oldGroupUpdatetime` = current updatetime from step 1
   - `newGroupUpdatetime` = same value
   - `ind` = -1 (indicates adding new favorite)
   - JSON body with location data
3. If error 106 is received, refreshes updatetime and retries once
4. Updates local database on success

### `getCurrentGroupUpdatetime()` (private)

**Purpose**: Fetch the current updatetime for a favorites group

**Parameters**:
- `groupName: String` - The full group filename (e.g., "favorites-Charging Locations.gpx")

**Returns**: `Long?` - The updatetime in milliseconds, or null if group doesn't exist

**Note**: This is called automatically by `updateFavorite()` and `addFavorite()`. You should not need to call it directly.

## Error Handling

Both functions implement comprehensive error handling:

### Retry Logic for Error 106
- If the server returns error code 106 ("File was changed!")
- The function automatically refreshes the updatetime and retries once
- This handles the race condition where another client modified the group between our `/fav/list` call and `/fav/update` call

### Network Errors
- Network errors are caught and returned as `Result.failure()`
- Functions log detailed error information for debugging

### HTTP Errors
- All HTTP error codes are handled
- Error bodies are parsed and logged
- Specific handling for error 106 (timestamp mismatch)

### Validation
- Missing updatetime results in failure (can't proceed without it)
- Session cookie validation is logged

## Request/Response Format

### Request to `/mapapi/fav/update`

**URL**:
```
https://osmand.net/mapapi/fav/update?wptName=Location+Name&oldGroupName=favorites-Charging+Locations.gpx&newGroupName=favorites-Charging+Locations.gpx&oldGroupUpdatetime=1732268934642&newGroupUpdatetime=1732268934642&ind=5
```

**Headers**:
```
Content-Type: application/json
Cookie: SESSION=...
Origin: https://osmand.net
Referer: https://osmand.net/map/mydata/favorites/
```

**Body** (for update or add):
```json
{
  "name": "Display Name",
  "desc": "Description text",
  "address": "123 Main St, City, State 12345",
  "color": "#FF2C2C",
  "background": "octagon",
  "hidden": "true",
  "icon": "charging_station",
  "lat": -27.468953,
  "lon": 153.1039064,
  "category": "Charging Locations",
  "ext": {
    "firstPoint": false,
    "lastPoint": false,
    "lat": 0.0,
    "lon": 0.0,
    "time": 1732268934642,
    "ele": 6.0,
    "speed": 0.0,
    "hdop": null,
    "heading": null,
    "bearing": null,
    "deleted": false,
    "speedColor": 0,
    "altitudeColor": 0,
    "slopeColor": 0,
    "colourARGB": 0,
    "distance": 0.0,
    "extensions": {}
  }
}
```

### Response from `/mapapi/fav/list`

```json
{
  "favGroups": [
    {
      "name": "favorites-Charging Locations.gpx",
      "updatetime": 1732268934642,
      "clienttime": 1732268934000,
      "filesize": 45678
    },
    {
      "name": "favorites-Other Category.gpx",
      "updatetime": 1732100000000,
      "clienttime": 1732099999000,
      "filesize": 12345
    }
  ]
}
```

### Error Response (error 106)

```json
{
  "error": {
    "errorCode": 106,
    "errorMessage": "File was changed!"
  }
}
```

## Integration with Existing Code

### GPX Parser Integration

The existing `GpxParser` class already parses favorites and provides:
- Zero-based index for each waypoint
- Current name of each waypoint
- All other location data

You can use this to get the `currentName` and `index` parameters needed for `updateFavorite()`.

### Location Repository Integration

Both functions automatically update the local database via `localRepository.updateLocation()` after successful server updates, marking locations with `SyncStatus.SYNCED`.

### Session Management

The functions use the existing session cookie managed by `OsmandApiClient` and `CookieManager`. Ensure the user is logged in before calling these functions.

## Testing Recommendations

1. **Test Update Flow**:
   ```kotlin
   // Download favorites to get current state
   val favorites = authRepository.downloadFavoritesGpx("Charging Locations")
   val location = favorites.getOrNull()?.first()

   // Update the first location
   if (location != null) {
       val updated = location.copy(name = "Updated Name")
       osmandSyncService.updateFavorite(
           location = updated,
           categoryName = "Charging Locations",
           currentName = location.name,
           index = 0
       )
   }
   ```

2. **Test Add Flow**:
   ```kotlin
   val newLocation = Location(
       id = UUID.randomUUID().toString(),
       name = "New Charging Point",
       latitude = -27.5,
       longitude = 153.0,
       // ... other fields
   )

   osmandSyncService.addFavorite(
       location = newLocation,
       categoryName = "Charging Locations"
   )
   ```

3. **Test Error 106 Handling**:
   - Simulate concurrent updates by manually changing a favorite in the OSMAnd web interface
   - Then immediately call `updateFavorite()` with stale data
   - Verify it retries with fresh updatetime

## Known Limitations

1. **Index Requirement**: For updates, you must know the exact zero-based index of the waypoint in the GPX file. This requires downloading and parsing the GPX first.

2. **Exact Name Match**: The `currentName` parameter must match exactly (case-sensitive) what's on the server.

3. **Session Required**: Both functions require an active SESSION cookie. Call `authRepository.loginWithWebApi()` first.

4. **No Batch Operations**: Each favorite must be updated/added individually. For batch operations, you'll need to call these functions in a loop.

## Logging

All functions include comprehensive debug logging:
- Request URLs with all parameters
- Request payloads (JSON)
- Response codes and bodies
- Error details
- Updatetime values
- Session cookies (truncated for security)

Look for log tag: `"OsmandLocationSync"`

## Summary

The implementation provides a robust, production-ready solution for updating and adding OSMAnd favorites that:
- ✅ Follows the official OSMAnd web API exactly
- ✅ Handles the critical updatetime concurrency requirement
- ✅ Implements automatic retry logic for error 106
- ✅ Integrates seamlessly with existing code
- ✅ Provides comprehensive error handling and logging
- ✅ Updates local database automatically
- ✅ No reverse-engineered hacks - uses official documented API

Both functions are ready to use in production code.
