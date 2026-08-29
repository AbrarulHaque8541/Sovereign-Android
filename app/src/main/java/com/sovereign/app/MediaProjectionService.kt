package com.sovereign.app

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.projection.MediaProjection
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
class MediaProjectionService : Service() {
    private const val TAG = "MediaProjectionService"
    
    private var mediaProjection: MediaProjection? = null
    private var isBound = false

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "MediaProjectionService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return START_STICKY
        
        when (action) {
            "CREATE_PROJECTION" -> {
                val projectionData = intent.getParcelableExtra<MediaProjection>("media_projection")
                projectionData?.let { projection ->
                    this.mediaProjection = projection
                    // Notify waiting components
                    sendProjectionReady(projection)
                }
            }
            "STOP_PROJECTION" -> {
                mediaProjection?.stop()
                mediaProjection = null
            }
        }
        
        return START_STICKY
    }

    private fun sendProjectionReady(projection: MediaProjection) {
        val intent = Intent("com.sovereign.PROJECTION_READY")
        intent.putExtra("media_projection", projection)
        sendBroadcast(intent)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        mediaProjection?.stop()
        mediaProjection = null
        super.onDestroy()
        Log.d(TAG, "MediaProjectionService destroyed")
    }

    companion object {
        fun createIntent(context: Context): Intent {
            return Intent(context, MediaProjectionService::class.java).apply {
                action = "CREATE_PROJECTION"
            }
        }
        
        fun createStopIntent(context: Context): Intent {
            return Intent(context, MediaProjectionService::class.java).apply {
                action = "STOP_PROJECTION"
            }
        }
    }
}