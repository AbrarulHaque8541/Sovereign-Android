package com.sovereign.app

import android.content.Context
import android.content.Intent
import android.util.Log

object LocalSyncService {
    private const val TAG = "LocalSyncService"
    private var serverPort: Int = 8000
    private var isEnabled: Boolean = true

    fun getServerPort(): Int = serverPort
    fun getServerEnabled(): Boolean = isEnabled

    fun setServerPort(port: Int) {
        this.serverPort = port
        Log.d(TAG, "Server port set to $port")
    }

    fun setServerEnabled(enabled: Boolean) {
        this.isEnabled = enabled
        Log.d(TAG, "Server enabled set to $enabled")
    }

    fun createStartIntent(context: Context): Intent {
        return Intent(context, LocalSyncService::class.java).apply {
            action = "com.sovereign.app.LOCAL_SYNC_START"
            putExtra("server_port", serverPort)
            putExtra("server_enabled", isEnabled)
        }
    }

    fun createStopIntent(context: Context): Intent {
        return Intent(context, LocalSyncService::class.java).apply {
            action = "com.sovereign.app.LOCAL_SYNC_STOP"
        }
    }
}
