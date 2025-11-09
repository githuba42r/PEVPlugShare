# PEVPlugShare Implementation Plan - Version 6

## Document Overview
This implementation plan details the design and development approach for PEVPlugShare, an Android app that integrates with OSMAnd to manage EV charging locations. The plan provides a comprehensive roadmap for implementing all features, addressing both functional requirements and UX improvements.

**Target Platform:** Android 16+  
**Development Language:** Kotlin  
**Key Integration:** OSMAnd My Places API  

---

## 1. Project Architecture

### 1.1 High-Level Architecture
```
┌─────────────────────────────────────────────────┐
│          PEVPlugShare App UI Layer              │
├─────────────────────────────────────────────────┤
│  Authentication | Main List | Add Location     │
│  Edit Location | Settings | About              │
├─────────────────────────────────────────────────┤
│         Business Logic & Services Layer         │
├─────────────────────────────────────────────────┤
│  OSMAnd API Service | Location Service         │
│  Photo Service | Sync Service | Offline Queue  │
├─────────────────────────────────────────────────┤
│      Local Data Layer (SQLite Database)        │
├─────────────────────────────────────────────────┤
│  Locations | Photos | Categories | Sync State  │
├─────────────────────────────────────────────────┤
│      Device Services                           │
├─────────────────────────────────────────────────┤
│  GPS/Location | File Storage | Permissions    │
└─────────────────────────────────────────────────┘
```

### 1.2 Technology Stack
- **Language:** Kotlin
- **UI Framework:** Jetpack Compose (recommended) or XML layouts
- **Database:** Room with SQLite
- **Networking:** Retrofit + OkHttp
- **Authentication:** OAuth2 with Authorization Code Flow
- **Image Handling:** Jetpack Image Library or similar
- **Background Tasks:** WorkManager for periodic syncs
- **Location Services:** Google Location Services or Android's Location Manager
- **Dependency Injection:** Hilt or Dagger2

---

## 2. Data Model

### 2.1 Core Entities

#### Location
```
id: Long (Primary Key)
osmandId: String (OSMAnd identifier)
name: String (Required)
description: String (Optional)
latitude: Double (Required)
longitude: Double (Required)
addressLine: String (Optional)

// Socket Types & Capacity
socketTypes: List<SocketType> (AS3112, Type2 EV, others)
gpoCount: Int (Required, default 1)

// Metadata
isPublic: Boolean (flag for public/private)
hasFee: Boolean (flag for paid charging)
feeDescription: String (Optional)
providerName: String (Optional)
providerWebsite: String (Optional)
approximateCost: String (Optional)
additionalInfo: String (Optional)

// Operating Hours
openingHours: String (Optional, e.g., "9-5", "24x7", "Weekdays only")

// Status & Sync
isFavourite: Boolean (default false)
isOfflineCreated: Boolean (for offline support)
lastModified: Long (timestamp)
syncStatus: SyncStatus (PENDING, SYNCED, FAILED)
color: Int (from category)
icon: String (from category)
categoryId: Long (reference to category)
```

#### SocketType
```
id: Long (Primary Key)
name: String (e.g., "AS3112", "Type2 EV", "CCS", "CHAdeMO")
description: String (Optional)
```

#### Photo
```
id: Long (Primary Key)
locationId: Long (Foreign Key)
filePath: String (local storage path)
fileName: String
description: String (Optional)
latitude: Double (GPS data embedded)
longitude: Double (GPS data embedded)
createdAt: Long (timestamp)
syncedToOSMAnd: Boolean
metadata: String (JSON containing app name, location details, etc.)
```

#### Category
```
id: Long (Primary Key)
osmandId: String (OSMAnd identifier)
name: String
color: Int (RGB color code)
icon: String (icon identifier)
description: String (Optional)
isSelected: Boolean (currently selected category)
lastSynced: Long (timestamp)
```

#### SyncState
```
id: Long (Primary Key)
lastFullSync: Long (timestamp)
nextScheduledSync: Long (timestamp)
isCurrentlySyncing: Boolean
lastSyncStatus: SyncStatus (SUCCESS, FAILED, PARTIAL)
offlineLocationCount: Int
```

#### PendingOfflineLocation
```
id: Long (Primary Key)
locationData: String (JSON serialized Location object)
photoFilePaths: List<String>
createdAt: Long (timestamp)
retryCount: Int
```

### 2.2 Enumerations

#### SyncStatus
- PENDING
- SYNCING
- SYNCED
- FAILED
- PARTIAL

#### SocketType Enum
- AS3112 (primary, Australian)
- TYPE2_EV
- CCS (Combined Charging System)
- CHADEMO
- OTHER

---

## 3. Feature Specifications

### 3.1 Authentication Flow

#### Feature: OSMAnd OAuth2 Integration
**Status:** Critical Path

**Requirement:**
- Implement OAuth2 Authorization Code Flow with OSMAnd
- Secure token storage and refresh token management
- Handle token expiration and automatic refresh

**Implementation Details:**
1. On app launch, check for stored OAuth token
2. If no token exists, redirect to OSMAnd OAuth login screen
3. Capture authorization code and exchange for access token
4. Store tokens securely (encrypted SharedPreferences or EncryptedSharedPreferences)
5. Implement token refresh before expiration
6. Provide logout functionality that clears tokens

**API Endpoints:**
- Authorization: `https://osmand.net/oauth/authorize`
- Token Exchange: `https://osmand.net/oauth/token`
- User Profile: OSMAnd API endpoint

**Error Handling:**
- Network errors: Retry mechanism with exponential backoff
- Invalid tokens: Force re-authentication
- API rate limiting: Respect rate limit headers

---

### 3.2 Main Screen (Location List)

#### Feature: Locations List with Filtering and Distance Calculation
**Status:** Core Feature

**UI Components:**
1. **Top Bar**
   - App logo/title
   - Offline indicator (icon showing offline status)
   - Hamburger menu icon

2. **Filter Section** (Collapsible)
   - Toggle: Open Now / All Hours
   - Capacity Minimum (slider or dropdown: 1, 2, 3, 4+)
   - Socket Type Filter (Multi-select: AS3112, Type2, etc.)
   - Public/Private Filter
   - Fee Filter (Free, Paid, All)
   - Favourites Toggle (show only favourites)

3. **Location List**
   - Sorted by distance (line-of-sight, not navigation distance)
   - Each item shows:
     - Location Name
     - Distance (calculated in real-time)
     - GPO Count / Capacity
     - Opening Status indicator (Open/Closed/Unknown)
     - Favourite star (toggleable)
     - Navigation action button
     - Edit action button
   - Color and icon from OSMAnd category

4. **Floating Action Button (FAB)**
   - Quick Add button for new location
   - Color matches category theme

**Location Distance Calculation:**
```
// Haversine formula for line-of-sight distance
// pseudocode:
distance = calculateHaversine(
  current_latitude, current_longitude,
  location_latitude, location_longitude
)
```

**Opening Hours Logic:**
- Parse opening hours string (e.g., "9-5", "24x7", "Weekdays only")
- Compare current time with opening hours
- Display "Open Now", "Closed", or "Unknown" status
- Filter list based on "Open Now" toggle

**Feature: Favourites Management**
- Star icon toggleable on each location
- Visual indication of favourited locations (highlighted, different color)
- Filter list to show only favourites
- Persist favourite status locally and sync to OSMAnd

**Feature: Navigation Integration**
- Button on each location item: "Navigate"
- Intent call to OSMAnd with location coordinates
- OSMAnd will handle routing and navigation

---

### 3.3 Add Location Feature

#### Feature: Quick Add Location (Minimal Entry)
**Status:** Core Feature

**UI/UX:**
- Accessed via FAB on main screen
- Minimal form with only required fields:
  - **Location Name** (Required, text input)
  - **Number of GPO** (Required, number input with +/- buttons)
  - **Photos** (Optional, camera or gallery)
  - **Description** (Optional, text area)
  - **Opening Hours** (Optional, preset dropdown)
  - **Socket Type** (Optional, multi-select checkboxes)
  - **Public/Private** (Optional, toggle)
  - **Has Fee** (Optional, toggle)
  - **Additional Info** (Optional, expandable section)
    - Provider Name (if paid)
    - Provider Website (if paid)
    - Approximate Cost
    - Other Info

**Location Capture:**
- Capture current GPS location (Fine Location permission)
- Display current coordinates on form
- Allow user to adjust if needed
- Store with high precision (Fine Location)

**Duplicate Detection:**
- Query local database for locations within 250m radius
- If duplicates found:
  - Show alert: "Other locations found nearby (distance km away)"
  - Allow user to view nearby locations or confirm adding new location
- If adding anyway, note this decision in offline queue

**Form Validation:**
- Location Name: Required, non-empty
- GPO Count: Required, minimum 1
- At least one socket type should be selected (default to AS3112)
- Coordinates must be valid

**Save Behavior:**
- If online: Submit to OSMAnd API immediately, then sync to local DB
- If offline: Save to local DB with `isOfflineCreated=true` and add to `PendingOfflineLocation` queue
- Return to main list view
- Show success notification

**Photo Capture During Add:**
- Camera button to capture new photo
- Gallery button to select existing photo
- Preview thumbnail of selected photos
- Multiple photos allowed
- Photos stored in app's cache/files directory
- Embed GPS metadata into photo EXIF

---

### 3.4 Edit Location Feature

#### Feature: Edit Existing Locations
**Status:** Core Feature

**UI/UX:**
- Access via edit button on location list item or location detail view
- Similar form to Add Location
- Pre-populated with existing data
- All fields editable
- Add/remove photos
- Delete photo option with confirmation

**Edit Validation & Conflict Resolution:**
- If location modified online since last sync, show conflict warning
- Option to: Overwrite local changes with server, Keep local changes, or Merge
- Track edit timestamp for conflict detection

**Save Behavior:**
- If online: Submit changes to OSMAnd API, sync locally
- If offline: Update local DB, add to sync queue
- Return to list view
- Show success notification

---

### 3.5 Delete Location Feature

#### Feature: Delete Location with Confirmation
**Status:** Core Feature

**UI/UX:**
- Access via delete button/option in edit view
- Show confirmation dialog: "Delete this location permanently?"
- If confirmed:
  - Delete from local DB
  - Queue deletion for OSMAnd sync
  - Remove associated photos
  - Return to list view
  - Show success notification

**Delete Behavior:**
- If online: Submit deletion to OSMAnd API immediately
- If offline: Mark for deletion locally, queue for sync when online

---

### 3.6 Photo Management

#### Feature: Photo Capture and Storage
**Status:** Important Feature

**Photo Storage:**
- Store locally in app's files directory: `/data/data/com.app.name/files/photos/`
- Organize by location: `/photos/{locationId}/{timestamp}.jpg`
- Compress images to reasonable size (target ~500KB per image)

**EXIF Metadata Embedding:**
- Include GPS coordinates (latitude, longitude)
- Include timestamp
- Custom fields (in EXIF User Comment):
  ```json
  {
    "app": "PEVPlugShare",
    "locationName": "Location Name",
    "locationDescription": "Description",
    "chargerType": "AS3112",
    "gpoCount": 4,
    "capturedAt": "2024-01-15T10:30:00Z",
    "additionalInfo": "Any other info"
  }
  ```

**Photo Sync with OSMAnd:**
- Investigation phase: Determine if OSMAnd API supports photo attachment
- If supported: Upload photos as attachments to location
- If not supported: Store locally, prepare for future upload to photo service
- Mark photos as `syncedToOSMAnd` after successful upload

**Photo UI Features:**
- Thumbnail preview in location detail view
- Gallery view to browse all photos for a location
- Delete individual photos
- Re-order photos (drag and drop)

---

### 3.7 Sync & Offline Support

#### Feature: Background Sync with OSMAnd
**Status:** Critical Feature

**Sync Strategy:**
1. **Initial Sync (on app launch or manual refresh):**
   - Fetch all locations from selected OSMAnd My Places category
   - Fetch all categories available in OSMAnd account
   - Store in local database with `syncStatus=SYNCED`
   - Clear old data

2. **Periodic Background Sync:**
   - Configurable interval (default 1 day): 4h, 8h, 12h, 1d, 2d, 5d, 14d, 28d
   - Use WorkManager for reliable scheduling
   - Run even if app is closed (as long as device is on)
   - Respect battery optimizations and Doze mode

3. **Sync on Demand:**
   - Manual resync button in menu
   - Button disabled during active sync
   - Show progress/status in notification

**Sync Process:**
1. Start background notification: "Syncing locations..."
2. Fetch user profile to validate token
3. Fetch all categories from OSMAnd account
4. Update local category list
5. Fetch all locations from selected category
6. Compare with local DB:
   - New locations: Insert
   - Modified locations: Update (check `lastModified` timestamp)
   - Deleted locations: Mark as deleted or remove
   - Local changes synced: Update `syncStatus=SYNCED`
7. Upload offline queue (see Offline Queue section)
8. Show completion notification: "Sync complete" or "Sync failed"
9. Update `SyncState` table with completion timestamp

**Sync Failure Handling:**
- Retry mechanism: exponential backoff (1min, 5min, 15min, 1h)
- Maximum retries: 3
- Log errors for debugging
- Show failure notification to user
- Keep old data if sync fails (don't clear on error)

**Sync Notification:**
- Silent notification in notification drawer while syncing
- Notification title: "PEVPlugShare"
- Progress indication if possible
- Dismissible after sync completes

#### Feature: Offline Mode Support
**Status:** Important Feature

**Offline Detection:**
- Use ConnectivityManager to detect network status
- Show offline indicator in top bar (icon: no wifi/signal)
- Offline status also shown in About screen

**Offline Behavior:**
- All operations performed on local database
- No API calls to OSMAnd
- Store new locations and edits locally
- Queue changes for sync when online

**Offline Queue Management:**
- `PendingOfflineLocation` table tracks unsync'd changes
- When device comes online:
  - Show background notification
  - Automatically start sync process
  - Upload all pending changes
  - Handle conflicts (user notified if server data differs)

**User Notifications:**
- Toast/snackbar when going offline: "App is now offline. Changes will sync when online."
- Toast/snackbar when coming online: "Connection restored. Syncing..."
- Notification when all pending items synced: "Sync complete"

---

### 3.8 Settings Screen

#### Feature: User Settings Management
**Status:** Core Feature

**Settings Menu Items:**
1. **My Places Category Selection**
   - List of available categories from OSMAnd account
   - Radio button or dropdown to select
   - Shows currently selected category
   - Clicking changes category fetches new locations

2. **Sync Frequency**
   - Dropdown with options: 4h, 8h, 12h, 1d, 2d, 5d, 14d, 28d
   - Default: 1 day
   - Updates WorkManager job schedule

3. **About & Information**
   - App version number
   - Build date/time
   - Basic feature description

4. **Account Information**
   - OSMAnd account email
   - Last sync time (with timestamp)
   - Next scheduled sync time

5. **Offline Storage Status**
   - Number of pending offline locations
   - Size of offline cache
   - Option to clear cache (with confirmation)

6. **Search** (Menu item)
   - Accessible from menu, not main screen
   - Search across location names, descriptions
   - Filter results by distance, type, etc.

7. **Resync**
   - Manual trigger for immediate background sync
   - Button disabled during active sync

8. **Logout**
   - Clears OAuth tokens
   - Clears local location data
   - Returns to login screen
   - Confirmation dialog before logout

**Settings Persistence:**
- Use SharedPreferences (or EncryptedSharedPreferences for sensitive data)
- Keys: `selected_category_id`, `sync_frequency_hours`, `last_sync_time`, etc.

---

### 3.9 About Screen

#### Feature: Application Information
**Status:** Core Feature

**About Screen Sections:**

1. **Version Information**
   - App Title: "PEVPlugShare"
   - Version Number: e.g., "1.0.0"
   - Build Date: e.g., "2024-01-15"
   - Build Time: e.g., "14:30:00"

2. **Usage Information**
   - Brief description of app purpose
   - Key features list:
     - Manage EV charging locations
     - Integrate with OSMAnd
     - Quick location add
     - Offline support
     - Photo storage

3. **Account Information**
   - OSMAnd Account Email (if logged in)
   - Last Sync Time: "2024-01-15 14:30" or "Never"
   - Next Scheduled Sync: "2024-01-16 14:30"
   - Sync Status: "Up to date", "Syncing...", "Offline"

4. **Offline Status Indicator**
   - If offline: Show prominent "Currently Offline" indicator with icon
   - If online: Show "Connected" status

5. **Links & Information**
   - OSMAnd website link
   - App GitHub repository (if public)
   - About/Help link

---

### 3.10 Search Feature

#### Feature: Search Locations
**Status:** Lower Priority (optional for v1)

**UI/UX:**
- Accessible from hamburger menu, not main screen
- Search bar with autocomplete
- Search across:
  - Location name
  - Description
  - Address
  - Provider name (if applicable)

**Search Implementation:**
- Local search in SQLite database
- Instant results as user types
- Case-insensitive matching
- Optional filters: distance, type, capacity

**Results Display:**
- Sorted by relevance (name match first, then description)
- Same list item format as main locations list

---

### 3.11 Map View (Future Enhancement)

#### Feature: Map Display of Locations
**Status:** Future Enhancement, NOT in v1

**Implementation Note:**
- Requested for future versions
- User preference: local/offline map (no paid APIs)
- Integration approach TBD after v1 release
- Consider: OSMAnd map tiles, local maps library

---

### 3.12 Share & Export Feature

#### Feature: Export and Share Locations
**Status:** Important Feature

**Export Format:**
- JSON file containing all locations with full metadata
- File structure:
  ```json
  {
    "version": "1.0",
    "exportDate": "2024-01-15T10:30:00Z",
    "appName": "PEVPlugShare",
    "locations": [
      {
        "id": "unique-id",
        "name": "Location Name",
        "latitude": -37.123,
        "longitude": 144.123,
        "gpoCount": 4,
        "socketTypes": ["AS3112", "Type2 EV"],
        "description": "...",
        "isPublic": true,
        "hasFee": false,
        ...
      }
    ]
  }
  ```

**Share Methods:**
1. Export to file (saved to Downloads or shared storage)
2. Share via email, messaging, drive, etc.
3. Intent to open file: When user opens shared .json file with PEVPlugShare:
   - App launches with import dialog
   - Show merge/replace/skip options for each location
   - User selects action and imports

**Import Logic:**
- Check if location already exists (by name + coordinates within 50m)
- If exists: Offer merge (update if newer), skip, or replace
- If new: Add to database
- Mark imported locations appropriately
- Show completion summary

**Permission Handling:**
- Request WRITE_EXTERNAL_STORAGE for export (if targeting older Android)
- For Android 11+, use app-specific directories or MediaStore

---

### 3.13 Socket Type Support

#### Feature: Multiple Socket Type Support
**Status:** Important Feature

**Supported Types:**
- AS3112 (Australia, primary)
- Type2 EV (European)
- CCS (Combined Charging System)
- CHAdeMO (Japanese)
- Other (configurable by user)

**Implementation:**
- Socket types stored as tags/flags on location
- Multi-select UI during location add/edit
- Filter list by socket type
- Display available types on location detail

---

### 3.14 Public/Private & Fee Flags

#### Feature: Location Classification
**Status:** Important Feature

**Public vs Private:**
- Flag: `isPublic` (boolean)
- Public: Location access is in public area (park, charging center, etc.)
- Private: Owned and operated by private business
- UI: Display as icon or label on list

**Fee Information:**
- Flag: `hasFee` (boolean)
- If true, show additional fields:
  - Provider Name (text)
  - Provider Website (URL)
  - Approximate Cost (text, e.g., "$2.50 per kWh")
  - Other relevant info (text area)
- Filter by: Free only, Paid only, All

---

## 4. Permission Strategy

### 4.1 Required Permissions

**Android Manifest Permissions:**
```xml
<!-- Location -->
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />

<!-- Network -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

<!-- Camera & Photos -->
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />

<!-- Background Work -->
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
```

### 4.2 Permission Requests at Runtime

**Fine Location:**
- Requested when adding location (capture current GPS)
- Rationale: "To capture the location of the charging point"
- User can deny: Manual coordinate entry or use coarse location

**Coarse Location:**
- Requested on app startup
- Used for distance calculations to locations in list
- User can deny: App works but shows no distances

**Camera:**
- Requested when user selects photo capture
- Rationale: "To capture photos of the charging location"

**Read/Write Storage:**
- Requested when accessing gallery or saving photos
- Rationale: "To store and access photos of charging locations"

**Notifications:**
- Requested for sync notifications (Android 13+)
- Rationale: "To notify you of sync status and updates"

---

## 5. Development Phases

### Phase 1: Project Setup & Authentication (Week 1)
- [ ] Gradle configuration, dependencies
- [ ] Android manifest configuration
- [ ] OAuth2 authentication implementation
- [ ] Secure token storage
- [ ] Login/logout flow UI
- [ ] Basic app navigation structure

### Phase 2: Data Layer & Database (Week 1-2)
- [ ] Room database setup
- [ ] Entity definitions and migrations
- [ ] DAOs for all entities
- [ ] Database initialization
- [ ] Test data seeding

### Phase 3: OSMAnd API Integration (Week 2)
- [ ] Retrofit setup and service interfaces
- [ ] OAuth2 interceptor for token refresh
- [ ] Category fetching implementation
- [ ] Location fetching implementation
- [ ] Error handling and retry logic

### Phase 4: Main Screen & Location List (Week 2-3)
- [ ] Main activity/fragment layout
- [ ] Location list adapter
- [ ] Distance calculation (Haversine formula)
- [ ] Opening hours parsing logic
- [ ] Filter implementation
- [ ] Sorting by distance
- [ ] Favourites toggle

### Phase 5: Add Location Feature (Week 3-4)
- [ ] Quick add form UI
- [ ] GPS location capture
- [ ] Duplicate detection (250m radius)
- [ ] Photo capture and storage
- [ ] EXIF metadata embedding
- [ ] Offline queue for offline entries
- [ ] Save to local DB and sync

### Phase 6: Edit & Delete Features (Week 4)
- [ ] Edit location form UI
- [ ] Edit logic and save
- [ ] Delete with confirmation
- [ ] Conflict resolution UI

### Phase 7: Sync Engine (Week 4-5)
- [ ] WorkManager setup
- [ ] Background sync service
- [ ] Sync schedule configuration
- [ ] Offline queue processing
- [ ] Sync notifications
- [ ] Error handling and retries

### Phase 8: Offline Support (Week 5)
- [ ] Network state detection
- [ ] Offline indicator UI (top bar, about screen)
- [ ] Offline data persistence
- [ ] Automatic sync when online
- [ ] Conflict resolution

### Phase 9: Settings & About Screens (Week 5-6)
- [ ] Settings UI layout
- [ ] Category selection
- [ ] Sync frequency configuration
- [ ] About screen UI
- [ ] Account info display
- [ ] Offline status display

### Phase 10: Search Feature (Week 6)
- [ ] Search bar implementation
- [ ] Search logic and filtering
- [ ] Results display
- [ ] (Optional: can be deferred to v1.1)

### Phase 11: Share & Export (Week 6-7)
- [ ] Export to JSON implementation
- [ ] Import from JSON implementation
- [ ] Intent handling for shared files
- [ ] Merge/replace logic
- [ ] File sharing UI

### Phase 12: Testing & Polish (Week 7-8)
- [ ] Unit tests for key components
- [ ] Integration tests for sync
- [ ] UI tests for major flows
- [ ] Bug fixes
- [ ] Performance optimization
- [ ] Security review
- [ ] Battery/data optimization

### Phase 13: Deployment (Week 8)
- [ ] Release build configuration
- [ ] Testing on multiple Android versions
- [ ] Play Store preparation
- [ ] Documentation and changelog

---

## 6. Technical Implementation Details

### 6.1 Distance Calculation (Haversine Formula)

```kotlin
// Calculate line-of-sight distance between two GPS coordinates
fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val earthRadiusKm = 6371.0
    val latDiff = Math.toRadians(lat2 - lat1)
    val lonDiff = Math.toRadians(lon2 - lon1)
    
    val a = Math.sin(latDiff / 2) * Math.sin(latDiff / 2) +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
            Math.sin(lonDiff / 2) * Math.sin(lonDiff / 2)
    val c = 2 * Math.asin(Math.sqrt(a))
    return earthRadiusKm * c
}
```

### 6.2 Opening Hours Parsing

```kotlin
// Parse opening hours string and determine if location is open
fun isLocationOpen(openingHours: String?): Boolean {
    if (openingHours.isNullOrEmpty()) return true // Unknown
    
    val now = Calendar.getInstance()
    val hour = now.get(Calendar.HOUR_OF_DAY)
    val dayOfWeek = now.get(Calendar.DAY_OF_WEEK)
    
    return when {
        openingHours.equals("24x7", ignoreCase = true) -> true
        openingHours.contains("weekdays", ignoreCase = true) && 
        dayOfWeek in 2..6 -> parseTimeRange(openingHours, hour)
        openingHours.contains("weekend", ignoreCase = true) && 
        dayOfWeek in arrayOf(1, 7) -> parseTimeRange(openingHours, hour)
        else -> parseTimeRange(openingHours, hour)
    }
}

fun parseTimeRange(range: String, currentHour: Int): Boolean {
    val regex = """(\d{1,2})-(\d{1,2})""".toRegex()
    val match = regex.find(range)
    return if (match != null) {
        val start = match.groupValues[1].toInt()
        val end = match.groupValues[2].toInt()
        currentHour >= start && currentHour < end
    } else true
}
```

### 6.3 EXIF Metadata Embedding

```kotlin
// Embed GPS and custom metadata into photo EXIF
fun embedMetadata(imagePath: String, location: Location) {
    try {
        val exif = ExifInterface(imagePath)
        
        // GPS data
        exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, convertToDMS(location.latitude))
        exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, if (location.latitude > 0) "N" else "S")
        exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, convertToDMS(location.longitude))
        exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, if (location.longitude > 0) "E" else "W")
        
        // Custom metadata in User Comment
        val metadata = mapOf(
            "app" to "PEVPlugShare",
            "locationName" to location.name,
            "locationDescription" to (location.description ?: ""),
            "gpoCount" to location.gpoCount,
            "capturedAt" to System.currentTimeMillis()
        )
        
        exif.setAttribute(
            ExifInterface.TAG_USER_COMMENT,
            Json.encodeToString(metadata)
        )
        
        exif.saveAttributes()
    } catch (e: Exception) {
        Log.e("EXIF", "Error embedding metadata", e)
    }
}
```

### 6.4 WorkManager Setup for Periodic Sync

```kotlin
// Schedule periodic background sync
fun scheduleSyncWorker(syncFrequencyHours: Long = 24) {
    val syncWorker = PeriodicWorkRequestBuilder<SyncWorker>(
        syncFrequencyHours, TimeUnit.HOURS
    ).setBackoffCriteria(
        BackoffPolicy.EXPONENTIAL,
        WorkRequest.MIN_BACKOFF_MILLIS,
        TimeUnit.MILLISECONDS
    ).build()
    
    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "osmand_sync",
        ExistingPeriodicWorkPolicy.REPLACE,
        syncWorker
    )
}

class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // Show notification
            showSyncNotification()
            
            // Perform sync (fetch categories, locations, upload pending)
            syncManager.performFullSync()
            
            // Hide notification
            hideSyncNotification()
            Result.success()
        } catch (e: Exception) {
            Log.e("SyncWorker", "Sync failed", e)
            Result.retry()
        }
    }
}
```

### 6.5 OAuth2 Token Refresh Interceptor

```kotlin
class OAuth2TokenInterceptor(private val tokenManager: TokenManager) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()
        
        // Add current token
        var token = tokenManager.getAccessToken()
        request = addTokenToRequest(request, token)
        
        var response = chain.proceed(request)
        
        // If 401, try to refresh token and retry
        if (response.code == 401) {
            synchronized(this) {
                token = tokenManager.getAccessToken()
                if (shouldRefreshToken(token)) {
                    try {
                        tokenManager.refreshAccessToken()
                        token = tokenManager.getAccessToken()
                        request = addTokenToRequest(request, token)
                        response.close()
                        response = chain.proceed(request)
                    } catch (e: Exception) {
                        Log.e("OAuth", "Token refresh failed", e)
                    }
                }
            }
        }
        
        return response
    }
    
    private fun addTokenToRequest(request: Request, token: String): Request {
        return request.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()
    }
}
```

---

## 7. Security Considerations

### 7.1 Token Management
- Store OAuth tokens in EncryptedSharedPreferences
- Use secure random for CSRF tokens
- Implement certificate pinning for OSMAnd API calls
- Regular token refresh (before expiration)
- Clear tokens on logout

### 7.2 Data Storage
- Encrypt sensitive data in SQLite (Room with encryption or SQLCipher)
- Restrict file permissions on cached photos
- Clear cache on logout

### 7.3 Network Security
- Use HTTPS only (no cleartext HTTP)
- Validate SSL certificates
- Implement request signing if applicable
- Rate limiting awareness

### 7.4 Permission Best Practices
- Request permissions at runtime (Android 6+)
- Show permission rationales
- Handle permission denials gracefully
- Use minimal required permissions

---

## 8. Performance Optimization

### 8.1 Location List Performance
- Pagination or lazy loading for large lists
- RecyclerView with ViewHolder pattern
- Database queries optimized with indices
- Caching of calculated distances

### 8.2 Sync Performance
- Batch API requests where possible
- Incremental sync (only fetch changed items)
- Background processing with WorkManager
- Network-aware sync (avoid on mobile data if WiFi preferred)

### 8.3 Memory Optimization
- Image compression and thumbnail caching
- Lazy loading of photos
- Lifecycle-aware components
- Proper resource cleanup

### 8.4 Battery Optimization
- Respect battery saver mode
- Adaptive sync frequency (more frequent when charging)
- Location updates throttled appropriately
- Geofencing instead of constant polling (if needed)

---

## 9. Testing Strategy

### 9.1 Unit Tests
- Location/distance calculation logic
- Opening hours parsing
- EXIF metadata embedding
- Data model validation

### 9.2 Integration Tests
- OSMAnd API integration
- Database operations (Room)
- OAuth2 flow
- Offline queue processing
- Sync workflow

### 9.3 UI Tests (Espresso/Compose Tests)
- Navigation flow
- Add location form
- Edit location form
- List filtering and sorting
- Settings changes

### 9.4 Manual Testing
- End-to-end sync workflow
- Offline/online transitions
- Network error handling
- Permission requests
- Photo capture and EXIF

---

## 10. Deployment & Release

### 10.1 Version Numbering
- Format: MAJOR.MINOR.PATCH (e.g., 1.0.0)
- Update version in build.gradle before release
- Update version display in About screen

### 10.2 Build Configuration
- Release build signing with app keystore
- ProGuard/R8 obfuscation for release builds
- Optimize resources

### 10.3 Release Testing Checklist
- [ ] All UI flows tested on Android 16+ device
- [ ] Sync workflow tested (online and offline)
- [ ] Photos and EXIF metadata verified
- [ ] Permissions work correctly
- [ ] Error scenarios handled
- [ ] Battery drain acceptable
- [ ] Crash logs reviewed

### 10.4 Play Store Submission
- Prepare app listing and graphics
- Write release notes
- Test on multiple devices via beta
- Submit for review

---

## 11. Future Enhancements (Post v1.0)

1. **Map View:** Interactive map with location markers
2. **Advanced Search:** Full-text search with Lucene indexing
3. **Photo Service Integration:** Mapillary, Dropbox, Google Drive uploads
4. **Advanced Analytics:** Usage statistics, popular locations
5. **Social Features:** User ratings, reviews, community contributions
6. **Batch Operations:** Multi-select and bulk actions
7. **Custom Filters:** User-defined filter presets
8. **Notifications:** Alerts for nearby charging locations
9. **Voice Input:** Voice-to-text for descriptions
10. **Accessibility:** Enhanced accessibility features

---

## 12. Success Metrics

- [ ] Quick location add (<30 seconds for required fields)
- [ ] Sync completes in <5 seconds on WiFi
- [ ] App uses <50MB storage
- [ ] Battery drain <2% per hour during normal use
- [ ] Offline mode stores and syncs changes correctly
- [ ] Photo EXIF metadata preserved and accessible
- [ ] No crashes in standard workflows
- [ ] Intuitive UI requiring minimal onboarding

---

## Appendix A: File Structure

```
PEVPlugShare/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── AndroidManifest.xml
│   │   │   ├── java/com/example/pevplugshare/
│   │   │   │   ├── MainActivity.kt
│   │   │   │   ├── auth/
│   │   │   │   │   ├── AuthManager.kt
│   │   │   │   │   ├── LoginActivity.kt
│   │   │   │   │   └── TokenManager.kt
│   │   │   │   ├── data/
│   │   │   │   │   ├── db/
│   │   │   │   │   │   ├── AppDatabase.kt
│   │   │   │   │   │   ├── dao/
│   │   │   │   │   │   │   ├── LocationDao.kt
│   │   │   │   │   │   │   ├── PhotoDao.kt
│   │   │   │   │   │   │   ├── CategoryDao.kt
│   │   │   │   │   │   │   └── SyncStateDao.kt
│   │   │   │   │   │   └── entity/
│   │   │   │   │   │       ├── Location.kt
│   │   │   │   │   │       ├── Photo.kt
│   │   │   │   │   │       ├── Category.kt
│   │   │   │   │   │       └── SyncState.kt
│   │   │   │   │   ├── network/
│   │   │   │   │   │   ├── OSMandApiService.kt
│   │   │   │   │   │   ├── OAuth2Interceptor.kt
│   │   │   │   │   │   └── models/
│   │   │   │   │   └── repository/
│   │   │   │   │       ├── LocationRepository.kt
│   │   │   │   │       ├── CategoryRepository.kt
│   │   │   │   │       └── SyncRepository.kt
│   │   │   │   ├── ui/
│   │   │   │   │   ├── location/
│   │   │   │   │   │   ├── LocationListFragment.kt
│   │   │   │   │   │   ├── LocationDetailFragment.kt
│   │   │   │   │   │   ├── AddLocationFragment.kt
│   │   │   │   │   │   └── EditLocationFragment.kt
│   │   │   │   │   ├── settings/
│   │   │   │   │   │   ├── SettingsFragment.kt
│   │   │   │   │   │   └── SearchFragment.kt
│   │   │   │   │   ├── about/
│   │   │   │   │   │   └── AboutFragment.kt
│   │   │   │   │   └── adapter/
│   │   │   │   │       └── LocationListAdapter.kt
│   │   │   │   ├── service/
│   │   │   │   │   ├── SyncWorker.kt
│   │   │   │   │   ├── SyncManager.kt
│   │   │   │   │   ├── LocationService.kt
│   │   │   │   │   ├── PhotoService.kt
│   │   │   │   │   └── NetworkService.kt
│   │   │   │   ├── util/
│   │   │   │   │   ├── LocationUtils.kt
│   │   │   │   │   ├── DateTimeUtils.kt
│   │   │   │   │   ├── FileUtils.kt
│   │   │   │   │   ├── ExifUtils.kt
│   │   │   │   │   └── Constants.kt
│   │   │   │   └── di/
│   │   │   │       └── AppModule.kt (Hilt/Dagger)
│   │   │   └── res/
│   │   │       ├── layout/
│   │   │       ├── drawable/
│   │   │       ├── values/
│   │   │       │   ├── strings.xml
│   │   │       │   ├── colors.xml
│   │   │       │   ├── styles.xml
│   │   │       │   └── dimens.xml
│   │   │       └── menu/
│   │   └── test/
│   │       └── java/com/example/pevplugshare/
│   │           ├── LocationUtilsTest.kt
│   │           ├── SyncManagerTest.kt
│   │           └── ...
│   └── build.gradle.kts
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

---

## Appendix B: Key Libraries & Dependencies

```kotlin
// build.gradle.kts
dependencies {
    // Core Android
    implementation("androidx.core:core-ktx:1.x.x")
    implementation("androidx.appcompat:appcompat:1.x.x")
    implementation("com.google.android.material:material:1.x.x")
    
    // Jetpack
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.x.x")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.x.x")
    implementation("androidx.fragment:fragment-ktx:1.x.x")
    implementation("androidx.work:work-runtime-ktx:2.x.x")
    
    // Database
    implementation("androidx.room:room-runtime:2.x.x")
    implementation("androidx.room:room-ktx:2.x.x")
    kapt("androidx.room:room-compiler:2.x.x")
    
    // Networking
    implementation("com.squareup.retrofit2:retrofit:2.x.x")
    implementation("com.squareup.retrofit2:converter-gson:2.x.x")
    implementation("com.squareup.okhttp3:okhttp:4.x.x")
    implementation("com.squareup.okhttp3:logging-interceptor:4.x.x")
    
    // JSON
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.x.x")
    
    // Image Handling
    implementation("io.coil-kt:coil:2.x.x")
    
    // Location
    implementation("com.google.android.gms:play-services-location:21.x.x")
    
    // Security
    implementation("androidx.security:security-crypto:1.x.x")
    
    // DI
    implementation("com.google.dagger:hilt-android:2.x.x")
    kapt("com.google.dagger:hilt-compiler:2.x.x")
    
    // EXIF
    implementation("androidx.exifinterface:exifinterface:1.x.x")
    
    // Logging
    implementation("com.jakewharton.timber:timber:5.x.x")
    
    // Testing
    testImplementation("junit:junit:4.x.x")
    testImplementation("org.mockito.kotlin:mockito-kotlin:4.x.x")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.x.x")
}
```

---

## Document Sign-Off

**Plan Version:** 6.0  
**Created:** January 15, 2024  
**Status:** Ready for Review  
**Next Step:** User Review and Feedback  

**Areas for User Feedback:**
1. Are the features prioritized correctly?
2. Any missing requirements or clarifications needed?
3. Are the UX design choices acceptable?
4. Any concerns about technical approach?
5. Timeline and resource allocation feasible?

