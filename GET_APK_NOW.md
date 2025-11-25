# Get Your F150 Monitor APK in 5 Minutes

## Fastest Way: Use GitHub to Build (FREE)

GitHub will build your APK automatically - no software to install on your computer!

### Step 1: Create GitHub Account (if needed)
1. Go to https://github.com
2. Click "Sign up" 
3. Create free account

### Step 2: Create New Repository
1. Click the "+" icon (top right) → "New repository"
2. Name it: `F150Monitor`
3. Make it "Public" (required for free builds)
4. Click "Create repository"

### Step 3: Upload the Code
1. Click "uploading an existing file"
2. Drag and drop ALL files from the F150Monitor folder
3. Make sure to include ALL folders (app, gradle, .github)
4. Click "Commit changes"

### Step 4: Enable GitHub Actions
1. Click the "Actions" tab
2. Click "I understand my workflows, go ahead and enable them"
3. The build will start automatically!

### Step 5: Download Your APK
1. Wait 3-5 minutes for build to complete
2. Click on the green checkmark next to "Build F150 Monitor APK"
3. Scroll down to "Artifacts"
4. Click "F150Monitor-APK" to download
5. Extract the zip - your APK is inside!

### Step 6: Install on Your Pixel
1. Transfer `app-debug.apk` to your phone
2. Open Files app and tap the APK
3. Allow "Install unknown apps" when prompted
4. Tap "Install"

---

## Alternative: Use Online Build Service

### AppCenter (Microsoft)
1. Go to https://appcenter.ms
2. Sign up free with GitHub
3. Create new app (Android)
4. Connect your GitHub repo
5. It builds automatically

### Codemagic
1. Go to https://codemagic.io
2. Sign up free
3. Add your GitHub repo
4. Start build
5. Download APK

---

## Alternative: Build Locally with Android Studio

### Install Android Studio
1. Download from: https://developer.android.com/studio
2. Install (takes ~20 minutes)
3. Open F150Monitor folder
4. Wait for sync
5. Build → Build APK
6. Find APK in `app/build/outputs/apk/debug/`

---

## Troubleshooting

**GitHub build fails:**
- Make sure you uploaded ALL files including hidden `.github` folder
- Check that `app` folder structure is complete

**APK won't install:**
- Enable "Install unknown apps" in Settings → Apps → Special access
- Make sure you're using the debug APK, not a folder

**Need help?**
- Check the build logs in GitHub Actions tab
- Error messages tell you what's wrong

---

## What You'll Get

A fully functional Android app that:
- ✅ Connects to your OBDLink MX via Bluetooth
- ✅ Monitors real-time engine data
- ✅ Uses your Pixel's temperature sensor
- ✅ Provides 2006 F150 maintenance recommendations
- ✅ Alerts you to critical conditions
- ✅ Runs in background while driving

**Total time: 5-10 minutes** (mostly waiting for build)
