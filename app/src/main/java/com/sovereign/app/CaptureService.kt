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
import android.os.PowerManager
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Surface
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
class CaptureService : Service() {
    private const val TAG = "CaptureService"
    private const val CHANNEL_ID = "sovereign_capture_channel"
    private const val NOTIFICATION_ID = 1001
    private const val OVERLAY_REQUEST_CODE = 1001

    private val mediaProjectionManager by lazy { getSystemService(MediaProjectionManager::class.java) }
    private val windowManager by lazy { getSystemService(WindowManager::class.java) }
    private val notificationManager by lazy { getSystemService(NotificationManager::class.java) }
    private val powerManager: PowerManager by lazy { getSystemService(PowerManager::class.java) }

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: android.media.projection.VirtualDisplay? = null
    private var mediaCodec: MediaCodec? = null
    private var mediaRecorder: MediaRecorder? = null
    private var surface: Surface? = null
    private var outputFile: java.io.File? = null
    private var fileOutputStream: java.io.FileOutputStream? = null
    private var fileChannel: java.nio.channels.FileChannel? = null

    private val isCapturing = java.util.concurrent.atomic.AtomicBoolean(false)
    private val isPaused = java.util.concurrent.atomic.AtomicBoolean(false)
    private val overlayView = java.util.concurrent.atomic.AtomicReference<View?>(null)
    private var overlayParams: android.view.WindowManager.LayoutParams? = null

    private var captureJob: kotlinx.coroutines.Job? = null
    private var statsJob: kotlinx.coroutines.Job? = null
    private var startTime = 0L
    private var frameCount = 0L
    private var bytesWritten = 0L

    private var videoFormat: android.media.MediaFormat? = null
    private var audioFormat: android.media.MediaFormat? = null
    private var videoTrackIndex = -1
    private var audioTrackIndex = -1

    // SharedPreferences for settings (migrated from DataStore)
    private val prefs: android.content.SharedPreferences by lazy {
        android.preference.PreferenceManager.getDefaultSharedPreferences(this)
    }

    private val captureResolution: String
        get = prefs.getString("capture_resolution", "1920x1080") ?: "1920x1080"

    private val captureFps: Int
        get = prefs.getInt("capture_fps", 30)

    private val captureBitrate: Int
        get = prefs.getInt("capture_bitrate", 8000000)

    private val audioSource: Int
        get = prefs.getInt("audio_source", 7) // CAMCORDER

    private val audioSampleRate: Int
        get = prefs.getInt("audio_sample_rate", 48000)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
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
            "UPDATE_OVERLAY" -> {
                val text = intent.getStringExtra("text")
                if (text != null) updateOverlay(text)
            }
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

        // Create output file
        val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())
        val storageDir = android.os.Environment.getExternalStorageDirectory()
        val dir = java.io.File(storageDir, "Sovereign/Captures")
        dir.mkdirs()
        outputFile = java.io.File(dir, "capture_$timestamp.mp4")

        // Initialize HEVC video encoder
        initHevcVideoEncoder()

        // Show overlay
        showOverlay()

        // Start stats update job using backgroundScope
        statsJob = backgroundScope.launch {
            while (isCapturing.get()) {
                kotlinx.coroutines.delay(1000)
                val size = bytesWritten
                updateOverlay("REC ${formatDuration(System.currentTimeMillis() - startTime)} | ${formatBytes(size)} | ${frameCount}f")
            }
        }

        Log.i(TAG, "Capture started: ${outputFile?.absolutePath}")
    }

    private fun initHevcVideoEncoder() {
        try {
            val resolution = captureResolution
            val (width, height) = resolution.split("x").map { it.toInt() }
            val fps = captureFps
            val bitrate = captureBitrate

            // HEVC/H.265 Configuration
            videoFormat = android.media.MediaFormat.createVideoFormat(android.media.MediaFormat.MIMETYPE_VIDEO_HEVC, width, height)
            videoFormat?.setInteger(android.media.MediaFormat.KEY_BIT_RATE, bitrate)
            videoFormat?.setInteger(android.media.MediaFormat.KEY_FRAME_RATE, fps)
            videoFormat?.setInteger(android.media.MediaFormat.KEY_COLOR_FORMAT, android.media.MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            videoFormat?.setInteger(android.media.MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            videoFormat?.setInteger(android.media.MediaFormat.KEY_PROFILE, android.media.MediaCodecInfo.CodecProfileLevel.HEVCProfileMain)
            videoFormat?.setInteger(android.media.MediaFormat.KEY_LEVEL, 93) // Level 3.1

            // Find HEVC encoder
            val codecList = android.media.MediaCodecList(android.media.MediaCodecList.REGULAR_CODECS)
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

            mediaCodec = MediaCodec.createByCodecName(codecName)
            mediaCodec?.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
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
            val resolution = captureResolution
            val (width, height) = resolution.split("x").map { it.toInt() }
            val fps = captureFps
            val bitrate = captureBitrate

            val format = android.media.MediaFormat.createVideoFormat(android.media.MediaFormat.MIMETYPE_VIDEO_AVC, width, height)
            format.setInteger(android.media.MediaFormat.KEY_BIT_RATE, bitrate)
            format.setInteger(android.media.MediaFormat.KEY_FRAME_RATE, fps)
            format.setInteger(android.media.MediaFormat.KEY_COLOR_FORMAT, android.media.MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            format.setInteger(android.media.MediaFormat.KEY_I_FRAME_INTERVAL, 1)

            mediaCodec = MediaCodec.createEncoderByType(android.media.MediaFormat.MIMETYPE_VIDEO_AVC)
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
            val sampleRate = audioSampleRate
            val channelConfig = android.content.res.AudioFormat.CHANNEL_IN_STEREO
            val audioFormat = android.content.res.AudioFormat.ENCODING_PCM_16BIT
            val bufferSize = android.media.MediaRecorder.getMinBufferSize(sampleRate, channelConfig, audioFormat) * 4

            mediaRecorder = MediaRecorder().apply {
                setAudioSource(audioSource)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(sampleRate)
                setOutputFile(outputFile!!.absolutePath)
            }

            mediaRecorder?.prepare()
            mediaRecorder?.start()

            Log.i(TAG, "Audio recorder initialized: ${sampleRate}Hz AAC")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to init audio recorder", e)
        }
    }

    private fun createVirtualDisplay() {
        val resolution = captureResolution
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

        hideForeground()
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

    private fun hideForeground() {
        stopForeground(true)
        stopSelf()
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
        overlayParams = android.view.WindowManager.LayoutParams(
            android.view.WindowManager.LayoutParams.WRAP_CONTENT,
            android.view.WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                android.view.WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                android.view.WindowManager.LayoutParams.TYPE_PHONE
            },
            android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                android.view.WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            android.graphics.PixelFormat.TRANSLUCENT
        ).apply {
            gravity = android.view.Gravity.TOP or android.view.Gravity.END
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
        return if (hours > 0) java.lang.String.format("%02d:%02d:%02d", hours, minutes, secs)
            else java.lang.String.format("%02d:%02d", minutes, secs)
    }

    private fun formatBytes(bytes: Long): String {
        val kb = bytes / 1024
        val mb = kb / 1024
        if (mb > 0) return "${mb}MB"
        else if (kb > 0) return "${kb}KB"
        else return "${bytes}B"
    }

    override fun onDestroy() {
        stopCapture()
        hideOverlay()
        statsJob?.cancel()
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