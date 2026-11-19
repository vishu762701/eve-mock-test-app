/*
 * Derived from Telegram Android ThemeActivity.java @ 21f7d109da6ab3506c9e9577f98c6a38771ab335
 * Copyright Nikolai Kudashov, 2013-2023. GNU GPL v2 or later. See THIRD_PARTY_NOTICES.md.
 */

package com.eve.app.ui.settings

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import com.eve.app.R
import com.eve.app.designsystem.DesignCardView
import com.eve.app.theme.TelegramThemeEngine
import com.eve.app.ui.common.EveBaseActivity
import com.eve.app.util.AppBulletin
import org.telegram.ui.ActionBar.ActionBar
import org.telegram.ui.ActionBar.AlertDialog
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.Cells.HeaderCell
import org.telegram.ui.Cells.ShadowSectionCell
import org.telegram.ui.Cells.SlideIntChooseView
import org.telegram.ui.Cells.TextCell
import org.telegram.ui.Cells.TextInfoPrivacyCell
import org.telegram.ui.Components.CheckBox2
import org.telegram.ui.Components.LayoutHelper
import org.telegram.ui.Components.SlideChooseView
import org.telegram.ui.Stories.recorder.ButtonWithCounterView

/**
 * Settings > Appearance: Powered by real ported Telegram ActionBar, SlideChooseView,
 * SlideIntChooseView, HeaderCell, TextCell, CheckBox2, AlertDialog, and ButtonWithCounterView.
 * Preserves Day/Night/System modes, House presets, accent colors, live EVE question preview,
 * Auto-Night schedule, transactional apply/cancel/reset, and bounded .attheme imports.
 */
open class ThemeActivity : EveBaseActivity() {

    private fun dp(value: Float): Int = (value * resources.displayMetrics.density + 0.5f).toInt()

    private var isApplied = false
    private var currentFontSize: Int = 16
    private var currentBubbleRadius: Int = 10

    private lateinit var rootContainer: FrameLayout
    private lateinit var actionBar: ActionBar
    private lateinit var cardPreview: DesignCardView
    private lateinit var layoutPreviewContent: LinearLayout
    private lateinit var tvPreviewTag: TextView
    private lateinit var tvPreviewTitle: TextView
    private lateinit var tvPreviewSubtitle: TextView
    private lateinit var btnPreviewAction: TextView

    private lateinit var modeSlider: SlideChooseView
    private lateinit var layoutThemesContainer: LinearLayout
    private lateinit var layoutAccentsContainer: LinearLayout
    private lateinit var textSizeSlider: SlideIntChooseView
    private lateinit var cornerRadiusSlider: SlideIntChooseView
    private lateinit var cellAutoNight: TextCell
    private lateinit var cellImportTheme: TextCell
    private lateinit var cellResetDefault: TextCell
    private lateinit var btnApply: ButtonWithCounterView

    private val sectionCards = mutableListOf<View>()

    private val importThemeLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                handleThemeImport(uri)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        TelegramThemeEngine.init(this)
        currentFontSize = TelegramThemeEngine.getFontSize(this)
        currentBubbleRadius = TelegramThemeEngine.getBubbleRadius(this)

        if (savedInstanceState != null) {
            val dayTheme = savedInstanceState.getString("preview_day_theme", "day")
            val dayAccent = savedInstanceState.getInt("preview_day_accent", 9)
            val nightTheme = savedInstanceState.getString("preview_night_theme", "night")
            val nightAccent = savedInstanceState.getInt("preview_night_accent", 0)
            val modeStr = savedInstanceState.getString("preview_mode", TelegramThemeEngine.ThemeMode.SYSTEM.name)
            val mode = try {
                TelegramThemeEngine.ThemeMode.valueOf(modeStr)
            } catch (_: Exception) {
                TelegramThemeEngine.ThemeMode.SYSTEM
            }
            TelegramThemeEngine.restorePreviewState(dayTheme, dayAccent, nightTheme, nightAccent, mode, this)
            currentFontSize = savedInstanceState.getInt("preview_font_size", currentFontSize)
            currentBubbleRadius = savedInstanceState.getInt("preview_bubble_radius", currentBubbleRadius)
        } else {
            TelegramThemeEngine.beginPreview()
        }

        Theme.syncFromEvePalette(this)
        val palette = TelegramThemeEngine.resolveCurrentPalette(this)

        rootContainer = FrameLayout(this).apply {
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray))
        }
        setContentView(rootContainer)

        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        rootContainer.addView(mainLayout)

        // 1. Real Telegram ActionBar
        actionBar = ActionBar(this).apply {
            setBackgroundColor(Theme.getColor(Theme.key_actionBarDefault))
            setItemsBackgroundColor(Theme.getColor(Theme.key_actionBarDefaultSelector), false)
            setItemsColor(Theme.getColor(Theme.key_actionBarDefaultIcon), false)
            setTitleColor(Theme.getColor(Theme.key_actionBarDefaultTitle))
            setBackButtonImage(R.drawable.ic_ab_back)
            setTitle("Appearance")
            setAllowOverlayTitle(true)
            setActionBarMenuOnItemClick(object : ActionBar.ActionBarMenuOnItemClick() {
                override fun onItemClick(id: Int) {
                    if (id == -1) {
                        TelegramThemeEngine.cancelPreview(this@ThemeActivity)
                        finish()
                    }
                }
            })
        }
        mainLayout.addView(actionBar, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))

        // 2. Scrollable Content
        val scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
            isFillViewport = true
        }
        val contentLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        scrollView.addView(contentLayout)
        mainLayout.addView(scrollView)

        val cardBg = Theme.getColor(Theme.key_windowBackgroundWhite)

        // Live Question Preview Card
        val previewWrapper = FrameLayout(this).apply {
            setPadding(dp(16f), dp(16f), dp(16f), dp(16f))
        }
        cardPreview = DesignCardView(this).apply {
            cardElevation = dp(2f).toFloat()
            radius = dp(currentBubbleRadius.toFloat()).toFloat()
        }
        layoutPreviewContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16f), dp(14f), dp(16f), dp(14f))
        }
        tvPreviewTag = TextView(this).apply {
            text = "LIVE QUESTION PREVIEW • MOCK TEST"
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
        }
        tvPreviewTitle = TextView(this).apply {
            text = "Which article of the Indian Constitution deals with Fundamental Rights?"
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, dp(6f), 0, dp(4f))
        }
        tvPreviewSubtitle = TextView(this).apply {
            text = "A) Articles 12–35   •   B) Articles 36–51"
            setPadding(0, 0, 0, dp(10f))
        }
        btnPreviewAction = TextView(this).apply {
            text = "Save & Next"
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(dp(16f), dp(8f), dp(16f), dp(8f))
        }
        layoutPreviewContent.addView(tvPreviewTag)
        layoutPreviewContent.addView(tvPreviewTitle)
        layoutPreviewContent.addView(tvPreviewSubtitle)
        layoutPreviewContent.addView(
            btnPreviewAction,
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        )
        cardPreview.addView(layoutPreviewContent)
        previewWrapper.addView(
            cardPreview,
            FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT)
        )
        contentLayout.addView(previewWrapper)

        // Section 1: Theme Mode (Real Telegram SlideChooseView)
        val headerMode = HeaderCell(this).apply {
            setBackgroundColor(cardBg)
            setText("Theme Mode")
        }
        sectionCards.add(headerMode)
        contentLayout.addView(headerMode)

        val initialModeIdx = when (TelegramThemeEngine.getThemeMode()) {
            TelegramThemeEngine.ThemeMode.DAY -> 0
            TelegramThemeEngine.ThemeMode.NIGHT -> 1
            TelegramThemeEngine.ThemeMode.SYSTEM -> 2
        }
        modeSlider = SlideChooseView(this).apply {
            setBackgroundColor(cardBg)
            setOptions(initialModeIdx, "Day", "Night", "System")
            setCallback { idx ->
                val nextMode = when (idx) {
                    0 -> TelegramThemeEngine.ThemeMode.DAY
                    1 -> TelegramThemeEngine.ThemeMode.NIGHT
                    else -> TelegramThemeEngine.ThemeMode.SYSTEM
                }
                TelegramThemeEngine.setPreviewMode(nextMode, this@ThemeActivity)
                refreshAllPreviewViews()
            }
        }
        sectionCards.add(modeSlider)
        contentLayout.addView(modeSlider, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 50))
        contentLayout.addView(ShadowSectionCell(this))

        // Section 2: Color Theme Presets & Accent Colors
        val headerThemes = HeaderCell(this).apply {
            setBackgroundColor(cardBg)
            setText("Color Theme")
        }
        sectionCards.add(headerThemes)
        contentLayout.addView(headerThemes)

        val themesScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            setBackgroundColor(cardBg)
            setPadding(dp(16f), dp(6f), dp(16f), dp(12f))
            clipToPadding = false
        }
        sectionCards.add(themesScroll)
        layoutThemesContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        themesScroll.addView(layoutThemesContainer)
        contentLayout.addView(themesScroll)

        val accentsScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            setBackgroundColor(cardBg)
            setPadding(dp(16f), dp(2f), dp(16f), dp(14f))
            clipToPadding = false
        }
        sectionCards.add(accentsScroll)
        layoutAccentsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        accentsScroll.addView(layoutAccentsContainer)
        contentLayout.addView(accentsScroll)
        contentLayout.addView(ShadowSectionCell(this))

        // Section 3: Text Size (Real Telegram SlideIntChooseView)
        val headerTextSize = HeaderCell(this).apply {
            setBackgroundColor(cardBg)
            setText("Message & Question Text Size")
        }
        sectionCards.add(headerTextSize)
        contentLayout.addView(headerTextSize)

        textSizeSlider = SlideIntChooseView(this, null).apply {
            setBackgroundColor(cardBg)
            set(
                currentFontSize.coerceIn(12, 30),
                SlideIntChooseView.Options.make(0, 12, 30) { v -> "${v}sp" }
            ) { newSize ->
                currentFontSize = newSize
                updatePreviewCard()
            }
        }
        sectionCards.add(textSizeSlider)
        contentLayout.addView(textSizeSlider, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))
        contentLayout.addView(ShadowSectionCell(this))

        // Section 4: Card Corner Radius (Real Telegram SlideIntChooseView)
        val headerRadius = HeaderCell(this).apply {
            setBackgroundColor(cardBg)
            setText("Card Corner Radius")
        }
        sectionCards.add(headerRadius)
        contentLayout.addView(headerRadius)

        cornerRadiusSlider = SlideIntChooseView(this, null).apply {
            setBackgroundColor(cardBg)
            set(
                currentBubbleRadius.coerceIn(0, 17),
                SlideIntChooseView.Options.make(0, 0, 17) { v -> "${v}dp" }
            ) { newRadius ->
                currentBubbleRadius = newRadius
                updatePreviewCard()
            }
        }
        sectionCards.add(cornerRadiusSlider)
        contentLayout.addView(cornerRadiusSlider, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))
        contentLayout.addView(ShadowSectionCell(this))

        // Section 5: Auto-Night, Import .attheme, Reset
        val headerMore = HeaderCell(this).apply {
            setBackgroundColor(cardBg)
            setText("Theme Options")
        }
        sectionCards.add(headerMore)
        contentLayout.addView(headerMore)

        cellAutoNight = TextCell(this).apply {
            setBackgroundColor(cardBg)
            setOnClickListener { showAutoNightDialog() }
        }
        sectionCards.add(cellAutoNight)
        contentLayout.addView(cellAutoNight)

        cellImportTheme = TextCell(this).apply {
            setBackgroundColor(cardBg)
            setText("Import .attheme File", true)
            setOnClickListener {
                try {
                    importThemeLauncher.launch("*/*")
                } catch (e: Exception) {
                    AppBulletin.showError(this@ThemeActivity, "Cannot open file picker: ${e.message}")
                }
            }
        }
        sectionCards.add(cellImportTheme)
        contentLayout.addView(cellImportTheme)

        cellResetDefault = TextCell(this).apply {
            setBackgroundColor(cardBg)
            setText("Reset to House Default", false)
            setOnClickListener {
                TelegramThemeEngine.resetPreviewToDefaults(this@ThemeActivity)
                currentFontSize = 16
                currentBubbleRadius = 10
                textSizeSlider.set(currentFontSize, SlideIntChooseView.Options.make(0, 12, 30) { v -> "${v}sp" }) { newSize ->
                    currentFontSize = newSize
                    updatePreviewCard()
                }
                cornerRadiusSlider.set(currentBubbleRadius, SlideIntChooseView.Options.make(0, 0, 17) { v -> "${v}dp" }) { newRadius ->
                    currentBubbleRadius = newRadius
                    updatePreviewCard()
                }
                modeSlider.setOptions(2, "Day", "Night", "System")
                refreshAllPreviewViews()
                AppBulletin.show(this@ThemeActivity, "Reset preview to House theme default (tap Apply to save)")
            }
        }
        sectionCards.add(cellResetDefault)
        contentLayout.addView(cellResetDefault)

        val infoFooter = TextInfoPrivacyCell(this).apply {
            setText("Preview changes live above. Tap Apply Theme to save across EVE.")
        }
        contentLayout.addView(infoFooter)

        // Bottom Apply CTA (Real Telegram ButtonWithCounterView)
        val applyContainer = FrameLayout(this).apply {
            setBackgroundColor(cardBg)
            setPadding(dp(16f), dp(12f), dp(16f), dp(16f))
        }
        sectionCards.add(applyContainer)
        btnApply = ButtonWithCounterView(this, null).apply {
            setText("Apply Theme", false)
            setOnClickListener {
                isApplied = true
                TelegramThemeEngine.setFontSize(this@ThemeActivity, currentFontSize)
                TelegramThemeEngine.setBubbleRadius(this@ThemeActivity, currentBubbleRadius)
                TelegramThemeEngine.applyChanges(this@ThemeActivity)
                AppBulletin.show(this@ThemeActivity, "Theme applied")
                finish()
            }
        }
        applyContainer.addView(
            btnApply,
            FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, dp(48f))
        )
        mainLayout.addView(applyContainer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                TelegramThemeEngine.cancelPreview(this@ThemeActivity)
                finish()
            }
        })

        updateAutoNightLabel()
        refreshAllPreviewViews()
    }

    private fun refreshAllPreviewViews() {
        Theme.syncFromEvePalette(this)
        val palette = TelegramThemeEngine.resolveCurrentPalette(this)
        rootContainer.setBackgroundColor(palette.windowBackground)
        for (v in sectionCards) {
            v.setBackgroundColor(palette.surface)
        }
        actionBar.setBackgroundColor(Theme.getColor(Theme.key_actionBarDefault))
        actionBar.setTitleColor(Theme.getColor(Theme.key_actionBarDefaultTitle))
        actionBar.setItemsColor(Theme.getColor(Theme.key_actionBarDefaultIcon), false)
        rebuildThemesList()
        rebuildAccentsList()
        updatePreviewCard()
    }

    private fun showAutoNightDialog() {
        val options = arrayOf("Disabled", "Scheduled (22:00 – 07:00)")
        val current = TelegramThemeEngine.getAutoNightSchedule(this)
        AlertDialog.Builder(this)
            .setTitle("Auto-Night Mode")
            .setItems(options) { dialog, which ->
                TelegramThemeEngine.setAutoNightSchedule(this, which)
                updateAutoNightLabel()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateAutoNightLabel() {
        val schedule = TelegramThemeEngine.getAutoNightSchedule(this)
        cellAutoNight.setTextAndValue("Auto-Night Mode", if (schedule == 1) "Scheduled" else "Disabled", true)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("preview_day_theme", TelegramThemeEngine.getDayThemeId())
        outState.putInt("preview_day_accent", TelegramThemeEngine.getDayAccentId())
        outState.putString("preview_night_theme", TelegramThemeEngine.getNightThemeId())
        outState.putInt("preview_night_accent", TelegramThemeEngine.getNightAccentId())
        outState.putString("preview_mode", TelegramThemeEngine.getThemeMode().name)
        outState.putInt("preview_font_size", currentFontSize)
        outState.putInt("preview_bubble_radius", currentBubbleRadius)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing && !isApplied) {
            TelegramThemeEngine.cancelPreview(this)
        }
    }

    override fun onApplyThemePalette(palette: TelegramThemeEngine.ResolvedPalette) {
        super.onApplyThemePalette(palette)
        if (::rootContainer.isInitialized) {
            refreshAllPreviewViews()
        }
    }

    private fun rebuildThemesList() {
        val palette = TelegramThemeEngine.resolveCurrentPalette(this)
        layoutThemesContainer.removeAllViews()
        val isDark = TelegramThemeEngine.isDarkActive(this)
        val activeThemeId = if (isDark) TelegramThemeEngine.getNightThemeId() else TelegramThemeEngine.getDayThemeId()
        val filteredThemes = TelegramThemeEngine.getAllThemes().filter { it.isDark == isDark }

        val density = resources.displayMetrics.density
        for (theme in filteredThemes) {
            val isSelected = theme.id == activeThemeId
            val card = DesignCardView(this).apply {
                radius = 16 * density
                strokeWidth = if (isSelected) (2 * density).toInt() else 0
                strokeColor = if (isSelected) theme.baseAccentColor else Color.TRANSPARENT
                setCardBackgroundColor(palette.surface)
                cardElevation = 3 * density

                layoutParams = LinearLayout.LayoutParams((96 * density).toInt(), (104 * density).toInt()).apply {
                    marginEnd = (10 * density).toInt()
                }

                val content = LinearLayout(this@ThemeActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    setPadding((8 * density).toInt(), (8 * density).toInt(), (8 * density).toInt(), (8 * density).toInt())

                    val circle = View(this@ThemeActivity).apply {
                        val size = (36 * density).toInt()
                        layoutParams = LinearLayout.LayoutParams(size, size).apply {
                            bottomMargin = (8 * density).toInt()
                        }
                        background = GradientDrawable().apply {
                            shape = GradientDrawable.OVAL
                            setColor(theme.baseAccentColor)
                        }
                    }
                    addView(circle)

                    val label = TextView(this@ThemeActivity).apply {
                        text = theme.name
                        gravity = Gravity.CENTER
                        setTextColor(palette.textPrimary)
                        textSize = 12f
                        typeface = if (isSelected) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                    }
                    addView(label)
                }
                addView(content)

                setOnClickListener {
                    if (isDark) {
                        TelegramThemeEngine.setPreviewNightTheme(theme.id, theme.defaultAccentId, this@ThemeActivity)
                    } else {
                        TelegramThemeEngine.setPreviewDayTheme(theme.id, theme.defaultAccentId, this@ThemeActivity)
                    }
                    refreshAllPreviewViews()
                }
            }
            layoutThemesContainer.addView(card)
        }
    }

    private fun rebuildAccentsList() {
        val palette = TelegramThemeEngine.resolveCurrentPalette(this)
        layoutAccentsContainer.removeAllViews()
        val isDark = TelegramThemeEngine.isDarkActive(this)
        val activeThemeId = if (isDark) TelegramThemeEngine.getNightThemeId() else TelegramThemeEngine.getDayThemeId()
        val activeAccentId = if (isDark) TelegramThemeEngine.getNightAccentId() else TelegramThemeEngine.getDayAccentId()
        val theme = TelegramThemeEngine.getTheme(activeThemeId)

        val density = resources.displayMetrics.density
        for (accent in theme.accentOptions) {
            val isSelected = accent.id == activeAccentId
            val touchSize = (48 * density).toInt()
            val visualSize = (40 * density).toInt()

            val touchContainer = FrameLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(touchSize, touchSize).apply {
                    marginEnd = (4 * density).toInt()
                    topMargin = (2 * density).toInt()
                    bottomMargin = (2 * density).toInt()
                }
                contentDescription = accent.name
                setOnClickListener {
                    if (isDark) {
                        TelegramThemeEngine.setPreviewNightTheme(theme.id, accent.id, this@ThemeActivity)
                    } else {
                        TelegramThemeEngine.setPreviewDayTheme(theme.id, accent.id, this@ThemeActivity)
                    }
                    refreshAllPreviewViews()
                }
            }

            val swatch = View(this).apply {
                layoutParams = FrameLayout.LayoutParams(visualSize, visualSize, Gravity.CENTER)
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(accent.color)
                    if (isSelected) {
                        setStroke((2 * density).toInt(), palette.textPrimary)
                    }
                }
                isClickable = false
            }
            touchContainer.addView(swatch)
            if (isSelected) {
                val check = CheckBox2(this, 22).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        (22 * density).toInt(),
                        (22 * density).toInt(),
                        Gravity.CENTER
                    )
                    setChecked(true, false)
                    isClickable = false
                    isFocusable = false
                }
                touchContainer.addView(check)
            }
            layoutAccentsContainer.addView(touchContainer)
        }
    }

    private fun updatePreviewCard() {
        val palette = TelegramThemeEngine.resolveCurrentPalette(this)
        val density = resources.displayMetrics.density
        cardPreview.setCardBackgroundColor(palette.surface)
        cardPreview.radius = currentBubbleRadius * density
        layoutPreviewContent.setBackgroundColor(palette.surface)
        tvPreviewTitle.setTextColor(palette.textPrimary)
        tvPreviewTitle.textSize = currentFontSize.toFloat()
        tvPreviewSubtitle.setTextColor(palette.textSecondary)
        tvPreviewSubtitle.textSize = (currentFontSize - 2).coerceAtLeast(12).toFloat()
        tvPreviewTag.setTextColor(palette.textSecondary)
        btnPreviewAction.setTextColor(palette.onAccent)
        btnPreviewAction.background = GradientDrawable().apply {
            cornerRadius = (8 * density)
            setColor(palette.accent)
        }
    }

    private fun handleThemeImport(uri: Uri) {
        try {
            contentResolver.openInputStream(uri)?.use { stream ->
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

                refreshAllPreviewViews()
                AppBulletin.show(this, "Theme imported successfully")
            }
        } catch (e: Exception) {
            AppBulletin.showError(this, "Failed to import theme: ${e.message}")
        }
    }
}
