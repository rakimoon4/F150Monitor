# F150 Monitor - Intelligent OBD Maintenance App

A comprehensive Android app for monitoring and maintaining your 2006 Ford F150 using your OBDLink MX Bluetooth adapter. This app provides real-time diagnostics, intelligent maintenance recommendations, and leverages your Pixel phone's ambient temperature sensor for enhanced analysis.

## Features

### 🚗 Real-Time Monitoring
- **Live OBD-II Data**: Continuous monitoring of critical engine parameters
  - Engine RPM and vehicle speed
  - Coolant temperature with critical alerts
  - Engine load and throttle position
  - Mass Air Flow (MAF) sensor readings
  - Fuel trim values (short and long term)
  - Battery voltage monitoring
  - Intake air temperature

### 🌡️ Smart Temperature Analysis
- **Pixel Ambient Sensor Integration**: Uses your phone's temperature sensor to:
  - Detect phone overheating risk in hot vehicle conditions
  - Correlate ambient temperature with engine bay temps
  - Validate coolant temperature readings
  - Provide season-aware performance insights
  - Alert when phone temperature is critical

### 🔧 Intelligent Maintenance Recommendations
Analyzes your driving patterns and OBD data to provide **2006 F150-specific** maintenance guidance:

- **Oil Change Intelligence**
  - Detects severe duty conditions (towing, short trips, idling)
  - Adjusts oil change intervals based on actual driving (3,000 or 5,000 miles)
  - Time-based recommendations (every 90 days)

- **Coolant System Analysis**
  - Monitors for high coolant temperatures
  - Detects thermostat failures (common in 2006 F150s)
  - Tracks coolant flush intervals (2 years/30,000 miles)

- **Fuel System Health**
  - Analyzes fuel trim for vacuum leaks or fuel delivery issues
  - Detects MAF sensor degradation
  - Identifies rich/lean running conditions

- **Transmission Monitoring**
  - Critical 4R75E/4R75W transmission fluid service reminders
  - Recognizes importance of regular fluid changes for transmission longevity

- **Sensor Health**
  - O2 sensor lifespan tracking (critical at 19 years old)
  - Battery and charging system monitoring
  - MAF sensor cleaning recommendations

### 📊 Background Monitoring
- **Foreground Service**: Continuously monitors while driving
- **Smart Alerts**: Real-time notifications for critical conditions
  - Critical coolant temperature (>230°F)
  - Low battery voltage
  - Phone overheating
  - Extreme fuel trim values
- **Automatic Trip Tracking**: Records all driving sessions

### 📱 User Interface
- **Dashboard**: Real-time data display with color-coded warnings
- **Maintenance Tab**: Prioritized maintenance recommendations with cost estimates
- **Alerts Tab**: Historical alerts and warnings
- **Settings**: Easy OBD adapter selection from paired Bluetooth devices

## Why This App is Valuable for Your 2006 F150

### Common 2006 F150 Issues Detected
1. **Thermostat Failures**: Very common - app detects stuck open/closed thermostats
2. **O2 Sensor Degradation**: At 19 years old, original sensors are past lifespan
3. **Fuel Trim Issues**: Detects vacuum leaks and MAF sensor problems
4. **Transmission Health**: Critical monitoring for known transmission longevity needs
5. **Coolant System**: Tracks corrosion-prone cooling system health

### Driving Condition Analysis
The app understands that not all miles are equal:
- **Severe Duty Detection**: Identifies towing, short trips, frequent idling
- **Adjusted Intervals**: Shortens maintenance intervals for harsh conditions
- **Real-Time Alerts**: Warns before problems become failures

### Cost Savings
- **Preventive Maintenance**: Catches issues early
- **Avoid Overdue Service**: Reminds you based on actual conditions
- **Smart Oil Changes**: Doesn't waste money on premature changes
- **Data-Driven Decisions**: Know what your truck actually needs

## Technical Specifications

### Requirements
- **Android Version**: Android 8.0 (API 26) or higher
- **Phone**: Works on any Android phone (optimized for Pixel with temp sensor)
- **OBD Adapter**: OBDLink MX (Bluetooth)
- **Vehicle**: 2006 Ford F150 (adaptable to other vehicles)

### Monitored Parameters
- Engine RPM (PID 010C)
- Vehicle Speed (PID 010D)
- Coolant Temperature (PID 0105)
- Engine Load (PID 0104)
- Throttle Position (PID 0111)
- Intake Air Temperature (PID 010F)
- MAF Rate (PID 0110)
- Short/Long Term Fuel Trim Bank 1 & 2 (PIDs 0106-0109)
- Battery Voltage (PID 0142)
- Diagnostic Trouble Codes (Mode 03)

### Database
- **Room Database**: Stores all readings locally
- **Historical Data**: Access past trips and trends
- **Maintenance Records**: Track service history
- **Alert History**: Review past warnings

## Usage Instructions

### Initial Setup
1. **Pair OBDLink MX**
   - Turn on truck ignition
   - Pair OBDLink MX in Android Bluetooth settings

2. **Open F150 Monitor**
   - Grant all requested permissions
   - Go to Settings tab
   - Select your OBDLink MX

3. **Start Monitoring**
   - Return to Dashboard
   - Tap "Start Monitoring"
   - Wait for connection (5-10 seconds)

### During Driving
- App runs in background with persistent notification
- Critical alerts appear immediately
- Data logged every 2 seconds
- Phone temperature monitored continuously

### Reviewing Maintenance
- Check Maintenance tab regularly for recommendations
- Each recommendation includes:
  - Priority level (Critical/High/Medium/Low)
  - Detailed description
  - Technical reasoning
  - Estimated cost range

### Understanding Alerts
- **CRITICAL (Red)**: Immediate action required - pull over safely
- **WARNING (Orange)**: Monitor closely, address soon
- **INFO (Blue)**: Informational, no immediate concern

## Phone Temperature Sensor Benefits

Your Pixel's ambient temperature sensor provides unique capabilities:

1. **Phone Safety**: Alerts before phone overheats and shuts down
2. **Temperature Correlation**: Validates coolant temp readings
3. **Seasonal Baselines**: Understands performance changes with weather
4. **Engine Bay Heat**: Detects excessive heat that could indicate cooling issues

## Maintenance Philosophy

This app follows Ford's severe duty schedule because:
- Massachusetts has cold winters (hard on batteries, oil)
- Construction work often involves:
  - Short trips that don't fully warm engine
  - Extended idling at job sites
  - Towing trailers and equipment
  - Heavy loads (lumber, materials)

## Privacy & Data

- **All data stored locally** on your phone
- **No cloud sync** or external servers
- **No telemetry** or usage tracking
- **Complete control** over your data

## Support

### Troubleshooting

**Connection Issues:**
- Ensure OBDLink MX is paired
- Truck ignition must be ON
- No other apps using OBD adapter
- Try restarting Bluetooth

**No Data Appearing:**
- Wait 10-15 seconds after connecting
- Check that OBDLink is properly seated in OBD port
- Verify truck is 2006 or newer (OBD-II standard)

**Permissions Denied:**
- Go to: Settings → Apps → F150 Monitor → Permissions
- Enable all permissions manually
- Location is required for Bluetooth scanning

**High Battery Usage:**
- Background monitoring uses power
- Consider only running during actual trips
- App uses partial wake lock to maintain connection

## Future Enhancements

Possible additions:
- Trip statistics and fuel economy calculation
- Historical charts and graphs
- Export data to CSV
- Multiple vehicle profiles
- Custom alert thresholds
- Transmission temperature monitoring (with extended PIDs)

## Technical Notes

### ELM327 Protocol
- Uses standard ELM327 command set
- Compatible with most OBD-II adapters
- Optimized for OBDLink MX performance

### Battery Optimization
- Uses Android's foreground service properly
- Requests battery optimization exemption
- Partial wake lock for reliable Bluetooth

### Thread Safety
- Kotlin coroutines for all async operations
- Background service on separate process
- Room database with coroutine support

## Credits

Built specifically for the 2006 Ford F150 with consideration for:
- 5.4L Triton V8 characteristics
- 4R75E/4R75W transmission requirements
- Known issues and failure points
- Massachusetts climate conditions
- Construction work usage patterns

## License

Personal use for your 2006 F150. Feel free to modify for your needs.

---

**Remember**: This app is a diagnostic tool and maintenance reminder. It does NOT replace:
- Professional mechanic inspections
- Manufacturer service intervals
- Warning lights and gauges
- Your own mechanical knowledge and judgment

**Stay safe and keep that F150 running strong! 🛻**
