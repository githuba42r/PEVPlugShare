#!/bin/bash

# Test file migration on emulator
EMULATOR=$(adb devices | grep emulator | awk '{print $1}' | head -1)
PACKAGE="com.a42r.pevplugshare"
PROJECT_ROOT=$(dirname "$0")

if [ -z "$EMULATOR" ]; then
    echo "Error: No emulator running"
    exit 1
fi

echo "=== Testing File Migration ==="
echo "Emulator: $EMULATOR"
echo ""

# Step 1: Uninstall old version
echo "1. Uninstalling old version..."
adb -s "$EMULATOR" uninstall "$PACKAGE" 2>/dev/null || true
sleep 2

# Step 2: Install new APK
APK_PATH="$PROJECT_ROOT/app/build/outputs/apk/debug/app-debug.apk"
if [ ! -f "$APK_PATH" ]; then
    echo "Error: APK not found at $APK_PATH"
    exit 1
fi

echo "2. Installing updated APK..."
adb -s "$EMULATOR" install "$APK_PATH"

# Step 3: The images should still be on /sdcard from earlier
echo ""
echo "3. Verifying backup images on /sdcard..."
IMAGE_COUNT=$(adb -s "$EMULATOR" shell find /sdcard/location_images -type f -iname "*.jpg" 2>/dev/null | wc -l)
echo "   Found $IMAGE_COUNT images on /sdcard"

# Step 4: Launch app (migration runs on startup)
echo ""
echo "4. Launching app (migration will run automatically)..."
adb -s "$EMULATOR" shell am start -n "$PACKAGE/.MainActivity"

sleep 5

# Step 5: Check if files were migrated
echo ""
echo "5. Checking migrated files..."
MIGRATED_COUNT=$(adb -s "$EMULATOR" shell find /data/data/$PACKAGE/files/location_images -type f -iname "*.jpg" 2>/dev/null | wc -l)
echo "   Found $MIGRATED_COUNT images in app data directory"

# Step 6: Check logs
echo ""
echo "6. Checking app logs for migration status..."
adb -s "$EMULATOR" logcat -d | grep "FileMigrationHelper" | tail -5

echo ""
if [ "$MIGRATED_COUNT" -gt 0 ]; then
    echo "✅ FILE MIGRATION SUCCESSFUL!"
    echo "   Images are now accessible to the app"
    echo "   Locations should display images properly"
else
    echo "⚠️  Migration may still be in progress"
    echo "   Wait a few seconds and restart the app to trigger it"
fi

echo ""
echo "Next: Open the app in the emulator and check if locations display images"
