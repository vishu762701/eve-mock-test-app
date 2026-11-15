/*
 * Telegram: TMessagesProj/src/main/java/org/telegram/ui/Components/RoundButton.java:30-120 @ 3de37c8cfb039452be7398909461da6288babf15
 * Telegram: TMessagesProj/src/main/java/org/telegram/ui/Components/ButtonBounce.java:15-110 @ 3de37c8cfb039452be7398909461da6288babf15
 * Copyright Nikolai Kudashov, 2013-2018; GNU GPL v2 or later. See THIRD_PARTY_NOTICES.md.
 */

package com.eve.app.designsystem

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatButton

/**
 * 1:1 Telegram button extending AppCompatButton with official upstream ButtonBounce physics
 * and Telegram rounded ripple selector.
 * Source reference: org.telegram.ui.Components.RoundButton & ButtonBounce
 */
class TelegramButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.appcompat.R.attr.buttonStyle
) : AppCompatButton(context, attrs, defStyleAttr) {

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

    override fun onDraw(canvas: Canvas) {
        val scale = buttonBounce.getScale(0.04f)
        if (scale != 1f) {
            canvas.save()
            canvas.scale(scale, scale, width / 2f, height / 2f)
            super.onDraw(canvas)
            canvas.restore()
        } else {
            super.onDraw(canvas)
        }
    }
}
