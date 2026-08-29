package com.sovereign.ui.theme

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.sovereign.app.SovereignApplication.Companion.dataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

enum class ThemeType(val id: String, val displayName: String, val description: String) {
    AURORA("aurora", "Aurora Glass", "Futuristic glassmorphism with mesh gradients and neon glows"),
    CYBER("cyber", "Cyber Neon", "High-contrast cyberpunk with neon accents"),
    SUNSET("sunset", "Sunset Haze", "Warm gradient with coral and gold tones"),
    OCEAN("ocean", "Ocean Deep", "Deep blue oceanic theme with cyan accents"),
    VELVET("velvet", "Midnight Velvet", "Rich purple and magenta dark theme"),
    FOREST("forest", "Forest Mist", "Natural green forest theme"),
    CANDY("candy", "Candy Pop", "Bright colorful pastel theme"),
    MONO("mono", "Mono Editorial", "Clean black & white editorial theme")
}

data class ThemeConfig(
    val type: ThemeType,
    val colors: ThemeColors,
    val elevations: ThemeElevations,
    val shapes: ThemeShapes,
    val animations: ThemeAnimations,
    val typography: ThemeTypography
)

data class ThemeColors(
    val background: Color,
    val backgroundSecondary: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val primary: Color,
    val primaryVariant: Color,
    val secondary: Color,
    val secondaryVariant: Color,
    val onPrimary: Color,
    val onSecondary: Color,
    val onBackground: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val outline: Color,
    val outlineVariant: Color,
    val error: Color,
    val onError: Color,
    val glowPrimary: Color,
    val glowSecondary: Color,
    val accentGlow: Color,
    val activeTab: Color,
    val inactiveTab: Color,
    val notificationBadge: Color
)

data class ThemeElevations(
    val cardElevation: Float,
    val modalElevation: Float,
    val fabElevation: Float,
    val dockElevation: Float,
    val glowBlur: Float,
    val innerShadowBlur: Float
)

data class ThemeShapes(
    val cardRadius: Float,
    val modalRadius: Float,
    val buttonRadius: Float,
    val chipRadius: Float,
    val dockRadius: Float,
    val fabRadius: Float
)

data class ThemeAnimations(
    val cardHoverScale: Float,
    val cardHoverBlur: Float,
    val buttonPressScale: Float,
    val switchDuration: Int,
    val transitionDuration: Int,
    val meshGradientDuration: Int
)

data class ThemeTypography(
    val headerFontSize: Float,
    val titleFontSize: Float,
    val bodyFontSize: Float,
    val captionFontSize: Float,
    val fontFamily: String
)

object ThemeManager {
    private const val STORAGE_KEY = "selected_theme"
    val DEFAULT_THEME_TYPE = ThemeType.AURORA
    
    @Composable
    fun rememberThemeManager(): ThemeManagerState {
        return remember { ThemeManagerState() }
    }
    
    fun getThemeConfig(type: ThemeType): ThemeConfig = when (type) {
        ThemeType.AURORA -> AuroraTheme.config
        ThemeType.CYBER -> CyberTheme.config
        ThemeType.SUNSET -> SunsetTheme.config
        ThemeType.OCEAN -> OceanTheme.config
        ThemeType.VELVET -> VelvetTheme.config
        ThemeType.FOREST -> ForestTheme.config
        ThemeType.CANDY -> CandyTheme.config
        ThemeType.MONO -> MonoTheme.config
    }
    
    suspend fun setTheme(context: Context, themeType: ThemeType) {
        dataStore.edit { prefs: androidx.datastore.preferences.core.MutablePreferences ->
            prefs[stringPreferencesKey(STORAGE_KEY)] = themeType.id
        }
    }
    
    suspend fun getSavedTheme(): ThemeType {
        val prefs = dataStore.data.first()
        val saved = prefs[stringPreferencesKey(STORAGE_KEY)] ?: ThemeType.AURORA.id
        return ThemeType.values().find { it.id == saved } ?: ThemeType.AURORA
    }
}

class ThemeManagerState {
    var currentTheme by mutableStateOf(ThemeType.AURORA)
    var showRestartDialog by mutableStateOf(false)
    var pendingTheme by mutableStateOf<ThemeType?>(null)
    
    fun requestThemeChange(newTheme: ThemeType) {
        if (newTheme != currentTheme) {
            pendingTheme = newTheme
            showRestartDialog = true
        }
    }
    
    fun confirmRestart() {
        showRestartDialog = false
        pendingTheme?.let { newTheme ->
            currentTheme = newTheme
        }
        pendingTheme = null
    }
    
    fun cancelRestart() {
        showRestartDialog = false
        pendingTheme = null
    }
}

object AuroraTheme {
    val config = ThemeConfig(
        type = ThemeType.AURORA,
        colors = ThemeColors(
            background = Color(0xFF0F0C29.toLong()),
            backgroundSecondary = Color(0xFF302B63.toLong()),
            surface = Color(0x801A1A2E.toLong()),
            surfaceVariant = Color(0x802A2A4E.toLong()),
            primary = Color(0xFFEE36FF.toLong()),
            primaryVariant = Color(0xFF00CBFF.toLong()),
            secondary = Color(0xFF29AFCD.toLong()),
            secondaryVariant = Color(0xFF00CBFF.toLong()),
            onPrimary = Color.White,
            onSecondary = Color.White,
            onBackground = Color.White,
            onSurface = Color.White,
            onSurfaceVariant = Color(0xB3BAABFF.toLong()),
            outline = Color(0x33FFFFFF.toLong()),
            outlineVariant = Color(0x05FFFFFF.toLong()),
            error = Color(0xFFCF6679.toLong()),
            onError = Color.White,
            glowPrimary = Color(0xFFEE36FF.toLong()),
            glowSecondary = Color(0xFF00CBFF.toLong()),
            accentGlow = Color(0xFF29AFCD.toLong()),
            activeTab = Color(0xFF00CBFF.toLong()),
            inactiveTab = Color(0x80FFFFFF.toLong()),
            notificationBadge = Color(0xFFFF3B30.toLong())
        ),
        elevations = ThemeElevations(
            cardElevation = 8f,
            modalElevation = 12f,
            fabElevation = 12f,
            dockElevation = 8f,
            glowBlur = 110f,
            innerShadowBlur = 8f
        ),
        shapes = ThemeShapes(
            cardRadius = 24f,
            modalRadius = 32f,
            buttonRadius = 999f,
            chipRadius = 12f,
            dockRadius = 24f,
            fabRadius = 999f
        ),
        animations = ThemeAnimations(
            cardHoverScale = 1.02f,
            cardHoverBlur = 140f,
            buttonPressScale = 1.15f,
            switchDuration = 250,
            transitionDuration = 300,
            meshGradientDuration = 8000
        ),
        typography = ThemeTypography(
            headerFontSize = 28f,
            titleFontSize = 20f,
            bodyFontSize = 16f,
            captionFontSize = 12f,
            fontFamily = "Inter"
        )
    )
}

object CyberTheme {
    val config = ThemeConfig(
        type = ThemeType.CYBER,
        colors = ThemeColors(
            background = Color(0xFF000000.toLong()),
            backgroundSecondary = Color(0xFF0A0A0A.toLong()),
            surface = Color(0xFF0A0A0A.toLong()),
            surfaceVariant = Color(0xFF1A0033.toLong()),
            primary = Color(0xFF00FF88.toLong()),
            primaryVariant = Color(0xFF00CC6A.toLong()),
            secondary = Color(0xFFFF00FF.toLong()),
            secondaryVariant = Color(0xFFCC00CC.toLong()),
            onPrimary = Color.Black,
            onSecondary = Color.Black,
            onBackground = Color(0xFF00FF88.toLong()),
            onSurface = Color(0xFF00FF88.toLong()),
            onSurfaceVariant = Color(0xFF666666.toLong()),
            outline = Color(0xFF00FF88.toLong()),
            outlineVariant = Color(0xFF00FF88.toLong()),
            error = Color(0xFFFF0000.toLong()),
            onError = Color.White,
            glowPrimary = Color(0xFF00FF88.toLong()),
            glowSecondary = Color(0xFFFF00FF.toLong()),
            accentGlow = Color(0xFF00FF88.toLong()),
            activeTab = Color(0xFF00FF88.toLong()),
            inactiveTab = Color(0xFF666666.toLong()),
            notificationBadge = Color(0xFFFF0000.toLong())
        ),
        elevations = ThemeElevations(
            cardElevation = 8f,
            modalElevation = 12f,
            fabElevation = 12f,
            dockElevation = 8f,
            glowBlur = 100f,
            innerShadowBlur = 8f
        ),
        shapes = ThemeShapes(
            cardRadius = 16f,
            modalRadius = 20f,
            buttonRadius = 12f,
            chipRadius = 8f,
            dockRadius = 0f,
            fabRadius = 16f
        ),
        animations = ThemeAnimations(
            cardHoverScale = 1.0f,
            cardHoverBlur = 0f,
            buttonPressScale = 0.98f,
            switchDuration = 250,
            transitionDuration = 200,
            meshGradientDuration = 0
        ),
        typography = ThemeTypography(
            headerFontSize = 28f,
            titleFontSize = 20f,
            bodyFontSize = 16f,
            captionFontSize = 12f,
            fontFamily = "Inter"
        )
    )
}

object SunsetTheme {
    val config = ThemeConfig(
        type = ThemeType.SUNSET,
        colors = ThemeColors(
            background = Color(0xFFFF9A8B.toLong()),
            backgroundSecondary = Color(0xFFFF6A88.toLong()),
            surface = Color(0xE6FFFFFF.toLong()),
            surfaceVariant = Color(0xCCFFFFFF.toLong()),
            primary = Color(0xFFFF6A88.toLong()),
            primaryVariant = Color(0xFFFFD89B.toLong()),
            secondary = Color(0xFFFFD89B.toLong()),
            secondaryVariant = Color(0xFFFF9A8B.toLong()),
            onPrimary = Color.White,
            onSecondary = Color.White,
            onBackground = Color(0xFF333333.toLong()),
            onSurface = Color(0xFF333333.toLong()),
            onSurfaceVariant = Color(0xFF888888.toLong()),
            outline = Color(0xFFFFFFFF.toLong()),
            outlineVariant = Color(0x80FFFFFF.toLong()),
            error = Color(0xFFFF3B30.toLong()),
            onError = Color.White,
            glowPrimary = Color(0xFFFF6A88.toLong()),
            glowSecondary = Color(0xFFFFD89B.toLong()),
            accentGlow = Color(0xFFFF9A8B.toLong()),
            activeTab = Color(0xFFFF6A88.toLong()),
            inactiveTab = Color(0x80FFFFFF.toLong()),
            notificationBadge = Color(0xFFFF3B30.toLong())
        ),
        elevations = ThemeElevations(
            cardElevation = 8f,
            modalElevation = 12f,
            fabElevation = 12f,
            dockElevation = 8f,
            glowBlur = 80f,
            innerShadowBlur = 8f
        ),
        shapes = ThemeShapes(
            cardRadius = 20f,
            modalRadius = 28f,
            buttonRadius = 16f,
            chipRadius = 10f,
            dockRadius = 20f,
            fabRadius = 999f
        ),
        animations = ThemeAnimations(
            cardHoverScale = 1.02f,
            cardHoverBlur = 80f,
            buttonPressScale = 1.10f,
            switchDuration = 250,
            transitionDuration = 300,
            meshGradientDuration = 6000
        ),
        typography = ThemeTypography(
            headerFontSize = 28f,
            titleFontSize = 20f,
            bodyFontSize = 16f,
            captionFontSize = 12f,
            fontFamily = "Inter"
        )
    )
}

object OceanTheme {
    val config = ThemeConfig(
        type = ThemeType.OCEAN,
        colors = ThemeColors(
            background = Color(0xFF001F3F.toLong()),
            backgroundSecondary = Color(0xFF003366.toLong()),
            surface = Color(0x80003366.toLong()),
            surfaceVariant = Color(0x80004080.toLong()),
            primary = Color(0xFF00CCFF.toLong()),
            primaryVariant = Color(0xFF0099CC.toLong()),
            secondary = Color(0xFF00FF88.toLong()),
            secondaryVariant = Color(0xFF00FFCC.toLong()),
            onPrimary = Color.White,
            onSecondary = Color.White,
            onBackground = Color.White,
            onSurface = Color.White,
            onSurfaceVariant = Color(0xFF00CCFF.toLong()),
            outline = Color(0x1AFFFFFF.toLong()),
            outlineVariant = Color(0x0DFFFFFF.toLong()),
            error = Color(0xFFFF3B30.toLong()),
            onError = Color.White,
            glowPrimary = Color(0xFF00CCFF.toLong()),
            glowSecondary = Color(0xFF00FF88.toLong()),
            accentGlow = Color(0xFF00CCFF.toLong()),
            activeTab = Color(0xFF00CCFF.toLong()),
            inactiveTab = Color(0x80FFFFFF.toLong()),
            notificationBadge = Color(0xFFFF3B30.toLong())
        ),
        elevations = ThemeElevations(
            cardElevation = 12f,
            modalElevation = 16f,
            fabElevation = 16f,
            dockElevation = 12f,
            glowBlur = 120f,
            innerShadowBlur = 10f
        ),
        shapes = ThemeShapes(
            cardRadius = 24f,
            modalRadius = 32f,
            buttonRadius = 16f,
            chipRadius = 12f,
            dockRadius = 24f,
            fabRadius = 999f
        ),
        animations = ThemeAnimations(
            cardHoverScale = 1.02f,
            cardHoverBlur = 120f,
            buttonPressScale = 1.10f,
            switchDuration = 300,
            transitionDuration = 400,
            meshGradientDuration = 6000
        ),
        typography = ThemeTypography(
            headerFontSize = 28f,
            titleFontSize = 20f,
            bodyFontSize = 16f,
            captionFontSize = 12f,
            fontFamily = "Inter"
        )
    )
}

object VelvetTheme {
    val config = ThemeConfig(
        type = ThemeType.VELVET,
        colors = ThemeColors(
            background = Color(0xFF1A0033.toLong()),
            backgroundSecondary = Color(0xFF330066.toLong()),
            surface = Color(0x80330066.toLong()),
            surfaceVariant = Color(0xCC4D0099.toLong()),
            primary = Color(0xFFCC66FF.toLong()),
            primaryVariant = Color(0xFF9933FF.toLong()),
            secondary = Color(0xFFFF3399.toLong()),
            secondaryVariant = Color(0xFFFF66CC.toLong()),
            onPrimary = Color.White,
            onSecondary = Color.White,
            onBackground = Color.White,
            onSurface = Color.White,
            onSurfaceVariant = Color(0xFFCC66FF.toLong()),
            outline = Color(0x33CC66FF.toLong()),
            outlineVariant = Color(0x1AFFFFFF.toLong()),
            error = Color(0xFFFF3B30.toLong()),
            onError = Color.White,
            glowPrimary = Color(0xFFCC66FF.toLong()),
            glowSecondary = Color(0xFFFF3399.toLong()),
            accentGlow = Color(0xFFCC66FF.toLong()),
            activeTab = Color(0xFFCC66FF.toLong()),
            inactiveTab = Color(0x80FFFFFF.toLong()),
            notificationBadge = Color(0xFFFF3B30.toLong())
        ),
        elevations = ThemeElevations(
            cardElevation = 10f,
            modalElevation = 14f,
            fabElevation = 14f,
            dockElevation = 10f,
            glowBlur = 100f,
            innerShadowBlur = 8f
        ),
        shapes = ThemeShapes(
            cardRadius = 22f,
            modalRadius = 28f,
            buttonRadius = 16f,
            chipRadius = 10f,
            dockRadius = 22f,
            fabRadius = 999f
        ),
        animations = ThemeAnimations(
            cardHoverScale = 1.02f,
            cardHoverBlur = 100f,
            buttonPressScale = 1.10f,
            switchDuration = 250,
            transitionDuration = 300,
            meshGradientDuration = 6000
        ),
        typography = ThemeTypography(
            headerFontSize = 28f,
            titleFontSize = 20f,
            bodyFontSize = 16f,
            captionFontSize = 12f,
            fontFamily = "Inter"
        )
    )
}

object ForestTheme {
    val config = ThemeConfig(
        type = ThemeType.FOREST,
        colors = ThemeColors(
            background = Color(0xFF0D2818.toLong()),
            backgroundSecondary = Color(0xFF1A4D2E.toLong()),
            surface = Color(0x801A4D2E.toLong()),
            surfaceVariant = Color(0x802D6A4F.toLong()),
            primary = Color(0xFF90BE6D.toLong()),
            primaryVariant = Color(0xFF2D6A4F.toLong()),
            secondary = Color(0xFF9BE89E.toLong()),
            secondaryVariant = Color(0xFF40916C.toLong()),
            onPrimary = Color.White,
            onSecondary = Color.White,
            onBackground = Color.White,
            onSurface = Color.White,
            onSurfaceVariant = Color(0xFF90BE6D.toLong()),
            outline = Color(0x3390BE6D.toLong()),
            outlineVariant = Color(0x1AFFFFFF.toLong()),
            error = Color(0xFFFF3B30.toLong()),
            onError = Color.White,
            glowPrimary = Color(0xFF90BE6D.toLong()),
            glowSecondary = Color(0xFF2D6A4F.toLong()),
            accentGlow = Color(0xFF90BE6D.toLong()),
            activeTab = Color(0xFF90BE6D.toLong()),
            inactiveTab = Color(0x80FFFFFF.toLong()),
            notificationBadge = Color(0xFFFF3B30.toLong())
        ),
        elevations = ThemeElevations(
            cardElevation = 8f,
            modalElevation = 12f,
            fabElevation = 12f,
            dockElevation = 8f,
            glowBlur = 90f,
            innerShadowBlur = 8f
        ),
        shapes = ThemeShapes(
            cardRadius = 20f,
            modalRadius = 28f,
            buttonRadius = 16f,
            chipRadius = 10f,
            dockRadius = 20f,
            fabRadius = 999f
        ),
        animations = ThemeAnimations(
            cardHoverScale = 1.02f,
            cardHoverBlur = 90f,
            buttonPressScale = 1.10f,
            switchDuration = 250,
            transitionDuration = 300,
            meshGradientDuration = 6000
        ),
        typography = ThemeTypography(
            headerFontSize = 28f,
            titleFontSize = 20f,
            bodyFontSize = 16f,
            captionFontSize = 12f,
            fontFamily = "Inter"
        )
    )
}

object CandyTheme {
    val config = ThemeConfig(
        type = ThemeType.CANDY,
        colors = ThemeColors(
            background = Color(0xFFFF6B9D.toLong()),
            backgroundSecondary = Color(0xFFC06CFF.toLong()),
            surface = Color.White,
            surfaceVariant = Color(0xE6FFFFFF.toLong()),
            primary = Color(0xFFC06CFF.toLong()),
            primaryVariant = Color(0xFF6C9CFF.toLong()),
            secondary = Color(0xFF6C9CFF.toLong()),
            secondaryVariant = Color(0xFFFF6B9D.toLong()),
            onPrimary = Color.White,
            onSecondary = Color.White,
            onBackground = Color(0xFF333333.toLong()),
            onSurface = Color(0xFF333333.toLong()),
            onSurfaceVariant = Color(0xFF888888.toLong()),
            outline = Color(0xFF666666.toLong()),
            outlineVariant = Color(0xFFCCCCCC.toLong()),
            error = Color(0xFFFF3B30.toLong()),
            onError = Color.White,
            glowPrimary = Color(0xFFC06CFF.toLong()),
            glowSecondary = Color(0xFF6C9CFF.toLong()),
            accentGlow = Color(0xFFFF6B9D.toLong()),
            activeTab = Color(0xFFC06CFF.toLong()),
            inactiveTab = Color(0xFF666666.toLong()),
            notificationBadge = Color(0xFFFF3B30.toLong())
        ),
        elevations = ThemeElevations(
            cardElevation = 12f,
            modalElevation = 16f,
            fabElevation = 16f,
            dockElevation = 12f,
            glowBlur = 60f,
            innerShadowBlur = 8f
        ),
        shapes = ThemeShapes(
            cardRadius = 24f,
            modalRadius = 32f,
            buttonRadius = 16f,
            chipRadius = 12f,
            dockRadius = 24f,
            fabRadius = 999f
        ),
        animations = ThemeAnimations(
            cardHoverScale = 1.02f,
            cardHoverBlur = 60f,
            buttonPressScale = 1.10f,
            switchDuration = 250,
            transitionDuration = 300,
            meshGradientDuration = 4000
        ),
        typography = ThemeTypography(
            headerFontSize = 28f,
            titleFontSize = 20f,
            bodyFontSize = 16f,
            captionFontSize = 12f,
            fontFamily = "Inter"
        )
    )
}

object MonoTheme {
    val config = ThemeConfig(
        type = ThemeType.MONO,
        colors = ThemeColors(
            background = Color(0xFFFAFAFA.toLong()),
            backgroundSecondary = Color(0xFFF5F5F5.toLong()),
            surface = Color.White,
            surfaceVariant = Color(0xFFE6E6E6.toLong()),
            primary = Color.Black,
            primaryVariant = Color(0xFF333333.toLong()),
            secondary = Color(0xFF333333.toLong()),
            secondaryVariant = Color(0xFF666666.toLong()),
            onPrimary = Color.White,
            onSecondary = Color.White,
            onBackground = Color.Black,
            onSurface = Color.Black,
            onSurfaceVariant = Color(0xFF666666.toLong()),
            outline = Color.Black,
            outlineVariant = Color(0xFF666666.toLong()),
            error = Color(0xFFFF3B30.toLong()),
            onError = Color.White,
            glowPrimary = Color.Black,
            glowSecondary = Color(0xFF333333.toLong()),
            accentGlow = Color.Black,
            activeTab = Color.Black,
            inactiveTab = Color(0xFF666666.toLong()),
            notificationBadge = Color(0xFFFF3B30.toLong())
        ),
        elevations = ThemeElevations(
            cardElevation = 0f,
            modalElevation = 0f,
            fabElevation = 0f,
            dockElevation = 0f,
            glowBlur = 0f,
            innerShadowBlur = 0f
        ),
        shapes = ThemeShapes(
            cardRadius = 8f,
            modalRadius = 12f,
            buttonRadius = 8f,
            chipRadius = 4f,
            dockRadius = 0f,
            fabRadius = 24f
        ),
        animations = ThemeAnimations(
            cardHoverScale = 1.0f,
            cardHoverBlur = 0f,
            buttonPressScale = 0.98f,
            switchDuration = 150,
            transitionDuration = 150,
            meshGradientDuration = 0
        ),
        typography = ThemeTypography(
            headerFontSize = 28f,
            titleFontSize = 20f,
            bodyFontSize = 16f,
            captionFontSize = 12f,
            fontFamily = "Inter"
        )
    )
}