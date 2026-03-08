#!/bin/bash
# Helper script to change keystore passwords
# Run this script to set secure passwords for your keystore

set -e

KEYSTORE_FILE="pevplugshare-release.jks"
KEY_ALIAS="pevplugshare"
OLD_STORE_PASS="temp_password_CHANGE_ME"
OLD_KEY_PASS="temp_password_CHANGE_ME"

echo "=== PEV PlugShare Keystore Password Changer ==="
echo ""
echo "This script will help you change the keystore passwords from the"
echo "temporary passwords to your secure passwords."
echo ""
echo "IMPORTANT: Write down your new passwords in a secure location!"
echo "           You will need them for every release."
echo ""

# Check if keystore exists
if [ ! -f "$KEYSTORE_FILE" ]; then
    echo "ERROR: $KEYSTORE_FILE not found!"
    exit 1
fi

# Change keystore password
echo "Step 1: Change keystore password"
echo "You will be prompted for:"
echo "  - Old password (current: temp_password_CHANGE_ME)"
echo "  - New password (your secure password)"
echo ""
keytool -storepasswd -keystore "$KEYSTORE_FILE"

if [ $? -ne 0 ]; then
    echo "ERROR: Failed to change keystore password"
    exit 1
fi

echo ""
echo "✓ Keystore password changed successfully!"
echo ""

# Change key password
echo "Step 2: Change key password"
echo "You will be prompted for:"
echo "  - Keystore password (your new password from step 1)"
echo "  - Old key password (current: temp_password_CHANGE_ME)"
echo "  - New key password (can be same as keystore password)"
echo ""
keytool -keypasswd -alias "$KEY_ALIAS" -keystore "$KEYSTORE_FILE"

if [ $? -ne 0 ]; then
    echo "ERROR: Failed to change key password"
    exit 1
fi

echo ""
echo "✓ Key password changed successfully!"
echo ""
echo "=== Next Steps ==="
echo ""
echo "1. Edit keystore.properties and update the passwords:"
echo "   nano keystore.properties"
echo ""
echo "2. Replace 'temp_password_CHANGE_ME' with your new passwords"
echo ""
echo "3. BACKUP the following files to a secure location:"
echo "   - pevplugshare-release.jks"
echo "   - keystore.properties"
echo ""
echo "4. Test the build:"
echo "   ./gradlew bundleRelease"
echo ""
