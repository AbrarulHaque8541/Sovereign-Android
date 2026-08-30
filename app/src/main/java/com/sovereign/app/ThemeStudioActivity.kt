package com.sovereign.app

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat

class ThemeStudioActivity : AppCompatActivity() {

    private lateinit var prefs: android.content.SharedPreferences
    private lateinit var themeEngine: ThemeEngine
    private lateinit var currentTheme: ThemeEngine.Theme
    private lateinit var rootLayout: ScrollView
    private lateinit var contentContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        prefs = getSharedPreferences("sovereign_prefs", Context.MODE_PRIVATE)
        themeEngine = ThemeEngine(this)
        
        val savedThemeId = prefs.getString("theme_id", "LIQUID_GLASS") ?: "LIQUID_GLASS"
        currentTheme = ThemeEngine.getTheme(ThemeEngine.ThemeId.valueOf(savedThemeId))
        
        setupUI()
        applyTheme()
    }

    private fun setupUI() {
        rootLayout = ScrollView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        contentContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        // Header
        val header = createHeader()
        contentContainer.addView(header)

        // Theme Grid
        val themeGrid = createThemeGrid()
        contentContainer.addView(themeGrid)

        // Preview Section
        val previewSection = createPreviewSection()
        contentContainer.addView(previewSection)

        rootLayout.addView(contentContainer)
        setContentView(rootLayout)
    }

    private fun createHeader(): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(32, 48, 32, 32)
            setBackgroundColor(darken(currentTheme.background, 0.1f))

            addView(TextView(context).apply {
                text = "🎨 Theme Studio"
                textSize = 28f
                setTypeface(Typeface.DEFAULT_BOLD)
                setTextColor(currentTheme.textPrimary)
                gravity = Gravity.CENTER
            })

            addView(TextView(context).apply {
                text = "Express your style with premium themes"
                textSize = 14f
                setTextColor(currentTheme.textSecondary)
                gravity = Gravity.CENTER
                setPadding(0, 8, 0, 0)
            })
        }
    }

    private fun createThemeGrid(): View {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
        }

        ThemeEngine.getAllThemes().forEach { theme ->
            val card = createThemeCard(theme)
            container.addView(card)
        }

        return container
    }

    private fun createThemeCard(theme: ThemeEngine.Theme): View {
        val isActive = theme.id == currentTheme.id

        return CardView(this).apply {
            radius = theme.borderRadius
            cardElevation = theme.cardElevation
            setCardBackgroundColor(theme.surface)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 12, 0, 12) }

            if (isActive) {
                val glowDrawable = GradientDrawable().apply {
                    setColor(theme.surface)
                    setStroke(6, theme.accent)
                    cornerRadius = theme.borderRadius
                }
                background = glowDrawable
            }

            setOnClickListener {
                applyThemeWithAnimation(theme)
            }

            val content = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(24, 20, 24, 20)
            }

            // Top row: Emoji + Name + Status
            val topRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            topRow.addView(TextView(context).apply {
                text = theme.emoji
                textSize = 40f
                layoutParams = LinearLayout.LayoutParams(80, 80)
                gravity = Gravity.CENTER
            })

            val infoColumn = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    .apply { marginStart = 16 }
            }

            infoColumn.addView(TextView(context).apply {
                text = theme.name
                textSize = 18f
                setTypeface(Typeface.DEFAULT_BOLD)
                setTextColor(theme.textPrimary)
            })

            infoColumn.addView(TextView(context).apply {
                text = theme.description
                textSize = 12f
                setTextColor(theme.textSecondary)
                setPadding(0, 4, 0, 0)
            })

            topRow.addView(infoColumn)

            if (isActive) {
                topRow.addView(TextView(context).apply {
                    text = "✓ Active"
                    textSize = 14f
                    setTypeface(Typeface.DEFAULT_BOLD)
                    setTextColor(theme.accent)
                    setPadding(16, 0, 0, 0)
                })
            }

            content.addView(topRow)

            // Color palette preview
            val paletteRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 16, 0, 0)
            }

            listOf(theme.background, theme.surface, theme.primary, theme.secondary, theme.accent).forEach { color ->
                paletteRow.addView(View(context).apply {
                    setBackgroundColor(color)
                    layoutParams = LinearLayout.LayoutParams(48, 48).apply { marginEnd = 8 }
                })
            }

            content.addView(paletteRow)

            addView(content)

            // Apply animation based on theme type
            applyThemeAnimation(this, theme)
        }
    }

    private fun createPreviewSection(): View {
        return CardView(this).apply {
            radius = currentTheme.borderRadius
            cardElevation = currentTheme.cardElevation
            setCardBackgroundColor(currentTheme.surface)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { 
                setMargins(16, 24, 16, 16)
            }

            val content = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(24, 24, 24, 24)
            }

            content.addView(TextView(context).apply {
                text = "✨ Live Preview"
                textSize = 16f
                setTypeface(Typeface.DEFAULT_BOLD)
                setTextColor(currentTheme.textPrimary)
            })

            val previewCards = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, 16, 0, 0)
            }

            // Sample cards to show theme effect
            val sampleData = listOf(
                "📱 Screen Capture" to "Record your screen",
                "🔧 ADB Tools" to "Debug & control",
                "📦 OTA Updates" to "Stay updated"
            )

            sampleData.forEach { (emoji, text) ->
                previewCards.addView(themeEngine.createThemedCard(
                    previewCards,
                    currentTheme,
                    text,
                    emoji
                ) { })
            }

            content.addView(previewCards)
            addView(content)
        }
    }

    private fun applyThemeWithAnimation(newTheme: ThemeEngine.Theme) {
        // Save theme preference
        prefs.edit().putString("theme_id", newTheme.name).apply()

        // Animate transition
        val fadeOut = ObjectAnimator.ofFloat(contentContainer, "alpha", 1f, 0f)
        fadeOut.duration = 200

        val fadeIn = ObjectAnimator.ofFloat(contentContainer, "alpha", 0f, 1f)
        fadeIn.duration = 300

        fadeOut.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                currentTheme = newTheme
                setupUI()
                fadeIn.start()
            }
        })

        fadeOut.start()
    }

    private fun applyTheme() {
        contentContainer.setBackgroundColor(currentTheme.background)
    }

    private fun applyThemeAnimation(view: View, theme: ThemeEngine.Theme) {
        when (theme.animationType) {
            ThemeEngine.AnimationType.SPRING -> {
                val scaleX = ObjectAnimator.ofFloat(view, "scaleX", 0.95f, 1f)
                val scaleY = ObjectAnimator.ofFloat(view, "scaleY", 0.95f, 1f)
                AnimatorSet().apply {
                    playTogether(scaleX, scaleY)
                    duration = 400
                    interpolator = OvershootInterpolator(2f)
                    start()
                }
            }
            ThemeEngine.AnimationType.BOUNCY -> {
                view.setOnTouchListener { v, event ->
                    when (event.action) {
                        android.view.MotionEvent.ACTION_DOWN -> {
                            v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).start()
                        }
                        android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                            v.animate().scaleX(1f).scaleY(1f).setDuration(200)
                                .setInterpolator(OvershootInterpolator(3f)).start()
                        }
                    }
                    false
                }
            }
            ThemeEngine.AnimationType.FLOAT -> {
                val anim = ObjectAnimator.ofFloat(view, "translationY", 0f, -8f, 0f)
                anim.duration = 2000
                anim.repeatCount = ValueAnimator.INFINITE
                anim.repeatMode = ValueAnimator.REVERSE
                anim.interpolator = AccelerateDecelerateInterpolator()
                anim.start()
            }
            else -> {}
        }
    }

    private fun darken(color: Int, factor: Float): Int {
        val a = Color.alpha(color)
        val r = (Color.red(color) * (1 - factor)).toInt()
        val g = (Color.green(color) * (1 - factor)).toInt()
        val b = (Color.blue(color) * (1 - factor)).toInt()
        return Color.argb(a, r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255))
    }
}
