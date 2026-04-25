# Emulator Setup Guide

## Current Status

✅ **Emulator running:** emulator-5554 (Android 16, API 36)  
✅ **App installed:** com.a42r.pevplugshare  
✅ **Backup restored:** 20260425_113300  
✅ **Test data loaded:** 35 locations, 7 saved items, 35 photos  

## Quick Commands

### View App Logs
```bash
adb logcat -s PEVPlugShare
```

### Restart App
```bash
adb shell am force-stop com.a42r.pevplugshare
adb shell am start -n com.a42r.pevplugshare/.MainActivity
```

### Debug in Android Studio
1. Open Android Studio
2. Click **Run** → **Debug 'app'**
3. Set breakpoints in code
4. Interact with emulator to trigger breakpoints

### Reinstall Latest Build
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Take Screenshots
```bash
adb shell screencap -p > screenshot.png
```

## Device Backups

Keep multiple timestamped backups for regression testing:

```bash
# View backup metadata
cat device_backups/20260425_113300/BACKUP_METADATA.txt

# Inspect database
./device_backups/20260425_113300/inspect-database.sh

# Query database directly
sqlite3 device_backups/20260425_113300/backup_extracted/apps/com.a42r.pevplugshare/db/pevplugshare.db
```

## Test Data Reference

### Database Tables

| Table | Records | Purpose |
|-------|---------|---------|
| locations | 35 | Stored location markers |
| saved_for_later | 7 | User-saved locations |
| location_photos | 35 | Photo metadata |
| user_preferences | N/A | App settings |
| sync_state | N/A | Sync tracking |

### Restore Original S25 Device

If you need to restore the backup to your S25 device:

```bash
# Navigate to backup
cd device_backups/20260425_113300

# Restore to device
./restore-to-device.sh 10.0.1.91:45425
```

## Troubleshooting

### App Crashes
Check logs for error details:
```bash
adb logcat -s PEVPlugShare:* AndroidRuntime:* -v threadtime
```

### Database Issues
Reset app data (careful - loses local data):
```bash
adb shell pm clear com.a42r.pevplugshare
```

### Emulator Not Responding
Restart emulator:
```bash
adb emu kill
emulator -avd Medium_Phone_API_36
```

## Feature Development Workflow

1. **Write code** in Android Studio
2. **Build & deploy:** `adb install -r app/build/outputs/apk/debug/app-debug.apk`
3. **Test with real data** in emulator (35 locations, etc.)
4. **Debug** using Android Studio debugger or logcat
5. **Verify** against test data from S25 device

## Notes

- Test data (35 locations, 7 saved items) provides realistic app behavior
- Multiple backups allow regression testing against different app versions
- Both S25 device and emulator can run current/past app versions for testing
