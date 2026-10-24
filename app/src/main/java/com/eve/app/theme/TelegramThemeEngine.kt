package com.eve.app.theme

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.Color
import androidx.core.graphics.ColorUtils
import androidx.core.math.MathUtils
import java.io.File
import java.io.InputStream
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Authoritative Telegram theme engine for EVE.
 * EVE-specific adaptation informed by Telegram theme sources; not an exact port.
 * Resolves .attheme color definitions, accent options, fallback chains, and transactional previews.
 */
object TelegramThemeEngine {

    const val PREFS_NAME = "eve_telegram_theme"
    private const val KEY_DAY_THEME = "day_theme_id"
    private const val KEY_DAY_ACCENT = "day_accent_id"
    private const val KEY_NIGHT_THEME = "night_theme_id"
    private const val KEY_NIGHT_ACCENT = "night_accent_id"
    private const val KEY_THEME_MODE = "theme_mode"
    private const val SCHEMA_VERSION = 1

    enum class ThemeMode {
        DAY, NIGHT, SYSTEM
    }

    data class ThemeAccentOption(
        val id: Int,
        val color: Int,
        val name: String
    )

    data class ThemeDefinition(
        val id: String,
        val name: String,
        val isDark: Boolean,
        val assetName: String?,
        val baseAccentColor: Int,
        val defaultAccentId: Int,
        val accentOptions: List<ThemeAccentOption>,
        val isCustom: Boolean = false,
        val customFilePath: String? = null
    )

    data class ResolvedPalette(
        val windowBackground: Int,
        val surface: Int,
        val surfaceInset: Int,
        val textPrimary: Int,
        val textSecondary: Int,
        val accent: Int,
        val onAccent: Int,
        val hairline: Int,
        val glassTargetMainTabs: Int,
        val glassTargetMainTopPanel: Int,
        val glassTabSelected: Int,
        val glassTabSelectedText: Int,
        val glassTabUnselected: Int,
        val actionSelector: Int,
        val drawerIcon: Int,
        val isDark: Boolean
    )

    // Built-in Day accents (from Theme.java:3979-3986)
    val DAY_ACCENTS = listOf(
        ThemeAccentOption(9, 0xFF56A2C9.toInt(), "Home Blue"),
        ThemeAccentOption(0, 0xFF4C91DF.toInt(), "Default Blue"),
        ThemeAccentOption(10, 0xFFCC6E83.toInt(), "Rose"),
        ThemeAccentOption(11, 0xFFD08E47.toInt(), "Amber"),
        ThemeAccentOption(12, 0xFFCC6462.toInt(), "Coral"),
        ThemeAccentOption(13, 0xFF867CD2.toInt(), "Violet"),
        ThemeAccentOption(1, 0xFF57B4D9.toInt(), "Cyan"),
        ThemeAccentOption(2, 0xFF54B169.toInt(), "Green"),
        ThemeAccentOption(3, 0xFFD9BF3F.toInt(), "Gold"),
        ThemeAccentOption(4, 0xFFCC6462.toInt(), "Red"),
        ThemeAccentOption(5, 0xFFCC6E83.toInt(), "Pink"),
        ThemeAccentOption(6, 0xFF9B7BD2.toInt(), "Purple"),
        ThemeAccentOption(7, 0xFFD79144.toInt(), "Orange"),
        ThemeAccentOption(8, 0xFF7B88AB.toInt(), "Slate")
    )

    // Built-in Night accents (from Theme.java:4003-4010)
    val NIGHT_ACCENTS = listOf(
        ThemeAccentOption(0, 0xFF3E88F7.toInt(), "Home Blue"),
        ThemeAccentOption(9, 0xFF6ABE3F.toInt(), "Green"),
        ThemeAccentOption(10, 0xFF8D78E3.toInt(), "Purple"),
        ThemeAccentOption(11, 0xFFDE5E7E.toInt(), "Pink"),
        ThemeAccentOption(12, 0xFF5977E8.toInt(), "Indigo"),
        ThemeAccentOption(13, 0xFFDBC11A.toInt(), "Yellow"),
        ThemeAccentOption(1, 0xFF4AB5D3.toInt(), "Cyan"),
        ThemeAccentOption(2, 0xFF4AB841.toInt(), "Emerald"),
        ThemeAccentOption(3, 0xFFD95576.toInt(), "Coral"),
        ThemeAccentOption(4, 0xFFE27D2B.toInt(), "Orange"),
        ThemeAccentOption(5, 0xFF936CDA.toInt(), "Violet"),
        ThemeAccentOption(6, 0xFFD04336.toInt(), "Red"),
        ThemeAccentOption(7, 0xFFE8AE1C.toInt(), "Amber"),
        ThemeAccentOption(8, 0xFF7988A3.toInt(), "Slate")
    )

    val BUILT_IN_THEMES = listOf(
        ThemeDefinition(
            id = "day",
            name = "Day",
            isDark = false,
            assetName = "day.attheme",
            baseAccentColor = 0xFF4C91DF.toInt(),
            defaultAccentId = 9,
            accentOptions = DAY_ACCENTS
        ),
        ThemeDefinition(
            id = "night",
            name = "Night",
            isDark = true,
            assetName = "night.attheme",
            baseAccentColor = 0xFF3E88F7.toInt(),
            defaultAccentId = 0,
            accentOptions = NIGHT_ACCENTS
        ),
        ThemeDefinition(
            id = "arctic",
            name = "Arctic Blue",
            isDark = false,
            assetName = "arctic.attheme",
            baseAccentColor = 0xFF40B1E2.toInt(),
            defaultAccentId = 0,
            accentOptions = listOf(
                ThemeAccentOption(0, 0xFF40B1E2.toInt(), "Arctic Ice"),
                ThemeAccentOption(1, 0xFF41B05D.toInt(), "Arctic Pine")
            )
        ),
        ThemeDefinition(
            id = "darkblue",
            name = "Dark Blue",
            isDark = true,
            assetName = "darkblue.attheme",
            baseAccentColor = 0xFF927BD4.toInt(),
            defaultAccentId = 0,
            accentOptions = listOf(
                ThemeAccentOption(0, 0xFF927BD4.toInt(), "Dark Indigo"),
                ThemeAccentOption(1, 0xFF698AFB.toInt(), "Ocean")
            )
        ),
        ThemeDefinition(
            id = "bluebubbles",
            name = "Blue Bubbles",
            isDark = false,
            assetName = "bluebubbles.attheme",
            baseAccentColor = 0xFF5890C5.toInt(),
            defaultAccentId = 99,
            accentOptions = listOf(
                ThemeAccentOption(99, 0xFF5890C5.toInt(), "Classic Blue")
            )
        )
    )

    private const val MAX_CUSTOM_THEME_BYTES = 256 * 1024
    private val customThemeFileName = Regex("^custom_[0-9]+\\.attheme$")
    private val customThemes = mutableListOf<ThemeDefinition>()
    private val listeners = mutableListOf<(ResolvedPalette) -> Unit>()

    // Committed state
    private var committedDayThemeId = "day"
    private var committedDayAccentId = 9
    private var committedNightThemeId = "night"
    private var committedNightAccentId = 0
    private var committedMode = ThemeMode.SYSTEM

    // Preview state (transactional)
    private var previewActive = false
    private var previewDayThemeId = "day"
    private var previewDayAccentId = 9
    private var previewNightThemeId = "night"
    private var previewNightAccentId = 0
    private var previewMode = ThemeMode.SYSTEM

    private var initialized = false

    val FALLBACK_KEYS: Map<String, String> = mapOf(
        "glass_targetMainTabs" to "dialogBackground",
        "glass_targetMainTopPanel" to "dialogBackground",
        "glass_tabSelected" to "chat_messagePanelSend",
        "glass_tabSelectedText" to "chat_messagePanelSend",
        "glass_tabUnselected" to "windowBackgroundWhiteBlackText",
        "dialogBackground" to "windowBackgroundWhite",
        "windowBackgroundWhiteGrayText" to "windowBackgroundWhiteBlackText"
    )

    private val RECOGNIZED_THEME_KEYS = setOf(
        "windowBackgroundGray", "windowBackgroundWhite", "chat_messagePanelSend",
        "dialogBackground", "windowBackgroundWhiteBlackText", "windowBackgroundWhiteGrayText"
    )

    fun init(context: Context) {
        if (initialized) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        committedDayThemeId = prefs.getString(KEY_DAY_THEME, "day") ?: "day"
        committedDayAccentId = prefs.getInt(KEY_DAY_ACCENT, 9)
        committedNightThemeId = prefs.getString(KEY_NIGHT_THEME, "night") ?: "night"
        committedNightAccentId = prefs.getInt(KEY_NIGHT_ACCENT, 0)

        // Persist the initial SYSTEM choice before mirroring it to the legacy preference.
        // Otherwise a second cold start misreads that mirror as an explicit DAY/NIGHT choice.
        val legacyPrefs = context.getSharedPreferences(com.eve.app.util.ThemeManager.PREFS, Context.MODE_PRIVATE)
        committedMode = resolvePersistedMode(prefs, legacyPrefs)

        // Synchronize NightMode globally
        syncNightModeWithSystem(context)
        restoreCustomThemes(context)
        initialized = true
    }

    internal fun resolvePersistedMode(prefs: SharedPreferences, legacyPrefs: SharedPreferences): ThemeMode {
        if (prefs.contains(KEY_THEME_MODE)) {
            val stored = prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name)
            return try {
                ThemeMode.valueOf(stored ?: ThemeMode.SYSTEM.name)
            } catch (_: IllegalArgumentException) {
                ThemeMode.SYSTEM
            }
        }
        val migrated = if (legacyPrefs.contains(com.eve.app.util.ThemeManager.KEY_DARK_MODE)) {
            if (legacyPrefs.getBoolean(com.eve.app.util.ThemeManager.KEY_DARK_MODE, false)) {
                ThemeMode.NIGHT
            } else {
                ThemeMode.DAY
            }
        } else {
            ThemeMode.SYSTEM
        }
        prefs.edit().putString(KEY_THEME_MODE, migrated.name).apply()
        return migrated
    }

    private fun syncNightModeWithSystem(context: Context) {
        val targetNightMode = when (committedMode) {
            ThemeMode.DAY -> androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
            ThemeMode.NIGHT -> androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
            ThemeMode.SYSTEM -> androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        if (androidx.appcompat.app.AppCompatDelegate.getDefaultNightMode() != targetNightMode) {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(targetNightMode)
        }
        context.getSharedPreferences(com.eve.app.util.ThemeManager.PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(com.eve.app.util.ThemeManager.KEY_DARK_MODE, isDarkActive(context))
            .apply()
    }

    /**
     * Restore only bounded, app-private theme files. Persisted selections refer to
     * the same deterministic ID on the next process start.
     */
    private fun restoreCustomThemes(context: Context) {
        context.filesDir.listFiles { file ->
            file.isFile && customThemeFileName.matches(file.name)
        }?.sortedBy { it.name }?.forEach { registerCustomTheme(context, it) }
    }

    /**
     * Register a validated imported theme before it can be previewed or selected.
     * An unregistered ID otherwise silently falls back to the built-in Day theme.
     */
    fun registerCustomTheme(context: Context, file: File): ThemeDefinition? {
        if (!customThemeFileName.matches(file.name) ||
            !file.isFile || file.length() <= 0L || file.length() > MAX_CUSTOM_THEME_BYTES) {
            return null
        }
        if (file.canonicalFile.parentFile != context.filesDir.canonicalFile) return null

        val colors = mutableMapOf<String, Int>()
        try {
            file.inputStream().use { parseAtthemeStream(it, colors) }
        } catch (_: Exception) {
            return null
        }
        if (colors.isEmpty() || colors.none { it.key in RECOGNIZED_THEME_KEYS }) return null

        val accent = colors["chat_messagePanelSend"] ?: 0xFF56A2C9.toInt()
        val bg = colors["windowBackgroundGray"] ?: colors["windowBackgroundWhite"]
        val isDark = if (bg != null) computePerceivedBrightness(bg) < 0.45f else false
        val definition = ThemeDefinition(
            id = file.name.removeSuffix(".attheme"),
            name = "Imported Theme",
            isDark = isDark,
            assetName = null,
            baseAccentColor = accent,
            defaultAccentId = 0,
            accentOptions = listOf(ThemeAccentOption(0, accent, "Custom")),
            isCustom = true,
            customFilePath = file.canonicalPath
        )
        customThemes.removeAll { it.id == definition.id }
        customThemes.add(definition)
        return definition
    }

    fun getAllThemes(): List<ThemeDefinition> = BUILT_IN_THEMES + customThemes

    fun getTheme(id: String, isDark: Boolean = false): ThemeDefinition {
        return getAllThemes().firstOrNull { it.id == id }
            ?: (if (isDark) getAllThemes().firstOrNull { it.id == "night" } ?: BUILT_IN_THEMES[1]
                else getAllThemes().firstOrNull { it.id == "day" } ?: BUILT_IN_THEMES[0])
    }

    fun getDayThemeId(): String = if (previewActive) previewDayThemeId else committedDayThemeId
    fun getDayAccentId(): Int = if (previewActive) previewDayAccentId else committedDayAccentId
    fun getNightThemeId(): String = if (previewActive) previewNightThemeId else committedNightThemeId
    fun getNightAccentId(): Int = if (previewActive) previewNightAccentId else committedNightAccentId
    fun getThemeMode(): ThemeMode = if (previewActive) previewMode else committedMode

    fun isPreviewActive(): Boolean = previewActive

    fun restorePreviewState(
        dayTheme: String,
        dayAccent: Int,
        nightTheme: String,
        nightAccent: Int,
        mode: ThemeMode,
        context: Context
    ) {
        previewActive = true
        previewDayThemeId = dayTheme
        previewDayAccentId = dayAccent
        previewNightThemeId = nightTheme
        previewNightAccentId = nightAccent
        previewMode = mode
        notifyPaletteChanged(context)
    }

    fun beginPreview() {
        previewActive = true
        previewDayThemeId = committedDayThemeId
        previewDayAccentId = committedDayAccentId
        previewNightThemeId = committedNightThemeId
        previewNightAccentId = committedNightAccentId
        previewMode = committedMode
    }

    fun setPreviewDayTheme(themeId: String, accentId: Int, context: Context) {
        previewActive = true
        previewDayThemeId = themeId
        previewDayAccentId = accentId
        notifyPaletteChanged(context)
    }

    fun setPreviewNightTheme(themeId: String, accentId: Int, context: Context) {
        previewActive = true
        previewNightThemeId = themeId
        previewNightAccentId = accentId
        notifyPaletteChanged(context)
    }

    fun setPreviewMode(mode: ThemeMode, context: Context) {
        previewActive = true
        previewMode = mode
        notifyPaletteChanged(context)
    }

    fun setCommittedMode(mode: ThemeMode, context: Context) {
        committedMode = mode
        previewActive = false
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_THEME_MODE, committedMode.name).apply()
        syncNightModeWithSystem(context)
        notifyPaletteChanged(context)
    }

    fun applyChanges(context: Context) {
        committedDayThemeId = previewDayThemeId
        committedDayAccentId = previewDayAccentId
        committedNightThemeId = previewNightThemeId
        committedNightAccentId = previewNightAccentId
        committedMode = previewMode
        previewActive = false

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_DAY_THEME, committedDayThemeId)
            .putInt(KEY_DAY_ACCENT, committedDayAccentId)
            .putString(KEY_NIGHT_THEME, committedNightThemeId)
            .putInt(KEY_NIGHT_ACCENT, committedNightAccentId)
            .putString(KEY_THEME_MODE, committedMode.name)
            .apply()

        syncNightModeWithSystem(context)
        notifyPaletteChanged(context)
    }

    fun cancelPreview(context: Context) {
        previewActive = false
        previewDayThemeId = committedDayThemeId
        previewDayAccentId = committedDayAccentId
        previewNightThemeId = committedNightThemeId
        previewNightAccentId = committedNightAccentId
        previewMode = committedMode
        notifyPaletteChanged(context)
    }

    fun resetPreviewToDefaults(context: Context) {
        // Reset is part of the preview transaction. Only Apply may persist it.
        previewActive = true
        previewDayThemeId = "day"
        previewDayAccentId = 9
        previewNightThemeId = "night"
        previewNightAccentId = 0
        previewMode = ThemeMode.SYSTEM
        notifyPaletteChanged(context)
    }

    fun resetToDefaults(context: Context) {
        resetPreviewToDefaults(context)
    }

    fun addThemeChangeListener(listener: (ResolvedPalette) -> Unit) {
        listeners.add(listener)
    }

    fun removeThemeChangeListener(listener: (ResolvedPalette) -> Unit) {
        listeners.remove(listener)
    }

    private fun notifyPaletteChanged(context: Context) {
        val palette = resolveCurrentPalette(context)
        for (listener in listeners) {
            listener(palette)
        }
    }

    fun isDarkActive(context: Context): Boolean {
        return when (getThemeMode()) {
            ThemeMode.DAY -> false
            ThemeMode.NIGHT -> true
            ThemeMode.SYSTEM -> {
                val nightMask = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                nightMask == Configuration.UI_MODE_NIGHT_YES
            }
        }
    }

    fun resolveCurrentPalette(context: Context): ResolvedPalette {
        if (!initialized) init(context)
        val isDark = isDarkActive(context)
        val themeId = if (isDark) getNightThemeId() else getDayThemeId()
        val accentId = if (isDark) getNightAccentId() else getDayAccentId()
        val themeDef = getTheme(themeId, isDark)
        return resolvePalette(context, themeDef, accentId)
    }

    fun resolvePalette(context: Context, themeDef: ThemeDefinition, accentId: Int): ResolvedPalette {
        val rawColors = loadThemeRawColors(context, themeDef)
        val isDark = themeDef.isDark

        // Determine accent color
        val accentColor = themeDef.accentOptions.firstOrNull { it.id == accentId }?.color
            ?: themeDef.baseAccentColor

        // Apply accent color shift if needed
        val adjustedColors = applyAccentShift(rawColors, themeDef.baseAccentColor, accentColor, isDark)

        // Resolve keys with fallback chain
        val windowBg = resolveColor(adjustedColors, "windowBackgroundGray", if (isDark) 0xFF000000.toInt() else 0xFFF5F5F5.toInt())
        val surface = resolveColor(adjustedColors, "windowBackgroundWhite", if (isDark) 0xFF181819.toInt() else 0xFFFFFFFF.toInt())
        val surfaceInset = if (isDark) 0xFF2C2C2E.toInt() else 0xFFF5F5F5.toInt()
        val textPrimary = resolveColor(adjustedColors, "windowBackgroundWhiteBlackText", if (isDark) 0xFFFFFFFF.toInt() else 0xFF1A1D21.toInt())
        val textSecondary = resolveColor(adjustedColors, "windowBackgroundWhiteGrayText", if (isDark) 0xFFA1A1A6.toInt() else 0xFF6C6C70.toInt())

        // Chat panel send button color / tab selected
        val sendColor = resolveColor(adjustedColors, "chat_messagePanelSend", accentColor)
        val glassTabSelected = resolveColor(adjustedColors, "glass_tabSelected", sendColor)
        val glassTabSelectedText = resolveColor(adjustedColors, "glass_tabSelectedText", sendColor)
        val glassTabUnselected = resolveColor(adjustedColors, "glass_tabUnselected", textPrimary)
        val glassTargetTabs = resolveColor(adjustedColors, "glass_targetMainTabs", if (isDark) 0xFF232324.toInt() else 0xFFFFFFFF.toInt())
        val glassTargetTop = resolveColor(adjustedColors, "glass_targetMainTopPanel", if (isDark) 0xFF232324.toInt() else 0xFFFFFFFF.toInt())
        val actionSelector = if (isDark) 0x16FFFFFF else 0x121A1D21
        val drawerIcon = if (isDark) 0xFF828282.toInt() else 0xFF889198.toInt()
        val hairline = if (isDark) 0x2EFFFFFF else 0x1F000000

        // Prefer the foreground with the higher WCAG contrast ratio. A brightness
        // threshold picks white on House Blue (only ~2.84:1), which is unreadable.
        val onAccent = if (ColorUtils.calculateContrast(Color.BLACK, accentColor) >=
            ColorUtils.calculateContrast(Color.WHITE, accentColor)
        ) Color.BLACK else Color.WHITE

        return ResolvedPalette(
            windowBackground = windowBg,
            surface = surface,
            surfaceInset = surfaceInset,
            textPrimary = textPrimary,
            textSecondary = textSecondary,
            accent = accentColor,
            onAccent = onAccent,
            hairline = hairline,
            glassTargetMainTabs = glassTargetTabs,
            glassTargetMainTopPanel = glassTargetTop,
            glassTabSelected = glassTabSelected,
            glassTabSelectedText = glassTabSelectedText,
            glassTabUnselected = glassTabUnselected,
            actionSelector = actionSelector,
            drawerIcon = drawerIcon,
            isDark = isDark
        )
    }

    private fun loadThemeRawColors(context: Context, themeDef: ThemeDefinition): Map<String, Int> {
        val result = mutableMapOf<String, Int>()
        if (themeDef.isCustom && themeDef.customFilePath != null) {
            val file = File(themeDef.customFilePath)
            if (file.exists()) {
                parseAtthemeStream(file.inputStream(), result)
            }
            return result
        }

        val assetName = themeDef.assetName ?: return result
        try {
            val assetPath = "themes/$assetName"
            context.assets.open(assetPath).use { stream ->
                parseAtthemeStream(stream, result)
            }
        } catch (_: Exception) {
            // Asset load fallback
        }
        return result
    }

    fun parseAtthemeStream(stream: InputStream, out: MutableMap<String, Int>) {
        val reader = stream.bufferedReader()
        for (line in reader.lineSequence()) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("//")) continue
            val eq = trimmed.indexOf('=')
            if (eq > 0) {
                val key = trimmed.substring(0, eq).trim()
                val valueStr = trimmed.substring(eq + 1).trim()
                val color = parseColorValue(valueStr)
                if (color != null) {
                    out[key] = color
                }
            }
        }
    }

    fun parseColorValue(valueStr: String): Int? {
        return try {
            if (valueStr.startsWith("#")) {
                Color.parseColor(valueStr)
            } else {
                val longVal = valueStr.toLong()
                if (longVal < Int.MIN_VALUE || longVal > 0xFFFFFFFFL) {
                    null
                } else {
                    longVal.toInt()
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun resolveColor(colors: Map<String, Int>, key: String, default: Int): Int {
        var currentKey: String? = key
        val visited = HashSet<String>()
        while (currentKey != null && visited.add(currentKey)) {
            val color = colors[currentKey]
            if (color != null) return color
            currentKey = FALLBACK_KEYS[currentKey]
        }
        return default
    }

    /**
     * EVE adaptation of Telegram's accent-shift concept; not source-exact.
     */
    fun applyAccentShift(rawColors: Map<String, Int>, baseAccentColor: Int, targetAccentColor: Int, isDark: Boolean): Map<String, Int> {
        if (baseAccentColor == 0 || targetAccentColor == 0 || baseAccentColor == targetAccentColor) {
            return rawColors
        }

        val baseHsv = FloatArray(3)
        val accentHsv = FloatArray(3)
        Color.colorToHSV(baseAccentColor, baseHsv)
        Color.colorToHSV(targetAccentColor, accentHsv)

        val result = HashMap(rawColors)
        for ((key, color) in rawColors) {
            // Apply accent transformation to blue/accent-tinted keys
            val newColor = changeColorAccent(baseHsv, accentHsv, color, isDark, color)
            if (newColor != color) {
                result[key] = newColor
            }
        }
        return result
    }

    fun changeColorAccent(baseHsv: FloatArray, accentHsv: FloatArray, color: Int, isDarkTheme: Boolean, fallback: Int): Int {
        val colorHsv = FloatArray(3)
        Color.colorToHSV(color, colorHsv)

        val diffH = min(abs(colorHsv[0] - baseHsv[0]), abs(colorHsv[0] - baseHsv[0] - 360f))
        if (diffH > 30f) {
            return fallback
        }

        val dist = if (baseHsv[1] > 0f) min(1.5f * colorHsv[1] / baseHsv[1], 1f) else 0f
        colorHsv[0] = (colorHsv[0] + accentHsv[0] - baseHsv[0] + 360f) % 360f
        colorHsv[1] = if (baseHsv[1] > 0f) MathUtils.clamp(colorHsv[1] * accentHsv[1] / baseHsv[1], 0f, 1f) else 0f
        colorHsv[2] = if (baseHsv[2] > 0f) MathUtils.clamp(colorHsv[2] * (1f - dist + dist * accentHsv[2] / baseHsv[2]), 0f, 1f) else colorHsv[2]

        var newColor = Color.HSVToColor(Color.alpha(color), colorHsv)

        val origBrightness = computePerceivedBrightness(color)
        val newBrightness = computePerceivedBrightness(newColor)

        val needRevertBrightness = if (isDarkTheme) origBrightness > newBrightness else origBrightness < newBrightness

        if (needRevertBrightness && newBrightness > 0f) {
            val amountOfNew = 0.6f
            val fallbackAmount = (1f - amountOfNew) * origBrightness / newBrightness + amountOfNew
            newColor = changeBrightness(newColor, fallbackAmount)
        }

        return newColor
    }

    private fun changeBrightness(color: Int, amount: Float): Int {
        val r = MathUtils.clamp((Color.red(color) * amount).roundToInt(), 0, 255)
        val g = MathUtils.clamp((Color.green(color) * amount).roundToInt(), 0, 255)
        val b = MathUtils.clamp((Color.blue(color) * amount).roundToInt(), 0, 255)
        return Color.argb(Color.alpha(color), r, g, b)
    }

    fun computePerceivedBrightness(color: Int): Float {
        return (Color.red(color) * 0.299f + Color.green(color) * 0.587f + Color.blue(color) * 0.114f) / 255f
    }

    fun multAlpha(color: Int, alpha: Float): Int {
        val a = MathUtils.clamp((Color.alpha(color) * alpha).roundToInt(), 0, 255)
        return Color.argb(a, Color.red(color), Color.green(color), Color.blue(color))
    }

    /**
     * Solve source color for blur overlays according to BlurredBackgroundProviderImpl.solveSrcColor.
     */
    fun solveSrcColor(bgColor: Int, outColor: Int, rawAlpha: Float): Int {
        val alpha = MathUtils.clamp(rawAlpha, 0f, 1f)
        if (alpha <= 0f) return 0
        if (alpha >= 1f) return Color.argb(255, Color.red(outColor), Color.green(outColor), Color.blue(outColor))

        val bgR = Color.red(bgColor)
        val bgG = Color.green(bgColor)
        val bgB = Color.blue(bgColor)

        val outR = Color.red(outColor)
        val outG = Color.green(outColor)
        val outB = Color.blue(outColor)

        val invA = 1f - alpha
        val srcR = MathUtils.clamp(((outR - bgR * invA) / alpha).roundToInt(), 0, 255)
        val srcG = MathUtils.clamp(((outG - bgG * invA) / alpha).roundToInt(), 0, 255)
        val srcB = MathUtils.clamp(((outB - bgB * invA) / alpha).roundToInt(), 0, 255)
        val a8 = MathUtils.clamp((alpha * 255f).roundToInt(), 0, 255)

        return Color.argb(a8, srcR, srcG, srcB)
    }
}
