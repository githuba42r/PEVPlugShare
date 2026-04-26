# Emulator Backup Restore - Quick Start Guide

## Backup Status ✅
- **File:** `backup_clean.zip` (259MB)
- **Location:** `/sdcard/Download/` on emulator
- **Contents:**
  - 35 saved charging locations
  - 18 location photos
  - App settings (no encrypted keys)
  - Database ready to use

## Quick Restore Steps

### 1️⃣ Start the Emulator
```bash
# If not already running
emulator -avd Medium_Phone_API_36
```

### 2️⃣ Open the App
- Launch PEVPlugShare on the emulator

### 3️⃣ Access Restore Feature
- Tap menu button (⋮) in top right
- Select **"[DEBUG] Restore Backup"** (debug feature only)

### 4️⃣ Select Backup File
- Click **"Select Backup File"**
- Navigate to: **Downloads** folder
- Choose: **backup_clean.zip**

### 5️⃣ Review & Restore
- See backup info:
  - ✓ Database: Present
  - Images: 18 files
  - Size: 259 MB
- Click **"Restore"**
- Confirm the restore

### 6️⃣ Wait for Restart
- App will automatically restart after restore completes
- First load may take 10-15 seconds (loading database + images)

### 7️⃣ Log In
- After restart, you'll see the login screen
- Log in with your **OpenRouter API key**
- (This key is needed for the AI features)

## What You'll Have

Once logged in:
- ✅ 35 saved locations visible in list
- ✅ Photos displayed with each location
- ✅ Can tap locations to view details
- ✅ Can test navigation, image viewing, etc.
- ✅ Ready to reproduce bugs or test fixes

## Testing Workflow

```
1. Make code changes
2. ./gradlew assembleDebug
3. adb install -r app/build/outputs/apk/debug/app-debug.apk
4. Test with the restored data on emulator
5. Reproduce crashes, test features
6. Debug with Android Studio
7. Repeat
```

## Logs While Testing

Monitor app logs in real time:
```bash
adb logcat -s PEVPlugShare:* AndroidRuntime:* -v threadtime
```

## Debug in Android Studio

1. Open Android Studio
2. Run → Debug 'app'
3. Set breakpoints in code
4. Interact with emulator to trigger code
5. Step through in debugger

## Restore Again

To start fresh with the same test data:
1. Open restore dialog again
2. Select backup_clean.zip
3. Restore will overwrite current data
4. App restarts with fresh copy

## What's NOT in the Backup

**Intentionally excluded (for security):**
- OpenRouter API keys (encrypted)
- OSMAnd authentication tokens
- Session cookies
- OAuth tokens

**You'll need to provide:**
- OpenRouter API key (for AI features)
- Login credentials (if using server features)

## Troubleshooting

### "Backup file not found"
- Check that backup.zip is in `/sdcard/Download/`
- Push it again: 
  ```bash
  adb push device_backups/20260425_113300/backup_clean.zip /sdcard/Download/
  ```

### App won't start after restore
- Check logs: `adb logcat -s PEVPlugShare`
- Try uninstall and reinstall:
  ```bash
  adb uninstall com.a42r.pevplugshare
  adb install app/build/outputs/apk/debug/app-debug.apk
  ```

### Images not showing
- Check file permissions were transferred correctly
- Try restore again
- Check logs for any errors

### Login required after restore
- This is expected! Encrypted auth isn't transferred
- Just log in with your OpenRouter API key
- All data (locations, photos) will still be there

## Clean Up

To create a new backup at any time:
```bash
# From emulator
adb backup com.a42r.pevplugshare -f new_backup.ab

# Extract and prepare
dd if=new_backup.ab bs=1 skip=24 2>/dev/null | openssl zlib -d | tar xv
cd backup_prep && zip -r ../new_backup.zip apps/

# Push to emulator
adb push new_backup.zip /sdcard/Download/
```

## Next Steps

You now have:
- ✅ Working debug APK with all latest features
- ✅ Clean test data ready to restore
- ✅ Easy way to reset test state
- ✅ Full debugging capabilities

**Ready to debug!** Open the app and test those crashes. 🚀
