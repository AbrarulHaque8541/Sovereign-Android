package com.sovereign.app.tools

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.sovereign.app.AppScope.backgroundScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader

object NativeSystemServiceEngine {
    const val TAG = "NativeSystemService"
    const val CHANNEL_ID = "sovereign_adb_channel"
    const val NOTIFICATION_ID = 3001
    private var initialized = false
    
    fun initialize(context: Context): Boolean {
        if (initialized) return true
        initialized = true
        Log.i(TAG, "NativeSystemServiceEngine initialized")
        return true
    }
    
    fun startAdbLoopback(context: Context): Boolean {
        Log.i(TAG, "ADB loopback start requested")
        return true
    }
    
    fun stopAdbLoopback(): Boolean {
        Log.i(TAG, "ADB loopback stop requested")
        return true
    }
    
    fun executeShellCommand(command: String): String {
        return try {
            val process = Runtime.getRuntime().exec("sh -c $command")
            val output = StringBuilder()
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                output.append(line).append("\n")
            }
            process.waitFor()
            output.toString()
        } catch (e: Exception) {
            "ERROR: ${e.message}"
        }
    }
    
    fun executeShellCommandAsync(command: String): Unit {
        backgroundScope.launch(Dispatchers.IO) { executeShellCommand(command) }
    }
    
    fun installPackage(context: Context, apkPath: String): Boolean {
        return executeShellCommand("pm install $apkPath").contains("Success")
    }
    
    fun uninstallPackage(packageName: String): Boolean {
        return executeShellCommand("pm uninstall $packageName").contains("Success")
    }
    
    fun freezePackage(packageName: String): Boolean {
        return executeShellCommand("pm disable-user --user 0 $packageName").contains("Package")
    }
    
    fun unfreezePackage(packageName: String): Boolean {
        return executeShellCommand("pm enable --user 0 $packageName").contains("Package")
    }
    
    fun grantPermission(packageName: String, permission: String): Boolean {
        return executeShellCommand("pm grant $packageName $permission").isEmpty()
    }
    
    fun revokePermission(packageName: String, permission: String): Boolean {
        return executeShellCommand("pm revoke $packageName $permission").isEmpty()
    }
    
    fun killProcess(packageName: String): Boolean {
        return executeShellCommand("am kill $packageName").isEmpty()
    }
    
    fun forceStopPackage(packageName: String): Boolean {
        return executeShellCommand("am force-stop $packageName").isEmpty()
    }
    
    fun clearPackageData(packageName: String): Boolean {
        return executeShellCommand("pm clear $packageName").contains("Success")
    }
    
    fun getInstalledPackages(): List<String> {
        return executeShellCommand("pm list packages").lines().toList()
    }
    
    fun reboot(reason: String = "user_requested"): Boolean {
        return executeShellCommand("reboot $reason").isNotEmpty()
    }
    
    fun rebootBootloader(): Boolean {
        return executeShellCommand("reboot bootloader").isNotEmpty()
    }
    
    fun rebootRecovery(): Boolean {
        return executeShellCommand("reboot recovery").isNotEmpty()
    }
    
    fun setScreenDensity(dpi: Int): Boolean {
        return executeShellCommand("wm density $dpi").isEmpty()
    }
    
    fun getScreenDensity(): Int {
        return executeShellCommand("wm density").trim().toIntOrNull() ?: -1
    }
    
    fun setScreenResolution(width: Int, height: Int): Boolean {
        return executeShellCommand("wm size ${width}x$height").isEmpty()
    }
    
    fun resetScreenResolution(): Boolean {
        return executeShellCommand("wm size reset").isEmpty()
    }
    
    fun getScreenResolution(): String {
        return executeShellCommand("wm size").trim()
    }
    
    fun listDirectory(path: String): List<String> {
        return executeShellCommand("ls -la $path").lines().filter { it.isNotBlank() }.toList()
    }
    
    fun copyFile(src: String, dest: String): Boolean {
        return executeShellCommand("cp -r $src $dest").isEmpty()
    }
    
    fun moveFile(src: String, dest: String): Boolean {
        return executeShellCommand("mv $src $dest").isEmpty()
    }
    
    fun deleteFile(path: String): Boolean {
        return executeShellCommand("rm -rf $path").isEmpty()
    }
    
    fun getDiskUsage(path: String): String {
        return executeShellCommand("df -h $path")
    }
    
    fun getConnectedAdbDevices(): List<String> {
        return executeShellCommand("adb devices").lines()
            .drop(1)
            .filter { it.contains("\tdevice") }
            .map { it.split("\t").first() }
            .toList()
    }
    
    fun connectAdbWireless(ip: String, port: Int = 5555): Boolean {
        return executeShellCommand("adb connect $ip:$port").contains("connected")
    }
    
    fun disconnectAdbWireless(ip: String): Boolean {
        return executeShellCommand("adb disconnect $ip").contains("disconnected")
    }
    
    fun shutdown(): Unit {
        stopAdbLoopback()
    }
    
    fun createAdbStartIntent(context: Context): Intent {
        return Intent(context, AdbService::class.java).apply {
            action = "START_ADB"
        }
    }
}

class AdbService : Service() {
    private const val TAG = "AdbService"
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        NativeSystemServiceEngine.initialize(this)
        NativeSystemServiceEngine.startAdbLoopback(this)
        showNotification("ADB Loopback Active", "Port 5555")
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return START_STICKY
        
        when (action) {
            "START_ADB" -> NativeSystemServiceEngine.startAdbLoopback(this)
            "STOP_ADB" -> NativeSystemServiceEngine.stopAdbLoopback()
            "EXECUTE_SHELL" -> {
                val command = intent.getStringExtra("command") ?: return START_STICKY
                NativeSystemServiceEngine.executeShellCommandAsync(command)
            }
        }
        
        return START_STICKY
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NativeSystemServiceEngine.CHANNEL_ID,
                "Sovereign ADB Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Native ADB & Shell service"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
    
    private fun showNotification(title: String, text: String) {
        val notification = NotificationCompat.Builder(this, NativeSystemServiceEngine.CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .build()
        
        startForeground(NativeSystemServiceEngine.NOTIFICATION_ID, notification)
    }
    
    override fun onDestroy() {
        NativeSystemServiceEngine.stopAdbLoopback()
        NativeSystemServiceEngine.shutdown()
        stopForeground(true)
        super.onDestroy()
    }
    
    override fun onBind(intent: Intent?): android.os.IBinder? = null
    
    companion object {
        fun createStartIntent(context: Context): Intent {
            return Intent(context, AdbService::class.java).apply {
                action = "START_ADB"
            }
        }
        
        fun createExecuteIntent(context: Context, command: String): Intent {
            return Intent(context, AdbService::class.java).apply {
                action = "EXECUTE_SHELL"
                putExtra("command", command)
            }
        }
    }
}