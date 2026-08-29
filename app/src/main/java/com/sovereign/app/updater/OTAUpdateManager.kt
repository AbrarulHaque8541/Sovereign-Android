package com.sovereign.app.updater

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

object OTAUpdateManager {
    private const val TAG = "OTAUpdateManager"
    const val CHANNEL_ID = "sovereign_ota_channel"
    private const val NOTIFICATION_ID = 1001
    private const val UPDATE_URL = "https://api.github.com/repos/AbrarulHaque8541/Hermes-1/releases/latest"
    
    @Suppress("UNUSED_PARAMETER")
    fun initialize(context: Context) {
        Log.i(TAG, "OTAUpdateManager initialized")
    }
    
    fun checkForUpdates(context: Context, force: Boolean = false) {
        backgroundScope.launch(Dispatchers.IO) {
            try {
                val url = java.net.URL(UPDATE_URL)
                val connection = url.openConnection()
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                val inputStream = connection.getInputStream()
                val response = inputStream.reader().readText()
                inputStream.close()
                
                val hasUpdate = parseVersionFromResponse(response)
                if (hasUpdate || force) {
                    showUpdateNotification(context, "Update Available", "Tap to download")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Update check failed", e)
            }
        }
    }
    
    private fun parseVersionFromResponse(response: String): Boolean {
        return response.contains("tag_name")
    }
    
    fun downloadUpdate(context: Context) {
        backgroundScope.launch(Dispatchers.IO) {
            showUpdateNotification(context, "Downloading...", "Update in progress")
        }
    }
    
    fun installUpdate(context: Context) {
        // Simplified
    }
    
    fun cancelDownload() {
        // Simplified
    }
    
    private fun showUpdateNotification(context: Context, title: String, text: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setAutoCancel(true)
            .build()
        
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
    }
    
    fun createCheckIntent(context: Context): Intent {
        return Intent(context, OTAUpdateService::class.java).apply {
            action = "CHECK_UPDATES"
        }
    }
    
    fun createDownloadIntent(context: Context): Intent {
        return Intent(context, OTAUpdateService::class.java).apply {
            action = "DOWNLOAD_UPDATE"
        }
    }
}

class OTAUpdateService : Service() {
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return START_STICKY
        
        when (action) {
            "CHECK_UPDATES" -> OTAUpdateManager.checkForUpdates(this, intent.getBooleanExtra("force", false))
            "DOWNLOAD_UPDATE" -> OTAUpdateManager.downloadUpdate(this)
            "INSTALL_UPDATE" -> OTAUpdateManager.installUpdate(this)
            "CANCEL" -> OTAUpdateManager.cancelDownload()
        }
        
        return START_STICKY
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                OTAUpdateManager.CHANNEL_ID,
                "Sovereign OTA Updates",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "OTA update notifications"
                setShowBadge(true)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
    
    override fun onBind(intent: Intent?): android.os.IBinder? = null
    override fun onDestroy() {
        super.onDestroy()
    }
}