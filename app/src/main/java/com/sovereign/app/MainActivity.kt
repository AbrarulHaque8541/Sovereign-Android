package com.sovereign.app

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.sovereign.app.SovereignApplication.Companion.backgroundScope
import com.sovereign.app.SovereignApplication.Companion.dataStore
import com.sovereign.app.SovereignApplication.Companion.getServerEnabled
import com.sovereign.app.SovereignApplication.Companion.getServerPort
import com.sovereign.app.SovereignApplication.Companion.setCaptureEnabled
import com.sovereign.app.SovereignApplication.Companion.setServerEnabled
import com.sovereign.app.SovereignApplication.Companion.setServerPort
import com.sovereign.app.tools.ADBControlHubScreen
import com.sovereign.app.tools.NativeSystemServiceEngine
import com.sovereign.app.tools.ScriptRunnerUtility
import com.sovereign.app.tools.FastbootProtocolBridge
import com.sovereign.ui.UniversalCaptureScreen
import com.sovereign.ui.theme.ThemeManager
import com.sovereign.ui.theme.ThemeManagerState
import com.sovereign.ui.theme.ThemeType
import com.sovereign.ui.theme.ThemeWrapper
import com.sovereign.ui.SharedComponents
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private const val TAG = "MainActivity"
    private const val MEDIA_PROJECTION_REQUEST = 1001
    private const val OVERLAY_PERMISSION_REQUEST = 1002
    private const val NOTIFICATION_PERMISSION_REQUEST = 1003

    private val mediaProjectionManager by lazy { getSystemService(MediaProjectionManager::class.java) }
    
    private var mediaProjection: MediaProjection? = null
    private var isCapturing = false
    private var captureIntent: Intent? = null
    private var syncServiceIntent: Intent? = null
    
    private val requestMediaProjection = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            if (data != null) {
                mediaProjection = mediaProjectionManager.getMediaProjection(result.resultCode, data)
                startCapture(mediaProjection!!)
            }
        } else {
            Toast.makeText(this, "Screen capture permission denied", Toast.LENGTH_SHORT).show()
        }
    }
    
    private val requestOverlayPermission = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (android.provider.Settings.canDrawOverlays(this)) {
                Log.d(TAG, "Overlay permission granted")
            } else {
                Toast.makeText(this, "Overlay permission required for capture controls", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private val requestNotificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            Log.d(TAG, "Notification permission granted")
        } else {
            Toast.makeText(this, "Notification permission needed for foreground service", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Check and request permissions
        checkPermissions()
        
        // Initialize sync service
        syncServiceIntent = LocalSyncService.createStartIntent(this)
        startForegroundService(syncServiceIntent!!)
        
        // Initialize ADB service
        val adbIntent = NativeSystemServiceEngine.createAdbStartIntent(this)
        startForegroundService(adbIntent)
        
        // Initialize Fastboot service
        val fastbootIntent = FastbootProtocolBridge.createFastbootIntent(this)
        startForegroundService(fastbootIntent)
        
        // Register UI reload receiver
        registerUiReloadReceiver()
        
        setContent {
            ThemeWrapper {
                MainScreen(
                    isCapturing = isCapturing,
                    onCaptureToggle = { toggleCapture() },
                    onSettingsClick = { /* Open settings */ },
                    serverPort = 8000,
                    serverEnabled = true
                )
            }
        }
        
        // Apply saved settings
        applySavedSettings()
    }

    private fun checkPermissions() {
        // Notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        
        // Overlay permission for floating controls
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!android.provider.Settings.canDrawOverlays(this)) {
                val intent = Intent(this, OverlayPermissionActivity::class.java)
                requestOverlayPermission.launch(intent)
            }
        }
    }

    private fun applySavedSettings() {
        backgroundScope.launch {
            // Load saved theme
            val savedTheme = ThemeManager.getSavedTheme()
            // Theme is applied via ThemeWrapper
        }
    }

    private fun registerUiReloadReceiver() {
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val component = intent.getStringExtra("component") ?: "unknown"
                val payload = intent.getStringExtra("payload") ?: ""
                Log.d(TAG, "UI Reload: $component")
            }
        }
        val filter = android.content.IntentFilter("com.sovereign.UI_RELOAD")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(receiver, filter)
        }
    }

    private fun toggleCapture() {
        if (isCapturing) {
            stopCapture()
        } else {
            requestMediaProjectionPermission()
        }
    }

    private fun requestMediaProjectionPermission() {
        val intent = mediaProjectionManager.createScreenCaptureIntent()
        requestMediaProjection.launch(intent)
    }

    private fun startCapture(projection: MediaProjection) {
        isCapturing = true
        captureIntent = CaptureService.createCaptureIntent(this).apply {
            putExtra("media_projection", projection)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(captureIntent!!)
        } else {
            startService(captureIntent!!)
        }
        
        backgroundScope.launch {
            setCaptureEnabled(true)
        }
        
        recreate()
    }

    private fun stopCapture() {
        isCapturing = false
        if (captureIntent != null) {
            val stopIntent = CaptureService.createStopIntent(this)
            startService(stopIntent)
            captureIntent = null
        }
        
        backgroundScope.launch {
            setCaptureEnabled(false)
        }
        
        recreate()
    }

    override fun onDestroy() {
        if (captureIntent != null) {
            stopCapture()
        }
        if (syncServiceIntent != null) {
            val stopIntent = LocalSyncService.createStopIntent(this)
            startService(stopIntent)
        }
        super.onDestroy()
    }
}

@Composable
fun MainScreen(
    isCapturing: Boolean,
    onCaptureToggle: () -> Unit,
    onSettingsClick: () -> Unit,
    serverPort: Int,
    serverEnabled: Boolean
) {
    val selectedTab = remember { mutableStateOf(0) }
    
    Column(Modifier.fillMaxSize()) {
        // Top Tab Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            tabButton(0, "Capture", androidx.compose.material.icons.Icons.Filled.PlayArrow, selectedTab.value == 0) { selectedTab.value = 0 }
            tabButton(1, "System Tools", androidx.compose.material.icons.Icons.Filled.Construction, selectedTab.value == 1) { selectedTab.value = 1 }
            tabButton(2, "Settings", androidx.compose.material.icons.Icons.Filled.Settings, selectedTab.value == 2) { selectedTab.value = 2 }
        }
        
        androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.1f)))
        
        // Tab Content
        androidx.compose.foundation.layout.Box(Modifier.fillMaxSize()) {
            when (selectedTab.value) {
                0 -> UniversalCaptureScreen(
                    modifier = Modifier.fillMaxSize(),
                    onNavigateBack = { /* Navigate back */ },
                    onSearchClick = { /* Handle search */ },
                    onSettingsClick = { /* Open settings */ }
                )
                1 -> ADBControlHubScreen(
                    modifier = Modifier.fillMaxSize(),
                    onNavigateBack = { selectedTab.value = 0 }
                )
                2 -> SettingsScreen(
                    onNavigateBack = { selectedTab.value = 0 },
                    serverPort = serverPort,
                    serverEnabled = serverEnabled
                )
            }
        }
    }
}

@Composable
fun tabButton(
    index: Int,
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) Color(0xFFBB86FC).copy(alpha = 0.2f) else Color(0xFF1E1E1E),
            contentColor = if (selected) Color(0xFFBB86FC) else Color.White
        ),
        shape = RoundedCornerShape(12.dp),
        border = if (selected) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBB86FC)) else null
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = "", tint = if (selected) Color(0xFFBB86FC) else Color.White)
            androidx.compose.foundation.layout.Box(Modifier.width(8.dp))
            Text(label, fontWeight = FontWeight.Medium, color = if (selected) Color(0xFFBB86FC) else Color.White)
        }
    }
}

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    serverPort: Int,
    serverEnabled: Boolean
) {
    val localPort = remember { mutableStateOf(serverPort.toString()) }
    val localServerEnabled = remember { mutableStateOf(serverEnabled) }
    
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Server Settings
        Text("Server Settings", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Local Sync Server", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                        Text("Port: $serverPort", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                    androidx.compose.material3.Switch(
                        checked = localServerEnabled.value,
                        onCheckedChange = { localServerEnabled.value = it },
                        colors = androidx.material3.SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                    )
                }
                
                TextField(
                    value = localPort.value,
                    onValueChange = { localPort.value = it },
                    label = { Text("Port", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)) },
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    singleLine = true,
                    keyboardOptions = androidx.compose.ui.text.input.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    colors = TextFieldDefaults.textFieldColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        textColor = MaterialTheme.colorScheme.onSurface,
                        placeholderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                )
            }
        }
        
        // Capture Settings
        Text("Capture Settings", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 8.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Configure capture resolution, FPS, and bitrate in the Capture tab", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            }
        }
        
        // System Info
        Text("System Info", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 8.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("App Version: 1.0.0", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                Text("Build: Debug", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                Text("Target SDK: 34", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                Text("Min SDK: 24", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            }
        }
    }
}