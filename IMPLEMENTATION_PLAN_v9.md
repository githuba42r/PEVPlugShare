# PEV PlugShare - Implementation Plan v9

## Project Overview
Android app for managing OSMAnd "My Places" PEV charging locations with quick add/edit capabilities, offline support, and location-based filtering.

**Target:** Android 16+ | Phone Primary | Mobile-first

---

## Phase 1: Project Setup & Core Infrastructure

### 1.1 Project Structure
- [ ] Initialize Android project with Kotlin support
- [ ] Configure Gradle with required dependencies
- [ ] Set up package structure following MVVM/MVI architecture
- [ ] Configure build variants (debug/release)

### 1.2 Dependencies & Libraries
- [ ] OSMAnd API SDK integration
- [ ] OAuth2 authentication library (Android)
- [ ] Room Database (local storage)
- [ ] Retrofit + OkHttp (API calls)
- [ ] Jetpack Compose or Material Design 3 (UI)
- [ ] Coroutines & Flow (reactive)
- [ ] Location Services (Google Play Services)
- [ ] Photo compression library
- [ ] Workmanager (background sync)
- [ ] Datastore (preferences)

### 1.3 Permissions & Configuration
- [ ] AndroidManifest.xml configuration
- [ ] Permissions: FINE_LOCATION, COARSE_LOCATION, INTERNET, READ/WRITE_EXTERNAL_STORAGE, CAMERA
- [ ] Permissions handling for Android 6.0+ (runtime permissions)
- [ ] Network security configuration

---

## Phase 2: Authentication & Account Management

### 2.1 OAuth2 Authentication Flow
- [ ] Implement Authorization Grant Flow with OSMAnd API
- [ ] Create AuthActivity with OAuth2 callback handling
- [ ] Store access tokens securely using EncryptedSharedPreferences
- [ ] Implement token refresh logic
- [ ] Add logout functionality

### 2.2 Session Management
- [ ] Create AuthManager service
- [ ] Implement session persistence
- [ ] Handle unauthorized responses (401) with re-authentication
- [ ] Add biometric authentication option (future)

### 2.3 Account Information Storage
- [ ] Store account metadata (user info, email, etc.)
- [ ] Track last sync time
- [ ] Maintain sync preferences

---

## Phase 3: Data Model & Local Storage

### 3.1 Database Schema (Room)
**Tables:**
- `User` - Account info
- `Location` - PEV charging locations with fields:
  - id, name, description, latitude, longitude, altitude
  - socketTypes (array: AS3112, Type2, etc.)
  - gpoCount, isPublic, hasFee
  - openingHours, additionalInfo (JSON)
  - isFavorite, lastModified, osmandId
  - photos (relation)
  - createdOffline (boolean)
- `LocationPhoto` - Photos with metadata:
  - id, locationId, filePath, gpxData, description, timestamp, metadata (JSON)
- `SyncLog` - Sync history
- `OfflineQueue` - Pending changes when offline

### 3.2 Data Models (Kotlin Data Classes)
- [ ] LocationData, PhotoData, UserData
- [ ] LocationFilter (open status, capacity, favorites)
- [ ] SyncStatus enum

### 3.3 Repository Pattern
- [ ] LocationRepository (CRUD operations)
- [ ] PhotoRepository (image management)
- [ ] SyncRepository (OSMAnd API sync)
- [ ] AuthRepository (token management)

---

## Phase 4: OSMAnd API Integration

### 4.1 API Client Setup
- [ ] Create Retrofit service interface
- [ ] Implement API endpoints:
  - OAuth2 token exchange
  - Get "My Places" categories
  - Fetch locations from category
  - Create location
  - Update location
  - Delete location
  - Upload photos

### 4.2 Data Sync Engine
- [ ] Background sync service using WorkManager
- [ ] Sync strategy (1 day default, configurable to 4/8/12h, 1/2/5/14/28 days)
- [ ] Conflict resolution (server wins, local wins, merge)
- [ ] Silent sync notifications
- [ ] Offline queue management with auto-upload on reconnect

### 4.3 Photo Handling
- [ ] Local photo storage in app private directory
- [ ] Photo metadata embedding (GPX data, app name, location name)
- [ ] Photo compression before upload
- [ ] Future placeholder for online photo services (Mapillary, Dropbox, etc.)

---

## Phase 5: Location Services & Permissions

### 5.1 Location Management
- [ ] Fine location resolution when adding locations (10+ meters accuracy)
- [ ] Coarse location resolution for distance calculations (~100 meters)
- [ ] Line-of-sight distance calculation (straight line, not navigation)
- [ ] Duplicate detection (250m radius check)
- [ ] Location update background service

### 5.2 Permission Handling
- [ ] Runtime permissions request flow
- [ ] Graceful degradation if location unavailable
- [ ] Settings redirect for permission management
- [ ] Permission status indicators in UI

---

## Phase 6: User Interface - Core Screens

### 6.1 Authentication UI
- [ ] LoginActivity with OSMAnd OAuth2 button
- [ ] Loading screen during auth process
- [ ] Error handling and retry
- [ ] Splash screen with app branding

### 6.2 Main Location List Screen
**Layout:**
- Top bar with:
  - App title / OSMAnd icon
  - Offline indicator (if offline)
  - Hamburger menu button
  - Search icon (hamburger menu access)

- Filter section (collapsible):
  - Status filter (Open Now / All)
  - Min capacity filter (slider: 1-20)
  - Favorites filter toggle
  - Clear filters button

- Location list (sorted by distance):
  - Each item shows:
    - Location name (bold)
    - Distance (e.g., "2.3 km")
    - Socket types and GPO count (e.g., "AS3112 x4")
    - Opening status badge (green=open, gray=closed)
    - Favorite star (toggleable)
    - Fee indicator ($ icon if paid)
    - Navigate button (compass icon)

- Floating Action Button (FAB):
  - Quick add location button (circle with + icon)

### 6.3 Add/Edit Location Screen
**Required fields (initial add):**
- Location name (text input)
- GPS coordinates (auto-captured, manually adjustable)
- Socket types (multi-select: AS3112, Type2, etc.)
- GPO count (number input)

**Optional fields (visible but optional):**
- Photos (camera/gallery picker, multiple)
- Description (text area)
- Opening hours (predefined: 24/7, 9-5, weekdays only, custom)
- Public/Private flag
- Fee flag
- Additional info (provider name, website, costs)

**Actions:**
- Save (add to offline queue if offline)
- Duplicate warning (if location within 250m)
- Cancel

### 6.4 Location Detail Screen
- Display all location information
- Edit button (opens add/edit screen)
- Delete button (with confirmation)
- Photos gallery
- Navigate to location (OSMAnd intent)
- Share location button

### 6.5 Hamburger Menu
- My Places Category Selection
- Search Locations
- Favorites Filter
- Settings
- About
- Resync Now
- Export/Share Locations
- Logout

### 6.6 Settings Screen
- [ ] My Places category selection (dropdown)
- [ ] Sync frequency (radio: 4h, 8h, 12h, 1d, 2d, 5d, 14d, 28d)
- [ ] Offline mode info
- [ ] Storage info (photos, database size)
- [ ] Notification preferences
- [ ] Clear cache/data options

### 6.7 About Screen
- [ ] App version, build date/time
- [ ] Connected OSMAnd account email
- [ ] Last sync timestamp
- [ ] Online/Offline status indicator
- [ ] Feature list
- [ ] License info

---

## Phase 7: Advanced Features

### 7.1 Favorites System
- [ ] Star/unstar location (local flag + sync)
- [ ] Favorites filter in main list
- [ ] Persist favorite state across sync

### 7.2 Offline Support
- [ ] Offline indicator in top bar
- [ ] Queue new locations when offline
- [ ] Auto-upload pending changes on reconnect
- [ ] OfflineQueue database table
- [ ] Network state monitoring (ConnectivityManager)

### 7.3 Search Functionality
- [ ] Access from hamburger menu
- [ ] Search by location name
- [ ] Search by location distance range
- [ ] Search by socket type
- [ ] Display results with distance

### 7.4 Map View (OSMAnd Integration)
- [ ] Display all locations on OSMAnd map
- [ ] Tap location to show details
- [ ] Add location directly from map
- [ ] Use OSMAnd map via Intent (not embedded)

### 7.5 Export/Share Locations
- [ ] Export to JSON file format:
  ```json
  {
    "version": "1.0",
    "appName": "PEVPlugShare",
    "exportDate": "2025-11-07",
    "locations": [
      {
        "id": "...",
        "name": "...",
        "latitude": 0.0,
        "longitude": 0.0,
        ...
      }
    ]
  }
  ```
- [ ] Share file via email/messaging apps
- [ ] Import received locations with merge/replace options
- [ ] Intent filter for .json files to auto-open app

### 7.6 Photo Management
- [ ] Camera integration
- [ ] Gallery picker (single/multiple)
- [ ] Photo preview/delete in add/edit screens
- [ ] Metadata embedding:
  - GPS coordinates from location
  - Timestamp
  - App name: "PEVPlugShare"
  - Location name and description
  - Custom metadata in EXIF
- [ ] Local storage in `/app_data/photos/` directory
- [ ] Compression before upload

---

## Phase 8: Background Services & Sync

### 8.1 WorkManager Setup
- [ ] Periodic sync job (configurable interval)
- [ ] Retry logic with exponential backoff
- [ ] Constraints: requires network, battery optimization allowed
- [ ] Job scheduling on boot completion

### 8.2 Sync Algorithm
- [ ] Fetch latest locations from OSMAnd API
- [ ] Merge with local database:
  - Server additions: insert
  - Server updates: update if not modified locally
  - Server deletions: delete (unless modified locally)
  - Local creations: sync to server
  - Local updates: sync to server
  - Local deletions: delete on server
  - Offline queue: upload when online

### 8.3 Notifications
- [ ] Silent notification during sync (sync progress)
- [ ] Error notification if sync fails
- [ ] Sync completion notification (optional)
- [ ] Notification channel setup for Android 8+

---

## Phase 9: Error Handling & Logging

### 9.1 Error Handling
- [ ] Network error handling (timeout, no internet, 4xx, 5xx)
- [ ] Location service errors (permission denied, no GPS signal)
- [ ] API rate limiting (429 responses)
- [ ] Invalid token handling (re-auth flow)
- [ ] Database errors (corruption recovery)

### 9.2 Logging
- [ ] Structured logging (debug, info, warning, error)
- [ ] Log file storage (optional crash logs)
- [ ] Crash reporting (Firebase Crashlytics or similar)
- [ ] Analytics (app usage, feature adoption)

---

## Phase 10: Testing & QA

### 10.1 Unit Tests
- [ ] Repository layer tests
- [ ] ViewModel tests (LiveData, Flow)
- [ ] Utility function tests (distance calc, filtering)
- [ ] Data model validation

### 10.2 Integration Tests
- [ ] API mock tests (Retrofit + MockWebServer)
- [ ] Database tests (Room)
- [ ] Sync logic tests
- [ ] Offline queue tests

### 10.3 UI Tests
- [ ] Espresso tests for main screens
- [ ] Navigation tests
- [ ] Permission flow tests
- [ ] Offline/online transitions

### 10.4 Manual Testing
- [ ] Real device testing (Android 16+)
- [ ] Network condition testing (4G, WiFi, offline)
- [ ] Battery and performance profiling
- [ ] Location accuracy testing

---

## Phase 11: Build & Deployment

### 11.1 Build Configuration
- [ ] Signing key setup (debug + release)
- [ ] ProGuard/R8 obfuscation rules
- [ ] Build variants (flavor configurations)
- [ ] CI/CD pipeline setup (GitHub Actions, etc.)

### 11.2 App Packaging
- [ ] APK generation
- [ ] App bundle (AAB) for Play Store
- [ ] Version code/name strategy
- [ ] Release notes generation

### 11.3 Deployment
- [ ] Internal testing (TestFlight equivalent)
- [ ] Beta testing
- [ ] Play Store submission
- [ ] Update strategy for bug fixes

---

## Implementation Checklist by Priority

### Must Have (Phase 1-6)
- [x] OAuth2 authentication
- [x] Location list with distance sorting
- [x] Quick add location (minimal fields)
- [x] Filter by open status and capacity
- [x] Photo capture and storage
- [x] Edit/delete existing locations
- [x] Offline queue support
- [x] Background sync with WorkManager
- [x] Favorites system
- [x] Permission handling

### Should Have (Phase 7-8)
- [ ] Search functionality
- [ ] Export/import locations (JSON)
- [ ] Enhanced photo metadata
- [ ] Offline indicator
- [ ] Settings screen
- [ ] About screen with account info

### Nice to Have (Phase 9+)
- [ ] Map view integration with OSMAnd
- [ ] Batch operations (for future)
- [ ] Analytics
- [ ] Advanced location sharing
- [ ] Photo upload to online services

---

## Technology Stack

| Component | Technology |
|-----------|-----------|
| Language | Kotlin |
| Architecture | MVVM with Repository Pattern |
| UI Framework | Jetpack Compose (or Material Design 3) |
| Database | Room (SQLite) |
| API Client | Retrofit + OkHttp |
| Reactive | Coroutines + Flow |
| Location | Google Play Services Location API |
| Background Tasks | WorkManager |
| Secure Storage | EncryptedSharedPreferences |
| Preferences | DataStore |
| Image Processing | Built-in Android APIs (future: Glide/Picasso) |
| Authentication | OAuth2 (Authorization Grant Flow) |

---

## Key Decisions & Rationale

1. **Local-First Architecture**: All data stored locally with sync to OSMAnd, enabling offline usage and privacy.

2. **Room Database**: Provides type-safe access, migration support, and efficient querying over raw SQLite.

3. **WorkManager**: Preferred for background sync due to battery optimization and reliability across Android versions.

4. **Line-of-Sight Distance**: Simpler calculation, faster, and more intuitive than navigation distance for proximity-based filtering.

5. **OAuth2 Authorization Grant Flow**: Industry standard, secure token-based authentication without storing user credentials.

6. **Quick Add Focus**: Minimal required fields to reduce friction for in-ride location additions.

7. **JSON Export Format**: Human-readable, easy to parse, suitable for sharing and future platform integration.

---

## Success Criteria

- ✅ App installs and runs on Android 16+
- ✅ OAuth2 authentication works reliably
- ✅ Locations sync with OSMAnd account
- ✅ Can add/edit/delete locations offline
- ✅ Distance calculations are accurate
- ✅ All UI screens render properly
- ✅ Background sync completes without errors
- ✅ Photo storage and metadata working
- ✅ Export/import locations functional
- ✅ No crashes on main use cases

---

## Open Questions for Clarification

1. **Photo Upload to OSMAnd**: Confirm if OSMAnd API supports photo uploads or if we store locally only initially.
2. **Socket Type Extensibility**: Should we provide a predefined list or allow user custom entries for socket types?
3. **Opening Hours Format**: Confirm exact format (e.g., "9-5" vs "9:00-17:00" vs custom text)?
4. **Sync Conflict Resolution**: In case of simultaneous edits, server wins or local wins?
5. **Distance Calculation Accuracy**: For coarse location (100m), is approximate distance acceptable or need higher precision?
6. **Map View**: Should we embed OSMAnd maps or use intents to launch OSMAnd app?
7. **Batch Export**: Limit on number of locations for export/import?

---

**Status**: Ready for Review  
**Created**: 2025-11-07  
**Version**: 9.0
