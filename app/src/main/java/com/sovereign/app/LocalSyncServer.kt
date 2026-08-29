package com.sovereign.app

import android.content.Context
import android.util.Log

object LocalSyncServer {
    private const val TAG = "LocalSyncServer"
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

    fun startServer(context: Context) {
        Log.d(TAG, "Starting local sync server on port $serverPort")
    }

    fun stopServer() {
        Log.d(TAG, "Stopping local sync server")
    }
}