# OSMAnd Intent-Based API Integration

## Overview

Grok was **correct** - OSMAnd does **NOT** support OAuth2. Instead, it uses an **Intent-based API** that allows external apps to interact with OSMAnd through Android Intents.

Our current implementation uses a fake OAuth2 flow that won't work. We need to completely rewrite the integration to use OSMAnd's Intent API.

## OSMAnd Intent API Commands

### 1. Add Favorite (Adding Charging Locations)

**Command**: `osmand.api://add_favorite`

**Parameters**:
- `lat` (double) - Latitude
- `lon` (double) - Longitude  
- `name` (string) - Location name
- `desc` (string) - Description (optional)
- `category` (string) - Category name (e.g., "Charging Stations")
- `color` (string) - Color tag (e.g., "red", "blue", "green")
- `visible` (boolean) - Whether visible on map (default: true)

**Example**:
```kotlin
val uri = Uri.parse("osmand.api://add_favorite" +
    "?lat=44.98062" +
    "&lon=34.09258" +
    "&name=Charging%20Station" +
    "&desc=AS3112%20GPO%20x2" +
    "&category=Charging%20Stations" +
    "&color=green" +
    "&visible=true")
val intent = Intent(Intent.ACTION_VIEW, uri)
intent.setPackage("net.osmand.plus")
startActivity(intent)
```

### 2. Show Location

**Command**: `osmand.api://show_location`

**Parameters**:
- `lat` (double) - Latitude
- `lon` (double) - Longitude

**Example**:
```kotlin
val uri = Uri.parse("osmand.api://show_location?lat=44.98062&lon=34.09258")
val intent = Intent(Intent.ACTION_VIEW, uri)
intent.setPackage("net.osmand.plus")
startActivity(intent)
```

### 3. Navigate to Location

**Command**: `osmand.api://navigate`

**Parameters**:
- `dest_lat` (double) - Destination latitude
- `dest_lon` (double) - Destination longitude
- `dest_name` (string) - Destination name
- `start_lat` (double) - Start latitude (optional)
- `start_lon` (double) - Start longitude (optional)
- `start_name` (string) - Start name (optional)
- `profile` (string) - Navigation profile (e.g., "bicycle", "car", "pedestrian")
- `force` (boolean) - Force navigation even if already navigating

**Example**:
```kotlin
val uri = Uri.parse("osmand.api://navigate" +
    "?dest_lat=44.97799" +
    "&dest_lon=34.10286" +
    "&dest_name=Charging%20Station" +
    "&profile=bicycle")
val intent = Intent(Intent.ACTION_VIEW, uri)
intent.setPackage("net.osmand.plus")
startActivity(intent)
```

### 4. Add Map Marker

**Command**: `osmand.api://add_map_marker`

**Parameters**:
- `lat` (double) - Latitude
- `lon` (double) - Longitude
- `name` (string) - Marker name

**Example**:
```kotlin
val uri = Uri.parse("osmand.api://add_map_marker" +
    "?lat=44.98062" +
    "&lon=34.09258" +
    "&name=Charging%20Station")
val intent = Intent(Intent.ACTION_VIEW, uri)
intent.setPackage("net.osmand.plus")
startActivity(intent)
```

### 5. Get Info (Query OSMAnd State)

**Command**: `osmand.api://get_info`

**Returns** (via Intent result):
- `lat`, `lon` - Current location
- `map_lat`, `map_lon` - Map center
- `destination_lat`, `destination_lon` - Current destination (if navigating)
- `eta` - Estimated time of arrival
- `time_left` - Time remaining
- `distance_left` - Distance remaining
- `version` - API version

**Example**:
```kotlin
val uri = Uri.parse("osmand.api://get_info")
val intent = Intent(Intent.ACTION_VIEW, uri)
intent.setPackage("net.osmand.plus")
startActivityForResult(intent, REQUEST_CODE_OSMAND_INFO)

// In onActivityResult:
override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    if (requestCode == REQUEST_CODE_OSMAND_INFO && resultCode == Activity.RESULT_OK) {
        val lat = data?.getDoubleExtra("lat", 0.0)
        val lon = data?.getDoubleExtra("lon", 0.0)
        // Process result...
    }
}
```

## Limitations - NO Direct Data Query API

**CRITICAL FINDING**: OSMAnd's Intent API does **NOT** provide:
1. ❌ List all favorites in a category
2. ❌ Query favorites by category
3. ❌ Update existing favorites
4. ❌ Delete favorites
5. ❌ List available categories
6. ❌ Bulk operations

**What This Means**:
- We **cannot** query OSMAnd's database directly
- We **cannot** list "My Places" categories via Intent
- We **can only** ADD new favorites, not read existing ones

## Alternative Approach: AIDL API

OSMAnd also has an **AIDL (Android Interface Definition Language) API** that provides MORE functionality:

### AIDL API Features:
- Get favorites list
- Get favorite groups
- Update favorites
- Remove favorites
- Search functionality
- More control over OSMAnd features

### AIDL Implementation Requirements:
1. Define AIDL interface files
2. Bind to OSMAnd service
3. Use IPC (Inter-Process Communication)
4. More complex than Intent API

**Reference**: `OsmandAidlApi.java` in OSMAnd source code

## Recommended Architecture Changes

### Option 1: Local Storage Only (RECOMMENDED)
Store all charging locations in our own Room database, use OSMAnd only for:
- ✅ Navigation to location (Intent API)
- ✅ Show location on map (Intent API)
- ✅ Add favorite when creating location (Intent API)

**Pros**:
- Full control over data
- No dependency on OSMAnd data structure
- Works offline
- Simple implementation

**Cons**:
- Data not shared with OSMAnd's favorites
- User must manage two separate databases

### Option 2: AIDL Integration (COMPLEX)
Use OSMAnd's AIDL API for full integration:
- Read favorites from OSMAnd
- Sync changes bidirectionally
- Maintain consistency

**Pros**:
- True integration with OSMAnd
- Single source of truth
- Shared favorites

**Cons**:
- Much more complex
- AIDL binding required
- Fragile (depends on OSMAnd AIDL interface)
- May break with OSMAnd updates

### Option 3: Hybrid Approach (MIDDLE GROUND)
- Store data locally in Room
- Export to OSMAnd favorites on demand
- No automatic sync
- User manually triggers "Export to OSMAnd"

**Pros**:
- Simple implementation
- Local data control
- Optional OSMAnd integration

**Cons**:
- Manual export step
- Data can become out of sync

## Proposed Implementation Plan

### Phase 1: Remove OAuth2 Code
1. Delete `LoginActivity`, `LoginViewModel`
2. Remove OAuth2 API client code
3. Remove token storage
4. Remove `AuthRepository`

### Phase 2: Simplify Authentication
1. No authentication needed for local storage
2. Optional: Check if OSMAnd is installed
3. Show tutorial on first run

### Phase 3: Update Data Flow
**Current (Broken)**:
```
App → OAuth2 → OSMAnd API → Favorites
```

**New (Working)**:
```
App → Room Database → Charging Locations
App → Intent → OSMAnd (for navigation/show)
```

### Phase 4: Add OSMAnd Integration Helper
Create `OsmandIntentHelper.kt`:
```kotlin
object OsmandIntentHelper {
    const val OSMAND_PACKAGE = "net.osmand.plus"
    
    fun isOsmandInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(OSMAND_PACKAGE, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
    
    fun addFavorite(
        context: Context,
        location: ChargingLocation
    ): Boolean {
        if (!isOsmandInstalled(context)) return false
        
        val uri = Uri.parse("osmand.api://add_favorite" +
            "?lat=${location.latitude}" +
            "&lon=${location.longitude}" +
            "&name=${Uri.encode(location.name)}" +
            "&desc=${Uri.encode(location.description ?: "")}" +
            "&category=Charging%20Stations" +
            "&color=green" +
            "&visible=true")
        
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.setPackage(OSMAND_PACKAGE)
        
        return try {
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    fun navigateToLocation(
        context: Context,
        location: ChargingLocation
    ): Boolean {
        if (!isOsmandInstalled(context)) return false
        
        val uri = Uri.parse("osmand.api://navigate" +
            "?dest_lat=${location.latitude}" +
            "&dest_lon=${location.longitude}" +
            "&dest_name=${Uri.encode(location.name)}" +
            "&profile=bicycle")
        
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.setPackage(OSMAND_PACKAGE)
        
        return try {
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    fun showLocationOnMap(
        context: Context,
        location: ChargingLocation
    ): Boolean {
        if (!isOsmandInstalled(context)) return false
        
        val uri = Uri.parse("osmand.api://show_location" +
            "?lat=${location.latitude}" +
            "&lon=${location.longitude}")
        
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.setPackage(OSMAND_PACKAGE)
        
        return try {
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }
}
```

### Phase 5: Update UI
1. Remove login screen
2. Add "Export to OSMAnd" button per location
3. Add "Open in OSMAnd" button
4. Add OSMAnd installation check

### Phase 6: Optional Features
1. Bulk export all locations to OSMAnd
2. Import from GPX files
3. Share locations via JSON

## Implementation Timeline

- **Phase 1-2**: 2-3 hours (Remove OAuth, simplify)
- **Phase 3**: 1 hour (Update data flow)
- **Phase 4**: 2-3 hours (OSMAnd Intent helper)
- **Phase 5**: 2-3 hours (Update UI)
- **Total**: ~8-12 hours

## Testing Checklist

- [ ] OSMAnd installed - all features work
- [ ] OSMAnd not installed - graceful degradation
- [ ] Add favorite via Intent
- [ ] Navigate to location via Intent
- [ ] Show location on map via Intent
- [ ] Local database CRUD operations
- [ ] Offline functionality
- [ ] Data persistence

## Resources

- **OSMAnd Source**: https://github.com/osmandapp/OsmAnd
- **ExternalApiHelper.java**: Main Intent API handler
- **OsmandAidlApi.java**: AIDL interface (advanced)
- **OSMAnd Docs**: https://osmand.net/docs/

## Conclusion

**Recommendation**: Implement **Option 1 (Local Storage Only)** with simple Intent integration for navigation and showing locations. This is:
- ✅ Simplest to implement
- ✅ Most reliable
- ✅ Easiest to maintain
- ✅ Works offline
- ✅ No complex dependencies

The OAuth2 code in our app is **completely wrong** and needs to be removed. OSMAnd uses Intents, not web APIs.
