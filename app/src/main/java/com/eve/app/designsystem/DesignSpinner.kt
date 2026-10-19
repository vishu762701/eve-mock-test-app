package com.eve.app.designsystem

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatSpinner

/** Native spinner selection/adapter semantics with shared popup chrome and hit region. */
class DesignSpinner @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : AppCompatSpinner(context, attrs) {
    override fun performClick(): Boolean {
        var handled = false
        DesignPopupGlass.around(this) { handled = super.performClick() }
        return handled
    }
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        TouchTargets.ensure(this)
    }
}
