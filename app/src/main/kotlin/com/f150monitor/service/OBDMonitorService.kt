package com.f150monitor.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.f150monitor.MainActivity
import com.f150monitor.data.*
import com.f150monitor.obd.OBDBluetoothManager
import com.f150monitor.obd.OBDCommands
import com.f150monitor.sensors.AmbientTempSensorManager
import com.f150monitor.utils.MaintenanceAnalyzer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.util.UUID

class OBDMonitorService : Service() {
    
    private val TAG = "OBDMonitorService"
    private val CHANNEL_ID = "obd_monitor_channel"
    private val NOTIFICATION_ID = 1
    
    private lateinit var obdManager: OBDBluetoothManager
    private lateinit var tempSensorManager: AmbientTempSensorManager
    private lateinit var database: F150Database
    private lateinit var maintenanceAnalyzer: MaintenanceAnalyzer
    
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var monitoringJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null
    
    private var currentTripId: String? = null
    private var tripStartTime: Long = 0
    
    companion object {
        const val ACTION_START_MONITORING = "START_MONITORING"
        const val ACTION_STOP_MONITORING = "STOP_MONITORING"
        const val EXTRA_DEVICE_ADDRESS = "device_address"
        
        private const val SCAN_INTERVAL_MS = 2000L // Scan every 2 seconds
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        
        obdManager = OBDBluetoothManager()
        tempSensorManager = AmbientTempSensorManager(this)
        database = F150Database.getDatabase(this)
        maintenanceAnalyzer = MaintenanceAnalyzer(
            database.obdReadingDao(),
            database.maintenanceEventDao(),
            database.alertDao()
        )
        
        createNotificationChannel()
        
        // Start ambient temperature monitoring
        tempSensorManager.startMonitoring()
        
        // Acquire wake lock to keep CPU running
        wakeLock = (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
            newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "F150Monitor::OBDWakeLock").apply {
                acquire(10*60*1000L /*10 minutes*/)
            }
        }
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_MONITORING -> {
                val deviceAddress = intent.getStringExtra(EXTRA_DEVICE_ADDRESS)
                if (deviceAddress != null) {
                    startMonitoring(deviceAddress)
                }
            }
            ACTION_STOP_MONITORING -> {
                stopMonitoring()
                stopSelf()
            }
        }
        
        return START_STICKY
    }
    
    private fun startMonitoring(deviceAddress: String) {
        Log.d(TAG, "Starting monitoring for device: $deviceAddress")
        
        val notification = createNotification("Connecting to OBD adapter...")
        startForeground(NOTIFICATION_ID, notification)
        
        monitoringJob = serviceScope.launch {
            try {
                // Connect to OBD adapter
                val connected = obdManager.connect(deviceAddress)
                
                if (connected) {
                    Log.d(TAG, "Connected to OBD adapter")
                    updateNotification("Monitoring F150...")
                    
                    // Start new trip
                    startNewTrip()
                    
                    // Begin monitoring loop
                    monitoringLoop()
                } else {
                    Log.e(TAG, "Failed to connect to OBD adapter")
                    createAlert(
                        AlertSeverity.WARNING,
                        AlertCategory.OTHER,
                        "OBD Connection Failed",
                        "Unable to connect to OBDLink MX. Check Bluetooth connection."
                    )
                    updateNotification("Connection failed")
                    delay(5000)
                    stopSelf()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in monitoring: ${e.message}", e)
                stopSelf()
            }
        }
    }
    
    private suspend fun monitoringLoop() = coroutineScope {
        while (isActive && obdManager.isConnected) {
            try {
                // Read all monitored PIDs
                val reading = readAllParameters()
                
                // Save reading to database
                database.obdReadingDao().insert(reading)
                
                // Analyze reading for alerts
                analyzeForAlerts(reading)
                
                // Update notification with key metrics
                updateNotification(
                    "Speed: ${reading.vehicleSpeed?.toInt() ?: "--"} MPH  " +
                    "Coolant: ${reading.coolantTemp?.toInt() ?: "--"}°F  " +
                    "Ambient: ${reading.ambientTemp?.toInt() ?: "--"}°F"
                )
                
                // Update trip summary
                updateTripSummary(reading)
                
                delay(SCAN_INTERVAL_MS)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error in monitoring loop: ${e.message}", e)
                delay(SCAN_INTERVAL_MS)
            }
        }
    }
    
    private suspend fun readAllParameters(): OBDReading {
        val pidList = listOf(
            OBDCommands.ENGINE_RPM,
            OBDCommands.VEHICLE_SPEED,
            OBDCommands.COOLANT_TEMP,
            OBDCommands.ENGINE_LOAD,
            OBDCommands.THROTTLE_POSITION,
            OBDCommands.INTAKE_TEMP,
            OBDCommands.MAF_RATE,
            OBDCommands.SHORT_TERM_FUEL_TRIM_BANK1,
            OBDCommands.LONG_TERM_FUEL_TRIM_BANK1,
            OBDCommands.CONTROL_MODULE_VOLTAGE
        )
        
        val values = mutableMapOf<String, Double?>()
        
        // Read each PID
        pidList.forEach { pid ->
            val response = obdManager.readPID(pid)
            if (response.success) {
                values[pid] = response.value
            }
        }
        
        // Get ambient temperature from phone sensor
        val ambientTemp = tempSensorManager.ambientTemp.value
        val phoneOverheating = tempSensorManager.phoneOverheating.value
        
        return OBDReading(
            timestamp = System.currentTimeMillis(),
            engineRpm = values[OBDCommands.ENGINE_RPM],
            vehicleSpeed = values[OBDCommands.VEHICLE_SPEED],
            coolantTemp = values[OBDCommands.COOLANT_TEMP],
            engineLoad = values[OBDCommands.ENGINE_LOAD],
            throttlePosition = values[OBDCommands.THROTTLE_POSITION],
            intakeTemp = values[OBDCommands.INTAKE_TEMP],
            mafRate = values[OBDCommands.MAF_RATE],
            shortFuelTrimBank1 = values[OBDCommands.SHORT_TERM_FUEL_TRIM_BANK1],
            longFuelTrimBank1 = values[OBDCommands.LONG_TERM_FUEL_TRIM_BANK1],
            batteryVoltage = values[OBDCommands.CONTROL_MODULE_VOLTAGE],
            ambientTemp = ambientTemp,
            phoneOverheating = phoneOverheating,
            tripId = currentTripId
        )
    }
    
    private suspend fun analyzeForAlerts(reading: OBDReading) {
        // Critical coolant temperature
        reading.coolantTemp?.let { temp ->
            if (temp > 230.0) {
                createAlert(
                    AlertSeverity.CRITICAL,
                    AlertCategory.COOLANT,
                    "CRITICAL: Coolant Temperature",
                    "Coolant temperature is ${temp.toInt()}°F. Pull over safely and turn off engine!"
                )
            } else if (temp > 220.0) {
                createAlert(
                    AlertSeverity.WARNING,
                    AlertCategory.COOLANT,
                    "High Coolant Temperature",
                    "Coolant temperature is ${temp.toInt()}°F. Monitor closely."
                )
            }
        }
        
        // Low battery voltage
        reading.batteryVoltage?.let { voltage ->
            if (voltage < 12.0) {
                createAlert(
                    AlertSeverity.CRITICAL,
                    AlertCategory.ELECTRICAL,
                    "Critical Battery Voltage",
                    "Battery voltage is ${"%.1f".format(voltage)}V. Charging system failure!"
                )
            } else if (voltage < 12.5) {
                createAlert(
                    AlertSeverity.WARNING,
                    AlertCategory.ELECTRICAL,
                    "Low Battery Voltage",
                    "Battery voltage is ${"%.1f".format(voltage)}V. Check battery and alternator."
                )
            }
        }
        
        // Phone overheating
        if (reading.phoneOverheating) {
            createAlert(
                AlertSeverity.WARNING,
                AlertCategory.PHONE,
                "Phone Overheating",
                "Ambient temperature is ${reading.ambientTemp?.toInt()}°F. Move phone to cooler location."
            )
        }
        
        // Extreme fuel trim
        reading.longFuelTrimBank1?.let { trim ->
            if (trim > 20.0 || trim < -20.0) {
                createAlert(
                    AlertSeverity.WARNING,
                    AlertCategory.FUEL_SYSTEM,
                    "Extreme Fuel Trim",
                    "Long-term fuel trim is ${trim.toInt()}%. Engine compensation issue detected."
                )
            }
        }
        
        // Ambient temperature correlation
        if (reading.ambientTemp != null && reading.coolantTemp != null) {
            val analysis = tempSensorManager.analyzeTempDifferential(
                reading.ambientTemp,
                reading.coolantTemp.toFloat()
            )
            
            if (!analysis.isNormal && analysis.concern != null) {
                createAlert(
                    AlertSeverity.WARNING,
                    AlertCategory.COOLANT,
                    "Temperature Correlation Issue",
                    analysis.concern
                )
            }
        }
    }
    
    private suspend fun createAlert(
        severity: AlertSeverity,
        category: AlertCategory,
        title: String,
        message: String
    ) {
        // Check if similar alert exists recently (avoid spam)
        val recentAlerts = database.alertDao().getRecentAlerts(20).first()
        val similarAlert = recentAlerts.find { alert ->
            alert.title == title && 
            System.currentTimeMillis() - alert.timestamp < 300000 // 5 minutes
        }
        
        if (similarAlert == null) {
            val alert = Alert(
                severity = severity,
                category = category,
                title = title,
                message = message
            )
            database.alertDao().insert(alert)
            
            // Show notification for critical/warning alerts
            if (severity == AlertSeverity.CRITICAL || severity == AlertSeverity.WARNING) {
                showAlertNotification(title, message, severity)
            }
        }
    }
    
    private fun startNewTrip() {
        currentTripId = UUID.randomUUID().toString()
        tripStartTime = System.currentTimeMillis()
        
        serviceScope.launch {
            val trip = TripSummary(
                tripId = currentTripId!!,
                startTime = tripStartTime
            )
            database.tripSummaryDao().insert(trip)
        }
    }
    
    private suspend fun updateTripSummary(reading: OBDReading) {
        currentTripId?.let { tripId ->
            val trip = database.tripSummaryDao().getTripById(tripId)
            trip?.let {
                // Update trip statistics
                // This is simplified - full implementation would track all metrics
                val updated = it.copy(
                    endTime = System.currentTimeMillis()
                )
                database.tripSummaryDao().update(updated)
            }
        }
    }
    
    private fun stopMonitoring() {
        Log.d(TAG, "Stopping monitoring")
        monitoringJob?.cancel()
        obdManager.disconnect()
        tempSensorManager.stopMonitoring()
        
        // End current trip
        serviceScope.launch {
            currentTripId?.let { tripId ->
                val trip = database.tripSummaryDao().getTripById(tripId)
                trip?.let {
                    database.tripSummaryDao().update(
                        it.copy(endTime = System.currentTimeMillis())
                    )
                }
            }
        }
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "OBD Monitoring"
            val descriptionText = "Foreground service for OBD monitoring"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(contentText: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("F150 Monitor")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
    
    private fun updateNotification(contentText: String) {
        val notification = createNotification(contentText)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    private fun showAlertNotification(title: String, message: String, severity: AlertSeverity) {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )
        
        val priority = when (severity) {
            AlertSeverity.CRITICAL -> NotificationCompat.PRIORITY_HIGH
            AlertSeverity.WARNING -> NotificationCompat.PRIORITY_DEFAULT
            else -> NotificationCompat.PRIORITY_LOW
        }
        
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentIntent(pendingIntent)
            .setPriority(priority)
            .setAutoCancel(true)
            .build()
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed")
        stopMonitoring()
        tempSensorManager.stopMonitoring()
        serviceScope.cancel()
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
        obdManager.cleanup()
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
}
