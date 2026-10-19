package com.eve.app.designsystem

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.appcompat.widget.AppCompatImageButton

/** Preserve the native widget; expand small interactive regions after layout. */
class DesignImageButton @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = androidx.appcompat.R.attr.imageButtonStyle
) : AppCompatImageButton(context, attrs, defStyleAttr) {
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (isClickable) TouchTargets.ensure(this)
    }
    override fun setOnClickListener(listener: View.OnClickListener?) {
        super.setOnClickListener(listener)
        if (listener != null && isAttachedToWindow) TouchTargets.ensure(this)
    }
}
