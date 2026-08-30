package com.sovereign.storage

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPOutputStream
import java.util.zip.GZIPInputStream

object ExtremeStorageEngine {
    private const val TAG = "ExtremeStorage"
    private const val MAX_CACHE_SIZE = 50L * 1024 * 1024 // 50MB

    @Volatile
    private var initialized = false
    private val fileCache = ConcurrentHashMap<String, FileEntry>()
    private val cacheSize = AtomicLong(0L)
    private val backgroundJob = Job()

    data class FileEntry(
        val path: String,
        val size: Long,
        val lastAccess: Long,
        val compressed: Boolean
    )

    fun initialize(context: Context) {
        if (initialized) return
        initialized = true

        // Start background maintenance using a simple scope
        CoroutineScope(Dispatchers.IO + backgroundJob).launch {
            pruneLoop()
        }

        CoroutineScope(Dispatchers.IO + backgroundJob).launch {
            flushLoop()
        }

        Log.i(TAG, "ExtremeStorageEngine initialized (simplified)")
    }

    fun writeFile(path: String, data: ByteArray, compress: Boolean = true): Boolean {
        return try {
            val file = File(path)
            file.parentFile?.mkdirs()

            val outputData = if (compress) compressGZIP(data) else data

            FileOutputStream(file).use { fos ->
                fos.write(outputData)
            }

            val entry = FileEntry(path, outputData.size.toLong(), System.currentTimeMillis(), compress)
            fileCache[path] = entry
            cacheSize.addAndGet(outputData.size.toLong())

            pruneIfNeeded()
            true
        } catch (e: IOException) {
            Log.e(TAG, "Write failed: $path", e)
            false
        }
    }

    fun readFile(path: String, decompress: Boolean = true): ByteArray? {
        return try {
            val file = File(path)
            if (!file.exists()) return null

            val data = file.readBytes()
            val entry = fileCache[path] ?: FileEntry(path, data.size.toLong(), System.currentTimeMillis(), decompress)
            fileCache[path] = entry.copy(lastAccess = System.currentTimeMillis())

            if (decompress && entry.compressed) {
                decompressGZIP(data)
            } else {
                data
            }
        } catch (e: IOException) {
            Log.e(TAG, "Read failed: $path", e)
            null
        }
    }

    fun deleteFile(path: String): Boolean {
        return try {
            val file = File(path)
            val deleted = file.delete()
            if (deleted) {
                fileCache.remove(path)?.let { entry ->
                    cacheSize.addAndGet(-entry.size)
                }
            }
            deleted
        } catch (e: Exception) {
            Log.e(TAG, "Delete failed: $path", e)
            false
        }
    }

    fun getCacheSize(): Long = cacheSize.get()
    fun getCacheEntryCount(): Int = fileCache.size
    fun shutdown() {
        backgroundJob.cancel()
    }

    private fun compressGZIP(data: ByteArray): ByteArray {
        return java.io.ByteArrayOutputStream().use { baos ->
            java.util.zip.GZIPOutputStream(baos).use { gzos ->
                gzos.write(data)
            }
            baos.toByteArray()
        }
    }

    private fun decompressGZIP(data: ByteArray): ByteArray {
        return java.io.ByteArrayInputStream(data).use { bais ->
            java.util.zip.GZIPInputStream(bais).use { gis ->
                val result = ByteArray(1024)
                val buffer = ByteArray(1024)
                val output = java.io.ByteArrayOutputStream()
                var len = gis.read(buffer)
                while (len > 0) {
                    output.write(buffer, 0, len)
                    len = gis.read(buffer)
                }
                output.toByteArray()
            }
        }
    }

    private fun pruneIfNeeded() {
        while (cacheSize.get() > MAX_CACHE_SIZE) {
            val oldest = fileCache.values.minByOrNull { it.lastAccess }
            oldest?.let {
                deleteFile(it.path)
            } ?: break
        }
    }

    // Non-suspend version - runs in a loop
    private fun pruneLoop() {
        while (true) {
            try {
                pruneIfNeeded()
                Thread.sleep(60_000L)
            } catch (e: InterruptedException) {
                break
            }
        }
    }

    // Non-suspend version - runs in a loop
    private fun flushLoop() {
        while (true) {
            try {
                Thread.sleep(30_000L)
            } catch (e: InterruptedException) {
                break
            }
        }
    }
}
