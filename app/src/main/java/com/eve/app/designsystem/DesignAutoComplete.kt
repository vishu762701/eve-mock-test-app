package com.eve.app.designsystem

import android.content.Context
import android.util.AttributeSet
import com.google.android.material.textfield.MaterialAutoCompleteTextView

/** Preserve Material's filtering, keyboard and accessibility popup; decorate only its surface. */
class DesignAutoComplete @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : MaterialAutoCompleteTextView(context, attrs) {
    override fun showDropDown() { DesignPopupGlass.around(this) { super.showDropDown() } }
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        DesignType.apply(this, DesignType.Role.BODY)
        TouchTargets.ensure(this)
    }
}
