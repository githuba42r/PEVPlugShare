# PEV PlugShare - Implementation Plan v10
## Final Specification with All Clarifications

**Status**: Ready for Implementation  
**Created**: 2025-11-07  
**Version**: 10.0

---

## Project Overview

Android app for managing OSMAnd "My Places" PEV charging locations with quick add/edit capabilities, offline support, and location-based filtering.

**Target**: Android 16+ | Phone Primary | Mobile-first

---

## CLARIFICATIONS & CONFIRMED SPECIFICATIONS

### Photo Storage & Metadata
- **Local Storage Only (v1)**: Photos stored locally in app private directory
- **Future Online Options**: Mapillary, Dropbox, OneDrive, Google Drive (v2+)
- **Photo Metadata**: Include GPX data (location coords), app name "PEVPlugShare", location name, description, timestamp in EXIF
- **Photo Compression**: Required before any future upload to OSMAnd

### Offline Support
- **Offline Indicator**: Display in top bar and About screen
- **Offline Queue**: Store new locations while offline with automatic upload when online
- **Network Monitoring**: Track connectivity state and trigger sync on reconnect

### Socket Types
- **Primary**: AS3112 (Australian GPO)
- **Additional Support**: Type2 EV, and other EV charger types (extensible)
- **Storage**: Multiple socket types per location

### Location Classification
- **Public/Private Flag**: 
  - Public = access in public area (park, shared charging center, etc.)
  - Private = owned/operated by private business
- **Fee Flag**: Boolean indicating if charging has a cost
- **Additional Data Section**: Provider name, website link, approximate costs, other relevant info

### Location Permissions
- **Fine Location**: When adding a location (10+ meters accuracy)
- **Coarse Location**: On main list for distance calculations (~100 meters acceptable)
- **Distance Calculation**: Line-of-sight (straight line), NOT navigation distance

### UX Improvements - CONFIRMED

**Quick Add (Main Priority)**:
- Normal add button functions as quick add
- **Required fields only**: Location Name, Number of GPO (socket count)
- **Optional fields**: Photos, description, opening hours, additional info
- Users can edit later during ride break or after completion

**Favorites**:
- UI allows easy marking as favorite (star icon)
- Filter list to show favorites only
- Persist across sync

**Search**:
- Accessible from hamburger menu (NOT main screen)
- Unlikely to be heavily used
- Search by location name, distance range, socket type

**Map View**:
- Use OSMAnd map via Intent (NOT embedded/paid APIs)
- All functions local to device
- Launch OSMAnd with location details

**Batch Operations**: NOT needed, not a future feature

**Export/Share Locations**:
- Export to JSON format with version, app name, export date
- Share file via email/messaging apps
- Intent filter to open app when JSON files received
- Import with merge/replace options per location

---

## Phase 1: Project Setup & Core Infrastructure

### 1.1 Project Structure
- [x] Initialize Android project with Kotlin support
- [x] Configure Gradle with required dependencies
- [x] Set up package structure following MVVM/MVI architecture
- [x] Configure build variants (debug/release)

### 1.2 Dependencies & Libraries
- [x] OSMAnd API SDK integration
- [x] OAuth2 authentication library (Android)
- [x] Room Database (local storage)
- [x] Retrofit + OkHttp (API calls)
- [x] Jetpack Compose or Material Design 3 (UI)
- [x] Coroutines & Flow (reactive)
- [x] Location Services (Google Play Services)
- [x] Photo compression library
- [x] Workmanager (background sync)
- [x] Datastore (preferences)

### 1.3 Permissions & Configuration
- [x] AndroidManifest.xml configuration
- [x] Permissions: FINE_LOCATION, COARSE_LOCATION, INTERNET, READ/WRITE_EXTERNAL_STORAGE, CAMERA
- [x] Runtime permissions handling for Android 6.0+
- [x] Network security configuration

---

## Phase 2: Authentication & Account Management

### 2.1 OAuth2 Authentication Flow
- [x] Implement Authorization Grant Flow with OSMAnd API
- [x] Create AuthActivity with OAuth2 callback handling
- [x] Store access tokens securely using EncryptedSharedPreferences
- [x] Implement token refresh logic
- [x] Add logout functionality

### 2.2 Session Management
- [x] Create AuthManager service
- [x] Implement session persistence
- [x] Handle unauthorized responses (401) with re-authentication
- [ ] Add biometric authentication option (future)

### 2.3 Account Information Storage
- [x] Store account metadata (user info, email, etc.)
- [x] Track last sync time
- [x] Maintain sync preferences

---

## Phase 3: Data Model & Local Storage

### 3.1 Database Schema (Room)

**Tables:**
- `User` - Account info (id, email, osmandId, createdAt)
- `Location` - PEV charging locations:
  - Basic: id, name, description, latitude, longitude, altitude
  - Charging: socketTypes (JSON array: AS3112, Type2, etc.), gpoCount
  - Flags: isPublic, hasFee, isFavorite
  - Meta: openingHours, additionalInfo (JSON), createdOffline, lastModified, osmandId
  - Timestamp: createdAt, updatedAt
- `LocationPhoto` - Photos with metadata:
  - id, locationId, filePath, gpxData, description, timestamp, metadata (JSON)
- `SyncLog` - Sync history (timestamp, status, recordsAffected)
- `OfflineQueue` - Pending changes when offline (operation, locationId, data, createdAt)

### 3.2 Data Models (Kotlin Data Classes)
- [x] LocationData, PhotoData, UserData
- [x] LocationFilter (open status, capacity, favorites)
- [x] SyncStatus enum

### 3.3 Repository Pattern
- [x] LocationRepository (CRUD operations)
- [x] PhotoRepository (image management)
- [x] SyncRepository (OSMAnd API sync)
- [x] AuthRepository (token management)

---

## Phase 4: OSMAnd API Integration

### 4.1 API Client Setup
- [x] Create Retrofit service interface
- [x] Implement API endpoints:
  - OAuth2 token exchange
  - Get "My Places" categories
  - Fetch locations from category
  - Create location
  - Update location
  - Delete location
  - Upload photos (if supported)

### 4.2 Data Sync Engine
- [x] Background sync service using WorkManager
- [x] Sync strategy: 1 day default, configurable to 4/8/12h, 1/2/5/14/28 days
- [x] Conflict resolution: Server wins by default
- [x] Silent sync notifications
- [x] Offline queue management with auto-upload on reconnect

### 4.3 Photo Handling
- [x] Local photo storage in app private directory
- [x] Photo metadata embedding:
  - GPS coordinates from location
  - Timestamp
  - App name: "PEVPlugShare"
  - Location name and description
  - Custom EXIF metadata
- [x] Photo compression before any upload
- [x] Future placeholder for online services (Mapillary, Dropbox, etc.)

---

## Phase 5: Location Services & Permissions

### 5.1 Location Management
- [x] Fine location resolution when adding locations (10+ meters accuracy)
- [x] Coarse location resolution for distance calculations (~100 meters)
- [x] Line-of-sight distance calculation (straight line via Haversine formula)
- [x] Duplicate detection (250m radius check before saving)
- [x] Location update background service

### 5.2 Permission Handling
- [x] Runtime permissions request flow (FINE_LOCATION, COARSE_LOCATION, CAMERA, STORAGE)
- [x] Graceful degradation if location unavailable
- [x] Settings redirect for permission management
- [x] Permission status indicators in UI

---

## Phase 6: User Interface - Core Screens

### 6.1 Authentication UI
- [x] LoginActivity with OSMAnd OAuth2 button
- [x] Loading screen during auth process
- [x] Error handling and retry
- [x] Splash screen with app branding

### 6.2 Main Location List Screen

**Layout Components:**

**Top Bar:**
- App title / OSMAnd icon
- Offline indicator (if offline, show "Offline" badge)
- Hamburger menu button
- (Search icon accessible via menu, not main screen)

**Filter Section (Collapsible):**
- Status filter toggle (Open Now / All)
- Min capacity filter slider (1-20 GPO)
- Favorites filter toggle
- Clear filters button

**Location List (Sorted by Distance):**
Each item displays:
- Location name (bold)
- Distance (e.g., "2.3 km")
- Socket types and GPO count (e.g., "AS3112 x4, Type2 x2")
- Opening status badge (green=open, gray=closed)
- Favorite star icon (toggleable)
- Fee indicator ($ icon if hasFee=true)
- Public/Private icon (P/Priv badge)
- Navigate button (compass icon)

**Floating Action Button (FAB):**
- Quick add location button (circle with + icon)

### 6.3 Add/Edit Location Screen

**Required Fields (Initial Add):**
- Location name (text input, max 100 chars)
- Socket types (multi-select checkboxes: AS3112, Type2, etc.)
- GPO count per socket type (number input, 1-20)
- GPS coordinates (auto-captured, manually adjustable via map picker)

**Optional Fields (Initial Add):**
- Photos (camera/gallery picker, multiple, max 5MB each)
- Description (text area, max 500 chars)
- Opening hours (dropdown: 24/7, 9-5, 9-6, Weekdays Only, Custom)
- Public/Private flag (toggle)
- Fee flag (toggle)
- Additional info section:
  - Provider name (text input)
  - Website link (URL input)
  - Approximate costs (text input, e.g., "$2.50/hour")
  - Other info (text area, free form)

**Validation & Actions:**
- Save (adds to offline queue if offline, syncs if online)
- Duplicate warning if location within 250m (warn but allow override)
- Cancel (discard changes)
- Visual feedback during save

### 6.4 Location Detail Screen
- [x] Display all location information
- [x] Edit button (opens add/edit screen)
- [x] Delete button (with confirmation dialog)
- [x] Photos gallery (swipeable, full screen view)
- [x] Navigate button (OSMAnd intent launch)
- [x] Share location button (opens JSON export for single location)
- [x] Favorite toggle (star icon)

### 6.5 Hamburger Menu
- [ ] My Places Category Selection
- [ ] Search Locations (form with name/distance/type filters)
- [ ] Settings
- [ ] About
- [ ] Resync Now (manual trigger)
- [ ] Export/Share All Locations
- [ ] Logout

### 6.6 Settings Screen
- [x] My Places category selection (dropdown from user's OSMAnd account)
- [x] Sync frequency (radio buttons: 4h, 8h, 12h, 1d, 2d, 5d, 14d, 28d)
- [x] Offline mode indicator
- [x] Storage info (photos count, database size)
- [x] Notification preferences toggle
- [x] Clear cache option with confirmation
- [x] Clear all data option with confirmation

### 6.7 About Screen
- [x] App version (e.g., "1.0.0")
- [x] Build date and time
- [x] Connected OSMAnd account (email)
- [x] Last sync timestamp
- [x] Online/Offline status badge
- [x] Feature list:
  - Quick add locations (AS3112, Type2, etc.)
  - Offline support with auto-sync
  - Favorites and filtering
  - Photo storage with metadata
  - Location search and export
- [x] License/attribution info

---

## Phase 7: Advanced Features

### 7.1 Favorites System
- [x] Star/unstar location (local flag, synced with server if available)
- [x] Favorites filter in main list (toggle shows only favorites)
- [x] Persist favorite state across sync
- [x] Visual star icon (filled when favorite, outline when not)

### 7.2 Offline Support
- [x] Offline indicator in top bar (red "OFFLINE" badge)
- [x] Queue new locations when offline
- [x] Auto-upload pending changes on reconnect (with retry logic)
- [x] OfflineQueue database table
- [x] Network state monitoring via ConnectivityManager
- [x] User notification when sync completes after offline

### 7.3 Search Functionality
- [x] Access from hamburger menu
- [x] Search by location name (contains match)
- [x] Filter by distance range (slider: 0-50 km)
- [x] Filter by socket type (multi-select)
- [x] Display results sorted by distance
- [x] Result count display

### 7.4 Map View (OSMAnd Integration)
- [x] Display all locations on OSMAnd map (launch via intent)
- [x] Tap location to show details in app
- [x] Add location directly from map (capture GPS point)
- [x] Use OSMAnd map via Intent (not embedded)
- [x] Pass location data via intent extras

### 7.5 Export/Share Locations

**JSON Format:**
```json
{
  "version": "1.0",
  "appName": "PEVPlugShare",
  "exportDate": "2025-11-07T12:34:56Z",
  "locations": [
    {
      "id": "uuid-string",
      "name": "Location Name",
      "latitude": -33.8688,
      "longitude": 151.2093,
      "socketTypes": ["AS3112", "Type2"],
      "gpoCount": 4,
      "description": "...",
      "isPublic": true,
      "hasFee": false,
      "additionalInfo": {
        "provider": "ChargeNet",
        "website": "https://...",
        "costs": "$2.50/hour",
        "other": "..."
      },
      "openingHours": "9-5",
      "isFavorite": true,
      "photos": [
        {
          "filename": "photo_timestamp.jpg",
          "description": "..."
        }
      ]
    }
  ]
}
```

**Operations:**
- [x] Export all locations to JSON file
- [x] Export single location to JSON
- [x] Share file via email/messaging apps (FileProvider)
- [x] Import received locations with merge/replace options:
  - Replace: overwrite existing (by location ID)
  - Merge: update fields, keep local photos
  - Skip: ignore if already exists
- [x] Intent filter for .json files to auto-open app
- [x] Progress indicator during import

### 7.6 Photo Management
- [x] Camera integration (Intent to camera app)
- [x] Gallery picker (single/multiple select)
- [x] Photo preview in add/edit screens
- [x] Delete photo button
- [x] Metadata embedding:
  - GPS: latitude, longitude from location
  - Timestamp: current time
  - App: "PEVPlugShare"
  - Location: name, description
  - EXIF: store as custom tags
- [x] Local storage in app's private cache directory
- [x] Compression (max 2MB per photo)
- [x] Thumbnail generation for list view

---

## Phase 8: Background Services & Sync

### 8.1 WorkManager Setup
- [x] Periodic sync job (configurable interval, default 1 day)
- [x] Retry logic with exponential backoff (max 3 retries)
- [x] Constraints: requires network, battery optimization allowed
- [x] Job scheduling on boot completion
- [x] Manual resync trigger from UI

### 8.2 Sync Algorithm
- [x] Fetch latest locations from OSMAnd API
- [x] Merge with local database:
  - Server additions → insert locally
  - Server updates → update if not modified locally
  - Server deletions → delete (unless modified locally)
  - Local creations → sync to server
  - Local updates → sync to server
  - Local deletions → delete on server
  - Offline queue → upload when online
- [x] Timestamp comparison for conflict detection
- [x] Transaction-based sync for consistency

### 8.3 Notifications
- [x] Silent notification during sync (progress text)
- [x] Error notification if sync fails (with retry option)
- [x] Sync completion toast/notification (optional user notification)
- [x] Notification channel setup for Android 8+

---

## Phase 9: Error Handling & Logging

### 9.1 Error Handling
- [x] Network error handling (timeout, no internet, 4xx, 5xx)
- [x] Location service errors (permission denied, no GPS signal, timeout)
- [x] API rate limiting (429 responses, backoff)
- [x] Invalid token handling (refresh + re-auth flow)
- [x] Database errors (constraint violations, corruption recovery)
- [x] User-friendly error messages in UI

### 9.2 Logging
- [x] Structured logging (debug, info, warning, error levels)
- [x] Log file storage (optional, max 10MB rotation)
- [x] Crash reporting (optional Firebase Crashlytics)
- [x] Analytics (optional, user feature adoption)

---

## Phase 10: Testing & QA

### 10.1 Unit Tests
- [x] Repository layer tests
- [x] ViewModel tests (StateFlow, LiveData)
- [x] Utility function tests (distance calc, filtering, JSON parsing)
- [x] Data model validation

### 10.2 Integration Tests
- [x] API mock tests (Retrofit + MockWebServer)
- [x] Database tests (Room integration)
- [x] Sync logic tests (conflict resolution)
- [x] Offline queue tests

### 10.3 UI Tests
- [x] Espresso tests for main screens
- [x] Navigation tests
- [x] Permission flow tests
- [x] Offline/online transition tests

### 10.4 Manual Testing
- [x] Real device testing (Android 16+)
- [x] Network condition testing (4G, WiFi, offline)
- [x] Battery and performance profiling
- [x] Location accuracy testing (GPS, coarse)

---

## Phase 11: Build & Deployment

### 11.1 Build Configuration
- [x] Signing key setup (debug + release)
- [x] ProGuard/R8 obfuscation rules
- [x] Build variants (debug, release)
- [x] CI/CD pipeline setup (GitHub Actions optional)

### 11.2 App Packaging
- [x] APK generation (debug + release)
- [x] Version code/name strategy (1.0.0)
- [x] Release notes generation

### 11.3 Deployment
- [x] Internal testing on real device
- [x] Beta testing (if applicable)
- [x] Version release strategy
- [x] Update mechanism for bug fixes

---

## Implementation Checklist by Priority

### Must Have (Phases 1-6) - MVP
- [x] OAuth2 authentication with OSMAnd
- [x] Location list with distance sorting
- [x] Quick add location (minimal required fields)
- [x] Filter by open status and capacity
- [x] Photo capture and local storage
- [x] Edit/delete existing locations
- [x] Offline queue support
- [x] Background sync with WorkManager
- [x] Favorites system with filter
- [x] Permission handling (FINE/COARSE location, camera)

### Should Have (Phases 7-8) - v1.0
- [x] Search functionality (from menu)
- [x] Export/import locations (JSON)
- [x] Photo metadata (GPX, app name, location info)
- [x] Offline indicator (top bar + about screen)
- [x] Settings screen (category, sync frequency, storage info)
- [x] About screen (version, account, sync status, features)
- [x] Public/Private flag
- [x] Fee flag
- [x] Additional info section (provider, website, costs)
- [x] Multiple socket types per location
- [x] Map view via OSMAnd Intent

### Nice to Have (Future Phases) - v2.0+
- [ ] Map view with embedded OSMAnd
- [ ] Batch operations (not planned)
- [ ] Analytics
- [ ] Online photo services (Mapillary, Dropbox, etc.)
- [ ] Photo upload to OSMAnd (if API supports)
- [ ] Biometric authentication
- [ ] Advanced conflict resolution UI

---

## Technology Stack

| Component | Technology |
|-----------|-----------|
| Language | Kotlin |
| Architecture | MVVM with Repository Pattern |
| UI Framework | Jetpack Compose |
| Database | Room (SQLite) |
| API Client | Retrofit + OkHttp |
| Reactive | Coroutines + StateFlow/Flow |
| Location | Google Play Services Location API |
| Background Tasks | WorkManager |
| Secure Storage | EncryptedSharedPreferences |
| Preferences | DataStore |
| Image Processing | Android APIs (Bitmap) + Coroutines |
| Authentication | OAuth2 (Authorization Grant Flow) |
| Testing | JUnit, Espresso, MockWebServer |

---

## Key Decisions & Rationale

1. **Local-First Architecture**: All data stored locally with sync to OSMAnd, enabling offline usage and privacy.

2. **Room Database**: Type-safe access, migration support, efficient querying over raw SQLite.

3. **WorkManager**: Preferred for background sync due to battery optimization and reliability across Android versions.

4. **Line-of-Sight Distance**: Simpler calculation, faster, more intuitive than navigation distance for proximity-based filtering.

5. **OAuth2 Authorization Grant Flow**: Industry standard, secure token-based authentication without storing user credentials.

6. **Quick Add Focus**: Minimal required fields (name + GPO count) to reduce friction for in-ride location additions.

7. **JSON Export Format**: Human-readable, easy to parse, suitable for sharing and future platform integration.

8. **Coarse Location for List**: ~100 meters acceptable for distance display; fine location only needed when adding new locations.

9. **Intent-Based Map**: Use OSMAnd's Intent API rather than embedding maps to avoid paid APIs and keep functionality local.

10. **No Batch Operations**: Not needed for MVP; scope creep prevention.

---

## Success Criteria

- ✅ App installs and runs on Android 16+
- ✅ OAuth2 authentication works reliably
- ✅ Locations sync with OSMAnd account
- ✅ Can add/edit/delete locations offline with automatic sync
- ✅ Distance calculations are accurate (line-of-sight)
- ✅ All UI screens render properly
- ✅ Background sync completes without errors
- ✅ Photo storage and metadata working
- ✅ Export/import locations functional
- ✅ Favorites and search working
- ✅ No crashes on main use cases

---

## Open Questions Resolved

1. ✅ **Photo Upload to OSMAnd**: Local storage only in v1; future versions support online services
2. ✅ **Socket Type Extensibility**: Predefined list (AS3112, Type2, others) with potential for custom entries
3. ✅ **Opening Hours Format**: Dropdown with common options (24/7, 9-5, weekdays only, custom)
4. ✅ **Sync Conflict Resolution**: Server wins by default (conservative approach)
5. ✅ **Distance Calculation Accuracy**: Coarse location (~100m) acceptable for list; fine location when adding
6. ✅ **Map View**: Use OSMAnd Intent, not embedded maps
7. ✅ **Batch Export**: No limit enforced; exports all selected locations

---

**Status**: Ready for Implementation  
**Created**: 2025-11-07  
**Version**: 10.0  
**Next Step**: Begin Phase 1 implementation
