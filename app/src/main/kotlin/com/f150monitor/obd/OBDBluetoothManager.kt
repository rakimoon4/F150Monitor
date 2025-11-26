package com.f150monitor.obd

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import kotlinx.coroutines.*
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

class OBDBluetoothManager {
    
    private val TAG = "OBDBluetoothManager"
    
    // Standard SPP UUID for OBD adapters
    private val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    
    private var bluetoothSocket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null
    
    var isConnected = false
        private set
    
    @SuppressLint("MissingPermission")
    suspend fun connect(deviceAddress: String): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Attempting to connect to: $deviceAddress")
            
            // Get the Bluetooth adapter
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            if (bluetoothAdapter == null) {
                Log.e(TAG, "Bluetooth not supported on this device")
                return@withContext false
            }
            
            // Cancel discovery to speed up connection
            try {
                bluetoothAdapter.cancelDiscovery()
            } catch (e: Exception) {
                Log.w(TAG, "Could not cancel discovery: ${e.message}")
            }
            
            // Get the remote device
            val device: BluetoothDevice = try {
                bluetoothAdapter.getRemoteDevice(deviceAddress)
            } catch (e: Exception) {
                Log.e(TAG, "Invalid device address: ${e.message}")
                return@withContext false
            }
            
            Log.d(TAG, "Got remote device: ${device.name}")
            
            // Close any existing connection
            disconnect()
            
            // Create socket and connect
            bluetoothSocket = try {
                Log.d(TAG, "Creating RFCOMM socket...")
                device.createRfcommSocketToServiceRecord(SPP_UUID)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create socket: ${e.message}")
                return@withContext false
            }
            
            try {
                Log.d(TAG, "Connecting socket...")
                bluetoothSocket?.connect()
                Log.d(TAG, "Socket connected!")
            } catch (e: IOException) {
                Log.e(TAG, "Connection failed: ${e.message}")
                // Try fallback method
                try {
                    Log.d(TAG, "Trying fallback connection method...")
                    bluetoothSocket?.close()
                    val method = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                    bluetoothSocket = method.invoke(device, 1) as BluetoothSocket
                    bluetoothSocket?.connect()
                    Log.d(TAG, "Fallback connection successful!")
                } catch (e2: Exception) {
                    Log.e(TAG, "Fallback also failed: ${e2.message}")
                    disconnect()
                    return@withContext false
                }
            }
            
            // Get streams
            inputStream = bluetoothSocket?.inputStream
            outputStream = bluetoothSocket?.outputStream
            
            if (inputStream == null || outputStream == null) {
                Log.e(TAG, "Failed to get streams")
                disconnect()
                return@withContext false
            }
            
            // Initialize ELM327
            Log.d(TAG, "Initializing ELM327...")
            if (initialize()) {
                isConnected = true
                Log.d(TAG, "Successfully connected and initialized OBD adapter")
                true
            } else {
                Log.e(TAG, "ELM327 initialization failed")
                disconnect()
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Connection error: ${e.message}", e)
            disconnect()
            false
        }
    }
    
    private suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        try {
            delay(500)
            
            // Reset adapter
            var response = sendCommand("ATZ")
            Log.d(TAG, "ATZ response: $response")
            delay(1000)
            
            // Echo off
            response = sendCommand("ATE0")
            Log.d(TAG, "ATE0 response: $response")
            delay(200)
            
            // Line feed off
            response = sendCommand("ATL0")
            Log.d(TAG, "ATL0 response: $response")
            delay(200)
            
            // Spaces off
            response = sendCommand("ATS0")
            Log.d(TAG, "ATS0 response: $response")
            delay(200)
            
            // Headers off
            response = sendCommand("ATH0")
            Log.d(TAG, "ATH0 response: $response")
            delay(200)
            
            // Auto protocol
            response = sendCommand("ATSP0")
            Log.d(TAG, "ATSP0 response: $response")
            delay(500)
            
            // Test with voltage read
            response = sendCommand("ATRV")
            Log.d(TAG, "ATRV response: $response")
            
            val success = response.isNotEmpty() && !response.contains("ERROR") && !response.contains("?")
            Log.d(TAG, "Initialization ${if (success) "successful" else "failed"}")
            success
        } catch (e: Exception) {
            Log.e(TAG, "Initialization error: ${e.message}")
            false
        }
    }
    
    suspend fun sendCommand(command: String): String = withContext(Dispatchers.IO) {
        if (outputStream == null || inputStream == null) {
            Log.w(TAG, "Streams not available")
            return@withContext ""
        }
        
        try {
            // Clear input buffer
            while (inputStream?.available() ?: 0 > 0) {
                inputStream?.read()
            }
            
            // Send command
            val cmdBytes = "$command\r".toByteArray(Charsets.US_ASCII)
            outputStream?.write(cmdBytes)
            outputStream?.flush()
            
            // Read response
            delay(100)
            
            val response = StringBuilder()
            val buffer = ByteArray(1024)
            var attempts = 0
            
            while (attempts < 30) {
                val available = inputStream?.available() ?: 0
                if (available > 0) {
                    val bytesRead = inputStream?.read(buffer, 0, minOf(available, buffer.size)) ?: 0
                    if (bytesRead > 0) {
                        response.append(String(buffer, 0, bytesRead, Charsets.US_ASCII))
                        if (response.contains(">")) {
                            break
                        }
                    }
                }
                delay(100)
                attempts++
            }
            
            response.toString()
                .replace(command, "")
                .replace("\r", "")
                .replace("\n", " ")
                .replace(">", "")
                .trim()
        } catch (e: Exception) {
            Log.e(TAG, "Error sending command '$command': ${e.message}")
            ""
        }
    }
    
    suspend fun readPID(pid: String): OBDResponse = withContext(Dispatchers.IO) {
        val rawResponse = sendCommand(pid)
        
        if (rawResponse.isEmpty() || rawResponse.contains("ERROR") || 
            rawResponse.contains("NO DATA") || rawResponse.contains("UNABLE") ||
            rawResponse.contains("?")) {
            return@withContext OBDResponse(pid, rawResponse, false, null)
        }
        
        try {
            val cleaned = rawResponse.replace(" ", "")
            if (cleaned.length >= 4) {
                val dataStart = 4
                if (cleaned.length > dataStart) {
                    val dataBytes = cleaned.substring(dataStart)
                    val pidInfo = OBDCommands.monitoringPIDs[pid]
                    val value = pidInfo?.formula?.invoke(dataBytes)
                    return@withContext OBDResponse(pid, dataBytes, true, value)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing PID response: ${e.message}")
        }
        
        OBDResponse(pid, rawResponse, false, null)
    }
    
    fun disconnect() {
        try {
            isConnected = false
            inputStream?.close()
            outputStream?.close()
            bluetoothSocket?.close()
            inputStream = null
            outputStream = null
            bluetoothSocket = null
            Log.d(TAG, "Disconnected from OBD adapter")
        } catch (e: IOException) {
            Log.e(TAG, "Error disconnecting: ${e.message}")
        }
    }
    
    fun cleanup() {
        disconnect()
    }
    
    data class OBDResponse(
        val pid: String,
        val raw: String,
        val success: Boolean,
        val value: Double?
    )
}
