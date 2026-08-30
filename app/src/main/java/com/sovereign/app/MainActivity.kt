package com.sovereign.app

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.projection.MediaProjection
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
import com.sovereign.app.CaptureService

class MainActivity : AppCompatActivity() {
    private val MEDIA_PROJECTION_REQUEST = 1001
    private val mediaProjectionManager by lazy { getSystemService(MediaProjectionManager::class.java) }
    private var mediaProjection: MediaProjection? = null
    private var isCapturing = false
    private var captureIntent: Intent? = null

    // Colors - loaded from theme
    private val prefs by lazy { getSharedPreferences("sovereign_prefs", MODE_PRIVATE) }
    private val themeEngine by lazy { ThemeEngine(this) }
    private val currentTheme by lazy {
        val themeId = prefs.getString("theme_id", "LIQUID_GLASS") ?: "LIQUID_GLASS"
        try {
            ThemeEngine.getTheme(ThemeEngine.ThemeId.valueOf(themeId))
        } catch (e: Exception) {
            ThemeEngine.getTheme(ThemeEngine.ThemeId.LIQUID_GLASS)
        }
    }
    private val primaryColor get() = currentTheme.primary
    private val primaryDark get() = currentTheme.secondary
    private val backgroundColor get() = currentTheme.background
    private val cardBgColor get() = currentTheme.surface
    private val textPrimary get() = currentTheme.textPrimary
    private val textSecondary get() = currentTheme.textSecondary

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1003)
            }
        }
        
        setupMainUI()
    }

    private fun setupMainUI() {
        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(backgroundColor)
        }

        // Header
        mainLayout.addView(createHeader())

        // Content ScrollView
        val scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            isFillViewport = true
        }

        val contentLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 24, 32, 100)
        }

        // Description
        contentLayout.addView(createDescription())
        
        // Quick Actions
        contentLayout.addView(createSectionTitle("⚡ Quick Actions"))
        contentLayout.addView(createQuickActionsGrid())

        // Main Tools
        contentLayout.addView(createSectionTitle("🔧 Tools"))
        
        val tools = listOf(
            Triple("📱", "ADB Control", "Connect & manage devices" to primaryColor),
            Triple("⚡", "Fastboot Tools", "Flash partitions" to Color.parseColor("#FF9800")),
            Triple("📜", "Script Runner", "Execute shell scripts" to Color.parseColor("#4CAF50")),
            Triple("📦", "Package Manager", "Install & manage apps" to Color.parseColor("#E91E63")),
            Triple("🎬", "Screen Capture", "Record screen + audio" to primaryColor),
            Triple("🎨", "Theme Studio", "Customize look & feel" to Color.parseColor("#2196F3")),
            Triple("🔄", "OTA Updates", "Check for updates" to Color.parseColor("#FF5722")),
            Triple("💾", "Extreme Storage", "Optimize storage" to Color.parseColor("#607D8B"))
        )

        tools.forEach { (emoji, title, descColor) ->
            val desc = descColor as Pair<*, *>
            contentLayout.addView(createFeatureCard(emoji, title, desc.first as String, desc.second as Int))
        }

        // Settings
        contentLayout.addView(createSectionTitle("⚙️ Settings"))
        contentLayout.addView(createSettingsSection())

        scrollView.addView(contentLayout)
        mainLayout.addView(scrollView)

        setContentView(mainLayout)
    }

    private fun createHeader(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 48, 32, 32)
            
            val gradient = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(primaryColor, primaryDark)
            )
            background = gradient
            
            val titleRow = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            val logo = TextView(this@MainActivity).apply {
                text = "⚡"
                textSize = 40f
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { marginEnd = 16 }
            }

            val titleCol = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
            }

            titleCol.addView(TextView(this@MainActivity).apply {
                text = "SOVEREIGN"
                setTypeface(null, Typeface.BOLD)
                textSize = 28f
                setTextColor(Color.WHITE)
            })
            titleCol.addView(TextView(this@MainActivity).apply {
                text = "Universal Android Control Hub"
                textSize = 14f
                setTextColor(Color.parseColor("#E0E0E0"))
            })

            titleRow.addView(logo)
            titleRow.addView(titleCol)
            addView(titleRow)
        }
    }

    private fun createDescription(): TextView {
        return TextView(this).apply {
            text = "Powerful tools for Android device management, screen capture, ADB control, and more. All offline, no root required."
            textSize = 14f
            setTextColor(textSecondary)
            setPadding(0, 8, 0, 24)
        }
    }

    private fun createSectionTitle(title: String): TextView {
        return TextView(this).apply {
            text = title
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            setTextColor(textPrimary)
            setPadding(0, 24, 0, 16)
        }
    }

    private fun createQuickActionsGrid(): LinearLayout {
        val grid = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            weightSum = 2f
        }

        val captureCard = createQuickActionCard("🎬", "Record", primaryColor) { toggleCapture() }
        val settingsCard = createQuickActionCard("🎨", "Themes", primaryColor) { startActivity(Intent(this@MainActivity, ThemeStudioActivity::class.java)) }

        captureCard.layoutParams = LinearLayout.LayoutParams(0, 160, 1f).apply { marginEnd = 8 }
        settingsCard.layoutParams = LinearLayout.LayoutParams(0, 160, 1f).apply { marginStart = 8 }

        grid.addView(captureCard)
        grid.addView(settingsCard)
        return grid
    }

    private fun createQuickActionCard(emoji: String, label: String, color: Int, action: () -> Unit): CardView {
        return CardView(this).apply {
            radius = 16f
            cardElevation = 4f
            setCardBackgroundColor(color)
            setOnClickListener { action() }

            val content = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(16, 24, 16, 24)
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }

            content.addView(TextView(context).apply {
                text = emoji
                textSize = 36f
                gravity = Gravity.CENTER
            })
            content.addView(TextView(context).apply {
                text = label
                textSize = 14f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                setPadding(0, 12, 0, 0)
            })

            addView(content)
        }
    }

    private fun createFeatureCard(emoji: String, title: String, description: String, accentColor: Int): CardView {
        return CardView(this).apply {
            radius = 12f
            cardElevation = 2f
            setCardBackgroundColor(cardBgColor)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 12, 0, 12) }
            
            setOnClickListener {
                when(title) {
                    "Screen Capture" -> toggleCapture()
                    "Theme Studio" -> startActivity(Intent(this@MainActivity, ThemeStudioActivity::class.java))
                    else -> showToast("$title - Coming soon!")
                }
            }

            val content = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(20, 20, 20, 20)
            }

            val iconBg = FrameLayout(context).apply {
                layoutParams = LinearLayout.LayoutParams(64, 64).apply { marginEnd = 16 }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    colors = intArrayOf(accentColor, Color.parseColor("#333333"))
                }
            }

            iconBg.addView(TextView(context).apply {
                text = emoji
                textSize = 28f
                gravity = Gravity.CENTER
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            })

            val textContent = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            textContent.addView(TextView(context).apply {
                text = title
                textSize = 16f
                setTypeface(null, Typeface.BOLD)
                setTextColor(textPrimary)
            })
            textContent.addView(TextView(context).apply {
                text = description
                textSize = 12f
                setTextColor(textSecondary)
                setPadding(0, 4, 0, 0)
            })

            content.addView(iconBg)
            content.addView(textContent)
            content.addView(TextView(context).apply {
                text = "→"
                textSize = 20f
                setTextColor(textSecondary)
            })

            addView(content)
        }
    }

    private fun createSettingsSection(): CardView {
        return CardView(this).apply {
            radius = 12f
            cardElevation = 2f
            setCardBackgroundColor(cardBgColor)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 12, 0, 24) }

            val content = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
            }

            val settingsItems = listOf(
                Triple("ℹ️", "About", "Version 1.1.0"),
                Triple("🔒", "Permissions", "Manage app permissions"),
                Triple("🎨", "Theme Studio", "${currentTheme.emoji} ${currentTheme.name}"),
                Triple("❓", "Help & Feedback", "Get support")
            )

            settingsItems.forEachIndexed { index, (emoji, title, subtitle) ->
                if (index > 0) {
                    val divider = View(context)
                    divider.setBackgroundColor(Color.parseColor("#333333"))
                    val params = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 1
                    )
                    params.setMargins(84, 0, 20, 0)
                    divider.layoutParams = params
                    content.addView(divider)
                }
                content.addView(createSettingsRow(emoji, title, subtitle))
            }

            addView(content)
        }
    }

    private fun createSettingsRow(emoji: String, title: String, subtitle: String): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(20, 16, 20, 16)

            setOnClickListener {
                when(title) {
                    "About" -> showAboutDialog()
                    "Permissions" -> requestOverlayPermission()
                    "Theme" -> startActivity(Intent(this@MainActivity, ThemeStudioActivity::class.java))
                    "Help & Feedback" -> showToast("Contact: support@sovereign.app")
                }
            }

            addView(TextView(context).apply {
                text = emoji
                textSize = 24f
                layoutParams = LinearLayout.LayoutParams(48, 48).apply { marginEnd = 16 }
            })

            val textLayout = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            textLayout.addView(TextView(context).apply {
                text = title
                textSize = 15f
                setTextColor(textPrimary)
            })
            textLayout.addView(TextView(context).apply {
                text = subtitle
                textSize = 12f
                setTextColor(textSecondary)
            })
            addView(textLayout)

            addView(TextView(context).apply {
                text = "→"
                textSize = 18f
                setTextColor(textSecondary)
            })
        }
    }

    private fun toggleCapture() {
        if (!Settings.canDrawOverlays(this)) {
            showOverlayPermissionDialog()
            return
        }
        if (isCapturing) stopCapture() else requestMediaProjection()
    }

    private fun showOverlayPermissionDialog() {
        AlertDialog.Builder(this)
            .setTitle("🔒 Overlay Permission Required")
            .setMessage("Screen recording needs overlay permission to display the indicator. Grant it now?")
            .setPositiveButton("Grant") { _, _ -> requestOverlayPermission() }
            .setNegativeButton("Skip") { _, _ -> showToast("Screen recording disabled") }
            .show()
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
        showToast("🎬 Recording started!")
    }

    private fun stopCapture() {
        isCapturing = false
        if (captureIntent != null) {
            val stopIntent = CaptureService.createStopIntent(this)
            startService(stopIntent)
            captureIntent = null
        }
        showToast("⏹️ Recording stopped!")
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showAboutDialog() {
        AlertDialog.Builder(this)
            .setTitle("⚡ SOVEREIGN")
            .setMessage("""
                Universal Android Control Hub
                
                Version: 1.1.0
                
                Features:
                • ADB Control Hub
                • Fastboot Tools
                • Screen Recording
                • Script Runner
                • Package Manager
                • 10 Premium Themes
                • OTA Updates
                • Extreme Storage
                
                Built with ❤️
                No Shizuku/Bugjaeger needed!
            """.trimIndent())
            .setPositiveButton("OK", null)
            .show()
    }

    override fun onDestroy() {
        if (captureIntent != null) stopCapture()
        super.onDestroy()
    }
}