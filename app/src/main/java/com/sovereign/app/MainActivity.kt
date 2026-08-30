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
import com.sovereign.app.LocalSyncService
import com.sovereign.app.CaptureService

class MainActivity : AppCompatActivity() {
    private val TAG = "MainActivity"
    private val MEDIA_PROJECTION_REQUEST = 1001
    private val OVERLAY_PERMISSION_REQUEST = 1002
    private val NOTIFICATION_PERMISSION_REQUEST = 1003

    private val mediaProjectionManager by lazy { getSystemService(MediaProjectionManager::class.java) }

    private var mediaProjection: MediaProjection? = null
    private var isCapturing = false
    private var captureIntent: Intent? = null
    private var syncServiceIntent: Intent? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkPermissions()
        initServices()
        setupButtons()
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

    private fun initServices() {
        // Services initialized in background - simplified for build
    }

    private fun setupButtons() {
        findViewById<View>(R.id.btn_toggle)?.setOnClickListener { toggleCapture() }
        findViewById<View>(R.id.btn_adb)?.setOnClickListener { openAdb() }
        findViewById<View>(R.id.btn_fastboot)?.setOnClickListener { openFastboot() }
        findViewById<View>(R.id.btn_scripts)?.setOnClickListener { openScripts() }
        findViewById<View>(R.id.btn_packages)?.setOnClickListener { openPackages() }
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
        captureIntent = CaptureService.createCaptureIntent(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(captureIntent!!)
        } else {
            startService(captureIntent!!)
        }
        Toast.makeText(this, "Capture started", Toast.LENGTH_SHORT).show()
        recreate()
    }

    private fun stopCapture() {
        isCapturing = false
        if (captureIntent != null) {
            val stopIntent = CaptureService.createStopIntent(this)
            startService(stopIntent)
            captureIntent = null
        }
        Toast.makeText(this, "Capture stopped", Toast.LENGTH_SHORT).show()
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
        Toast.makeText(this, "ADB Tools", Toast.LENGTH_SHORT).show()
    }

    private fun openFastboot() {
        Toast.makeText(this, "Fastboot Tools", Toast.LENGTH_SHORT).show()
    }

    private fun openScripts() {
        Toast.makeText(this, "Scripts", Toast.LENGTH_SHORT).show()
    }

    private fun openPackages() {
        Toast.makeText(this, "Package Manager", Toast.LENGTH_SHORT).show()
    }
}