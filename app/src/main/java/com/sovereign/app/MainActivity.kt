package com.sovereign.app

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    private val MEDIA_PROJECTION_REQUEST = 1001
    private val mediaProjectionManager by lazy { getSystemService(MediaProjectionManager::class.java) }
    private var isCapturing = false
    private var captureIntent: Intent? = null

    private val prefs by lazy { getSharedPreferences("sovereign_prefs", Context.MODE_PRIVATE) }
    private val currentTheme: ThemeEngine.Theme by lazy {
        val name = prefs.getString("theme_id", "LIQUID_GLASS") ?: "LIQUID_GLASS"
        try { ThemeEngine.getTheme(ThemeEngine.ThemeId.valueOf(name)) }
        catch (e: Exception) { ThemeEngine.getTheme(ThemeEngine.ThemeId.LIQUID_GLASS) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1003)
            }
        }

        setupMainUI()
    }

    private fun setupMainUI() {
        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(currentTheme.background)
        }

        // Simple Header
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 56, 32, 32)
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(currentTheme.primary, currentTheme.secondary)
            )
        }

        header.addView(TextView(this).apply {
            text = "⚡ Sovereign"
            textSize = 32f
            setTypeface(Typeface.DEFAULT_BOLD)
            setTextColor(Color.WHITE)
        })

        header.addView(TextView(this).apply {
            text = "Your Android, Your Control"
            textSize = 14f
            setTextColor(Color.parseColor("#E0E0E0"))
            setPadding(0, 4, 0, 0)
        })

        mainLayout.addView(header)

        // 5 main feature buttons
        val scrollView = ScrollView(this)
        val contentLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 32, 24, 100)
        }

        val features = listOf(
            Triple("🎬", "Screen Record", "Record screen + audio") to ::openScreenCapture,
            Triple("🔧", "ADB Tools", "Debug & control devices") to ::openAdbTools,
            Triple("📦", "OTA Updates", "Check for updates") to ::openOtaUpdater,
            Triple("💾", "Storage Manager", "Optimize storage") to ::openStorageManager,
            Triple("🎨", "Change Theme", "Switch your look") to ::openThemeStudio
        )

        features.forEach { (info, action) ->
            val (emoji, title, desc) = info
            contentLayout.addView(createSimpleCard(emoji, title, desc, action))
        }

        scrollView.addView(contentLayout)
        mainLayout.addView(scrollView)
        setContentView(mainLayout)
    }

    private fun createSimpleCard(emoji: String, title: String, desc: String, onClick: () -> Unit): CardView {
        return CardView(this).apply {
            radius = 16f
            cardElevation = 4f
            setCardBackgroundColor(currentTheme.surface)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 12, 0, 12) }

            setOnClickListener { onClick() }

            val content = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(20, 20, 20, 20)
            }

            // Emoji in circle
            val iconBg = FrameLayout(context).apply {
                layoutParams = LinearLayout.LayoutParams(64, 64).apply { marginEnd = 16 }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(currentTheme.primary)
                }
            }

            iconBg.addView(TextView(context).apply {
                text = emoji
                textSize = 32f
                gravity = Gravity.CENTER
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            })

            content.addView(iconBg)

            val textCol = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }

            textCol.addView(TextView(context).apply {
                text = title
                textSize = 17f
                setTypeface(Typeface.DEFAULT_BOLD)
                setTextColor(currentTheme.textPrimary)
            })

            textCol.addView(TextView(context).apply {
                text = desc
                textSize = 13f
                setTextColor(currentTheme.textSecondary)
                setPadding(0, 4, 0, 0)
            })

            content.addView(textCol)

            content.addView(TextView(context).apply {
                text = "→"
                textSize = 20f
                setTextColor(currentTheme.textSecondary)
            })

            addView(content)
        }
    }

    // ===== REAL WORKING FEATURES =====

    private fun openScreenCapture() {
        if (!Settings.canDrawOverlays(this)) {
            AlertDialog.Builder(this)
                .setTitle("Overlay Permission")
                .setMessage("Screen recording needs overlay permission. Grant now?")
                .setPositiveButton("Grant") { _, _ ->
                    startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")))
                }
                .setNegativeButton("Cancel", null)
                .show()
            return
        }
        startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), MEDIA_PROJECTION_REQUEST)
    }

    private fun openAdbTools() {
        try {
            val intent = Intent(this, com.sovereign.app.adb.AdbControlActivity::class.java)
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "ADB Tools: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun openOtaUpdater() {
        try {
            val intent = Intent(this, com.sovereign.app.updater.UpdaterActivity::class.java)
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "OTA: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun openStorageManager() {
        try {
            val intent = Intent(this, com.sovereign.app.storage.StorageManagerActivity::class.java)
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Storage: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun openThemeStudio() {
        try {
            startActivity(Intent(this, ThemeStudioActivity::class.java))
        } catch (e: Exception) {
            Toast.makeText(this, "Theme: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == MEDIA_PROJECTION_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            isCapturing = true
            captureIntent = CaptureService.createCaptureIntent(this)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(captureIntent!!)
            } else {
                startService(captureIntent!!)
            }
            Toast.makeText(this, "🎬 Recording started!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        if (isCapturing && captureIntent != null) {
            startService(CaptureService.createStopIntent(this))
        }
        super.onDestroy()
    }
}
