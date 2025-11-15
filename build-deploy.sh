#!/bin/bash

set -e

echo "Building APK..."
./gradlew assembleDebug

APK_PATH="app/build/outputs/apk/debug/app-debug.apk"

if [ -f "$APK_PATH" ]; then
    echo "APK built successfully. Deploying to device..."
    adb install -r "$APK_PATH"
    echo "Deployment complete."
else
    echo "APK not found! Build may have failed."
    exit 1
fi