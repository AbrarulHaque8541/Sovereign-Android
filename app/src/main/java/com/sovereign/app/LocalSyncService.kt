package com.sovereign.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.sovereign.app.SovereignApplication.Companion.backgroundScope
import com.sovereign.app.SovereignApplication.Companion.dataStore
import com.sovereign.app.SovereignApplication.Companion.getServerEnabled
import com.sovereign.app.SovereignApplication.Companion.getServerPort
import com.sovereign.app.SovereignApplication.Companion.getServerAuthToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class LocalSyncService : Service() {
    private const val TAG = "LocalSyncService"
    private const val CHANNEL_ID = "sovereign_sync_channel"
    private const val NOTIFICATION_ID = 1002

    private var serverJob: Job? = null
    private var settingsJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        Log.i(TAG, "LocalSyncService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: "START"
        
        when (action) {
            "START" -> {
                if (getServerEnabled()) {
                    startServer()
                }
            }
            "STOP" -> {
                stopServer()
            }
            "RESTART" -> {
                stopServer()
                if (getServerEnabled()) {
                    startServer()
                }
            }
        }
        
        return START_STICKY
    }

    private fun startServer() {
        if (serverJob?.isActive == true) {
            Log.w(TAG, "Server already running")
            return
        }
        
        serverJob = backgroundScope.launch {
            LocalSyncServer.start(this@LocalSyncService) { component, payload ->
                Log.d(TAG, "UI Reload triggered: $component")
                // Send broadcast to MainActivity for UI update
                val intent = Intent("com.sovereign.UI_RELOAD")
                intent.putExtra("component", component)
                intent.putExtra("payload", payload)
                sendBroadcast(intent)
            }
        }
        
        // Watch for settings changes
        settingsJob = backgroundScope.launch {
            val prevPort = getServerPort()
            val prevEnabled = getServerEnabled()
            
            dataStore.data.collect { prefs ->
                val currentPort = prefs[SovereignApplication.Companion.KEY_SERVER_PORT] ?: 8000
                val currentEnabled = prefs[SovereignApplication.Companion.KEY_SERVER_ENABLED] ?: true
                
                if (currentPort != prevPort || currentEnabled != prevEnabled) {
                    Log.i(TAG, "Server settings changed, restarting...")
                    restartServer()
                }
            }
        }
        
        // Show foreground notification
        val notification = buildNotification("Running on port $getServerPort()", "Tap to open")
        startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        
        Log.i(TAG, "LocalSyncService started on port $getServerPort()")
    }

    private fun stopServer() {
        settingsJob?.cancel()
        settingsJob = null
        LocalSyncServer.stop()
        serverJob?.cancel()
        serverJob = null
        stopForeground(true)
        Log.i(TAG, "LocalSyncService stopped")
    }

    private fun restartServer() {
        stopServer()
        if (getServerEnabled()) {
            startServer()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Sovereign Sync",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Local HTTP sync and hot-reload server"
                setShowBadge(false)
                lockscreenVisibility = NotificationCompat.VISIBILITY_SECRET
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(title: String, text: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    override fun onDestroy() {
        stopServer()
        super.onDestroy()
        Log.i(TAG, "LocalSyncService destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null
    
    companion object {
        fun createStartIntent(context: Context): Intent {
            return Intent(context, LocalSyncService::class.java).apply {
                action = "START"
            }
        }
        
        fun createStopIntent(context: Context): Intent {
            return Intent(context, LocalSyncService::class.java).apply {
                action = "STOP"
            }
        }
        
        fun createRestartIntent(context: Context): Intent {
            return Intent(context, LocalSyncService::class.java).apply {
                action = "RESTART"
            }
        }
    }
}