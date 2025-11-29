# OsmAnd Location Sync - Complete Diagnostic Guide

## Problem: No logs appear when creating/syncing location

### Root Cause Analysis

The sync might not be triggered because:

1. **Sync dialog never appears** → Check EditLocationActivity
2. **Dialog appears but user doesn't tap** → Manual step required
3. **User tapped but service doesn't start** → Check startService call
4. **Service starts but sync doesn't run** → Check intent extras
5. **Sync runs but logs filtered** → Check logcat filter level

---

## Quick Diagnostic - Run This First

```bash
# Terminal 1: Clear and start monitoring ALL logs
adb logcat -c
adb logcat -v threadtime com.a42r.pevplugshare:V *:S 2>&1 | tee osmand_full_debug.log

# Terminal 2: Open app and create location
adb shell am start -n com.a42r.pevplugshare/.ui.main.MainActivity

# In app:
# 1. Create new location  
# 2. Fill in details
# 3. Save location
# 4. Watch Terminal 1 for:
#    - "EditLocationViewModel"
#    - "Saving location"
#    - "Showing sync dialog" 
#    - "OsmandBackgroundSync"
```

---

## Trace Every Step

### Step 1: Location Creation
Look for these logs in order:

```
EditLocationActivity: Location saved
EditLocationViewModel: Save completed
MainActivity: Returned from edit
```

**If missing:** Location save didn't complete

### Step 2: Sync Dialog
```
EditLocationViewModel: Should show sync dialog?
MainActivity: Displaying sync prompt
```

**If missing:** No sync dialog being shown (check if that code path is hit)

### Step 3: Service Start
```
OsmandBackgroundSync: onStartCommand called
OsmandBackgroundSync: Location ID: [uuid]
```

**If missing:** startService() not being called

### Step 4: Location Lookup
```
OsmandBackgroundSync: Found location: [name]
OsmandBackgroundSync: Calling smartSync...
```

**If missing:** Location lookup failed in DB

### Step 5: smartSync Execution
```
OsmandLocationSync: ADD FAVORITE - EXACT ALGORITHM
OsmandLocationSync: STEP 1: Getting current updatetime
...
OsmandLocationSync: COMPLETE: Location synced to OsmAnd
```

**If missing:** smartSync not being called

---

## Detailed Grep Filters

### Find service startup:
```bash
adb logcat | grep -i "OsmandBackgroundSync\|startService"
```

### Find all OsmAnd operations:
```bash
adb logcat | grep -i "osmand\|favorite\|sync" | head -100
```

### Find location save:
```bash
adb logcat | grep -i "EditLocation\|save.*location" | head -50
```

### Find errors only:
```bash
adb logcat com.a42r.pevplugshare:E *:S
```

### Real-time monitoring with timestamps:
```bash
adb logcat -v threadtime | grep -E "OsmandBackgroundSync|OsmandLocationSync"
```

### Save full log to file:
```bash
adb logcat -v threadtime > osmand_$(date +%s).log &
# Now trigger sync, then kill the background job
kill %1
```

---

## Check EditLocationActivity Code Path

The sync might not be triggered if:

1. **Dialog not shown** → Check SharedPreferences setting
   ```bash
   adb shell getprop | grep osmand
   ```

2. **Dialog shown but condition not met** → Check EditLocationViewModel
   ```
   Look for: "getSyncToOsmandOnUpdate()"
   ```

3. **User must tap explicitly** → Look for sync prompt UI

---

## Verify Intent Extras

If service starts, check these logs:

```bash
adb logcat | grep -E "location_id|EXTRA_"
```

Required extras:
- `location_id` - UUID string (required)
- `description` - Location description (optional if generating)
- `generate_description` - Boolean (optional)

---

## Database Lookup Issues

If service starts but location not found:

```bash
# Check location was actually saved
adb logcat | grep -i "location.*saved\|database.*insert"

# Check repository queries
adb logcat | grep -i "getLocationsByCategory\|room.*query"
```

---

## Network/API Issues

If sync starts but fails:

```bash
# Check authentication
adb logcat | grep -i "token\|auth\|401\|403"

# Check API calls  
adb logcat | grep -i "https\|osmand.*api\|http.*response"

# Check network errors
adb logcat | grep -i "network\|connection\|timeout"
```

---

## Expected Log Output (Complete Flow)

### Success Path:
```
EditLocationActivity: Saving location
EditLocationViewModel: Location saved to database
MainActivity: Showing OsmAnd sync dialog
[User taps "Sync to OsmAnd"]
EditLocationActivity: User confirmed sync
MainActivity: Starting OsmandBackgroundSyncService
OsmandBackgroundSync: ═════════════════════════════════════════
OsmandBackgroundSync: 🚀 OsmandBackgroundSyncService onStartCommand called
OsmandBackgroundSync: ═════════════════════════════════════════
OsmandBackgroundSync: Location ID: [uuid]
OsmandBackgroundSync: Generate description: false
OsmandBackgroundSync: Found location: [location name]
OsmandBackgroundSync: 🔄 CALLING smartSync with category: Charging Locations
OsmandLocationSync: ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
OsmandLocationSync: ADD FAVORITE - EXACT ALGORITHM
OsmandLocationSync: 📍 LOCATION DETAILS:
OsmandLocationSync: ✅ ADD SUCCESSFUL!
OsmandBackgroundSync: ✅✅✅ Smart sync SUCCESS ✅✅✅
OsmandBackgroundSync: Broadcast result: success=true
```

### What Each Log Tells You:
- **EditLocationActivity** → UI layer
- **EditLocationViewModel** → Business logic
- **MainActivity** → Navigation
- **OsmandBackgroundSync** → Service layer (user tapped sync)
- **OsmandLocationSync** → API calls to OsmAnd

---

## Most Common Issues & Solutions

### Issue 1: No OsmandBackgroundSync logs
**Cause:** Service not being started
**Check:**
```bash
adb logcat | grep "startService\|startForegroundService"
```
**Fix:** Verify EditLocationActivity/ViewModel calling startService()

### Issue 2: Service starts but OsmandLocationSync logs missing
**Cause:** smartSync not being called
**Check:**
```bash
adb logcat | grep "CALLING smartSync"
```
**Fix:** Check if location lookup failed (not found in DB)

### Issue 3: smartSync starts but ADD FAVORITE missing
**Cause:** addFavorite not called, or updateFavorite called instead
**Check:**
```bash
adb logcat | grep "ADD FAVORITE\|UPDATE FAVORITE\|SMART SYNC"
```
**Fix:** Check location matching logic in smartSync

### Issue 4: Logs appear then stop abruptly
**Cause:** Exception/error in middle of operation
**Check:**
```bash
adb logcat | grep Exception
```
**Fix:** Look at full stack trace

---

## How to Enable More Verbose Logging

If you still don't see logs:

1. **Increase log buffer:**
   ```bash
   adb logcat -G 16M
   adb logcat -g  # check current size
   ```

2. **Enable all logs (dangerous - lots of output):**
   ```bash
   adb logcat *:V
   ```

3. **Try without filter:**
   ```bash
   adb logcat | head -200
   ```

4. **Check if app is even running:**
   ```bash
   adb shell "ps aux | grep pevplugshare"
   ```

---

## Create Your Own Diagnostic Flow

Run this complete test:

```bash
#!/bin/bash

echo "=== OSMAND SYNC DIAGNOSTIC ==="
echo ""
echo "1. Clearing logs..."
adb logcat -c

echo "2. Starting log capture..."
adb logcat -v threadtime > osmand_diag_$(date +%s).log &
LOG_PID=$!

echo "3. Open app manually and:"
echo "   - Create location"
echo "   - Tap 'Sync to OsmAnd'"
echo "   - Wait 10 seconds"
echo ""
read -p "Press ENTER when done..."

echo "4. Saving logs..."
sleep 2
kill $LOG_PID 2>/dev/null
wait $LOG_PID 2>/dev/null

echo "5. Analyzing..."
LOG_FILE=$(ls -t osmand_diag_*.log | head -1)
echo "Log file: $LOG_FILE"
echo ""
echo "Service started:"
grep "OsmandBackgroundSync\|onStartCommand" $LOG_FILE | head -5
echo ""
echo "Sync called:"
grep "smartSync\|ADD FAVORITE" $LOG_FILE | head -5
echo ""
echo "Errors:"
grep "Error\|Exception" $LOG_FILE | head -5
```

---

## Next Steps

1. **Run basic logcat:** `adb logcat com.a42r.pevplugshare:V *:S`
2. **Create location** in app
3. **Look for OsmandBackgroundSync** in output
4. **If no service logs:** Check EditLocationActivity code
5. **If service logs:** Check smartSync invocation
6. **If smartSync logs:** Check OsmandLocationSync logs
7. **Save output:** `adb logcat > debug_$(date +%s).log`

