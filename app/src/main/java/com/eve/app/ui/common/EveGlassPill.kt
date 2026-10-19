package com.eve.app.ui.common

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import com.eve.app.R
import com.eve.app.util.EveTouchHelper

/**
 * Eve capsule label and shared capsule-background helper.
 * Features:
 * - Full pill rounded geometry (999dp)
 * - Opaque content surface; glass belongs to the surrounding chrome
 * - Tactile compression (0.98) with haptic clock tick
 */
class EveGlassPill @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    companion object {
        /** Retain the supplied semantic fill/stroke while giving a bound badge its capsule shape. */
        fun setCapsuleBackground(view: android.view.View, @androidx.annotation.DrawableRes resource: Int) {
            view.setBackgroundResource(resource)
            val background = view.background.mutate() as android.graphics.drawable.GradientDrawable
            background.cornerRadius = view.resources.getDimension(R.dimen.design_input_height)
            view.background = background
        }
    }

    init {
        setBackgroundResource(R.drawable.bg_soft_pill)
        gravity = Gravity.CENTER
        val density = resources.displayMetrics.density
        val padH = (12f * density).toInt()
        val padV = (6f * density).toInt()
        setPadding(padH, padV, padH, padV)
        setTextColor(ContextCompat.getColor(context, R.color.text_primary))
        androidx.core.widget.TextViewCompat.setTextAppearance(this, R.style.TextAppearance_Eve_Token_Secondary)
        EveTouchHelper.attachTactileFeedback(this, pressScale = 0.97f)
    }
}
