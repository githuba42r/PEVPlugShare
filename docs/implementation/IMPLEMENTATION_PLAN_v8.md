# PEVPlugShare - Comprehensive Implementation Plan v8
**Review Document - For Approval Before Development Continues**

---

## Executive Summary

PEVPlugShare is an Android application designed to help users quickly add and manage electric vehicle (EV) charging locations in OSMAnd's "My Places" category. The app emphasizes rapid data entry, offline functionality, and seamless integration with OSMAnd for navigation.

**Target Users**: EV/PEV riders who want crowdsourced charging location data synchronized with OSMAnd.

**Core Value Proposition**: 
- ⚡ Quick location entry (2-3 taps) during ride breaks
- 🗺️ OSMAnd navigation integration
- 📱 Full offline support with auto-sync
- 🔒 Secure OAuth2 authentication
- ❤️ Favorite marking and filtering

---

## Phase Overview (12 Phases Total)

### ✅ Completed Phases 1-4 (Foundation)
1. **Phase 1**: Project setup, Room database, Hilt DI, Material Design
2. **Phase 2**: OAuth2 authentication, Retrofit API client, token management
3. **Phase 3**: Location list UI, GPS integration, distance calculations
4. **Phase 4**: Add location form with validation

### 📋 Planned Phases 5-12 (Remaining)
5. **Phase 5**: Photo capture & storage (local only)
6. **Phase 6**: Edit/delete locations with photos
7. **Phase 7**: Offline support & auto-sync
8. **Phase 8**: Duplicate detection (250m radius)
9. **Phase 9**: Search & advanced filtering
10. **Phase 10**: Export/Import with JSON sharing
11. **Phase 11**: Settings & preferences
12. **Phase 12**: Map view & advanced features

---

## Detailed Requirements by Feature Area

### 1. Location Data Model

#### Core Location Fields
```
- id: String (UUID)
- name: String (REQUIRED)
- description: String (optional)
- latitude: Double (REQUIRED - captured on add)
- longitude: Double (REQUIRED - captured on add)
- socketTypes: List<String> (REQUIRED, min 1)
  * AS3112 (AU GPO) - PRIMARY
  * Type2 EV
  * Other EV Charger
- capacity: Int (REQUIRED - number of GPO/charging points)
- accessType: String (REQUIRED for later phases)
  * PUBLIC - public area (park, shared center)
  * PRIVATE - private business
- hasFee: Boolean (REQUIRED)
- feeDetails: FeeDetails (optional, shown if hasFee=true)
  - providerName: String
  - providerWebsite: String
  - approximateCost: String
  - additionalInfo: String
- openingHours: String (optional)
- photos: List<Photo> (optional, Phase 5+)
- isFavorite: Boolean (default: false, Phase 3+)
- syncStatus: Enum (NEW vs PENDING vs SYNCED vs FAILED)
- lastModified: Long
- createdAt: Long
```

#### Photo Model
```
- id: String
- locationId: String
- photoPath: String (local storage path)
- gpsData: GPSData (location + accuracy)
  - latitude, longitude, altitude, accuracy
- metadata: PhotoMetadata
  - timestamp, appName, locationName, description
  - exifData (camera info if available)
- syncStatus: String (LOCAL, SYNCED, FAILED)
```

---

### 2. User Authentication

**Status**: ✅ Phase 2 Complete

#### Implementation
- OAuth2 Authorization Code Grant flow
- EncryptedSharedPreferences for token storage (AES-256-GCM)
- Token refresh mechanism
- Automatic logout on 401/403
- Login activity with browser-based OAuth

#### Security
- No passwords stored locally
- Tokens encrypted at rest
- HTTPS for all API calls
- Clean logout clears all token data

---

### 3. Main Location List Screen

**Status**: ✅ Phase 3 Complete

#### Features
- **List Display**
  - Sorted by distance (nearest first)
  - Shows: Name, Distance, GPO Count, Socket Types
  - Color/icon from OSMAnd category

- **Location Highlighting**
  - ✅ Highlight locations open at current time (visual indicator)
  - Allow navigation to closed locations
  - Visual distinction for favorites

- **Filters**
  - Open/All (time-based)
  - Minimum capacity (number of GPO)
  - Favorites only (Phase 3+)
  - Socket type (Phase 9+)
  - Access type: Public/Private (Phase 9+)
  - Fee status (Phase 9+)

- **Location Actions**
  - Navigate via OSMAnd (intent)
  - View details
  - Mark/unmark favorite
  - Edit (Phase 6+)
  - Delete (Phase 6+)

- **Distance Calculation**
  - Line-of-sight (Haversine formula), NOT navigation distance
  - Coarse location permission for list display
  - Updates as user moves

- **Sync Indicator**
  - Shows current sync status
  - Indicates pending operations
  - Manual refresh button

- **Offline Indicator**
  - Top bar shows "OFFLINE" when no network
  - About screen shows connectivity status

---

### 4. Add Location Screen

**Status**: ✅ Phase 4 Complete (Core logic)

#### UX Approach: Quick Entry Model
**Design Philosophy**: Minimize friction during ride. User adds location in seconds, edits details later during breaks.

#### Add Screen - REQUIRED Fields Only
```
Screen 1: Location Capture
- Location Name (text input, REQUIRED)
- GPS Capture Button (one tap to get current location)
  - Shows "Capturing..." during acquisition
  - Shows coordinates once captured
- Number of GPO/Charging Points (spinner 1-20, REQUIRED)

Screen 2: Socket Types & Access
- Primary Socket Type (radio group, REQUIRED)
  * AS3112 (default, selected)
  * Type2 EV
  * Other EV Charger
  * [Add secondary socket types - Phase 6+]
- Access Type (radio group, REQUIRED)
  * PUBLIC
  * PRIVATE
- Has Fee? (toggle, REQUIRED)

Screen 3: Optional Details (Collapsible)
- Description (text area, optional)
- Opening Hours (time picker / preset options, optional)
- Fee Details (shown only if "Has Fee" = true)
  * Provider Name
  * Provider Website
  * Approximate Cost
  * Additional Info
- Photo buttons (Phase 5+)

Submit Flow
- Validate required fields
- Check for duplicates within 250m (Phase 8+)
- If duplicate found:
  * Show warning: "Location found within 250m"
  * Ask: "Do you want to add anyway?"
  * User can inspect existing location or proceed
- Submit to OSMAnd API
- Show success/error toast
- Return to location list
```

#### Field Validation
```
- Name: non-empty, max 100 chars
- GPO Count: 1-999
- Socket Type: at least one selected
- GPS: valid coordinates
- Fee Details: required if hasFee=true
- Opening Hours: time format validation
```

#### Form Persistence
- Save draft when user navigates away
- Restore draft on return to add screen
- Clear draft on successful submit

#### Error Handling
```
- API error: Show toast with retry button
- GPS error: Show "Unable to get location" with manual entry option
- Network error: Queue for offline sync (Phase 7+)
- Duplicate detected: Confirmation dialog with inspection option
```

---

### 5. Photos (Phase 5)

#### Storage Strategy
- **Local Storage Only** in v1
- Store in app's internal files directory: `getFilesDir()/photos/`
- Organize by locationId for easy cleanup
- Include EXIF/metadata in files

#### Photo Capture Flow
```
- Photo button on add/edit screen
- Opens camera via intent
- Save with metadata:
  * GPS coordinates of location
  * App name ("PEVPlugShare")
  * Location name
  * Location description
  * Timestamp
  * Camera EXIF data (if available)
- Multiple photos per location (store as list)
- Photo preview thumbnail in list/detail
```

#### Metadata Attachment
- Use ExifInterface to embed:
  - GPS data
  - Timestamp
  - Custom tags (app name, location description)
- Store JSON metadata file alongside photo for v1
- Enable JSON export of photo metadata (Phase 10+)

#### Future Considerations
- v2: Upload to OSMAnd if API supports
- v3: Integration with Mapillary, Dropbox, OneDrive, Google Drive

---

### 6. Edit Location (Phase 6)

#### Features
- Load existing location data into form
- Edit all fields (name, GPS, socket types, etc.)
- Add/remove/view photos
- Update optional details
- Submit changes to OSMAnd API
- Handle conflicts (server has newer version)

#### Conflict Resolution
- Detect if location modified on server since load
- Show warning: "This location was updated by another user"
- Options:
  * View server version
  * Keep local changes
  * Take server version
  * Manual merge

#### Delete Confirmation
- Confirmation dialog
- Show location name, address
- Option to cancel
- Permanent deletion from OSMAnd

---

### 7. Offline Support (Phase 7)

#### Architecture
```
PendingOperation Queue (Room Database)
├─ Add Location (offline)
├─ Edit Location (offline)
├─ Delete Location (offline)
└─ Photo Upload (offline)
```

#### Implementation
- **PendingOperationEntity**
  - id, operationType, locationId, operationData, timestamp, retryCount, status
- **ConnectivityObserver**
  - Monitors network state changes
  - WorkManager integration
- **SyncManager**
  - Manages sync logic
  - Retry with exponential backoff
  - Conflict resolution
- **AutoSyncWorker**
  - Periodic background sync
  - Triggered on connectivity change

#### Offline Operations
```
When Offline:
1. User adds/edits location
2. Queue to PendingOperation table
3. Mark location as PENDING
4. Show queued indicator
5. Continue normal use

When Online:
1. Detect connectivity change
2. AutoSyncWorker triggered
3. Process queue (1-3 retries)
4. Mark as SYNCED on success
5. Show sync notification
6. User sees location updated

If Sync Fails:
- Retry with exponential backoff (1s, 2s, 4s, 8s)
- Show error indicator in UI
- Manual retry option in settings
```

#### Offline Indicator
- **Top Bar**: "OFFLINE" badge when no network
- **List View**: Each pending location shows "⏳ Pending Sync" badge
- **About Screen**: "Last Sync: X minutes ago" + connectivity status
- **Sync Notification**: Silent notification during active sync

#### Cached Data
- All locations remain cached in Room
- Accessible even offline
- Sync updates cache when online

---

### 8. Duplicate Detection (Phase 8)

#### Algorithm
```
When User Adds Location:
1. Get user's current location (fine resolution)
2. Query nearby locations within 250m radius
3. Calculate Haversine distance to each
4. If any found:
   - Show "Duplicate Location Alert"
   - Display nearby location details
   - Option to "Inspect" (view in details)
   - Option to "Add Anyway"
   - Option to "Cancel"
5. If user proceeds, add new location
6. Mark as potential duplicate in metadata
```

#### Duplicate Threshold
- **250 meters** radius (configurable in Phase 11)
- Line-of-sight calculation (Haversine), NOT road distance
- Exact coordinate matching (< 1 meter) = likely duplicate

#### User Experience
```
"⚠️ Charging Location Found Nearby

Existing Location:
📍 Shell Charging Station
Distance: 120m
GPO Count: 6

Do you want to:
[View Location] [Add Anyway] [Cancel]"
```

---

### 9. Search & Filtering (Phase 9)

#### Search Features
- Search by location name
- Search by provider name
- Not primary UI (accessible from menu)
- Case-insensitive substring matching
- Results sorted by distance

#### Advanced Filtering
```
Filter Chips (on main list):
- Open/All (time-based) ✅ Phase 3
- Minimum Capacity (number of GPO)
- Favorites Only ✅ Phase 3
- Socket Type (AS3112 / Type2 / Other)
- Access Type (Public / Private)
- Fee Status (Free / Paid)
- Distance Range (1km / 5km / 10km / All)

Applied filters shown as active chips
Clear all filters button
Persistent filter state across sessions
```

#### Search Experience
```
Menu → Search
- Text input
- Real-time results as user types
- Results show distance, socket types
- Tap to view full details
- Navigate from search results

OR use filters on main list for most common cases
```

---

### 10. Export & Import (Phase 10)

#### Export Format
```json
{
  "version": "1.0",
  "app": "PEVPlugShare",
  "timestamp": "2025-11-07T10:30:00Z",
  "locationCount": 3,
  "locations": [
    {
      "id": "uuid-1",
      "name": "Location Name",
      "latitude": -33.8688,
      "longitude": 151.2093,
      "socketTypes": ["AS3112", "Type2"],
      "capacity": 4,
      "accessType": "PUBLIC",
      "hasFee": true,
      "feeDetails": {...},
      "openingHours": "9-17",
      "description": "...",
      "isFavorite": true,
      "createdAt": "...",
      "lastModified": "..."
    }
  ],
  "metadata": {
    "totalCount": 3,
    "osmandCategory": "PEV Charging Locations"
  }
}
```

#### Export Options
- **Share Button**: Export visible locations as JSON
- **Email/Messaging**: Send JSON file
- **Storage**: Save to Downloads folder
- **Multiple Formats**: JSON (v1), CSV (future)

#### Import Options
- **Receive Shared File**: User opens .json file
- **Intent Handler**: App registers for .json files
- **Import Dialog**: Shows import preview
  * File details
  * Location count
  * Merge strategy selection

#### Merge Strategies
```
For each location in import file:
1. NEW: Add if doesn't exist locally
2. SKIP: Ignore if exists
3. REPLACE: Overwrite with import data
4. ASK_USER: Show dialog for each

Conflict Detection:
- Match by ID first
- Then by name + coordinates (within 5m)
- Show conflicts to user
- Let user choose strategy
```

---

### 11. Settings & Preferences (Phase 11)

#### User Settings
```
Screen 1: OSMAnd Integration
- Selected Category (dropdown, changeable)
- Last Sync Time
- Manual Sync Button
- Auto-Sync Frequency (dropdown)
  * 4/8/12 hours
  * 1/2/5/14/28 days
  * Default: 1 day

Screen 2: Location Preferences
- Duplicate Detection Distance (250m default)
- Minimum Capacity Filter Default
- Default Socket Type
- Default Access Type

Screen 3: Units & Display
- Distance Units (km / miles)
- Theme (Light / Dark / System default)
- 24-hour time format (toggle)

Screen 4: Advanced
- Clear Cache
- Export All Locations
- Data Privacy Policy
- App Version & Build Info
```

#### Persistent Storage
- SharedPreferences for simple settings
- Room database for complex preferences
- Migration path for future schema changes

---

### 12. Map View (Phase 12 - Optional)

#### Approach: Local-Only, No Paid APIs

#### Options Considered
1. **OSMAnd Integration**: Limited API for embedded maps
2. **Google Maps**: Paid API (RULED OUT)
3. **Mapbox**: Paid API (RULED OUT)
4. **OpenStreetMap + Leaflet**: Web-based (can embed)
5. **OSMdroid**: Open source, offline-capable (RECOMMENDED)

#### Phase 12 Implementation
- Use OSMdroid library for offline map rendering
- Mark locations as pins/markers on map
- Cluster markers at high zoom levels
- Tap marker to view location details
- Tap "Navigate" to send to OSMAnd
- Color-coded markers (favorites, open/closed, etc.)

#### Limitations
- Map tiles downloaded separately (user controlled)
- No real-time traffic
- No turn-by-turn routing (use OSMAnd for that)

---

## Technical Architecture

### Technology Stack
```
Language: Kotlin 100%
Min API: 26 (Android 8+)
Target API: 34 (Android 15+)

Build System
├─ Gradle 8.6+
├─ Kotlin Gradle Plugin 1.9.x
└─ Android Gradle Plugin 8.x

Networking
├─ Retrofit2 2.11.0
└─ OkHttp3 4.x

Database
├─ Room 2.6.1
└─ Encrypted SharedPreferences

UI/UX
├─ Material Design 3
├─ AndroidX (lifecycle, navigation, etc.)
└─ Coroutines + StateFlow

Location Services
├─ Google Play Services (Fused Location)
└─ Haversine for distance calc

Background Jobs
├─ WorkManager 2.8.x
└─ Hilt for DI

Security
├─ OAuth2 (Authorization Code Grant)
├─ AES-256-GCM encryption (tokens)
└─ EncryptedSharedPreferences
```

### Architectural Pattern
```
UI Layer (Activities/ViewModels)
        ↓
Domain Layer (Repositories/Use Cases)
        ↓
Data Layer (API Client/Local DB)
        ↓
External Services (OSMAnd API, Google Play Services)
```

### Database Schema (Room)
```
Tables:
├─ Location (main data)
├─ Photo (attached to locations)
├─ Category (OSMAnd categories)
├─ User (account info)
├─ PendingOperation (offline queue)
└─ SyncLog (sync history)

Relationships:
- Location → Category (many-to-one)
- Location → Photo (one-to-many)
- Location → PendingOperation (one-to-many)
```

---

## Permission Requirements

### Requested Permissions
```xml
<!-- Location Services -->
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
  └─ Used: When adding location (capture current GPS)
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
  └─ Used: List view distance calculations

<!-- Camera -->
<uses-permission android:name="android.permission.CAMERA" />
  └─ Used: Phase 5 - photo capture

<!-- Network -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

<!-- Storage -->
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
  └─ Used: Photo storage (Phase 5+)
```

### Runtime Permissions
- Fine location: Requested on "Add Location" screen
- Coarse location: Requested on app startup
- Camera: Requested on first photo capture
- All with graceful degradation if denied

---

## API Integration: OSMAnd

### Endpoints Used
```
Authentication
POST /auth/access-token → OAuth2 token exchange

User & Categories
GET /account/info → User profile
GET /resources/categories → List user's categories

Locations
GET /resources/locations?categoryId={id} → Get locations
POST /resources/locations → Create location
PUT /resources/locations/{id} → Update location
DELETE /resources/locations/{id} → Delete location
GET /resources/changes?categoryId={id} → Get changes since timestamp

Photos (if available)
POST /resources/locations/{id}/photos → Upload photo
GET /resources/locations/{id}/photos → Download photos
```

### Error Handling
```
- 401/403: Redirect to login (expired token)
- 409: Conflict (duplicate/newer version exists)
- 400: Validation error (show user message)
- 500: Server error (retry with exponential backoff)
- Network: Queue for offline sync (Phase 7)
```

### Rate Limiting
- Check OSMAnd API rate limits
- Implement exponential backoff for retries
- Batch operations where possible

---

## User Interface Layouts

### Main Screens
1. **SplashActivity** - Loading, auth check
2. **LoginActivity** - OAuth2 flow
3. **MainActivity** - Location list
4. **AddLocationActivity** - Add form (multi-screen)
5. **LocationDetailActivity** - View/edit location
6. **SettingsActivity** - User preferences
7. **AboutActivity** - App info, account info

### Dialogs
- Duplicate Location Alert
- Offline Indicator
- Sync Status Notification
- Error Messages
- Confirmation (delete, logout)

### Material Design Components
- AppBarLayout with offline indicator
- FloatingActionButton for add
- CardView for location items
- RecyclerView with swipe/long-click
- BottomSheet for filters
- Material buttons, text fields, chips

---

## Testing Strategy

### Unit Tests
- Repository methods (with mocks)
- ViewModel logic
- Validation functions
- Distance calculations
- Date/time formatting

### Integration Tests
- API client calls
- Database operations
- OAuth flow
- Navigation transitions

### UI Tests
- Add location flow
- List filtering
- Edit/delete operations
- Offline mode switching
- Sync operations

### Manual Testing
- GPS accuracy variations
- Network interruption scenarios
- Battery drain (location polling)
- Memory usage (large location lists)

---

## Performance Targets

### Build Time
- Clean build: <30s
- Incremental build: <10s

### Runtime
- List scroll: 60 FPS
- Location load: <500ms
- Distance calculation: <100ms per location
- API call: <2s average

### APK Size
- Debug: ~8-10MB
- Release: ~6-7MB (estimated)

### Battery Impact
- Location polling: Balanced accuracy
- Background sync: Minimal (WorkManager)
- Photo storage: No continuous access

---

## Security Considerations

### Authentication
- ✅ OAuth2 (no password storage)
- ✅ Token encryption (AES-256-GCM)
- ✅ Token refresh mechanism
- ✅ Secure logout

### Data Protection
- ✅ HTTPS only for API calls
- ✅ Encrypted local database (Room)
- ✅ No sensitive data in logs
- ✅ Permission checks before GPS access

### Privacy
- ✅ No analytics/tracking (Phase 11 optional)
- ✅ Photos stored locally only
- ✅ User controls what data syncs
- ✅ Clear data on logout

---

## Deployment Checklist

### Pre-Release
- [ ] Full test coverage (unit + integration)
- [ ] Performance optimization
- [ ] Signed APK generation
- [ ] ProGuard/R8 minification
- [ ] Play Store configuration
- [ ] Privacy policy & ToS
- [ ] Screenshots for all screen sizes
- [ ] App icon (all densities)
- [ ] Release notes
- [ ] Beta testing (internal)

### Post-Release
- [ ] Monitor crash reports
- [ ] User feedback collection
- [ ] Metrics/analytics review
- [ ] Bug fix prioritization

---

## Known Limitations & Future Enhancements

### Current Limitations (v1)
- Photos stored locally only (not synced to OSMAnd)
- No map view embedded in app (use OSMAnd for maps)
- No analytics or usage metrics
- No push notifications for updates
- No batch editing operations

### Planned Enhancements (v2+)
- [ ] Photo upload to OSMAnd API (if supported)
- [ ] Integration with Mapillary/Dropbox/Google Drive
- [ ] Map view with OSMdroid
- [ ] Push notifications for nearby new locations
- [ ] Batch edit/delete operations
- [ ] Multi-language support
- [ ] Dark mode theme options
- [ ] Integration with other charger databases (ChargeFinder, etc.)

### Backward Compatibility
- Database migrations for schema changes
- Versioned API responses
- Graceful degradation for new features

---

## Questions for Review & Approval

Before proceeding with Phases 5-12, please confirm:

### 1. **Photo Storage**
- ✅ Confirmed: Local storage only in v1
- ✅ Confirmed: Include GPS/EXIF metadata in photos
- Question: Maximum photos per location? (Suggested: 5-10)

### 2. **Offline Mode**
- ✅ Confirmed: Queue add/edit operations offline
- ✅ Confirmed: Auto-sync when online
- ✅ Confirmed: Offline indicator in top bar
- Question: Sync frequency when online? (Suggested: immediate vs batched)

### 3. **Socket Types**
- ✅ Confirmed: AS3112 primary, Type2, Other EV as options
- ✅ Confirmed: Allow multiple socket types per location
- Question: Support for CCS/CHAdeMO in future? (Suggested: yes)

### 4. **Public/Private Flags**
- ✅ Confirmed: Add public/private flag to each location
- ✅ Confirmed: Add paid/free flag to each location
- ✅ Confirmed: Add provider info for paid locations
- Question: How to validate public vs private? (Suggested: user self-reports)

### 5. **Permissions**
- ✅ Confirmed: Fine location for adding location
- ✅ Confirmed: Coarse location for distance calculations
- ✅ Confirmed: Line-of-sight distance (Haversine)
- Question: Continuous location polling for list updates? (Suggested: no, update on scroll/refresh)

### 6. **UX: Quick Add**
- ✅ Confirmed: Normal add button = quick add
- ✅ Confirmed: Only name + GPO count required initially
- ✅ Confirmed: Optional fields for later edit
- Question: Show location map pin for visual confirmation? (Suggested: mini map or coordinates)

### 7. **Favorites**
- ✅ Confirmed: Allow marking locations as favorites
- ✅ Confirmed: Favorite filter on list
- Question: Star icon or bookmark icon? (Suggested: star)

### 8. **Search**
- ✅ Confirmed: Search from menu, not main screen
- ✅ Confirmed: Search location name + provider name
- Question: Real-time as user types or "Search" button? (Suggested: real-time)

### 9. **Map View**
- ✅ Confirmed: Local map view desired (Phase 12)
- ✅ Confirmed: No paid APIs (OSMdroid or similar)
- Question: Priority relative to other Phase 12 features? (Suggested: medium priority)

### 10. **Export/Import**
- ✅ Confirmed: JSON format for sharing
- ✅ Confirmed: Import with merge/replace options
- ✅ Confirmed: Intent handler for .json files
- Question: Support for other formats (CSV, GPX)? (Suggested: future enhancement)

---

## Implementation Timeline (Estimated)

```
Phase 5:  Photo Capture & Storage       1-2 weeks
Phase 6:  Edit/Delete Locations         1 week
Phase 7:  Offline Support & Auto-Sync   2 weeks
Phase 8:  Duplicate Detection           1 week
Phase 9:  Search & Advanced Filtering   1 week
Phase 10: Export/Import                 1 week
Phase 11: Settings & Preferences        1 week
Phase 12: Map View (optional)            2 weeks

Total Estimated: 10-12 weeks (2.5-3 months)
```

---

## Sign-Off

### For Approval
- [ ] Product Owner: Confirm all requirements match intended use
- [ ] Technical Lead: Confirm architecture is sound
- [ ] UX/Design: Confirm UI/UX approach is acceptable

### Approved By
- Name: _______________________
- Title: _______________________
- Date: _______________________
- Comments: _______________________

---

## Document History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| v8 | 2025-11-07 | Copilot | Comprehensive plan for phases 5-12 with Q&A section |
| v7 | 2025-11-06 | - | Previous version |

---

**Ready for Review & Approval**

This plan is ready for stakeholder review. Once approved, development will proceed with Phase 5 (Photo Capture & Storage).

Questions or clarifications needed? Please update this document before approval.
