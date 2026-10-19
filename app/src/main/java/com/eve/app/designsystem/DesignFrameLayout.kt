package com.eve.app.designsystem

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout

/** Preserve the native widget; expand small interactive regions after layout. */
class DesignFrameLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (isClickable) TouchTargets.ensure(this)
        if (elevation > 0f) ShadowHost.releaseClipping(this)
    }
    override fun setOnClickListener(listener: View.OnClickListener?) {
        super.setOnClickListener(listener)
        if (listener != null && isAttachedToWindow) TouchTargets.ensure(this)
    }
}
