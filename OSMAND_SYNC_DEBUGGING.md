# OsmAnd Sync Debugging Guide

## Quick ADB Command

Monitor the OsmAnd location sync function in real-time:

```bash
adb logcat -v threadtime com.a42r.pevplugshare:D *:S | grep -E "OsmandLocationSync|ADD FAVORITE|STEP|✅|❌|⚠️|📍|💾|📤|📥"
```

## Alternative Commands

### Show full verbose output with colors:
```bash
adb logcat -v color com.a42r.pevplugshare:D *:S | grep "OsmandLocationSync"
```

### Capture to file and monitor simultaneously:
```bash
adb logcat -v threadtime com.a42r.pevplugshare:D *:S | tee osmand_sync.log | grep -E "OsmandLocationSync"
```

### Show only errors and warnings:
```bash
adb logcat -v threadtime com.a42r.pevplugshare:E com.a42r.pevplugshare:W *:S | grep "OsmandLocationSync"
```

### Real-time monitoring with timestamps:
```bash
adb logcat -v time com.a42r.pevplugshare:D *:S | grep -E "ADD FAVORITE|✅|❌"
```

## Log Output Structure

The debugging logs are organized into clear sections with emoji indicators:

### Location Details Phase
```
📍 LOCATION DETAILS:
  Name: [location name]
  Category: [category]
  Coordinates: [lat, lon]
```

### Validation Phase
```
🔍 CHECKING AUTHENTICATION:
  Has token: [true/false]
  Token length: [number]
```

### API Request Phase
```
📤 REQUEST DETAILS:
  Endpoint: POST https://osmand.net/mapapi/fav/add
  fileName: [group name]
  updatetime: [timestamp]
```

### API Response Phase
```
📥 RESPONSE RECEIVED:
  HTTP Status: [code] [message]
  Is Successful: [true/false]
```

### Database Update Phase
```
💾 LOCAL DB UPDATE:
  osmandSyncedToServer: [true/false]
  osmandGpxIndex: [index or null]
```

## What to Look For

### Success Indicators
- ✅ `ADD SUCCESSFUL!`
- ✅ `Found location at index` (in GPX verification)
- ✅ `COMPLETE: Location synced to OsmAnd`

### Failure Indicators
- ❌ `ADD FAILED: HTTP [code]`
- ⚠️ `ERROR 106: File was changed!` (timestamp mismatch)
- ❌ `Could not find location in GPX`
- ❌ `Could not download GPX`

### Common Issues

#### 1. Authentication Issues
```
🔍 CHECKING AUTHENTICATION:
  Has token: false
  Token length: 0
```
**Solution**: Check OsmAnd OAuth token is properly stored

#### 2. Timestamp Mismatch (Error 106)
```
⚠️ ERROR 106: File was changed!
Timestamp mismatch - will retry with fresh updatetime
```
**Solution**: Automatic retry with fresh updatetime (up to 2 attempts)

#### 3. Location Not Found in GPX
```
⚠️ Could not find location in GPX - index will remain null
  Searched for: '[location name]'
```
**Solution**: Check if name contains special characters or encoding issues

#### 4. Network Error
```
❌ Exception during add: Connection failed
💥 STACK TRACE:
    at okhttp3.internal.connection.RealConnection.connect
```
**Solution**: Check network connectivity and API endpoint

## Detailed Monitoring Steps

### Step 1: Trigger Location Creation
1. Open app
2. Create new location
3. Save location
4. Watch logs for initial ADD operation

### Step 2: Monitor API Call
```bash
adb logcat -v threadtime | grep -E "STEP [0-9]|POST|GET|Response:"
```

### Step 3: Verify Database Update
```bash
adb logcat -v threadtime | grep "LOCAL DB UPDATE"
```

### Step 4: Check for Duplicates
After sync, check in OsmAnd app that only one location was added.

## Log Levels

- `Log.d()` - Debug (detailed information)
- `Log.w()` - Warning (⚠️ issues that don't stop operation)
- `Log.e()` - Error (❌ operation failed)

## Disabling Debugging

To reduce log noise in production, the `OsmandLocationSyncService.kt` can be modified to use `BuildConfig.DEBUG`:

```kotlin
if (BuildConfig.DEBUG) {
    Log.d(TAG, "Detailed debug message")
}
```

## Related Files

- **Main Service**: `app/src/main/java/com/a42r/pevplugshare/data/repository/OsmandLocationSyncService.kt`
- **API Client**: `app/src/main/java/com/a42r/pevplugshare/data/remote/OsmandApiClient.kt`
- **Models**: `app/src/main/java/com/a42r/pevplugshare/data/api/OsmandApiService.kt`
- **Token Manager**: `app/src/main/java/com/a42r/pevplugshare/data/remote/TokenManager.kt`

## Performance Notes

- First sync attempt: ~2-5 seconds (with GPX download/parsing)
- Retry on error 106: ~1-3 seconds
- DB update: <100ms

Total expected time for successful ADD: 3-8 seconds depending on network
