package com.sovereign.ui

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.PopupProperties
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.tap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.awaitPointerEventScope
import androidx.compose.ui.input.pointer.awaitFirstDown
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UniversalCaptureScreen(
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val languageManager = remember { LanguageManager(context) }
    val uiLanguage by languageManager.uiLanguage
    val showLanguagePicker by remember { mutableStateOf(false) }
    val showDropdownMenu by remember { mutableStateOf(false) }
    val captureText by remember { mutableStateOf("") }
    val isProcessing by remember { mutableStateOf(false) }
    val downloadedLanguages by remember { mutableStateOf<Set<String>>(setOf("en")) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopHeaderBar(
            uiLanguage = uiLanguage,
            onLanguageClick = { showLanguagePicker = true },
            onSearchClick = onSearchClick,
            onSettingsClick = { showDropdownMenu = true },
            onNavigateBack = onNavigateBack
        )

        UniversalCaptureBar(
            captureText = captureText,
            onTextChange = { captureText = it },
            onMicClick = { /* Handle voice input */ },
            onAttachmentClick = { /* Handle file attachment */ },
            onProcessClick = { isProcessing = true },
            isProcessing = isProcessing,
            uiLanguage = uiLanguage
        )

        ContextualQuickChips(
            uiLanguage = uiLanguage,
            onChipClick = { action ->
                when (action) {
                    "format_json" -> { /* Format JSON */ }
                    "redact_pii" -> { /* Redact PII */ }
                    "extract_ocr" -> { /* Extract OCR */ }
                    "encrypt_note" -> { /* Encrypt Note */ }
                    "extract_links" -> { /* Extract Links */ }
                }
            }
        )

        LanguagePickerOverlay(
            visible = showLanguagePicker,
            onDismiss = { showLanguagePicker = false },
            languageManager = languageManager,
            downloadedLanguages = downloadedLanguages,
            onDownloadLanguage = { code ->
                CoroutineScope(Dispatchers.IO).launch {
                    languageManager.downloadLanguagePack(code)
                    downloadedLanguages.plusAssign(code)
                }
            }
        )

        DropdownMenu(
            expanded = showDropdownMenu,
            onDismissRequest = { showDropdownMenu = false },
            properties = PopupProperties(focusable = true)
        ) {
            DropdownMenuItem(text = { Text(stringResource(R.string.settings)) }) { showDropdownMenu = false }
            DropdownMenuItem(text = { Text(stringResource(R.string.privacy_policy)) }) { showDropdownMenu = false }
            DropdownMenuItem(text = { Text(stringResource(R.string.about)) }) { showDropdownMenu = false }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopHeaderBar(
    uiLanguage: String,
    onLanguageClick: () -> Unit,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onNavigateBack: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.Filled.Menu,
                    contentDescription = "Navigation drawer",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = "Sovereign",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(onClick = onLanguageClick) {
                Icon(
                    imageVector = Icons.Filled.Language,
                    contentDescription = "Language selector",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            IconButton(onClick = onSearchClick) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UniversalCaptureBar(
    captureText: String,
    onTextChange: (String) -> Unit,
    onMicClick: () -> Unit,
    onAttachmentClick: () -> Unit,
    onProcessClick: () -> Unit,
    isProcessing: Boolean,
    uiLanguage: String
) {
    val resources = LocalContext.current.resources
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1E1E),
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = captureText,
                    onValueChange = onTextChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 12.dp),
                    placeholder = { Text("Type, Paste, or Drop files...") },
                    singleLine = false,
                    minLines = 3,
                    maxLines = 5,
                    colors = TextFieldDefaults.textFieldColors(
                        containerColor = Color(0xFF121212),
                        focusedContainerColor = Color(0xFF1E1E1E),
                        textColor = MaterialTheme.colorScheme.onSurface,
                        placeholderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        cursorColor = Color(0xFFBB86FC)
                    )
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onMicClick, enabled = !isProcessing) {
                    Icon(
                        imageVector = Icons.Filled.Mic,
                        contentDescription = "Voice input",
                        tint = if (isProcessing) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f) else Color(0xFFBB86FC)
                    )
                }
                IconButton(onClick = onAttachmentClick, enabled = !isProcessing) {
                    Icon(
                        imageVector = Icons.Filled.AttachFile,
                        contentDescription = "Attach file",
                        tint = if (isProcessing) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurface
                    )
                }
                Button(
                    onClick = onProcessClick,
                    enabled = captureText.isNotBlank() && !isProcessing,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (captureText.isNotBlank() && !isProcessing) Color(0xFFBB86FC) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                        contentColor = if (captureText.isNotBlank() && !isProcessing) Color.Black else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                ) {
                    if (isProcessing) {
                        Text("Processing...", fontWeight = FontWeight.Medium)
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Process", fontWeight = FontWeight.Medium)
                            Icon(
                                imageVector = Icons.Filled.ArrowForward,
                                contentDescription = "",
                                size = 18.dp
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContextualQuickChips(
    uiLanguage: String,
    onChipClick: (String) -> Unit
) {
    val chips = listOf(
        ChipData("format_json", "Format JSON", Icons.Filled.FormatAlignLeft),
        ChipData("redact_pii", "Redact PII", Icons.Filled.Shield),
        ChipData("extract_ocr", "Extract OCR", Icons.Filled.TextFields),
        ChipData("encrypt_note", "Encrypt Note", Icons.Filled.Lock),
        ChipData("extract_links", "Extract Links", Icons.Filled.Link)
    )

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(chips) { chip ->
            Chip(
                label = chip.label,
                icon = chip.icon,
                onClick = { onChipClick(chip.id) },
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
    }
}

@Composable
fun Chip(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.material3.Chip(
        onClick = onClick,
        modifier = modifier,
        colors = androidx.compose.material3.ChipDefaults.chipColors(
            containerColor = Color(0xFF1E1E1E),
            selectedContainerColor = Color(0xFFBB86FC)
        ),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.material3.ChipDefaults.chipBorder(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = "",
                tint = MaterialTheme.colorScheme.onSurface,
                size = 18.dp
            )
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguagePickerOverlay(
    visible: Boolean,
    onDismiss: () -> Unit,
    languageManager: LanguageManager,
    downloadedLanguages: Set<String>,
    onDownloadLanguage: (String) -> Unit
) {
    if (!visible) return

    val languages = languageManager.getPrioritizedLanguages()
    val selectedLanguage = languageManager.uiLanguage.value

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable { onDismiss() }
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .height(400.dp),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1E1E1E)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Select Language",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }

                androidx.compose.foundation.layout.Box(modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(languages) { lang ->
                        LanguageListItem(
                            language = lang,
                            isSelected = lang.code == selectedLanguage,
                            isDownloaded = downloadedLanguages.contains(lang.code),
                            onClick = {
                                if (downloadedLanguages.contains(lang.code)) {
                                    languageManager.setLanguage(lang.code)
                                    onDismiss()
                                } else {
                                    onDownloadLanguage(lang.code)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LanguageListItem(
    language: LanguageManager.LanguageInfo,
    isSelected: Boolean,
    isDownloaded: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFFBB86FC).copy(alpha = 0.15f) else Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        shape = RoundedCornerShape(12.dp),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBB86FC)) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = language.flag,
                    fontSize = 24.sp
                )
                Column {
                    Text(
                        text = language.nativeName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = language.englishName,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Selected",
                    tint = Color(0xFFBB86FC),
                    size = 24.dp
                )
            } else if (!isDownloaded) {
                Icon(
                    imageVector = Icons.Filled.Download,
                    contentDescription = "Download",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    size = 20.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Downloaded",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    size = 24.dp
                )
            }
        }
    }
}

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
        LanguageInfo("en", "English", "English", "🇺🇸", "US"),
        LanguageInfo("es", "Spanish", "Español", "🇪🇸", "ES"),
        LanguageInfo("fr", "French", "Français", "🇫🇷", "FR"),
        LanguageInfo("de", "German", "Deutsch", "🇩🇪", "DE"),
        LanguageInfo("zh", "Chinese (Simplified)", "中文", "🇨🇳", "CN"),
        LanguageInfo("ja", "Japanese", "日本語", "🇯🇵", "JP"),
        LanguageInfo("ko", "Korean", "한국어", "🇰🇷", "KR"),
        LanguageInfo("pt", "Portuguese", "Português", "🇵🇹", "PT"),
        LanguageInfo("ru", "Russian", "Русский", "🇷🇺", "RU"),
        LanguageInfo("ar", "Arabic", "العربية", "🇸🇦", "SA"),
        LanguageInfo("hi", "Hindi", "हिन्दी", "🇮🇳", "IN"),
        LanguageInfo("it", "Italian", "Italiano", "🇮🇹", "IT"),
        LanguageInfo("tr", "Turkish", "Türkçe", "🇹🇷", "TR"),
        LanguageInfo("pl", "Polish", "Polski", "🇵🇱", "PL"),
        LanguageInfo("nl", "Dutch", "Nederlands", "🇳🇱", "NL"),
        LanguageInfo("vi", "Vietnamese", "Tiếng Việt", "🇻🇳", "VN"),
        LanguageInfo("th", "Thai", "ไทย", "🇹🇭", "TH"),
        LanguageInfo("sv", "Swedish", "Svenska", "🇸🇪", "SE"),
        LanguageInfo("da", "Danish", "Dansk", "🇩🇰", "DK"),
        LanguageInfo("fi", "Finnish", "Suomi", "🇫🇮", "FI"),
        LanguageInfo("no", "Norwegian", "Norsk", "🇳🇴", "NO"),
        LanguageInfo("he", "Hebrew", "עברית", "🇮🇱", "IL"),
        LanguageInfo("id", "Indonesian", "Bahasa Indonesia", "🇮🇩", "ID"),
        LanguageInfo("ms", "Malay", "Bahasa Melayu", "🇲🇾", "MY"),
        LanguageInfo("cs", "Czech", "Čeština", "🇨🇿", "CZ"),
        LanguageInfo("hu", "Hungarian", "Magyar", "🇭🇺", "HU"),
        LanguageInfo("ro", "Romanian", "Română", "🇷🇴", "RO"),
        LanguageInfo("sk", "Slovak", "Slovenčina", "🇸🇰", "SK"),
        LanguageInfo("uk", "Ukrainian", "Українська", "🇺🇦", "UA"),
        LanguageInfo("el", "Greek", "Ελληνικά", "🇬🇷", "GR")
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

            if (lang.code == systemLanguage) {
                priority = 0
            } else if (lang.region == systemCountry) {
                priority = 10
            } else if (systemLanguage.startsWith(lang.code) || lang.code.startsWith(systemLanguage)) {
                priority = 20
            }

            localePriorityMap[lang.code] = priority
        }
    }

    fun getPrioritizedLanguages(): List<LanguageInfo> {
        return allLanguages.sortedBy { localePriorityMap[it.code] ?: 999 }
    }

    fun setLanguage(code: String) {
        _uiLanguage.value = code
    }

    fun downloadLanguagePack(code: String) {
        // Simulate downloading language pack (20-30 KB JSON)
        // In production: fetch from CDN/GitHub releases, cache to local storage
        Thread.sleep(500) // Simulate network delay
        // Save to DataStore/SharedPreferences for persistence
    }

    fun getString(key: String): String {
        // In production: load from downloaded JSON pack
        // Fallback to English
        return when (key) {
            "capture_placeholder" -> "Type, Paste, or Drop files..."
            "process" -> "Process"
            "format_json" -> "Format JSON"
            "redact_pii" -> "Redact PII"
            "extract_ocr" -> "Extract OCR"
            "encrypt_note" -> "Encrypt Note"
            "extract_links" -> "Extract Links"
            "settings" -> "Settings"
            "privacy_policy" -> "Privacy Policy"
            "about" -> "About"
            "select_language" -> "Select Language"
            else -> key
        }
    }
}

data class ChipData(
    val id: String,
    val label: String,
    val icon: ImageVector
)

object MaterialTheme {
    val colorScheme = ColorScheme(
        primary = Color(0xFFBB86FC),
        primaryVariant = Color(0xFF3700B3),
        secondary = Color(0xFF03DAC6),
        secondaryVariant = Color(0xFF018786),
        background = Color(0xFF121212),
        surface = Color(0xFF1E1E1E),
        error = Color(0xFFCF6679),
        onPrimary = Color.Black,
        onSecondary = Color.Black,
        onBackground = Color.White,
        onSurface = Color.White,
        onError = Color.Black,
        outline = Color.White.copy(alpha = 0.12f)
    )
}

data class ColorScheme(
    val primary: Color,
    val primaryVariant: Color,
    val secondary: Color,
    val secondaryVariant: Color,
    val background: Color,
    val surface: Color,
    val error: Color,
    val onPrimary: Color,
    val onSecondary: Color,
    val onBackground: Color,
    val onSurface: Color,
    val onError: Color,
    val outline: Color
)

object CardDefaults {
    fun cardColors(
        containerColor: Color = Color(0xFF1E1E1E),
        contentColor: Color = Color.White
    ) = CardColors(containerColor, contentColor)

    fun cardElevation(defaultElevation: androidx.compose.ui.unit.Dp) = CardElevation(defaultElevation)
}

data class CardColors(
    val containerColor: Color,
    val contentColor: Color
)

data class CardElevation(
    val defaultElevation: androidx.compose.ui.unit.Dp
