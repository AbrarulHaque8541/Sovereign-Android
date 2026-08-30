package com.sovereign.app

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.sovereign.app.vault.VaultActivity
import com.sovereign.app.vault.VaultLockActivity
import com.sovereign.app.vault.VaultManager

class MainActivity : AppCompatActivity() {

    private lateinit var vaultManager: VaultManager
    private lateinit var drawerLayout: DrawerLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vaultManager = VaultManager.getInstance(this)
        setupUI()
    }

    private fun setupUI() {
        drawerLayout = DrawerLayout(this)
        drawerLayout.addView(createMainContent())

        // Drawer view (right side)
        val drawerContent = createDrawerContent()
        val params = DrawerLayout.LayoutParams(
            (resources.displayMetrics.widthPixels * 0.85).toInt(),
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        params.gravity = GravityCompat.END
        drawerContent.layoutParams = params
        drawerLayout.addView(drawerContent)

        setContentView(drawerLayout)
    }

    private fun createMainContent(): LinearLayout {
        val mainContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF0a0e27.toInt())
        }

        // Header
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(24, 56, 24, 24)
            setBackgroundColor(0xFF1a1a2e.toInt())
        }

        header.addView(TextView(this).apply {
            text = "⚡ SOVEREIGN"
            setTypeface(Typeface.DEFAULT_BOLD)
            textSize = 22f
            setTextColor(Color.WHITE)
        })

        header.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })

        header.addView(ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_menu_myplaces)
            setColorFilter(Color.WHITE)
            setBackgroundColor(Color.TRANSPARENT)
            setOnClickListener { drawerLayout.openDrawer(GravityCompat.END) }
            layoutParams = LinearLayout.LayoutParams(80, 80)
        })

        header.addView(Button(this).apply {
            text = "⋮"
            setTextColor(Color.WHITE)
            textSize = 24f
            setBackgroundColor(Color.TRANSPARENT)
            setOnClickListener { drawerLayout.openDrawer(GravityCompat.END) }
            layoutParams = LinearLayout.LayoutParams(80, 80)
        })

        mainContent.addView(header)

        // Subtitle
        mainContent.addView(TextView(this).apply {
            text = "Universal Android Control Hub"
            setTextColor(0xFF9ca3af.toInt())
            textSize = 13f
            setPadding(24, 0, 24, 24)
            setBackgroundColor(0xFF1a1a2e.toInt())
        })

        // Scrollable content
        val scrollView = ScrollView(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 100)
        }

        // ===== VAULT HERO CARD =====
        content.addView(createVaultHeroCard())

        // ===== SECURITY STATUS =====
        content.addView(createSecurityStatusWidget())

        // ===== QUICK ACCESS ROW =====
        content.addView(createQuickAccessRow())

        // ===== ACTIVITY FEED =====
        content.addView(createActivityFeed())

        scrollView.addView(content)
        mainContent.addView(scrollView)
        return mainContent
    }

    private fun createVaultHeroCard(): CardView {
        val vaultSize = vaultManager.getVaultSize()
        val isLocked = vaultManager.isLocked()

        return CardView(this).apply {
            radius = 24f
            cardElevation = 8f
            setCardBackgroundColor(0xFF1e1e2e.toInt())

            background = GradientDrawable().apply {
                setColor(0xFF1e1e2e.toInt())
                setStroke(2, 0xFFef4444.toInt())
                cornerRadius = 24f
            }

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 8, 0, 16) }

            setOnClickListener { openVault() }

            addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(24, 24, 24, 24)

                // Top row: Lock icon + Title
                addView(LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL

                    addView(TextView(context).apply {
                        text = "🔒"
                        textSize = 32f
                    })

                    addView(LinearLayout(context).apply {
                        orientation = LinearLayout.VERTICAL
                        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                            .apply { marginStart = 16 }

                        addView(TextView(context).apply {
                            text = "PRIVATE VAULT"
                            setTypeface(Typeface.DEFAULT_BOLD)
                            textSize = 18f
                            setTextColor(Color.WHITE)
                        })

                        addView(TextView(context).apply {
                            text = if (isLocked) "Tap to unlock" else "Tap to open"
                            setTextColor(0xFF9ca3af.toInt())
                            textSize = 13f
                        })
                    })

                    addView(TextView(context).apply {
                        text = if (isLocked) "🔐" else "🔓"
                        textSize = 28f
                    })
                })

                addView(View(context).apply {
                    setBackgroundColor(0xFF2d2d3d.toInt())
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 1
                    ).apply { setMargins(0, 16, 0, 16) }
                })

                // Category grid
                addView(LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER

                    val cats = listOf(
                        Triple("📷", "Photos", vaultManager.getCategoryCount("photos")),
                        Triple("🎬", "Videos", vaultManager.getCategoryCount("videos")),
                        Triple("📁", "Files", vaultManager.getCategoryCount("files")),
                        Triple("📱", "Apps", vaultManager.getCategoryCount("apps"))
                    )

                    cats.forEach { (emoji, name, count) ->
                        addView(LinearLayout(context).apply {
                            orientation = LinearLayout.VERTICAL
                            gravity = Gravity.CENTER
                            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)

                            addView(TextView(context).apply {
                                text = emoji
                                textSize = 24f
                                gravity = Gravity.CENTER
                            })

                            addView(TextView(context).apply {
                                text = "$count"
                                setTextColor(0xFFef4444.toInt())
                                textSize = 14f
                                setTypeface(Typeface.DEFAULT_BOLD)
                                gravity = Gravity.CENTER
                            })

                            addView(TextView(context).apply {
                                text = name
                                setTextColor(0xFF9ca3af.toInt())
                                textSize = 11f
                                gravity = Gravity.CENTER
                            })
                        })
                    }
                })

                addView(TextView(context).apply {
                    text = "Storage: ${formatSize(vaultSize)} used"
                    setTextColor(0xFF9ca3af.toInt())
                    textSize = 12f
                    setPadding(0, 16, 0, 0)
                })
            })
        }
    }

    private fun createSecurityStatusWidget(): CardView {
        val isLocked = vaultManager.isLocked()
        val fingerprintOn = vaultManager.isFingerprintEnabled()
        val autoLockOn = vaultManager.getAutoLockTimeout() > 0

        return CardView(this).apply {
            radius = 16f
            cardElevation = 4f
            setCardBackgroundColor(0xFF1e293b.toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 8, 0, 16) }

            addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(20, 16, 20, 16)

                addView(TextView(context).apply {
                    text = "🛡️ Security Status"
                    setTypeface(Typeface.DEFAULT_BOLD)
                    textSize = 14f
                    setTextColor(Color.WHITE)
                    setPadding(0, 0, 0, 12)
                })

                addView(LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL

                    val items = listOf(
                        Triple(if (isLocked) "🔐" else "🔓", if (isLocked) "Locked" else "Unlocked", 0xFFef4444.toInt()),
                        Triple(if (fingerprintOn) "👆" else "❌", if (fingerprintOn) "Fingerprint" else "No FP", if (fingerprintOn) 0xFF22c55e.toInt() else 0xFF9ca3af.toInt()),
                        Triple(if (autoLockOn) "⏱️" else "∞", if (autoLockOn) "Auto-lock" else "No Timer", if (autoLockOn) 0xFF22c55e.toInt() else 0xFF9ca3af.toInt())
                    )

                    items.forEach { (icon, label, color) ->
                        addView(LinearLayout(context).apply {
                            orientation = LinearLayout.VERTICAL
                            gravity = Gravity.CENTER
                            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)

                            addView(TextView(context).apply {
                                text = icon
                                textSize = 20f
                                gravity = Gravity.CENTER
                            })

                            addView(TextView(context).apply {
                                text = label
                                setTextColor(color)
                                textSize = 12f
                                gravity = Gravity.CENTER
                            })
                        })
                    }
                })
            })
        }
    }

    private fun createQuickAccessRow(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 8, 0, 16)

            // Camera shortcut
            addView(CardView(context).apply {
                radius = 12f
                cardElevation = 2f
                setCardBackgroundColor(0xFF1e293b.toInt())
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    .apply { setMargins(4, 0, 4, 0) }
                setOnClickListener { quickAddToVault("photos") }

                addView(LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    setPadding(12, 16, 12, 16)

                    addView(TextView(context).apply {
                        text = "📷"
                        textSize = 24f
                        gravity = Gravity.CENTER
                    })

                    addView(TextView(context).apply {
                        text = "Camera"
                        setTextColor(0xFF9ca3af.toInt())
                        textSize = 11f
                        gravity = Gravity.CENTER
                    })
                })
            })

            // Files shortcut
            addView(CardView(context).apply {
                radius = 12f
                cardElevation = 2f
                setCardBackgroundColor(0xFF1e293b.toInt())
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    .apply { setMargins(4, 0, 4, 0) }
                setOnClickListener { quickAddToVault("files") }

                addView(LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    setPadding(12, 16, 12, 16)

                    addView(TextView(context).apply {
                        text = "📁"
                        textSize = 24f
                        gravity = Gravity.CENTER
                    })

                    addView(TextView(context).apply {
                        text = "Files"
                        setTextColor(0xFF9ca3af.toInt())
                        textSize = 11f
                        gravity = Gravity.CENTER
                    })
                })
            })

            // Apps shortcut
            addView(CardView(context).apply {
                radius = 12f
                cardElevation = 2f
                setCardBackgroundColor(0xFF1e293b.toInt())
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    .apply { setMargins(4, 0, 4, 0) }
                setOnClickListener { quickAddToVault("apps") }

                addView(LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    setPadding(12, 16, 12, 16)

                    addView(TextView(context).apply {
                        text = "📱"
                        textSize = 24f
                        gravity = Gravity.CENTER
                    })

                    addView(TextView(context).apply {
                        text = "Apps"
                        setTextColor(0xFF9ca3af.toInt())
                        textSize = 11f
                        gravity = Gravity.CENTER
                    })
                })
            })
        }
    }

    private fun createActivityFeed(): CardView {
        val actions = listOf(
            Triple("🎬", "Screen Record", "Record screen + audio") to { openScreenRecord() },
            Triple("🔧", "ADB Tools", "Debug & control") to { openAdbTools() },
            Triple("📦", "OTA Updates", "Check for updates") to { openOtaUpdater() },
            Triple("💾", "Storage", "Manage storage") to { openStorageManager() },
            Triple("🎨", "Themes", "Change look") to { openThemeStudio() }
        )

        return CardView(this).apply {
            radius = 16f
            cardElevation = 4f
            setCardBackgroundColor(0xFF1e293b.toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 8, 0, 16) }

            addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(20, 16, 20, 16)

                addView(TextView(context).apply {
                    text = "⚡ Quick Actions"
                    setTypeface(Typeface.DEFAULT_BOLD)
                    textSize = 14f
                    setTextColor(Color.WHITE)
                    setPadding(0, 0, 0, 12)
                })

                actions.forEachIndexed { index, (info, action) ->
                    val (emoji, title, desc) = info
                    addView(LinearLayout(context).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        setPadding(0, 8, 0, 8)
                        setOnClickListener { action() }

                        addView(TextView(context).apply {
                            text = emoji
                            textSize = 20f
                        })

                        addView(LinearLayout(context).apply {
                            orientation = LinearLayout.VERTICAL
                            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                                .apply { marginStart = 12 }

                            addView(TextView(context).apply {
                                text = title
                                setTextColor(Color.WHITE)
                                textSize = 14f
                            })

                            addView(TextView(context).apply {
                                text = desc
                                setTextColor(0xFF9ca3af.toInt())
                                textSize = 12f
                            })
                        })

                        addView(TextView(context).apply {
                            text = "→"
                            setTextColor(0xFF9ca3af.toInt())
                            textSize = 18f
                        })
                    })

                    if (index < actions.size - 1) {
                        addView(View(context).apply {
                            setBackgroundColor(0xFF2d2d3d.toInt())
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT, 1
                            ).apply { setMargins(0, 4, 0, 4) }
                        })
                    }
                }
            })
        }
    }

    private fun createDrawerContent(): LinearLayout {
        val drawer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF1e1e2e.toInt())
        }

        // Drawer header
        drawer.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(24, 56, 24, 24)
            setBackgroundColor(0xFF0a0e27.toInt())

            addView(TextView(this@MainActivity).apply {
                text = "👤 Profile"
                setTypeface(Typeface.DEFAULT_BOLD)
                textSize = 20f
                setTextColor(Color.WHITE)
            })

            addView(View(this@MainActivity).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })

            addView(Button(this@MainActivity).apply {
                text = "✕"
                setTextColor(0xFF9ca3af.toInt())
                setBackgroundColor(Color.TRANSPARENT)
                setOnClickListener { drawerLayout.closeDrawer(GravityCompat.END) }
            })
        })

        drawer.addView(View(this).apply {
            setBackgroundColor(0xFF2d2d3d.toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 2
            )
        })

        // Tools section
        drawer.addView(TextView(this).apply {
            text = "🛠️ TOOLS"
            setTypeface(Typeface.DEFAULT_BOLD)
            textSize = 12f
            setTextColor(0xFF9ca3af.toInt())
            setPadding(24, 24, 24, 8)
        })

        val tools = listOf(
            Triple("🔧", "ADB Control", "Connect & manage devices") to { openAdbTools() },
            Triple("⚡", "Fastboot Tools", "Flash partitions") to { Toast.makeText(this@MainActivity, "Fastboot - Coming soon", Toast.LENGTH_SHORT).show() },
            Triple("📜", "Script Runner", "Execute shell scripts") to { Toast.makeText(this@MainActivity, "Script Runner - Coming soon", Toast.LENGTH_SHORT).show() },
            Triple("📦", "Package Manager", "Install & manage apps") to { Toast.makeText(this@MainActivity, "Packages - Coming soon", Toast.LENGTH_SHORT).show() },
            Triple("🎬", "Screen Capture", "Record screen + audio") to { openScreenRecord() },
            Triple("🎨", "Theme Studio", "Customize look & feel") to { openThemeStudio() },
            Triple("📡", "Network Tools", "WiFi, hotspot, tethering") to { Toast.makeText(this@MainActivity, "Network - Coming soon", Toast.LENGTH_SHORT).show() },
            Triple("🔒", "Privacy Manager", "Permissions & data") to { Toast.makeText(this@MainActivity, "Privacy - Coming soon", Toast.LENGTH_SHORT).show() }
        )

        val scrollDrawer = ScrollView(this)
        val toolsLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, 32)
        }

        tools.forEach { (info, action) ->
            val (emoji, title, desc) = info
            toolsLayout.addView(LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(24, 16, 24, 16)
                setOnClickListener {
                    drawerLayout.closeDrawer(GravityCompat.END)
                    action()
                }

                addView(TextView(context).apply {
                    text = emoji
                    textSize = 24f
                })

                addView(LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                        .apply { marginStart = 16 }

                    addView(TextView(context).apply {
                        text = title
                        setTextColor(Color.WHITE)
                        textSize = 15f
                    })

                    addView(TextView(context).apply {
                        text = desc
                        setTextColor(0xFF9ca3af.toInt())
                        textSize = 12f
                    })
                })

                addView(TextView(context).apply {
                    text = "→"
                    setTextColor(0xFF9ca3af.toInt())
                    textSize = 18f
                })
            })

            toolsLayout.addView(View(this).apply {
                setBackgroundColor(0xFF2d2d3d.toInt())
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 1
                ).apply { setMargins(64, 0, 24, 0) }
            })
        }

        scrollDrawer.addView(toolsLayout)
        drawer.addView(scrollDrawer)
        return drawer
    }

    private fun openVault() {
        if (!vaultManager.isVaultInitialized() || vaultManager.isLocked()) {
            startActivity(Intent(this, VaultLockActivity::class.java))
        } else {
            startActivity(Intent(this, VaultActivity::class.java))
        }
    }

    private fun quickAddToVault(category: String) {
        if (vaultManager.isLocked()) {
            startActivity(Intent(this, VaultLockActivity::class.java))
        } else {
            startActivity(Intent(this, VaultActivity::class.java))
        }
    }

    private fun openScreenRecord() {
        if (!Settings.canDrawOverlays(this)) {
            AlertDialog.Builder(this)
                .setTitle("Overlay Permission")
                .setMessage("Screen recording needs overlay permission.")
                .setPositiveButton("Grant") { _, _ ->
                    startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")))
                }
                .setNegativeButton("Cancel", null)
                .show()
        } else {
            Toast.makeText(this, "Screen recording - Tap vault feature", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openAdbTools() {
        try {
            startActivity(Intent(this, com.sovereign.app.adb.AdbControlActivity::class.java))
        } catch (e: Exception) {
            Toast.makeText(this, "ADB: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun openOtaUpdater() {
        try {
            startActivity(Intent(this, com.sovereign.app.updater.UpdaterActivity::class.java))
        } catch (e: Exception) {
            Toast.makeText(this, "OTA: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun openStorageManager() {
        try {
            startActivity(Intent(this, com.sovereign.app.storage.StorageManagerActivity::class.java))
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

    private fun formatSize(bytes: Long): String {
        return when {
            bytes >= 1_073_741_824 -> String.format("%.1f GB", bytes / 1_073_741_824.0)
            bytes >= 1_048_576 -> String.format("%.1f MB", bytes / 1_048_576.0)
            bytes >= 1024 -> String.format("%.1f KB", bytes / 1024.0)
            else -> "$bytes B"
        }
    }

    override fun onResume() {
        super.onResume()
        if (::drawerLayout.isInitialized) {
            drawerLayout.closeDrawer(GravityCompat.END)
        }
    }

    @Deprecated("Use OnBackPressedDispatcher")
    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
            drawerLayout.closeDrawer(GravityCompat.END)
        } else {
            super.onBackPressed()
        }
    }
}
