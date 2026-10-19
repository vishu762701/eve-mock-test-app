package com.eve.app.designsystem

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.widget.FrameLayout
import com.eve.app.R
import com.google.android.material.progressindicator.CircularProgressIndicator

/** Shared loading state. The native indicator follows visibility/lifecycle and system animation scale. */
class DesignLoadingView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : FrameLayout(context, attrs) {
    init {
        val size = resources.getDimensionPixelSize(R.dimen.design_ring_size)
        addView(CircularProgressIndicator(context).apply { isIndeterminate = true }, LayoutParams(size, size, Gravity.CENTER))
    }
}
