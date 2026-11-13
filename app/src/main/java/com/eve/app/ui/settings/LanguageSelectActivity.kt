/*
 * Derived from Telegram Android LanguageSelectActivity.java.
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
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.eve.app.R
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
 * Uses F1 TelegramActionBar, F2 TelegramRadioIndicator, and F5 HeaderCell/ShadowSectionCell/TextInfoPrivacyCell.
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

    private val rowCells = mutableListOf<LanguageCell>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val palette = TelegramThemeEngine.resolveCurrentPalette(this)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(palette.windowBackground)
        }

        val actionBar = TelegramActionBar(this).apply {
            setTitle("Language")
            onBackPressed = { EveNavigationHelper.finishWithTransition(this@LanguageSelectActivity) }
        }
        root.addView(actionBar, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ))

        val listColumn = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(palette.cardSurface)
        }

        val headerCell = HeaderCell(this).apply {
            setText("App Language")
        }
        listColumn.addView(headerCell, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ))

        val currentTags = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        val selectedTag = when {
            currentTags.isEmpty() -> ""
            currentTags.startsWith("hi") -> "hi"
            else -> "en"
        }

        options.forEachIndexed { index, option ->
            val cell = LanguageCell(this, palette).apply {
                setLanguage(option.nativeTitle, option.englishSubtitle, option.tag == selectedTag)
                needDivider = index < options.lastIndex
                setOnClickListener {
                    val locales = if (option.tag.isEmpty()) {
                        LocaleListCompat.getEmptyLocaleList()
                    } else {
                        LocaleListCompat.forLanguageTags(option.tag)
                    }
                    rowCells.forEachIndexed { i, c ->
                        c.setChecked(i == index, animated = true)
                    }
                    AppCompatDelegate.setApplicationLocales(locales)
                    EveNavigationHelper.finishWithTransition(this@LanguageSelectActivity)
                }
            }
            rowCells.add(cell)
            listColumn.addView(cell, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ))
        }

        val shadowCell = ShadowSectionCell(this).apply {
            updatePalette(palette)
        }
        listColumn.addView(shadowCell, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ))

        val infoCell = TextInfoPrivacyCell(this).apply {
            setText("Choose your preferred language for Eve's interface.")
            setBackgroundColor(palette.windowBackground)
        }
        listColumn.addView(infoCell, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ))

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

    /** LanguageCell (TextRadioCell / LanguageCell.java): 54dp height, native title + subtitle + TelegramRadioIndicator. */
    private class LanguageCell(
        context: Context,
        palette: TelegramThemeEngine.ResolvedPalette
    ) : FrameLayout(context) {

        private val density = resources.displayMetrics.density
        private fun dp(v: Float): Int = (v * density + 0.5f).toInt()

        private val titleView = TextView(context).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTextColor(palette.textPrimary)
            setLines(1)
            isSingleLine = true
            ellipsize = TextUtils.TruncateAt.END
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
        }

        private val subtitleView = TextView(context).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setTextColor(palette.textSecondary)
            setLines(1)
            isSingleLine = true
            ellipsize = TextUtils.TruncateAt.END
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
        }

        private val radioIndicator = TelegramRadioIndicator(
            density,
            dp(22f)
        ) { true }.apply {
            setColors(palette.textSecondary, palette.accent)
        }

        private val radioView = ImageView(context).apply {
            setImageDrawable(radioIndicator)
            scaleType = ImageView.ScaleType.CENTER
        }

        private val dividerPaint = Paint().apply {
            strokeWidth = 1f
            color = palette.divider
        }

        var needDivider: Boolean = true
            set(value) {
                field = value
                setWillNotDraw(!value)
                invalidate()
            }

        init {
            background = TelegramThemeSelectors.getSelectorDrawable(false, palette)
            isClickable = true
            isFocusable = true
            setWillNotDraw(false)

            val textCol = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                addView(titleView, LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ))
                addView(subtitleView, LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(2f) })
            }

            addView(textCol, LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.START or Gravity.CENTER_VERTICAL
            ).apply {
                marginStart = dp(21f)
                marginEnd = dp(64f)
            })

            addView(radioView, LayoutParams(
                dp(24f),
                dp(24f),
                Gravity.END or Gravity.CENTER_VERTICAL
            ).apply {
                marginEnd = dp(21f)
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
                MeasureSpec.makeMeasureSpec(dp(54f) + if (needDivider) 1 else 0, MeasureSpec.EXACTLY)
            )
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            if (needDivider) {
                canvas.drawLine(
                    dp(21f).toFloat(),
                    (height - 1).toFloat(),
                    width.toFloat(),
                    (height - 1).toFloat(),
                    dividerPaint
                )
            }
        }
    }
}
