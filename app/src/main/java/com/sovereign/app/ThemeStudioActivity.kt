package com.sovereign.app

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

class ThemeStudioActivity : AppCompatActivity() {

    private lateinit var prefs: android.content.SharedPreferences
    private lateinit var currentTheme: ThemeEngine.Theme

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences("sovereign_prefs", Context.MODE_PRIVATE)
        loadCurrentTheme()
        setupUI()
    }

    private fun loadCurrentTheme() {
        val savedThemeName = prefs.getString("theme_id", "LIQUID_GLASS") ?: "LIQUID_GLASS"
        currentTheme = try {
            ThemeEngine.getTheme(ThemeEngine.ThemeId.valueOf(savedThemeName))
        } catch (e: Exception) {
            ThemeEngine.getTheme(ThemeEngine.ThemeId.LIQUID_GLASS)
        }
    }

    private fun setupUI() {
        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(currentTheme.background)
        }

        // Header
        mainLayout.addView(createHeader())

        // Scrollable content
        val scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }

        val contentLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 16, 24, 100)
        }

        // Subtitle
        contentLayout.addView(TextView(this).apply {
            text = "Tap any theme to apply instantly"
            textSize = 14f
            setTextColor(currentTheme.textSecondary)
            setPadding(0, 0, 0, 24)
        })

        // Theme list
        ThemeEngine.getAllThemes().forEach { theme ->
            contentLayout.addView(createThemeItem(theme))
        }

        scrollView.addView(contentLayout)
        mainLayout.addView(scrollView)
        setContentView(mainLayout)
    }

    private fun createHeader(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 48, 24, 32)

            val gradient = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(currentTheme.primary, currentTheme.secondary)
            )
            background = gradient

            addView(TextView(context).apply {
                text = "🎨 Theme Studio"
                textSize = 24f
                setTypeface(Typeface.DEFAULT_BOLD)
                setTextColor(Color.WHITE)
            })
        }
    }

    private fun createThemeItem(theme: ThemeEngine.Theme): CardView {
        val isActive = theme.id == currentTheme.id

        return CardView(this).apply {
            radius = 16f
            cardElevation = 4f
            setCardBackgroundColor(theme.surface)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 12, 0, 12) }

            if (isActive) {
                background = GradientDrawable().apply {
                    setColor(theme.surface)
                    setStroke(4, theme.accent)
                    cornerRadius = 16f
                }
            }

            setOnClickListener {
                applyTheme(theme)
            }

            val content = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(20, 16, 20, 16)
            }

            // Emoji
            content.addView(TextView(context).apply {
                text = theme.emoji
                textSize = 36f
                layoutParams = LinearLayout.LayoutParams(80, 80)
                gravity = Gravity.CENTER
            })

            // Info
            val infoCol = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    .apply { marginStart = 16 }
            }

            infoCol.addView(TextView(context).apply {
                text = theme.name
                textSize = 18f
                setTypeface(Typeface.DEFAULT_BOLD)
                setTextColor(theme.textPrimary)
            })

            infoCol.addView(TextView(context).apply {
                text = theme.description
                textSize = 12f
                setTextColor(theme.textSecondary)
                setPadding(0, 4, 0, 0)
            })

            content.addView(infoCol)

            // Active indicator or arrow
            content.addView(if (isActive) {
                TextView(context).apply {
                    text = "✓"
                    textSize = 24f
                    setTextColor(theme.accent)
                    setTypeface(Typeface.DEFAULT_BOLD)
                }
            } else {
                TextView(context).apply {
                    text = "→"
                    textSize = 20f
                    setTextColor(theme.textSecondary)
                }
            })

            addView(content)
        }
    }

    private fun applyTheme(theme: ThemeEngine.Theme) {
        // Save immediately
        prefs.edit().putString("theme_id", theme.id.name).apply()

        Toast.makeText(this, "${theme.emoji} ${theme.name} applied!", Toast.LENGTH_SHORT).show()

        // Recreate activity with new theme
        recreate()
    }
}
