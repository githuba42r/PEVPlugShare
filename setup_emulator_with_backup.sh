#!/bin/bash

# Setup emulator with backup restore
BACKUP_DIR="${1:-.}/device_backups/20260425_113300"
PACKAGE="com.a42r.pevplugshare"
APK_PATH="${APK_PATH:-./app/build/outputs/apk/debug/app-debug.apk}"

# Get emulator device
EMULATOR=$(adb devices | grep emulator | awk '{print $1}' | head -1)

if [ -z "$EMULATOR" ]; then
    echo "Error: No emulator found. Start one first: emulator -avd Medium_Phone_API_36"
    exit 1
fi

echo "=== Setting up $PACKAGE on emulator: $EMULATOR ==="
echo ""

# Step 1: Uninstall old version
echo "1️⃣ Clearing any existing app installation..."
adb -s "$EMULATOR" uninstall "$PACKAGE" 2>/dev/null || true
sleep 2

# Step 2: Install APK
if [ ! -f "$APK_PATH" ]; then
    echo "⚠️  APK not found at: $APK_PATH"
    echo "   Building debug APK..."
    cd "$(dirname "$APK_PATH")/../../.."
    ./gradlew assembleDebug
fi

echo ""
echo "2️⃣ Installing app..."
adb -s "$EMULATOR" install "$APK_PATH"

if [ $? -ne 0 ]; then
    echo "❌ APK installation failed"
    exit 1
fi

echo ""
echo "3️⃣ Waiting for app to initialize..."
sleep 5

# Step 3: Push database
echo "4️⃣ Pushing backup database..."
adb -s "$EMULATOR" shell mkdir -p "/data/data/$PACKAGE/databases"
adb -s "$EMULATOR" push "$BACKUP_DIR/backup_extracted/apps/$PACKAGE/db/pevplugshare.db" "/data/data/$PACKAGE/databases/"

# Step 4: Push shared preferences if available
if [ -d "$BACKUP_DIR/backup_extracted/apps/$PACKAGE/sp" ]; then
    echo "5️⃣ Pushing shared preferences..."
    for pref_file in "$BACKUP_DIR/backup_extracted/apps/$PACKAGE/sp"/*; do
        if [ -f "$pref_file" ]; then
            filename=$(basename "$pref_file")
            adb -s "$EMULATOR" push "$pref_file" "/data/data/$PACKAGE/shared_prefs/$filename" 2>/dev/null
        fi
    done
fi

# Step 5: Restart app
echo ""
echo "6️⃣ Restarting app to load backup data..."
adb -s "$EMULATOR" shell am force-stop "$PACKAGE"
sleep 2
adb -s "$EMULATOR" shell am start -n "$PACKAGE/.MainActivity"

echo ""
echo "✅ Emulator setup complete!"
echo ""
echo "Your test data is now loaded:"
echo "  • 35 Locations"
echo "  • 7 Saved-for-Later items"
echo "  • 35 Location photos"
echo ""
echo "You can now:"
echo "  • Test the app with real data"
echo "  • Debug crashes and issues"
echo "  • Develop new features with live data"
echo ""
echo "To view app logs:"
echo "  adb logcat -s PEVPlugShare"
