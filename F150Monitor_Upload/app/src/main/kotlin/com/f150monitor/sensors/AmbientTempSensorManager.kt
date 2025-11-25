package com.f150monitor.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Manages the phone's ambient temperature sensor
 * Pixel phones have this sensor which can be used to:
 * - Detect if phone is overheating in hot vehicle
 * - Correlate ambient temp with engine bay temperature
 * - Track seasonal performance patterns
 * - Validate coolant temperature readings
 */
class AmbientTempSensorManager(context: Context) : SensorEventListener {
    
    private val TAG = "AmbientTempSensor"
    private val sensorManager: SensorManager = 
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    
    private val tempSensor: Sensor? = 
        sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE)
    
    private val _ambientTemp = MutableStateFlow<Float?>(null)
    val ambientTemp: StateFlow<Float?> = _ambientTemp
    
    private val _phoneOverheating = MutableStateFlow(false)
    val phoneOverheating: StateFlow<Boolean> = _phoneOverheating
    
    val isSensorAvailable: Boolean
        get() = tempSensor != null
    
    init {
        if (tempSensor != null) {
            Log.d(TAG, "Ambient temperature sensor available: ${tempSensor.name}")
        } else {
            Log.w(TAG, "Ambient temperature sensor not available on this device")
        }
    }
    
    fun startMonitoring() {
        tempSensor?.let { sensor ->
            sensorManager.registerListener(
                this,
                sensor,
                SensorManager.SENSOR_DELAY_NORMAL
            )
            Log.d(TAG, "Started ambient temperature monitoring")
        }
    }
    
    fun stopMonitoring() {
        sensorManager.unregisterListener(this)
        Log.d(TAG, "Stopped ambient temperature monitoring")
    }
    
    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            if (it.sensor.type == Sensor.TYPE_AMBIENT_TEMPERATURE) {
                val tempCelsius = it.values[0]
                val tempFahrenheit = celsiusToFahrenheit(tempCelsius)
                
                _ambientTemp.value = tempFahrenheit
                
                // Check for phone overheating (critical for phone safety in hot vehicle)
                // Most phones start throttling around 40°C (104°F)
                _phoneOverheating.value = tempCelsius > 40.0f
                
                Log.d(TAG, "Ambient temp: ${"%.1f".format(tempFahrenheit)}°F")
                
                if (_phoneOverheating.value) {
                    Log.w(TAG, "Phone temperature critical! Consider moving to cooler location.")
                }
            }
        }
    }
    
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not typically needed for temperature sensor
    }
    
    /**
     * Analyzes temperature differential between ambient and engine bay
     * Useful for detecting:
     * - Cooling system issues (excessive engine bay heat)
     * - AC system performance
     * - Seasonal baseline shifts
     */
    fun analyzeTempDifferential(
        ambientTempF: Float,
        engineCoolantTempF: Float
    ): TempAnalysis {
        val differential = engineCoolantTempF - ambientTempF
        
        return TempAnalysis(
            ambientTemp = ambientTempF,
            coolantTemp = engineCoolantTempF,
            differential = differential,
            isNormal = differential in 20.0..200.0, // Normal differential range
            concern = when {
                differential < 20.0 -> "Engine not reaching operating temperature"
                differential > 220.0 -> "Excessive engine bay heat - check cooling system"
                engineCoolantTempF > 220.0 -> "Coolant temperature high"
                else -> null
            }
        )
    }
    
    /**
     * Checks if ambient conditions could affect engine performance
     */
    fun getAmbientConditionWarnings(ambientTempF: Float): List<String> {
        val warnings = mutableListOf<String>()
        
        when {
            ambientTempF > 100.0f -> {
                warnings.add("Extreme heat: Monitor coolant temp closely")
                warnings.add("AC system working harder - watch engine load")
            }
            ambientTempF < 32.0f -> {
                warnings.add("Freezing conditions: Allow longer warm-up")
                warnings.add("Check for frozen coolant or fluids")
            }
            ambientTempF < 10.0f -> {
                warnings.add("Extreme cold: Battery capacity reduced")
                warnings.add("Engine oil thicker - ensure proper warm-up")
            }
        }
        
        // Phone safety
        if (ambientTempF > 104.0f) {
            warnings.add("⚠️ Phone at risk of overheating - move to cooler location")
        }
        
        return warnings
    }
    
    private fun celsiusToFahrenheit(celsius: Float): Float {
        return celsius * 9.0f / 5.0f + 32.0f
    }
    
    data class TempAnalysis(
        val ambientTemp: Float,
        val coolantTemp: Float,
        val differential: Float,
        val isNormal: Boolean,
        val concern: String?
    )
}
