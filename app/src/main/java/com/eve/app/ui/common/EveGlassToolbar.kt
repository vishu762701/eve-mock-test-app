package com.eve.app.ui.common

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.widget.FrameLayout

/**
 * Transparent, non-blurring layout container for top navigation headers.
 * Keeps screen titles and actions directly over the screen canvas in accordance
 * with Apple iOS 26 navigation bar principles, without any continuous capsule clipping,
 * blur backdrop, overlay tint, outline, or decorative separator line.
 */
class EveGlassToolbar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    init {
        setBackgroundColor(Color.TRANSPARENT)
    }
}
