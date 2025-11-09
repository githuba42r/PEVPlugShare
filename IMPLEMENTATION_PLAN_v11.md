# PEV PlugShare - Implementation Plan v11

## Project Overview
Android application to assist PEV (Personal Electric Vehicle) riders in managing and discovering charging locations integrated with OSMAnd "My Places".

### Target Platform
- **Android Version**: Android 16 (API Level 35)
- **Primary Device**: Mobile phones
- **Architecture**: MVVM with Repository pattern
- **Language**: Kotlin
- **Build System**: Gradle with Kotlin DSL

---

## Phase 1: Project Setup & Core Infrastructure

### 1.1 Project Initialization
- [x] Create new Android Studio project
  - Target Android 16 (API 35)
  - Minimum SDK: Android 8.0 (API 26)
  - Package: `com.pevplugshare`
  - Language: Kotlin
- [x] Configure Gradle dependencies
  - AndroidX libraries (Core, AppCompat, Material)
  - Jetpack Compose for modern UI
  - Room for local database
  - Retrofit for API communication
  - Kotlin Coroutines & Flow
  - Hilt for dependency injection
  - WorkManager for background sync
  - Location services
  - CameraX for photos
  - ExifInterface for photo metadata

### 1.2 Project Structure
```
app/
├── data/
│   ├── local/
│   │   ├── dao/
│   │   ├── entities/
│   │   └── database/
│   ├── remote/
│   │   ├── api/
│   │   └── dto/
│   └── repository/
├── domain/
│   ├── model/
│   └── usecase/
├── presentation/
│   ├── screens/
│   ├── components/
│   └── viewmodels/
└── utils/
```

### 1.3 Permissions Setup
- **Fine Location**: For adding new locations (GPS coordinates)
- **Coarse Location**: For calculating distances to locations in list
- **Camera**: For taking photos of charging locations
- **Storage**: For saving photos locally
- **Internet**: For OSMAnd API sync
- **Foreground Service**: For background sync notifications

---

## Phase 2: Data Layer Implementation

### 2.1 Local Database (Room)
**ChargingLocation Entity**
```kotlin
@Entity(tableName = "charging_locations")
data class ChargingLocation(
    @PrimaryKey val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val description: String?,
    
    // Socket/Charger Information
    val socketTypes: List<SocketType>, // AS3112, Type2, etc.
    val gpoCount: Int, // Number of AS3112 sockets
    
    // Location Metadata
    val isPublic: Boolean, // Public vs Private access
    val hasFee: Boolean, // Free vs Paid
    val providerName: String?, // For paid locations
    val providerWebsite: String?, // For paid locations
    val approximateCost: String?, // Charging costs
    val additionalInfo: String?, // Other relevant info
    
    // Opening Hours
    val openingHours: String?, // e.g., "9-5", "24x7", "weekdays"
    val isOpen247: Boolean,
    
    // UI State
    val isFavorite: Boolean,
    val color: String?, // From OSMAnd category
    val icon: String?, // From OSMAnd category
    
    // Sync metadata
    val osmandId: String?,
    val categoryId: String,
    val lastSyncTime: Long,
    val pendingUpload: Boolean, // For offline mode
    
    // Photos
    val photoUris: List<String>, // Local file paths
    
    // Timestamps
    val createdAt: Long,
    val updatedAt: Long
)

enum class SocketType {
    AS3112_GPO,  // Australian General Purpose Outlet (primary)
    TYPE2_EV,    // Type 2 EV charger
    CCS,         // Combined Charging System
    CHADEMO,     // CHAdeMO
    OTHER
}
```

**AppSettings Entity**
```kotlin
@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey val id: Int = 1,
    val selectedCategoryId: String?,
    val resyncFrequency: ResyncFrequency,
    val lastSyncTime: Long?,
    val osmandAccountId: String?,
    val osmandAccessToken: String?,
    val osmandRefreshToken: String?
)

enum class ResyncFrequency(val hours: Int) {
    HOURS_4(4), HOURS_8(8), HOURS_12(12),
    DAYS_1(24), DAYS_2(48), DAYS_5(120), 
    DAYS_14(336), DAYS_28(672)
}
```

### 2.2 Remote API (OSMAnd Integration)
- OSMAnd OAuth2 authentication (Authorization Grant Flow)
- API endpoints:
  - `/oauth/authorize` - OAuth authorization
  - `/oauth/token` - Token exchange
  - `/api/myplaces/categories` - List categories
  - `/api/myplaces/locations` - Get/Post/Put/Delete locations
  - `/api/myplaces/sync` - Sync operations

### 2.3 Repository Layer
- **ChargingLocationRepository**: CRUD operations, sync logic
- **AuthRepository**: OAuth flow, token management
- **PhotoRepository**: Local photo storage, metadata handling

---

## Phase 3: Authentication & Account Management

### 3.1 OAuth2 Implementation
- Implement Authorization Grant Flow with OSMAnd
- Secure token storage (EncryptedSharedPreferences)
- Automatic token refresh
- Account selection (if multiple OSMAnd accounts)

### 3.2 Category Selection
- First-run experience: prompt for category selection
- Display list of available "My Places" categories
- Remember selected category in settings
- Allow category change in settings

---

## Phase 4: Main Location List Screen

### 4.1 Location List UI
**Features:**
- RecyclerView/LazyColumn with location cards
- Sort by distance (using coarse location)
- Distance calculation: Line-of-sight (haversine formula)
- Highlight open locations (based on current time)
- Show: Name, Distance, Socket count (GPO), Open/Closed status

**Filter Section:**
- Opening hours: All / Currently Open
- Minimum capacity (GPO count)
- Favorite locations toggle
- Socket type filter

### 4.2 Location Card Actions
- Tap: View details / Edit
- Navigation button: Start OSMAnd navigation
  - Use Android Intent to launch OSMAnd
  - Pass coordinates for navigation target
- Favorite toggle (star icon)
- Long press: Quick actions menu

### 4.3 Distance Calculation
```kotlin
// Haversine formula for line-of-sight distance
fun calculateDistance(
    lat1: Double, lon1: Double,
    lat2: Double, lon2: Double
): Double
```

---

## Phase 5: Add/Edit Location Screen

### 5.1 Quick Add Form (Primary Flow)
**Required Fields:**
- Location Name (text input)
- Number of GPO/Sockets (number picker)

**Auto-captured:**
- GPS coordinates (fine location)
- Timestamp

**Optional Fields (Expandable):**
- Description
- Photos (camera/gallery)
- Opening hours (quick select options)
- Socket types (multi-select)
- Public/Private toggle
- Free/Paid toggle
- Provider info (if paid)
- Additional information

### 5.2 Opening Hours Selection
Pre-defined options:
- 24/7
- Business hours (9-5)
- Weekdays only
- Weekends only
- Custom (time picker)

### 5.3 Photo Capture
- CameraX integration
- Local storage in app-specific directory
- EXIF metadata embedding:
  - GPS coordinates
  - App name ("PEV PlugShare")
  - Location name
  - Description
  - Timestamp
- Multiple photos per location
- Photo preview and deletion

### 5.4 Duplicate Detection
- Check for locations within 250m radius
- Show warning with existing location details
- Allow override with confirmation
- Use fine location for comparison

### 5.5 Offline Support
- Allow location creation while offline
- Mark as `pendingUpload = true`
- Show offline indicator in top bar
- Auto-sync when connection restored

---

## Phase 6: Location Details & Edit Screen

### 6.1 View Mode
- Display all location information
- Photo gallery (swipe/scroll)
- Map preview (static, no external API)
- Opening hours display
- Socket information
- Distance from current location
- Last updated timestamp

### 6.2 Edit Mode
- Pre-populate all fields
- Add additional photos
- Delete existing photos
- Update any field
- Save changes locally
- Sync with OSMAnd

### 6.3 Delete Location
- Confirmation dialog
- Soft delete (mark for deletion)
- Sync deletion with OSMAnd
- Remove from local database

---

## Phase 7: Navigation Integration

### 7.1 OSMAnd Intent
```kotlin
fun startOsmandNavigation(lat: Double, lon: Double, name: String) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        data = Uri.parse("osmand.navigation:q=$lat,$lon($name)")
        setPackage("net.osmand.plus")
    }
    if (intent.resolveActivity(packageManager) != null) {
        startActivity(intent)
    } else {
        // Show "OSMAnd not installed" message
    }
}
```

---

## Phase 8: Sync & Background Operations

### 8.1 Automatic Sync (WorkManager)
- Periodic sync based on user preference
- Upload pending locations (offline mode)
- Download updates from OSMAnd
- Handle conflicts (server wins)
- Delete removed locations

### 8.2 Sync Notifications
- Silent notification during sync
- Show in notification drawer
- Progress indicator
- Error notifications

### 8.3 Manual Resync
- Menu option to force sync
- Pull-to-refresh on location list
- Show sync status

---

## Phase 9: Settings & Menu

### 9.1 Top Bar Menu (Hamburger)
- Settings
- Search
- About
- Logout
- Resync

### 9.2 Settings Screen
- Selected Category (changeable)
- Auto-sync frequency selector
- Offline mode toggle
- Default socket type
- Units preference (km/mi)
- Theme selection (Light/Dark/System)

### 9.3 Search Functionality
- Search by location name
- Search by description
- Filter by socket type
- Distance radius filter
- Accessible from menu (not main screen)

### 9.4 About Screen
**Version Information:**
- Version number
- Build date/time
- App description

**Account Information:**
- Connected OSMAnd account
- Last sync time
- Offline status indicator
- Number of locations

**Features List:**
- Brief feature overview
- Links to documentation/support

### 9.5 Logout
- Clear auth tokens
- Optionally clear local data
- Return to login screen

---

## Phase 10: Favorites & Filtering

### 10.1 Favorite Management
- Toggle favorite on location card
- Star icon indicator
- Filter: Show favorites only
- Persist to local database
- Sync favorite status to OSMAnd (if supported)

### 10.2 Advanced Filtering
- Combine multiple filters (AND logic)
- Save filter presets (optional)
- Clear all filters button

---

## Phase 11: Map View (Local)

### 11.1 Offline Map Display
- Use OSMDroid or similar offline map library
- Display charging locations as markers
- No paid API services (Google Maps, etc.)
- Download free OSM tiles
- Cache tiles locally

### 11.2 Map Features
- Cluster markers when zoomed out
- Show current location
- Tap marker for location details
- Navigate to location from map

---

## Phase 12: Export & Import (Sharing)

### 12.1 Export Locations
**JSON Format:**
```json
{
  "version": "1.0",
  "exported_at": "2024-01-15T10:30:00Z",
  "locations": [
    {
      "name": "...",
      "latitude": 0.0,
      "longitude": 0.0,
      // ... all location fields
    }
  ]
}
```

### 12.2 Share Functionality
- Export selected/all locations to JSON
- Share via Android Share Sheet
- Generate shareable file

### 12.3 Import Functionality
- Register file type handler (.pevshare or .json)
- Intent filter for shared files
- Import flow:
  1. Parse JSON file
  2. Show location list preview
  3. Merge options per location:
     - Skip (keep existing)
     - Replace (update existing)
     - Add as new (duplicate)
  4. Import selected locations
  5. Sync to OSMAnd

---

## Phase 13: UI/UX Polish

### 13.1 Material Design 3
- Modern Material You theming
- Dynamic colors (Android 12+)
- Adaptive layouts (phones/tablets)
- Dark mode support

### 13.2 Animations
- Smooth transitions
- Loading states
- Error states
- Empty states

### 13.3 Accessibility
- Content descriptions
- TalkBack support
- Large text support
- High contrast mode

### 13.4 Offline Indicator
- Persistent indicator in top bar
- Status on About screen
- Visual feedback for pending uploads

---

## Phase 14: Testing & Validation

### 14.1 Unit Tests
- Repository layer
- Use cases
- ViewModels
- Distance calculations
- Duplicate detection

### 14.2 Integration Tests
- Database operations
- API calls (mocked)
- Sync logic

### 14.3 UI Tests
- Critical user flows
- Add location
- Edit location
- Sync operations

### 14.4 Manual Testing
- OAuth flow
- Offline mode
- Photo capture & metadata
- OSMAnd navigation
- Import/Export

---

## Phase 15: Documentation & Deployment

### 15.1 User Documentation
- Quick start guide
- Feature documentation
- FAQ
- Troubleshooting

### 15.2 Developer Documentation
- Code architecture
- API documentation
- Build instructions
- Contribution guidelines

### 15.3 Release Preparation
- App icon design
- Screenshots
- Store listing
- Privacy policy
- Terms of service

### 15.4 Deployment
- Generate signed APK/AAB
- Version management
- Release notes
- Distribution (Play Store, F-Droid, or direct APK)

---

## Technical Considerations

### Security
- Secure token storage (EncryptedSharedPreferences)
- HTTPS only for API calls
- Input validation
- SQL injection prevention (Room handles this)

### Performance
- Lazy loading for large location lists
- Image compression for photos
- Efficient database queries
- Background thread for sync operations

### Storage
- Estimate storage requirements
- Photo size limits
- Database size management
- Cache cleanup

### Battery
- Efficient location updates
- WorkManager constraints (charging, WiFi)
- Doze mode compatibility

---

## Dependencies Summary

```kotlin
// Core Android
implementation("androidx.core:core-ktx:1.12.0")
implementation("androidx.appcompat:appcompat:1.6.1")
implementation("com.google.android.material:material:1.11.0")

// Compose
implementation("androidx.compose.ui:ui:1.5.4")
implementation("androidx.compose.material3:material3:1.1.2")
implementation("androidx.activity:activity-compose:1.8.2")

// Navigation
implementation("androidx.navigation:navigation-compose:2.7.6")

// Room
implementation("androidx.room:room-runtime:2.6.1")
implementation("androidx.room:room-ktx:2.6.1")
kapt("androidx.room:room-compiler:2.6.1")

// Retrofit
implementation("com.squareup.retrofit2:retrofit:2.9.0")
implementation("com.squareup.retrofit2:converter-gson:2.9.0")

// Coroutines
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

// Hilt
implementation("com.google.dagger:hilt-android:2.50")
kapt("com.google.dagger:hilt-compiler:2.50")

// Location
implementation("com.google.android.gms:play-services-location:21.0.1")

// CameraX
implementation("androidx.camera:camera-camera2:1.3.1")
implementation("androidx.camera:camera-lifecycle:1.3.1")
implementation("androidx.camera:camera-view:1.3.1")

// ExifInterface
implementation("androidx.exifinterface:exifinterface:1.3.7")

// WorkManager
implementation("androidx.work:work-runtime-ktx:2.9.0")

// OSMDroid (offline maps)
implementation("org.osmdroid:osmdroid-android:6.1.17")

// Security
implementation("androidx.security:security-crypto:1.1.0-alpha06")

// Testing
testImplementation("junit:junit:4.13.2")
androidTestImplementation("androidx.test.ext:junit:1.1.5")
androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
```

---

## Timeline Estimate

- **Phase 1-2**: 1 week (Setup & Data Layer)
- **Phase 3**: 3 days (Authentication)
- **Phase 4**: 1 week (Main List)
- **Phase 5**: 1 week (Add Location)
- **Phase 6**: 3 days (Details/Edit)
- **Phase 7**: 2 days (Navigation)
- **Phase 8**: 1 week (Sync)
- **Phase 9**: 3 days (Settings/Menu)
- **Phase 10**: 2 days (Favorites)
- **Phase 11**: 1 week (Map View)
- **Phase 12**: 4 days (Import/Export)
- **Phase 13**: 1 week (Polish)
- **Phase 14**: 1 week (Testing)
- **Phase 15**: 3 days (Documentation)

**Total Estimated Time**: 8-10 weeks

---

## Open Questions for Review

### OSMAnd API Clarifications
1. ✅ **CONFIRMED**: OSMAnd API documentation: https://osmand.net/docs/technical/osmand-api-sdk/
2. ✅ **CONFIRMED**: Photo storage: Local only, upload to OSMAnd if possible
3. ✅ **CONFIRMED**: Photo metadata: Include GPX data, app name, location details

### Feature Confirmations
4. ✅ **CONFIRMED**: Offline support with indicator and queued uploads
5. ✅ **CONFIRMED**: Socket types: AS3112 primary, Type2 EV and others
6. ✅ **CONFIRMED**: Public/Private flag
7. ✅ **CONFIRMED**: Fee flag and provider information
8. ✅ **CONFIRMED**: Location permissions: Fine for add, Coarse for distance
9. ✅ **CONFIRMED**: Line-of-sight distance calculation
10. ✅ **CONFIRMED**: Quick add with minimal required fields
11. ✅ **CONFIRMED**: Favorites functionality
12. ✅ **CONFIRMED**: Search from menu
13. ✅ **CONFIRMED**: Local map view (no paid APIs)
14. ✅ **CONFIRMED**: Export/Import with merge options

### Additional Considerations
15. Should the app support multiple OSMAnd accounts on the same device?
    - **Recommendation**: Start with single account, add multi-account in future version

16. Photo size/quality settings?
    - **Recommendation**: Medium quality (1920x1080), compressed to <1MB per photo

17. Maximum photos per location?
    - **Recommendation**: 10 photos per location

18. Should favorites sync to OSMAnd or remain local?
    - **Recommendation**: Local only (OSMAnd may not support favorite flag)

19. Default color/icon for new locations if category doesn't have defaults?
    - **Recommendation**: Use app-defined defaults (blue marker icon)

20. Conflict resolution strategy for sync (local vs server)?
    - **Recommendation**: Server wins (last-write-wins), with option to keep both

---

## Success Criteria

1. ✅ User can authenticate with OSMAnd account
2. ✅ User can view list of charging locations sorted by distance
3. ✅ User can quickly add a new location with minimal input (name + GPO count)
4. ✅ User can add photos with proper EXIF metadata
5. ✅ User can navigate to locations via OSMAnd
6. ✅ App works offline and syncs when online
7. ✅ User can filter and search locations
8. ✅ User can mark favorites
9. ✅ User can export and share locations
10. ✅ App respects battery and data usage
11. ✅ UI is intuitive and follows Material Design guidelines

---

## Risk Assessment

### High Risk
- OSMAnd API availability and reliability
- OAuth implementation complexity
- Photo upload to OSMAnd (may not be supported)

### Medium Risk
- Offline map performance with many locations
- Background sync battery impact
- Sync conflict resolution

### Low Risk
- UI implementation
- Local database operations
- Distance calculations

---

## Future Enhancements (Out of Scope v1)

- Cloud photo storage (Mapillary, Dropbox, OneDrive, Google Drive)
- Route planning with multiple charging stops
- Community features (ratings, comments)
- Statistics and analytics
- Widget for quick add
- Android Auto integration
- Multi-account support
- Batch operations
- Advanced map features (routing overlay, heatmaps)

---

**Review Status**: Ready for review and approval
**Next Steps**: Address any questions, refine plan, begin Phase 1 implementation

