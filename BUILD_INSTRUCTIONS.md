# Building F150 Monitor APK

## Prerequisites

1. **Install Android Studio**
   - Download from: https://developer.android.com/studio
   - Install version "Hedgehog" (2023.1.1) or newer
   - During installation, select "Android SDK" and "Android Virtual Device"

2. **Install Required SDK Components**
   - Open Android Studio
   - Go to: Tools → SDK Manager
   - Under "SDK Platforms", install:
     - Android 14.0 (API 34)
     - Android 13.0 (API 33)
   - Under "SDK Tools", ensure these are installed:
     - Android SDK Build-Tools 34.0.0
     - Android SDK Platform-Tools
     - Android SDK Command-line Tools

## Build Steps

### Option 1: Build with Android Studio (Recommended)

1. **Open the Project**
   ```
   - Launch Android Studio
   - Click "Open" (or File → Open)
   - Navigate to the F150Monitor folder
   - Click "OK"
   ```

2. **Wait for Gradle Sync**
   - Android Studio will automatically sync and download dependencies
   - This may take 5-10 minutes the first time
   - Watch the bottom status bar for "Gradle sync finished"

3. **Add Launcher Icons** (Optional but recommended)
   - You can use Android Studio's Image Asset tool:
   - Right-click on `app` → New → Image Asset
   - Select "Launcher Icons (Adaptive and Legacy)"
   - Choose an image or use the built-in icons
   - Click "Next" and "Finish"

4. **Build the APK**
   - Go to: Build → Build Bundle(s) / APK(s) → Build APK(s)
   - Wait for the build to complete (1-3 minutes)
   - A notification will appear: "APK(s) generated successfully"
   - Click "locate" to find the APK file
   - The APK will be at: `app/build/outputs/apk/debug/app-debug.apk`

5. **Install on Your Pixel**
   - Connect your Pixel 10 to your computer via USB
   - Enable "Developer Options" on your phone:
     - Go to Settings → About phone
     - Tap "Build number" 7 times
     - Go back to Settings → System → Developer options
     - Enable "USB debugging"
   - In Android Studio, click the "Run" button (green triangle)
   - Select your Pixel from the device list
   - The app will install and launch automatically

### Option 2: Build from Command Line

1. **Open Terminal/Command Prompt**
   - Navigate to the F150Monitor directory

2. **Build Debug APK**
   ```bash
   # On Windows:
   gradlew.bat assembleDebug
   
   # On Mac/Linux:
   ./gradlew assembleDebug
   ```

3. **Find the APK**
   - Location: `app/build/outputs/apk/debug/app-debug.apk`

4. **Install on Phone**
   ```bash
   # Make sure phone is connected with USB debugging enabled
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

## First-Time Setup on Your Pixel

1. **Pair OBDLink MX**
   - Turn on your truck's ignition
   - Go to: Settings → Connected devices → Pair new device
   - Select your OBDLink MX from the list
   - Complete pairing (no PIN usually required)

2. **Grant Permissions**
   - Open F150 Monitor app
   - Grant all requested permissions:
     - Bluetooth
     - Location (required for Bluetooth scanning)
     - Notifications
     - Background location (optional, for trip tracking)

3. **Configure OBD Device**
   - Open the app
   - Go to Settings tab
   - Select your OBDLink MX from the list

4. **Start Monitoring**
   - Go to Dashboard tab
   - Click "Start Monitoring"
   - The app will connect to your OBD adapter
   - Real-time data will begin appearing

## Troubleshooting

### Gradle Sync Failed
- Check your internet connection
- Try: File → Invalidate Caches → Invalidate and Restart

### Build Failed - SDK Not Found
- Open SDK Manager (Tools → SDK Manager)
- Install Android 14.0 (API 34)

### Cannot Install APK on Phone
- Enable "Install unknown apps" for your file manager
- Go to: Settings → Apps → Special app access → Install unknown apps
- Enable for "Files" or your browser

### OBD Connection Fails
- Ensure OBDLink MX is paired in Bluetooth settings
- Turn truck ignition to ON position
- Try restarting the app
- Check that no other app is using the OBD adapter

### Permissions Denied
- Go to: Settings → Apps → F150 Monitor → Permissions
- Enable all permissions manually

## Building Release APK (For Distribution)

1. **Generate Signing Key**
   ```bash
   keytool -genkey -v -keystore f150-release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias f150-key
   ```

2. **Configure Signing** in `app/build.gradle`:
   ```gradle
   android {
       signingConfigs {
           release {
               storeFile file("path/to/f150-release-key.jks")
               storePassword "your_store_password"
               keyAlias "f150-key"
               keyPassword "your_key_password"
           }
       }
       buildTypes {
           release {
               signingConfig signingConfigs.release
               ...
           }
       }
   }
   ```

3. **Build Release APK**
   ```bash
   ./gradlew assembleRelease
   ```

4. **APK Location**: `app/build/outputs/apk/release/app-release.apk`

## Notes

- The debug APK is perfect for personal use
- Only build a release APK if you plan to distribute to others
- Keep your signing key secure - you'll need it for updates
- The app requires Android 8.0 (API 26) or higher
