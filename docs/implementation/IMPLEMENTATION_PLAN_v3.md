# PEVPlugShare - Implementation Plan v3

## Project Overview
A quick and easy Android app for adding EV charging locations to OSMAnd's "My Places" category. The app prioritizes rapid data entry with minimal friction, supporting offline operations and comprehensive charging location metadata.

**Target Platform:** Android 16+  
**Primary Language:** Kotlin  
**API Reference:** [OSMAnd Technical API Documentation](https://osmand.net/docs/technical/osmand-api-sdk/)

---

## Phase 1: Core Architecture & Authentication

### 1.1 Authentication System
- **OAuth2 Integration**: Implement authorization grant flow for OSMAnd account
  - Secure token storage using Android KeyStore
  - Token refresh mechanism
  - Session management and logout functionality

### 1.2 Data Model Design
```
Location Entity:
- id (String - UUID)
- name (String, required)
- description (String, optional)
- latitude (Double, required)
- longitude (Double, required)
- socketTypes (List<SocketType>)
  - AS3112 (default, Australia)
  - Type2 EV
  - Other EV types (extensible)
- numberOfSockets (Int, required)
- publicPrivate (Enum: PUBLIC, PRIVATE, required)
- hasFee (Boolean, required)
- openingHours (String, optional)
- provider (String, optional - for paid locations)
- providerWebsite (String, optional - URL)
- estimatedChargingCosts (String, optional)
- additionalInfo (String, optional)
- photoUris (List<String>)
- photoMetadata (List<PhotoMetadata>)
- isFavourite (Boolean)
- createdAt (LocalDateTime)
- updatedAt (LocalDateTime)
- syncedWithOSMAnd (Boolean)
- lastSyncedAt (LocalDateTime, nullable)
- isOfflineRecord (Boolean)
```

### 1.3 Local Database
- **Room Database** for local persistence
  - LocationEntity table
  - PhotoMetadataEntity table
  - Sync status tracking
- Migration strategy for schema updates

### 1.4 Network & Sync Module
- OSMAnd API client using Retrofit
- Periodic sync scheduler (background service)
- Offline queue for new locations created while offline
- Conflict resolution strategy (last-write-wins or user choice)

---

## Phase 2: Location Management

### 2.1 Location Listing Screen
**UI Components:**
- Top bar with offline indicator and hamburger menu
- Filter section (expandable)
  - Open now / All times
  - Minimum socket count
  - Public / Private filter
  - Favorites only toggle
- Sortable list by distance (line-of-sight)
  - Display: Name, Distance, Socket count, Open status
  - Color-coded by open/closed status
- Bottom FAB for adding new location

**Location Permissions:**
- **Fine Location**: Used during location addition (high precision)
- **Coarse Location**: Used for main list distance calculations
- Runtime permission requests with clear explanations
- Graceful degradation if permissions denied

### 2.2 Location List Ordering
- Distance calculations: Haversine formula (line-of-sight, not navigation)
- Real-time distance updates as user moves
- Location caching with update frequency

### 2.3 Location Navigation
- Integration with OSMAnd for navigation
- "Navigate To" button per location
- OSMAnd intent with coordinates and location name

---

## Phase 3: Add Location Workflow

### 3.1 Quick Add Form (MVP - Required Fields Only)
**Screen 1: Quick Entry**
- Location Name (text input, required)
- Socket Count (number picker, required)
- Auto-populated GPS coordinates (fine location)
- "Add Now" button (creates location)
- "Add With Details" button (proceeds to Screen 2)

**Screen 2: Enhanced Details (Optional)**
- Socket Types selection (checkboxes: AS3112, Type2, others)
- Public/Private flag (radio buttons)
- Has Fee flag (toggle)
- Opening Hours (time picker or text)
- Description (text area)
- Provider Name (text input, if paid)
- Provider Website (URL input, if paid)
- Charging Costs (text input, if paid)
- Additional Info (text area)
- Photo Capture/Upload

### 3.2 Duplicate Detection
- Search for existing locations within 250m radius
- Display warning if found, allow user to confirm override
- Calculate using Haversine formula with coarse location if fine location unavailable during sync

### 3.3 Photo Capture & Metadata
**Photo Handling:**
- Camera integration for on-site capture
- Photo gallery access for selecting existing images
- Local storage in app's private directory
- Photo metadata preservation:
  - GPS coordinates from EXIF or location capture time
  - App name and version in EXIF
  - Location name and description in EXIF comment field
  - Socket types, opening hours in EXIF

### 3.4 Offline Support
- Create location record with isOfflineRecord = true
- Store in local database with sync status = PENDING
- Auto-sync when connectivity restored
- Show sync status indicator to user

### 3.5 Sync with OSMAnd
- Post location to OSMAnd My Places API
- Include color and icon from selected category
- Update syncedWithOSMAnd = true and lastSyncedAt
- Handle sync errors gracefully with retry mechanism

---

## Phase 4: Edit & Delete Operations

### 4.1 Edit Existing Location
- Open location detail view
- Edit any field (all fields should be editable post-creation)
- Validate changes
- Sync update to OSMAnd
- Track last update timestamp

### 4.2 Delete Location
- Confirmation dialog with location name
- Delete locally
- Delete from OSMAnd (via API)
- Handle offline case (mark for deletion, sync when online)

### 4.3 Favourites Management
- Heart icon in location row (easy toggling)
- Visual indication of favorited locations
- Filter option in list ("Show Favorites Only")
- Persisted locally and synced to OSMAnd metadata

---

## Phase 5: Main Screen Features

### 5.1 Category Selection
- On first launch: prompt to select "My Places" category
- On settings change: refresh list from new category
- Graceful handling if no categories exist
- Option to create new category (future enhancement)

### 5.2 List Filtering
- Collapsible filter panel at top
- **Open Status Filter**: "Open Now" / "All" radio buttons
  - Parse opening hours (9-5, 24x7, weekdays only, etc)
  - Compare with current device time and timezone
  - Visual indicator (green = open, gray = closed)
- **Socket Count Filter**: Minimum number (slider or picker)
- **Type Filter**: Public / Private toggle
- **Favorites Filter**: Checkbox for favorites only
- Filter persistence (remember user's last selection)

### 5.3 Offline Indicator
- Top bar status:
  - Online: Minimal indicator or no indicator
  - Offline: Prominent "OFFLINE" badge or icon
- About screen: Show "Currently Offline" or "Last Online: [time]"
- Auto-detect via connectivity checks

---

## Phase 6: Navigation & Menu

### 6.1 Top Bar
- App title/logo (left)
- Offline status indicator (center/right)
- Hamburger menu button (right)

### 6.2 Hamburger Menu
- **Settings**: Opens settings screen
- **Search**: Opens search screen (accessible but not prominent)
- **Export**: Opens export/share screen
- **About**: Opens about screen
- **Resync Now**: Trigger immediate background sync
- **Logout**: Clears auth token and returns to login

### 6.3 Settings Screen
- **Category Selection**: Dropdown of available My Places categories
- **Auto-Sync Frequency**: 
  - Default: 1 day
  - Options: 4/8/12 hours, 1/2/5/14/28 days
  - Implementation: WorkManager scheduled job
- **Location Permission**: Show current status and allow changes
- **Clear Local Cache**: Option to clear downloaded data (manual refresh)

---

## Phase 7: Search Functionality

### 7.1 Search Screen
- Accessible from menu (not main screen)
- Search by location name (case-insensitive)
- Filter results by the same filters as main list
- Display results in list format with distances

---

## Phase 8: Export & Sharing

### 8.1 Export Format
- JSON file containing locations array
- Schema:
  ```json
  {
    "version": "1.0",
    "appName": "PEVPlugShare",
    "exportedAt": "ISO8601 timestamp",
    "locations": [
      {
        "name": "...",
        "latitude": "...",
        "longitude": "...",
        "socketTypes": ["AS3112", "Type2"],
        "numberOfSockets": 2,
        "publicPrivate": "PUBLIC",
        "hasFee": false,
        "openingHours": "24/7",
        "provider": null,
        "providerWebsite": null,
        "estimatedChargingCosts": null,
        "additionalInfo": null,
        "createdAt": "ISO8601 timestamp"
      }
    ]
  }
  ```

### 8.2 Share Functionality
- Generate JSON file
- Share via intent (email, messaging, etc)
- File saved to device Downloads or app-specific directory

### 8.3 Import Functionality
- Intent handler for .json files
- Import dialog with options:
  - **Merge**: Keep local versions if duplicate (within 250m)
  - **Replace**: Overwrite local duplicates
  - **Skip Duplicates**: Import only new locations
- Confirmation showing count of imported locations
- Auto-sync to OSMAnd after import

---

## Phase 9: About Screen

### 9.1 Content Sections
- **Version Information**
  - App version number
  - Build date and time
  - Feature list summary
  
- **Account Information**
  - OSMAnd account email/username
  - Connected status
  - Last sync time (with timezone)
  - Current sync status
  
- **Offline Status**
  - Current connectivity state
  - Data pending sync count
  
- **Statistics** (Optional enhancement)
  - Total locations
  - Locations with photos
  - Last modification date

---

## Phase 10: Background Services

### 10.1 Sync Service
- **Implementation**: Android WorkManager
- **Trigger Events**:
  - Scheduled interval (user-configurable)
  - Device boot completion
  - Connectivity change (online after offline)
  - Manual trigger from menu
  
- **Sync Operations**:
  - Download latest locations from OSMAnd
  - Upload pending local changes
  - Handle conflicts (last-write-wins)
  - Clear completed sync queue
  
- **Notification**:
  - Silent notification in drawer during sync
  - Notification removal on completion

### 10.2 Connectivity Monitoring
- BroadcastReceiver for connectivity changes
- Update offline indicator in real-time
- Attempt sync when connectivity restored

---

## Phase 11: Data Security

### 11.1 Authentication Token Storage
- Android KeyStore for secure token storage
- Encrypted SharedPreferences for sensitive data
- No hardcoded credentials

### 11.2 Local Database Encryption
- Room database encryption (optional, depends on data sensitivity)
- User location data is non-sensitive but good practice

### 11.3 Network Security
- HTTPS only for OSMAnd API calls
- Certificate pinning (optional, depends on OSMAnd API stability)

---

## Phase 12: Testing Strategy

### 12.1 Unit Tests
- Distance calculation (Haversine formula)
- Opening hours parsing and comparison
- JSON import/export validation
- Data model validation

### 12.2 Integration Tests
- OAuth2 flow (with mock server)
- Database CRUD operations
- Sync service behavior
- Offline queue handling

### 12.3 UI Tests
- Espresso tests for main workflows:
  - Add location flow
  - Filter list
  - Edit/delete operations
  - Export/import

### 12.4 Manual Testing
- Real device with OSMAnd
- Offline scenarios (flight mode)
- Network switching (WiFi to mobile)
- Permission denial scenarios

---

## Technical Stack

| Component | Technology |
|-----------|------------|
| Language | Kotlin |
| Min API | 16 |
| UI Framework | Jetpack Compose (or XML layouts) |
| Database | Room |
| Networking | Retrofit + OkHttp |
| Async/Background | WorkManager, Coroutines |
| Image Handling | Coil or Glide |
| Location | Google Play Services Location |
| JSON | Gson or Kotlinx Serialization |
| DI | Hilt or Manual |
| Auth | Retrofit + Custom TokenManager |

---

## Implementation Timeline (Estimated)

| Phase | Tasks | Duration |
|-------|-------|----------|
| 1 | Architecture, Auth, Data Model | 2 weeks |
| 2 | Location List, Permissions, Navigation | 2 weeks |
| 3 | Add Location, Photos, Sync | 2.5 weeks |
| 4 | Edit, Delete, Favorites | 1 week |
| 5 | Filters, Offline Indicator | 1.5 weeks |
| 6 | Menu, Settings, Navigation | 1 week |
| 7 | Search, Export, Import | 1.5 weeks |
| 8 | About Screen, Background Services | 1 week |
| 9 | Security, Testing | 2 weeks |
| 10 | Bug Fixes, Polish, Documentation | 1.5 weeks |

**Total Estimate: 16-17 weeks**

---

## Known Limitations & Future Enhancements

### Current Version Limitations
- Photos stored locally only (no cloud upload in v1)
- No map view (uses line-of-sight distance only)
- No batch operations
- No advanced charger type filtering (extensible structure, basic UI)

### Future Enhancements
- Cloud photo storage (Mapillary, Dropbox, OneDrive, Google Drive integration)
- Interactive map view using OSMAnd or local tiles
- Advanced charger network integration
- Photo gallery view within app
- User ratings and reviews
- Charger availability status (real-time via third-party APIs)
- Batch operations for category migration
- Location suggestions based on popular charging networks

---

## Dependencies & External APIs

### Required
- **OSMAnd API**: Location upload/download
- **Google Play Services**: Location services
- **Android Framework APIs**: Camera, Gallery, Connectivity

### Optional/Future
- **Mapillary API**: Photo hosting
- **Map Tile Providers**: For map view feature

---

## Success Criteria

1. ✓ User can add new charging location in < 30 seconds
2. ✓ Location list loads and updates in real-time
3. ✓ Offline add/edit/delete works with automatic sync
4. ✓ Filters work correctly and persist across sessions
5. ✓ Photos include location metadata
6. ✓ Export/import functionality preserves data integrity
7. ✓ Background sync doesn't drain battery significantly
8. ✓ App handles poor connectivity gracefully
9. ✓ All permissions requested at appropriate times
10. ✓ Unit and UI test coverage > 70%

---

## Review Checklist

- [ ] Does this plan address all requirements from the Initial Design Brief?
- [ ] Are the technical choices appropriate for the constraints?
- [ ] Is the timeline realistic?
- [ ] Are there any missing features or edge cases?
- [ ] Any concerns with the data model or sync strategy?
- [ ] Should any phases be reordered or combined?
- [ ] Are there privacy/security concerns?
- [ ] Any questions about OSMAnd API capabilities?
