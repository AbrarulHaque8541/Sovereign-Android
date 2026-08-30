package com.sovereign.app

import android.content.Context
import android.content.Intent

object MediaProjectionService {
    fun createStartIntent(context: Context): Intent {
        return Intent().apply {
            setAction("start_projection")
        }
    }

    fun createStopIntent(context: Context): Intent {
        return Intent().apply {
            setAction("stop_projection")
        }
    }
}