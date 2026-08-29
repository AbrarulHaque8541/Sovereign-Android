package com.sovereign.app

import android.content.Context
import android.content.Intent
import android.util.Log

object ModelManager {
    private const val TAG = "ModelManager"
    private var isEnabled: Boolean = false
    private var modelPath: String = ""
    private var threads: Int = 4

    fun getModelEnabled(): Boolean = isEnabled
    fun getModelPath(): String = modelPath
    fun getModelThreads(): Int = threads

    fun setModelEnabled(enabled: Boolean) {
        this.isEnabled = enabled
        Log.d(TAG, "Model enabled set to $enabled")
    }

    fun setModelPath(path: String) {
        this.modelPath = path
        Log.d(TAG, "Model path set to $path")
    }

    fun setModelThreads(threads: Int) {
        this.threads = threads
        Log.d(TAG, "Model threads set to $threads")
    }

    fun createStartIntent(context: Context): Intent {
        return Intent(context, ModelManager::class.java).apply {
            putExtra("action", "start_model")
        }
    }

    fun createStopIntent(context: Context): Intent {
        return Intent(context, ModelManager::class.java).apply {
            putExtra("action", "stop_model")
        }
    }
}
