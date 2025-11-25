package com.f150monitor.obd

/**
 * OBD-II PIDs (Parameter IDs) for monitoring
 * Focused on parameters critical for 2006 F150 maintenance
 */
object OBDCommands {
    
    // Standard OBD PIDs
    const val ENGINE_RPM = "010C"
    const val VEHICLE_SPEED = "010D"
    const val COOLANT_TEMP = "0105"
    const val ENGINE_LOAD = "0104"
    const val THROTTLE_POSITION = "0111"
    const val INTAKE_TEMP = "010F"
    const val MAF_RATE = "0110"
    const val FUEL_PRESSURE = "010A"
    const val TIMING_ADVANCE = "010E"
    const val INTAKE_MANIFOLD_PRESSURE = "010B"
    
    // Fuel System
    const val FUEL_SYSTEM_STATUS = "0103"
    const val SHORT_TERM_FUEL_TRIM_BANK1 = "0106"
    const val LONG_TERM_FUEL_TRIM_BANK1 = "0107"
    const val SHORT_TERM_FUEL_TRIM_BANK2 = "0108"
    const val LONG_TERM_FUEL_TRIM_BANK2 = "0109"
    
    // O2 Sensors
    const val O2_SENSOR_BANK1_SENSOR1 = "0114"
    const val O2_SENSOR_BANK1_SENSOR2 = "0115"
    const val O2_SENSOR_BANK2_SENSOR1 = "0116"
    const val O2_SENSOR_BANK2_SENSOR2 = "0117"
    
    // Runtime and Distance
    const val RUNTIME_SINCE_START = "011F"
    const val DISTANCE_WITH_MIL = "0121"
    const val DISTANCE_SINCE_CODES_CLEARED = "0131"
    
    // Control Module Voltage
    const val CONTROL_MODULE_VOLTAGE = "0142"
    
    // Catalyst and Evap System
    const val CATALYST_TEMP_BANK1_SENSOR1 = "013C"
    const val CATALYST_TEMP_BANK2_SENSOR1 = "013E"
    
    // Mode 03 - DTCs
    const val GET_DTCS = "03"
    const val CLEAR_DTCS = "04"
    
    // Mode 09 - Vehicle Info
    const val VIN = "0902"
    
    // ELM327 AT Commands
    const val RESET = "ATZ"
    const val ECHO_OFF = "ATE0"
    const val LINE_FEED_OFF = "ATL0"
    const val HEADERS_OFF = "ATH0"
    const val SPACES_OFF = "ATS0"
    const val AUTO_PROTOCOL = "ATSP0"
    const val DESCRIBE_PROTOCOL = "ATDP"
    const val READ_VOLTAGE = "ATRV"
    
    // Transmission temp (if supported via extended PIDs)
    const val TRANSMISSION_TEMP = "0105" // May need Ford-specific PID
    
    data class PIDInfo(
        val pid: String,
        val name: String,
        val unit: String,
        val formula: (String) -> Double,
        val criticalHigh: Double? = null,
        val criticalLow: Double? = null,
        val warningHigh: Double? = null,
        val warningLow: Double? = null
    )
    
    val monitoringPIDs = mapOf(
        COOLANT_TEMP to PIDInfo(
            COOLANT_TEMP,
            "Coolant Temperature",
            "°F",
            { hex -> (hexToInt(hex) - 40) * 9.0 / 5.0 + 32 },
            criticalHigh = 240.0,
            warningHigh = 220.0,
            warningLow = 160.0
        ),
        ENGINE_RPM to PIDInfo(
            ENGINE_RPM,
            "Engine RPM",
            "RPM",
            { hex -> hexToInt(hex) / 4.0 },
            criticalHigh = 6000.0,
            warningHigh = 5500.0
        ),
        VEHICLE_SPEED to PIDInfo(
            VEHICLE_SPEED,
            "Vehicle Speed",
            "MPH",
            { hex -> hexToInt(hex) * 0.621371 }
        ),
        ENGINE_LOAD to PIDInfo(
            ENGINE_LOAD,
            "Engine Load",
            "%",
            { hex -> hexToInt(hex) * 100.0 / 255.0 },
            warningHigh = 90.0
        ),
        THROTTLE_POSITION to PIDInfo(
            THROTTLE_POSITION,
            "Throttle Position",
            "%",
            { hex -> hexToInt(hex) * 100.0 / 255.0 }
        ),
        MAF_RATE to PIDInfo(
            MAF_RATE,
            "MAF Rate",
            "g/s",
            { hex -> hexToInt(hex) / 100.0 },
            criticalLow = 2.0  // Low MAF could indicate dirty sensor
        ),
        SHORT_TERM_FUEL_TRIM_BANK1 to PIDInfo(
            SHORT_TERM_FUEL_TRIM_BANK1,
            "Short Fuel Trim B1",
            "%",
            { hex -> (hexToInt(hex) - 128) * 100.0 / 128.0 },
            warningHigh = 20.0,
            warningLow = -20.0
        ),
        LONG_TERM_FUEL_TRIM_BANK1 to PIDInfo(
            LONG_TERM_FUEL_TRIM_BANK1,
            "Long Fuel Trim B1",
            "%",
            { hex -> (hexToInt(hex) - 128) * 100.0 / 128.0 },
            warningHigh = 15.0,
            warningLow = -15.0
        ),
        CONTROL_MODULE_VOLTAGE to PIDInfo(
            CONTROL_MODULE_VOLTAGE,
            "Battery Voltage",
            "V",
            { hex -> hexToInt(hex) / 1000.0 },
            criticalLow = 11.5,
            warningLow = 12.5,
            warningHigh = 15.0
        ),
        INTAKE_TEMP to PIDInfo(
            INTAKE_TEMP,
            "Intake Air Temp",
            "°F",
            { hex -> (hexToInt(hex) - 40) * 9.0 / 5.0 + 32 }
        )
    )
    
    private fun hexToInt(hex: String): Int {
        return try {
            val cleaned = hex.replace(" ", "").trim()
            if (cleaned.length <= 2) {
                Integer.parseInt(cleaned, 16)
            } else {
                // Multi-byte value
                var result = 0
                for (i in cleaned.indices step 2) {
                    result = result shl 8
                    result += Integer.parseInt(cleaned.substring(i, i + 2), 16)
                }
                result
            }
        } catch (e: Exception) {
            0
        }
    }
}
