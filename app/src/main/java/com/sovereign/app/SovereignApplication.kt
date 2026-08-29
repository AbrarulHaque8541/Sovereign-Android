package com.sovereign.app

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.registerOnSharedPreferenceChangeListener
import kotlinx.coroutines.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch

class SovereignApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppScope.init(this)
    }

    companion object {
        private const val PREFS_NAME = "sovereign_prefs"

        // Capture Settings
        private val KEY_CAPTURE_ENABLED = "capture_enabled"
        private val KEY_CAPTURE_RESOLUTION = "capture_resolution"
        private val KEY_CAPTURE_FPS = "capture_fps"
        private val KEY_CAPTURE_BITRATE = "capture_bitrate"
        private val KEY_AUDIO_SOURCE = "audio_source"
        private val KEY_AUDIO_SAMPLE_RATE = "audio_sample_rate"

        // Server Settings
        private val KEY_SERVER_PORT = "server_port"
        private val KEY_SERVER_ENABLED = "server_enabled"
        private val KEY_SERVER_AUTH_TOKEN = "server_auth_token"

        // Model Settings
        private val KEY_MODEL_PATH = "model_path"
        private val KEY_MODEL_ENABLED = "model_enabled"
        private val KEY_MODEL_THREADS = "model_threads"

        // Security
        private val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        private val KEY_ENCRYPTION_ENABLED = "encryption_enabled"

        // Storage
        private val KEY_STORAGE_PATH = "storage_path"
        private val KEY_MAX_STORAGE_GB = "max_storage_gb"

        // State
        private val KEY_LAST_CAPTURE_TIME = "last_capture_time"
        private val KEY_TOTAL_CAPTURES = "total_captures"
        private val KEY_TOTAL_BYTES = "total_bytes"

        private lateinit var sharedPrefs: SharedPreferences
        private lateinit var sharedPrefsEdit: SharedPreferences.Editor

        fun initPrefs(context: Context) {
            sharedPrefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)
            sharedPrefsEdit = sharedPrefs.edit()
        }

        val captureEnabledFlow: Flow<Boolean> = sharedPrefs
            .registerOnSharedPreferenceChangeListener { _, key ->
                when (key) {
                    KEY_CAPTURE_ENABLED -> {}
                }
            }
            .map { sharedPrefs.getBoolean(KEY_CAPTURE_ENABLED, false) }

        val serverEnabledFlow: Flow<Boolean> = sharedPrefs
            .registerOnSharedPreferenceChangeListener { _, key ->
                when (key) {
                    KEY_SERVER_ENABLED -> {}
                }
            }
            .map { sharedPrefs.getBoolean(KEY_SERVER_ENABLED, true) }

        val serverPortFlow: Flow<Int> = sharedPrefs
            .registerOnSharedPreferenceChangeListener { _, key ->
                when (key) {
                    KEY_SERVER_PORT -> {}
                }
            }
            .map { sharedPrefs.getInt(KEY_SERVER_PORT, 8000) }

        val biometricEnabledFlow: Flow<Boolean> = sharedPrefs
            .registerOnSharedPreferenceChangeListener { _, key ->
                when (key) {
                    KEY_BIOMETRIC_ENABLED -> {}
                }
            }
            .map { sharedPrefs.getBoolean(KEY_BIOMETRIC_ENABLED, false) }

        val modelEnabledFlow: Flow<Boolean> = sharedPrefs
            .registerOnSharedPreferenceChangeListener { _, key ->
                when (key) {
                    KEY_MODEL_ENABLED -> {}
                }
            }
            .map { sharedPrefs.getBoolean(KEY_MODEL_ENABLED, false) }

        // Suspend setters
        suspend fun setCaptureEnabled(enabled: Boolean) = withContext(Dispatchers.IO) {
            sharedPrefsEdit.putBoolean(KEY_CAPTURE_ENABLED, enabled).apply { commit() }
        }
        suspend fun setCaptureResolution(res: String) = withContext(Dispatchers.IO) {
            sharedPrefsEdit.putString(KEY_CAPTURE_RESOLUTION, res).apply { commit() }
        }
        suspend fun setCaptureFps(fps: Int) = withContext(Dispatchers.IO) {
            sharedPrefsEdit.putInt(KEY_CAPTURE_FPS, fps).apply { commit() }
        }
        suspend fun setCaptureBitrate(bitrate: Int) = withContext(Dispatchers.IO) {
            sharedPrefsEdit.putInt(KEY_CAPTURE_BITRATE, bitrate).apply { commit() }
        }
        suspend fun setAudioSource(source: Int) = withContext(Dispatchers.IO) {
            sharedPrefsEdit.putInt(KEY_AUDIO_SOURCE, source).apply { commit() }
        }
        suspend fun setAudioSampleRate(rate: Int) = withContext(Dispatchers.IO) {
            sharedPrefsEdit.putInt(KEY_AUDIO_SAMPLE_RATE, rate).apply { commit() }
        }
        suspend fun setServerPort(port: Int) = withContext(Dispatchers.IO) {
            sharedPrefsEdit.putInt(KEY_SERVER_PORT, port).apply { commit() }
        }
        suspend fun setServerEnabled(enabled: Boolean) = withContext(Dispatchers.IO) {
            sharedPrefsEdit.putBoolean(KEY_SERVER_ENABLED, enabled).apply { commit() }
        }
        suspend fun setServerAuthToken(token: String) = withContext(Dispatchers.IO) {
            sharedPrefsEdit.putString(KEY_SERVER_AUTH_TOKEN, token).apply { commit() }
        }
        suspend fun setModelPath(path: String) = withContext(Dispatchers.IO) {
            sharedPrefsEdit.putString(KEY_MODEL_PATH, path).apply { commit() }
        }
        suspend fun setModelEnabled(enabled: Boolean) = withContext(Dispatchers.IO) {
            sharedPrefsEdit.putBoolean(KEY_MODEL_ENABLED, enabled).apply { commit() }
        }
        suspend fun setModelThreads(threads: Int) = withContext(Dispatchers.IO) {
            sharedPrefsEdit.putInt(KEY_MODEL_THREADS, threads).apply { commit() }
        }
        suspend fun setBiometricEnabled(enabled: Boolean) = withContext(Dispatchers.IO) {
            sharedPrefsEdit.putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply { commit() }
        }
        suspend fun setEncryptionEnabled(enabled: Boolean) = withContext(Dispatchers.IO) {
            sharedPrefsEdit.putBoolean(KEY_ENCRYPTION_ENABLED, enabled).apply { commit() }
        }
        suspend fun setStoragePath(path: String) = withContext(Dispatchers.IO) {
            sharedPrefsEdit.putString(KEY_STORAGE_PATH, path).apply { commit() }
        }
        suspend fun setMaxStorageGb(gb: Long) = withContext(Dispatchers.IO) {
            sharedPrefsEdit.putLong(KEY_MAX_STORAGE_GB, gb).apply { commit() }
        }
        suspend fun incrementStats(bytes: Long) = withContext(Dispatchers.IO) {
            sharedPrefsEdit.putLong(KEY_LAST_CAPTURE_TIME, System.currentTimeMillis()).apply {
                val totalCaptures = (sharedPrefs.getLong(KEY_TOTAL_CAPTURES, 0L) + 1L)
                val totalBytes = (sharedPrefs.getLong(KEY_TOTAL_BYTES, 0L) + bytes)
                sharedPrefsEdit.putLong(KEY_TOTAL_CAPTURES, totalCaptures).apply { commit() }
                sharedPrefsEdit.putLong(KEY_TOTAL_BYTES, totalBytes).apply { commit() }
            }
        }

        // Getters
        suspend fun getCaptureResolution(): String = sharedPrefs.getString(KEY_CAPTURE_RESOLUTION) ?: "1920x1080"
        suspend fun getCaptureFps(): Int = sharedPrefs.getInt(KEY_CAPTURE_FPS, 30)
        suspend fun getCaptureBitrate(): Int = sharedPrefs.getInt(KEY_CAPTURE_BITRATE, 8000000)
        suspend fun getAudioSource(): Int = sharedPrefs.getInt(KEY_AUDIO_SOURCE, 7) // VOICE_COMMUNICATION
        suspend fun getAudioSampleRate(): Int = sharedPrefs.getInt(KEY_AUDIO_SAMPLE_RATE, 48000)
        suspend fun getServerPort(): Int = sharedPrefs.getInt(KEY_SERVER_PORT, 8000)
        suspend fun getServerAuthToken(): String = sharedPrefs.getString(KEY_SERVER_AUTH_TOKEN) ?: ""
        suspend fun getModelPath(): String = sharedPrefs.getString(KEY_MODEL_PATH) ?: ""
        suspend fun getModelThreads(): Int = sharedPrefs.getInt(KEY_MODEL_THREADS, 4)
        suspend fun getStoragePath(): String = sharedPrefs.getString(KEY_STORAGE_PATH) ?: ""
        suspend fun getMaxStorageGb(): Long = sharedPrefs.getLong(KEY_MAX_STORAGE_GB) ?: 10L
        suspend fun getTotalCaptures(): Long = sharedPrefs.getLong(KEY_TOTAL_CAPTURES) ?: 0L
        suspend fun getTotalBytes(): Long = sharedPrefs.getLong(KEY_TOTAL_BYTES) ?: 0L
    }
}

// Global coroutine scope for background operations
object AppScope {
    private var scope: Scope? = null
    private var job: kotlinx.coroutines.Job? = null

    fun init(context: Context) {
        job = kotlinx.coroutines.Job()
        scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO + job!!)
    }

    val backgroundScope: Scope
        get() = scope!!

    fun shutdown() {
        job?.cancel()
        job = null
        scope = null
    }
}