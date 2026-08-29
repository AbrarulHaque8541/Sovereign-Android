package com.sovereign.app

object MediaProjectionService {
    fun createStartIntent(context: Context): Intent {
        return Intent(context::class.java).apply {
            putExtra("action", "start_projection")
        }
    }

    fun createStopIntent(context: Context): Intent {
        return Intent(context::class.java).apply {
            putExtra("action", "stop_projection")
        }
    }
}
