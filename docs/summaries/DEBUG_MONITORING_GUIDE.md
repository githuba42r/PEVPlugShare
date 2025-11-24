# OsmAnd Web API Debug Monitoring Guide

## Debug Logging Added

The app now includes comprehensive debug logging for:
1. **Login Process** - Request, response, and session cookies
2. **List Files API** - Request, response, and file details
3. **Favorites Categories** - Extracted category names

## ADB Logcat Commands

### Monitor ALL Authentication Debug Messages
```bash
adb logcat -s OsmAndAuth
```

### Monitor Login + ViewModel Messages
```bash
adb logcat | grep -E "(OsmAndAuth|LoginViewModel)"
```

### Monitor Complete Authentication Flow
```bash
adb logcat | grep -E "(OsmAndAuth|LoginViewModel|CookieManager)"
```

### Monitor with Color (if supported)
```bash
adb logcat -s OsmAndAuth | grep --color=always -E "✅|❌|━━━|Cookie|Response|Category"
```

### Save to File
```bash
adb logcat -s OsmAndAuth > osmand_debug.log
```

### Clear Log First, Then Monitor
```bash
adb logcat -c && adb logcat -s OsmAndAuth
```

### Monitor Specific Device
```bash
adb -s 10.0.1.91:5555 logcat -s OsmAndAuth
```

## Debug Output Format

### Login Flow Debug Output:

```
D/OsmAndAuth: ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
D/OsmAndAuth: Starting Web API Login
D/OsmAndAuth: Username: p@a42r.com
D/OsmAndAuth: Endpoint: https://osmand.net/mapapi/auth/login
D/OsmAndAuth: Cleared existing cookies
D/OsmAndAuth: Sending login request...
D/OsmAndAuth: Login Response:
D/OsmAndAuth:   Status: ok
D/OsmAndAuth:   Message: null
D/OsmAndAuth:   User ID: 12345
D/OsmAndAuth: Session Cookies Received: 1
D/OsmAndAuth:   Cookie 0:
D/OsmAndAuth:     Name: JSESSIONID
D/OsmAndAuth:     Value: ABC123... (truncated)
D/OsmAndAuth:     Domain: osmand.net
D/OsmAndAuth:     Path: /
D/OsmAndAuth:     Expires: 1699999999999
D/OsmAndAuth:     Secure: true
D/OsmAndAuth:     HttpOnly: true
D/OsmAndAuth: ✅ Login Successful!
D/OsmAndAuth: Credentials saved, session refresh scheduled
D/OsmAndAuth: ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

### List Files Flow Debug Output:

```
D/OsmAndAuth: ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
D/OsmAndAuth: Fetching Favorites Categories
D/OsmAndAuth: Endpoint: https://osmand.net/mapapi/list-files
D/OsmAndAuth: Using 1 session cookie(s)
D/OsmAndAuth: Sending list-files request...
D/OsmAndAuth: List Files Response:
D/OsmAndAuth:   Total Files: 10
D/OsmAndAuth:   Total Unique Files: 8
D/OsmAndAuth:   Unique Files Count: 8
D/OsmAndAuth: Favorites Files Found: 3
D/OsmAndAuth:   File 0:
D/OsmAndAuth:     Name: PEV Charging.gpx
D/OsmAndAuth:     Type: FAVOURITES
D/OsmAndAuth:     Size: 4096 bytes
D/OsmAndAuth:     Points: 25
D/OsmAndAuth:     Categories: PEV Charging
D/OsmAndAuth:   File 1:
D/OsmAndAuth:     Name: Home.gpx
D/OsmAndAuth:     Type: FAVOURITES
D/OsmAndAuth:     Size: 2048 bytes
D/OsmAndAuth:     Points: 5
D/OsmAndAuth:     Categories: Home
D/OsmAndAuth: Extracted Category Names: PEV Charging, Home
D/OsmAndAuth: ✅ Successfully retrieved 2 categories
D/OsmAndAuth: ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

## Quick Start Testing

1. **Clear logs and start monitoring:**
   ```bash
   adb logcat -c && adb logcat -s OsmAndAuth
   ```

2. **Open app and login with credentials:**
   - Email: p@a42r.com
   - Password: 2K3f@gydN06Kt7Sk

3. **Watch the debug output** showing:
   - Login request sent
   - Login response received
   - Session cookies saved
   - Favorites categories fetched
   - Category names extracted

## Debug Information Logged

### Login Process:
- ✅ Username (email)
- ✅ API endpoint
- ✅ Response status
- ✅ Response message
- ✅ User ID
- ✅ Number of cookies received
- ✅ Cookie details (name, value preview, domain, path, expiry, flags)
- ✅ Success/failure indication

### List Files Process:
- ✅ API endpoint
- ✅ Number of cookies sent
- ✅ Total files count
- ✅ Unique files count
- ✅ Number of FAVOURITES files found
- ✅ Each file details (name, type, size, points, categories)
- ✅ Extracted category names
- ✅ Success indication

## Troubleshooting

### No logs appearing?
```bash
# Check if device is connected
adb devices

# Try filtering differently
adb logcat | grep OsmAnd

# Check all logs
adb logcat
```

### Too many logs?
```bash
# Filter only our debug tag
adb logcat -s OsmAndAuth:D *:S

# Or use grep for cleaner output
adb logcat | grep --line-buffered "OsmAndAuth"
```

### Want to see HTTP traffic?
```bash
# Add OkHttp logging
adb logcat | grep -E "(OsmAndAuth|OkHttp)"
```

## Advanced Filtering

### See only successful operations:
```bash
adb logcat -s OsmAndAuth | grep "✅"
```

### See only errors:
```bash
adb logcat -s OsmAndAuth | grep "❌"
```

### See only cookies:
```bash
adb logcat -s OsmAndAuth | grep -i cookie
```

### See only categories:
```bash
adb logcat -s OsmAndAuth | grep -i category
```

## Live Monitoring Script

Save this as `monitor_auth.sh`:
```bash
#!/bin/bash
echo "Clearing logcat..."
adb logcat -c
echo "Monitoring OsmAnd authentication..."
echo "=================================="
adb logcat -s OsmAndAuth
```

Make it executable:
```bash
chmod +x monitor_auth.sh
./monitor_auth.sh
```

## Integration with Development

The debug logs are controlled by:
- Tag: `OsmAndAuth` (constant in AuthRepositoryImpl)
- Level: `Log.d()` for info, `Log.e()` for errors
- Format: Boxed with ━━━ separators for readability

To modify debug level in production, change:
```kotlin
companion object {
    private const val TAG = "OsmAndAuth"
    private const val DEBUG = BuildConfig.DEBUG // Only log in debug builds
}
```
