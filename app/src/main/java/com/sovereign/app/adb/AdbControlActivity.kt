package com.sovereign.app.adb

import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

class AdbControlActivity : AppCompatActivity() {

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
            setBackgroundColor(0xFF6200EE.toInt())
            addView(TextView(act).apply {
                text = "🔧 ADB Tools"
                textSize = 28f
                setTextColor(0xFFFFFFFF.toInt())
            })
        })

        // Content
        val scroll = ScrollView(act)
        val content = LinearLayout(act).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 32, 24, 32)
        }

        val features = listOf(
            "📱 Device Info" to "View connected device details",
            "📋 Shell Commands" to "Execute ADB shell commands",
            "📁 File Transfer" to "Push/pull files to device",
            "🔄 App Install" to "Install APK via ADB",
            "🔌 Reboot" to "Reboot device to different modes"
        )

        features.forEach { (title, _) ->
            content.addView(CardView(act).apply {
                radius = 16f
                cardElevation = 4f
                setCardBackgroundColor(0xFF2D2D2D.toInt())
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 12, 0, 12) }
                addView(LinearLayout(act).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(20, 20, 20, 20)
                    gravity = Gravity.CENTER_VERTICAL
                    addView(TextView(act).apply {
                        text = title
                        textSize = 16f
                        setTextColor(0xFFFFFFFF.toInt())
                    })
                })
            })
        }

        scroll.addView(content)
        layout.addView(scroll)
        setContentView(layout)
    }
}
