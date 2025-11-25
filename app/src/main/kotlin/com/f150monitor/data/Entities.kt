package com.f150monitor.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Entity(tableName = "obd_readings")
@TypeConverters(Converters::class)
data class OBDReading(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    
    // Engine parameters
    val engineRpm: Double? = null,
    val vehicleSpeed: Double? = null,
    val coolantTemp: Double? = null,
    val engineLoad: Double? = null,
    val throttlePosition: Double? = null,
    val intakeTemp: Double? = null,
    val mafRate: Double? = null,
    
    // Fuel system
    val shortFuelTrimBank1: Double? = null,
    val longFuelTrimBank1: Double? = null,
    val shortFuelTrimBank2: Double? = null,
    val longFuelTrimBank2: Double? = null,
    
    // Electrical
    val batteryVoltage: Double? = null,
    
    // Ambient sensor data from phone
    val ambientTemp: Float? = null,
    val phoneOverheating: Boolean = false,
    
    // Runtime
    val runtimeSinceStart: Int? = null,
    
    // Trip tracking
    val tripId: String? = null
)

@Entity(tableName = "maintenance_events")
data class MaintenanceEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val mileage: Int? = null,
    val eventType: MaintenanceType,
    val description: String,
    val notes: String? = null,
    val cost: Double? = null,
    val nextDueDate: Long? = null,
    val nextDueMileage: Int? = null,
    val completed: Boolean = false
)

enum class MaintenanceType {
    OIL_CHANGE,
    TRANSMISSION_FLUID,
    COOLANT_FLUSH,
    AIR_FILTER,
    SPARK_PLUGS,
    O2_SENSOR,
    MAF_CLEANING,
    BATTERY,
    TIRES,
    BRAKES,
    OTHER
}

@Entity(tableName = "alerts")
data class Alert(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val severity: AlertSeverity,
    val category: AlertCategory,
    val title: String,
    val message: String,
    val acknowledged: Boolean = false,
    val relatedPid: String? = null,
    val value: Double? = null
)

enum class AlertSeverity {
    INFO,
    WARNING,
    CRITICAL
}

enum class AlertCategory {
    COOLANT,
    ENGINE,
    FUEL_SYSTEM,
    ELECTRICAL,
    SENSORS,
    MAINTENANCE,
    PHONE,
    OTHER
}

@Entity(tableName = "diagnostic_codes")
data class DiagnosticCode(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val code: String,
    val description: String,
    val severity: AlertSeverity,
    val cleared: Boolean = false,
    val clearedTimestamp: Long? = null
)

@Entity(tableName = "trip_summary")
data class TripSummary(
    @PrimaryKey
    val tripId: String,
    val startTime: Long,
    val endTime: Long? = null,
    val startMileage: Int? = null,
    val endMileage: Int? = null,
    val distance: Double? = null,
    val avgSpeed: Double? = null,
    val avgRpm: Double? = null,
    val maxSpeed: Double? = null,
    val maxRpm: Double? = null,
    val avgCoolantTemp: Double? = null,
    val maxCoolantTemp: Double? = null,
    val avgEngineLoad: Double? = null,
    val avgFuelTrim: Double? = null,
    val hardAccelerations: Int = 0,
    val hardBraking: Int = 0,
    val idleTime: Long = 0
)

// Type converters for complex types
class Converters {
    private val gson = Gson()
    
    @TypeConverter
    fun fromMaintenanceType(value: MaintenanceType): String {
        return value.name
    }
    
    @TypeConverter
    fun toMaintenanceType(value: String): MaintenanceType {
        return MaintenanceType.valueOf(value)
    }
    
    @TypeConverter
    fun fromAlertSeverity(value: AlertSeverity): String {
        return value.name
    }
    
    @TypeConverter
    fun toAlertSeverity(value: String): AlertSeverity {
        return AlertSeverity.valueOf(value)
    }
    
    @TypeConverter
    fun fromAlertCategory(value: AlertCategory): String {
        return value.name
    }
    
    @TypeConverter
    fun toAlertCategory(value: String): AlertCategory {
        return AlertCategory.valueOf(value)
    }
}
