# Debug: Backup Restore Feature

This is a debug-only feature (available only in debug APKs) that allows you to restore test data (database + images) from a zip file within the app.

## Purpose

- **Quick test data setup** on emulator without permission issues
- **Reproduce bugs** with specific location data
- **Test image handling** with real photos
- **Regression testing** against different data sets
- **Faster iterations** during debugging

## Components

### Files Added
- `BackupRestoreHelper.kt` - Core restoration logic
- `BackupRestoreViewModel.kt` - UI state management  
- `BackupRestoreDialogFragment.kt` - User interface
- `UtilityModule.kt` - Dependency injection
- `fragment_backup_restore.xml` - Layout
- Menu item in `main_menu.xml`

### How It Works

1. User selects a backup zip file
2. Helper validates the file structure
3. Extracts database and images to temp directory
4. Copies database to app's database directory
5. Copies images to app's files directory
6. Restarts app to reload the data

## Using the Feature

### Step 1: Prepare a Backup Zip

Create a zip file containing the backup structure:

```
backup.zip
├── apps/
│   └── com.a42r.pevplugshare/
│       ├── db/
│       │   └── pevplugshare.db
│       └── f/
│           └── location_images/
│               ├── uuid-1/
│               │   ├── image_0.jpg
│               │   └── image_1.jpg
│               └── location-name/
│                   ├── image_0.jpg
│                   └── image_1.jpg
```

**Pre-made backup available at:**
```
device_backups/20260425_113300/backup.zip
```

This contains:
- 35 saved locations
- 7 saved-for-later items
- 35 location photos

### Step 2: Make Zip Accessible to App

Push the zip to emulator storage:

```bash
adb push device_backups/20260425_113300/backup.zip /sdcard/Download/
```

Or use the file picker to select from local storage.

### Step 3: Restore via Debug Menu

1. **Open the app** on emulator
2. **Click menu button** (⋮) in top right
3. **Select "[DEBUG] Restore Backup"**
4. **Click "Select Backup File"**
5. **Choose the backup zip** (e.g., backup.zip)
6. **Review the info** (should show database and image count)
7. **Click "Restore"** and confirm
8. **Wait for completion** - app will restart automatically

### Step 4: Verify

After restart:
- Locations list should show saved locations
- Images should display with each location
- Database should contain all test data

## Creating Your Own Backup

To capture current test data as a backup:

```bash
# From device
adb shell am force-stop com.a42r.pevplugshare
adb shell am start -n com.a42r.pevplugshare/.ui.auth.SplashActivity

# Backup (shows 35+ locations, images, etc)
adb backup com.a42r.pevplugshare -f backup.ab

# Extract
dd if=backup.ab bs=1 skip=24 | zlib-decompress | tar xv

# Create zip
zip -r backup.zip apps/
```

## Debug Features

The feature is only visible in debug APKs:
- Menu item: `[DEBUG] Restore Backup`
- Hidden in release builds
- No production impact

## Testing

### Manual Test Workflow

```bash
# 1. Build and install debug APK
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk

# 2. Push backup zip
adb push device_backups/20260425_113300/backup.zip /sdcard/Download/

# 3. Open app, use restore feature via menu

# 4. Verify data loaded
adb logcat -s PEVPlugShare
# Should see: "Database restored successfully" etc.

# 5. Test with the locations
# • Open location list
# • Tap on a location
# • Verify images display
# • Test interactions
```

### Troubleshooting

#### "Backup file not found"
- Ensure zip file exists and is readable
- Try pushing to /sdcard/Download/ first

#### "Database file not found in backup"
- Verify zip structure matches expected format
- Check that `apps/com.a42r.pevplugshare/db/pevplugshare.db` exists

#### "Cannot read backup file"  
- Check file permissions
- Try a different file location (/sdcard/Download/)

#### No locations appear after restore
- Check app logs: `adb logcat -s PEVPlugShare`
- Verify database was copied correctly
- Try restarting app manually

## Performance Notes

- Extraction happens on background thread
- Progress UI prevents user interaction during restore
- First restore may take 5-10 seconds (unzip + copy)
- Subsequent restores are faster
- No impact on release app

## Security

- Debug feature only (BuildConfig.DEBUG check)
- Requires explicit file selection
- Asks for confirmation before restoring
- Cannot be accessed in release builds
- Full validation of backup structure

## Future Enhancements

Potential improvements:
- Progress bar during extraction
- Support for partial restores (DB only, images only)
- Backup creation from current state
- Multiple backup management
- Scheduled restores
