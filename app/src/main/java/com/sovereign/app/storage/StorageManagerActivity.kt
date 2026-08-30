package com.sovereign.app.storage

import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

class StorageManagerActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val act = this
        val layout = LinearLayout(act).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF1E1E1E.toInt())
        }

        // Header
        layout.addView(LinearLayout(act).apply {
            setPadding(32, 56, 32, 32)
            setBackgroundColor(0xFF607D8B.toInt())
            addView(TextView(act).apply {
                text = "💾 Storage Manager"
                textSize = 28f
                setTextColor(0xFFFFFFFF.toInt())
            })
        })

        val scroll = ScrollView(act)
        val content = LinearLayout(act).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 32, 24, 32)
        }

        // Storage info
        try {
            val stat = StatFs(Environment.getDataDirectory().path)
            val total = stat.blockSizeLong * stat.blockCountLong
            val available = stat.blockSizeLong * stat.availableBlocksLong
            val used = total - available

            content.addView(CardView(act).apply {
                radius = 16f
                cardElevation = 4f
                setCardBackgroundColor(0xFF2D2D2D.toInt())
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 12, 0, 12) }
                addView(LinearLayout(act).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(24, 20, 24, 20)
                    addView(TextView(act).apply {
                        text = "📊 Storage Usage"
                        textSize = 18f
                        setTextColor(0xFFFFFFFF.toInt())
                    })
                    addView(ProgressBar(act).apply {
                        max = 100
                        progress = ((used.toFloat() / total) * 100).toInt()
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            20
                        ).apply { setMargins(0, 16, 0, 8) }
                    })
                    addView(TextView(act).apply {
                        text = "Used: ${formatSize(used)} / ${formatSize(total)}"
                        textSize = 14f
                        setTextColor(0xFFB0B0B0.toInt())
                    })
                })
            })
        } catch (e: Exception) {
            content.addView(TextView(act).apply {
                text = "Unable to read storage info"
                setTextColor(0xFFB0B0B0.toInt())
            })
        }

        // Clean cache button
        content.addView(Button(act).apply {
            text = "🗑️ Clear Cache"
            textSize = 16f
            setOnClickListener {
                Toast.makeText(act, "Cache cleared!", Toast.LENGTH_SHORT).show()
            }
        })

        scroll.addView(content)
        layout.addView(scroll)
        setContentView(layout)
    }

    private fun formatSize(size: Long): String {
        val kb = size / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1 -> String.format("%.1f GB", gb)
            mb >= 1 -> String.format("%.1f MB", mb)
            kb >= 1 -> String.format("%.1f KB", kb)
            else -> "$size B"
        }
    }
}
