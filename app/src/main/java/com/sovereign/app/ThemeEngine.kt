package com.sovereign.app

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.*
import android.widget.*
import androidx.cardview.widget.CardView
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * 10 Unique Premium Themes - One Click Switchable
 * Inspired by: Lightswind UI, SmoothUI, Hover.dev, Motion, Magic UI, Aceternity UI
 * 100% Native Android - No Web Dependencies
 */
class ThemeEngine(private val context: Context) {

    enum class ThemeId {
        LIQUID_GLASS, NEON_CYBER, MINIMAL_ZEN, AURORA_DREAMS,
        BRUTALIST_BOLD, NATURE_ORGANIC, RETRO_PIXEL, LUXURY_GOLD,
        PLAYFUL_POP, FUTURISTIC_TECH
    }

    data class Theme(
        val id: ThemeId,
        val name: String,
        val emoji: String,
        val description: String,
        val background: Int,
        val surface: Int,
        val primary: Int,
        val secondary: Int,
        val accent: Int,
        val textPrimary: Int,
        val textSecondary: Int,
        val borderRadius: Float,
        val cardElevation: Float,
        val animationType: AnimationType,
        val hasGlow: Boolean = false,
        val hasScanlines: Boolean = false,
        val hasFloating: Boolean = false,
        val has3DEffect: Boolean = false,
        val hasParticles: Boolean = false
    )

    enum class AnimationType { SPRING, SMOOTH, QUICK, DRAMATIC, BOUNCY, FLOAT }

    companion object {
        fun getAllThemes(): List<Theme> = listOf(
            // Theme 1: Liquid Glass - Inspired by Lightswind UI
            Theme(
                id = ThemeId.LIQUID_GLASS,
                name = "Liquid Glass",
                emoji = "💎",
                description = "Glassmorphism with fluid effects",
                background = Color.parseColor("#667eea"),
                surface = Color.argb(40, 255, 255, 255),
                primary = Color.parseColor("#ffffff"),
                secondary = Color.argb(60, 255, 255, 255),
                accent = Color.parseColor("#6366f1"),
                textPrimary = Color.WHITE,
                textSecondary = Color.parseColor("#E0E0E0"),
                borderRadius = 24f,
                cardElevation = 12f,
                animationType = AnimationType.SMOOTH,
                hasGlow = true,
                hasFloating = true
            ),

            // Theme 2: Neon Cyber - Inspired by Cyberpunk UI
            Theme(
                id = ThemeId.NEON_CYBER,
                name = "Neon Cyber",
                emoji = "⚡",
                description = "Cyberpunk neon glow aesthetic",
                background = Color.parseColor("#0a0a0f"),
                surface = Color.parseColor("#1a1a2e"),
                primary = Color.parseColor("#00f3ff"),
                secondary = Color.parseColor("#ff00ff"),
                accent = Color.parseColor("#ffff00"),
                textPrimary = Color.parseColor("#00f3ff"),
                textSecondary = Color.parseColor("#ff00ff"),
                borderRadius = 8f,
                cardElevation = 0f,
                animationType = AnimationType.QUICK,
                hasGlow = true,
                hasScanlines = true
            ),

            // Theme 3: Minimal Zen - Inspired by SmoothUI
            Theme(
                id = ThemeId.MINIMAL_ZEN,
                name = "Minimal Zen",
                emoji = "🧘",
                description = "Ultra clean, whitespace focused",
                background = Color.parseColor("#fafafa"),
                surface = Color.WHITE,
                primary = Color.parseColor("#2c2c2c"),
                secondary = Color.parseColor("#f5f5f5"),
                accent = Color.parseColor("#4f46e5"),
                textPrimary = Color.parseColor("#1a1a1a"),
                textSecondary = Color.parseColor("#666666"),
                borderRadius = 12f,
                cardElevation = 2f,
                animationType = AnimationType.SPRING
            ),

            // Theme 4: Aurora Dreams - Inspired by Magic UI
            Theme(
                id = ThemeId.AURORA_DREAMS,
                name = "Aurora Dreams",
                emoji = "🌌",
                description = "Soft pastels, ethereal gradients",
                background = Color.parseColor("#f5f7fa"),
                surface = Color.parseColor("#ffffff"),
                primary = Color.parseColor("#a8edea"),
                secondary = Color.parseColor("#fed6e3"),
                accent = Color.parseColor("#d299c2"),
                textPrimary = Color.parseColor("#2d3748"),
                textSecondary = Color.parseColor("#718096"),
                borderRadius = 32f,
                cardElevation = 8f,
                animationType = AnimationType.FLOAT,
                hasFloating = true
            ),

            // Theme 5: Brutalist Bold - Inspired by Aceternity UI
            Theme(
                id = ThemeId.BRUTALIST_BOLD,
                name = "Brutalist Bold",
                emoji = "🔲",
                description = "Raw, edgy, high contrast",
                background = Color.WHITE,
                surface = Color.WHITE,
                primary = Color.BLACK,
                secondary = Color.parseColor("#ff0000"),
                accent = Color.parseColor("#ffff00"),
                textPrimary = Color.BLACK,
                textSecondary = Color.parseColor("#333333"),
                borderRadius = 0f,
                cardElevation = 8f,
                animationType = AnimationType.QUICK
            ),

            // Theme 6: Nature Organic - Inspired by SmoothUI
            Theme(
                id = ThemeId.NATURE_ORGANIC,
                name = "Nature Organic",
                emoji = "🌿",
                description = "Earth tones, organic shapes",
                background = Color.parseColor("#f4f1de"),
                surface = Color.parseColor("#e9edc9"),
                primary = Color.parseColor("#3d5a3d"),
                secondary = Color.parseColor("#6b8f71"),
                accent = Color.parseColor("#d4a373"),
                textPrimary = Color.parseColor("#2d3748"),
                textSecondary = Color.parseColor("#4a5568"),
                borderRadius = 40f,
                cardElevation = 4f,
                animationType = AnimationType.SPRING,
                hasFloating = true
            ),

            // Theme 7: Retro Pixel - Inspired by Magic UI
            Theme(
                id = ThemeId.RETRO_PIXEL,
                name = "Retro Pixel",
                emoji = "👾",
                description = "8-bit retro gaming aesthetic",
                background = Color.parseColor("#2d1b4e"),
                surface = Color.parseColor("#3d2b5e"),
                primary = Color.parseColor("#f8b862"),
                secondary = Color.parseColor("#854ec9"),
                accent = Color.parseColor("#4ec985"),
                textPrimary = Color.parseColor("#f8f8f8"),
                textSecondary = Color.parseColor("#e0e0e0"),
                borderRadius = 0f,
                cardElevation = 0f,
                animationType = AnimationType.QUICK,
                hasScanlines = true
            ),

            // Theme 8: Luxury Gold - Inspired by Aceternity UI
            Theme(
                id = ThemeId.LUXURY_GOLD,
                name = "Luxury Gold",
                emoji = "👑",
                description = "Premium dark with gold accents",
                background = Color.parseColor("#0a0a0a"),
                surface = Color.parseColor("#1a1a1a"),
                primary = Color.parseColor("#d4af37"),
                secondary = Color.parseColor("#f4e5c2"),
                accent = Color.parseColor("#c9a961"),
                textPrimary = Color.parseColor("#f5f5f5"),
                textSecondary = Color.parseColor("#d4af37"),
                borderRadius = 8f,
                cardElevation = 6f,
                animationType = AnimationType.SMOOTH,
                hasGlow = true
            ),

            // Theme 9: Playful Pop - Inspired by Hover.dev
            Theme(
                id = ThemeId.PLAYFUL_POP,
                name = "Playful Pop",
                emoji = "🎈",
                description = "Bright, vibrant, bouncy",
                background = Color.parseColor("#fff5f5"),
                surface = Color.parseColor("#ffffff"),
                primary = Color.parseColor("#ff6b6b"),
                secondary = Color.parseColor("#4ecdc4"),
                accent = Color.parseColor("#ffe66d"),
                textPrimary = Color.parseColor("#2d3748"),
                textSecondary = Color.parseColor("#718096"),
                borderRadius = 50f,
                cardElevation = 6f,
                animationType = AnimationType.BOUNCY,
                hasParticles = true
            ),

            // Theme 10: Futuristic Tech - Inspired by Lightswind UI
            Theme(
                id = ThemeId.FUTURISTIC_TECH,
                name = "Futuristic Tech",
                emoji = "🚀",
                description = "Holographic sci-fi aesthetic",
                background = Color.parseColor("#050508"),
                surface = Color.parseColor("#0f0f15"),
                primary = Color.parseColor("#00d9ff"),
                secondary = Color.parseColor("#7b2cbf"),
                accent = Color.parseColor("#ff006e"),
                textPrimary = Color.parseColor("#e0e0e0"),
                textSecondary = Color.parseColor("#00d9ff"),
                borderRadius = 4f,
                cardElevation = 0f,
                animationType = AnimationType.DRAMATIC,
                hasGlow = true,
                has3DEffect = true,
                hasParticles = true
            )
        )

        fun getTheme(id: ThemeId): Theme = getAllThemes().find { it.id == id }!!
    }

    /**
     * Apply theme to any view with smooth animation
     */
    fun applyTheme(view: View, theme: Theme, duration: Long = 300) {
        val anim = ValueAnimator.ofFloat(0f, 1f)
        anim.duration = duration
        anim.addUpdateListener { 
            view.alpha = it.animatedValue as Float
        }
        anim.start()

        view.setBackgroundColor(theme.background)
        
        when (theme.animationType) {
            AnimationType.SPRING -> animateSpring(view)
            AnimationType.SMOOTH -> animateSmooth(view)
            AnimationType.QUICK -> animateQuick(view)
            AnimationType.DRAMATIC -> animateDramatic(view)
            AnimationType.BOUNCY -> animateBouncy(view)
            AnimationType.FLOAT -> animateFloat(view)
        }
    }

    /**
     * Create animated card with theme effects
     */
    fun createThemedCard(parent: ViewGroup, theme: Theme, title: String, emoji: String, onClick: () -> Unit): View {
        val card = CardView(parent.context).apply {
            radius = theme.borderRadius
            cardElevation = theme.cardElevation
            setCardBackgroundColor(theme.surface)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 12, 0, 12) }
            setOnClickListener { onClick() }
        }

        val content = LinearLayout(parent.context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(24, 20, 24, 20)
        }

        // Emoji icon
        val icon = TextView(parent.context).apply {
            text = emoji
            textSize = 32f
            setTextColor(theme.accent)
            layoutParams = LinearLayout.LayoutParams(80, 80).apply { marginEnd = 16 }
            gravity = Gravity.CENTER
        }

        // Title
        val titleView = TextView(parent.context).apply {
            text = title
            textSize = 16f
            setTextColor(theme.textPrimary)
            if (theme.id == ThemeId.BRUTALIST_BOLD) {
                setTypeface(Typeface.MONOSPACE, Typeface.BOLD)
            }
        }

        content.addView(icon)
        content.addView(titleView)
        card.addView(content)

        if (theme.hasGlow) addGlowEffect(card, theme)
        if (theme.animationType == AnimationType.BOUNCY) addBounceEffect(card)

        return card
    }

    /**
     * Create theme switcher button
     */
    fun createThemeSwitcher(parent: ViewGroup, currentTheme: Theme, onThemeSelected: (Theme) -> Unit): View {
        val container = LinearLayout(parent.context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
            setBackgroundColor(currentTheme.surface)
        }

        val title = TextView(parent.context).apply {
            text = "🎨 Choose Your Theme"
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            setTextColor(currentTheme.textPrimary)
            setPadding(0, 0, 0, 16)
        }
        container.addView(title)

        // Grid of themes
        val grid = LinearLayout(parent.context).apply {
            orientation = LinearLayout.VERTICAL
        }

        getAllThemes().forEach { theme ->
            val themeCard = createThemePreview(parent.context, theme, currentTheme, onThemeSelected)
            grid.addView(themeCard)
        }

        val scroll = ScrollView(parent.context)
        scroll.addView(grid)
        container.addView(scroll)

        return container
    }

    private fun createThemePreview(ctx: Context, theme: Theme, current: Theme, onClick: (Theme) -> Unit): View {
        val isSelected = theme.id == current.id

        val card = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(16, 12, 16, 12)
            setBackgroundColor(theme.surface)
            
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 4, 0, 4)
            layoutParams = params

            if (isSelected) {
                background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(theme.surface)
                    setStroke(4, theme.accent)
                    cornerRadius = 12f
                }
            }

            setOnClickListener { onClick(theme) }
        }

        // Color preview circles
        val colorPreview = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        listOf(theme.primary, theme.secondary, theme.accent).forEach { color ->
            colorPreview.addView(View(ctx).apply {
                setBackgroundColor(color)
                layoutParams = LinearLayout.LayoutParams(32, 32).apply { marginEnd = 4 }
            })
        }

        card.addView(colorPreview)

        val nameText = TextView(ctx).apply {
            text = "${theme.emoji} ${theme.name}"
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            setTextColor(theme.textPrimary)
            setPadding(16, 0, 0, 0)
        }
        card.addView(nameText)

        if (isSelected) {
            card.addView(TextView(ctx).apply {
                text = "✓ Active"
                textSize = 12f
                setTextColor(theme.accent)
                setTypeface(null, Typeface.BOLD)
            })
        }

        return card
    }

    // Animation effects
    private fun animateSpring(view: View) {
        val scaleX = ObjectAnimator.ofFloat(view, "scaleX", 0.95f, 1f)
        val scaleY = ObjectAnimator.ofFloat(view, "scaleY", 0.95f, 1f)
        val anim = AnimatorSet().apply {
            playTogether(scaleX, scaleY)
            duration = 400
            interpolator = OvershootInterpolator(2f)
        }
        anim.start()
    }

    private fun animateSmooth(view: View) {
        val fade = ObjectAnimator.ofFloat(view, "alpha", 0.7f, 1f)
        fade.duration = 300
        fade.interpolator = DecelerateInterpolator()
        fade.start()
    }

    private fun animateQuick(view: View) {
        val anim = ObjectAnimator.ofFloat(view, "alpha", 0.5f, 1f)
        anim.duration = 150
        anim.start()
    }

    private fun animateDramatic(view: View) {
        val scale = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(view, "scaleX", 0.8f, 1.05f, 1f),
                ObjectAnimator.ofFloat(view, "scaleY", 0.8f, 1.05f, 1f),
                ObjectAnimator.ofFloat(view, "alpha", 0f, 1f)
            )
            duration = 600
        }
        scale.start()
    }

    private fun animateBouncy(view: View) {
        val anim = ObjectAnimator.ofFloat(view, "translationY", 0f, -20f, 0f)
        anim.duration = 500
        anim.interpolator = BounceInterpolator()
        anim.start()
    }

    private fun animateFloat(view: View) {
        val anim = ObjectAnimator.ofFloat(view, "translationY", 0f, -8f, 0f)
        anim.duration = 2000
        anim.repeatCount = ValueAnimator.INFINITE
        anim.repeatMode = ValueAnimator.REVERSE
        anim.interpolator = AccelerateDecelerateInterpolator()
        anim.start()
    }

    private fun addGlowEffect(view: View, theme: Theme) {
        view.elevation = theme.cardElevation + 4
        // In production, would add shadow with theme.accent
    }

    private fun addBounceEffect(view: View) {
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
}