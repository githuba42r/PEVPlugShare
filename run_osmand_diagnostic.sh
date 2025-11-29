#!/bin/bash

# OsmAnd Sync Diagnostic - One-stop debugging

set -e

PROJECT_ROOT="/home/philg/src/AndroidStudioProjects/PEVPlugShare"
cd "$PROJECT_ROOT"

echo "╔════════════════════════════════════════════════════════════════════════╗"
echo "║                   OSMAND SYNC DIAGNOSTIC TOOL                         ║"
echo "╚════════════════════════════════════════════════════════════════════════╝"
echo ""

# Step 1: Check device
echo "Step 1/5: Checking device connection..."
if ! adb devices | grep -q "device$"; then
    echo "❌ No device found. Connect device with USB."
    exit 1
fi
DEVICE=$(adb devices | grep "device$" | head -1 | awk '{print $1}')
echo "✅ Device found: $DEVICE"
echo ""

# Step 2: Check app installation
echo "Step 2/5: Checking app installation..."
if ! adb shell pm list packages | grep -q "com.a42r.pevplugshare"; then
    echo "❌ App not installed. Building and installing..."
    ./gradlew assembleDebug -q || exit 1
fi
echo "✅ App installed"
echo ""

# Step 3: Clear and start logging
echo "Step 3/5: Starting diagnostic logging..."
LOG_FILE="osmand_diagnostic_$(date +%s).log"
adb logcat -c
adb logcat -v threadtime > "$LOG_FILE" &
LOG_PID=$!
echo "✅ Capturing logs to: $LOG_FILE"
echo ""

# Step 4: Launch app
echo "Step 4/5: Launching app..."
adb shell am start -n com.a42r.pevplugshare/.ui.main.MainActivity
sleep 2
echo "✅ App launched"
echo ""

# Step 5: Instructions
echo "Step 5/5: Manual test required"
echo "════════════════════════════════════════════════════════════════════════"
echo ""
echo "Now in the app:"
echo "  1. Create a new location"
echo "  2. Fill in location details"  
echo "  3. Save/confirm location"
echo "  4. If dialog appears, tap 'Sync to OsmAnd'"
echo "  5. Wait 5-10 seconds for sync to complete"
echo ""
echo "Watch this output while you test. Looking for key events..."
echo ""

# Monitor for key events in real-time
echo "Key logs to look for:"
echo "  📍 'OsmandBackgroundSync' - Service started"
echo "  🔄 'smartSync\|ADD FAVORITE' - Sync process started"
echo "  ✅ 'COMPLETE\|SUCCESS' - Sync completed"
echo "  ❌ 'FAILED\|Error\|Exception' - Sync failed"
echo ""

# Watch for key events
for i in {1..60}; do
    if grep -q "OsmandBackgroundSync" "$LOG_FILE" 2>/dev/null; then
        echo "✅ Service started detected!"
        break
    fi
    if grep -q "ADD FAVORITE\|smartSync" "$LOG_FILE" 2>/dev/null; then
        echo "✅ Sync process detected!"
        break
    fi
    sleep 1
done

echo ""
echo "Waiting 10 more seconds for sync to complete..."
sleep 10

# Stop logging
echo ""
echo "════════════════════════════════════════════════════════════════════════"
echo "Stopping diagnostic..."
kill $LOG_PID 2>/dev/null || true
wait $LOG_PID 2>/dev/null || true

# Analyze results
echo ""
echo "ANALYSIS RESULTS:"
echo "════════════════════════════════════════════════════════════════════════"
echo ""

echo "📋 Log file: $LOG_FILE"
echo ""

echo "1️⃣  Service initialization:"
if grep -q "OsmandBackgroundSync.*onStartCommand" "$LOG_FILE"; then
    echo "   ✅ Service started"
    grep "OsmandBackgroundSync.*onStartCommand" "$LOG_FILE" | head -1
else
    echo "   ❌ Service NOT started - check if user tapped sync button"
fi
echo ""

echo "2️⃣  Location lookup:"
if grep -q "Found location:" "$LOG_FILE"; then
    echo "   ✅ Location found in database"
    grep "Found location:" "$LOG_FILE" | head -1
else
    echo "   ⚠️  Location NOT found - check database"
fi
echo ""

echo "3️⃣  Sync initiation:"
if grep -q "smartSync\|ADD FAVORITE" "$LOG_FILE"; then
    echo "   ✅ Sync started"
    grep "ADD FAVORITE\|SMART SYNC" "$LOG_FILE" | head -1
else
    echo "   ❌ Sync NOT started - check if location was found"
fi
echo ""

echo "4️⃣  API communication:"
if grep -q "RESPONSE RECEIVED\|HTTP.*200" "$LOG_FILE"; then
    echo "   ✅ API response received"
    grep "HTTP.*200\|RESPONSE RECEIVED" "$LOG_FILE" | head -1
elif grep -q "HTTP\|RESPONSE" "$LOG_FILE"; then
    echo "   ⚠️  API response (check status code)"
    grep "HTTP\|RESPONSE" "$LOG_FILE" | head -3
else
    echo "   ❌ No API communication - check network/auth"
fi
echo ""

echo "5️⃣  Completion:"
if grep -q "COMPLETE\|SUCCESS.*sync" "$LOG_FILE"; then
    echo "   ✅ Sync completed successfully"
    grep "COMPLETE\|SUCCESS" "$LOG_FILE" | tail -1
elif grep -q "FAILED\|Error\|Exception" "$LOG_FILE"; then
    echo "   ❌ Sync failed"
    grep "FAILED\|Error\|Exception" "$LOG_FILE" | head -3
else
    echo "   ❓ Completion status unknown"
fi
echo ""

echo "════════════════════════════════════════════════════════════════════════"
echo "NEXT STEPS:"
echo ""
echo "Review the full log:"
echo "  $ cat $LOG_FILE"
echo ""
echo "Or search for specific patterns:"
echo "  $ grep 'OsmandBackgroundSync\|OsmandLocationSync' $LOG_FILE"
echo "  $ grep 'Error\|Exception' $LOG_FILE"
echo "  $ grep 'HTTP\|RESPONSE' $LOG_FILE"
echo ""
echo "✅ Diagnostic complete!"

