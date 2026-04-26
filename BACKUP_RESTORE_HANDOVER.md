# Backup/Restore Feature - Handover Document

**Date:** April 26, 2026  
**Feature:** Debug-only backup/restore functionality for Android app testing  
**Status:** ✅ Complete and tested

---

## 1. Executive Summary

A complete backup/restore system has been implemented for the PEVPlugShare Android app. This feature allows users to:
- Create ZIP backups of their local database and images
- Restore from backups with automatic image relocation
- Recover location details through coordinate-based lookups
- Migrate between database schema versions automatically

**Current Capability:**
- Restore 35 locations with all metadata
- Restore 7 saved locations with 49 associated images
- Automatically migrate database from v11 → v12
- Lookup and restore location names from coordinates

---

## 2. Feature Overview

### What It Does
The backup/restore system captures the app's entire local state (database + images) and reconstructs it on restoration. Key challenges solved:

1. **Image Path Mismatch**: Backup images have timestamps in filenames (e.g., `QUICK_LOC_20260424_134426_3153270749532375703.jpg`) but database expects generic paths (e.g., `quick_location_1768986427202_404.jpg`). **Solution**: `completeSavedLocations()` maps actual restored files to database entries sequentially.

2. **Location Names Missing**: Saved locations store only coordinates; names must be looked up. **Solution**: Query locations table by lat/lng proximity (±0.001°) to find matching location names.

3. **Database Schema Evolution**: Old backups may be v11; app is v12. **Solution**: Room handles automatic migration + fallback to destructive migration if needed.

4. **Threading Issues**: Restore runs on IO thread but LiveData requires main thread. **Solution**: Use `postValue()` instead of `setValue()` for thread-safe updates.

### User Flow
1. User opens app menu → selects "[DEBUG] Restore Backup"
2. File picker opens (Android's DocumentsUI)
3. User selects backup ZIP file from storage
4. Restore dialog shows progress (0-100%)
5. App restores database → migrates schema → restores images → updates paths → looks up location names
6. On completion: "Restore complete!" message
7. App can be restarted to see full recovered data

---

## 3. Architecture & Components

### Core Files

#### `BackupRestoreHelper.kt` (Main logic)
**Location:** `app/src/main/java/com/a42r/pevplugshare/util/`

**Key Methods:**

| Method | Purpose | Key Logic |
|--------|---------|-----------|
| `restoreFromZip()` | Entry point for restore | Orchestrates extraction → DB restore → image restore → completion |
| `extractZip()` | Unzip backup file | Preserves directory structure in temp dir |
| `restoreDatabase()` | Copy DB + handle migrations | Clears Room instance → copies file → lets Room handle migration |
| `restoreImages()` | Extract images from backup | Handles both `location_images/` and `ef/Pictures/` paths |
| `completeSavedLocations()` | **[NEW]** Fix paths & names | Maps images to saved_for_later + looks up location names |

**Critical Implementation:**

```kotlin
// Image mapping algorithm
val restoredImages = appImagesDir.listFiles().filter { it.isFile }
var imageIndex = 0
for (saved_location in saved_for_later) {
    val expectedCount = saved_location.photoPaths.split(",").size
    val assignedImages = restoredImages.subList(imageIndex, 
                                                 imageIndex + expectedCount)
    updateDB(saved_location.id, assignedImages.map { it.absolutePath })
    imageIndex += expectedCount
}

// Location name lookup
val closestLocation = db.query(
    "SELECT name FROM locations 
     WHERE ABS(latitude - ?) < 0.001 AND ABS(longitude - ?) < 0.001"
)
updateDB(saved_location.id, closestLocation.name)
```

#### `BackupRestoreViewModel.kt` (State management)
**Location:** `app/src/main/java/com/a42r/pevplugshare/viewmodel/`

**Sealed Class: RestoreState**
- `Idle` - No operation
- `SelectingFile` - File picker open
- `FileSelected(fileName)` - File chosen
- `Validating` - Checking backup integrity
- `Restoring(message, progress)` - In progress (0-100%)
- `Success` - Completed
- `Error(message)` - Failed

**Critical Pattern:**
```kotlin
val progressCallback = BackupRestoreHelper.ProgressCallback { message, progress ->
    _restoreState.postValue(RestoreState.Restoring(message, progress))  // postValue = thread-safe!
}
```

#### `BackupRestoreDialogFragment.kt` (UI)
**Location:** `app/src/main/java/com/a42r/pevplugshare/ui/debug/`

**UI Elements:**
- Status text (shows current operation)
- Progress bar (0-100%)
- Action buttons (Restore/Cancel)

**Integration Point:**
```kotlin
// In MainActivity.kt
fun showRestoreBackup() {
    BackupRestoreDialogFragment().show(supportFragmentManager, "backup_restore")
}
```

#### `PEVPlugShareDatabase.kt` (Database setup)
**Location:** `app/src/main/java/com/a42r/pevplugshare/data/db/`

**Key Migrations:**
- `MIGRATION_10_11` - Set all locations to SYNCED
- `MIGRATION_11_12` - Add server sync fields (serverId, serverVersion, etc.)

**Fallback Handling:**
```kotlin
fun getDatabase(context: Context): PEVPlugShareDatabase {
    try {
        return buildDatabase(context, false)  // Try normal path
    } catch (e: IllegalStateException) {
        if (e.message?.contains("Room cannot verify data integrity") == true) {
            return buildDatabase(context, true)  // Use fallback migration
        }
        throw e
    }
}
```

---

## 4. Data Flow Diagram

```
ZIP File (backup_full.zip)
    ↓
extractZip() → Temp directory with:
    ├── apps/com.a42r.pevplugshare/db/pevplugshare.db
    ├── apps/com.a42r.pevplugshare/db/pevplugshare.db-wal
    ├── apps/com.a42r.pevplugshare/db/pevplugshare.db-shm
    ├── apps/com.a42r.pevplugshare/f/location_images/...jpg
    └── apps/com.a42r.pevplugshare/ef/Pictures/QUICK_LOC_*.jpg
    ↓
restoreDatabase() → /data/user/0/com.a42r.pevplugshare/databases/
    ├── Copy .db file
    ├── Copy .wal & .shm files
    └── Room opens → triggers MIGRATION_11_12
    ↓
restoreImages() → /data/user/0/com.a42r.pevplugshare/files/location_images/
    ├── Copy location_images/*
    └── Copy ef/Pictures/* (rename to match naming scheme)
    ↓
completeSavedLocations()
    ├── Map QUICK_LOC_*.jpg → actual filenames
    ├── Look up location names by coordinates
    └── Update saved_for_later table
    ↓
Progress: 100% "Restore complete!"
```

---

## 5. Testing & Validation

### Test Data
**Source:** S25 device backup from 2026-04-26
**Location:** `~/backup_full.zip` (259 MB)

**Contents:**
- 35 charging locations with full metadata
- 7 saved locations with photos
- 49 image files (JPGs)
- Database v11 schema

### Test Results

| Test | Result | Evidence |
|------|--------|----------|
| Database restoration | ✅ PASS | "Location count before migration: 35" |
| Schema migration | ✅ PASS | "MIGRATION_11_12 completed successfully" |
| Image extraction | ✅ PASS | "Restored 49 image files" |
| Image path mapping | ✅ PASS | Photo paths updated in saved_for_later |
| Location name lookup | ✅ PASS | "Found location name for saved_1773226196895: Springfield Central" |
| Threading | ✅ PASS | No "Cannot invoke setValue on background thread" errors |

### Manual Testing Checklist
```
[ ] Start app with clean database
[ ] Open menu → [DEBUG] Restore Backup
[ ] Select ~/backup_full.zip
[ ] Verify progress bar shows 0-100%
[ ] Verify status messages: "Extracting...", "Restoring database...", "Restoring images...", "Restore complete!"
[ ] Restart app
[ ] Verify 35 locations display with names
[ ] Open Saved tab
[ ] Verify 7 saved locations show with location names
[ ] Verify images display (not gray placeholders)
[ ] Tap on saved location → verify photo and name visible
```

### Automated Tests
**Location:** `app/src/androidTest/java/com/a42r/pevplugshare/`

**Test Class:** `BackupRestoreTest.kt`
- Tests database preservation
- Tests image file count
- Tests saved locations data integrity

**Run with:**
```bash
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.a42r.pevplugshare.BackupRestoreTest
```

---

## 6. Known Limitations & Future Work

### Current Limitations
1. **Debug-only feature** - No UI in release builds (controlled by `isDebugBuild`)
2. **Manual file selection** - No automatic backup discovery
3. **No encryption** - Backup files are unencrypted ZIP (OK for testing)
4. **No incremental backup** - Always backs up everything
5. **Image naming mismatch** - Backup uses different naming than app (handled by `completeSavedLocations()`)

### Potential Improvements
1. **Auto-backup scheduling** - Periodic backups to cloud storage
2. **Selective restore** - Choose which locations to restore
3. **Backup verification** - Checksum validation before restore
4. **Compression options** - Reduce backup size for cloud upload
5. **Multi-device sync** - Sync between S25 and emulator via cloud
6. **User-facing restore** - Remove [DEBUG] label, make production-ready
7. **Backup history** - Track and manage multiple backup versions

---

## 7. Setup & Deployment

### Building Debug APK
```bash
cd /home/philg/src/AndroidStudioProjects/PEVPlugShare
./gradlew assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk
```

### Installing on Emulator
```bash
adb uninstall com.a42r.pevplugshare
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Pushing Backup File
```bash
adb push ~/backup_full.zip /sdcard/Download/
```

### Accessing Feature
1. Launch app
2. Skip login (local database mode)
3. Tap hamburger menu (☰)
4. Scroll to bottom → "[DEBUG] Restore Backup"
5. Select backup file from file picker

### Logs
```bash
adb logcat -d | grep "BackupRestore\|DB_MIGRATION"
adb logcat | grep "BackupRestore"  # Real-time
```

---

## 8. Code Review Checklist

Items implemented:
- ✅ Proper coroutine usage (Dispatchers.IO for file ops)
- ✅ Thread-safe LiveData (postValue vs setValue)
- ✅ Error handling with try-catch blocks
- ✅ Logging at all critical points
- ✅ Resource cleanup (temp directory deletion)
- ✅ NULL safety (?.let patterns)
- ✅ No hardcoded paths (using context.filesDir)
- ✅ Proper database connection management (close() calls)

---

## 9. Troubleshooting Guide

### "Database file not found in backup"
**Cause:** Backup ZIP has wrong structure  
**Solution:** Verify ZIP contains `apps/com.a42r.pevplugshare/db/pevplugshare.db`  
**Check:** `unzip -l backup_full.zip | grep pevplugshare.db`

### "Room cannot verify the data integrity"
**Cause:** Old database schema incompatible  
**Solution:** Fallback migration triggers automatically  
**Evidence:** Log shows "fallback=true"

### "Cannot invoke setValue on background thread"
**Cause:** Updating LiveData from Dispatchers.IO  
**Solution:** Already fixed - using postValue() in progress callback  
**Verify:** Search for `_restoreState.postValue()` in code

### Images not showing after restore
**Cause:** Image path mapping failed  
**Solution:** Check `completeSavedLocations()` logs for "Mapped" entries  
**Verify:** `SELECT photoPaths FROM saved_for_later LIMIT 1;`

### Location names showing as "Unknown"
**Cause:** Coordinates don't match within 0.001°  
**Solution:** Adjust tolerance in `completeSavedLocations()` from 0.001 to 0.01  
**Code:** `WHERE ABS(latitude - ?) < 0.001` line 316

---

## 10. Contact & Questions

For questions about this implementation:
1. Check logs: `adb logcat -d | grep BackupRestore`
2. Review test results in `BackupRestoreTest.kt`
3. Compare with working example from 12:02:37 session logs
4. Examine `completeSavedLocations()` method documentation

---

## 11. Files Modified/Created

### New Files
- `app/src/main/java/com/a42r/pevplugshare/util/BackupRestoreHelper.kt`
- `app/src/main/java/com/a42r/pevplugshare/viewmodel/BackupRestoreViewModel.kt`
- `app/src/main/java/com/a42r/pevplugshare/ui/debug/BackupRestoreDialogFragment.kt`
- `app/src/androidTest/java/com/a42r/pevplugshare/BackupRestoreTest.kt`

### Modified Files
- `app/src/main/java/com/a42r/pevplugshare/ui/main/MainActivity.kt` - Added restore menu item
- `app/src/main/java/com/a42r/pevplugshare/data/db/PEVPlugShareDatabase.kt` - Enhanced migration handling
- `app/build.gradle` - Added Hilt + Room dependencies

### Key Commit
```
fix: Handle schema mismatch gracefully with fallback and restore images from both paths
- Implement completeSavedLocations() for image path mapping
- Lookup location names by coordinates
- Add Room migration fallback on schema validation failure
- Use postValue() for thread-safe LiveData updates
```

---

## 12. Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2026-04-26 | Initial implementation - database restore, image extraction, path mapping, name lookup |

---

**Document Status:** ✅ Ready for handover  
**Last Updated:** 2026-04-26 12:15 UTC
