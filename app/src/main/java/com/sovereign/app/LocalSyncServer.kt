package com.sovereign.app

import android.content.Context
import android.os.Build
import android.util.Log
import com.sovereign.app.SovereignApplication.Companion.backgroundScope
import com.sovereign.app.SovereignApplication.Companion.dataStore
import com.sovereign.app.SovereignApplication.Companion.getCaptureEnabled
import com.sovereign.app.SovereignApplication.Companion.getServerPort
import com.sovereign.app.SovereignApplication.Companion.getServerEnabled
import com.sovereign.app.SovereignApplication.Companion.getServerAuthToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

typealias ReloadCallback = (String, String) -> Unit

data class ReloadPayload(
    val component: String,
    val payload: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class HealthResponse(
    val status: String = "healthy",
    val service: String = "sovereign-sync",
    val version: String = "1.0.0",
    val uptime: Long = System.currentTimeMillis() - LocalSyncServer.startTime,
    val capturesActive: Boolean = false,
    val serverPort: Int = 8000,
    val totalCaptures: Long = 0,
    val totalBytes: Long = 0,
    val freeStorageBytes: Long = 0,
    val cpuUsage: Double = 0.0,
    val memoryUsage: Long = 0,
    val batteryLevel: Int = -1
)

data class CaptureCommand(
    val action: String,
    val params: JsonObject? = null
)

data class CaptureResponse(
    val success: Boolean,
    val message: String,
    val data: JsonObject? = null
)

object LocalSyncServer {
    private const val TAG = "LocalSyncServer"
    private const val DEFAULT_PORT = 8000
    private const val ENDPOINT_RELOAD = "/__reload"
    private const val ENDPOINT_HEALTH = "/__health"
    private const val ENDPOINT_CAPTURE_START = "/capture/start"
    private const val ENDPOINT_CAPTURE_STOP = "/capture/stop"
    private const val ENDPOINT_CAPTURE_STATUS = "/capture/status"
    val startTime = System.currentTimeMillis()

    private var serverThread: Thread? = null
    private var serverSocket: ServerSocket? = null
    private val isRunning = AtomicBoolean(false)
    private val reloadChannel = Channel<ReloadPayload>(10)
    private val captureCommandChannel = Channel<CaptureCommand>(10)
    private val contextRef = AtomicReference<Context?>(null)
    private val executor = Executors.newFixedThreadPool(4)
    private val json = Json { ignoreUnknownKeys = true }
    private val client = OkHttpClient.Builder()
        .connectTimeout(2, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.SECONDS)
        .build()
    private var serverJob: Job? = null

    var onCaptureCommand: ((CaptureCommand) -> CaptureResponse)? = null

    fun start(context: Context, onUiReload: ReloadCallback? = null) {
        if (isRunning.getAndSet(true)) {
            Log.w(TAG, "Server already running")
            return
        }

        contextRef.set(context)
        
        // Load port from preferences
        val port = SovereignApplication.getServerPort()
        
        serverJob = backgroundScope.launch {
            try {
                serverSocket = ServerSocket()
                serverSocket?.setReuseAddress(true)
                serverSocket?.bind(InetSocketAddress("0.0.0.0", port))
                Log.i(TAG, "LocalSyncServer started on 0.0.0.0:$port")

                // Start reload handler
                kotlinx.coroutines.launch(Dispatchers.IO) {
                    while (isRunning.get()) {
                        try {
                            val payload = reloadChannel.receive()
                            onUiReload?.invoke(payload.component, payload.payload)
                        } catch (e: Exception) {
                            if (isRunning.get()) Log.e(TAG, "Reload handler error", e)
                        }
                    }
                }

                // Start capture command handler
                kotlinx.coroutines.launch(Dispatchers.IO) {
                    while (isRunning.get()) {
                        try {
                            val cmd = captureCommandChannel.receive()
                            val response = onCaptureCommand?.invoke(cmd) ?: CaptureResponse(false, "No handler")
                            Log.d(TAG, "Capture command: ${cmd.action} -> ${response.success}")
                        } catch (e: Exception) {
                            if (isRunning.get()) Log.e(TAG, "Capture command handler error", e)
                        }
                    }
                }

                // Accept connections
                while (isRunning.get() && !Thread.currentThread().isInterrupted) {
                    try {
                        val socket = serverSocket?.accept() ?: break
                        executor.execute { handleClient(socket) }
                    } catch (e: java.io.IOException) {
                        if (isRunning.get()) Log.e(TAG, "Accept error", e)
                    }
                }
                executor.shutdown()
            } catch (e: java.io.IOException) {
                Log.e(TAG, "Failed to start server", e)
            } finally {
                stop()
            }
        }
    }

    private fun handleClient(socket: Socket) {
        socket.use { s ->
            s.setSoTimeout(5000)
            val input = s.getInputStream()
            val output = s.getOutputStream()
            val reader = BufferedReader(InputStreamReader(input))

            val requestLine = reader.readLine() ?: return
            val parts = requestLine.split(" ")
            if (parts.size < 2) {
                sendResponse(output, 400, "Bad Request", """{"error":"invalid_request"}""")
                return
            }

            val method = parts[0].uppercase()
            val path = parts[1]

            var contentLength = 0
            var line: String? = ""
            while (true) {
                val nextLine = reader.readLine()
                if (nextLine == null) break
                line = nextLine
                if (line.isBlank()) break
                if (line.startsWith("Content-Length:", true)) {
                    contentLength = line.substringAfter(":").trim().toIntOrNull() ?: 0
                }
                if (line.startsWith("Authorization:", true)) {
                    // TODO: Validate auth token
                }
            }

            val body = if (contentLength > 0) {
                val buffer = ByteArray(contentLength)
                var read = 0
                while (read < contentLength) {
                    val chunk = input.read(buffer, read, contentLength - read)
                    if (chunk == -1) break
                    read += chunk
                }
                String(buffer, 0, read)
            } else ""

            when {
                method == "POST" && path == ENDPOINT_RELOAD -> {
                    val component = parseComponentFromBody(body)
                    val payload = parsePayloadFromBody(body)
                    reloadChannel.trySend(ReloadPayload(component, payload))
                    sendResponse(output, 200, "OK", """{"status":"queued","component":"$component"}""")
                }
                method == "GET" && path == ENDPOINT_HEALTH -> {
                    val health = getHealthStatus()
                    val jsonStr = json.encodeToString(health)
                    sendResponse(output, 200, "OK", jsonStr)
                }
                method == "POST" && path == ENDPOINT_CAPTURE_START -> {
                    val cmd = parseCaptureCommand(body)
                    if (cmd.action == "start") {
                        captureCommandChannel.trySend(cmd)
                        sendResponse(output, 200, "OK", """{"status":"started"}""")
                    } else {
                        sendResponse(output, 400, "Bad Request", """{"error":"invalid_action"}""")
                    }
                }
                method == "POST" && path == ENDPOINT_CAPTURE_STOP -> {
                    val cmd = CaptureCommand("stop")
                    captureCommandChannel.trySend(cmd)
                    sendResponse(output, 200, "OK", """{"status":"stopped"}""")
                }
                method == "GET" && path == ENDPOINT_CAPTURE_STATUS -> {
                    val enabled = getCaptureEnabled()
                    val jsonStr = json.encodeToString(JsonObject(mapOf("enabled" to json.encodeToJsonElement(enabled))))
                    sendResponse(output, 200, "OK", jsonStr)
                }
                else -> {
                    sendResponse(output, 404, "Not Found", """{"error":"not_found"}""")
                }
            }
        }
    }

    private fun parseComponentFromBody(body: String): String {
        return try {
            val jsonObj = json.decodeFromString<JsonObject>(body)
            jsonObj["component"]?.jsonPrimitive?.content ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }

    private fun parsePayloadFromBody(body: String): String {
        return try {
            val jsonObj = json.decodeFromString<JsonObject>(body)
            jsonObj["payload"]?.toString() ?: body
        } catch (e: Exception) {
            body
        }
    }

    private fun parseCaptureCommand(body: String): CaptureCommand {
        return try {
            val jsonObj = json.decodeFromString<JsonObject>(body)
            val action = jsonObj["action"]?.jsonPrimitive?.content ?: "unknown"
            val params = jsonObj["params"]?.let { it as JsonObject }
            CaptureCommand(action, params)
        } catch (e: Exception) {
            CaptureCommand("unknown")
        }
    }

    private fun getHealthStatus(): HealthResponse {
        val runtime = Runtime.getRuntime()
        val totalMemory = runtime.totalMemory()
        val freeMemory = runtime.freeMemory()
        val usedMemory = totalMemory - freeMemory
        
        val statFs = android.os.StatFs(android.os.Environment.getDataDirectory().absolutePath)
        val freeStorage = statFs.blockSizeLong * statFs.availableBlocksLong

        return HealthResponse(
            serverPort = SovereignApplication.getServerPort(),
            totalCaptures = SovereignApplication.getTotalCaptures(),
            totalBytes = SovereignApplication.getTotalBytes(),
            freeStorageBytes = freeStorage,
            memoryUsage = usedMemory,
            cpuUsage = readCpuUsage()
        )
    }

    private fun readCpuUsage(): Double {
        return try {
            val reader = BufferedReader(java.io.FileReader("/proc/stat"))
            val line = reader.readLine() ?: return 0.0
            reader.close()
            val parts = line.split("\\s+".toRegex()).drop(1).map { it.toLong() }
            val total = parts.sum()
            val idle = parts[3] // idle time
            (1.0 - (idle.toDouble() / total.toDouble())) * 100
        } catch (e: Exception) {
            0.0
        }
    }

    private fun sendResponse(
        output: OutputStream,
        code: Int,
        status: String,
        body: String
    ) {
        val response = "HTTP/1.1 $code $status\r\n" +
                "Content-Type: application/json\r\n" +
                "Content-Length: ${body.length}\r\n" +
                "Connection: close\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "\r\n" +
                body
        output.write(response.toByteArray())
        output.flush()
    }

    fun stop() {
        if (!isRunning.getAndSet(false)) return
        
        try {
            serverSocket?.close()
            serverSocket = null
        } catch (e: java.io.IOException) {
            Log.e(TAG, "Error closing server socket", e)
        }
        serverJob?.cancel()
        serverJob = null
        executor.shutdown()
        reloadChannel.close()
        captureCommandChannel.close()
        Log.i(TAG, "LocalSyncServer stopped")
    }

    fun isActive(): Boolean = isRunning.get()

    fun triggerReload(component: String, payload: String = "") {
        if (!isRunning.get()) return
        backgroundScope.launch {
            try {
                val jsonStr = """{"component":"$component","payload":${if (payload.isBlank()) "{}" else payload}}"""
                val request = Request.Builder()
                    .url("http://127.0.0.1:$DEFAULT_PORT$ENDPOINT_RELOAD")
                    .post(jsonStr.toRequestBody("application/json".toMediaType()))
                    .build()
                client.newCall(request).execute().use { response ->
                    Log.d(TAG, "Reload triggered: $component - ${response.code}")
                }
            } catch (e: java.io.IOException) {
                Log.w(TAG, "Failed to trigger reload", e)
            }
        }
    }

    fun sendCaptureCommand(cmd: CaptureCommand) {
        captureCommandChannel.trySend(cmd)
    }

    fun getLocalIp(): String? {
        try {
            val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                if (iface.isLoopback || !iface.isUp) continue
                val addresses = iface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (!addr.isLoopbackAddress && addr is java.net.Inet4Address) {
                        return addr.hostAddress
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting local IP", e)
        }
        return null
    }
}