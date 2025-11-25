#!/bin/bash

echo "========================================"
echo "   F150 Monitor - Build Script"
echo "========================================"
echo ""

echo "Building APK..."
echo ""

./gradlew assembleDebug

if [ $? -eq 0 ]; then
    echo ""
    echo "========================================"
    echo "   BUILD SUCCESSFUL!"
    echo "========================================"
    echo ""
    echo "APK Location:"
    echo "app/build/outputs/apk/debug/app-debug.apk"
    echo ""
    echo "To install on your phone:"
    echo "1. Connect phone via USB with USB debugging enabled"
    echo "2. Run: adb install app/build/outputs/apk/debug/app-debug.apk"
    echo ""
else
    echo ""
    echo "========================================"
    echo "   BUILD FAILED!"
    echo "========================================"
    echo ""
    echo "Check the error messages above."
    echo "Common issues:"
    echo "- Android SDK not installed"
    echo "- Java not in PATH"
    echo "- Internet connection required for first build"
    echo ""
fi
