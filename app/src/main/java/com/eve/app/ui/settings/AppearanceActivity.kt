package com.eve.app.ui.settings

import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.eve.app.R
import com.eve.app.databinding.ActivityAppearanceBinding
import com.eve.app.theme.TelegramThemeEngine
import com.eve.app.ui.common.EveBaseActivity
import com.eve.app.util.AppBulletin
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

/**
 * Settings > Appearance: Full Telegram-style theme and accent color customizer.
 * Supports Day/Night/System modes, House default presets, Arctic, Dark Blue, Blue Bubbles,
 * real EVE question preview card, transactional apply/cancel/reset, and bounded .attheme imports.
 */
class AppearanceActivity : EveBaseActivity() {

    private lateinit var binding: ActivityAppearanceBinding
    private var isApplied = false

    private val importThemeLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                handleThemeImport(uri)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppearanceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        TelegramThemeEngine.init(this)
        if (savedInstanceState != null) {
            val dayTheme = savedInstanceState.getString("preview_day_theme", "day")
            val dayAccent = savedInstanceState.getInt("preview_day_accent", 9)
            val nightTheme = savedInstanceState.getString("preview_night_theme", "night")
            val nightAccent = savedInstanceState.getInt("preview_night_accent", 0)
            val modeStr = savedInstanceState.getString("preview_mode", TelegramThemeEngine.ThemeMode.SYSTEM.name)
            val mode = try { TelegramThemeEngine.ThemeMode.valueOf(modeStr) } catch (_: Exception) { TelegramThemeEngine.ThemeMode.SYSTEM }
            TelegramThemeEngine.restorePreviewState(dayTheme, dayAccent, nightTheme, nightAccent, mode, this)
        } else {
            TelegramThemeEngine.beginPreview()
        }

        binding.btnBack.setOnClickListener {
            TelegramThemeEngine.cancelPreview(this)
            finish()
        }

        binding.btnApply.setOnClickListener {
            isApplied = true
            TelegramThemeEngine.applyChanges(this)
            AppBulletin.show(this, "Theme applied")
            finish()
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                TelegramThemeEngine.cancelPreview(this@AppearanceActivity)
                finish()
            }
        })

        binding.btnResetDefault.setOnClickListener {
            TelegramThemeEngine.resetPreviewToDefaults(this)
            updateModeButtons()
            rebuildThemesList()
            rebuildAccentsList()
            updatePreviewCard()
            AppBulletin.show(this, "Reset preview to House theme default (tap Apply to save)")
        }

        binding.btnImportTheme.setOnClickListener {
            try {
                importThemeLauncher.launch("*/*")
            } catch (e: Exception) {
                AppBulletin.showError(this, "Cannot open file picker: ${e.message}")
            }
        }

        setupModeButtons()
        rebuildThemesList()
        rebuildAccentsList()
        updatePreviewCard()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("preview_day_theme", TelegramThemeEngine.getDayThemeId())
        outState.putInt("preview_day_accent", TelegramThemeEngine.getDayAccentId())
        outState.putString("preview_night_theme", TelegramThemeEngine.getNightThemeId())
        outState.putInt("preview_night_accent", TelegramThemeEngine.getNightAccentId())
        outState.putString("preview_mode", TelegramThemeEngine.getThemeMode().name)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing && !isApplied) {
            TelegramThemeEngine.cancelPreview(this)
        }
    }

    override fun onApplyThemePalette(palette: TelegramThemeEngine.ResolvedPalette) {
        super.onApplyThemePalette(palette)
        binding.root.setBackgroundColor(palette.windowBackground)
        binding.btnApply.backgroundTintList = ColorStateList.valueOf(palette.accent)
        binding.btnApply.setTextColor(palette.onAccent)
        updatePreviewCard()
        updateModeButtons()
    }

    private fun setupModeButtons() {
        binding.btnModeDay.setOnClickListener {
            TelegramThemeEngine.setPreviewMode(TelegramThemeEngine.ThemeMode.DAY, this)
            updateModeButtons()
            rebuildThemesList()
            rebuildAccentsList()
            updatePreviewCard()
        }
        binding.btnModeNight.setOnClickListener {
            TelegramThemeEngine.setPreviewMode(TelegramThemeEngine.ThemeMode.NIGHT, this)
            updateModeButtons()
            rebuildThemesList()
            rebuildAccentsList()
            updatePreviewCard()
        }
        binding.btnModeSystem.setOnClickListener {
            TelegramThemeEngine.setPreviewMode(TelegramThemeEngine.ThemeMode.SYSTEM, this)
            updateModeButtons()
            rebuildThemesList()
            rebuildAccentsList()
            updatePreviewCard()
        }
        updateModeButtons()
    }

    private fun updateModeButtons() {
        val currentMode = TelegramThemeEngine.getThemeMode()
        val palette = TelegramThemeEngine.resolveCurrentPalette(this)
        val selectedBg = palette.accent
        val selectedFg = palette.onAccent
        val defaultBg = ContextCompat.getColor(this, R.color.surface_inset)
        val defaultFg = palette.textPrimary

        fun highlight(btn: MaterialButton, active: Boolean) {
            val bg = if (active) selectedBg else defaultBg
            val fg = if (active) selectedFg else defaultFg
            btn.backgroundTintList = ColorStateList.valueOf(bg)
            btn.setTextColor(fg)
        }

        highlight(binding.btnModeDay, currentMode == TelegramThemeEngine.ThemeMode.DAY)
        highlight(binding.btnModeNight, currentMode == TelegramThemeEngine.ThemeMode.NIGHT)
        highlight(binding.btnModeSystem, currentMode == TelegramThemeEngine.ThemeMode.SYSTEM)
    }

    private fun rebuildThemesList() {
        binding.layoutThemesContainer.removeAllViews()
        val isDark = TelegramThemeEngine.isDarkActive(this)
        val activeThemeId = if (isDark) TelegramThemeEngine.getNightThemeId() else TelegramThemeEngine.getDayThemeId()

        val allThemes = TelegramThemeEngine.getAllThemes()
        val filteredThemes = allThemes.filter { it.isDark == isDark }

        val density = resources.displayMetrics.density
        for (theme in filteredThemes) {
            val isSelected = theme.id == activeThemeId
            val card = MaterialCardView(this).apply {
                radius = 16 * density
                strokeWidth = if (isSelected) (2 * density).toInt() else 0
                strokeColor = if (isSelected) theme.baseAccentColor else Color.TRANSPARENT
                setCardBackgroundColor(ContextCompat.getColor(this@AppearanceActivity, R.color.surface))
                elevation = 4 * density

                val lp = LinearLayout.LayoutParams((100 * density).toInt(), (110 * density).toInt()).apply {
                    marginEnd = (12 * density).toInt()
                }
                layoutParams = lp

                val content = LinearLayout(this@AppearanceActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    setPadding((8 * density).toInt(), (8 * density).toInt(), (8 * density).toInt(), (8 * density).toInt())

                    // Swatch circle
                    val circle = View(this@AppearanceActivity).apply {
                        val size = (36 * density).toInt()
                        layoutParams = LinearLayout.LayoutParams(size, size).apply {
                            bottomMargin = (8 * density).toInt()
                        }
                        background = android.graphics.drawable.GradientDrawable().apply {
                            shape = android.graphics.drawable.GradientDrawable.OVAL
                            setColor(theme.baseAccentColor)
                        }
                    }
                    addView(circle)

                    val label = TextView(this@AppearanceActivity).apply {
                        text = theme.name
                        gravity = Gravity.CENTER
                        setTextColor(ContextCompat.getColor(this@AppearanceActivity, R.color.text_primary))
                        textSize = 12f
                        typeface = if (isSelected) android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT
                    }
                    addView(label)
                }
                addView(content)

                setOnClickListener {
                    if (isDark) {
                        TelegramThemeEngine.setPreviewNightTheme(theme.id, theme.defaultAccentId, this@AppearanceActivity)
                    } else {
                        TelegramThemeEngine.setPreviewDayTheme(theme.id, theme.defaultAccentId, this@AppearanceActivity)
                    }
                    rebuildThemesList()
                    rebuildAccentsList()
                    updatePreviewCard()
                }
            }
            binding.layoutThemesContainer.addView(card)
        }
    }

    private fun rebuildAccentsList() {
        binding.layoutAccentsContainer.removeAllViews()
        val isDark = TelegramThemeEngine.isDarkActive(this)
        val activeThemeId = if (isDark) TelegramThemeEngine.getNightThemeId() else TelegramThemeEngine.getDayThemeId()
        val activeAccentId = if (isDark) TelegramThemeEngine.getNightAccentId() else TelegramThemeEngine.getDayAccentId()
        val theme = TelegramThemeEngine.getTheme(activeThemeId)

        val density = resources.displayMetrics.density
        for (accent in theme.accentOptions) {
            val isSelected = accent.id == activeAccentId
            val touchSize = (48 * density).toInt()
            val visualSize = (40 * density).toInt()

            val touchContainer = android.widget.FrameLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(touchSize, touchSize).apply {
                    marginEnd = (4 * density).toInt()
                    topMargin = (2 * density).toInt()
                    bottomMargin = (2 * density).toInt()
                }
                contentDescription = accent.name
                setOnClickListener {
                    if (isDark) {
                        TelegramThemeEngine.setPreviewNightTheme(theme.id, accent.id, this@AppearanceActivity)
                    } else {
                        TelegramThemeEngine.setPreviewDayTheme(theme.id, accent.id, this@AppearanceActivity)
                    }
                    rebuildAccentsList()
                    updatePreviewCard()
                }
            }

            val swatch = MaterialCardView(this).apply {
                radius = 20 * density
                strokeWidth = if (isSelected) (2 * density).toInt() else 0
                strokeColor = if (isSelected) ContextCompat.getColor(this@AppearanceActivity, R.color.text_primary) else Color.TRANSPARENT
                setCardBackgroundColor(accent.color)
                layoutParams = android.widget.FrameLayout.LayoutParams(visualSize, visualSize, Gravity.CENTER)
                isClickable = false
            }
            touchContainer.addView(swatch)
            binding.layoutAccentsContainer.addView(touchContainer)
        }
    }

    private fun updatePreviewCard() {
        val palette = TelegramThemeEngine.resolveCurrentPalette(this)
        binding.cardPreview.setCardBackgroundColor(palette.surface)
        binding.layoutPreviewContent.setBackgroundColor(palette.surface)
        binding.tvPreviewTitle.setTextColor(palette.textPrimary)
        binding.tvPreviewSubtitle.setTextColor(palette.textSecondary)
        binding.tvPreviewTag.setTextColor(palette.textSecondary)
        binding.btnPreviewAction.backgroundTintList = ColorStateList.valueOf(palette.accent)
        binding.btnPreviewAction.setTextColor(palette.onAccent)
    }

    private fun handleThemeImport(uri: Uri) {
        try {
            contentResolver.openInputStream(uri)?.use { stream ->
                // Enforce the size limit while reading, not after an unbounded readBytes().
                val output = java.io.ByteArrayOutputStream()
                val chunk = ByteArray(8192)
                while (true) {
                    val read = stream.read(chunk)
                    if (read < 0) break
                    if (read == 0) continue
                    if (output.size() + read > 256 * 1024) {
                        AppBulletin.showError(this, "Theme file exceeds 256KB limit")
                        return
                    }
                    output.write(chunk, 0, read)
                }
                val bytes = output.toByteArray()
                val colors = mutableMapOf<String, Int>()
                TelegramThemeEngine.parseAtthemeStream(bytes.inputStream(), colors)
                if (colors.isEmpty()) {
                    AppBulletin.showError(this, "Invalid or empty .attheme file")
                    return
                }

                val fileName = "custom_${System.currentTimeMillis()}.attheme"
                val destFile = java.io.File(filesDir, fileName)
                destFile.writeBytes(bytes)
                val customDef = TelegramThemeEngine.registerCustomTheme(this, destFile)
                if (customDef == null) {
                    destFile.delete()
                    AppBulletin.showError(this, "Theme import validation failed")
                    return
                }

                if (customDef.isDark) {
                    TelegramThemeEngine.setPreviewNightTheme(customDef.id, 0, this)
                } else {
                    TelegramThemeEngine.setPreviewDayTheme(customDef.id, 0, this)
                }

                rebuildThemesList()
                rebuildAccentsList()
                updatePreviewCard()
                AppBulletin.show(this, "Theme imported successfully")
            }
        } catch (e: Exception) {
            AppBulletin.showError(this, "Failed to import theme: ${e.message}")
        }
    }
}
