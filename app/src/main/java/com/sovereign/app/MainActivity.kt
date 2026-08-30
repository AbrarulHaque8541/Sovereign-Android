package com.sovereign.app

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.sovereign.app.LocalSyncService
import com.sovereign.app.CaptureService

class MainActivity : AppCompatActivity() {
    private val TAG = "MainActivity"
    private val MEDIA_PROJECTION_REQUEST = 1001

    private val mediaProjectionManager by lazy { getSystemService(MediaProjectionManager::class.java) }

    private var mediaProjection: MediaProjection? = null
    private var isCapturing = false
    private var captureIntent: Intent? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Check notification permission only (don't loop for overlay)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1003)
            }
        }
        
        setupUI()
    }

    private fun setupUI() {
        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(0xFF121212.toInt())
            setPadding(32, 32, 32, 32)
        }

        // Title
        val title = TextView(this).apply {
            text = "⚡ Sovereign"
            textSize = 32f
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER
        }

        // Subtitle
        val subtitle = TextView(this).apply {
            text = "Universal Android Control Hub"
            textSize = 16f
            setTextColor(0xFF888888.toInt())
            gravity = Gravity.CENTER
        }

        // Button style
        fun createButton(text: String, color: Int, action: () -> Unit): Button {
            return Button(this).apply {
                this.text = text
                setBackgroundColor(color)
                setTextColor(0xFFFFFFFF.toInt())
                textSize = 16f
                setPadding(24, 20, 24, 20)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 16, 0, 16) }
                setOnClickListener { action() }
            }
        }

        // Capture Button
        val captureBtn = createButton("🎬 Screen Capture", 0xFF6200EE.toInt()) { toggleCapture() }
        
        // ADB Button  
        val adbBtn = createButton("📱 ADB Control", 0xFF03DAC5.toInt()) { openAdbHub() }
        
        // Fastboot Button
        val fastbootBtn = createButton("⚡ Fastboot Mode", 0xFFFF9800.toInt()) { openFastboot() }
        
        // Scripts Button
        val scriptsBtn = createButton("📜 Script Runner", 0xFF4CAF50.toInt()) { openScripts() }
        
        // Packages Button
        val packagesBtn = createButton("📦 Package Manager", 0xFFE91E63.toInt()) { openPackages() }
        
        // Theme Button
        val themeBtn = createButton("🎨 Theme Settings", 0xFF9C27B0.toInt()) { openThemeSettings() }
        
        // Settings Button
        val settingsBtn = createButton("⚙️ Settings", 0xFF607D8B.toInt()) { openSettings() }

        mainLayout.addView(title)
        mainLayout.addView(subtitle)
        mainLayout.addView(createSpace(32))
        mainLayout.addView(captureBtn)
        mainLayout.addView(adbBtn)
        mainLayout.addView(fastbootBtn)
        mainLayout.addView(scriptsBtn)
        mainLayout.addView(packagesBtn)
        mainLayout.addView(createSpace(16))
        mainLayout.addView(themeBtn)
        mainLayout.addView(settingsBtn)

        setContentView(mainLayout)
    }

    private fun createSpace(height: Int): View {
        return View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                height
            )
        }
    }

    private fun toggleCapture() {
        if (!Settings.canDrawOverlays(this)) {
            // Show dialog to request overlay permission first
            showOverlayPermissionDialog()
            return
        }
        
        if (isCapturing) {
            stopCapture()
        } else {
            requestMediaProjection()
        }
    }

    private fun showOverlayPermissionDialog() {
        val dialog = android.app.AlertDialog.Builder(this)
            .setTitle("Overlay Permission Required")
            .setMessage("Screen capture needs overlay permission. Grant it now?")
            .setPositiveButton("Grant") { _, _ ->
                requestOverlayPermission()
            }
            .setNegativeButton("Cancel", null)
            .create()
        dialog.show()
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivity(intent)
        }
    }

    private fun requestMediaProjection() {
        val intent = mediaProjectionManager.createScreenCaptureIntent()
        startActivityForResult(intent, MEDIA_PROJECTION_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == MEDIA_PROJECTION_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data)
            startCapture()
        }
    }

    private fun startCapture() {
        isCapturing = true
        captureIntent = CaptureService.createCaptureIntent(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(captureIntent!!)
        } else {
            startService(captureIntent!!)
        }
        Toast.makeText(this, "🎬 Capture started", Toast.LENGTH_SHORT).show()
    }

    private fun stopCapture() {
        isCapturing = false
        if (captureIntent != null) {
            val stopIntent = CaptureService.createStopIntent(this)
            startService(stopIntent)
            captureIntent = null
        }
        Toast.makeText(this, "⏹️ Capture stopped", Toast.LENGTH_SHORT).show()
    }

    private fun openAdbHub() {
        Toast.makeText(this, "📱 ADB Control Hub - Coming soon", Toast.LENGTH_LONG).show()
    }

    private fun openFastboot() {
        Toast.makeText(this, "⚡ Fastboot Mode - Coming soon", Toast.LENGTH_LONG).show()
    }

    private fun openScripts() {
        Toast.makeText(this, "📜 Script Runner - Coming soon", Toast.LENGTH_LONG).show()
    }

    private fun openPackages() {
        Toast.makeText(this, "📦 Package Manager - Coming soon", Toast.LENGTH_LONG).show()
    }

    private fun openThemeSettings() {
        Toast.makeText(this, "🎨 Theme Settings - Coming soon", Toast.LENGTH_LONG).show()
    }

    private fun openSettings() {
        Toast.makeText(this, "⚙️ Settings - Coming soon", Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        if (captureIntent != null) {
            stopCapture()
        }
        super.onDestroy()
    }
}