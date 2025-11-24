# PEVPlugShare - Implementation Plan for Review

## Document Purpose
This document consolidates the Initial Design Brief requirements with clarification responses and presents a comprehensive implementation roadmap for the PEVPlugShare Android application. **This version is ready for review prior to commencing implementation work.**

---

## Project Overview

**App Name:** PEVPlugShare  
**Purpose:** Quick and easy management of PEV charging locations integrated with OSMAnd "My Places" and OAuth2 authentication.  
**Target:** Android 16 (API 34+) on mobile phones  
**Primary Goal:** Rapid location addition with minimal data entry (Location Name + GPO count only required)

---

## Requirements Summary

### Core Features (from Initial Design Brief + Feedback)

#### 1. Authentication & Account Management
- OAuth2 authentication via OSMAnd API (Authorization Code Flow)
- Secure token storage using EncryptedSharedPreferences
- Token refresh mechanism with automatic re-authentication
- Session management with logout capability
- Category selection from user's "My Places" collections

#### 2. Main Screen - Location List
```
┌─────────────────────────────────────────────────────┐
│ [☰ Menu] [📡 Status] Category Name                  │
├─────────────────────────────────────────────────────┤
│ Filters: [Status▼] [Capacity▼] [★ Favorites]       │
├─────────────────────────────────────────────────────┤
│ Location List (sorted by distance)                  │
│ ────────────────────────────────────────────────────│
│ Name                          5.2 km ⊙ 4 GPO        │
│ 🟢 Open | Public | [Navigation ➜]                 │
│ ────────────────────────────────────────────────────│
│ Name                          7.8 km ⊙ 6 GPO        │
│ 🔴 Closed | Private | 💰 | [Navigation ➜]         │
│                                                     │
│ [+ Add New Location]                               │
└─────────────────────────────────────────────────────┘
```

**List Features:**
- Ordered by distance from current location (line-of-sight Haversine, NOT navigation distance)
- Shows: Name, Distance, Capacity (GPO count), Status (Open/Closed)
- Indicators: Public/Private badge, Fee indicator (💰), Favorite star (★)
- Filter section: Status (Open/All), Capacity (min GPO), Favorites toggle
- Navigation button to trigger OSMAnd navigation to location
- Offline indicator in top bar showing connection status

#### 3. Quick Add Location (Primary UX)

**Required Fields:**
- GPS location capture (current location, Fine Location permission - ~5m accuracy)
- Location name (text input)
- Number of GPO (power points/sockets)

**Optional Fields (on same form, collapsible):**
- Socket types: AS3112 (primary/default), Type2 EV, Other EV charger types (multi-select)
- Public/Private flag (radio button)
- Description (text)
- Opening hours (dropdown: 9-5, 24x7, Weekdays only, Custom, etc.)
- Fee flag (Yes/No toggle)
- Photo capture/upload (multiple photos via camera)

**Additional Info Section (optional, collapsible):**
- *For paid locations:*
  - Provider name
  - Provider website link
  - Approximate charging costs
- *General:*
  - Other relevant information (text field)

**Duplicate Detection:**
- Check 250m radius before submission (line-of-sight calculation)
- Display warning with nearby locations found
- User can proceed or cancel

**Process:**
1. User taps "Add New Location" FAB
2. App captures current GPS location with Fine Location permission
3. User enters Name and GPO count (required) - takes ~30 seconds
4. Optional fields available but not required
5. Optional: Add photos (EXIF metadata embedded with location data, app name, GPX data)
6. Submit → duplicate check → confirm/add to queue if offline → upload when online

#### 4. Edit Location Screen
- Pre-populated with existing location data
- Modify all fields (name, description, hours, GPO count, socket types, fees, etc.)
- Add additional photos
- Delete location with confirmation dialog
- Save/Cancel actions

#### 5. Location Data Model & Classification

**Socket Types:**
- AS3112 (AU GPO) - PRIMARY default option
- Type2 EV - European standard
- Other EV Charger Types - Extensible for future additions

**Location Flags:**
- **Public/Private:** Public = public area access (park, shared charging center, etc.) | Private = private business owned/operated
- **Fee Flag:** Boolean indicator if location has charging fees
- **Status:** Open/Closed (based on opening hours)
- **Favorite:** User-marked favorite locations

**Additional Info for Paid Locations:**
- Provider name
- Provider website link
- Approximate charging costs
- Other relevant information

#### 6. Photo Management
- **Storage:** Local only (Phase 1), future support for Mapillary, Dropbox, OneDrive, Google Drive
- **EXIF Metadata:** Embed GPS coordinates, location name, description, app name ("PEVPlugShare"), app version, capture timestamp
- **GPX Data:** Associate GPX waypoint format data with photos
- **Offline Queue:** Queue photos for upload when connection restored
- **Limits:** ~10 photos per location, compress to ~2MB per photo (max ~5MB uncompressed)

#### 7. Offline Support Architecture
- **Network Detection:** Monitor connectivity changes in real-time
- **Offline Indicator:** Show status in top bar ("🔴 Offline" or "🟢 Online" or "🔄 Syncing")
- **Offline Queue:** Store locations, edits, and photos added while offline
- **Automatic Sync:** Upload queued items when device comes back online
- **Retry Logic:** Exponential backoff with max 3 retries on failure
- **Pending UI:** Show queued operations in About screen (count of pending locations)
- **Sync Status Tracking:** SYNCED, PENDING, FAILED statuses on each location

#### 8. Location Permissions & Resolution
- **Fine Location (~5m accuracy):** Requested when user initiates "Add Location"
- **Coarse Location:** Requested on app startup for main list (can use cached result, less accurate)
- **Distance Calculation:** Line-of-sight using Haversine formula, NOT navigation distance

#### 9. Favorites System
- Star icon on each location item (clickable to toggle)
- Quick filter: "Favorites" toggle in filter section
- Favorite status persisted locally
- Sync to OSMAnd if API supports, otherwise local-only

#### 10. Search Function
- Accessible via menu (NOT on main screen)
- Search by location name (case-insensitive substring match)
- Display filtered results in location list view
- Lower priority feature (unlikely to be used frequently)

#### 11. Navigation Integration
- Detect OSMAnd app presence on device
- Launch OSMAnd with target location via Intent
- Pass location coordinates and name
- Graceful fallback if OSMAnd not installed (show error message)

#### 12. Export & Sharing
- **Export to JSON:** Generate shareable JSON file with:
  - Location array (all fields)
  - App metadata (version, export timestamp)
  - Pretty-printed format
- **Import from JSON:** 
  - Accept via Intent or file picker
  - Present merge/replace options for each location conflict:
    - Skip (keep existing)
    - Replace (overwrite with imported)
    - Merge (combine metadata)
  - Validate JSON structure before import
  - Handle conflicts gracefully

#### 13. Menu Structure
- **Settings:** Configure app preferences (category selection, resync frequency, cache management)
- **About:** Version info, account info, sync status, offline status, queued locations count, app features
- **Search:** Location search by name
- **Manual Resync:** Force sync operation immediately
- **Logout:** Disconnect OSMAnd account with confirmation

#### 14. Settings Screen
- Select "My Places" category (display current selection)
- Configure resync frequency:
  - Options: 4h, 8h, 12h, 1 day (default), 2 days, 5 days, 14 days, 28 days
  - Scheduled background resync via WorkManager
- Clear app cache option
- Version information
- Visual explanations for each frequency option

#### 15. About Screen
- **Version Information:**
  - Version number
  - Build date/time
  - Online/Offline status indicator
  - Basic usage information
- **Account Information:**
  - OSMAnd account connected (email/username)
  - Last sync timestamp
  - Queued locations count (if offline)
- **App Features List**
- **App Description**

#### 16. Background Sync
- **Periodic Sync:** WorkManager for automatic resync per configured frequency (default: 1 day)
- **Silent Notifications:** Display only in notification drawer (non-intrusive):
  - "🔄 Syncing locations..." (during sync)
  - "✓ Locations synced" (on completion)
  - "⚠️ Sync failed - will retry" (on failure)
  - "📡 Connection restored - uploading pending locations" (on network reconnection)
- **Conflict Resolution:** Server data takes precedence for already-synced locations
- **Retry Strategy:** Exponential backoff with max 3 retries

#### 17. Map View
- **Phase 1:** NOT included (user preference for no paid APIs)
- **Future Enhancement:** Optional map view using open-source libraries (Osmdroid/Leaflet) with offline OSM tiles
- **Device-based:** All calculations local to device, no server dependency

#### 18. Batch Operations
- **NOT included** in Phase 1 or planned as future enhancement (per user feedback)

---

## Technical Architecture

### Technology Stack

| Component | Technology | Rationale |
|-----------|-----------|-----------|
| Language | Kotlin | Modern Android standard, null-safety, concise syntax |
| UI Framework | Jetpack Compose or XML | Declarative/procedural, maintainable UI |
| Architecture | MVVM + Repository | Clean separation of concerns, testability |
| Local Database | Room | Type-safe, integrated with LiveData, migrations |
| Networking | Retrofit2 | Robust HTTP client, interceptors, easy testing |
| Background Tasks | WorkManager | Respects battery constraints, built-in retry |
| Location Services | Google Play Services Fused Provider | Accurate, battery-efficient, multi-source |
| Dependency Injection | Hilt | Simplified DI with annotation processing |
| Photo Metadata | androidx.exifinterface:exifinterface | EXIF manipulation, standardized |
| Encryption | EncryptedSharedPreferences | Secure token storage |
| Testing | JUnit, Espresso, Mockito | Standard Android testing frameworks |

### Project Structure

```
PEVPlugShare/
├── app/
│   ├── src/main/kotlin/com/example/pevplugshare/
│   │   ├── ui/
│   │   │   ├── auth/
│   │   │   │   ├── LoginActivity.kt
│   │   │   │   └── CategorySelectionActivity.kt
│   │   │   ├── list/
│   │   │   │   ├── LocationListActivity.kt
│   │   │   │   ├── LocationListViewModel.kt
│   │   │   │   ├── LocationListAdapter.kt
│   │   │   │   └── activity_location_list.xml
│   │   │   ├── add/
│   │   │   │   ├── AddLocationActivity.kt
│   │   │   │   ├── AddLocationViewModel.kt
│   │   │   │   └── activity_add_location.xml
│   │   │   ├── edit/
│   │   │   │   ├── EditLocationActivity.kt
│   │   │   │   ├── EditLocationViewModel.kt
│   │   │   │   └── activity_edit_location.xml
│   │   │   ├── menu/
│   │   │   │   ├── SettingsActivity.kt
│   │   │   │   ├── AboutActivity.kt
│   │   │   │   └── SearchActivity.kt
│   │   │   └── common/
│   │   │       ├── OfflineIndicator.kt
│   │   │       └── FilterPanel.kt
│   │   ├── data/
│   │   │   ├── api/
│   │   │   │   ├── OsmandApiService.kt
│   │   │   │   └── OsmandApiClient.kt
│   │   │   ├── db/
│   │   │   │   ├── dao/
│   │   │   │   │   ├── LocationDao.kt
│   │   │   │   │   ├── PhotoDao.kt
│   │   │   │   │   └── PendingOperationDao.kt
│   │   │   │   ├── entity/
│   │   │   │   │   ├── LocationEntity.kt
│   │   │   │   │   ├── PhotoEntity.kt
│   │   │   │   │   └── PendingOperationEntity.kt
│   │   │   │   ├── AppDatabase.kt
│   │   │   │   └── migrations/
│   │   │   ├── repository/
│   │   │   │   ├── LocationRepository.kt
│   │   │   │   ├── SyncManager.kt
│   │   │   │   ├── PendingOperationRepository.kt
│   │   │   │   └── ConnectivityObserver.kt
│   │   │   └── model/
│   │   │       ├── Location.kt
│   │   │       ├── Photo.kt
│   │   │       ├── SocketType.kt
│   │   │       └── LocationAccess.kt
│   │   ├── worker/
│   │   │   ├── AutoSyncWorker.kt
│   │   │   └── AutoSyncScheduler.kt
│   │   ├── util/
│   │   │   ├── LocationCalculator.kt (Haversine formula)
│   │   │   ├── PhotoProcessor.kt (EXIF manipulation)
│   │   │   ├── GpxGenerator.kt
│   │   │   ├── JsonExporter.kt
│   │   │   ├── JsonImporter.kt
│   │   │   └── Constants.kt
│   │   └── PEVPlugShareApp.kt (Hilt Application)
│   ├── src/main/AndroidManifest.xml
│   ├── build.gradle.kts
│   └── src/test/ (unit & instrumentation tests)
├── build.gradle.kts
└── settings.gradle.kts
```

### Database Schema

**tables:**

1. **locations**
   - id (String, PK) - OSMAnd API ID
   - name (String) - required
   - description (String) - optional
   - latitude (Double)
   - longitude (Double)
   - socketTypes (String - JSON array) - AS3112, Type2, Other, etc.
   - publicPrivate (String - enum) - PUBLIC, PRIVATE
   - hasFee (Boolean)
   - gpoCount (Int) - required
   - openingHours (String) - optional
   - additionalInfo (String) - optional
   - providerName (String) - optional (paid locations)
   - providerWebsite (String) - optional (paid locations)
   - chargingCosts (String) - optional (paid locations)
   - color (String) - from category
   - icon (String) - from category
   - lastModified (Long)
   - isFavourite (Boolean) - local flag
   - syncStatus (String - enum) - SYNCED, PENDING, FAILED, SYNCING
   - createdAt (Long)
   - updatedAt (Long)

2. **location_photos**
   - id (String, PK)
   - locationId (String, FK)
   - filePath (String)
   - timestamp (Long)
   - exifMetadata (String - JSON)
   - gpxData (String - GPX format)

3. **pending_operations**
   - id (String, PK)
   - locationId (String, FK)
   - operationType (String - enum) - ADD, UPDATE, DELETE
   - timestamp (Long)
   - retryCount (Int)
   - lastRetryTime (Long)
   - errorMessage (String)

4. **user_preferences**
   - selectedCategoryId (String)
   - resyncFrequencyHours (Int) - default 24
   - lastSyncTime (Long)
   - offlineQueueCount (Int)

---

## Implementation Phases

### Phase 1: Core Architecture & Authentication (Week 1-1.5)
- [ ] Project setup with Hilt DI, Room database, Retrofit2
- [ ] OAuth2 implementation (Authorization Code Flow via OSMAnd API)
- [ ] Secure token storage with EncryptedSharedPreferences
- [ ] Category selection screen
- [ ] Basic app structure and navigation

**Deliverables:** Authentication flow, token management, category selection

### Phase 2: Location List & Basic UI (Week 2-3)
- [ ] Main location list screen with RecyclerView adapter
- [ ] Filter panel (Status, Capacity, Favorites)
- [ ] Distance sorting using Haversine formula
- [ ] Location item layout with indicators (status, public/private, fee, favorite)
- [ ] Offline indicator in top bar
- [ ] Menu structure (Settings, About, Search, Resync, Logout)

**Deliverables:** Location list view, filtering, menus, offline indicator

### Phase 3: Location Management - Add & Edit (Week 3-4)
- [ ] Quick Add Location activity with required fields (Name, GPO count, location capture)
- [ ] Optional fields (socket types, public/private, description, hours, photos)
- [ ] Photo capture integration with EXIF metadata embedding
- [ ] GPX waypoint generation and association
- [ ] Duplicate detection (250m radius check)
- [ ] Edit Location activity with pre-populated fields
- [ ] Delete Location with confirmation

**Deliverables:** Add/Edit/Delete location workflow, photo capture with metadata

### Phase 4: GPS & Location Services (Week 4-4.5)
- [ ] Fine Location permission request and handling
- [ ] Coarse Location for list view
- [ ] Fused Location Provider integration
- [ ] Haversine distance calculation (line-of-sight)
- [ ] Location update mechanisms and caching

**Deliverables:** Location services, distance calculations, permission handling

### Phase 5: Offline Support & Sync Queue (Week 5-6)
- [ ] Network connectivity monitoring (ConnectivityObserver)
- [ ] Pending operations queue (PendingOperationEntity & Repository)
- [ ] SyncManager for conflict resolution
- [ ] AutoSyncWorker for background sync
- [ ] Queue management UI (About screen)
- [ ] Retry logic with exponential backoff

**Deliverables:** Offline queue system, background sync, retry mechanism

### Phase 6: Additional Features (Week 6-7)
- [ ] Favorites system (toggle + filter)
- [ ] Search function (menu-accessible)
- [ ] Navigation integration with OSMAnd (Intent launch)
- [ ] Export to JSON functionality
- [ ] Import from JSON with merge/replace options

**Deliverables:** Favorites, search, navigation, export/import

### Phase 7: Settings & About Screens (Week 7-7.5)
- [ ] Settings activity (category selection, resync frequency, cache clearing)
- [ ] Resync frequency scheduling with WorkManager
- [ ] About screen (version, account, sync status, features)
- [ ] Silent notifications for sync operations
- [ ] Logout confirmation

**Deliverables:** Settings configuration, about information, sync scheduling

### Phase 8: Testing & Polish (Week 8-9)
- [ ] Unit tests (ViewModels, Repositories, Calculators)
- [ ] Instrumentation tests (UI, navigation, permissions)
- [ ] Offline mode testing (add locations offline, sync, retry)
- [ ] Photo handling and EXIF verification
- [ ] Edge case handling (no GPS, no network, token expiration)
- [ ] UI/UX polish and animation refinements

**Deliverables:** Comprehensive test suite, bug fixes, polished UI

---

## Addressing Initial Design Brief Feedback

### ✅ OSMAnd API Integration
- Using official OSMAnd API documentation: https://osmand.net/docs/technical/osmand-api-sdk/
- OAuth2 Authorization Code Flow implementation
- Integration points: Authentication, list categories, fetch/create/update/delete locations

### ✅ Photo Storage & Metadata
- **Phase 1:** Local-only storage in app-specific cache directory
- **EXIF Embedding:** GPS coordinates, location name, description, app name, version, timestamp
- **GPX Data:** Waypoint format associated with photos
- **Future:** Architecture ready for Mapillary, Dropbox, OneDrive, Google Drive integration

### ✅ Offline Support
- Network connectivity monitoring with real-time detection
- Offline indicator in top bar and About screen ("🔴 Offline", "🟢 Online", "🔄 Syncing")
- Offline queue for locations, edits, and photos
- Automatic sync when device comes back online
- Queued operations visible in About screen (pending count)
- Silent notifications for sync progress

### ✅ Socket Types & Location Flags
- **Primary:** AS3112 (AU GPO) with multi-select for Type2 EV and other types
- **Public/Private Flag:** PUBLIC (public area access) | PRIVATE (private business)
- **Fee Flag:** Boolean indicator for charging fees
- **Additional Info Section:** Provider name, website, charging costs, other info

### ✅ Location Permissions & Distance
- **Fine Location:** ~5m accuracy when adding location (Android 6+ runtime permission)
- **Coarse Location:** Less accurate for main list distance calculations
- **Distance Calculation:** Line-of-sight Haversine formula (NOT navigation distance)

### ✅ UX Improvements
- **Quick Add Focus:** Only Location Name + GPO count required; all other fields optional
- **Favorites System:** Star toggle on each item + Favorites filter in filter panel
- **Search Function:** Menu-accessible (not main screen), lower priority
- **Map View:** Deferred to Phase 2 (future enhancement with open-source libraries, no paid APIs)
- **Batch Operations:** NOT included (per user feedback, not expected as future feature)
- **Export/Import:** JSON format with merge/replace conflict resolution

---

## Key Design Decisions

### 1. No Paid APIs
All functionality is local to device or uses free services (OSMAnd API is free tier). Future map view will use Osmdroid or Leaflet with open-source OSM tiles.

### 2. Local-First Photo Storage
Phase 1 stores photos locally with EXIF metadata embedded. Future phases can integrate external storage services without breaking current architecture.

### 3. Silent Sync Notifications
Background sync operations display notifications only in notification drawer (non-intrusive), not as toast/pop-ups during active use.

### 4. Line-of-Sight Distance
Haversine formula provides great-circle distance, suitable for general proximity. Not using Google Maps API or other navigation services for distance calculations.

### 5. Simple Quick Add
Primary UX focuses on speed: Location Name + GPO count only. All other fields are optional and can be edited later during ride breaks or after completion.

### 6. Conflict Resolution: Server Wins
During sync, server data takes precedence for already-synced locations, preventing data loss from stale local updates.

---

## Testing Strategy

### Unit Tests
- LocationCalculator (Haversine distance calculations)
- PhotoProcessor (EXIF manipulation)
- GpxGenerator (GPX format generation)
- JsonExporter/JsonImporter (export/import logic)
- SyncManager (conflict resolution, retry logic)
- ViewModels (state management, data transformation)

### Integration Tests
- LocationRepository (database operations)
- PendingOperationRepository (queue management)
- AutoSyncWorker (background sync execution)

### Instrumentation Tests
- Navigation flows (login → category selection → main list)
- Add Location flow (permission request → photo capture → submission)
- Edit/Delete flows
- Filter and search functionality
- Offline queue behavior (add offline, sync online)

### Manual Testing
- Offline mode simulation (disable network)
- OSMAnd app integration (launch navigation)
- Photo EXIF metadata verification
- Permission handling edge cases

---

## Risk Mitigation

| Risk | Impact | Mitigation |
|------|--------|-----------|
| OSMAnd API changes | High | Monitor API changelog, version endpoints, fallback to cached data |
| Token expiration | Medium | Implement automatic token refresh, re-auth on 401 errors |
| Large photo files | Medium | Compress to ~2MB per photo, limit to 10 per location |
| Sync conflicts | Medium | Server-wins strategy, log conflicts for debugging |
| GPS unavailability | Low | Manual location entry fallback, cached last-known location |
| Network unreliability | Medium | Exponential backoff retry, persistent queue, user feedback |
| Database corruption | Low | Room migrations, backup on sync completion, error logging |

---

## Performance Considerations

### Battery Optimization
- Fused Location Provider uses GPS, WiFi, and cellular intelligently
- WorkManager respects device battery state
- Sync operations bundled, not frequent small requests
- Photo compression before storage

### Memory Management
- RecyclerView with efficient adapter (view pooling)
- Bitmap caching for photos
- LiveData observers properly lifecycle-aware
- Database queries with Room pagination for large lists

### Network Efficiency
- Batch API calls where possible
- Gzip compression for API responses
- Exponential backoff for failed retries
- Sync only modified data on incremental updates

---

## Future Enhancements (Phase 2+)

1. **Map View:** Open-source map library (Osmdroid) with offline OSM tiles
2. **Photo Upload Services:** Integration with Mapillary, Dropbox, OneDrive, Google Drive
3. **Advanced Filtering:** By socket type, fee status, provider, etc.
4. **Batch Operations:** Bulk edit/delete/export (user feedback: not needed in Phase 1)
5. **Photo Gallery:** View location photos in-app, full-screen viewer
6. **Location Sharing:** Share individual locations via social media
7. **Statistics:** Usage stats, most visited locations, charging history
8. **Route Optimization:** Suggest charging stops based on navigation route (requires route data)
9. **Cost Calculator:** Estimate charging costs based on battery capacity and location fees
10. **Charging Network Integration:** Link to real-time availability from charging networks

---

## Success Criteria

✅ **Functional:**
- User can authenticate via OAuth2
- Add location with 2 required fields in <2 minutes
- View nearby locations sorted by distance
- Edit/delete locations
- Sync with OSMAnd "My Places"
- Work offline with automatic sync when online
- Export and import locations via JSON

✅ **UX:**
- Offline indicator clearly visible
- Filters work responsively
- Navigation to OSMAnd works
- Photo capture with EXIF metadata
- No paid APIs used

✅ **Technical:**
- MVVM architecture with separation of concerns
- Room database with proper migrations
- WorkManager for background sync
- Comprehensive error handling
- Test coverage >70%

---

## Approval & Next Steps

### For Review:
1. ✅ Project scope and feature set
2. ✅ Technical architecture and technology choices
3. ✅ Database schema and data model
4. ✅ UI/UX flows and layout
5. ✅ Implementation timeline (estimated 8-9 weeks)
6. ✅ Risk mitigation strategies

### Approval Sign-Off:
- [ ] Client Review: Requirements verified?
- [ ] Technical Review: Architecture approved?
- [ ] Timeline Review: Delivery date acceptable?

### Upon Approval:
1. Finalize project setup and build configuration
2. Initialize Git repository with branch strategy
3. Set up CI/CD pipeline (optional: Android CI workflows)
4. Create detailed task breakdown for each phase
5. Begin Phase 1 implementation (Core Architecture & Authentication)

---

## References & Resources

- **OSMAnd API:** https://osmand.net/docs/technical/osmand-api-sdk/
- **Android Location Services:** https://developer.android.com/develop/sensors-and-location/location
- **Room Database:** https://developer.android.com/training/data-storage/room
- **WorkManager:** https://developer.android.com/develop/background-work/background-tasks/persistent-work
- **Retrofit2:** https://square.github.io/retrofit/
- **Hilt DI:** https://developer.android.com/training/dependency-injection/hilt-android
- **ExifInterface:** https://developer.android.com/reference/androidx/exifinterface/media/ExifInterface
- **Jetpack Compose:** https://developer.android.com/develop/ui/compose

---

## Document History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2025-11-07 | Implementation Team | Initial comprehensive plan incorporating Initial Design Brief + feedback responses |

---

**This document is ready for review and approval before commencing implementation.**

