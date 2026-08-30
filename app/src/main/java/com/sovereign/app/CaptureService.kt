package com.sovereign.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.hardware.display.VirtualDisplay
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
class CaptureService : Service() {
    private val TAG = "CaptureService"
    private val CHANNEL_ID = "sovereign_capture_channel"
    private val NOTIFICATION_ID = 1001

    private val mediaProjectionManager by lazy { getSystemService(MediaProjectionManager::class.java) }
    private val windowManager by lazy { getSystemService(WindowManager::class.java) }
    private val notificationManager by lazy { getSystemService(NotificationManager::class.java) }
    private val powerManager: PowerManager by lazy { getSystemService(PowerManager::class.java) }

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var mediaCodec: MediaCodec? = null
    private var mediaRecorder: MediaRecorder? = null
    private var surface: Surface? = null
    private var outputFile: java.io.File? = null

    private val isCapturing = AtomicBoolean(false)
    private val isPaused = AtomicBoolean(false)
    private val overlayView = AtomicReference<View?>(null)
    private var overlayParams: WindowManager.LayoutParams? = null

    private var captureJob: Job? = null
    private var statsJob: Job? = null
    private var startTime = 0L
    private var frameCount = 0L
    private var bytesWritten = 0L

    private var videoFormat: MediaFormat? = null

    private val prefs: SharedPreferences by lazy {
        getSharedPreferences(SovereignApplication.PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val captureResolution: String
        get() = prefs.getString("capture_resolution", "1920x1080") ?: "1920x1080"

    private val captureFps: Int
        get() = prefs.getInt("capture_fps", 30)

    private val captureBitrate: Int
        get() = prefs.getInt("capture_bitrate", 8000000)

    private val audioSource: Int
        get() = prefs.getInt("audio_source", 7)

    private val audioSampleRate: Int
        get() = prefs.getInt("audio_sample_rate", 48000)

    private val serviceScope = CoroutineScope(Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return START_STICKY

        when (action) {
            ACTION_START -> {
                val projection = mediaProjection
                if (projection != null) {
                    startCapture(projection)
                }
            }
            ACTION_STOP -> stopCapture()
            ACTION_PAUSE -> pauseCapture()
            ACTION_RESUME -> resumeCapture()
            ACTION_SHOW_OVERLAY -> showOverlay()
            ACTION_HIDE_OVERLAY -> hideOverlay()
            ACTION_UPDATE_OVERLAY -> {
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

        val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())
        val storageDir = android.os.Environment.getExternalStorageDirectory()
        val dir = java.io.File(storageDir, "Sovereign/Captures")
        dir.mkdirs()
        outputFile = java.io.File(dir, "capture_$timestamp.mp4")

        initHevcVideoEncoder()
        showOverlay()

        statsJob = serviceScope.launch {
            while (isCapturing.get()) {
                delay(1000)
                updateOverlay("REC ${formatDuration(System.currentTimeMillis() - startTime)} | ${formatBytes(bytesWritten)} | ${frameCount}f")
            }
        }

        Log.i(TAG, "Capture started: ${outputFile?.absolutePath}")
    }

    private fun initHevcVideoEncoder() {
        try {
            val resolution = captureResolution
            val parts = resolution.split("x")
            val width = parts[0].toInt()
            val height = parts[1].toInt()
            val fps = captureFps
            val bitrate = captureBitrate

            videoFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_HEVC, width, height)
            videoFormat?.setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
            videoFormat?.setInteger(MediaFormat.KEY_FRAME_RATE, fps)
            videoFormat?.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            videoFormat?.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            videoFormat?.setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.HEVCProfileMain)
            videoFormat?.setInteger(MediaFormat.KEY_LEVEL, 93)

            mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            mediaCodec?.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            surface = mediaCodec?.createInputSurface()
            mediaCodec?.start()
            createVirtualDisplay()

            Log.i(TAG, "HEVC encoder initialized: ${width}x$height @ ${fps}fps, ${bitrate}bps")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to init HEVC encoder, falling back to AVC", e)
            initAvcVideoEncoder()
        }
    }

    private fun initAvcVideoEncoder() {
        try {
            val resolution = captureResolution
            val parts = resolution.split("x")
            val width = parts[0].toInt()
            val height = parts[1].toInt()
            val fps = captureFps
            val bitrate = captureBitrate

            val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height)
            format.setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
            format.setInteger(MediaFormat.KEY_FRAME_RATE, fps)
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)

            mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            mediaCodec?.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            surface = mediaCodec?.createInputSurface()
            mediaCodec?.start()
            createVirtualDisplay()

            Log.i(TAG, "AVC fallback encoder initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to init AVC encoder", e)
            stopCapture()
        }
    }

    private fun initOpusAudioRecorder() {
        try {
            val sampleRate = audioSampleRate
            val channelConfig = AudioFormat.CHANNEL_IN_STEREO
            val audioEncoding = AudioFormat.ENCODING_PCM_16BIT
            val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioEncoding) * 4

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
        val parts = resolution.split("x")
        val width = parts[0].toInt()
        val height = parts[1].toInt()
        val density = resources.displayMetrics.densityDpi

        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "SovereignCapture",
            width, height, density,
            0,
            surface,
            null, null
        )

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

        hideForeground()
        Log.i(TAG, "Capture stopped. Frames: $frameCount, Bytes: $bytesWritten, Duration: ${formatDuration(System.currentTimeMillis() - startTime)}")
    }

    private fun pauseCapture() {
        if (isCapturing.get() && !isPaused.getAndSet(true)) {
            isPaused.set(true)
            mediaRecorder?.pause()
            updateOverlay("PAUSED")
            Log.i(TAG, "Capture paused")
        }
    }

    private fun resumeCapture() {
        if (isCapturing.get() && isPaused.getAndSet(false)) {
            isPaused.set(false)
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
        if (overlayView.get() == null) {
            createOverlayView()
        }
        val view = overlayView.get() ?: return
        try {
            if (view.parent == null) {
                windowManager.addView(view, overlayParams)
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

    private fun formatBytes(bytes: Long): String {
        val kb = bytes / 1024
        val mb = kb / 1024
        return when {
            mb > 0 -> "${mb}MB"
            kb > 0 -> "${kb}KB"
            else -> "${bytes}B"
        }
    }

    override fun onDestroy() {
        stopCapture()
        hideOverlay()
        statsJob?.cancel()
        captureJob?.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "START_CAPTURE"
        const val ACTION_STOP = "STOP_CAPTURE"
        const val ACTION_PAUSE = "PAUSE_CAPTURE"
        const val ACTION_RESUME = "RESUME_CAPTURE"
        const val ACTION_SHOW_OVERLAY = "SHOW_OVERLAY"
        const val ACTION_HIDE_OVERLAY = "HIDE_OVERLAY"
        const val ACTION_UPDATE_OVERLAY = "UPDATE_OVERLAY"
        const val EXTRA_MEDIA_PROJECTION = "media_projection"

        fun createCaptureIntent(context: Context): Intent {
            return Intent(context, CaptureService::class.java).apply {
                action = ACTION_START
            }
        }

        fun createStopIntent(context: Context): Intent {
            return Intent(context, CaptureService::class.java).apply {
                action = ACTION_STOP
            }
        }
    }
}