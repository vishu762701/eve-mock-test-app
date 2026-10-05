package com.eve.app.ui.common

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import com.eve.app.R
import com.eve.app.util.EveTouchHelper

/**
 * Reusable Apple iOS 26 Liquid Glass Pill.
 * Features:
 * - Full pill rounded geometry (999dp)
 * - Translucent glass background with hairline rim
 * - Tactile compression (0.98) with haptic clock tick
 */
class EveGlassPill @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    init {
        setBackgroundResource(R.drawable.bg_soft_pill)
        gravity = Gravity.CENTER
        val density = resources.displayMetrics.density
        val padH = (12f * density).toInt()
        val padV = (6f * density).toInt()
        setPadding(padH, padV, padH, padV)
        setTextColor(ContextCompat.getColor(context, R.color.eve_text))
        textSize = 13f
        typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
        EveTouchHelper.attachTactileFeedback(this, pressScale = 0.97f)
    }
}
