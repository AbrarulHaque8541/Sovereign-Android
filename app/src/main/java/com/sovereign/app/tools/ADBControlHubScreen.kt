package com.sovereign.app.tools

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Chip
import androidx.compose.material3.ChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Rocket
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.HourglassFull
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.RunningWithErrors
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.ui.window.PopupProperties
import com.sovereign.app.SovereignApplication.Companion.backgroundScope
import com.sovereign.app.tools.NativeSystemServiceEngine
import com.sovereign.app.tools.ScriptRunnerUtility
import com.sovereign.app.tools.FastbootProtocolBridge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ADBControlHubScreen(
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("System", "ADB", "Fastboot", "Scripts", "Packages")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
    ) {
        TopToolbar(onNavigateBack = onNavigateBack)

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(tabs.withIndex()) { indexed ->
                Chip(
                    onClick = { selectedTab = indexed.index },
                    selected = selectedTab == indexed.index,
                    label = { Text(indexed.value, color = Color.White) },
                    colors = ChipDefaults.chipColors(
                        containerColor = Color(0xFF1E1E1E),
                        selectedContainerColor = Color(0xFFBB86FC)
                    )
                )
            }
        }

        when (selectedTab) {
            0 -> SystemToolsTab()
            1 -> AdbTab()
            2 -> FastbootTab()
            3 -> ScriptsTab()
            4 -> PackagesTab()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopToolbar(onNavigateBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onNavigateBack) {
            Icon(
                imageVector = Icons.Filled.PowerSettingsNew,
                contentDescription = "Back",
                tint = Color.White
            )
        }
        Text(
            text = "System Tools & ADB Hub",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        IconButton(onClick = {}) {
            Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = Color.White)
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SystemToolsTab() {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("System Tools", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
        
        ActionCard("Normal Reboot", Icons.Filled.PowerSettingsNew, "Restart device normally") {
            NativeSystemServiceEngine.reboot("user")
        }
        ActionCard("Bootloader Mode", Icons.Filled.Usb, "Reboot to Fastboot/Bootloader") {
            NativeSystemServiceEngine.rebootBootloader()
        }
        ActionCard("Recovery Mode", Icons.Filled.HourglassFull, "Reboot to Recovery") {
            NativeSystemServiceEngine.rebootRecovery()
        }
        ActionCard("Safe Mode", Icons.Filled.RunningWithErrors, "Reboot into Safe Mode") {
            NativeSystemServiceEngine.reboot("safe")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdbTab() {
    val context = LocalContext.current
    var ipInput by remember { mutableStateOf("") }
    var adbRunning by remember { mutableStateOf(false) }
    val scope = remember { CoroutineScope(Dispatchers.IO) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("ADB Control", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextField(
                value = ipInput,
                onValueChange = { ipInput = it },
                modifier = Modifier.weight(1f),
                label = { Text("IP Address", color = Color.White.copy(alpha = 0.6f)) },
                colors = TextFieldDefaults.textFieldColors(
                    containerColor = Color(0xFF1E1E1E),
                    textColor = Color.White
                ),
                singleLine = true
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    if (ipInput.isNotBlank()) {
                        NativeSystemServiceEngine.connectAdbWireless(ipInput)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00CCFF))
            ) {
                Text("Connect", color = Color.Black)
            }
        }
        
        ActionCard("ADB Loopback", Icons.Filled.Radio, "Start wireless ADB: 127.0.0.1:5555") {
            NativeSystemServiceEngine.startAdbLoopback(context)
        }
        ActionCard("Stop ADB", Icons.Filled.Clear, "Stop wireless ADB loopback") {
            NativeSystemServiceEngine.stopAdbLoopback()
        }
        ActionCard("Execute Shell Command", Icons.Filled.Code, "Run shell command") {
            NativeSystemServiceEngine.executeShellCommand("getprop ro.build.version")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FastbootTab() {
    val context = LocalContext.current
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Fastboot Control", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
        
        ActionCard("Check Devices", Icons.Filled.Usb, "List fastboot devices") {
            FastbootProtocolBridge.discoverFastbootDevices()
        }
        ActionCard("Flash Boot", Icons.Filled.Rocket, "Flash boot partition") {
            FastbootProtocolBridge.flashPartition("boot", java.io.File("/dev/null"))
        }
        ActionCard("Flash Recovery", Icons.Filled.HourglassFull, "Flash recovery partition") {
            FastbootProtocolBridge.erasePartition("recovery")
        }
        ActionCard("Unlock Bootloader", Icons.Filled.Clear, "Unlock bootloader") {
            FastbootProtocolBridge.unlockBootloader()
        }
        ActionCard("Lock Bootloader", Icons.Filled.CheckCircle, "Re-lock bootloader") {
            FastbootProtocolBridge.lockBootloader()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScriptsTab() {
    val context = LocalContext.current
    val scripts by remember { mutableStateOf(ScriptRunnerUtility.listScripts(context)) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Script Runner", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
        
        if (scripts.isEmpty()) {
            Text("No scripts available", color = Color.White.copy(alpha = 0.6f))
        } else {
            LazyColumn {
                items(scripts) { script ->
                    ActionCard(
                        script.name,
                        Icons.Filled.Code,
                        "${script.type} - ${script.size} bytes"
                    ) {
                        ScriptRunnerUtility.executeScript(context, script.name)
                    }
                }
            }
        }
        
        Button(
            onClick = { ScriptRunnerUtility.createScriptFromTemplate(context, "system_info") },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBB86FC)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Create System Info Script", color = Color.Black)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PackagesTab() {
    val context = LocalContext.current
    val packages by remember { mutableStateOf(NativeSystemServiceEngine.getInstalledPackages()) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Package Manager", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
        
        ActionCard("List Packages", Icons.Filled.Apps, "List all installed packages") {
            NativeSystemServiceEngine.getInstalledPackages()
        }
        ActionCard("Clear Data", Icons.Filled.Clear, "Clear app data") {
            NativeSystemServiceEngine.clearPackageData("com.example.app")
        }
        ActionCard("Force Stop", Icons.Filled.Bolt, "Force stop app") {
            NativeSystemServiceEngine.forceStopPackage("com.example.app")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionCard(
    title: String,
    icon: ImageVector,
    description: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1E1E),
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(icon, contentDescription = title, tint = Color(0xFFBB86FC), size = 24.dp)
                Column {
                    Text(title, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color.White)
                    Text(description, fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
                }
            }
            Icon(Icons.Filled.FastForward, contentDescription = "Execute", tint = Color.White.copy(alpha = 0.4f), size = 18.dp)
        }
    }
}
