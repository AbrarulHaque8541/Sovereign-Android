package com.sovereign.ui.theme

import android.content.Context
import android.graphics.Color
import android.view.View
import android.content.SharedPreferences
import com.sovereign.app.R

enum class ThemeType(val id: String, val display: String) {
    AURORA("aurora", "Aurora"),
    CYBER("cyber", "Cyber"),
    SUNSET("sunset", "Sunset"),
    OCEAN("ocean", "Ocean"),
    VELVET("velvet", "Velvet"),
    FOREST("forest", "Forest"),
    CANDY("candy", "Candy"),
    MONO("mono", "Mono")
}

object ThemeColors {
    val AURORA = ThemeConfig(
        background = Color.rgb(15, 12, 41),
        surface = Color.rgb(26, 26, 46),
        primary = Color.rgb(238, 54, 255),
        onPrimary = Color.WHITE,
        onBackground = Color.WHITE
    )
    val CYBER = ThemeConfig(
        background = Color.rgb(0, 0, 0),
        surface = Color.rgb(10, 10, 10),
        primary = Color.rgb(0, 255, 136),
        onPrimary = Color.BLACK,
        onBackground = Color.rgb(0, 255, 136)
    )
    val SUNSET = ThemeConfig(
        background = Color.rgb(20, 39, 30),
        surface = Color.rgb(31, 58, 58),
        primary = Color.rgb(255, 107, 107),
        onPrimary = Color.WHITE,
        onBackground = Color.WHITE
    )
    val OCEAN = ThemeConfig(
        background = Color.rgb(13, 27, 42),
        surface = Color.rgb(26, 35, 50),
        primary = Color.rgb(0, 180, 216),
        onPrimary = Color.WHITE,
        onBackground = Color.WHITE
    )
    val VELVET = ThemeConfig(
        background = Color.rgb(26, 15, 47),
        surface = Color.rgb(42, 26, 74),
        primary = Color.rgb(155, 89, 182),
        onPrimary = Color.WHITE,
        onBackground = Color.WHITE
    )
    val FOREST = ThemeConfig(
        background = Color.rgb(13, 27, 42),
        surface = Color.rgb(26, 42, 42),
        primary = Color.rgb(46, 204, 113),
        onPrimary = Color.WHITE,
        onBackground = Color.WHITE
    )
    val CANDY = ThemeConfig(
        background = Color.rgb(245, 245, 245),
        surface = Color.rgb(250, 250, 250),
        primary = Color.rgb(236, 64, 122),
        onPrimary = Color.WHITE,
        onBackground = Color.BLACK
    )
    val MONA = ThemeConfig(
        background = Color.rgb(250, 250, 250),
        surface = Color.rgb(255, 255, 255),
        primary = Color.rgb(33, 33, 33),
        onPrimary = Color.WHITE,
        onBackground = Color.BLACK
    )
}

data class ThemeConfig(
    val background: Int,
    val surface: Int,
    val primary: Int,
    val onPrimary: Int,
    val onBackground: Int
)

object ThemeManager {
    @Volatile
    private var currentTheme = ThemeType.AURORA

    fun rememberThemeManager(): ThemeType {
        return currentTheme
    }

    fun getThemeConfig(type: ThemeType): ThemeConfig = when (type) {
        ThemeType.AURORA -> ThemeColors.AURORA
        ThemeType.CYBER -> ThemeColors.CYBER
        ThemeType.SUNSET -> ThemeColors.SUNSET
        ThemeType.OCEAN -> ThemeColors.OCEAN
        ThemeType.VELVET -> ThemeColors.VELVET
        ThemeType.FOREST -> ThemeColors.FOREST
        ThemeType.CANDY -> ThemeColors.CANDY
        ThemeType.MONO -> ThemeColors.MONA
    }

    fun setTheme(context: Context, themeType: ThemeType) {
        currentTheme = themeType
        val sharedPrefs = context.getSharedPreferences("sovereign_prefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().putString("saved_theme", themeType.id).apply()
    }

    fun getSavedTheme(context: Context): ThemeType {
        val sharedPrefs = context.getSharedPreferences("sovereign_prefs", Context.MODE_PRIVATE)
        val savedId = sharedPrefs.getString("saved_theme", "aurora")
        return ThemeType.values().find { it.id == savedId } ?: ThemeType.AURORA
    }

    fun requestThemeChange(newTheme: ThemeType, context: Context) {
        setTheme(context, newTheme)
    }

    fun applyThemeToView(view: View, context: Context) {
        val config = getThemeConfig(rememberThemeManager())
        view.setBackgroundColor(config.background)
    }
}