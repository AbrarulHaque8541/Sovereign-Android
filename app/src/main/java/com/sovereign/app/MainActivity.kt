package com.sovereign.app

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.backgroundScope
import com.sovereign.app.tools.LocalSyncService
import com.sovereign.app.tools.NativeSystemServiceEngine
import com.sovereign.app.tools.FastbootProtocolBridge
import com.sovereign.app.CaptureService

class MainActivity : AppCompatActivity() {
    private const val TAG = "MainActivity"
    private const val MEDIA_PROJECTION_REQUEST = 1001
    private const val OVERLAY_PERMISSION_REQUEST = 1002
    private const val NOTIFICATION_PERMISSION_REQUEST = 1003

    private val mediaProjectionManager by lazy { getSystemService(MediaProjectionManager::class.java) }

    private var mediaProjection: MediaProjection? = null
    private var isCapturing = false
    private var captureIntent: Intent? = null
    private var syncServiceIntent: Intent? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Check and request permissions
        checkPermissions()

        // Initialize sync service
        syncServiceIntent = LocalSyncService.createStartIntent(this)
        startForegroundService(syncServiceIntent!!)

        // Initialize ADB service
        val adbIntent = NativeSystemServiceEngine.createAdbStartIntent(this)
        startForegroundService(adbIntent)

        // Initialize Fastboot service
        val fastbootIntent = FastbootProtocolBridge.createFastbootIntent(this)
        startForegroundService(fastbootIntent)

        findViewById(R.id.btn_toggle).setOnClickListener { toggleCapture() }
        findViewById(R.id.btn_adb).setOnClickListener { openAdb() }
        findViewById(R.id.btn_fastboot).setOnClickListener { openFastboot() }
        findViewById(R.id.btn_scripts).setOnClickListener { openScripts() }
        findViewById(R.id.btn_packages).setOnClickListener { openPackages() }
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), NOTIFICATION_PERMISSION_REQUEST)
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!android.provider.Settings.canDrawOverlays(this)) {
                val intent = Intent(this, OverlayPermissionActivity::class.java)
                startActivity(intent)
            }
        }
    }

    private fun toggleCapture() {
        if (isCapturing) {
            stopCapture()
        } else {
            requestMediaProjectionPermission()
        }
    }

    private fun requestMediaProjectionPermission() {
        val intent = mediaProjectionManager.createScreenCaptureIntent()
        startActivityForResult(intent, MEDIA_PROJECTION_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == MEDIA_PROJECTION_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data)
            startCapture(mediaProjection!!)
        }
    }

    private fun startCapture(projection: MediaProjection) {
        isCapturing = true
        captureIntent = CaptureService.createCaptureIntent(this).apply {
            putExtra("media_projection", projection)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(captureIntent!!)
        } else {
            startService(captureIntent!!)
        }

        backgroundScope.launch {
            // setCaptureEnabled handled externally
        }

        recreate()
    }

    private fun stopCapture() {
        isCapturing = false
        if (captureIntent != null) {
            val stopIntent = CaptureService.createStopIntent(this)
            startService(stopIntent)
            captureIntent = null
        }
        backgroundScope.launch {
            // setCaptureEnabled handled externally
        }
        recreate()
    }

    override fun onDestroy() {
        if (captureIntent != null) {
            stopCapture()
        }
        if (syncServiceIntent != null) {
            val stopIntent = LocalSyncService.createStopIntent(this)
            startService(stopIntent)
        }
        super.onDestroy()
    }

    private fun openAdb() {
        val intent = Intent(this, com.sovereign.app.tools.ADBControlHubScreen::class.java)
        startActivity(intent)
    }

    private fun openFastboot() {
        val intent = Intent(this, com.sovereign.app.tools.FastbootProtocolBridge::class.java)
        startActivity(intent)
    }

    private fun openScripts() {
        val intent = Intent(this, com.sovereign.app.tools.ScriptRunnerUtility::class.java)
        startActivity(intent)
    }

    private fun openPackages() {
        val intent = Intent(this, com.sovereign.app.tools.NativeSystemServiceEngine::class.java)
        startActivity(intent)
    }
}