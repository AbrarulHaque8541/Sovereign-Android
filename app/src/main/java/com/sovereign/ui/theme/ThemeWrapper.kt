package com.sovereign.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sovereign.ui.theme.ThemeManager.rememberThemeManager
import com.sovereign.ui.theme.ThemeManager.getThemeConfig

@Composable
fun ThemeWrapper(content: @Composable () -> Unit) {
    val themeState = rememberThemeManager()
    val config = getThemeConfig(themeState.currentTheme)
    val colors = config.colors
    
    val colorScheme = darkColorScheme(
        primary = colors.primary,
        primaryContainer = colors.primaryVariant,
        secondary = colors.secondary,
        secondaryContainer = colors.secondaryVariant,
        tertiary = colors.accentGlow,
        background = colors.background,
        surface = colors.surface,
        surfaceVariant = colors.surfaceVariant,
        error = colors.error,
        onPrimary = colors.onPrimary,
        onSecondary = colors.onSecondary,
        onBackground = colors.onBackground,
        onSurface = colors.onSurface,
        onSurfaceVariant = colors.onSurfaceVariant,
        onError = colors.onError,
        outline = colors.outline,
        outlineVariant = colors.outlineVariant,
        inverseSurface = colors.onBackground,
        inverseOnSurface = colors.onSurface,
        inversePrimary = colors.primaryVariant
    )
    
    androidx.compose.material3.MaterialTheme(
        colorScheme = colorScheme,
        typography = androidx.compose.material3.Typography(),
        shapes = androidx.compose.material3.Shapes(
            small = androidx.compose.foundation.shape.RoundedCornerShape(config.shapes.chipRadius.dp),
            medium = androidx.compose.foundation.shape.RoundedCornerShape(config.shapes.cardRadius.dp),
            large = androidx.compose.foundation.shape.RoundedCornerShape(config.shapes.modalRadius.dp)
        ),
        content = content
    )
}