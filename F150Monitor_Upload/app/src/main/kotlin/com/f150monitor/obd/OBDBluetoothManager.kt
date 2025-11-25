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
    private val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    
    private var bluetoothSocket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null
    
    var isConnected = false
        private set
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    @SuppressLint("MissingPermission")
    suspend fun connect(deviceAddress: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            val device: BluetoothDevice = bluetoothAdapter.getRemoteDevice(deviceAddress)
            
            Log.d(TAG, "Attempting to connect to: ${device.name} - $deviceAddress")
            
            // Cancel discovery to speed up connection
            bluetoothAdapter.cancelDiscovery()
            
            bluetoothSocket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            bluetoothSocket?.connect()
            
            inputStream = bluetoothSocket?.inputStream
            outputStream = bluetoothSocket?.outputStream
            
            // Initialize ELM327
            if (initialize()) {
                isConnected = true
                Log.d(TAG, "Successfully connected and initialized OBD adapter")
                true
            } else {
                disconnect()
                false
            }
        } catch (e: IOException) {
            Log.e(TAG, "Connection failed: ${e.message}")
            disconnect()
            false
        }
    }
    
    private suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        try {
            // Reset adapter
            sendCommand(OBDCommands.RESET)
            delay(1500) // Wait for reset
            
            // Clear input buffer
            inputStream?.available()?.let { available ->
                val buffer = ByteArray(available)
                inputStream?.read(buffer)
            }
            
            // Configure adapter
            sendCommand(OBDCommands.ECHO_OFF)
            delay(200)
            sendCommand(OBDCommands.LINE_FEED_OFF)
            delay(200)
            sendCommand(OBDCommands.HEADERS_OFF)
            delay(200)
            sendCommand(OBDCommands.SPACES_OFF)
            delay(200)
            sendCommand(OBDCommands.AUTO_PROTOCOL)
            delay(500)
            
            // Test with a simple command
            val response = sendCommand(OBDCommands.READ_VOLTAGE)
            response.isNotEmpty() && !response.contains("ERROR")
        } catch (e: Exception) {
            Log.e(TAG, "Initialization failed: ${e.message}")
            false
        }
    }
    
    suspend fun sendCommand(command: String): String = withContext(Dispatchers.IO) {
        if (!isConnected) return@withContext ""
        
        try {
            // Clear input buffer
            inputStream?.available()?.let { available ->
                if (available > 0) {
                    val buffer = ByteArray(available)
                    inputStream?.read(buffer)
                }
            }
            
            // Send command
            val cmdBytes = "$command\r".toByteArray()
            outputStream?.write(cmdBytes)
            outputStream?.flush()
            
            // Read response
            delay(100) // Give time for response
            
            val response = StringBuilder()
            val buffer = ByteArray(1024)
            var attempts = 0
            
            while (attempts < 20) { // Max 2 seconds
                val available = inputStream?.available() ?: 0
                if (available > 0) {
                    val bytesRead = inputStream?.read(buffer, 0, available) ?: 0
                    response.append(String(buffer, 0, bytesRead))
                    
                    // Check if response is complete
                    if (response.contains(">")) {
                        break
                    }
                }
                delay(100)
                attempts++
            }
            
            // Clean up response
            response.toString()
                .replace(command, "")
                .replace("\r", "")
                .replace("\n", "")
                .replace(">", "")
                .trim()
                
        } catch (e: IOException) {
            Log.e(TAG, "Error sending command: ${e.message}")
            ""
        }
    }
    
    suspend fun readPID(pid: String): OBDResponse = withContext(Dispatchers.IO) {
        val rawResponse = sendCommand(pid)
        
        if (rawResponse.isEmpty() || rawResponse.contains("ERROR") || 
            rawResponse.contains("NO DATA") || rawResponse.contains("UNABLE")) {
            return@withContext OBDResponse(pid, "", false, null)
        }
        
        try {
            // Parse response - format is typically "41 0C XX XX" for PID 010C
            val parts = rawResponse.split(" ")
            if (parts.size >= 2) {
                // Extract data bytes (skip mode and PID echo)
                val dataBytes = parts.drop(2).joinToString("")
                
                // Get PID info and calculate value
                val pidInfo = OBDCommands.monitoringPIDs[pid]
                val value = pidInfo?.formula?.invoke(dataBytes)
                
                return@withContext OBDResponse(pid, dataBytes, true, value)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing PID response: ${e.message}")
        }
        
        return@withContext OBDResponse(pid, rawResponse, false, null)
    }
    
    fun disconnect() {
        try {
            isConnected = false
            inputStream?.close()
            outputStream?.close()
            bluetoothSocket?.close()
            Log.d(TAG, "Disconnected from OBD adapter")
        } catch (e: IOException) {
            Log.e(TAG, "Error disconnecting: ${e.message}")
        }
    }
    
    fun cleanup() {
        disconnect()
        scope.cancel()
    }
    
    data class OBDResponse(
        val pid: String,
        val raw: String,
        val success: Boolean,
        val value: Double?
    )
}
