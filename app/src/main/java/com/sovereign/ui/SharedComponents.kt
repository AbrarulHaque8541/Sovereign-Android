package com.sovereign.ui

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.vector.ImageVector
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

class LanguageManager(private val context: Context) {
    private val _uiLanguage = mutableStateOf("en")
    val uiLanguage: androidx.compose.runtime.State<String> = _uiLanguage

    data class LanguageInfo(
        val code: String,
        val englishName: String,
        val nativeName: String,
        val flag: String,
        val region: String
    )

    private val allLanguages = listOf(
        LanguageInfo("en", "English", "English", "\uD83C\uDDFA\uD83C\uDDF8", "US"),
        LanguageInfo("es", "Spanish", "Espa\u00f1ol", "\uD83C\uDDEA\uD83C\uDDF8", "ES"),
        LanguageInfo("fr", "French", "Fran\u00e7ais", "\uD83C\uDDEB\uD83C\uDDF7", "FR"),
        LanguageInfo("de", "German", "Deutsch", "\uD83C\uDDE9\uD83C\uDDEA", "DE"),
        LanguageInfo("zh", "Chinese (Simplified)", "\u4E2D\u6587", "\uD83C\uDDE8\uD83C\uDDF3", "CN"),
        LanguageInfo("ja", "Japanese", "\u65E5\u672C\u8A9E", "\uD83C\uDDEF\uD83C\uDDF5", "JP"),
        LanguageInfo("ko", "Korean", "\uD55C\uAD6D\uC5B4", "\uD83C\uDDF0\uD83C\uDDF7", "KR"),
        LanguageInfo("pt", "Portuguese", "Portugu\u00eas", "\uD83C\uDDF5\uD83C\uDDF9", "PT"),
        LanguageInfo("ru", "Russian", "\u0420\u0443\u0441\u0441\u043A\u0438\u0439", "\uD83C\uDDF7\uD83C\uDDFA", "RU"),
        LanguageInfo("ar", "Arabic", "\u0627\u0644\u0639\u0631\u0628\u064A\u0629", "\uD83C\uDDF8\uD83C\uDDE6", "SA"),
        LanguageInfo("hi", "Hindi", "\u0939\u093F\u0928\u094D\u0926\u0940", "\uD83C\uDDEE\uD83C\uDDF3", "IN"),
        LanguageInfo("it", "Italian", "Italiano", "\uD83C\uDDEE\uD83C\uDDF9", "IT"),
        LanguageInfo("tr", "Turkish", "T\u00fcrk\u00e7e", "\uD83C\uDDF9\uD83C\uDDF7", "TR"),
        LanguageInfo("pl", "Polish", "Polski", "\uD83C\uDDF5\uD83C\uDDF1", "PL"),
        LanguageInfo("nl", "Dutch", "Nederlands", "\uD83C\uDDF3\uD83C\uDDF1", "NL")
    )

    private val localePriorityMap = ConcurrentHashMap<String, Int>()

    init {
        calculateLocalePriority()
    }

    private fun calculateLocalePriority() {
        val systemLocale = Locale.getDefault()
        val systemLanguage = systemLocale.language
        val systemCountry = systemLocale.country

        allLanguages.forEachIndexed { index, lang ->
            var priority = index + 100
            if (lang.code == systemLanguage) priority = 0
            else if (lang.region == systemCountry) priority = 10
            else if (systemLanguage.startsWith(lang.code) || lang.code.startsWith(systemLanguage)) priority = 20
            localePriorityMap[lang.code] = priority
        }
    }

    fun getPrioritizedLanguages(): List<LanguageInfo> =
        allLanguages.sortedBy { localePriorityMap[it.code] ?: 999 }

    fun setLanguage(code: String) {
        _uiLanguage.value = code
    }

    fun downloadLanguagePack(code: String) {
        Thread.sleep(500)
    }
}

data class ChipData(val id: String, val label: String, val icon: ImageVector)
