package com.f150monitor.ui

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.f150monitor.data.*
import com.f150monitor.service.OBDMonitorService
import com.f150monitor.utils.MaintenanceAnalyzer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun F150MonitorApp() {
    var selectedTab by remember { mutableStateOf(0) }
    val context = LocalContext.current
    val viewModel: F150ViewModel = viewModel()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("F150 Monitor") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, "Dashboard") },
                    label = { Text("Dashboard") },
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Build, "Maintenance") },
                    label = { Text("Maintenance") },
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Warning, "Alerts") },
                    label = { Text("Alerts") },
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, "Settings") },
                    label = { Text("Settings") },
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (selectedTab) {
                0 -> DashboardScreen(viewModel)
                1 -> MaintenanceScreen(viewModel)
                2 -> AlertsScreen(viewModel)
                3 -> SettingsScreen(viewModel, context)
            }
        }
    }
}

@Composable
fun DashboardScreen(viewModel: F150ViewModel) {
    val isMonitoring by viewModel.isMonitoring.collectAsState()
    val latestReading by viewModel.latestReading.collectAsState()
    val context = LocalContext.current
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Connection status
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isMonitoring) 
                    MaterialTheme.colorScheme.primaryContainer 
                else 
                    MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (isMonitoring) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        if (isMonitoring) "Connected" else "Disconnected",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        if (isMonitoring) "OBDLink MX monitoring active" else "Not connected to OBD adapter",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Control button
        Button(
            onClick = {
                if (isMonitoring) {
                    viewModel.stopMonitoring(context)
                } else {
                    viewModel.startMonitoring(context)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isMonitoring) "Stop Monitoring" else "Start Monitoring")
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Real-time data
        if (isMonitoring && latestReading != null) {
            Text(
                "Live Data",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    DataCard("Speed", "${latestReading?.vehicleSpeed?.toInt() ?: "--"} MPH")
                }
                item {
                    DataCard("RPM", "${latestReading?.engineRpm?.toInt() ?: "--"}")
                }
                item {
                    DataCard(
                        "Coolant Temp", 
                        "${latestReading?.coolantTemp?.toInt() ?: "--"}°F",
                        warningThreshold = 220.0,
                        cardValue = latestReading?.coolantTemp
                    )
                }
                item {
                    DataCard(
                        "Engine Load",
                        "${latestReading?.engineLoad?.toInt() ?: "--"}%"
                    )
                }
                item {
                    DataCard(
                        "Battery Voltage",
                        "${"%.1f".format(latestReading?.batteryVoltage ?: 0.0)}V",
                        warningThreshold = 12.5,
                        cardValue = latestReading?.batteryVoltage,
                        inverse = true
                    )
                }
                
                latestReading?.ambientTemp?.let { temp ->
                    item {
                        DataCard(
                            "Ambient Temp (Phone)",
                            "${temp.toInt()}°F",
                            isPhoneSensor = true
                        )
                    }
                }
            }
        } else {
            Text(
                "No data available. Connect to OBD adapter to begin monitoring.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun DataCard(
    label: String,
    value: String,
    warningThreshold: Double? = null,
    cardValue: Double? = null,
    inverse: Boolean = false,
    isPhoneSensor: Boolean = false
) {
    val isWarning = warningThreshold != null && cardValue != null && (
        if (inverse) cardValue < warningThreshold else cardValue > warningThreshold
    )
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isWarning -> MaterialTheme.colorScheme.errorContainer
                isPhoneSensor -> MaterialTheme.colorScheme.tertiaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                label,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = if (isWarning) MaterialTheme.colorScheme.error else Color.Unspecified
            )
        }
    }
}

@Composable
fun MaintenanceScreen(viewModel: F150ViewModel) {
    val recommendations by viewModel.maintenanceRecommendations.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "Maintenance Recommendations",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (recommendations.isEmpty()) {
            Text("Loading recommendations...")
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(recommendations) { rec ->
                    MaintenanceCard(rec)
                }
            }
        }
    }
}

@Composable
fun MaintenanceCard(recommendation: MaintenanceAnalyzer.MaintenanceRecommendation) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (recommendation.priority) {
                MaintenanceAnalyzer.Priority.CRITICAL -> MaterialTheme.colorScheme.errorContainer
                MaintenanceAnalyzer.Priority.HIGH -> Color(0xFFFFE0B2)
                MaintenanceAnalyzer.Priority.MEDIUM -> MaterialTheme.colorScheme.primaryContainer
                MaintenanceAnalyzer.Priority.LOW -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    recommendation.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    recommendation.priority.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = when (recommendation.priority) {
                        MaintenanceAnalyzer.Priority.CRITICAL -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                recommendation.description,
                style = MaterialTheme.typography.bodyMedium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                "Why: ${recommendation.reasoning}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                "Est. Cost: $${recommendation.estimatedCost.start.toInt()}-$${recommendation.estimatedCost.endInclusive.toInt()}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun AlertsScreen(viewModel: F150ViewModel) {
    val alerts by viewModel.alerts.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "Recent Alerts",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (alerts.isEmpty()) {
            Text("No alerts")
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(alerts) { alert ->
                    AlertCard(alert)
                }
            }
        }
    }
}

@Composable
fun AlertCard(alert: Alert) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (alert.severity) {
                AlertSeverity.CRITICAL -> MaterialTheme.colorScheme.errorContainer
                AlertSeverity.WARNING -> Color(0xFFFFE0B2)
                AlertSeverity.INFO -> MaterialTheme.colorScheme.primaryContainer
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    alert.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    alert.severity.name,
                    style = MaterialTheme.typography.labelSmall
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                alert.message,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
fun SettingsScreen(viewModel: F150ViewModel, context: Context) {
    val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    val bluetoothAdapter = bluetoothManager.adapter
    val pairedDevices = bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()
    
    var selectedDevice by remember { mutableStateOf<String?>(null) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "Settings",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            "OBD Adapter",
            style = MaterialTheme.typography.titleMedium
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        pairedDevices.forEach { device ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                onClick = {
                    selectedDevice = device.address
                    viewModel.setDeviceAddress(device.address)
                }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (selectedDevice == device.address) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Column {
                        Text(device.name ?: "Unknown Device")
                        Text(
                            device.address,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        
        if (pairedDevices.isEmpty()) {
            Text("No paired Bluetooth devices found. Please pair your OBDLink MX in Android Bluetooth settings.")
        }
    }
}

// ViewModel
class F150ViewModel : ViewModel() {
    private val _isMonitoring = MutableStateFlow(false)
    val isMonitoring: StateFlow<Boolean> = _isMonitoring
    
    private val _latestReading = MutableStateFlow<OBDReading?>(null)
    val latestReading: StateFlow<OBDReading?> = _latestReading
    
    private val _maintenanceRecommendations = MutableStateFlow<List<MaintenanceAnalyzer.MaintenanceRecommendation>>(emptyList())
    val maintenanceRecommendations: StateFlow<List<MaintenanceAnalyzer.MaintenanceRecommendation>> = _maintenanceRecommendations
    
    private val _alerts = MutableStateFlow<List<Alert>>(emptyList())
    val alerts: StateFlow<List<Alert>> = _alerts
    
    private var deviceAddress: String? = null
    
    fun setDeviceAddress(address: String) {
        deviceAddress = address
    }
    
    fun startMonitoring(context: Context) {
        deviceAddress?.let { address ->
            val intent = Intent(context, OBDMonitorService::class.java).apply {
                action = OBDMonitorService.ACTION_START_MONITORING
                putExtra(OBDMonitorService.EXTRA_DEVICE_ADDRESS, address)
            }
            context.startForegroundService(intent)
            _isMonitoring.value = true
        }
    }
    
    fun stopMonitoring(context: Context) {
        val intent = Intent(context, OBDMonitorService::class.java).apply {
            action = OBDMonitorService.ACTION_STOP_MONITORING
        }
        context.startService(intent)
        _isMonitoring.value = false
    }
    
    // Note: In a complete implementation, these would observe Room database flows
    init {
        // Mock data for demonstration
        _maintenanceRecommendations.value = listOf(
            MaintenanceAnalyzer.MaintenanceRecommendation(
                MaintenanceType.OIL_CHANGE,
                MaintenanceAnalyzer.Priority.HIGH,
                "Oil Change Due",
                "Based on severe duty conditions",
                "Frequent short trips and high loads detected",
                40.0..80.0
            )
        )
    }
}
