# PEVPlugShare Implementation Plan v7

## Project Overview
PEVPlugShare is an Android application designed to assist EV riders in quickly adding and managing charging locations integrated with OSMAnd's "My Places" feature. The app targets Android 16+ and focuses on providing a streamlined UX for location management with minimal required inputs.

---

## Phase 1: Foundation & Authentication (Week 1-2)

### 1.1 Project Setup
- **Task**: Initialize Android project with required dependencies
- **Components**:
  - Kotlin/Java setup with latest Gradle configuration
  - Material Design 3 implementation
  - AndroidX libraries
  - Jetpack Compose (optional, for modern UI)
  - Room database for local storage
  - Retrofit + OkHttp for network requests
  - WorkManager for background sync
  - Location Services (Google Play Services)
  - File storage permissions setup

### 1.2 OSMAnd Authentication
- **Task**: Implement OAuth2 authorization grant flow
- **Components**:
  - OAuth2 authentication activity
  - Token management and storage (encrypted SharedPreferences)
  - Session persistence
  - Token refresh mechanism
  - Logout functionality
- **Deliverables**:
  - LoginActivity with OSMAnd OAuth2 flow
  - AuthManager class for token lifecycle
  - Secure token storage

### 1.3 OSMAnd API Integration
- **Task**: Establish API communication with OSMAnd backend
- **Components**:
  - API client setup (Retrofit)
  - OSMAnd endpoints mapping
  - Error handling and retry logic
  - Request/response models
- **Deliverables**:
  - OSmandApiClient class
  - API data models (Location, Category, etc.)

---

## Phase 2: Data Models & Local Storage (Week 2-3)

### 2.1 Database Schema
- **Task**: Design and implement local database for offline support
- **Entities**:
  - ChargingLocation (id, name, latitude, longitude, altitude, plugTypes[], isPublic, hasFee, capacity, openingHours, description, photos[], isDeleted, lastModified, isFavorite, createdAt, uploadedAt)
  - PlugType (id, type: AS3112/Type2/Other, quantity)
  - Photo (id, locationId, fileUri, gpxData, timestamp, description)
  - AdditionalInfo (locationId, providerName, website, estimatedCost, notes)
  - Category (id, name, isSelected)
  - SyncLog (id, timestamp, status, recordCount)

### 2.2 Room Implementation
- **Task**: Set up Room database with DAOs and relationships
- **Components**:
  - Location DAO (CRUD operations)
  - Photo DAO
  - AdditionalInfo DAO
  - Database migrations
  - Type converters for complex objects
- **Deliverables**:
  - AppDatabase class
  - Complete DAO interfaces

### 2.3 Data Sync Model
- **Task**: Implement conflict resolution strategy
- **Strategy**:
  - Last-write-wins for server conflicts
  - Track sync state (NEW, MODIFIED, SYNCED, DELETED)
  - Sync timestamp tracking
  - Offline queue for pending changes
- **Deliverables**:
  - SyncManager class with conflict resolution

---

## Phase 3: Location Services & Permissions (Week 3-4)

### 3.1 Location Permissions
- **Task**: Implement fine and coarse location permissions
- **Implementation**:
  - Fine location (GPS) for adding new locations
  - Coarse location (network/wifi) for distance calculations on main list
  - Runtime permission requests (Android 6.0+)
  - Permission state UI indicators
  - Graceful degradation without permissions
- **Deliverables**:
  - LocationPermissionManager
  - PermissionRequestActivity
  - LocationProvider abstraction

### 3.2 Distance Calculations
- **Task**: Implement line-of-sight distance calculations
- **Implementation**:
  - Haversine formula for great-circle distance
  - Location sorting by distance
  - Real-time distance updates
  - Caching for performance
- **Deliverables**:
  - DistanceCalculator utility
  - Location repository with distance sorting

### 3.3 GPS Data Integration
- **Task**: Capture current location for new entries
- **Implementation**:
  - LocationManager/FusedLocationProviderClient
  - Accuracy threshold validation
  - Duplicate detection (250m radius)
  - Background location updates
- **Deliverables**:
  - GpsLocationProvider class
  - DuplicateDetectionService

---

## Phase 4: UI - Main Screen & Location List (Week 4-6)

### 4.1 Main Activity
- **Task**: Implement primary list view with location data
- **Components**:
  - RecyclerView for location list
  - Location card layout (name, distance, capacity, status)
  - Status indicator (open/closed/unknown)
  - Navigation button for each location
  - Swipe-to-favorites functionality
- **Features**:
  - Sort by distance (default)
  - Scroll with offline indicator
  - Pull-to-refresh sync
  - Offline mode banner
- **Deliverables**:
  - MainActivity
  - LocationListAdapter
  - LocationCard composable/layout

### 4.2 Filters & Search
- **Task**: Implement filtering UI at top of list
- **Filters**:
  - Open Now / All Hours toggle
  - Minimum capacity filter (spinner/slider)
  - Favorites only toggle
  - Fee status filter (Has Fee / Free / All)
- **Search**:
  - Search via menu (not main screen)
  - Search by location name
  - Search by provider name (if data available)
- **Deliverables**:
  - FilterFragment or BottomSheet
  - SearchActivity
  - FilterViewModel with LiveData

### 4.3 Favorites System
- **Task**: Implement bookmark functionality
- **Features**:
  - Toggle favorite on location card
  - Persist to database
  - Filter list to favorites only
  - Visual indicator (star icon)
- **Deliverables**:
  - FavoritesRepository
  - FavoritesToggleUseCase

---

## Phase 5: Location Management - Add/Edit (Week 6-8)

### 5.1 Add Location Flow (Quick Add)
- **Task**: Implement streamlined location addition
- **Required Fields**:
  - Location Name (text input)
  - Number of GPO / Socket Types (spinner + quantity for AS3112 primary, Type2 secondary, others)
  - Current GPS Location (auto-captured)
- **Optional Fields** (available in initial screen with expandable):
  - Opening Hours (predefined options: 9-5, 24x7, Weekdays, etc.)
  - Description (text)
  - Photo capture
  - Public/Private flag
  - Fee status flag
  - Additional Info (provider, website, costs, notes)
- **Duplicate Detection**:
  - Check for locations within 250m radius
  - Show confirmation dialog if duplicates found
  - Allow override if user confirms
- **Auto-fill**:
  - Category icon/color from selected "My Places" category
  - Timestamp and GPS altitude
- **Deliverables**:
  - AddLocationActivity
  - PlugTypeSelector component
  - DuplicateDetectionDialog

### 5.2 Edit Location Flow
- **Task**: Implement location modification
- **Features**:
  - Load existing location data
  - Edit all fields
  - Add additional photos
  - Delete location with confirmation
  - Track modification timestamp
- **Deliverables**:
  - EditLocationActivity (shared with Add using intent extras)
  - LocationEditorViewModel

### 5.3 Photo Management
- **Task**: Handle photo capture and storage
- **Implementation**:
  - Camera integration (intent or CameraX)
  - Photo storage to app-specific directory
  - Metadata injection (GPX data, location name, description, timestamp, app info)
  - EXIF data preservation and enhancement
  - Gallery/photo picker integration
  - Photo compression/optimization
  - Local-only storage (no upload in v1)
  - Photo deletion
- **Deliverables**:
  - PhotoCaptureManager
  - PhotoMetadataInjector
  - PhotoAdapter for preview/editing

### 5.4 Opening Hours
- **Task**: Simplified opening hours input
- **Implementation**:
  - Predefined options: 24/7, 9-5, Weekdays Only, Custom
  - Custom time picker for start/end times
  - Day selection (all days, weekdays, specific days)
  - Display in location view
- **Deliverables**:
  - OpeningHoursSelector
  - OpeningHoursModel with display formatting

---

## Phase 6: Background Sync & Offline Support (Week 8-9)

### 6.1 Sync Service
- **Task**: Implement periodic background syncing
- **Features**:
  - WorkManager for scheduled sync (default 1 day, configurable)
  - Manual sync trigger
  - Sync status tracking
  - Conflict resolution (last-write-wins)
  - Bandwidth awareness (sync on WiFi preferably)
  - Exponential backoff on failures
- **Deliverables**:
  - SyncWorker (WorkManager)
  - SyncScheduler
  - SyncRepository

### 6.2 Offline Indicators
- **Task**: Visual feedback for connectivity status
- **Components**:
  - Network state monitoring (ConnectivityManager)
  - Topbar offline badge
  - About screen connectivity status
  - Sync status in notification
  - Queue status display
- **Deliverables**:
  - NetworkStateManager
  - OfflineIndicatorView

### 6.3 Offline Queue
- **Task**: Store changes while offline, sync when online
- **Features**:
  - Queue new locations
  - Queue edits to existing locations
  - Queue location deletions
  - Pending item counter
  - Auto-sync when connectivity restored
  - Manual queue flush
- **Deliverables**:
  - OfflineQueueManager
  - QueuedOperation model

---

## Phase 7: UI - Top Bar & Menu (Week 9-10)

### 7.1 Top App Bar
- **Task**: Implement hamburger menu and status indicators
- **Components**:
  - Hamburger menu button
  - App title
  - Offline indicator badge
  - Sync status spinner (during sync)
  - Sync counter (pending changes count)
- **Deliverables**:
  - TopAppBar composable/layout
  - AppBarViewModel

### 7.2 Navigation Drawer Menu
- **Task**: Implement settings, about, and utility options
- **Menu Items**:
  - Settings
  - About
  - Search (icon/menu item)
  - Resync Now
  - Export Locations
  - Help/Documentation
  - Logout
- **Deliverables**:
  - NavigationDrawer component
  - MenuClickListener handling

### 7.3 Settings Screen
- **Task**: User configuration options
- **Settings**:
  - Select "My Places" category (spinner with categories from OSMAnd)
  - Sync frequency (4/8/12 hours, 1/2/5/14/28 days)
  - Photo storage location selection
  - Clear offline queue option
  - App permissions overview
  - About app version
- **Deliverables**:
  - SettingsActivity/Fragment
  - PreferenceManager wrapper

---

## Phase 8: About Screen & Information (Week 10)

### 8.1 About Screen
- **Task**: Display app and account information
- **Sections**:
  - App Version (version number, build date/time)
  - Basic usage statistics
  - Account Information (OSMAnd account, last sync time)
  - Offline status
  - Features list
  - Open source libraries acknowledgment
- **Deliverables**:
  - AboutActivity
  - VersionInfoManager

---

## Phase 9: OSMAnd Navigation Integration (Week 10-11)

### 9.1 Navigation Intent
- **Task**: Launch OSMAnd navigation to location
- **Implementation**:
  - Intent to OSMAnd with coordinates
  - Handle app not installed scenario
  - Navigate button on location card and detail view
  - Safe intent creation
- **Deliverables**:
  - OsmandNavigationLauncher
  - IntentUtils

---

## Phase 10: Export & Sharing (Week 11)

### 10.1 Export Functionality
- **Task**: Export locations to shareable JSON format
- **Features**:
  - Export all locations
  - Export selected locations
  - JSON schema with all location data
  - Include photos (embedded as base64 or separate)
  - File storage to device download folder
  - Share via Intent (email, messaging, storage)
- **Deliverables**:
  - LocationExporter
  - JsonLocationSerializer
  - ExportDialog

### 10.2 Import Functionality
- **Task**: Import shared location files
- **Features**:
  - File picker integration
  - JSON validation
  - Merge/Replace options per location
  - Conflict resolution UI
  - Duplicate detection during import
  - Status feedback
- **Deliverables**:
  - LocationImporter
  - JsonLocationDeserializer
  - MergeConflictDialog

### 10.3 Deep Linking
- **Task**: Handle app launch from shared files
- **Implementation**:
  - Register content provider for JSON files
  - Intent filters for .json files
  - Launch import flow on app open
  - Pass file URI to import activity
- **Deliverables**:
  - ContentProvider for location files
  - DeepLinkHandler

---

## Phase 11: Category Selection (Week 11-12)

### 11.1 Category Selection Flow
- **Task**: Implement category selection on first launch
- **Features**:
  - Fetch categories from OSMAnd account
  - Display list with category names
  - Filter for charging-related categories (UX suggestion)
  - Store selected category preference
  - Allow changing category in settings
  - Auto-apply category icon/color to new locations
- **Deliverables**:
  - CategorySelectionActivity
  - CategoryRepository
  - CategoryManager

---

## Phase 12: Testing & Quality Assurance (Week 12-14)

### 12.1 Unit Tests
- **Coverage**:
  - Distance calculation tests
  - Duplicate detection tests
  - Data model tests
  - Sync logic tests
  - Filter logic tests
  - Export/Import tests
- **Tools**: JUnit, Mockito

### 12.2 Integration Tests
- **Coverage**:
  - Database operations
  - API communication mocks
  - Sync workflow
  - Offline queue operations
- **Tools**: Espresso, JUnit

### 12.3 UI Tests
- **Coverage**:
  - Navigation flows
  - Add/Edit location workflows
  - Filter and search functionality
  - Menu operations
- **Tools**: Espresso, Compose Testing

### 12.4 Manual Testing
- **Scenarios**:
  - Offline functionality
  - Long-term sync reliability
  - Photo metadata preservation
  - Distance calculations accuracy
  - Duplicate detection edge cases
  - Permission handling on various Android versions

---

## Phase 13: Optimization & Polish (Week 14-15)

### 13.1 Performance Optimization
- **Tasks**:
  - Lazy loading for location list (pagination)
  - Image lazy loading
  - Database query optimization
  - Memory leak detection
  - ANR prevention
  - Crash reporting setup

### 13.2 UI/UX Polish
- **Tasks**:
  - Animations for transitions
  - Loading states for all async operations
  - Error messages and user guidance
  - Accessibility (TalkBack support, contrast ratios)
  - Keyboard navigation
  - Input validation and error feedback

### 13.3 Documentation
- **Tasks**:
  - In-app help text
  - User guide (optional)
  - API documentation (internal)
  - Code comments for complex logic

---

## Phase 14: Release Preparation (Week 15-16)

### 14.1 Build & Release
- **Tasks**:
  - Release build configuration
  - ProGuard/R8 rules for code obfuscation
  - Signing key setup
  - Version bump to 1.0
  - Release notes preparation
  - PlayStore store listing (if needed)

### 14.2 Deployment
- **Tasks**:
  - Beta testing APK generation
  - Device testing on target Android 16+
  - PlayStore submission (if applicable)
  - Social media/community announcement

---

## Architecture Overview

### MVVM Architecture
- **Model**: Room entities, repositories
- **View**: Activities, Fragments, Composables
- **ViewModel**: Jetpack ViewModel with LiveData/StateFlow

### Repository Pattern
- Abstraction of data sources (local DB, remote API)
- Single source of truth (Room database)
- Offline-first approach

### Dependency Injection
- Manual or Hilt (Google's DI framework)
- Easy testing and modularity

### Key Design Patterns
- Observer pattern (LiveData/Flow)
- Singleton (API client, Database)
- Factory (DAO creation)
- Strategy (Sync strategies)

---

## Technology Stack

| Component | Technology |
|-----------|-----------|
| Language | Kotlin |
| UI Framework | XML Layouts / Jetpack Compose (optional) |
| Database | Room |
| Networking | Retrofit + OkHttp |
| Async/Background | Coroutines + WorkManager |
| Location | Google Play Services - Location |
| DI | Hilt (optional) or manual |
| Testing | JUnit, Mockito, Espresso |
| Build Tool | Gradle |
| VCS | Git |

---

## Functional Requirements Summary

✅ OAuth2 authentication to OSMAnd  
✅ Fetch and display "My Places" categories  
✅ List locations sorted by distance (coarse location permission)  
✅ Add new location (fine location permission, quick add with name + socket types required)  
✅ Edit existing location  
✅ Delete location with confirmation  
✅ Photo capture and local storage with metadata injection  
✅ Opening hours (predefined + custom options)  
✅ Socket type support (AS3112 primary, Type2, other options)  
✅ Public/Private flag  
✅ Fee status flag  
✅ Additional info section (provider, website, costs, notes)  
✅ Favorites/Bookmarks system  
✅ Offline support with sync queue  
✅ Background sync (scheduled, configurable frequency)  
✅ Network status indicators  
✅ Duplicate detection (250m radius)  
✅ OSMAnd navigation intent  
✅ Settings for category and sync frequency  
✅ About screen with version/account info  
✅ Export locations to JSON  
✅ Import locations from JSON with merge/replace  
✅ Search functionality (menu-accessible)  
✅ Filter by open hours, capacity, favorites, fee status  

---

## Non-Functional Requirements

✅ Local-first architecture (no external paid APIs for map)  
✅ Offline-first design  
✅ Line-of-sight distance calculations  
✅ Encrypted token storage  
✅ Efficient sync (last-write-wins conflict resolution)  
✅ App-specific photo storage  
✅ GPX metadata in photos  
✅ Responsive UI  
✅ Battery-efficient location updates  
✅ Data persistence through app restarts  
✅ Graceful permission handling  

---

## Future Enhancements (Not in v1)

- Map view (local OSM data or offline map)
- Photo upload to OSMAnd
- Photo upload to Mapillary/Dropbox/OneDrive/Google Drive
- Batch operations
- Advanced search (location radius, distance)
- User reviews/ratings
- Real-time collaborator updates
- Cloud backup to alternative services
- Multi-device synchronization
- Photo gallery on location view
- Statistics dashboard
- Route planning integration
- Location sharing with expiry
- QR code generation for locations

---

## Risk Mitigation

| Risk | Mitigation |
|------|-----------|
| OSMAnd API changes | Monitor OSMAnd docs, version compatibility checks |
| Offline sync conflicts | Implement robust conflict resolution tests |
| Photo storage space | Implement compression, cleanup options |
| Battery drain from GPS | Use coarse location for distance calc, batch updates |
| Network timeouts | Implement exponential backoff, timeout configurations |
| Data loss on app crash | Database transactions, backup strategy |
| Location permission denial | Graceful UI fallback, clear permission explanations |

---

## Success Criteria

- ✅ App successfully authenticates with OSMAnd OAuth2
- ✅ Can add new location with just name and socket count in <30 seconds
- ✅ Location list displays sorted by distance within 1 second
- ✅ Photos properly store GPX and location metadata
- ✅ Offline sync queue reliably syncs on reconnection
- ✅ No crashes or ANRs under normal usage
- ✅ Duplicate detection works within 250m radius
- ✅ Export/Import preserves all location data
- ✅ Supports Android 16+
- ✅ Battery impact <5% over 24 hours idle

---

## Questions for Further Clarification

1. **Map Display**: Should location list show any map preview, or is list-only acceptable?
2. **Photo Storage Quota**: Any limits on local photo storage? Suggest automatic compression if >100MB per location?
3. **Batch Operations**: Confirmed no batch add/edit/delete in v1?
4. **Opening Hours**: Should app validate if location is open based on current time?
5. **Provider Lookup**: Future feature to detect charging providers by coordinates (PlugShare API)?
6. **Real-time Updates**: Should app pull updates from other users (crowd-sourced) or just OSMAnd My Places?
7. **Analytics**: Should app collect anonymous usage metrics (no personal data)?
8. **Accessibility**: Priority level for TalkBack/Accessibility support?

---

## Revision History

| Version | Date | Changes |
|---------|------|---------|
| v7 | 2025-11-07 | Initial comprehensive plan with 14 phases, technology stack, and detailed requirements |

