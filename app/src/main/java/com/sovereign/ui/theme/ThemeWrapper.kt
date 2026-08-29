package com.sovereign.ui.theme

import android.content.Context
import android.util.AttributeSet
import android.view.View
import com.sovereign.app.R

class ThemeWrapper(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    init {
        val config = ThemeManager.rememberThemeManager()
        val themeConfig = ThemeManager.getThemeConfig(config)
        if (themeConfig != null) {
            setBackgroundColor(themeConfig.background)
        }
    }
}