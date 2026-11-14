/*
 * Derived from Telegram Android LanguageSelectActivity.java and LanguageCell.java.
 * Copyright Nikolai Kudashov, 2013-2018. GNU GPL v2 or later.
 * See THIRD_PARTY_NOTICES.md.
 */
package com.eve.app.ui.settings

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Bundle
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.eve.app.designsystem.TelegramThemeSelectors
import com.eve.app.theme.TelegramThemeEngine
import com.eve.app.ui.admin.tg.HeaderCell
import com.eve.app.ui.admin.tg.ShadowSectionCell
import com.eve.app.ui.admin.tg.TelegramActionBar
import com.eve.app.ui.admin.tg.TextInfoPrivacyCell
import com.eve.app.ui.common.EveBaseActivity
import com.eve.app.ui.common.TelegramRadioIndicator
import com.eve.app.util.EveNavigationHelper

/**
 * Telegram LanguageSelectActivity.java port for Eve.
 * Uses F1 TelegramActionBar (with animated search filter), F2 TelegramRadioIndicator,
 * F5 HeaderCell/ShadowSectionCell/TextInfoPrivacyCell, and exact LanguageCell.java:30-80 layout.
 */
class LanguageSelectActivity : EveBaseActivity() {

    private data class LanguageOption(
        val tag: String,
        val nativeTitle: String,
        val englishSubtitle: String
    )

    private val options = listOf(
        LanguageOption("", "System default", "Follow device language"),
        LanguageOption("en", "English", "English"),
        LanguageOption("hi", "हिन्दी", "Hindi")
    )

    private var selectedTag: String = ""
    private lateinit var languagesContainer: LinearLayout
    private lateinit var headerCell: HeaderCell
    private lateinit var shadowCell: ShadowSectionCell
    private lateinit var infoCell: TextInfoPrivacyCell
    private lateinit var emptyView: TextView
    private lateinit var palette: TelegramThemeEngine.ResolvedPalette

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        palette = TelegramThemeEngine.resolveCurrentPalette(this)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(palette.windowBackground)
        }

        val actionBar = TelegramActionBar(this).apply {
            setTitle("Language")
            onBackPressed = { EveNavigationHelper.finishWithTransition(this@LanguageSelectActivity) }
            enableSearch("Search") { query ->
                filterLanguages(query.trim())
            }
        }
        root.addView(actionBar, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ))

        val listColumn = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(palette.surface)
        }

        headerCell = HeaderCell(this).apply {
            setText("App Language")
        }
        listColumn.addView(headerCell, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ))

        languagesContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        listColumn.addView(languagesContainer, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ))

        emptyView = TextView(this).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTextColor(palette.textSecondary)
            gravity = Gravity.CENTER
            text = "No results"
            visibility = View.GONE
            setPadding(0, dp(48f), 0, dp(48f))
        }
        listColumn.addView(emptyView, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ))

        shadowCell = ShadowSectionCell(this)
        listColumn.addView(shadowCell, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ))

        infoCell = TextInfoPrivacyCell(this).apply {
            setText("Choose your preferred language for Eve's interface.")
            setBackgroundColor(palette.windowBackground)
        }
        listColumn.addView(infoCell, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ))

        val currentTags = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        selectedTag = when {
            currentTags.isEmpty() -> ""
            currentTags.startsWith("hi") -> "hi"
            else -> "en"
        }

        renderLanguageList(options)

        val scrollView = ScrollView(this).apply {
            isVerticalScrollBarEnabled = false
            addView(listColumn, FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ))
        }
        root.addView(scrollView, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            0,
            1f
        ))

        setContentView(root)
    }

    private fun dp(v: Float): Int = (v * resources.displayMetrics.density + 0.5f).toInt()

    private fun filterLanguages(query: String) {
        val filtered = if (query.isEmpty()) {
            options
        } else {
            options.filter {
                it.nativeTitle.contains(query, ignoreCase = true) ||
                    it.englishSubtitle.contains(query, ignoreCase = true)
            }
        }

        val isSearching = query.isNotEmpty()
        headerCell.visibility = if (isSearching) View.GONE else View.VISIBLE
        shadowCell.visibility = if (isSearching) View.GONE else View.VISIBLE
        infoCell.visibility = if (isSearching) View.GONE else View.VISIBLE

        if (filtered.isEmpty()) {
            languagesContainer.visibility = View.GONE
            emptyView.visibility = View.VISIBLE
        } else {
            languagesContainer.visibility = View.VISIBLE
            emptyView.visibility = View.GONE
            renderLanguageList(filtered)
        }
    }

    private fun renderLanguageList(items: List<LanguageOption>) {
        languagesContainer.removeAllViews()
        items.forEachIndexed { index, option ->
            val cell = LanguageCell(this, palette).apply {
                setLanguage(option.nativeTitle, option.englishSubtitle, option.tag == selectedTag)
                needDivider = index < items.lastIndex
                setOnClickListener {
                    selectedTag = option.tag
                    setChecked(true, animated = true)
                    val locales = if (option.tag.isEmpty()) {
                        LocaleListCompat.getEmptyLocaleList()
                    } else {
                        LocaleListCompat.forLanguageTags(option.tag)
                    }
                    AppCompatDelegate.setApplicationLocales(locales)
                    languagesContainer.postDelayed({
                        EveNavigationHelper.finishWithTransition(this@LanguageSelectActivity)
                    }, 150L)
                }
            }
            languagesContainer.addView(cell, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ))
        }
    }

    /**
     * Exact Telegram Cells/LanguageCell.java port:
     * - 60dp height (dp(60) + if (needDivider) 1 else 0)
     * - RadioButton at start (22x22dp, size 20dp, marginStart 20dp)
     * - Native title: 16sp, marginStart 62dp, marginEnd 23dp
     * - Subtitle: 13sp, marginStart 62dp, marginEnd 23dp
     * - Divider: drawn from marginStartDp - 3 = 59dp to measuredWidth
     */
    private class LanguageCell(
        context: Context,
        palette: TelegramThemeEngine.ResolvedPalette
    ) : FrameLayout(context) {

        private val density = resources.displayMetrics.density
        private fun dp(v: Float): Int = (v * density + 0.5f).toInt()

        private val marginStartDp = 62
        private val marginEndDp = 23

        private val radioIndicator = TelegramRadioIndicator(
            density,
            dp(20f)
        ) { true }.apply {
            setColors(palette.textSecondary, palette.accent)
        }

        private val radioView = ImageView(context).apply {
            setImageDrawable(radioIndicator)
            scaleType = ImageView.ScaleType.CENTER
        }

        private val titleView = TextView(context).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTextColor(palette.textPrimary)
            isSingleLine = true
            ellipsize = TextUtils.TruncateAt.END
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
        }

        private val subtitleView = TextView(context).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setTextColor(palette.textSecondary)
            isSingleLine = true
            ellipsize = TextUtils.TruncateAt.END
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
        }

        private val dividerPaint = Paint().apply {
            strokeWidth = 1f
            color = palette.hairline
        }

        var needDivider: Boolean = true
            set(value) {
                field = value
                setWillNotDraw(!value)
                invalidate()
            }

        init {
            background = TelegramThemeSelectors.getSelectorDrawable(palette.actionSelector)
            isClickable = true
            isFocusable = true
            setWillNotDraw(false)

            // RadioButton at start (per LanguageCell.java:47)
            addView(radioView, LayoutParams(
                dp(22f),
                dp(22f),
                Gravity.START or Gravity.CENTER_VERTICAL
            ).apply {
                marginStart = dp(20f)
            })

            // Text column (per LanguageCell.java:55, 63)
            val textCol = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                addView(titleView, LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ))
                addView(subtitleView, LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = dp(2f)
                })
            }

            addView(textCol, LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.START or Gravity.CENTER_VERTICAL
            ).apply {
                marginStart = dp(marginStartDp.toFloat())
                marginEnd = dp(marginEndDp.toFloat())
            })
        }

        fun setLanguage(nativeName: String, subtitle: String, checked: Boolean) {
            titleView.text = nativeName
            subtitleView.text = subtitle
            radioIndicator.setChecked(checked, false)
        }

        fun setChecked(checked: Boolean, animated: Boolean) {
            radioIndicator.setChecked(checked, animated && isAttachedToWindow)
        }

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            super.onMeasure(
                MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(dp(60f) + if (needDivider) 1 else 0, MeasureSpec.EXACTLY)
            )
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            if (needDivider) {
                canvas.drawLine(
                    dp((marginStartDp - 3).toFloat()).toFloat(),
                    (height - 1).toFloat(),
                    width.toFloat(),
                    (height - 1).toFloat(),
                    dividerPaint
                )
            }
        }
    }
}
