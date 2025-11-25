# F150 Monitor - Quick Start Guide

## 🎯 Goal
Get your 2006 F150 monitored with intelligent maintenance recommendations in under 10 minutes.

## ⚡ Fast Track Setup

### Step 1: Build the App (5 minutes)
1. Install Android Studio from https://developer.android.com/studio
2. Open the F150Monitor folder in Android Studio
3. Wait for Gradle sync to complete
4. Click Build → Build Bundle(s) / APK(s) → Build APK(s)
5. Find APK at: `app/build/outputs/apk/debug/app-debug.apk`

### Step 2: Install on Your Pixel (2 minutes)
1. Enable USB debugging on your Pixel:
   - Settings → About phone → Tap "Build number" 7 times
   - Settings → System → Developer options → Enable "USB debugging"
2. Connect phone to computer
3. In Android Studio, click Run (green triangle)
4. Select your Pixel from the list

### Step 3: First Use (3 minutes)
1. **In your truck with ignition ON:**
   - Go to Settings → Connected devices → Pair new device
   - Select "OBDLink MX" and pair it

2. **Open F150 Monitor app:**
   - Grant all permissions (Bluetooth, Location, Notifications)
   - Go to Settings tab
   - Tap your OBDLink MX device

3. **Start Monitoring:**
   - Go to Dashboard tab
   - Tap "Start Monitoring"
   - Wait 10 seconds for connection
   - You'll see live data appear!

## 📱 Daily Use

### When You Start Driving:
1. Open F150 Monitor
2. Tap "Start Monitoring" on Dashboard
3. Let it run in background (you'll see persistent notification)
4. Drive normally - data logs automatically

### Check Maintenance Needs:
- Open app anytime
- Go to Maintenance tab
- See prioritized recommendations with costs and reasoning

### Critical Alerts:
- App will notify you immediately for:
  - High coolant temp (>230°F)
  - Low battery voltage
  - Phone overheating
  - Fuel system issues

## 💡 Pro Tips

1. **Keep the App Running**: Start it at the beginning of each drive for best results
2. **Check Maintenance Weekly**: Review recommendations regularly
3. **Watch for Patterns**: The app learns your severe duty conditions
4. **Phone Placement**: Put phone where you can see alerts but not in direct sun
5. **Battery Optimization**: Disable battery optimization for F150 Monitor

## 🔧 What the App Tells You

### Immediate Benefits:
- Real-time engine vitals (coolant temp, RPM, speed)
- Battery health monitoring
- Fuel system efficiency

### Within One Week:
- Oil change recommendations based on YOUR driving
- Fuel trim analysis (detects vacuum leaks, MAF issues)
- Driving pattern analysis

### Ongoing:
- Maintenance interval tracking
- Historical trend analysis
- Cost-effective service scheduling

## ⚠️ Important Alerts You'll Get

**CRITICAL** (Take immediate action):
- Coolant temp >230°F → Pull over, turn off engine
- Battery voltage <12.0V → Charging system failure
- Extreme fuel trim → Major engine issue

**WARNING** (Address soon):
- Coolant temp 220-230°F → Monitor closely
- Battery voltage 12.0-12.5V → Weak battery/alternator
- Fuel trim ±20% → Needs diagnosis

## 📊 Understanding Your Data

### Normal Ranges (2006 F150):
- **Coolant Temp**: 190-210°F (normal operating)
- **Battery Voltage**: 13.5-14.5V (when running)
- **Fuel Trim**: ±5% (healthy engine)
- **Engine Load**: 20-40% (cruising), 60-80% (acceleration)

### Concern Thresholds:
- **Coolant >220°F**: Cooling system issue likely
- **Battery <12.5V**: Battery/alternator problem
- **Fuel Trim >15%**: Vacuum leak or fuel delivery issue
- **Fuel Trim <-15%**: Rich condition, sensor issue

## 🛠️ What Each Maintenance Priority Means

**CRITICAL**: Do this NOW or risk breakdown
- Example: High coolant temps with possible thermostat failure

**HIGH**: Schedule within 1-2 weeks
- Example: Oil change overdue or severe duty detected

**MEDIUM**: Schedule within 1 month
- Example: Coolant flush due, fuel trim slightly off

**LOW**: Keep on radar, normal maintenance
- Example: O2 sensor monitoring, MAF cleaning suggestion

## 🌡️ Pixel Temperature Sensor Magic

Your Pixel's ambient sensor gives you unique advantages:

- **Phone Safety**: Warns before phone overheats in hot truck
- **Validation**: Confirms coolant readings make sense
- **Seasonal Intelligence**: Adjusts expectations for weather
- **Engine Bay Heat Detection**: Spots excessive heat early

Normal differential: Coolant should be 20-200°F above ambient.
- Too low? Thermostat stuck open.
- Too high? Cooling system problem.

## 🚫 Common Mistakes to Avoid

1. ❌ Not pairing OBDLink first → App won't find it
2. ❌ Denying Location permission → Bluetooth scanning fails
3. ❌ Turning off phone's Bluetooth → Connection lost
4. ❌ Other OBD apps running → Conflicts with connection
5. ❌ Not waiting for connection → Give it 10-15 seconds
6. ❌ Phone in direct sunlight → False temperature readings

## 📞 Quick Troubleshooting

**"Can't connect to OBD adapter"**
- Is truck ignition ON?
- Is OBDLink paired in Bluetooth settings?
- Close other OBD apps
- Restart Bluetooth

**"No data showing"**
- Wait 15 seconds after connecting
- Check OBDLink is fully inserted in OBD port
- Try unplugging and replugging OBDLink

**"Battery draining fast"**
- Normal - background monitoring uses power
- Only run during actual driving
- Consider car charger for long trips

**"Permission denied"**
- Go to Settings → Apps → F150 Monitor → Permissions
- Enable all permissions manually

## 🎓 Learning More

For detailed information:
- Read README.md for full feature list
- Check BUILD_INSTRUCTIONS.md for building details
- Review code comments for technical understanding

## 💪 Your 2006 F150 Deserves This

At 19 years old, your truck needs attention to:
- Aging sensors (O2, MAF)
- Thermostat failures (very common)
- Transmission fluid (CRITICAL for longevity)
- Cooling system corrosion
- Battery and charging system

This app helps you stay ahead of these issues with data-driven maintenance.

**Keep that F150 running strong! 🛻**
