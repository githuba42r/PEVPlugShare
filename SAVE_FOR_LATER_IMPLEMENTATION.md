# Save for Later Feature Implementation Summary

## Overview
Added a "Save for Later" feature to the quick add wizard that allows users to save their current location and captured images for later completion.

## Implementation Details

### 1. Data Layer

#### New Database Entity
- **SavedForLaterEntity** (`data/db/entity/Entities.kt`)
  - Stores saved location drafts with photos
  - Fields: id, latitude, longitude, bearing, photoPaths, photoMetadata, createdAt
  - Uses comma-separated photo paths and JSON-encoded metadata

#### New DAO
- **SavedForLaterDao** (`data/db/dao/Daos.kt`)
  - CRUD operations for saved for later items
  - Count queries with Flow support for reactive UI updates

#### Repository Layer
- **SavedForLaterRepository** (interface in `domain/repository/`)
- **SavedForLaterRepositoryImpl** (`data/repository/`)
- Dependency injection configured in `di/RepositoryModule.kt` and `di/DatabaseModule.kt`

#### Database Migration
- Updated database version from 7 to 8
- Added `saved_for_later` table

### 2. UI Layer

#### Modified QuickLocationActivity
- Added "Save for Later" button to photo capture dialog
- Injects SavedForLaterRepository
- `saveForLater()` method:
  - Gets current location
  - Serializes photo metadata to JSON
  - Saves to database
  - Returns to main activity

#### New SavedForLaterActivity
- Displays list of saved locations
- Shows thumbnails, location info, coordinates, and time saved
- Two actions per item:
  - **Continue**: Opens EditLocationActivity with saved data
  - **Delete**: Confirms and deletes saved location + photos

#### New SavedForLaterViewModel
- Manages state (Loading, Success, Error)
- Converts entities to display items
- Reverse geocoding for location display names
- Formats timestamps ("2 hours ago")
- Handles deletion of saved locations and associated photo files

#### New SavedForLaterAdapter
- RecyclerView adapter for saved location list
- Nested ThumbnailAdapter for horizontal photo gallery
- Uses Glide for image loading

#### Layouts Created
- `activity_saved_for_later.xml` - Main activity layout with RecyclerView
- `item_saved_for_later.xml` - Card layout for each saved location
- `item_thumbnail.xml` - Simple ImageView for photo thumbnails

### 3. Menu Integration

#### MainActivity Updates
- Added menu observation for saved for later count
- Menu item dynamically updates title with count
- Menu item disabled when count is 0
- Title changes: "Complete Saved Location" (1) vs "Complete Saved Locations (N)"

#### Menu XML
- Added `menu_saved_for_later` item to `res/menu/main_menu.xml`

### 4. Features Implemented

✅ Save for Later option in photo dialog  
✅ Stores current location and all captured images  
✅ Menu option shows count badge  
✅ Menu option disabled when no saved locations  
✅ List view with image thumbnails  
✅ Reverse geocoding for location display names  
✅ Display suburb, state, postcode  
✅ Continue button opens add location form with saved data  
✅ Delete button with confirmation dialog  
✅ Deletes associated photo files on deletion  
✅ Passes focus areas and metadata to EditLocationActivity  

### 5. Technical Details

#### Photo Metadata Serialization
- Custom JSON serialization for PhotoMetadata map
- Preserves GPS data, bearing, and focus rectangles
- Compatible with existing PhotoMetadata structure

#### Location Services
- Reuses existing GeocodingService for reverse geocoding
- Handles sealed class result types (Success/Error)
- Falls back to "Unknown location" on geocoding failure

#### Photo Management
- Photos stored in cache directory with unique filenames
- Photo files deleted when saved location is deleted
- Validates file existence before displaying thumbnails

#### State Management
- Uses Kotlin Flow for reactive updates
- ViewModel manages loading, success, and error states
- Activity observes state changes and updates UI accordingly

## Testing Recommendations

1. **Workflow Testing**
   - Take photos with camera
   - Select "Save for Later"
   - Verify location saved in menu (count updates)
   - Open saved locations list
   - Verify thumbnails display correctly
   - Test "Continue" button - should open EditLocationActivity with data
   - Test "Delete" button - should show confirmation and delete

2. **Edge Cases**
   - Multiple saved locations
   - No saved locations (menu disabled)
   - Location permission denied (should use fallback)
   - Geocoding failure (should show "Unknown location")
   - Deleted photo files (should handle gracefully)

3. **Data Persistence**
   - Rotate device during save
   - App restart
   - Database migration from v7 to v8

## Files Modified

### New Files
- `app/src/main/java/com/a42r/pevplugshare/ui/savedforlater/SavedForLaterActivity.kt`
- `app/src/main/java/com/a42r/pevplugshare/ui/savedforlater/SavedForLaterViewModel.kt`
- `app/src/main/java/com/a42r/pevplugshare/ui/savedforlater/SavedForLaterAdapter.kt`
- `app/src/main/java/com/a42r/pevplugshare/domain/repository/SavedForLaterRepository.kt`
- `app/src/main/java/com/a42r/pevplugshare/data/repository/SavedForLaterRepositoryImpl.kt`
- `app/src/main/res/layout/activity_saved_for_later.xml`
- `app/src/main/res/layout/item_saved_for_later.xml`
- `app/src/main/res/layout/item_thumbnail.xml`

### Modified Files
- `app/src/main/java/com/a42r/pevplugshare/data/db/entity/Entities.kt`
- `app/src/main/java/com/a42r/pevplugshare/data/db/dao/Daos.kt`
- `app/src/main/java/com/a42r/pevplugshare/data/db/PEVPlugShareDatabase.kt`
- `app/src/main/java/com/a42r/pevplugshare/di/DatabaseModule.kt`
- `app/src/main/java/com/a42r/pevplugshare/di/RepositoryModule.kt`
- `app/src/main/java/com/a42r/pevplugshare/ui/quicklocation/QuickLocationActivity.kt`
- `app/src/main/java/com/a42r/pevplugshare/ui/main/MainActivity.kt`
- `app/src/main/java/com/a42r/pevplugshare/ui/main/MainViewModel.kt`
- `app/src/main/res/menu/main_menu.xml`
- `app/src/main/AndroidManifest.xml`
