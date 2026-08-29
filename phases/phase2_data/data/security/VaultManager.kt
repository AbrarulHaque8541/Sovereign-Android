package com.sovereign.data.security

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKeys
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class VaultManager(private val context: Context) {
    private val TAG = "VaultManager"
    private val masterKeyAlias = "sovereign_master_key"
    private var masterKey: SecretKey? = null
    private val secureRandom = SecureRandom()

    companion object {
        private const val AES_KEY_SIZE = 256
        private const val GCM_TAG_LENGTH = 128
        private const val GCM_IV_LENGTH = 12
        private const val VAULT_DIR = "sovereign_vault"
    }

    init {
        initializeMasterKey()
    }

    private fun initializeMasterKey() {
        try {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            // Use the alias directly for encryption - we'll use EncryptedFile for file operations
            // For direct encryption, generate a key from the alias
            generateFallbackKey()
        } catch (e: Exception) {
            // Fallback to manual key generation if Android Keystore fails
            generateFallbackKey()
        }
    }

    private fun generateFallbackKey() {
        try {
            val keyGenerator = KeyGenerator.getInstance("AES")
            keyGenerator.init(AES_KEY_SIZE)
            masterKey = keyGenerator.generateKey()
        } catch (e: Exception) {
            throw RuntimeException("Failed to generate encryption key", e)
        }
    }

    fun encrypt(data: ByteArray): EncryptedData {
        val iv = ByteArray(GCM_IV_LENGTH)
        secureRandom.nextBytes(iv)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, masterKey!!, spec)

        val encrypted = cipher.doFinal(data)
        val combined = ByteArray(iv.size + encrypted.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(encrypted, 0, combined, iv.size, encrypted.size)

        return EncryptedData(
            data = Base64.encodeToString(combined, Base64.NO_WRAP),
            iv = Base64.encodeToString(iv, Base64.NO_WRAP)
        )
    }

    fun decrypt(encryptedData: EncryptedData): ByteArray {
        val combined = Base64.decode(encryptedData.data, Base64.NO_WRAP)
        val iv = combined.copyOfRange(0, GCM_IV_LENGTH)
        val encrypted = combined.copyOfRange(GCM_IV_LENGTH, combined.size)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, masterKey!!, spec)

        return cipher.doFinal(encrypted)
    }

    fun encryptString(text: String): String {
        return encrypt(text.toByteArray()).data
    }

    fun decryptString(encryptedBase64: String): String {
        val data = decrypt(EncryptedData(encryptedBase64, ""))
        return String(data)
    }

    fun encryptFile(inputFile: File, outputFile: File) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val iv = ByteArray(GCM_IV_LENGTH)
        secureRandom.nextBytes(iv)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, masterKey!!, spec)

        val inputStream = FileInputStream(inputFile)
        val outputStream = FileOutputStream(outputFile)

        // Write IV first
        outputStream.write(iv)

        val buffer = ByteArray(4096)
        var bytesRead: Int
        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            val encrypted = cipher.update(buffer, 0, bytesRead)
            if (encrypted != null) outputStream.write(encrypted)
        }

        val finalBytes = cipher.doFinal()
        if (finalBytes.isNotEmpty()) outputStream.write(finalBytes)

        inputStream.close()
        outputStream.close()
    }

    fun decryptFile(inputFile: File, outputFile: File) {
        val inputStream = FileInputStream(inputFile)
        val iv = ByteArray(GCM_IV_LENGTH)
        inputStream.read(iv)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, masterKey!!, spec)

        val outputStream = FileOutputStream(outputFile)
        val buffer = ByteArray(4096)
        var bytesRead: Int

        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            val decrypted = cipher.update(buffer, 0, bytesRead)
            if (decrypted != null) outputStream.write(decrypted)
        }

        val finalBytes = cipher.doFinal()
        if (finalBytes.isNotEmpty()) outputStream.write(finalBytes)

        inputStream.close()
        outputStream.close()
    }

    fun getVaultDir(): File {
        val dir = File(context.filesDir, VAULT_DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    data class EncryptedData(
        val data: String,
        val iv: String
    )
}