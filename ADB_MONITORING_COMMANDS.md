# ============================================================================
# OSMAND LOCATION SYNC - ADB MONITORING COMMANDS
# ============================================================================

## PRIMARY COMMAND - Full Real-Time Monitoring
## Copy and paste this into your terminal to monitor ADD location operations:

adb logcat -v threadtime com.a42r.pevplugshare:D *:S | grep -E "OsmandLocationSync|ADD FAVORITE|STEP|✅|❌|⚠️|📍|💾|📤|📥"


## ALTERNATIVE COMMANDS FOR DIFFERENT SCENARIOS

# 1. MINIMAL OUTPUT - Only success/failure markers
adb logcat com.a42r.pevplugshare:D *:S | grep -E "✅|❌"

# 2. DEBUG WITH TIMESTAMPS
adb logcat -v time com.a42r.pevplugshare:D *:S

# 3. SHOW ONLY ERRORS AND WARNINGS
adb logcat -v threadtime com.a42r.pevplugshare:E com.a42r.pevplugshare:W *:S

# 4. SAVE TO FILE (also display in terminal)
adb logcat -v threadtime com.a42r.pevplugshare:D *:S | tee osmand_sync_$(date +%s).log

# 5. FOLLOW HTTP REQUEST/RESPONSE
adb logcat com.a42r.pevplugshare:D *:S | grep -E "POST|GET|Response:|HTTP|Error"

# 6. WATCH ONLY API RESPONSES
adb logcat com.a42r.pevplugshare:D *:S | grep -E "📥|HTTP|ADD SUCCESSFUL|ADD FAILED"

# 7. CLEAR LOGS BEFORE TEST (useful for clean monitoring)
adb logcat -c && adb logcat -v threadtime com.a42r.pevplugshare:D *:S

# ============================================================================
# STEP-BY-STEP TESTING WORKFLOW
# ============================================================================

# Terminal 1: Start monitoring
adb logcat -v threadtime com.a42r.pevplugshare:D *:S | grep -E "OsmandLocationSync|ADD FAVORITE|STEP|✅|❌|⚠️|📍|💾|📤|📥"

# Terminal 2: Build and deploy debug app
cd /home/philg/src/AndroidStudioProjects/PEVPlugShare
./gradlew assembleDebug -q && adb install -r app/build/outputs/apk/debug/app-debug.apk

# Terminal 1: Watch for logs as you:
#   1. Open app
#   2. Log in to OsmAnd
#   3. Create new location
#   4. Save location
#   5. Monitor sync process in logs

# ============================================================================
# WHAT TO LOOK FOR IN LOGS
# ============================================================================

# SUCCESS FLOW:
#   ✅ ADD FAVORITE - EXACT ALGORITHM
#   📍 LOCATION DETAILS: [location name]
#   🔍 CHECKING AUTHENTICATION: Has token: true
#   📡 Calling /mapapi/list-files endpoint...
#   ✅ Using updatetime: [timestamp]
#   📤 REQUEST DETAILS: POST https://osmand.net/mapapi/fav/add
#   📥 RESPONSE RECEIVED: HTTP 200 OK
#   ✅ ADD SUCCESSFUL!
#   ✅ Found location at index [N] in GPX
#   �� LOCAL DB UPDATE COMPLETE

# ERROR INDICATORS:
#   ❌ ADD FAILED: HTTP [error code]
#   ⚠️ ERROR 106: File was changed!
#   ❌ Could not find location in GPX
#   ❌ Could not download GPX
#   ❌ Exception during add

# ============================================================================
# SPECIFIC TROUBLESHOOTING COMMANDS
# ============================================================================

# Check if authentication is working:
adb logcat com.a42r.pevplugshare:D *:S | grep "CHECKING AUTHENTICATION"

# Check HTTP status codes:
adb logcat com.a42r.pevplugshare:D *:S | grep "HTTP"

# Check GPX parsing:
adb logcat com.a42r.pevplugshare:D *:S | grep -E "GPX|wptName|Found location"

# Check timestamp issues:
adb logcat com.a42r.pevplugshare:D *:S | grep -E "updatetime|ERROR 106"

# Check database updates:
adb logcat com.a42r.pevplugshare:D *:S | grep "LOCAL DB"

# ============================================================================
# PERFORMANCE MONITORING
# ============================================================================

# Time the entire operation (useful for optimization):
time (adb logcat -v threadtime com.a42r.pevplugshare:D *:S | grep -E "ADD FAVORITE|COMPLETE" | head -2)

# Count log messages per second:
adb logcat com.a42r.pevplugshare:D *:S | pv -L 100

# ============================================================================
# CLEANUP
# ============================================================================

# Clear app data (reset sync state):
adb shell pm clear com.a42r.pevplugshare

# Uninstall app:
adb uninstall com.a42r.pevplugshare

