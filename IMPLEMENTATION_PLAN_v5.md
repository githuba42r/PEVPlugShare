# PEV Plug Share - Implementation Plan v5.0

**Status:** Ready for Review  
**Date:** November 7, 2025  
**Target:** Android 16+

---

## Executive Summary

PEV Plug Share is an Android application that simplifies managing EV charging locations through integration with OSMAnd's "My Places" feature. Users can quickly add, view, edit, and share charging location data with offline support and optional photo storage with GPS metadata.

---

## Core Requirements

### 1. Authentication & Account Management
- **OAuth2 Integration**: Implement authorization grant flow for OSMAnd account authentication
- **Token Management**: Secure storage and refresh of OAuth2 tokens
- **Session Handling**: Automatic logout after token expiration
- **Account Verification**: Display connected OSMAnd account in settings/about screen

### 2. Main Location List Screen
- **Display**: List of locations from selected "My Places" category
- **Ordering**: By distance from current location (line-of-sight calculation)
- **Location Info**: Name, distance, capacity (number of GPO/sockets)
- **Visual Indicators**:
  - Highlight locations open at current time
  - Show socket type availability (AS3112, Type2, other EV charger types)
  - Star icon for favorited locations
  - Public/Private indicator badge
  - Fee indicator badge
- **Location Actions**: Set as navigation target in OSMAnd
- **Filter Controls** (top collapsible section):
  - Open locations only / All locations
  - Minimum capacity filter
  - Favorites only toggle
  - Search function (menu-accessible)
- **Permissions**:
  - Fine location: When adding locations
  - Coarse location: For distance calculations on main list
  - Camera: For taking photos
  - Photos: For selecting photos from gallery
- **Offline Indicator**: Battery icon or badge in topbar showing offline status
- **Add Button**: Primary action button (bottom-right FAB or menu)

### 3. Location Details View
**Data Structure per Location:**
```
{
  id: string,
  name: string (required),
  latitude: number,
  longitude: number,
  socketTypes: {
    AS3112: number (required, count),
    Type2: number,
    otherTypes: []
  },
  isPublic: boolean,
  hasFee: boolean,
  feeInfo: {
    providerName: string,
    website: string,
    approximateCost: string
  },
  description: string,
  openingHours: string,
  photos: [], // local file paths + optional GPX/metadata
  additionalInfo: string,
  isFavorite: boolean,
  lastModified: timestamp,
  createdDate: timestamp,
  syncStatus: "synced" | "pending" | "error"
}
```

### 4. Add/Edit Location Flow
**Initial Screen (Quick Add):**
- Location Name (text input, required)
- AS3112 GPO Count (number input, required)
- Current GPS coordinates (auto-captured with fine location)
- Duplicate detection (250m radius check)
- Confirmation dialog if duplicate found

**Optional Details Screen (accessible after initial add or during edit):**
- Socket types (Type2, other EV charger types - counters)
- Public/Private flag toggle
- Fee flag toggle
- Provider name (conditional on fee flag)
- Provider website URL (conditional on fee flag)
- Approximate cost (conditional on fee flag)
- Opening hours (predefined options: 24/7, 9-5, weekdays only, custom)
- Description/notes
- Additional information section
- Photo capture/upload (local storage)
- Map preview (optional)

**Photo Features:**
- Capture from camera or select from gallery
- Auto-embed GPS coordinates from capture location
- Embed location name, description, app name in metadata (EXIF)
- Local storage with reference in location record
- Optional: Future upload to OSMAnd (if API supports)

**Validation:**
- Required fields: Name, AS3112 count, valid GPS coordinates
- Duplicate check within 250m radius
- User confirmation before add if duplicate detected

### 5. Edit/Delete Locations
- **Edit Access**: Users can only edit locations they created (or implement full edit capability)
- **Edit Flow**: Open location details → Edit button → Modify fields → Save
- **Delete Flow**: Open location details → Delete button → Confirmation dialog → Delete
- **Sync Status**: Mark edited/deleted items for sync when online

### 6. Favorites System
- **Toggle Favorite**: Star icon on location list and detail view
- **Filter**: "Favorites only" toggle in filter section
- **Persistence**: Store favorite flag locally and sync with server

### 7. Resync & Background Operations
- **Auto-Sync**: 
  - Frequency options: 4/8/12 hours, 1/2/5/14/28 days (default 1 day)
  - Manual resync via menu option
  - Silent notification during sync
- **Sync Logic**:
  - Download updated locations from OSMAnd My Places
  - Handle deletions on server
  - Merge local pending changes
  - Handle conflicts (local edits vs server updates)
- **Offline Mode**:
  - Store new locations locally when offline
  - Queue for upload when online
  - Periodic connectivity check
  - Auto-sync when connectivity restored
- **Notifications**: Silent status notification in drawer during sync

### 8. Search & Filter
- **Location**: Accessible from menu (not main screen)
- **Functionality**: 
  - Search by location name
  - Filter by capacity, open status, public/private, has fee
  - Display results in same list format
- **Implementation**: Local in-memory search on downloaded data

### 9. Map View Integration
- **OSMAnd Intent Integration**: 
  - Use `net.osmand.SHOW_ON_MAP` intent to open OSMAnd
  - Set navigation target
  - No additional map API dependencies required
- **Location Viewing**: Display location details and open in OSMAnd map
- **Future**: Custom map view using OSMAnd SDK (not in v1)

### 10. Export & Sharing
- **Export Format**: JSON-encoded file with location data
- **Export Contents**: Selected location(s) or all locations
- **Share Methods**: 
  - Email
  - File sharing
  - Other apps supporting file intent
- **Import Flow**:
  - User receives shared JSON file
  - App intercepts via intent filter
  - Display merge/replace dialog for each location
  - Options:
    - Replace existing (if ID matches)
    - Skip (keep existing)
    - Add as new (with new ID)
  - Batch import and confirmation

### 11. Topbar & Navigation
- **Topbar Elements**:
  - App logo/title
  - Offline indicator (battery-like icon)
  - Hamburger menu icon
- **Hamburger Menu Items**:
  - Settings
  - Search
  - About
  - Logout
  - Resync (manual trigger)

### 12. Settings Screen
- **Options**:
  - My Places Category selection (dropdown)
  - Auto-sync frequency selector
  - App storage info
  - Cache clear option
  - Notification preferences
  - Photo storage location (local only in v1)

### 13. About Screen
- **Content**:
  - App version number
  - Build date/time
  - App description
  - Features list
  - Account information (connected OSMAnd account)
  - Last sync timestamp
  - Current offline status indicator
  - App usage statistics (optional)

### 14. Architecture & Technical Requirements

**Tech Stack:**
- Language: Kotlin
- Min API: Android 16 (API level 16)
- Architecture: MVVM with Repository pattern
- Database: Room for local storage
- Networking: Retrofit + OkHttp for API calls
- Authentication: OAuth2 with secure token storage (Android Keystore)
- Background Tasks: WorkManager for scheduled sync
- UI Framework: Jetpack Compose or AndroidX

**Data Storage:**
- **SQLite (Room)**:
  - Locations table
  - Photos metadata table
  - Sync queue table
  - User preferences
- **File System**:
  - Photo images (app-private directory: /data/data/package/files/photos/)
  - Cache for metadata
- **Secure Storage (Keystore)**:
  - OAuth2 tokens
  - User preferences (encrypted)

**Key Dependencies:**
- androidx.lifecycle:lifecycle-* (MVVM)
- androidx.room:room-* (Database)
- com.squareup.retrofit2:* (Networking)
- com.google.android.gms:play-services-location (Location)
- work:work-runtime-ktx (Background sync)
- androidx.security:security-crypto (Encryption)

**API Integration:**
- OSMAnd API: https://osmand.net/docs/technical/osmand-api-sdk/
- OAuth2 endpoints for authentication
- My Places category management
- Location CRUD operations

---

## Implementation Phases

### Phase 1: Project Setup & Authentication
**Duration:** 2-3 days
- [ ] Project initialization with Gradle configuration
- [ ] Dependency setup (Retrofit, Room, WorkManager, etc.)
- [ ] OAuth2 authentication flow implementation
- [ ] Secure token storage
- [ ] Login screen UI
- [ ] Token refresh mechanism

### Phase 2: Core Data Models & Database
**Duration:** 2 days
- [ ] Location data model
- [ ] Photo metadata model
- [ ] Sync queue model
- [ ] Room database setup with migrations
- [ ] Repository pattern implementation
- [ ] DAO layer for CRUD operations

### Phase 3: OSMAnd API Integration
**Duration:** 2 days
- [ ] Retrofit service setup for OSMAnd API
- [ ] Fetch My Places categories
- [ ] Fetch locations from selected category
- [ ] Create/update/delete location API calls
- [ ] Error handling and retry logic

### Phase 4: Main Location List UI
**Duration:** 2-3 days
- [ ] RecyclerView adapter for locations
- [ ] Distance calculation logic
- [ ] Location filtering and sorting
- [ ] Opening hours logic
- [ ] Filter controls UI
- [ ] Location item detail view

### Phase 5: Add Location Flow
**Duration:** 2-3 days
- [ ] Quick add screen (minimal fields)
- [ ] GPS location capture
- [ ] Duplicate detection algorithm
- [ ] Enhanced details screen for optional fields
- [ ] Form validation
- [ ] Local save before sync

### Phase 6: Photo Handling
**Duration:** 2 days
- [ ] Camera integration
- [ ] Gallery picker
- [ ] Photo storage in app-private directory
- [ ] EXIF metadata embedding (GPS, location name)
- [ ] Photo preview in add/edit flow
- [ ] Local photo reference management

### Phase 7: Favorites & Search
**Duration:** 1-2 days
- [ ] Favorite flag toggle UI
- [ ] Favorite filter implementation
- [ ] Search screen
- [ ] Local search algorithm
- [ ] Search result display

### Phase 8: Background Sync & Offline Support
**Duration:** 2-3 days
- [ ] WorkManager scheduled sync setup
- [ ] Sync frequency configuration
- [ ] Offline queue management
- [ ] Automatic retry on connectivity restore
- [ ] Conflict resolution strategy
- [ ] Sync status notification

### Phase 9: Edit/Delete & Intent Integration
**Duration:** 1-2 days
- [ ] Edit location flow
- [ ] Delete location confirmation
- [ ] OSMAnd navigation intent
- [ ] Intent filter for shared files
- [ ] Import merge/replace logic

### Phase 10: Export & Sharing
**Duration:** 1-2 days
- [ ] JSON export functionality
- [ ] File sharing intent
- [ ] Import from shared files
- [ ] Merge/replace UI dialog
- [ ] Batch import handling

### Phase 11: Navigation & Settings
**Duration:** 1-2 days
- [ ] Topbar with offline indicator
- [ ] Hamburger menu implementation
- [ ] Settings screen
- [ ] About screen
- [ ] Category selection in settings
- [ ] Sync frequency configuration

### Phase 12: Testing & Polish
**Duration:** 2-3 days
- [ ] Unit tests (business logic)
- [ ] Integration tests (API/database)
- [ ] UI testing
- [ ] Edge case handling
- [ ] Performance optimization
- [ ] Bug fixes and refinement

---

## UX Considerations

### User Flows

**Flow 1: First Launch**
1. App detects no account configured
2. Display login/OAuth screen
3. User authenticates with OSMAnd
4. App fetches My Places categories
5. User selects PEV charging category
6. App syncs locations and displays list
7. Main screen shows locations sorted by distance

**Flow 2: Quick Add Location**
1. User taps FAB or add button
2. Quick add screen appears (Name + AS3112 count only)
3. GPS location auto-populated
4. User enters name and count, taps Add
5. Duplicate check performed
6. If no duplicate, location saved locally and queued for sync
7. Confirmation message shown
8. User can tap "Add Details" to add optional info immediately

**Flow 3: Edit Location (Later)**
1. User opens location from list
2. Taps edit button
3. Enhanced edit screen shows all fields
4. User adds photos, description, opening hours, etc.
5. Taps save
6. Changes marked for sync
7. Synced when online

**Flow 4: Offline Scenario**
1. User is offline (no connectivity)
2. Offline indicator visible in topbar
3. User can still add/edit locations
4. New locations stored locally
5. When connectivity restored, auto-sync triggered
6. Offline indicator disappears after successful sync

**Flow 5: Export & Share**
1. User opens location
2. Taps share/export button
3. Selects destinations (email, file share, etc.)
4. JSON file generated and shared
5. Recipient receives file
6. Opens with app or selects app to handle intent
7. App shows import dialog
8. Recipient chooses merge/replace/skip for each location
9. Locations imported with merge applied

---

## API Contract

### OSMAnd API Endpoints Required
- **Authentication**: OAuth2 endpoint
- **Categories**: GET /categories (fetch user's My Places categories)
- **Locations**: 
  - GET /locations?category_id={id} (fetch all)
  - POST /locations (create new)
  - PUT /locations/{id} (update)
  - DELETE /locations/{id} (delete)
- **Duplicate Check**: Local distance calculation (no API needed)

### Data Exchange Format
- **Request/Response**: JSON
- **Photo Upload**: Multipart form (if supported by OSMAnd API, otherwise local only)
- **Location Export**: JSON file format (custom)

---

## Security Considerations

1. **OAuth2 Token Storage**: Android Keystore System
2. **User Location Data**: End-to-end with OSMAnd server only
3. **EXIF Data**: Embedded in photos locally; consider privacy implications
4. **File Permissions**: Minimal permissions (location, camera, photos, files)
5. **Network Security**: Certificate pinning (recommended)
6. **Data Backup**: Exclude sensitive data from Android backup

---

## Testing Strategy

### Unit Tests
- Location model serialization/deserialization
- Distance calculation algorithm
- Duplicate detection logic
- Date/time formatting (opening hours)
- Search filtering logic

### Integration Tests
- OAuth2 flow end-to-end
- OSMAnd API communication
- Database CRUD operations
- Sync operation with conflict resolution
- Offline queue management

### UI Tests
- Add location flow (quick add + details)
- Edit location flow
- Filter and search functionality
- Favorites toggle
- Import dialog

### Manual Testing
- Offline/online transitions
- Sync at various network speeds
- Multiple users with same category
- Large photo handling
- Concurrent add/edit operations

---

## Performance Targets

- App launch: < 2 seconds (on-device data)
- Location list load: < 1 second (< 500 locations)
- Add location: < 500ms
- Background sync: < 5 minutes for 500 locations
- Photo handling: < 100MB per photo (compressed on save)
- Memory footprint: < 100MB

---

## Accessibility & Localization

### Accessibility (Initial)
- Large touch targets (min 48dp)
- High contrast text
- Content descriptions for images
- Screen reader support (TalkBack)

### Localization (Phase 2 - Future)
- English initial support
- String resources externalized
- RTL language support ready

---

## Future Enhancements (Not in v1)

1. **Photo Upload Services**: Integration with Mapillary, Dropbox, OneDrive, Google Drive
2. **Advanced Map View**: Embedded map with custom markers (using OSMAnd SDK)
3. **User Ratings/Reviews**: Community feedback on locations
4. **Analytics**: Usage statistics and popular routes
5. **Cloud Backup**: Automatic backup of locations and photos
6. **Offline Maps**: Full offline map viewing
7. **Batch Operations**: Multi-location actions
8. **Custom Notifications**: Alerts for chargers along planned routes

---

## Risk Mitigation

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|-----------|
| OSMAnd API unavailable | Low | High | Implement fallback to local-only mode |
| OAuth2 token expiration | Medium | Low | Implement automatic refresh, user re-login flow |
| Large photo handling | Medium | Medium | Implement compression, storage limits |
| Sync conflicts | Medium | Medium | Implement merge strategy, user resolution dialog |
| GPS accuracy in urban areas | High | Low | Display accuracy indicator, allow manual adjustment |
| Network latency | High | Low | Implement timeouts, retry with exponential backoff |

---

## Success Criteria

✅ App successfully authenticates with OSMAnd account  
✅ User can add location with name + count in < 10 seconds  
✅ Location list displays with accurate distances  
✅ Offline add/edit functions and syncs when online  
✅ Photos with GPS metadata stored locally  
✅ Export/import locations with merge functionality  
✅ Background sync runs on schedule without user intervention  
✅ App works on Android 16+  
✅ Navigation intent successfully opens location in OSMAnd  
✅ Search and filter functions operational  

---

## Questions for Stakeholder Review

1. **Duplicate Detection**: Should users be allowed to add duplicates if they confirm, or should it be prevented entirely? (Current plan: Allow with confirmation)

2. **Photo Compression**: Should app automatically compress photos to a maximum size, or upload full resolution? (Recommended: Auto-compress to 2MB max for local storage)

3. **Sync Conflicts**: When local changes conflict with server updates, should the app:
   - Keep local version (optimistic)?
   - Keep server version?
   - Create two entries (branching)?
   (Current plan: User-selectable, default to keep local)

4. **Location Verification**: Should locations require verification by community/admin before appearing in public view? (Not planned for v1)

5. **User Ratings**: Should locations have ratings/reviews in future? (Deferred to v2)

6. **Public/Private Distinction**: How should private locations (owned by businesses) be handled? Can any user edit, or only the owner? (Deferred pending OSMAnd API capabilities)

7. **Socket Type Icons**: Should the app display specific icons for different socket types (AS3112, Type2, etc.)? (Recommended: Yes, for UX clarity)

8. **Opening Hours Presets**: Are the suggested options (24/7, 9-5, weekdays) sufficient, or should more custom options be provided? (Recommended: Add more templates for common patterns)

---

## Document Version History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| v5.0 | Nov 7, 2025 | Team | Initial comprehensive plan for review |

---

**Status:** READY FOR REVIEW  
**Next Step:** Stakeholder approval → Phase 1 commencement

