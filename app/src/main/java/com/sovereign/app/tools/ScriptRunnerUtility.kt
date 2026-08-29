package com.sovereign.app.tools

import android.content.Context
import android.net.Uri
import android.util.Log
import com.sovereign.app.AppScope.backgroundScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ScriptRunnerUtility {
    private const val TAG = "ScriptRunner"
    private const val SCRIPTS_DIR = "scripts"
    private const val MAX_OUTPUT_SIZE = 10 * 1024 * 1024
    
    private var initialized = false
    
    data class ScriptInfo(
        val name: String,
        val path: String,
        val size: Long,
        val lastModified: Long,
        val type: ScriptType,
        val description: String = "",
        val isExecutable: Boolean = true
    )
    
    enum class ScriptType {
        SHELL, PYTHON, BINARY, UNKNOWN
    }
    
    data class ScriptExecution(
        val id: String,
        val scriptName: String,
        val startTime: Long,
        var process: Process? = null,
        val outputBuffer: StringBuilder = StringBuilder(),
        val isRunning: java.util.concurrent.atomic.AtomicBoolean = java.util.concurrent.atomic.AtomicBoolean(true),
        val exitCode: java.util.concurrent.atomic.AtomicReference<Int?> = java.util.concurrent.atomic.AtomicReference(null)
    )
    
    enum class ExecutionState {
        PENDING, RUNNING, COMPLETED, FAILED, CANCELLED
    }
    
    @Suppress("UNUSED_PARAMETER")
    fun initialize(context: Context) {
        if (initialized) return
        initialized = true
        
        val scriptsDir = getScriptsDir(context)
        scriptsDir.mkdirs()
        createExampleScripts(scriptsDir)
        
        Log.i(TAG, "ScriptRunnerUtility initialized")
    }
    
    private fun createExampleScripts(dir: File) {
        val exampleSh = File(dir, "example.sh")
        if (!exampleSh.exists()) {
            exampleSh.writeText("""#!/bin/sh
echo "=== Sovereign Script Runner ==="
echo "Date: \$(date)"
echo "Device: \$(getprop ro.product.model)"
echo "Android: \$(getprop ro.build.version.release)"
""".trimIndent())
        }
        dir.listFiles()?.forEach { it.setExecutable(true) }
    }
    
    fun getScriptsDir(context: Context): File {
        return File(context.filesDir, SCRIPTS_DIR).apply { mkdirs() }
    }
    
    fun listScripts(context: Context): List<ScriptInfo> {
        val dir = getScriptsDir(context)
        val files = dir.listFiles()?.filter { it.isFile } ?: return emptyList()
        return files.map { file ->
            val type = when (file.extension.lowercase()) {
                "sh" -> ScriptType.SHELL
                "py" -> ScriptType.PYTHON
                "bin" -> ScriptType.BINARY
                else -> ScriptType.UNKNOWN
            }
            ScriptInfo(
                name = file.nameWithoutExtension,
                path = file.absolutePath,
                size = file.length(),
                lastModified = file.lastModified(),
                type = type,
                isExecutable = file.canExecute()
            )
        }.sortedBy { it.name }
    }
    
    fun getScriptContent(context: Context, scriptName: String): String? {
        val file = File(getScriptsDir(context), scriptName)
        return if (file.exists()) file.readText() else null
    }
    
    fun saveScript(context: Context, scriptName: String, content: String): Boolean {
        return try {
            val dir = getScriptsDir(context)
            dir.mkdirs()
            val file = File(dir, scriptName)
            file.writeText(content)
            file.setExecutable(true)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Save script failed", e)
            false
        }
    }
    
    fun deleteScript(context: Context, scriptName: String): Boolean {
        return try {
            val file = File(getScriptsDir(context), scriptName)
            file.delete()
        } catch (e: Exception) {
            false
        }
    }
    
    fun uploadScript(context: Context, uri: Uri, targetName: String): Boolean {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return false
            val dir = getScriptsDir(context)
            dir.mkdirs()
            val targetFile = File(dir, targetName)
            val outputStream = FileOutputStream(targetFile)
            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.close()
            targetFile.setExecutable(true)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Upload script failed", e)
            false
        }
    }
    
    fun executeScript(
        context: Context,
        scriptName: String,
        args: List<String> = emptyList(),
        onOutput: ((String) -> Unit)? = null,
        onComplete: ((Int, String) -> Unit)? = null
    ): String {
        val executionId = "exec_${System.currentTimeMillis()}"
        
        val scriptFile = File(getScriptsDir(context), scriptName)
        if (!scriptFile.exists()) {
            onComplete?.invoke(-1, "Script not found: $scriptName")
            return executionId
        }
        
        if (!scriptFile.canExecute()) {
            scriptFile.setExecutable(true)
        }
        
        val execution = ScriptExecution(
            id = executionId,
            scriptName = scriptName,
            startTime = System.currentTimeMillis()
        )
        
        backgroundScope.launch(Dispatchers.IO) {
            try {
                val command = buildCommand(scriptFile, args)
                val process = Runtime.getRuntime().exec(command)
                execution.process = process
                
                val stdoutReader = BufferedReader(InputStreamReader(process.inputStream))
                var line: String?
                var outputSize = 0L
                
                while (execution.isRunning.get() && 
                       stdoutReader.readLine().also { line = it } != null && 
                       outputSize < MAX_OUTPUT_SIZE) {
                    
                    val outputLine = line ?: ""
                    execution.outputBuffer.append(outputLine).append("\n")
                    outputSize += outputLine.length + 1
                    onOutput?.invoke(outputLine)
                }
                
                val stderrReader = BufferedReader(InputStreamReader(process.errorStream))
                while (execution.isRunning.get() && 
                       stderrReader.readLine().also { line = it } != null) {
                    val errorLine = "[ERR] ${line ?: ""}"
                    execution.outputBuffer.append(errorLine).append("\n")
                    onOutput?.invoke(errorLine)
                }
                
                val exitCode = process.waitFor()
                execution.exitCode.set(exitCode)
                execution.isRunning.set(false)
                
                val output = execution.outputBuffer.toString()
                onComplete?.invoke(exitCode, output)
                
            } catch (e: Exception) {
                execution.isRunning.set(false)
                execution.exitCode.set(-1)
                val errorMsg = "Execution error: ${e.message}"
                execution.outputBuffer.append(errorMsg).append("\n")
                onOutput?.invoke(errorMsg)
                onComplete?.invoke(-1, errorMsg)
            }
        }
        
        return executionId
    }
    
    private fun buildCommand(scriptFile: File, args: List<String>): Array<String> {
        val extension = scriptFile.extension.lowercase()
        return when (extension) {
            "sh" -> arrayOf("sh", scriptFile.absolutePath) + args.toTypedArray()
            "py" -> arrayOf("python3", scriptFile.absolutePath) + args.toTypedArray()
            "bin" -> arrayOf(scriptFile.absolutePath) + args.toTypedArray()
            else -> arrayOf("sh", scriptFile.absolutePath) + args.toTypedArray()
        }
    }
    
    fun cancelExecution(executionId: String): Boolean {
        return false // Simplified
    }
    
    fun cancelAllExecutions() {
        // Simplified
    }
    
    fun getExecutionOutput(executionId: String): String = ""
    fun getExecutionState(executionId: String): ExecutionState = ExecutionState.PENDING
    
    fun getBuiltInTemplates(): Map<String, String> = mapOf(
        "system_info" to """#!/bin/sh
echo "=== System Info ==="
getprop ro.product.model
getprop ro.build.version.release
""",
        "battery_status" to """#!/bin/sh
echo "=== Battery Status ==="
dumpsys battery
""",
    )
    
    fun createScriptFromTemplate(context: Context, templateName: String, customName: String? = null): Boolean {
        val template = getBuiltInTemplates()[templateName]
        if (template == null) return false
        
        val name = customName ?: "${templateName}_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.sh"
        return saveScript(context, name, template)
    }
    
    fun shutdown() {}
}