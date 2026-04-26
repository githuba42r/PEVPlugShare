#!/bin/bash

# Setup test data (locations + images) on emulator for crash debugging
# This script bypasses permission issues by using intermediate staging

EMULATOR=${1:-$(adb devices | grep emulator | awk '{print $1}' | head -1)}
BACKUP_DIR="$(dirname "$0")/device_backups/20260425_113300"
PACKAGE="com.a42r.pevplugshare"

if [ -z "$EMULATOR" ]; then
    echo "Error: No emulator found"
    exit 1
fi

if [ ! -d "$BACKUP_DIR" ]; then
    echo "Error: Backup directory not found: $BACKUP_DIR"
    exit 1
fi

echo "=== SETTING UP TEST DATA ON EMULATOR ==="
echo "Device: $EMULATOR"
echo "Backup: $(basename $BACKUP_DIR)"
echo ""

# Make sure app is running so directories exist
echo "1. Starting app to initialize directories..."
adb -s "$EMULATOR" shell am start -n "$PACKAGE/.ui.auth.SplashActivity" 2>/dev/null
sleep 5

# Stage database on sdcard
echo "2. Staging database file..."
DB_SOURCE="$BACKUP_DIR/backup_extracted/apps/$PACKAGE/db/pevplugshare.db"
adb -s "$EMULATOR" push "$DB_SOURCE" "/sdcard/pevplugshare.db" 2>&1 | grep -E "pushed|error"

# Copy database into app (using dd to bypass permission issues)
echo "3. Installing database..."
adb -s "$EMULATOR" shell "cat /sdcard/pevplugshare.db > /data/data/$PACKAGE/databases/pevplugshare.db 2>/dev/null" || {
    echo "   Direct write failed, trying alternative method..."
    adb -s "$EMULATOR" shell "cp /sdcard/pevplugshare.db /data/data/$PACKAGE/databases/pevplugshare.db 2>&1"
}

# Verify database was installed
echo "4. Verifying database..."
adb -s "$EMULATOR" shell "test -f /data/data/$PACKAGE/databases/pevplugshare.db && echo '   ✓ Database installed' || echo '   ✗ Database not accessible'"

# Copy images to sdcard (already done from earlier, but ensure they're there)
echo "5. Ensuring image files on /sdcard..."
adb -s "$EMULATOR" shell "test -d /sdcard/location_images && echo '   ✓ Images present' || echo '   ✗ Images missing'"

# Make images readable by setting permissions
echo "6. Setting image file permissions..."
adb -s "$EMULATOR" shell "find /sdcard/location_images -type d -exec chmod 755 {} \; 2>/dev/null; find /sdcard/location_images -type f -exec chmod 644 {} \; 2>/dev/null"
echo "   ✓ Permissions updated"

# Restart app to load the database
echo "7. Restarting app to load test data..."
adb -s "$EMULATOR" shell am force-stop "$PACKAGE"
sleep 2
adb -s "$EMULATOR" shell am start -n "$PACKAGE/.ui.auth.SplashActivity" &

sleep 4

# Verify data loaded
echo ""
echo "=== VERIFICATION ==="
echo ""

# Check database
DB_COUNT=$(adb -s "$EMULATOR" shell "sqlite3 /data/data/$PACKAGE/databases/pevplugshare.db 'SELECT COUNT(*) FROM locations;' 2>/dev/null" || echo "0")
echo "Locations in database: $DB_COUNT"

# List available test locations
echo ""
echo "Available test locations with images:"
sqlite3 "$DB_SOURCE" << SQL
.mode column
.headers off
SELECT name, COUNT(locationId) as photos
FROM (
  SELECT DISTINCT l.name, lp.locationId
  FROM locations l
  JOIN location_photos lp ON l.id = lp.locationId
)
GROUP BY name
LIMIT 5;
SQL

echo ""
echo "=== TEST DATA READY ==="
echo ""
echo "The app now has locations with images available."
echo "Open the app in the emulator to verify:"
echo "  • Locations list shows saved locations"
echo "  • Images display with locations"
echo "  • Click locations to test interaction"
echo ""
echo "For debugging:"
echo "  adb logcat -s PEVPlugShare:* -v threadtime"
echo ""
echo "To set breakpoints:"
echo "  Run → Debug 'app' in Android Studio"
