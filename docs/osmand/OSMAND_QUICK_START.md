# OsmAnd ADD/UPDATE Quick Reference

## TL;DR

```kotlin
// ADD a new favorite
osmandSyncService.addFavorite(location, "Charging Locations")

// UPDATE an existing favorite
osmandSyncService.updateFavorite(location, "Charging Locations", "Current Name", 5)
```

## The One Rule

**NEVER cache updatetime!** Each operation fetches fresh updatetime from server automatically.

## UPDATE Requirements

You MUST provide:
- `currentName`: EXACT current `<name>` from GPX (case-sensitive)
- `index`: Zero-based position in GPX (0 = first, 1 = second, etc.)

Get these by downloading and parsing the GPX file first.

## ADD Requirements

You just need:
- The location object
- The category name

The server will append the new point to the group.

## Differences

| Operation | Needs index? | Needs currentName? | What happens               |
|-----------|--------------|-------------------|----------------------------|
| ADD       | No           | No                | Point appended to group    |
| UPDATE    | Yes          | Yes               | Point updated in-place     |

## Error 106

"File was changed!" = updatetime mismatch

**Handled automatically** - function will:
1. Catch the error
2. Fetch fresh updatetime
3. Retry once
4. Return error if second attempt fails

## HTTP Details

- Method: **GET** (with JSON body - yes, really)
- Required headers: Origin, Referer (set automatically)
- Authentication: SESSION cookie (from login)

## Category Name Format

✅ Correct: `"Charging Locations"`
❌ Wrong: `"favorites-Charging Locations.gpx"`

Just the plain category name, no prefix or suffix.

## Complete Example

```kotlin
@Inject lateinit var osmandSyncService: OsmandLocationSyncService
@Inject lateinit var authRepository: OsmandAuthRepository

// Ensure logged in
if (!authRepository.hasSessionToken()) {
    authRepository.loginWithWebApi(username, password)
}

// ADD new location
val addResult = osmandSyncService.addFavorite(
    location = newLocation,
    categoryName = "Charging Locations"
)

if (addResult.isSuccess) {
    Log.d(TAG, "Added successfully!")
}

// UPDATE existing location (need current name and index first)
val gpxFile = authRepository.downloadFavoritesGpx("Charging Locations")
val waypoints = parseGpx(gpxFile)
val index = waypoints.indexOfFirst { it.matches(location) }
val currentName = waypoints[index].name

val updateResult = osmandSyncService.updateFavorite(
    location = updatedLocation,
    categoryName = "Charging Locations",
    currentName = currentName,
    index = index
)

if (updateResult.isSuccess) {
    Log.d(TAG, "Updated successfully!")
}
```

## Logging

Filter logs by tag:
```bash
adb logcat | grep "OsmandLocationSync"
```

You'll see:
- ✅ Successful operations
- ⚠️ Warnings (like missing groups)
- ❌ Errors with details
- Step-by-step algorithm execution

## Common Mistakes

❌ Caching updatetime across multiple operations
❌ Using `"favorites-Category.gpx"` instead of `"Category"`
❌ Wrong index (e.g., using 1-based instead of 0-based)
❌ Wrong currentName (e.g., using new name instead of current)
❌ Using deprecated `updateLocationToOsmand()` function
❌ Including `ind` parameter in ADD operations

## Full Documentation

See `OSMAND_EXACT_ALGORITHM_IMPLEMENTATION.md` for complete details.
