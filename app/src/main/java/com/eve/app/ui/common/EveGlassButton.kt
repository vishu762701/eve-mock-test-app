package com.eve.app.ui.common

import android.content.Context
import android.util.AttributeSet
import com.google.android.material.button.MaterialButton
import com.eve.app.R
import com.eve.app.util.EveTouchHelper

/**
 * Reusable Apple iOS 26 Liquid Glass Button.
 * Features:
 * - Pill shape geometry
 * - Tactile compression (0.982) with clock-tick haptics
 */
class EveGlassButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = com.google.android.material.R.attr.materialButtonStyle
) : MaterialButton(context, attrs, defStyleAttr) {

    init {
        cornerRadius = resources.getDimensionPixelSize(R.dimen.eve_radius_pill)
        insetTop = 0
        insetBottom = 0
        EveTouchHelper.attachTactileFeedback(this, pressScale = 0.982f)
    }
}
