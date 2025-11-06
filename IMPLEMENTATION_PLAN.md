# PEVPlugShare - Implementation Plan

## Project Overview
Android app to assist users in managing PEV charging locations via OSMAnd "My Places" integration with OAuth2 authentication and GPS-based location discovery.

**Target:** Android 16 (API 34+)  
**Primary Focus:** Quick and easy addition of new charging locations to OSMAnd "My Places"

---

## Phase 1: Architecture & Setup

### 1.1 Project Structure
- **Module:** Android 16 (API 34+) target
- **Architecture:** MVVM with Repository pattern
- **Key Libraries:**
  - Retrofit2 for OSMAnd API calls (via https://osmand.net/docs/technical/osmand-api-sdk/)
  - Room Database for local caching & offline support
  - WorkManager for background sync operations
  - LiveData/ViewModel for state management
  - Google Play Services for GPS (fused location provider)

### 1.2 Authentication Module
- OAuth2 implementation (Authorization Code Flow via OSMAnd API)
- Secure token storage (EncryptedSharedPreferences)
- Token refresh mechanism
- Session management & logout

### 1.3 Offline Support Architecture
- Queue system for locations added while offline
- Sync mechanism to upload queued locations when connection restored
- Offline indicator in top bar & About screen
- Network state monitoring

---

## Phase 2: Core UI Components

### 2.1 Authentication Flow
- **Splash Screen:** Check existing OAuth token
- **Login Screen:** OAuth2 flow initiation via OSMAnd
- **Category Selection:** Display available categories, allow selection

### 2.2 Main Screen Layout
```
┌─ Top Bar ─────────────────────────────────┐
│ [☰ Menu] [OFFLINE INDICATOR] [Category]   │
├─ Filter Section (collapsible) ────────────┤
│  - Status (Open/All)                      │
│  - Min Capacity (GPO count)               │
│  - Favourites Only (toggle)               │
├─ Location List (sorted by distance) ──────┤
│  [Location Item]                          │
│  - Name, Distance, Capacity, Type         │
│  - Status indicator (Open/Closed)         │
│  - ★ Favourite toggle                     │
│  - Navigation button                      │
│  - Public/Private badge                   │
│  - Fee indicator (💰 if paid)             │
├─ (scrollable content area)               │
└─ FAB: Add New Location ───────────────────┘
```

**Offline Indicator:** Shows connection status (e.g., "🔴 Offline" or "🔄 Syncing")

### 2.3 Add Location Screen (Quick Add Mode - Primary UX)
**Required Fields:**
- GPS location capture (current location - Fine Location resolution)
- Location name (text input)
- Number of GPO

**Optional Fields (on same form - user can expand/collapse):**
- Socket types: AS3112 (primary), Type2 EV, Other EV charger types (multi-select)
- Public/Private flag (radio button: Public / Private)
- Description (optional)
- Opening hours: Dropdown/preset options (9-5, 24x7, Weekdays only, Custom)
- Fee flag (Yes/No toggle)
- Photo capture/upload: Camera integration (multiple photos)

**Additional Info Section (optional, collapsible):**
- *For paid locations:*
  - Provider name
  - Provider website link
  - Approximate charging costs
- *General:*
  - Other relevant information (text field)

**Duplicate Detection:** 250m radius check before submission
**Confirm/Cancel actions**

### 2.4 Photo Capture & Metadata
- Capture photo via camera
- Automatically embed in photo metadata (EXIF):
  - Location name, description
  - App name: "PEVPlugShare"
  - GPS coordinates (if available)
  - Additional location details
- Store locally with location reference
- Attach GPX waypoint data to photos
- Photos queued for local storage if offline

### 2.5 Edit Location Screen
- Pre-populate existing data
- Update all fields (name, description, hours, GPO count, socket types, etc.)
- Add additional photos
- Delete location confirmation dialog
- Save/Cancel actions

### 2.6 Menu Options
- **Settings** - Configure app preferences
- **About** - Version, account, sync info
- **Search** - Search locations by name
- **Logout** - Disconnect OSMAnd account
- **Manual Resync** - Force sync operation

### 2.7 Settings Screen
- Select "My Places" category (with current selection display)
- Configure resync frequency with visual explanations
- Options: 4h, 8h, 12h, 1d, 2d, 5d, 14d, 28d (default: 1d)
- Clear app cache option
- Version info

### 2.8 About Screen
- **Version Information:**
  - Version number
  - Build date/time
  - Online/Offline status indicator
- **Account Information:**
  - OSMAnd account connected (email/username)
  - Last sync timestamp
  - Queued locations count (if offline)
- **App Features List**
- **App Description**

---

## Phase 3: Data Management

### 3.1 Local Database (Room)
**Tables:**
- `locations` - Cached location data with all fields
- `sync_state` - Last sync timestamp, status, queue state
- `user_preferences` - Selected category, resync frequency
- `location_photos` - Photo references and metadata
- `pending_locations` - Queue of locations added while offline
- `favourites` - User's marked favourite locations

### 3.2 Data Models
```
Location:
  - id (String) - OSMAnd API ID
  - name (String) - required
  - description (String) - optional
  - latitude (Double)
  - longitude (Double)
  - socketTypes (List<String>) - AS3112, Type2, Other, etc.
  - publicPrivate (Enum) - PUBLIC, PRIVATE
  - hasFee (Boolean) - default: false
  - gpoCount (Int) - required
  - openingHours (String) - optional
  - photos (List<PhotoReference>) - optional
  - additionalInfo (String) - optional
  - providerName (String) - optional (for paid)
  - providerWebsite (String) - optional (for paid)
  - chargingCosts (String) - optional (for paid)
  - color (String) - from category
  - icon (String) - from category
  - lastModified (Long)
  - isFavourite (Boolean) - local flag
  - syncStatus (Enum) - SYNCED, PENDING, FAILED

Photo:
  - id (String)
  - locationId (String)
  - filePath (String)
  - timestamp (Long)
  - exifMetadata (String) - JSON encoded EXIF data
  - gpxData (String) - GPX waypoint format
```

### 3.3 Local Caching Strategy
- Download locations on app startup (if no recent sync)
- Cache with timestamp tracking
- Sync before displaying list
- Store photos locally in app-specific directory
- Manage cache size with LRU eviction

### 3.4 Offline Queue System
- Store pending locations in `pending_locations` table
- Flag each with syncStatus = PENDING
- On network restoration, iterate queue and upload
- Retry failed uploads with exponential backoff
- User can view queue status in About screen

---

## Phase 4: GPS & Location Services

### 4.1 Location Permissions & Resolution
- **Fine Location:** Requested when user initiates "Add Location" (high accuracy ~5m)
- **Coarse Location:** Requested on app startup for main list (can use cached result)
- **Distance Calculation:** Line-of-sight (Haversine formula), not navigation distance

### 4.2 Fused Location Provider
- Use Google Play Services fused location provider
- Leverage GPS, WiFi, and cellular data for accuracy
- Cache last known location to reduce battery impact
- Update location every 30 seconds during active use

### 4.3 Distance-Based Sorting
- Primary: Distance from current location (ascending, using Haversine)
- Secondary: Open status (open first if filter enabled)
- Recalculate on location update or manual refresh

---

## Phase 5: Features Implementation

### 5.1 Location Filters
- **Status:** Open / All (show all times)
- **Capacity:** Minimum GPO count slider/picker
- **Favourites:** Toggle to show only starred locations
- **Apply/Clear filters** with visual feedback

### 5.2 Favourites System
- Star icon on each location item (clickable to toggle)
- Quick filter in main list: "Favourites" toggle
- Favourite status persisted locally
- Sync to OSMAnd if API supports (otherwise local-only)

### 5.3 Navigation Integration
- Detect OSMAnd app presence on device
- Launch OSMAnd with target location via Intent
- Pass location coordinates and name
- Graceful fallback if OSMAnd not installed (show error message)

### 5.4 Duplicate Detection
- 250m radius check before adding new location (line-of-sight)
- Show warning dialog with list of nearby locations
- User can proceed or cancel

### 5.5 Search Function
- Accessible via menu (not main screen)
- Search by location name (case-insensitive substring match)
- Display filtered results in location list view

### 5.6 Export & Sharing
- **Export to JSON:** Generate shareable JSON file containing:
  - Location array with all fields
  - App metadata (version, export timestamp)
  - Formatting: Pretty-printed for readability
- **Import from JSON:** 
  - Accept JSON file via Intent or file picker
  - Present merge/replace options for each location:
    - Skip (keep existing)
    - Replace (overwrite with imported)
    - Merge (combine metadata)
  - Validate JSON structure before import
  - Handle conflicts gracefully

### 5.7 Map View (Future Consideration)
- **Phase 2 Enhancement:** Optional map view using open-source libraries (Osmdroid/Leaflet)
- No paid API services
- Leverage OSM tiles for local rendering
- Device-based calculation (no server dependency)

---

## Phase 6: Socket Types & Location Classification

### 6.1 Socket Type Support
- **AS3112 (Primary - AU GPO):** Default option
- **Type2 EV:** European charging standard
- **Other EV Charger Types:** Extensible list for future socket types
- Multi-select on add/edit form

### 6.2 Public/Private Classification
- **Public:** Access in public area (park, shared charging center, etc.)
- **Private:** Owned/operated by private business
- Radio button selection on add form
- Badge displayed on list items

### 6.3 Fee Tracking
- Boolean flag: Has Fee (Yes/No)
- When "Yes" selected, optional fields unlock:
  - Provider name
  - Provider website
  - Approximate charging costs
- Indicator (💰) shown on list items with fees

---

## Phase 7: Background Services & Sync

### 7.1 Sync Operations
- WorkManager for periodic resync (default: 1 day)
- Resync frequency options: 4h, 8h, 12h, 1d, 2d, 5d, 14d, 28d
- Silent notification during sync (notification drawer only)
- Conflict resolution: Server data takes precedence for synced locations

### 7.2 Offline Queue Upload
- Monitor network connectivity
- On connection restored, attempt upload of pending locations
- Show silent notification during upload
- Retry failed uploads with exponential backoff (max 3 retries)
- Mark synced locations with syncStatus = SYNCED

### 7.3 Notifications
- **Sync In Progress:** "🔄 Syncing locations..." (silent, dismissable)
- **Sync Complete:** "✓ Locations synced" (silent)
- **Sync Failed:** "⚠️ Sync failed - will retry" (silent)
- **Network Restored:** "📡 Connection restored - uploading pending locations" (silent)

---

## Phase 8: Error Handling & UX

### 8.1 Network Errors
- Detect offline state immediately
- Show "🔴 Offline" indicator in top bar
- Queue operations instead of blocking
- Retry mechanism with exponential backoff (for online operations)
- Display user-friendly error messages

### 8.2 Permissions
- **Fine Location:** REQUEST_FINE_LOCATION (Android 6+)
- **Coarse Location:** ACCESS_COARSE_LOCATION (Android 6+)
- **Camera:** CAMERA permission (for photos)
- **Notification:** POST_NOTIFICATIONS (Android 13+)
- **Internet:** INTERNET (required)
- **Network State:** ACCESS_NETWORK_STATE (for offline detection)

### 8.3 Edge Cases
- No OSMAnd account connected → Show login screen
- Empty "My Places" category → Show category selection
- No GPS signal → Show error, allow manual entry
- Sync failure → Queue for retry, show indicator
- Token expiration → Auto-refresh, re-authenticate if needed
- Large photo file → Compress before storage
- Photo metadata failure → Store photo anyway, skip EXIF

### 8.4 Data Validation
- Location name: Required, max 255 characters
- GPO count: Required, positive integer
- Opening hours: Optional free-form text
- Coordinates: Validated range (±90° lat, ±180° lon)
- Website URL: Basic URL format validation
- JSON import: Schema validation

---

## Phase 9: Photo Storage & GPX Integration

### 9.1 Photo Management
- Store locally in app-specific directory (`/Android/data/<package>/cache/photos/`)
- Filename pattern: `{locationId}_{timestamp}.jpg`
- Compress photos before storage (target: ~2MB per photo, max ~5MB uncompressed)
- Limit ~10 photos per location

### 9.2 EXIF Metadata Embedding
- Embed GPS coordinates (latitude, longitude) if available
- Embed location name, description in EXIF comment fields
- Embed app version, capture timestamp
- Use Android ExifInterface library for EXIF manipulation

### 9.3 GPX Data Association
- Generate GPX waypoint for location at time of add/edit
- Attach to photos as metadata or separate file reference
- Format: Standard GPX 1.1 schema
- Include waypoint name, description, extensions

### 9.4 Future Photo Upload (Phase 2)
- Abstract storage layer for future service integration
- Support for Mapillary, Dropbox, OneDrive, Google Drive
- User selectable storage backend in settings

---

## Implementation Timeline (Estimated)

| Phase | Component | Duration | Status |
|-------|-----------|----------|--------|
| 1 | Setup & Architecture | 1 week | - |
| 2 | Authentication & OSMAnd API Integration | 1.5 weeks | - |
| 3 | Main Screen UI & Filtering | 2 weeks | - |
| 4 | Add Location (Quick Add) & Photo Capture | 2 weeks | - |
| 5 | Edit/Delete & Duplicate Detection | 1.5 weeks | - |
| 6 | GPS & Distance Calculation | 1 week | - |
| 7 | Offline Support & Queue System | 1.5 weeks | - |
| 8 | Background Sync & Notifications | 1.5 weeks | - |
| 9 | Favourites & Search | 1 week | - |
| 10 | Export & JSON Import/Merge | 1.5 weeks | - |
| 11 | Error Handling & Polish | 1.5 weeks | - |
| 12 | Testing & Bug Fixes | 2 weeks | - |
| **Total** | | **~19 weeks** | |

---

## Technical Stack Summary

| Component | Technology | Rationale |
|-----------|-----------|-----------|
| Language | Kotlin | Modern Android standard |
| UI | Jetpack Compose (or XML if legacy) | Declarative, maintainable |
| Architecture | MVVM + Repository | Clean separation of concerns |
| Database | Room | Type-safe, integrated with LiveData |
| Networking | Retrofit2 | Robust HTTP client, easy testing |
| Background Tasks | WorkManager | Respects battery/system constraints |
| Location | Google Play Services Fused Provider | Accurate, battery-efficient |
| DI | Hilt | Simplified dependency injection |
| Testing | JUnit, Espresso, Mockito | Standard Android testing |

---

## Addressed Feedback Summary

✅ **OSMAnd API:** Using official documentation (https://osmand.net/docs/technical/osmand-api-sdk/)  
✅ **Photo Storage:** Local-only Phase 1, with architecture for future service integration  
✅ **Photo Metadata:** EXIF embedding with GPS, app name, location details  
✅ **Offline Support:** Queue system with automatic upload on reconnection + offline indicator  
✅ **Socket Types:** AS3112 primary + Type2 EV + extensible for other types  
✅ **Location Flags:** Public/Private + Fee tracking + provider info  
✅ **GPS Resolution:** Fine location for add, coarse for list, line-of-sight distance calc  
✅ **Quick Add Focus:** Name + GPO count only required; all else optional  
✅ **Favourites:** Star toggle + filter option  
✅ **Search:** Menu-accessible location search  
✅ **Export/Import:** JSON format with merge/replace conflict resolution  
✅ **No Paid APIs:** All functions local to device or using free services  

---

## Next Steps

1. ✅ **Implementation Plan Complete** - Ready for final review
2. ⏭️ **Approval & Kickoff** - Confirm scope and timeline with stakeholders
3. ⏭️ **Phase 1 Setup** - Initialize project, configure build, set up CI/CD
4. ⏭️ **Begin Phase 2** - Authentication and OSMAnd API integration

---

## Appendices

### A. OSMAnd API Integration Points
- OAuth2 authentication endpoint
- List "My Places" categories
- Fetch locations from category
- Create/update location
- Delete location
- Sync endpoint for bulk updates

### B. Permission Requirements
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" /> <!-- Android 13+ -->
```

### C. External Dependencies (To be evaluated)
- `com.google.android.gms:play-services-location` - Fused Location Provider
- `com.squareup.retrofit2:retrofit` - HTTP client
- `com.google.code.gson:gson` - JSON serialization
- `androidx.room:room-runtime` - Local database
- `androidx.work:work-runtime-ktx` - Background tasks
- `com.google.dagger:hilt-android` - Dependency injection
- `androidx.exifinterface:exifinterface` - EXIF manipulation

