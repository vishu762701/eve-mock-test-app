/*
 * Derived from Telegram Android PasscodeView.java and SlideView.java
 * DrKLO/Telegram@3de37c8cfb039452be7398909461da6288babf15
 * Copyright Nikolai Kudashov, 2013-2018; GNU GPL v2 or later. See THIRD_PARTY_NOTICES.md.
 */

package com.eve.app.designsystem

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import com.eve.app.R
import com.eve.app.telegram.animation.TelegramAnimationRuntime
import com.eve.app.theme.TelegramThemeEngine
import com.eve.app.util.FontManager
import org.telegram.ui.Components.CubicBezierInterpolator

/**
 * 1:1 Telegram upload stepper view.
 * Displays sequential progress markers ("Exam", "PDF", "Upload") with Telegram theme palette
 * and CubicBezierInterpolator physics.
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
                    setColor(palette.fieldBackground)
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

        update(examSelected = false, pdfSelected = false, uploaded = false)
    }

    fun update(
        examSelected: Boolean,
        pdfSelected: Boolean,
        uploaded: Boolean,
        errorOccurred: Boolean = false,
        isBusy: Boolean = false
    ) {
        val newCompleted = when {
            uploaded -> 2
            pdfSelected -> 1
            examSelected -> 0
            else -> -1
        }

        completed = newCompleted
        error = errorOccurred
        busy = isBusy

        val palette = TelegramThemeEngine.resolveCurrentPalette(context)
        val onAccent = ContextCompat.getColor(context, R.color.on_accent)

        for (i in 0..2) {
            val marker = markers[i]
            val label = stageLabels[i]
            val isDone = i <= completed
            val isCurrent = i == completed + 1

            val bg = marker.background as? GradientDrawable ?: GradientDrawable().apply {
                shape = GradientDrawable.OVAL
            }

            if (isDone) {
                bg.setColor(palette.accent)
                marker.setTextColor(onAccent)
                marker.text = "✓"
                label.setTextColor(palette.textPrimary)
            } else if (isCurrent) {
                bg.setColor(if (error) ContextCompat.getColor(context, R.color.error_red) else palette.fieldBackground)
                marker.setTextColor(if (error) onAccent else palette.accent)
                marker.text = (i + 1).toString()
                label.setTextColor(palette.textPrimary)
            } else {
                bg.setColor(palette.fieldBackground)
                marker.setTextColor(palette.textSecondary)
                marker.text = (i + 1).toString()
                label.setTextColor(palette.textSecondary)
            }
            marker.background = bg
        }

        for (i in connectors.indices) {
            val connector = connectors[i]
            val active = i < completed
            connector.setBackgroundColor(
                if (active) palette.accent
                else ContextCompat.getColor(context, R.color.hairline)
            )
        }

        caption.text = when {
            error -> "Upload failed. Please check the file and try again."
            busy -> "Uploading syllabus to server…"
            uploaded -> "Syllabus uploaded successfully!"
            pdfSelected -> "PDF selected. Ready to upload."
            examSelected -> "Exam selected. Please choose a syllabus PDF."
            else -> "Step 1: Choose an exam above."
        }
    }
}
