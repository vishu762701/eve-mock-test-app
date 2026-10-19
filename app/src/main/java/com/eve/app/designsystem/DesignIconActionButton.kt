package com.eve.app.designsystem

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.TooltipCompat
import com.google.android.material.button.MaterialButton

/** Native action semantics and dynamic labels, rendered as an icon-only navigation action. */
class DesignIconActionButton @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    MaterialButton(context, attrs) {
    private var ready = false
    init {
        ready = true
        setText(text, BufferType.NORMAL)
    }
    override fun setText(text: CharSequence?, type: BufferType?) {
        if (!ready) { super.setText(text, type); return }
        if (!text.isNullOrBlank()) {
            contentDescription = text
            TooltipCompat.setTooltipText(this, text)
        }
        super.setText("", type ?: BufferType.NORMAL)
    }
}
