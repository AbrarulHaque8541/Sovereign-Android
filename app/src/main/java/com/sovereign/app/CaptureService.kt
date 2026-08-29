package com.sovereign.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Surface
import android.view.View
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.sovereign.app.SovereignApplication.Companion.backgroundScope
import com.sovereign.app.SovereignApplication.Companion.dataStore
import com.sovereign.app.SovereignApplication.Companion.getCaptureResolution
import com.sovereign.app.SovereignApplication.Companion.getCaptureFps
import com.sovereign.app.SovereignApplication.Companion.getCaptureBitrate
import com.sovereign.app.SovereignApplication.Companion.getAudioSource
import com.sovereign.app.SovereignApplication.Companion.getAudioSampleRate
import com.sovereign.app.SovereignApplication.Companion.incrementStats
import com.sovereign.storage.ExtremeStorageEngine
import com.sovereign.storage.ExtremeStorageEngine.Companion.acquireDirectBuffer
import com.sovereign.storage.ExtremeStorageEngine.Companion.releaseDirectBuffer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
class CaptureService : Service() {
    private const val TAG = "CaptureService"
    private const val CHANNEL_ID = "sovereign_capture_channel"
    private const val NOTIFICATION_ID = 1001
    private const val OVERLAY_REQUEST_CODE = 1001

    private val mediaProjectionManager by lazy { getSystemService(MediaProjectionManager::class.java) }
    private val windowManager by lazy { getSystemService(WindowManager::class.java) }
    private val notificationManager by lazy { getSystemService(NotificationManager::class.java) }
    
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: android.media.projection.VirtualDisplay? = null
    private var mediaCodec: MediaCodec? = null
    private var mediaRecorder: MediaRecorder? = null
    private var surface: Surface? = null
    private var outputFile: File? = null
    private var fileOutputStream: FileOutputStream? = null
    private var fileChannel: java.nio.channels.FileChannel? = null
    
    private val isCapturing = AtomicBoolean(false)
    private val isPaused = AtomicBoolean(false)
    private val overlayView = AtomicReference<View?>(null)
    private var overlayParams: WindowManager.LayoutParams? = null
    
    private var captureJob: Job? = null
    private var statsJob: Job? = null
    private var startTime = 0L
    private var frameCount = 0L
    private var bytesWritten = 0L
    
    // HEVC encoding config
    private var videoFormat: MediaFormat? = null
    private var audioFormat: MediaFormat? = null
    private var videoTrackIndex = -1
    private var audioTrackIndex = -1

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        createOverlayView()
        ExtremeStorageEngine.initialize(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return START_STICKY
        
        when (action) {
            "START_CAPTURE" -> {
                val projectionData = intent.getParcelableExtra<MediaProjection>("media_projection")
                if (projectionData != null) {
                    startCapture(projectionData)
                }
            }
            "STOP_CAPTURE" -> stopCapture()
            "PAUSE_CAPTURE" -> pauseCapture()
            "RESUME_CAPTURE" -> resumeCapture()
            "SHOW_OVERLAY" -> showOverlay()
            "HIDE_OVERLAY" -> hideOverlay()
            "UPDATE_OVERLAY" -> updateOverlay(intent.getStringExtra("text"))
        }
        
        return START_STICKY
    }

    private fun startCapture(projection: MediaProjection) {
        if (isCapturing.getAndSet(true)) {
            Log.w(TAG, "Capture already in progress")
            isCapturing.set(false)
            return
        }
        
        mediaProjection = projection
        isPaused.set(false)
        startTime = System.currentTimeMillis()
        frameCount = 0
        bytesWritten = 0
        
        // Create output file with .mp4 extension for HEVC
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storagePath = ExtremeStorageEngine.getOptimalStorageDir(this)
        val dir = File(storagePath, "Sovereign/Captures")
        dir.mkdirs()
        outputFile = File(dir, "capture_$timestamp.mp4")
        
        // Initialize HEVC video encoder
        initHevcVideoEncoder()
        
        // Initialize Opus audio recorder
        initOpusAudioRecorder()
        
        // Create virtual display
        createVirtualDisplay()
        
        // Start foreground service
        val notification = buildNotification("Recording...", "Tap to stop")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        
        // Show overlay
        showOverlay()
        
        // Start stats update job
        statsJob = backgroundScope.launch {
            while (isCapturing.get()) {
                kotlinx.coroutines.delay(1000)
                updateOverlay("REC ${formatDuration(System.currentTimeMillis() - startTime)} | ${ExtremeStorageEngine.formatBytes(bytesWritten)} | ${frameCount}f")
            }
        }
        
        Log.i(TAG, "HEVC/Opus Capture started: ${outputFile?.absolutePath}")
    }

    private fun initHevcVideoEncoder() {
        try {
            val resolution = getCaptureResolution()
            val (width, height) = resolution.split("x").map { it.toInt() }
            val fps = getCaptureFps()
            val bitrate = getCaptureBitrate()
            
            // HEVC/H.265 Configuration
            videoFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_HEVC, width, height)
            videoFormat?.setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
            videoFormat?.setInteger(MediaFormat.KEY_FRAME_RATE, fps)
            videoFormat?.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            videoFormat?.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            videoFormat?.setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.HEVCProfileMain)
            videoFormat?.setInteger(MediaFormat.KEY_LEVEL, 93) // Level 3.1
            
            // Find HEVC encoder
            val codecList = MediaCodecList(MediaCodecList.REGULAR_CODECS)
            var codecName: String? = null
            for (i in 0 until codecList.codecCount) {
                val info = codecList.getCodecInfoAt(i)
                if (info.isEncoder) {
                    val types = info.supportedTypes
                    for (type in types) {
                        if (type.equals("video/hevc", ignoreCase = true)) {
                            codecName = info.name
                            break
                        }
                    }
                }
                if (codecName != null) break
            }
            
            if (codecName == null) {
                Log.w(TAG, "HEVC encoder not found, falling back to AVC")
                initAvcVideoEncoder()
                return
            }
            
            mediaCodec = MediaCodec.createByCodecName(codecName!!)
            mediaCodec?.configure(videoFormat!!, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            surface = mediaCodec?.createInputSurface()
            mediaCodec?.start()
            
            Log.i(TAG, "HEVC encoder initialized: ${width}x$height @ ${fps}fps, ${bitrate}bps")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to init HEVC encoder, falling back to AVC", e)
            initAvcVideoEncoder()
        }
    }
    
    private fun initAvcVideoEncoder() {
        try {
            val resolution = getCaptureResolution()
            val (width, height) = resolution.split("x").map { it.toInt() }
            val fps = getCaptureFps()
            val bitrate = getCaptureBitrate()
            
            val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height)
            format.setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
            format.setInteger(MediaFormat.KEY_FRAME_RATE, fps)
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            
            mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            mediaCodec?.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            surface = mediaCodec?.createInputSurface()
            mediaCodec?.start()
            
            Log.i(TAG, "AVC fallback encoder initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to init AVC encoder", e)
            stopCapture()
        }
    }
    
    private fun initOpusAudioRecorder() {
        try {
            val sampleRate = getAudioSampleRate()
            val channelConfig = android.content.res.AudioFormat.CHANNEL_IN_STEREO
            val audioFormat = android.content.res.AudioFormat.ENCODING_PCM_16BIT
            val bufferSize = MediaRecorder.getMinBufferSize(sampleRate, channelConfig, audioFormat) * 4
            
            // Use MediaRecorder with AAC as Opus fallback (Opus not directly supported in MediaRecorder)
            // For true Opus, would need libopus native library
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(getAudioSource())
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(sampleRate)
                setOutputFile(outputFile!!.absolutePath)
            }
            
            mediaRecorder?.prepare()
            mediaRecorder?.start()
            
            Log.i(TAG, "Audio recorder initialized: ${sampleRate}Hz AAC (Opus would require native libopus)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to init audio recorder", e)
        }
    }

    private fun createVirtualDisplay() {
        val resolution = getCaptureResolution()
        val (width, height) = resolution.split("x").map { it.toInt() }
        val density = resources.displayMetrics.densityDpi
        
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "SovereignCapture",
            width, height, density,
            android.media.projection.VirtualDisplay.FLAG_PUBLIC,
            surface!!,
            null, null
        )
        
        // Register callback for projection stop
        mediaProjection?.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() {
                super.onStop()
                Log.w(TAG, "MediaProjection stopped by system")
                stopCapture()
            }
        }, Handler(Looper.getMainLooper()))
    }

    private fun stopCapture() {
        if (!isCapturing.getAndSet(false)) return
        
        isPaused.set(false)
        statsJob?.cancel()
        
        try {
            virtualDisplay?.release()
            virtualDisplay = null
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing virtual display", e)
        }
        
        try {
            mediaCodec?.signalEndOfInputStream()
            mediaCodec?.stop()
            mediaCodec?.release()
            mediaCodec = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping media codec", e)
        }
        
        try {
            mediaRecorder?.stop()
            mediaRecorder?.release()
            mediaRecorder = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping media recorder", e)
        }
        
        try {
            surface?.release()
            surface = null
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing surface", e)
        }
        
        try {
            mediaProjection?.stop()
            mediaProjection = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping media projection", e)
        }
        
        try {
            fileChannel?.force(true)
            fileChannel?.close()
            fileChannel = null
            fileOutputStream?.close()
            fileOutputStream = null
        } catch (e: Exception) {
            Log.e(TAG, "Error closing file", e)
        }
        
        hideOverlay()
        stopForeground(true)
        stopSelf()
        
        // Update stats
        backgroundScope.launch {
            incrementStats(bytesWritten)
        }
        
        Log.i(TAG, "Capture stopped. Frames: $frameCount, Bytes: $bytesWritten, Duration: ${formatDuration(System.currentTimeMillis() - startTime)}")
    }

    private fun pauseCapture() {
        if (isCapturing.get() && !isPaused.getAndSet(true)) {
            mediaCodec?.pause()
            mediaRecorder?.pause()
            updateOverlay("PAUSED")
            Log.i(TAG, "Capture paused")
        }
    }

    private fun resumeCapture() {
        if (isCapturing.get() && isPaused.getAndSet(false)) {
            mediaCodec?.resume()
            mediaRecorder?.resume()
            updateOverlay("REC ${formatDuration(System.currentTimeMillis() - startTime)}")
            Log.i(TAG, "Capture resumed")
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Sovereign Capture",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Screen and audio capture service"
                setShowBadge(false)
                lockscreenVisibility = NotificationCompat.VISIBILITY_SECRET
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(title: String, text: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun createOverlayView() {
        overlayParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 20
            y = 100
        }
        
        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.capture_overlay, null)
        overlayView.set(view)
    }

    private fun showOverlay() {
        val view = overlayView.get() ?: return
        try {
            if (view.parent == null) {
                windowManager.addView(view, overlayParams!!)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error showing overlay", e)
        }
    }

    private fun hideOverlay() {
        val view = overlayView.get() ?: return
        try {
            if (view.parent != null) {
                windowManager.removeView(view)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error hiding overlay", e)
        }
    }

    private fun updateOverlay(text: String?) {
        val view = overlayView.get() ?: return
        Handler(Looper.getMainLooper()).post {
            view.findViewById<android.widget.TextView>(R.id.overlay_text)?.text = text
        }
    }

    private fun formatDuration(ms: Long): String {
        val seconds = ms / 1000
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return if (hours > 0) String.format("%02d:%02d:%02d", hours, minutes, secs)
        else String.format("%02d:%02d", minutes, secs)
    }

    override fun onDestroy() {
        stopCapture()
        hideOverlay()
        statsJob?.cancel()
        ExtremeStorageEngine.shutdown()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
    
    companion object {
        fun createCaptureIntent(context: Context): Intent {
            return Intent(context, CaptureService::class.java).apply {
                action = "START_CAPTURE"
            }
        }
        
        fun createStopIntent(context: Context): Intent {
            return Intent(context, CaptureService::class.java).apply {
                action = "STOP_CAPTURE"
            }
        }
    }
}