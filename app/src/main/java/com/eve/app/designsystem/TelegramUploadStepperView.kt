/*
 * Telegram: TMessagesProj/src/main/java/org/telegram/ui/Components/TimerDrawable.java:25-150 @ 3de37c8cfb03
 * Telegram: TMessagesProj/src/main/java/org/telegram/ui/Components/RadialProgressView.java:30-180 @ 3de37c8cfb03
 * Derived from Telegram Android PasscodeView.java and SlideView.java
 * DrKLO/Telegram@21f7d109da6ab3506c9e9577f98c6a38771ab335
 * Copyright Nikolai Kudashov, 2013-2018; GNU GPL v2 or later. See THIRD_PARTY_NOTICES.md.
 */

package com.eve.app.designsystem

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import com.eve.app.R
import com.eve.app.theme.TelegramThemeEngine
import com.eve.app.util.FontManager

/**
 * 1:1 Telegram upload stepper view.
 * Displays sequential progress markers ("Exam", "PDF", "Upload") with Telegram theme palette
 * and accessible state messages.
 */
open class TelegramUploadStepperView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private val density = resources.displayMetrics.density
    private fun dp(v: Float): Int = (v * density + 0.5f).toInt()

    private val labels = arrayOf("Exam", "PDF", "Upload")
    private val row: LinearLayout
    private val markers = mutableListOf<TextView>()
    private val stageLabels = mutableListOf<TextView>()
    private val connectors = mutableListOf<View>()
    private val caption: TextView

    private var completed = -1
    private var error = false
    private var busy = false

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER
        clipChildren = false
        clipToPadding = false

        ViewCompat.setAccessibilityLiveRegion(this, ViewCompat.ACCESSIBILITY_LIVE_REGION_POLITE)
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
        isFocusable = true

        val palette = TelegramThemeEngine.resolveCurrentPalette(context)

        row = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        addView(row, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))

        caption = TextView(context).apply {
            gravity = Gravity.CENTER
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTextColor(palette.textSecondary)
            typeface = FontManager.regularTypeface(context)
        }
        addView(caption, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(6f)
        })

        repeat(3) { index ->
            if (index > 0) {
                val connector = View(context).apply {
                    layoutParams = LinearLayout.LayoutParams(0, dp(2f), 1f).apply {
                        marginStart = dp(8f)
                        marginEnd = dp(8f)
                    }
                    setBackgroundColor(ContextCompat.getColor(context, R.color.hairline))
                }
                connectors.add(connector)
                row.addView(connector)
            }

            val stepContainer = LinearLayout(context).apply {
                orientation = VERTICAL
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
            }

            val marker = TextView(context).apply {
                gravity = Gravity.CENTER
                text = (index + 1).toString()
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                typeface = FontManager.telegramMediumTypeface(context)
                val size = dp(28f)
                layoutParams = LayoutParams(size, size)
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(palette.surfaceInset)
                }
                setTextColor(palette.textSecondary)
            }
            markers.add(marker)
            stepContainer.addView(marker)

            val label = TextView(context).apply {
                text = labels[index]
                gravity = Gravity.CENTER
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                typeface = FontManager.regularTypeface(context)
                setTextColor(palette.textSecondary)
                layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                    topMargin = dp(4f)
                }
            }
            stageLabels.add(label)
            stepContainer.addView(label)

            row.addView(stepContainer)
        }

        update(examReady = false, pdfReady = false, uploading = false, uploaded = false, failed = false)
    }

    fun update(
        examReady: Boolean,
        pdfReady: Boolean,
        uploading: Boolean,
        uploaded: Boolean,
        failed: Boolean
    ) {
        val state = UploadStepState.from(examReady, pdfReady, uploading, uploaded, failed)
        val progress = state.progress
        completed = progress
        error = state.failed
        busy = state.uploading

        val palette = TelegramThemeEngine.resolveCurrentPalette(context)
        val onAccent = ContextCompat.getColor(context, R.color.on_accent)

        for (i in 0..2) {
            val marker = markers[i]
            val label = stageLabels[i]
            val isDone = i < progress && (i < 2 || state.uploaded)
            val isCurrent = i == progress.coerceAtMost(2) && !state.uploaded
            val failedStage = state.failed && i == 2

            val bg = marker.background as? GradientDrawable ?: GradientDrawable().apply {
                shape = GradientDrawable.OVAL
            }

            if (isDone) {
                bg.setColor(palette.accent)
                marker.setTextColor(onAccent)
                marker.text = "✓"
                label.setTextColor(palette.textPrimary)
            } else if (failedStage) {
                bg.setColor(ContextCompat.getColor(context, R.color.system_red))
                marker.setTextColor(onAccent)
                marker.text = "!"
                label.setTextColor(palette.textPrimary)
            } else if (isCurrent) {
                bg.setColor(palette.surfaceInset)
                marker.setTextColor(palette.accent)
                marker.text = (i + 1).toString()
                label.setTextColor(palette.textPrimary)
            } else {
                bg.setColor(palette.surfaceInset)
                marker.setTextColor(palette.textSecondary)
                marker.text = (i + 1).toString()
                label.setTextColor(palette.textSecondary)
            }
            marker.background = bg
        }

        for (i in connectors.indices) {
            val connector = connectors[i]
            val active = i < progress - 1
            connector.setBackgroundColor(
                if (active) palette.accent
                else ContextCompat.getColor(context, R.color.hairline)
            )
        }

        caption.text = state.message
        contentDescription = state.message
    }

    fun update(examReady: Boolean, pdfReady: Boolean, uploaded: Boolean) =
        update(examReady, pdfReady, uploading = false, uploaded = uploaded, failed = false)
}
