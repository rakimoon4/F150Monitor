#!/bin/bash

# F150 Monitor - One-Click APK Builder
# This script will build the APK on your computer

echo "=============================================="
echo "   F150 Monitor - APK Builder"
echo "=============================================="
echo ""

# Check if Android SDK is available
if [ -z "$ANDROID_HOME" ]; then
    echo "⚠️  Android SDK not found!"
    echo ""
    echo "Please install Android Studio first:"
    echo "https://developer.android.com/studio"
    echo ""
    echo "After installation, Android Studio will set up"
    echo "the SDK automatically."
    echo ""
    exit 1
fi

echo "✓ Android SDK found: $ANDROID_HOME"
echo ""

# Navigate to project
cd "$(dirname "$0")"

echo "Building APK..."
echo ""

# Make gradlew executable
chmod +x gradlew

# Build the APK
./gradlew assembleDebug

if [ $? -eq 0 ]; then
    echo ""
    echo "=============================================="
    echo "   ✓ BUILD SUCCESSFUL!"
    echo "=============================================="
    echo ""
    echo "Your APK is ready at:"
    echo "$(pwd)/app/build/outputs/apk/debug/app-debug.apk"
    echo ""
    echo "To install on your Pixel:"
    echo "1. Connect phone via USB"
    echo "2. Enable USB debugging on phone"
    echo "3. Run: adb install app/build/outputs/apk/debug/app-debug.apk"
    echo ""
    echo "Or just drag the APK file to your phone and tap it!"
    echo ""
else
    echo ""
    echo "=============================================="
    echo "   ✗ BUILD FAILED"
    echo "=============================================="
    echo ""
    echo "Common fixes:"
    echo "1. Make sure Android Studio is installed"
    echo "2. Open the project in Android Studio first"
    echo "3. Let it sync and download dependencies"
    echo "4. Then run this script"
    echo ""
fi
