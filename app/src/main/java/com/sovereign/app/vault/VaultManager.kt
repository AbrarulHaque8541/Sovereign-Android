package com.sovereign.app.vault

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.MessageDigest

/**
 * VaultManager - Manages vault lock state, PIN, biometric, decoy mode
 * 100% local, encrypted storage, no cloud
 */
class VaultManager(private val context: Context) {

    private val encryptedPrefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            "vault_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private val regularPrefs: SharedPreferences =
        context.getSharedPreferences("vault_prefs", Context.MODE_PRIVATE)

    companion object {
        const val VAULT_DIR = "vault_storage"
        const val PHOTOS_DIR = "photos"
        const val VIDEOS_DIR = "videos"
        const val FILES_DIR = "files"
        const val APPS_DIR = "apps"
        const val MESSAGES_DIR = "messages"
        const val CALLS_DIR = "calls"

        const val LOCK_NONE = "none"
        const val LOCK_PIN = "pin"
        const val LOCK_PATTERN = "pattern"
        const val LOCK_FINGERPRINT = "fingerprint"

        const val DECOY_MODE_NONE = "none"
        const val DECOY_MODE_FAKE = "fake"

        const val AUTO_LOCK_DISABLED = 0
        const val AUTO_LOCK_30_SEC = 30
        const val AUTO_LOCK_1_MIN = 60
        const val AUTO_LOCK_5_MIN = 300

        @Volatile
        private var INSTANCE: VaultManager? = null

        fun getInstance(context: Context): VaultManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: VaultManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    fun isVaultInitialized(): Boolean = encryptedPrefs.contains("pin_hash")

    fun getLockType(): String = encryptedPrefs.getString("lock_type", LOCK_NONE) ?: LOCK_NONE

    fun setupPin(pin: String) {
        val hash = hashPin(pin)
        encryptedPrefs.edit()
            .putString("pin_hash", hash)
            .putString("lock_type", LOCK_PIN)
            .apply()
    }

    fun verifyPin(pin: String): Boolean {
        val storedHash = encryptedPrefs.getString("pin_hash", null) ?: return false
        return hashPin(pin) == storedHash
    }

    fun enableFingerprint(enable: Boolean) {
        encryptedPrefs.edit().putBoolean("fingerprint_enabled", enable).apply()
    }

    fun isFingerprintEnabled(): Boolean = encryptedPrefs.getBoolean("fingerprint_enabled", false)

    fun setDecoyMode(mode: String) {
        encryptedPrefs.edit().putString("decoy_mode", mode).apply()
    }

    fun getDecoyMode(): String = encryptedPrefs.getString("decoy_mode", DECOY_MODE_NONE) ?: DECOY_MODE_NONE

    fun isDecoyMode(): Boolean = getDecoyMode() == DECOY_MODE_FAKE

    fun setAutoLockTimeout(seconds: Int) {
        regularPrefs.edit().putInt("auto_lock_timeout", seconds).apply()
    }

    fun getAutoLockTimeout(): Int = regularPrefs.getInt("auto_lock_timeout", AUTO_LOCK_30_SEC)

    fun getLastUnlockTime(): Long = regularPrefs.getLong("last_unlock_time", 0L)

    fun recordUnlock() {
        regularPrefs.edit().putLong("last_unlock_time", System.currentTimeMillis()).apply()
    }

    fun isLocked(): Boolean {
        val timeout = getAutoLockTimeout()
        if (timeout == AUTO_LOCK_DISABLED) return false
        val lastUnlock = getLastUnlockTime()
        if (lastUnlock == 0L) return true
        val elapsed = (System.currentTimeMillis() - lastUnlock) / 1000
        return elapsed > timeout
    }

    fun lock() {
        regularPrefs.edit().putLong("last_unlock_time", 0L).apply()
    }

    fun isFirstRun(): Boolean = regularPrefs.getBoolean("first_run", true)

    fun setFirstRunComplete() {
        regularPrefs.edit().putBoolean("first_run", false).apply()
    }

    fun getVaultDirectory(): java.io.File {
        val dir = java.io.File(context.filesDir, VAULT_DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getCategoryDirectory(category: String): java.io.File {
        val dir = java.io.File(getVaultDirectory(), category)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getVaultSize(): Long {
        return getVaultDirectory().walkTopDown()
            .filter { it.isFile }
            .sumOf { it.length() }
    }

    fun getCategoryCount(category: String): Int {
        return getCategoryDirectory(category).listFiles()?.size ?: 0
    }

    fun getTotalCount(): Int {
        var total = 0
        listOf(PHOTOS_DIR, VIDEOS_DIR, FILES_DIR, APPS_DIR, MESSAGES_DIR, CALLS_DIR).forEach { cat ->
            total += getCategoryCount(cat)
        }
        return total
    }

    fun getCategorySize(category: String): Long {
        return getCategoryDirectory(category).walkTopDown()
            .filter { it.isFile }
            .sumOf { it.length() }
    }

    fun getCategoryStats(): Map<String, VaultCategoryStats> {
        return mapOf(
            "Photos" to VaultCategoryStats(getCategoryCount(PHOTOS_DIR), getCategorySize(PHOTOS_DIR), "📷"),
            "Videos" to VaultCategoryStats(getCategoryCount(VIDEOS_DIR), getCategorySize(VIDEOS_DIR), "🎬"),
            "Files" to VaultCategoryStats(getCategoryCount(FILES_DIR), getCategorySize(FILES_DIR), "📁"),
            "Apps" to VaultCategoryStats(getCategoryCount(APPS_DIR), getCategorySize(APPS_DIR), "📱"),
            "Messages" to VaultCategoryStats(getCategoryCount(MESSAGES_DIR), getCategorySize(MESSAGES_DIR), "💬"),
            "Calls" to VaultCategoryStats(getCategoryCount(CALLS_DIR), getCategorySize(CALLS_DIR), "📞")
        )
    }

    fun resetVault() {
        getVaultDirectory().deleteRecursively()
        regularPrefs.edit().clear().apply()
        encryptedPrefs.edit().clear().apply()
    }

    private fun hashPin(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(pin.toByteArray())
        return Base64.encodeToString(hash, Base64.NO_WRAP)
    }
}

data class VaultCategoryStats(
    val count: Int,
    val sizeBytes: Long,
    val emoji: String
) {
    fun getSizeFormatted(): String {
        val kb = sizeBytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1 -> String.format("%.1f GB", gb)
            mb >= 1 -> String.format("%.1f MB", mb)
            kb >= 1 -> String.format("%.1f KB", kb)
            else -> "$sizeBytes B"
        }
    }
}
