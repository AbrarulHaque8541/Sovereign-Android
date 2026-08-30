package com.sovereign.app.updater

import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

class UpdaterActivity : AppCompatActivity() {

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
            setBackgroundColor(0xFFFF5722.toInt())
            addView(TextView(act).apply {
                text = "📦 OTA Updates"
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
                    text = "Current Version"
                    textSize = 14f
                    setTextColor(0xFFB0B0B0.toInt())
                })
                addView(TextView(act).apply {
                    text = "v1.2.0"
                    textSize = 24f
                    setTextColor(0xFFFFFFFF.toInt())
                })
            })
        })

        content.addView(Button(act).apply {
            text = "🔍 Check for Updates"
            textSize = 16f
            setOnClickListener {
                Toast.makeText(act, "You are on the latest version!", Toast.LENGTH_SHORT).show()
            }
        })

        scroll.addView(content)
        layout.addView(scroll)
        setContentView(layout)
    }
}
