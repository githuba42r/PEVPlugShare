# PEVPlugShare - Implementation Plan v2.0
## Incorporating Clarifications and UX Improvements

**Date:** 2025-11-07  
**Target:** Android 16 (API 34+)  
**Architecture:** MVVM with Repository Pattern  
**Status:** Comprehensive Plan for Review

---

## Executive Summary

PEVPlugShare is an Android application that streamlines the addition and management of EV charging locations to OSMAnd's "My Places" category. This v2.0 plan incorporates user clarifications on features, UX improvements, and technical requirements from the initial design brief review.

**Key Objectives:**
- Minimize data entry friction with quick-add workflow
- Support multiple charger types (AS3112 primary, Type2 EV, others)
- Provide offline-first architecture with automatic sync
- Store photos locally with GPX metadata
- Enable location sharing via JSON export/import
- Track location attributes (public/private, fee-based, charger types)

---

## Phase 1: Core Architecture & Data Model

### 1.1 Enhanced Location Data Model

```kotlin
data class ChargingLocation(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val gpxData: String,
    val chargerTypes: List<ChargerType>,  // AS3112, Type2, etc.
    val capacity: Int,                     // Number of GPO
    val isPublic: Boolean,                 // Public vs Private
    val hasFee: Boolean,
    val provider: String?,                 // For paid locations
    val providerWebsite: String?,
    val approximateCost: String?,
    val additionalInfo: String?,
    val openingHours: String?,
    val description: String?,
    val isFavourite: Boolean = false,
    val syncStatus: SyncStatus,            // SYNCED, PENDING, FAILED, SYNCING
    val lastModified: Long,
    val createdAt: Long,
    val photos: List<PhotoMetadata>
)

enum class ChargerType {
    AS3112,       // AU GPO - primary
    TYPE2_EV,
    TYPE2_COMBO,
    CHADEMO,
    OTHER
}

data class PhotoMetadata(
    val id: String,
    val locationId: String,
    val filePath: String,
    val exifData: Map<String, String>,    // GPX, location, description
    val timestamp: Long
)

enum class SyncStatus {
    SYNCED,
    PENDING,
    FAILED,
    SYNCING
}
```

### 1.2 Database Schema

**Tables:**
- `locations` - Primary location data with sync status
- `photo_metadata` - Photo storage references and EXIF data
- `pending_operations` - Queue for offline operations
- `sync_log` - Audit trail of sync operations

### 1.3 Key Dependencies

```gradle
// Networking
implementation 'com.squareup.retrofit2:retrofit:2.9.0'
implementation 'com.squareup.okhttp3:okhttp:4.10.0'

// Database
implementation 'androidx.room:room-runtime:2.5.1'
implementation 'androidx.room:room-ktx:2.5.1'

// Background Tasks
implementation 'androidx.work:work-runtime-ktx:2.8.1'

// Location Services
implementation 'com.google.android.gms:play-services-location:21.0.1'

// Encryption
implementation 'androidx.security:security-crypto:1.1.0-alpha06'

// Image Processing
implementation 'androidx.exifinterface:exifinterface:1.3.6'

// Serialization
implementation 'com.squareup.moshi:moshi-kotlin:1.15.0'
```

---

## Phase 2: Authentication & Session Management

### 2.1 OAuth2 Implementation (OSMAnd Account)

**Flow:**
1. Check EncryptedSharedPreferences for existing token
2. If missing/expired, initiate OAuth2 authorization code flow
3. Use OSMAnd API endpoints per https://osmand.net/docs/technical/osmand-api-sdk/
4. Store access token & refresh token securely
5. Implement token refresh mechanism

**Key Components:**
- `AuthenticationManager` - OAuth flow orchestration
- `TokenStorage` - Secure token persistence via EncryptedSharedPreferences
- `AuthInterceptor` - Auto-inject auth header to Retrofit calls

### 2.2 Screens

- **Splash Screen** - Check token validity, navigate accordingly
- **Login Screen** - OAuth2 initiation with "Login with OSMAnd"
- **Category Selection** - Display user's "My Places" categories, select PEV charging category

---

## Phase 3: Main Screen & Location List

### 3.1 List Screen Layout

```
┌─────────────────────────────────────────────┐
│ [☰ Menu] [OFFLINE 🔴] [Category ▼]        │
├─────────────────────────────────────────────┤
│ FILTERS (collapsible)                      │
│ ○ Open Now  ● All                          │
│ Min Capacity: [5 ▼] GPO                    │
│ ○ All Locations  ● Favorites Only         │
│ [Search...] (from menu, not here)          │
├─────────────────────────────────────────────┤
│ Location List (ordered by distance)        │
│ ┌───────────────────────────────────────┐  │
│ │ Name: "Shopping Centre GPO"           │  │
│ │ Distance: 2.3 km                      │  │
│ │ Capacity: 8 × AS3112                 │  │
│ │ Status: 🟢 OPEN | Type: Public       │  │
│ │ [★ Fav] [🎯 Navigate]                │  │
│ ├───────────────────────────────────────┤  │
│ │ Name: "Shopping Centre GPO"           │  │
│ │ ...                                    │  │
│ └───────────────────────────────────────┘  │
├─────────────────────────────────────────────┤
│ [+ Add Location]  [🗺️ Map View]            │
└─────────────────────────────────────────────┘
```

### 3.2 Filter Features

- **Status:** Open Now / All times
- **Capacity:** Minimum GPO count
- **Charger Type:** AS3112, Type2, etc. (single selection or multiple)
- **Favorites:** Show only favorited locations
- **Visibility:** Public / Private / All

### 3.3 Location Interactions

- **Navigate Button** - Intent to OSMAnd navigation at coordinates
- **Favorite Star** - Toggle favorite, update locally and queue for sync
- **Tap Location** - Open detail/edit screen
- **Duplicate Detection** - Check 250m radius on add (fine-grained location)

---

## Phase 4: Add/Edit Location Screen

### 4.1 UX Design - Quick Add Workflow

**Minimal Entry (Required):**
1. Location name
2. Number of GPO
3. Primary charger type (default AS3112)

**Optional Sections (Expandable):**
- Photos (+ icon to capture/select)
- Description
- Opening hours (preset options: 24/7, 9-5, Weekdays Only, Custom)
- Public/Private flag
- Fee flag (with provider name, website, cost estimate)
- Additional charger types
- Additional info

### 4.2 Location Permissions Strategy

**Fine Location** (Exact):
- Used ONLY when user adds a new location
- Requested with context: "We need your exact location to add this charging point"
- Stored with high precision in database

**Coarse Location** (Approximate):
- Used ONLY for calculating distances on main list
- Requested with context: "Background location needed to calculate distances to chargers"
- Used for line-of-sight distance calculations (not navigation distance)
- Allows privacy-conscious users to disable if desired

**Implementation:**
```kotlin
// Fine location - one-time request during add
requestFineLocation(
    reason = "Add charging location",
    minAccuracy = 10.0  // 10 meters
)

// Coarse location - background monitoring for distance calc
requestCoarseLocation(
    reason = "Calculate distances to chargers",
    updateInterval = 30000  // 30 seconds
)

// Distance calculation (line-of-sight Haversine)
fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    // Haversine formula - straight line distance
    // NOT navigation distance
}
```

### 4.3 Photo Management

**Photo Capture/Selection:**
- Camera intent or gallery selection
- EXIF metadata extraction:
  - GPS coordinates (overwrite with precise location)
  - Timestamp
  - Camera model
- Add custom metadata:
  - App name: "PEVPlugShare"
  - Location name
  - Charger types
  - Description
- Store locally in app's private files directory
- Reference in database with PhotoMetadata entity

**Future Enhancement:**
- Batch upload to OSMAnd if API supports
- Move to Mapillary/Dropbox/OneDrive in v2.0

### 4.4 Duplicate Detection

```kotlin
suspend fun checkDuplicates(lat: Double, lon: Double): List<ChargingLocation> {
    // Query locations within 250m radius
    // Return all matches to user for confirmation
    // If user confirms, proceed with add despite duplicates
}
```

---

## Phase 5: Detail & Edit Screen

### 5.1 Features

- Display full location details
- Edit any field (with offline queue support)
- Add/remove photos
- Update opening hours
- Change favorite status
- Delete location (with confirmation)
- View sync status indicator

### 5.2 Edit Workflow

- All changes queued if offline
- Visual indicator on changed fields
- "Save" button uploads if online or queues if offline
- Undo option before save (single level)

---

## Phase 6: Offline Support Architecture

### 6.1 Offline-First Design

**When Offline:**
- All new location additions stored locally
- All edits queued in `pending_operations` table
- UI displays "OFFLINE 🔴" in top bar
- All features remain functional (read + write)

**When Online:**
- Auto-detect network restoration via ConnectivityManager
- Automatic sync of pending operations
- Exponential backoff for retries (1s, 2s, 4s, 8s, 16s max)
- Conflict resolution: Server version takes precedence for conflicts

### 6.2 Offline Infrastructure

**Database Tables:**
```sql
CREATE TABLE pending_operations (
    id INTEGER PRIMARY KEY,
    location_id TEXT NOT NULL,
    operation_type TEXT,  -- CREATE, UPDATE, DELETE, ADD_PHOTO
    payload TEXT,         -- JSON serialized data
    timestamp LONG,
    retry_count INTEGER DEFAULT 0,
    last_attempt LONG,
    created_at LONG,
    status TEXT           -- PENDING, FAILED, SUCCEEDED
);
```

**Components:**
- `ConnectivityObserver` - Monitor network status
- `SyncManager` - Orchestrate sync operations
- `PendingOperationRepository` - Queue management
- `AutoSyncWorker` - Background sync job via WorkManager

### 6.3 Sync Mechanism

```kotlin
class SyncManager(
    private val locationRepository: LocationRepository,
    private val pendingOpsRepository: PendingOperationRepository,
    private val osmandApi: OSmandAPI
) {
    suspend fun syncPendingOperations() {
        val pending = pendingOpsRepository.getPending()
        for (op in pending) {
            try {
                when (op.type) {
                    OperationType.CREATE -> osmandApi.createLocation(op.payload)
                    OperationType.UPDATE -> osmandApi.updateLocation(op.payload)
                    OperationType.DELETE -> osmandApi.deleteLocation(op.payload)
                    OperationType.ADD_PHOTO -> osmandApi.uploadPhoto(op.payload)
                }
                pendingOpsRepository.markSucceeded(op.id)
            } catch (e: Exception) {
                pendingOpsRepository.recordFailure(op.id, e)
            }
        }
    }
}
```

### 6.4 Offline Indicator

- **Top Bar:** 🔴 OFFLINE (red) or 🟢 ONLINE (green)
- **About Screen:** Last sync time, next scheduled sync
- **Pending Operations Counter:** Show count if > 0

---

## Phase 7: Background Sync & Notifications

### 7.1 WorkManager Configuration

```kotlin
val syncRequest = PeriodicWorkRequestBuilder<AutoSyncWorker>(
    duration = userPreferedInterval,  // 4/8/12 hours, 1/2/5/14/28 days
    timeUnit = TimeUnit.HOURS
).apply {
    setBackoffCriteria(
        backoffPolicy = BackoffPolicy.EXPONENTIAL,
        initialDelay = 1,
        timeUnit = TimeUnit.MINUTES
    )
}.build()

WorkManager.getInstance().enqueueUniquePeriodicWork(
    "auto_sync_locations",
    ExistingPeriodicWorkPolicy.KEEP,
    syncRequest
)
```

### 7.2 Notifications

- **Sync In Progress:** Silent notification in drawer, cancellable
- **Sync Completed:** Brief toast if no errors
- **Sync Errors:** Persistent notification with retry button

---

## Phase 8: Menu & Settings

### 8.1 Top Bar Hamburger Menu

**Items:**
1. Settings
2. Search (searchable location name/description)
3. About
4. Sync Now (manual trigger)
5. Logout

### 8.2 Settings Screen

- **My Places Category:** Dropdown to select target category
- **Sync Interval:** Dropdown (4/8/12 hours, 1/2/5/14/28 days) - default 1 day
- **Location Permissions:** Toggle fine/coarse location
- **Photo Storage:** Info about local storage usage
- **Export All Locations:** Button to generate JSON export
- **Import Locations:** Button to select JSON file for import
- **Clear Local Cache:** Dangerous option with confirmation

### 8.3 Search Functionality

- Accessible from menu only
- Search by:
  - Location name (substring match)
  - Description
  - Provider name
  - Charger type
- Results displayed in list
- Tap result to navigate to detail view

---

## Phase 9: Import/Export & Sharing

### 9.1 Export Format

**JSON Structure:**
```json
{
  "export_format_version": "1.0",
  "app_version": "1.0.0",
  "exported_at": "2025-11-07T01:13:13Z",
  "total_locations": 42,
  "locations": [
    {
      "id": "uuid",
      "name": "Shopping Centre Charger",
      "latitude": -33.8688,
      "longitude": 151.2093,
      "chargerTypes": ["AS3112", "TYPE2_EV"],
      "capacity": 8,
      "isPublic": true,
      "hasFee": false,
      "provider": null,
      "openingHours": "24/7",
      "description": "Fast chargers available",
      "photos": [
        {
          "filename": "photo_001.jpg",
          "exifData": {
            "gps_latitude": "-33.8688",
            "gps_longitude": "151.2093",
            "description": "Front entrance"
          }
        }
      ]
    }
  ]
}
```

### 9.2 Import Features

- Accept JSON files from external sources
- Preview locations before import
- **Merge Strategy Options:**
  - ✓ Skip existing (by name + distance)
  - ✓ Replace existing (overwrite all data)
  - ✓ Merge (new data overwrites, keeps existing photos)
- Apply strategy to all or per-location selection
- Progress indicator during import
- Summary report after completion

### 9.3 Share Intent

```kotlin
// When user exports/shares JSON file
val shareIntent = Intent(Intent.ACTION_SEND).apply {
    type = "application/json"
    putExtra(Intent.EXTRA_STREAM, jsonFileUri)
    putExtra(Intent.EXTRA_SUBJECT, "PEVPlugShare Locations")
}
startActivity(Intent.createChooser(shareIntent, "Share locations"))

// When user receives & opens JSON file
// Intent handler checks MIME type & filename
// Opens app -> Import screen with merge options
```

---

## Phase 10: About Screen

### 10.1 Content

**App Information:**
- App name: "PEVPlugShare"
- Version number (e.g., v1.0.0)
- Build date & time
- Feature list & quick overview

**Account Information:**
- OSMAnd Account: [username@email.com] Connected
- My Places Category: [Selected category name]
- Last Sync: [timestamp or "Never"]
- Next Scheduled Sync: [timestamp]
- Offline Status: 🔴 OFFLINE or 🟢 ONLINE

**Additional:**
- Open Source License info (if applicable)
- Privacy Policy link
- Support/Feedback email

---

## Phase 11: Map View Implementation

### 11.1 Map Display (OSMAnd Integration)

**Strategy:**
- Use OSMAnd's built-in map display instead of Google Maps (avoid paid APIs)
- Display all locations from current category as pins
- Use category's default color & icon style
- On marker tap, open detail view or quick preview
- Support filtering (same filters as list view)

**Implementation:**
```kotlin
// Launch OSMAnd map view via intent
// Pass coordinates & category info
val osmandIntent = Intent("net.osmand.android.MAP")
osmandIntent.data = Uri.parse("osmand.net/go?lat=$lat&lon=$lon&z=15")
startActivity(osmandIntent)
```

**Alternative (if OSMAnd API insufficient):**
- Embed local map using MapBox GL (free tier available)
- Or defer map view to v1.1 if complexity too high

---

## Phase 12: Additional Features

### 12.1 Favorite Locations

- Tap star icon to toggle favorite
- Favorite flag stored in database
- Filter list by "Favorites Only"
- Persist locally, sync with OSMAnd if API supports

### 12.2 Charger Type Support

**Primary:** AS3112 (AU GPO - default)

**Additional Types:**
- Type 2 EV
- Type 2 Combo (CCS)
- CHAdeMO
- Other (allow text entry)

**Implementation:**
- Multi-select on add/edit screen
- Filter by charger type on main list
- Display selected types on list & detail views

### 12.3 Location Metadata

**Public vs Private Flag:**
- Radio buttons on add/edit
- Filter support on main list
- Useful for private business charging vs public chargers

**Fee Information:**
- Checkbox: "Has charging fee"
- Conditional fields (only show if checked):
  - Provider name
  - Provider website (URL)
  - Approximate cost (text field, e.g., "$2.50/hr")
- Sync to OSMAnd if possible

---

## Implementation Timeline (Phased Approach)

### Phase 1: Core Setup (1-2 weeks)
- Project structure, dependencies, database schema
- OAuth2 authentication
- Location data model
- Local storage (Room DB)

### Phase 2: Main Screens (2-3 weeks)
- Splash, login, category selection
- Main location list
- Location detail view
- Basic filters

### Phase 3: Add Location (1-2 weeks)
- Quick-add form (name, GPO count, charger type)
- Location permissions (fine + coarse)
- Photo capture/selection
- Duplicate detection (250m)

### Phase 4: Offline Support (2 weeks)
- Database queue
- ConnectivityObserver
- Sync infrastructure
- Offline indicator

### Phase 5: Background Sync (1 week)
- WorkManager setup
- Auto-sync scheduling
- Notifications

### Phase 6: Advanced Features (2 weeks)
- Edit/delete locations
- Favorites system
- Search functionality
- Menu & settings

### Phase 7: Import/Export (1-2 weeks)
- JSON serialization
- Import with merge strategies
- Share intent handling

### Phase 8: Polish & Testing (1-2 weeks)
- UI refinement
- Bug fixes
- Performance optimization
- Comprehensive testing

**Total Estimated Duration:** 12-16 weeks for v1.0

---

## Technical Considerations

### 4.1 Location Accuracy & Privacy

- **Fine Location:** Only used during add operation, then downgraded to coarse
- **Coarse Location:** Background updates every 30s for distance calculations
- Users can disable location sharing in settings
- No analytics or tracking implemented
- GPS data + EXIF stored locally only (no cloud until user exports)

### 4.2 Network Resilience

- All API calls use exponential backoff (max 16s)
- Graceful degradation when offline
- Sync queue prevents data loss
- Manual sync button always available

### 4.3 OSMAnd API Limitations

- **Assumption:** OSMAnd API supports CRUD operations for "My Places"
- If API limitations exist, may need to store locations locally only initially
- Plan for OAuth token refresh every 30 days

### 4.4 Photo Storage

- Photos stored in app's private cache directory
- References in database with metadata
- Future migration path to Mapillary/Dropbox/OneDrive
- EXIF data preserved and enhanced with app metadata

### 4.5 Database Migrations

- Plan migrations for future schema changes
- Use Room's auto-migration where possible
- Document any breaking changes

---

## Security Considerations

1. **OAuth Tokens:** Stored using EncryptedSharedPreferences
2. **Location Data:** Stored in Room (no built-in encryption; SQLCipher optional)
3. **Network Requests:** HTTPS only via Retrofit
4. **No Hardcoded Secrets:** Client ID/Secret in BuildConfig (not committed)
5. **Permissions:** Only request necessary permissions with clear context

---

## Testing Strategy

### Unit Tests
- LocationRepository CRUD operations
- Distance calculations (Haversine)
- SyncManager logic
- Filter logic

### Integration Tests
- Database migrations
- Retrofit API calls (mock)
- Offline queue behavior

### UI Tests
- Add location flow
- List filtering & sorting
- Import/export JSON
- Navigation between screens

### Manual Testing
- Real OSMAnd account integration
- GPS accuracy validation
- Photo EXIF extraction
- Offline sync scenarios

---

## Future Enhancements (v1.1+)

1. **Photo Upload:** Batch upload to Mapillary/Dropbox/OneDrive
2. **Crowdsourced Data:** User ratings & reviews for locations
3. **Advanced Map View:** Full offline map with all locations
4. **Charging Session Logging:** Track charge times, costs
5. **Integration with Charging Apps:** PlugShare API integration
6. **Multi-Language Support:** Internationalization (i18n)
7. **Push Notifications:** New chargers nearby, price alerts
8. **Dark Mode:** System theme support
9. **Accessibility:** Screen reader support, high contrast mode

---

## Questions for Review

1. **OSMAnd API Capabilities:** Does the API support creating/updating custom "My Places" locations programmatically, or is this app limited to viewing/filtering existing locations?
2. **Photo Upload:** Should v1.0 support uploading photos to OSMAnd, or defer to v1.1?
3. **Map View Priority:** Is embedded map view essential for v1.0, or can it be v1.1?
4. **Batch Operations:** Confirmed not needed; any other bulk features desired?
5. **Charger Type Expansion:** Are AS3112 + Type2 EV sufficient for v1.0, or include more?
6. **Cost Estimate:** Should this be free-text or preset tiers (e.g., <$1, $1-2, $2-5, >$5/hr)?

---

## Sign-Off

This implementation plan provides a comprehensive, phased approach to building PEVPlugShare while addressing all clarifications from the design brief review. The plan emphasizes:

✓ **Quick-add UX** - Minimal required fields, optional expanded entry  
✓ **Offline-first** - Full functionality without connectivity  
✓ **Privacy-aware** - Fine location only on-demand, coarse for distance calc  
✓ **Local-centric** - No paid API dependencies, OSMAnd integration only  
✓ **Sharing-enabled** - JSON export/import with merge strategies  
✓ **Extensible** - Clear migration path for future v1.1+ features

**Ready for implementation after review & approval.**

