package com.sovereign.app

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class SovereignApplication : Application() {

    companion object {
        const val PREFS_NAME = "sovereign_prefs"

        // Capture Settings
        private const val KEY_CAPTURE_ENABLED = "capture_enabled"
        private const val KEY_CAPTURE_RESOLUTION = "capture_resolution"
        private const val KEY_CAPTURE_FPS = "capture_fps"
        private const val KEY_CAPTURE_BITRATE = "capture_bitrate"
        private const val KEY_AUDIO_SOURCE = "audio_source"
        private const val KEY_AUDIO_SAMPLE_RATE = "audio_sample_rate"

        // Server Settings
        private const val KEY_SERVER_PORT = "server_port"
        private const val KEY_SERVER_ENABLED = "server_enabled"
        private const val KEY_SERVER_AUTH_TOKEN = "server_auth_token"

        // Model Settings
        private const val KEY_MODEL_PATH = "model_path"
        private const val KEY_MODEL_ENABLED = "model_enabled"
        private const val KEY_MODEL_THREADS = "model_threads"

        // Security
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val KEY_ENCRYPTION_ENABLED = "encryption_enabled"

        // Storage
        private const val KEY_STORAGE_PATH = "storage_path"
        private const val KEY_MAX_STORAGE_GB = "max_storage_gb"

        // State
        private const val KEY_LAST_CAPTURE_TIME = "last_capture_time"
        private const val KEY_TOTAL_CAPTURES = "total_captures"
        private const val KEY_TOTAL_BYTES = "total_bytes"

        private lateinit var sharedPrefs: SharedPreferences

        fun initPrefs(context: Context) {
            sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }

        fun isCaptureEnabled(): Boolean = sharedPrefs.getBoolean(KEY_CAPTURE_ENABLED, false)
        fun setCaptureEnabled(enabled: Boolean) {
            sharedPrefs.edit().putBoolean(KEY_CAPTURE_ENABLED, enabled).apply()
        }

        fun getCaptureResolution(): String = sharedPrefs.getString(KEY_CAPTURE_RESOLUTION, "1920x1080") ?: "1920x1080"
        fun setCaptureResolution(res: String) {
            sharedPrefs.edit().putString(KEY_CAPTURE_RESOLUTION, res).apply()
        }

        fun getCaptureFps(): Int = sharedPrefs.getInt(KEY_CAPTURE_FPS, 30)
        fun setCaptureFps(fps: Int) {
            sharedPrefs.edit().putInt(KEY_CAPTURE_FPS, fps).apply()
        }

        fun getCaptureBitrate(): Int = sharedPrefs.getInt(KEY_CAPTURE_BITRATE, 8000000)
        fun setCaptureBitrate(bitrate: Int) {
            sharedPrefs.edit().putInt(KEY_CAPTURE_BITRATE, bitrate).apply()
        }

        fun getAudioSource(): Int = sharedPrefs.getInt(KEY_AUDIO_SOURCE, 7)
        fun setAudioSource(source: Int) {
            sharedPrefs.edit().putInt(KEY_AUDIO_SOURCE, source).apply()
        }

        fun getAudioSampleRate(): Int = sharedPrefs.getInt(KEY_AUDIO_SAMPLE_RATE, 48000)
        fun setAudioSampleRate(rate: Int) {
            sharedPrefs.edit().putInt(KEY_AUDIO_SAMPLE_RATE, rate).apply()
        }

        fun isServerEnabled(): Boolean = sharedPrefs.getBoolean(KEY_SERVER_ENABLED, true)
        fun setServerEnabled(enabled: Boolean) {
            sharedPrefs.edit().putBoolean(KEY_SERVER_ENABLED, enabled).apply()
        }

        fun getServerPort(): Int = sharedPrefs.getInt(KEY_SERVER_PORT, 8000)
        fun setServerPort(port: Int) {
            sharedPrefs.edit().putInt(KEY_SERVER_PORT, port).apply()
        }

        fun getServerAuthToken(): String = sharedPrefs.getString(KEY_SERVER_AUTH_TOKEN, "") ?: ""
        fun setServerAuthToken(token: String) {
            sharedPrefs.edit().putString(KEY_SERVER_AUTH_TOKEN, token).apply()
        }

        fun isModelEnabled(): Boolean = sharedPrefs.getBoolean(KEY_MODEL_ENABLED, false)
        fun setModelEnabled(enabled: Boolean) {
            sharedPrefs.edit().putBoolean(KEY_MODEL_ENABLED, enabled).apply()
        }

        fun getModelPath(): String = sharedPrefs.getString(KEY_MODEL_PATH, "") ?: ""
        fun setModelPath(path: String) {
            sharedPrefs.edit().putString(KEY_MODEL_PATH, path).apply()
        }

        fun getModelThreads(): Int = sharedPrefs.getInt(KEY_MODEL_THREADS, 4)
        fun setModelThreads(threads: Int) {
            sharedPrefs.edit().putInt(KEY_MODEL_THREADS, threads).apply()
        }

        fun isBiometricEnabled(): Boolean = sharedPrefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)
        fun setBiometricEnabled(enabled: Boolean) {
            sharedPrefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
        }

        fun isEncryptionEnabled(): Boolean = sharedPrefs.getBoolean(KEY_ENCRYPTION_ENABLED, false)
        fun setEncryptionEnabled(enabled: Boolean) {
            sharedPrefs.edit().putBoolean(KEY_ENCRYPTION_ENABLED, enabled).apply()
        }

        fun getStoragePath(): String = sharedPrefs.getString(KEY_STORAGE_PATH, "") ?: ""
        fun setStoragePath(path: String) {
            sharedPrefs.edit().putString(KEY_STORAGE_PATH, path).apply()
        }

        fun getMaxStorageGb(): Long = sharedPrefs.getLong(KEY_MAX_STORAGE_GB, 10L)
        fun setMaxStorageGb(gb: Long) {
            sharedPrefs.edit().putLong(KEY_MAX_STORAGE_GB, gb).apply()
        }

        fun getTotalCaptures(): Long = sharedPrefs.getLong(KEY_TOTAL_CAPTURES, 0L)
        fun getTotalBytes(): Long = sharedPrefs.getLong(KEY_TOTAL_BYTES, 0L)
        fun incrementStats(bytes: Long) {
            sharedPrefs.edit()
                .putLong(KEY_LAST_CAPTURE_TIME, System.currentTimeMillis())
                .putLong(KEY_TOTAL_CAPTURES, sharedPrefs.getLong(KEY_TOTAL_CAPTURES, 0L) + 1L)
                .putLong(KEY_TOTAL_BYTES, sharedPrefs.getLong(KEY_TOTAL_BYTES, 0L) + bytes)
                .apply()
        }
    }

    override fun onCreate() {
        super.onCreate()
        initPrefs(this)
        AppScope.init()
    }
}

object AppScope {
    private var job: Job? = null

    fun init() {
        job = Job()
    }

    val backgroundScope: CoroutineScope
        get() = CoroutineScope(Dispatchers.IO + (job ?: Job()))

    fun launch(block: suspend () -> Unit) {
        backgroundScope.launch { block() }
    }

    fun shutdown() {
        job?.cancel()
        job = null
    }
}