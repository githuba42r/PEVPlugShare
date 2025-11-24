# Quick Reference: OSMAnd Update & Add API

## Functions Available

### `OsmandLocationSyncService.updateFavorite()`
Updates an existing favorite on osmand.net.

**Parameters:**
- `location: Location` - The location with new data
- `categoryName: String` - Category name (e.g., "Charging Locations")
- `currentName: String` - EXACT current name on server
- `index: Int` - Zero-based index in GPX file

**Returns:** `Result<Unit>`

**Example:**
```kotlin
val result = osmandSyncService.updateFavorite(
    location = updatedLocation,
    categoryName = "Charging Locations",
    currentName = "Old Location Name",
    index = 5
)
```

---

### `OsmandLocationSyncService.addFavorite()`
Adds a new favorite to osmand.net.

**Parameters:**
- `location: Location` - The new location to add
- `categoryName: String` - Category name (e.g., "Charging Locations")

**Returns:** `Result<Unit>`

**Example:**
```kotlin
val result = osmandSyncService.addFavorite(
    location = newLocation,
    categoryName = "Charging Locations"
)
```

---

## Common Patterns

### Pattern 1: Update After Edit
```kotlin
// User edited a location in your app
suspend fun saveLocationChanges(editedLocation: Location, originalName: String, index: Int) {
    val result = osmandSyncService.updateFavorite(
        location = editedLocation,
        categoryName = "Charging Locations",
        currentName = originalName,
        index = index
    )

    if (result.isSuccess) {
        showMessage("Saved to OSMAnd!")
    } else {
        showError("Failed: ${result.exceptionOrNull()?.message}")
    }
}
```

### Pattern 2: Add New Location
```kotlin
// User created a new location
suspend fun addNewLocation(location: Location) {
    val result = osmandSyncService.addFavorite(
        location = location,
        categoryName = "Charging Locations"
    )

    if (result.isSuccess) {
        showMessage("Added to OSMAnd!")
    } else {
        showError("Failed: ${result.exceptionOrNull()?.message}")
    }
}
```

### Pattern 3: Get Index and Current Name
```kotlin
// Download favorites to get current state
suspend fun prepareForUpdate(locationId: String): Triple<Location, String, Int>? {
    val favoritesResult = authRepository.downloadFavoritesGpx("Charging Locations")
    val favorites = favoritesResult.getOrNull() ?: return null

    val index = favorites.indexOfFirst { it.id == locationId }
    if (index == -1) return null

    val current = favorites[index]
    return Triple(current, current.name, index)
}

// Use it
val (location, currentName, index) = prepareForUpdate(locationId) ?: return
val updated = location.copy(description = "New description")
osmandSyncService.updateFavorite(updated, "Charging Locations", currentName, index)
```

---

## Error Codes

- **Error 106** - "File was changed!" - Timestamp mismatch (handled automatically with retry)
- **HTTP 400** - Bad request (check parameters)
- **HTTP 401** - Unauthorized (session expired, need to log in again)
- **HTTP 500** - Server error (retry later)

---

## Prerequisites

✅ User must be logged in (SESSION cookie must exist)
```kotlin
if (!authRepository.hasSessionToken()) {
    // Show login screen
}
```

✅ For updates: You need the current exact name and index
```kotlin
// Download GPX first to get this info
val favorites = authRepository.downloadFavoritesGpx(categoryName)
```

---

## Important Notes

⚠️ **Index is zero-based** - First favorite = 0, second = 1, etc.

⚠️ **Name must match exactly** - Case-sensitive, must be the current name on server

⚠️ **Category name format** - Don't include "favorites-" prefix or ".gpx" suffix
   ✅ Correct: `"Charging Locations"`
   ❌ Wrong: `"favorites-Charging Locations.gpx"`

⚠️ **Automatic retry** - Error 106 is retried once automatically

⚠️ **No batch API** - Update multiple favorites by calling in a loop

---

## Full Documentation

See `OSMAND_UPDATE_ADD_IMPLEMENTATION.md` for complete documentation.
See `OsmandFavoriteUpdateExample.kt` for working code examples.
