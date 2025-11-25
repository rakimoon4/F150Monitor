package com.f150monitor.utils

import com.f150monitor.data.*
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * Maintenance intelligence for 2006 F150
 * Analyzes OBD data and maintenance history to provide actionable recommendations
 */
class MaintenanceAnalyzer(
    private val obdReadingDao: OBDReadingDao,
    private val maintenanceEventDao: MaintenanceEventDao,
    private val alertDao: AlertDao
) {
    
    companion object {
        // Maintenance intervals (in days or miles)
        const val OIL_CHANGE_SEVERE_MILES = 3000
        const val OIL_CHANGE_NORMAL_MILES = 5000
        const val OIL_CHANGE_DAYS = 90 // 3 months
        
        const val TRANS_FLUID_MILES = 30000
        const val COOLANT_FLUSH_MILES = 30000
        const val COOLANT_FLUSH_DAYS = 730 // 2 years
        
        const val AIR_FILTER_MILES = 15000
        const val SPARK_PLUGS_MILES = 100000 // For 2006 F150
        
        const val O2_SENSOR_DEGRADATION_THRESHOLD = 120000 // Start monitoring closely
        
        // Performance thresholds
        const val HIGH_ENGINE_LOAD_THRESHOLD = 80.0 // %
        const val SEVERE_DUTY_THRESHOLD = 0.7 // 70% of driving time
    }
    
    suspend fun analyzeMaintenanceNeeds(): List<MaintenanceRecommendation> {
        val recommendations = mutableListOf<MaintenanceRecommendation>()
        
        // Analyze oil change needs
        recommendations.add(analyzeOilChange())
        
        // Analyze coolant system
        recommendations.addAll(analyzeCoolantSystem())
        
        // Analyze fuel system
        recommendations.addAll(analyzeFuelSystem())
        
        // Analyze O2 sensors
        recommendations.addAll(analyzeO2Sensors())
        
        // Analyze MAF sensor
        recommendations.add(analyzeMAFSensor())
        
        // Analyze transmission
        recommendations.add(analyzeTransmission())
        
        // Analyze battery/electrical
        recommendations.add(analyzeBattery())
        
        return recommendations.sortedByDescending { it.priority }
    }
    
    private suspend fun analyzeOilChange(): MaintenanceRecommendation {
        val lastOilChange = maintenanceEventDao.getLastCompletedEventOfType(MaintenanceType.OIL_CHANGE)
        val currentTime = System.currentTimeMillis()
        
        // Check recent driving conditions to determine severity
        val recentReadings = obdReadingDao.getRecentReadings(100).first()
        val severeConditions = calculateSevereDutyPercentage(recentReadings)
        
        val intervalMiles = if (severeConditions > SEVERE_DUTY_THRESHOLD) {
            OIL_CHANGE_SEVERE_MILES
        } else {
            OIL_CHANGE_NORMAL_MILES
        }
        
        val daysSinceChange = if (lastOilChange != null) {
            TimeUnit.MILLISECONDS.toDays(currentTime - lastOilChange.timestamp)
        } else {
            999 // Unknown, assume overdue
        }
        
        return when {
            lastOilChange == null -> MaintenanceRecommendation(
                category = MaintenanceType.OIL_CHANGE,
                priority = Priority.HIGH,
                title = "Oil Change - No Record Found",
                description = "No previous oil change recorded. Recommend immediate oil change.",
                reasoning = "Maintaining proper oil quality is critical for engine longevity.",
                estimatedCost = 40.0..80.0
            )
            daysSinceChange > OIL_CHANGE_DAYS -> MaintenanceRecommendation(
                category = MaintenanceType.OIL_CHANGE,
                priority = Priority.HIGH,
                title = "Oil Change Overdue (Time-Based)",
                description = "Oil change is $daysSinceChange days overdue. Recommended every $OIL_CHANGE_DAYS days.",
                reasoning = "Engine oil degrades over time even with light use.",
                estimatedCost = 40.0..80.0
            )
            severeConditions > SEVERE_DUTY_THRESHOLD -> MaintenanceRecommendation(
                category = MaintenanceType.OIL_CHANGE,
                priority = Priority.MEDIUM,
                title = "Severe Duty Detected - Shortened Oil Interval",
                description = "Your driving shows ${(severeConditions * 100).toInt()}% severe duty conditions. " +
                        "Recommend oil change every $OIL_CHANGE_SEVERE_MILES miles instead of $OIL_CHANGE_NORMAL_MILES.",
                reasoning = "Frequent idling, short trips, or high loads accelerate oil degradation.",
                estimatedCost = 40.0..80.0
            )
            else -> MaintenanceRecommendation(
                category = MaintenanceType.OIL_CHANGE,
                priority = Priority.LOW,
                title = "Oil Change - On Schedule",
                description = "Last oil change was $daysSinceChange days ago. Monitor mileage.",
                reasoning = "Oil condition appears normal based on current data.",
                estimatedCost = 40.0..80.0
            )
        }
    }
    
    private suspend fun analyzeCoolantSystem(): List<MaintenanceRecommendation> {
        val recommendations = mutableListOf<MaintenanceRecommendation>()
        val recentReadings = obdReadingDao.getRecentReadings(50).first()
        
        val avgCoolantTemp = recentReadings
            .mapNotNull { it.coolantTemp }
            .average()
            .takeIf { !it.isNaN() }
        
        val highTempReadings = obdReadingDao.getHighCoolantTempReadings(220.0).first()
        
        when {
            highTempReadings.isNotEmpty() -> {
                recommendations.add(MaintenanceRecommendation(
                    category = MaintenanceType.COOLANT_FLUSH,
                    priority = Priority.CRITICAL,
                    title = "Coolant Temperature Consistently High",
                    description = "${highTempReadings.size} instances of coolant temp above 220°F detected. " +
                            "Immediate inspection required.",
                    reasoning = "High coolant temps can indicate: low coolant level, thermostat failure, " +
                            "water pump issues, or radiator blockage. 2006 F150s are known for thermostat failures.",
                    estimatedCost = 100.0..500.0
                )
                )
            }
            avgCoolantTemp != null && avgCoolantTemp < 180.0 -> {
                recommendations.add(MaintenanceRecommendation(
                    category = MaintenanceType.COOLANT_FLUSH,
                    priority = Priority.MEDIUM,
                    title = "Engine Not Reaching Operating Temperature",
                    description = "Average coolant temp is ${"%.1f".format(avgCoolantTemp)}°F. " +
                            "Normal is 190-210°F.",
                    reasoning = "Likely stuck-open thermostat. Reduces fuel efficiency and increases engine wear.",
                    estimatedCost = 20.0..100.0
                ))
            }
        }
        
        // Check coolant flush history
        val lastFlush = maintenanceEventDao.getLastCompletedEventOfType(MaintenanceType.COOLANT_FLUSH)
        val daysSinceFlush = lastFlush?.let {
            TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - it.timestamp)
        }
        
        if (lastFlush == null || daysSinceFlush!! > COOLANT_FLUSH_DAYS) {
            recommendations.add(MaintenanceRecommendation(
                category = MaintenanceType.COOLANT_FLUSH,
                priority = Priority.MEDIUM,
                title = "Coolant Flush Due",
                description = "Coolant should be flushed every 2 years or $COOLANT_FLUSH_MILES miles.",
                reasoning = "Old coolant loses anti-corrosion properties and can damage engine components. " +
                        "F150s from this era are prone to coolant system corrosion.",
                estimatedCost = 100.0..150.0
            ))
        }
        
        return recommendations
    }
    
    private suspend fun analyzeFuelSystem(): List<MaintenanceRecommendation> {
        val recommendations = mutableListOf<MaintenanceRecommendation>()
        
        val avgLongFuelTrim = obdReadingDao.getAverageLongFuelTrim(
            System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)
        )
        
        avgLongFuelTrim?.let { trim ->
            when {
                trim > 15.0 -> {
                    recommendations.add(MaintenanceRecommendation(
                        category = MaintenanceType.OTHER,
                        priority = Priority.HIGH,
                        title = "Fuel Trim Too Positive (+${trim.toInt()}%)",
                        description = "Engine running lean. Possible vacuum leak or fuel delivery issue.",
                        reasoning = "Long-term fuel trim above +15% indicates the engine is compensating " +
                                "for insufficient fuel. Check for: vacuum leaks, clogged fuel filter, " +
                                "weak fuel pump, or dirty MAF sensor.",
                        estimatedCost = 50.0..300.0
                    ))
                }
                trim < -15.0 -> {
                    recommendations.add(MaintenanceRecommendation(
                        category = MaintenanceType.OTHER,
                        priority = Priority.HIGH,
                        title = "Fuel Trim Too Negative (${trim.toInt()}%)",
                        description = "Engine running rich. Excess fuel being injected.",
                        reasoning = "Long-term fuel trim below -15% indicates too much fuel. Check: " +
                                "dirty air filter, faulty MAF sensor, leaking fuel injectors, or " +
                                "failing O2 sensors.",
                        estimatedCost = 50.0..400.0
                    ))
                }
                else -> {
                    // Fuel trim is within normal range, no recommendation needed
                }
            }
        }
        
        return recommendations
    }
    
    private suspend fun analyzeO2Sensors(): List<MaintenanceRecommendation> {
        val recommendations = mutableListOf<MaintenanceRecommendation>()
        
        // O2 sensors typically degrade over time - 2006 F150 should have been replaced by now
        // if original. Look at fuel trim stability and response time (advanced analysis)
        
        recommendations.add(MaintenanceRecommendation(
            category = MaintenanceType.O2_SENSOR,
            priority = Priority.LOW,
            title = "O2 Sensor Monitoring",
            description = "At 19 years old, original O2 sensors (if not replaced) are well past typical lifespan.",
            reasoning = "O2 sensors typically last 60,000-100,000 miles. Degraded sensors cause: " +
                    "poor fuel economy, rough idle, and increased emissions. 2006 F150 has 4 O2 sensors.",
            estimatedCost = 150.0..600.0
        ))
        
        return recommendations
    }
    
    private suspend fun analyzeMAFSensor(): MaintenanceRecommendation {
        // MAF sensor issues show up in fuel trim and rough idle
        return MaintenanceRecommendation(
            category = MaintenanceType.MAF_CLEANING,
            priority = Priority.LOW,
            title = "MAF Sensor Cleaning",
            description = "MAF sensor should be cleaned periodically, especially if air filter maintenance has been irregular.",
            reasoning = "Dirty MAF sensors cause: incorrect fuel mixture, poor throttle response, " +
                    "and fuel trim issues. Simple cleaning can prevent expensive replacements.",
            estimatedCost = 10.0..20.0
        )
    }
    
    private suspend fun analyzeTransmission(): MaintenanceRecommendation {
        val lastTransFluid = maintenanceEventDao.getLastCompletedEventOfType(MaintenanceType.TRANSMISSION_FLUID)
        
        return if (lastTransFluid == null) {
            MaintenanceRecommendation(
                category = MaintenanceType.TRANSMISSION_FLUID,
                priority = Priority.HIGH,
                title = "Transmission Fluid Service - No Record",
                description = "2006 F150 transmission fluid should have been serviced by now.",
                reasoning = "4R75E/4R75W transmissions in 2006 F150s are known for longevity when maintained. " +
                        "Regular fluid changes are CRITICAL. Neglect leads to expensive transmission failure.",
                estimatedCost = 150.0..300.0
            )
        } else {
            MaintenanceRecommendation(
                category = MaintenanceType.TRANSMISSION_FLUID,
                priority = Priority.LOW,
                title = "Transmission Fluid - On Schedule",
                description = "Monitor for transmission temp if supported by OBD adapter.",
                reasoning = "Transmission fluid changes every 30,000-50,000 miles recommended for longevity.",
                estimatedCost = 150.0..300.0
            )
        }
    }
    
    private suspend fun analyzeBattery(): MaintenanceRecommendation {
        val recentReadings = obdReadingDao.getRecentReadings(50).first()
        val avgVoltage = recentReadings
            .mapNotNull { it.batteryVoltage }
            .average()
            .takeIf { !it.isNaN() }
        
        return when {
            avgVoltage == null -> MaintenanceRecommendation(
                category = MaintenanceType.BATTERY,
                priority = Priority.LOW,
                title = "Battery Voltage - No Data",
                description = "Unable to read battery voltage from OBD.",
                reasoning = "Monitor battery voltage for early warning of battery failure.",
                estimatedCost = 120.0..200.0
            )
            avgVoltage < 12.5 -> MaintenanceRecommendation(
                category = MaintenanceType.BATTERY,
                priority = Priority.HIGH,
                title = "Low Battery Voltage",
                description = "Average voltage is ${"%.2f".format(avgVoltage)}V. Normal is 13.5-14.5V when running.",
                reasoning = "Low voltage indicates weak battery or charging system issues. " +
                        "Test battery and alternator before winter.",
                estimatedCost = 120.0..200.0
            )
            else -> MaintenanceRecommendation(
                category = MaintenanceType.BATTERY,
                priority = Priority.LOW,
                title = "Battery Voltage Normal",
                description = "Average voltage is ${"%.2f".format(avgVoltage)}V. Electrical system healthy.",
                reasoning = "Continue monitoring. Typical battery life is 3-5 years.",
                estimatedCost = 120.0..200.0
            )
        }
    }
    
    private fun calculateSevereDutyPercentage(readings: List<OBDReading>): Double {
        if (readings.isEmpty()) return 0.0
        
        var severeCount = 0
        readings.forEach { reading ->
            // Severe duty conditions:
            // - High engine load at low speed (towing, hills)
            // - Frequent idling
            // - Short trips (engine doesn't reach full operating temp)
            
            val isHighLoad = (reading.engineLoad ?: 0.0) > HIGH_ENGINE_LOAD_THRESHOLD
            val isLowSpeed = (reading.vehicleSpeed ?: 100.0) < 25.0
            val isCold = (reading.coolantTemp ?: 200.0) < 180.0
            
            if ((isHighLoad && isLowSpeed) || (isLowSpeed && isCold)) {
                severeCount++
            }
        }
        
        return severeCount.toDouble() / readings.size
    }
    
    data class MaintenanceRecommendation(
        val category: MaintenanceType,
        val priority: Priority,
        val title: String,
        val description: String,
        val reasoning: String,
        val estimatedCost: ClosedFloatingPointRange<Double>
    )
    
    enum class Priority {
        CRITICAL,
        HIGH,
        MEDIUM,
        LOW
    }
}
