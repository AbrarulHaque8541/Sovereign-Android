package com.sovereign.app

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class SovereignApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize global scope for background tasks
        AppScope.init(this)
    }

    companion object {
        private const val DATASTORE_NAME = "sovereign_prefs"
        
        // Capture Settings
        private val KEY_CAPTURE_ENABLED = booleanPreferencesKey("capture_enabled")
        private val KEY_CAPTURE_RESOLUTION = stringPreferencesKey("capture_resolution")
        private val KEY_CAPTURE_FPS = intPreferencesKey("capture_fps")
        private val KEY_CAPTURE_BITRATE = intPreferencesKey("capture_bitrate")
        private val KEY_AUDIO_SOURCE = intPreferencesKey("audio_source")
        private val KEY_AUDIO_SAMPLE_RATE = intPreferencesKey("audio_sample_rate")
        
        // Server Settings
        private val KEY_SERVER_PORT = intPreferencesKey("server_port")
        private val KEY_SERVER_ENABLED = booleanPreferencesKey("server_enabled")
        private val KEY_SERVER_AUTH_TOKEN = stringPreferencesKey("server_auth_token")
        
        // Model Settings
        private val KEY_MODEL_PATH = stringPreferencesKey("model_path")
        private val KEY_MODEL_ENABLED = booleanPreferencesKey("model_enabled")
        private val KEY_MODEL_THREADS = intPreferencesKey("model_threads")
        
        // Security
        private val KEY_BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        private val KEY_ENCRYPTION_ENABLED = booleanPreferencesKey("encryption_enabled")
        
        // Storage
        private val KEY_STORAGE_PATH = stringPreferencesKey("storage_path")
        private val KEY_MAX_STORAGE_GB = longPreferencesKey("max_storage_gb")
        
        // State
        private val KEY_LAST_CAPTURE_TIME = longPreferencesKey("last_capture_time")
        private val KEY_TOTAL_CAPTURES = longPreferencesKey("total_captures")
        private val KEY_TOTAL_BYTES = longPreferencesKey("total_bytes")
        
        val dataStore: androidx.datastore.preferences.PreferencesDataStore by preferencesDataStore(name = DATASTORE_NAME)

        // Flows
        val captureEnabledFlow: Flow<Boolean> = dataStore.data.map { it[KEY_CAPTURE_ENABLED] ?: false }
        val serverEnabledFlow: Flow<Boolean> = dataStore.data.map { it[KEY_SERVER_ENABLED] ?: true }
        val serverPortFlow: Flow<Int> = dataStore.data.map { it[KEY_SERVER_PORT] ?: 8000 }
        val biometricEnabledFlow: Flow<Boolean> = dataStore.data.map { it[KEY_BIOMETRIC_ENABLED] ?: false }
        val modelEnabledFlow: Flow<Boolean> = dataStore.data.map { it[KEY_MODEL_ENABLED] ?: false }
        
        // Suspend setters
        suspend fun setCaptureEnabled(enabled: Boolean) = dataStore.edit { it[KEY_CAPTURE_ENABLED] = enabled }
        suspend fun setCaptureResolution(res: String) = dataStore.edit { it[KEY_CAPTURE_RESOLUTION] = res }
        suspend fun setCaptureFps(fps: Int) = dataStore.edit { it[KEY_CAPTURE_FPS] = fps }
        suspend fun setCaptureBitrate(bitrate: Int) = dataStore.edit { it[KEY_CAPTURE_BITRATE] = bitrate }
        suspend fun setAudioSource(source: Int) = dataStore.edit { it[KEY_AUDIO_SOURCE] = source }
        suspend fun setAudioSampleRate(rate: Int) = dataStore.edit { it[KEY_AUDIO_SAMPLE_RATE] = rate }
        suspend fun setServerPort(port: Int) = dataStore.edit { it[KEY_SERVER_PORT] = port }
        suspend fun setServerEnabled(enabled: Boolean) = dataStore.edit { it[KEY_SERVER_ENABLED] = enabled }
        suspend fun setServerAuthToken(token: String) = dataStore.edit { it[KEY_SERVER_AUTH_TOKEN] = token }
        suspend fun setModelPath(path: String) = dataStore.edit { it[KEY_MODEL_PATH] = path }
        suspend fun setModelEnabled(enabled: Boolean) = dataStore.edit { it[KEY_MODEL_ENABLED] = enabled }
        suspend fun setModelThreads(threads: Int) = dataStore.edit { it[KEY_MODEL_THREADS] = threads }
        suspend fun setBiometricEnabled(enabled: Boolean) = dataStore.edit { it[KEY_BIOMETRIC_ENABLED] = enabled }
        suspend fun setEncryptionEnabled(enabled: Boolean) = dataStore.edit { it[KEY_ENCRYPTION_ENABLED] = enabled }
        suspend fun setStoragePath(path: String) = dataStore.edit { it[KEY_STORAGE_PATH] = path }
        suspend fun setMaxStorageGb(gb: Long) = dataStore.edit { it[KEY_MAX_STORAGE_GB] = gb }
        suspend fun incrementStats(bytes: Long) = dataStore.edit { prefs ->
            prefs[KEY_LAST_CAPTURE_TIME] = System.currentTimeMillis()
            prefs[KEY_TOTAL_CAPTURES] = (prefs[KEY_TOTAL_CAPTURES] ?: 0L) + 1
            prefs[KEY_TOTAL_BYTES] = (prefs[KEY_TOTAL_BYTES] ?: 0L) + bytes
        }
        
        // Getters
        suspend fun getCaptureResolution(): String = dataStore.data.first() [KEY_CAPTURE_RESOLUTION] ?: "1920x1080"
        suspend fun getCaptureFps(): Int = dataStore.data.first() [KEY_CAPTURE_FPS] ?: 30
        suspend fun getCaptureBitrate(): Int = dataStore.data.first() [KEY_CAPTURE_BITRATE] ?: 8000000
        suspend fun getAudioSource(): Int = dataStore.data.first() [KEY_AUDIO_SOURCE] ?: 7 // VOICE_COMMUNICATION
        suspend fun getAudioSampleRate(): Int = dataStore.data.first() [KEY_AUDIO_SAMPLE_RATE] ?: 48000
        suspend fun getServerPort(): Int = dataStore.data.first() [KEY_SERVER_PORT] ?: 8000
        suspend fun getServerAuthToken(): String = dataStore.data.first() [KEY_SERVER_AUTH_TOKEN] ?: ""
        suspend fun getModelPath(): String = dataStore.data.first() [KEY_MODEL_PATH] ?: ""
        suspend fun getModelThreads(): Int = dataStore.data.first() [KEY_MODEL_THREADS] ?: 4
        suspend fun getStoragePath(): String = dataStore.data.first() [KEY_STORAGE_PATH] ?: ""
        suspend fun getMaxStorageGb(): Long = dataStore.data.first() [KEY_MAX_STORAGE_GB] ?: 10L
        suspend fun getTotalCaptures(): Long = dataStore.data.first() [KEY_TOTAL_CAPTURES] ?: 0L
        suspend fun getTotalBytes(): Long = dataStore.data.first() [KEY_TOTAL_BYTES] ?: 0L
    }
}

// Global coroutine scope for background operations
object AppScope {
    private var scope: kotlinx.coroutines.CoroutineScope? = null
    private var job: kotlinx.coroutines.Job? = null
    
    fun init(context: Context) {
        job = kotlinx.coroutines.Job()
        scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO + job!!)
    }
    
    val backgroundScope: kotlinx.coroutines.CoroutineScope
        get() = scope!!
    
    fun shutdown() {
        job?.cancel()
        job = null
        scope = null
    }
}