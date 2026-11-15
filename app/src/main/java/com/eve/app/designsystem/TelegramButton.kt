package com.eve.app.designsystem

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import com.google.android.material.button.MaterialButton

/**
 * 1:1 Telegram button with official upstream ButtonBounce physics.
 * Source reference: org.telegram.ui.Components.ButtonBounce
 */
class TelegramButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = com.google.android.material.R.attr.materialButtonStyle
) : MaterialButton(context, attrs, defStyleAttr) {

    private val buttonBounce = ButtonBounce(this)
    private var isBusy = false
    private var idleText: CharSequence? = null

    var isLoading: Boolean
        get() = isBusy
        set(value) = setLoading(value)

    init {
        stateListAnimator = null
    }

    fun setLoading(loading: Boolean, pendingLabel: String = "Sending…") {
        if (isBusy == loading) return
        isBusy = loading
        isEnabled = !loading
        if (loading) {
            idleText = text
            text = pendingLabel
        } else {
            text = idleText ?: text
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> buttonBounce.setPressed(true)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> buttonBounce.setPressed(false)
        }
        return super.onTouchEvent(event)
    }
}
