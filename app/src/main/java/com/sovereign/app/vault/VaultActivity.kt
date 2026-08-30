package com.sovereign.app.vault

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.cardview.widget.CardView
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class VaultActivity : AppCompatActivity() {

    private lateinit var vaultManager: VaultManager
    private var currentCategory: String = VaultManager.PHOTOS_DIR
    private var currentTabIndex = 0
    private lateinit var tabContainer: LinearLayout
    private lateinit var contentContainer: FrameLayout
    private lateinit var statsContainer: LinearLayout
    private var selectedFiles = mutableListOf<String>()

    private val tabs = listOf(
        Triple(VaultManager.PHOTOS_DIR, "📷", "Photos"),
        Triple(VaultManager.VIDEOS_DIR, "🎬", "Videos"),
        Triple(VaultManager.FILES_DIR, "📁", "Files"),
        Triple(VaultManager.APPS_DIR, "📱", "Apps"),
        Triple(VaultManager.MESSAGES_DIR, "💬", "Messages"),
        Triple(VaultManager.CALLS_DIR, "📞", "Calls")
    )

    private val pickMedia = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        uris?.let { importFiles(it) }
    }

    private val pickFile = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { importFiles(listOf(it)) }
    }

    private val requestPermission = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
        if (perms.values.all { it }) {
            pickMedia.launch("*/*")
        } else {
            Toast.makeText(this, "Permission required to import files", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vaultManager = VaultManager.getInstance(this)

        if (vaultManager.isLocked()) {
            finish()
            return
        }

        setupUI()
    }

    private fun setupUI() {
        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF0a0e27.toInt())
        }

        // Header
        mainLayout.addView(createHeader())

        // Stats row
        statsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(16, 12, 16, 12)
        }
        updateStats()
        mainLayout.addView(statsContainer)

        // Tab container
        tabContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(0xFF1e1e2e.toInt())
            setPadding(8, 8, 8, 8)
        }
        tabs.forEachIndexed { index, (cat, emoji, name) ->
            tabContainer.addView(createTab(emoji, name, index))
        }
        mainLayout.addView(tabContainer)

        // Content area
        contentContainer = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0, 1f
            )
        }
        mainLayout.addView(contentContainer)

        // Action bar
        mainLayout.addView(createActionBar())

        setContentView(mainLayout)
        showCategory(currentCategory)
    }

    private fun createHeader(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(20, 48, 20, 16)
            setBackgroundColor(0xFF1a1a2e.toInt())

            addView(TextView(context).apply {
                text = "🔒 Private Vault"
                setTextColor(0xFFFFFFFF.toInt())
                textSize = 22f
                setTypeface(android.graphics.Typeface.DEFAULT_BOLD)
            })

            addView(View(context).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })

            addView(ImageButton(context).apply {
                setImageResource(android.R.drawable.ic_menu_add)
                setColorFilter(0xFFef4444.toInt())
                setBackgroundColor(0x00000000)
                setOnClickListener { showAddOptions() }
                layoutParams = LinearLayout.LayoutParams(64, 64)
            })

            addView(ImageButton(context).apply {
                setImageResource(android.R.drawable.ic_menu_more)
                setColorFilter(0xFFFFFFFF.toInt())
                setBackgroundColor(0x00000000)
                setOnClickListener { showVaultSettings() }
                layoutParams = LinearLayout.LayoutParams(64, 64)
            })
        }
    }

    private fun createTab(emoji: String, name: String, index: Int): Button {
        val isSelected = index == currentTabIndex
        return Button(this).apply {
            text = "$emoji"
            textSize = 18f
            setTextColor(if (isSelected) 0xFFef4444.toInt() else 0xFF9ca3af.toInt())
            setBackgroundColor(if (isSelected) 0xFF2d1f3d.toInt() else 0x00000000)
            layoutParams = LinearLayout.LayoutParams(0, 80, 1f)
            setOnClickListener {
                currentTabIndex = index
                currentCategory = tabs[index].first
                refreshTabs()
                showCategory(currentCategory)
            }
        }
    }

    private fun refreshTabs() {
        tabs.forEachIndexed { index, (_, emoji, _) ->
            val btn = tabContainer.getChildAt(index) as? Button
            btn?.apply {
                setTextColor(if (index == currentTabIndex) 0xFFef4444.toInt() else 0xFF9ca3af.toInt())
                setBackgroundColor(if (index == currentTabIndex) 0xFF2d1f3d.toInt() else 0x00000000)
            }
        }
    }

    private fun updateStats() {
        statsContainer.removeAllViews()
        val stats = vaultManager.getCategoryStats()
        val totalSize = vaultManager.getVaultSize()
        val totalCount = vaultManager.getTotalCount()

        val totalSizeStr = formatSize(totalSize)

        statsContainer.addView(TextView(this).apply {
            text = "📦 $totalCount items • $totalSizeStr"
            setTextColor(0xFF9ca3af.toInt())
            textSize = 13f
        })
    }

    private fun showCategory(category: String) {
        contentContainer.removeAllViews()
        val scrollView = ScrollView(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
        }

        val dir = vaultManager.getCategoryDirectory(category)
        val files = dir.listFiles()?.sortedByDescending { it.lastModified() } ?: emptyList()

        if (files.isEmpty()) {
            content.addView(createEmptyState(category))
        } else {
            files.forEach { file ->
                content.addView(createFileItem(file))
            }
        }

        scrollView.addView(content)
        contentContainer.addView(scrollView)
    }

    private fun createEmptyState(category: String): LinearLayout {
        val emoji = tabs.find { it.first == category }?.second ?: "📁"
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            setPadding(32, 80, 32, 32)

            addView(TextView(context).apply {
                text = emoji
                textSize = 64f
                gravity = android.view.Gravity.CENTER
            })

            addView(TextView(context).apply {
                text = "No $category yet"
                setTextColor(0xFF9ca3af.toInt())
                textSize = 16f
                gravity = android.view.Gravity.CENTER
                setPadding(0, 16, 0, 8)
            })

            addView(TextView(context).apply {
                text = "Tap + to add files"
                setTextColor(0xFF6b7280.toInt())
                textSize = 13f
                gravity = android.view.Gravity.CENTER
            })
        }
    }

    private fun createFileItem(file: File): CardView {
        val isPhoto = file.extension.lowercase() in listOf("jpg", "jpeg", "png", "gif", "webp")
        val isVideo = file.extension.lowercase() in listOf("mp4", "mkv", "avi", "mov", "webm")

        return CardView(this).apply {
            radius = 12f
            cardElevation = 2f
            setCardBackgroundColor(0xFF1e293b.toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 8, 0, 8) }

            addView(LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(16, 12, 16, 12)

                // Thumbnail
                addView(if (isPhoto) {
                    ImageView(context).apply {
                        setImageURI(Uri.fromFile(file))
                        layoutParams = LinearLayout.LayoutParams(60, 60)
                        scaleType = ImageView.ScaleType.CENTER_CROP
                    }
                } else {
                    TextView(context).apply {
                        text = if (isVideo) "🎬" else getFileEmoji(file.extension)
                        textSize = 32f
                        gravity = android.view.Gravity.CENTER
                        layoutParams = LinearLayout.LayoutParams(60, 60)
                    }
                })

                // Info
                addView(LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                        .apply { marginStart = 12 }

                    addView(TextView(context).apply {
                        text = file.name
                        setTextColor(0xFFFFFFFF.toInt())
                        textSize = 14f
                        maxLines = 1
                    })

                    addView(TextView(context).apply {
                        text = "${formatSize(file.length())} • ${formatDate(file.lastModified())}"
                        setTextColor(0xFF9ca3af.toInt())
                        textSize = 12f
                    })
                })

                // Actions
                addView(ImageButton(context).apply {
                    setImageResource(android.R.drawable.ic_menu_delete)
                    setColorFilter(0xFFef4444.toInt())
                    setBackgroundColor(0x00000000)
                    setOnClickListener {
                        AlertDialog.Builder(context)
                            .setTitle("Delete?")
                            .setMessage("Remove this file from vault?")
                            .setPositiveButton("Delete") { _, _ ->
                                file.delete()
                                showCategory(currentCategory)
                                updateStats()
                                Toast.makeText(context, "Deleted", Toast.LENGTH_SHORT).show()
                            }
                            .setNegativeButton("Cancel", null)
                            .show()
                    }
                })
            })
        }
    }

    private fun createActionBar(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(0xFF1e1e2e.toInt())
            setPadding(16, 12, 16, 12)
            gravity = android.view.Gravity.CENTER

            addView(Button(this@VaultActivity).apply {
                text = "🔒 Lock Vault"
                setTextColor(0xFFFFFFFF.toInt())
                setBackgroundColor(0xFFef4444.toInt())
                setOnClickListener {
                    vaultManager.lock()
                    Toast.makeText(context, "🔒 Vault Locked", Toast.LENGTH_SHORT).show()
                    finish()
                }
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    .apply { marginEnd = 8 }
            })

            addView(Button(this@VaultActivity).apply {
                text = "+ Add Files"
                setTextColor(0xFFFFFFFF.toInt())
                setBackgroundColor(0xFF374151.toInt())
                setOnClickListener { showAddOptions() }
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    .apply { marginStart = 8 }
            })
        }
    }

    private fun showAddOptions() {
        val options = when (currentCategory) {
            VaultManager.PHOTOS_DIR -> arrayOf("📷 From Gallery", "📷 From Camera")
            VaultManager.VIDEOS_DIR -> arrayOf("🎬 From Gallery", "🎬 From Camera")
            VaultManager.FILES_DIR -> arrayOf("📁 Select File", "📄 Select Documents")
            else -> arrayOf("📁 Import Files")
        }

        AlertDialog.Builder(this)
            .setTitle("Add to ${tabs[currentTabIndex].third}")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> pickMedia.launch("*/*")
                    1 -> pickFile.launch("*/*")
                }
            }
            .show()
    }

    private fun showVaultSettings() {
        val options = arrayOf(
            "🔐 Change PIN",
            "👆 Enable Fingerprint",
            "⏱️ Auto-lock Settings",
            "🎭 Decoy Mode",
            "🔄 Reset Vault"
        )

        AlertDialog.Builder(this)
            .setTitle("Vault Settings")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> Toast.makeText(this, "Change PIN - Coming soon", Toast.LENGTH_SHORT).show()
                    1 -> {
                        vaultManager.enableFingerprint(true)
                        Toast.makeText(this, "Fingerprint enabled!", Toast.LENGTH_SHORT).show()
                    }
                    2 -> showAutoLockDialog()
                    3 -> showDecoyModeDialog()
                    4 -> confirmResetVault()
                }
            }
            .show()
    }

    private fun showAutoLockDialog() {
        val options = arrayOf("30 seconds", "1 minute", "5 minutes", "Disabled")
        AlertDialog.Builder(this)
            .setTitle("Auto-lock Timeout")
            .setItems(options) { _, which ->
                val timeout = when (which) {
                    0 -> VaultManager.AUTO_LOCK_30_SEC
                    1 -> VaultManager.AUTO_LOCK_1_MIN
                    2 -> VaultManager.AUTO_LOCK_5_MIN
                    else -> VaultManager.AUTO_LOCK_DISABLED
                }
                vaultManager.setAutoLockTimeout(timeout)
                Toast.makeText(this, "Auto-lock set", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun showDecoyModeDialog() {
        AlertDialog.Builder(this)
            .setTitle("Decoy Mode")
            .setMessage("Fake vault shows empty when entered with decoy PIN. Enable?")
            .setPositiveButton("Enable") {
                _, _ -> vaultManager.setDecoyMode(VaultManager.DECOY_MODE_FAKE)
                Toast.makeText(this, "Decoy mode enabled", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Disable") { _, _ ->
                vaultManager.setDecoyMode(VaultManager.DECOY_MODE_NONE)
                Toast.makeText(this, "Decoy mode disabled", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun confirmResetVault() {
        AlertDialog.Builder(this)
            .setTitle("⚠️ Reset Vault?")
            .setMessage("This will DELETE ALL hidden files. This cannot be undone!")
            .setPositiveButton("DELETE ALL") { _, _ ->
                vaultManager.resetVault()
                Toast.makeText(this, "Vault reset", Toast.LENGTH_SHORT).show()
                showCategory(currentCategory)
                updateStats()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun importFiles(uris: List<Uri>) {
        var imported = 0
        uris.forEach { uri ->
            try {
                val inputStream = contentResolver.openInputStream(uri) ?: return@forEach
                val fileName = getFileName(uri) ?: "file_${System.currentTimeMillis()}"
                val destFile = File(vaultManager.getCategoryDirectory(currentCategory), fileName)

                FileOutputStream(destFile).use { output ->
                    inputStream.copyTo(output)
                }
                inputStream.close()
                imported++
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (imported > 0) {
            Toast.makeText(this, "✅ $imported files added to vault", Toast.LENGTH_SHORT).show()
            showCategory(currentCategory)
            updateStats()
        } else {
            Toast.makeText(this, "Failed to import files", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getFileName(uri: Uri): String? {
        var name: String? = null
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex >= 0) {
                name = cursor.getString(nameIndex)
            }
        }
        return name ?: "file_${System.currentTimeMillis()}"
    }

    private fun formatSize(bytes: Long): String {
        return when {
            bytes >= 1_073_741_824 -> String.format("%.1f GB", bytes / 1_073_741_824.0)
            bytes >= 1_048_576 -> String.format("%.1f MB", bytes / 1_048_576.0)
            bytes >= 1024 -> String.format("%.1f KB", bytes / 1024.0)
            else -> "$bytes B"
        }
    }

    private fun formatDate(timestamp: Long): String {
        return SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(timestamp))
    }

    private fun getFileEmoji(ext: String): String {
        return when (ext.lowercase()) {
            "pdf" -> "📄"
            "doc", "docx" -> "📝"
            "xls", "xlsx" -> "📊"
            "zip", "rar", "7z" -> "📦"
            "mp3", "wav", "aac" -> "🎵"
            "txt" -> "📃"
            else -> "📁"
        }
    }
}
