package com.sovereign.app.tools

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
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

        Row(
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
        Chip(
            onClick = {},
            label = { Text("Normal Reboot", color = Color.White) },
            colors = ChipDefaults.chipColors(
                containerColor = Color(0xFF1E1E1E),
                selectedContainerColor = Color(0xFFBB86FC)
            )
        )
        Chip(
            onClick = {},
            label = { Text("Bootloader Mode", color = Color.White) },
            colors = ChipDefaults.chipColors(
                containerColor = Color(0xFF1E1E1E),
                selectedContainerColor = Color(0xFFBB86FC)
            )
        )
        Chip(
            onClick = {},
            label = { Text("Recovery Mode", color = Color.White) },
            colors = ChipDefaults.chipColors(
                containerColor = Color(0xFF1E1E1E),
                selectedContainerColor = Color(0xFFBB86FC)
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdbTab() {
    val context = LocalContext.current
    var ipInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("ADB Control", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
        TextField(
            value = ipInput,
            onValueChange = { ipInput = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("IP Address", color = Color.White.copy(alpha = 0.6f)) },
            colors = TextFieldDefaults.textFieldColors(
                containerColor = Color(0xFF1E1E1E),
                textColor = Color.White
            ),
            singleLine = true
        )
        Chip(
            onClick = {},
            label = { Text("Connect", color = Color.White) },
            colors = ChipDefaults.chipColors(
                containerColor = Color(0xFF1E1E1E),
                selectedContainerColor = Color(0xFFBB86FC)
            )
        )
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
        Chip(
            onClick = {},
            label = { Text("Check Devices", color = Color.White) },
            colors = ChipDefaults.chipColors(
                containerColor = Color(0xFF1E1E1E),
                selectedContainerColor = Color(0xFFBB86FC)
            )
        )
        Chip(
            onClick = {},
            label = { Text("Flash Boot", color = Color.White) },
            colors = ChipDefaults.chipColors(
                containerColor = Color(0xFF1E1E1E),
                selectedContainerColor = Color(0xFFBB86FC)
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScriptsTab() {
    val context = LocalContext.current
    val scripts = remember { ScriptRunnerUtility.listScripts(context) }

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
            Chip(
                onClick = {},
                label = { Text("Create Script", color = Color.White) },
                colors = ChipDefaults.chipColors(
                    containerColor = Color(0xFF1E1E1E),
                    selectedContainerColor = Color(0xFFBB86FC)
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PackagesTab() {
    val context = LocalContext.current
    val packages = remember { NativeSystemServiceEngine.getInstalledPackages() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Package Manager", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Chip(
            onClick = {},
            label = { Text("List Packages", color = Color.White) },
            colors = ChipDefaults.chipColors(
                containerColor = Color(0xFF1E1E1E),
                selectedContainerColor = Color(0xFFBB86FC)
            )
        )
        Chip(
            onClick = {},
            label = { Text("Clear Data", color = Color.White) },
            colors = ChipDefaults.chipColors(
                containerColor = Color(0xFF1E1E1E),
                selectedContainerColor = Color(0xFFBB86FC)
            )
        )
    }
}
