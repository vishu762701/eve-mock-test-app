/*
 * Telegram: TMessagesProj/src/main/java/org/telegram/ui/Components/ProgressButton.java:16-102 @ 21f7d109da6ab3506c9e9577f98c6a38771ab335
 * Telegram: TMessagesProj/src/main/java/org/telegram/ui/Components/ButtonBounce.java:15-110 @ 21f7d109da6ab3506c9e9577f98c6a38771ab335
 * Copyright Nikolai Kudashov, 2013-2018; GNU GPL v2 or later. See THIRD_PARTY_NOTICES.md.
 */

package com.eve.app.designsystem

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.annotation.DrawableRes
import androidx.appcompat.widget.AppCompatButton

/**
 * 1:1 Telegram button extending AppCompatButton with official upstream ButtonBounce physics,
 * rounded ripple selector, and progress loading state.
 * Source reference: org.telegram.ui.Components.ProgressButton & ButtonBounce
 */
class TelegramButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.appcompat.R.attr.buttonStyle
) : AppCompatButton(context, attrs, defStyleAttr) {

    private val buttonBounce = ButtonBounce(this)
    private var isBusy = false
    private var idleText: CharSequence? = null

    var rippleColor: ColorStateList? = null
    var strokeColor: ColorStateList? = null
    var strokeWidth: Int = 0
    var cornerRadius: Int = 0
    var iconGravity: Int = 0
    var iconPadding: Int = 0
    var iconTint: ColorStateList? = null
    var iconTintMode: android.graphics.PorterDuff.Mode? = null

    var icon: Drawable?
        get() = compoundDrawablesRelative[0] ?: compoundDrawables[0]
        set(value) {
            setCompoundDrawablesWithIntrinsicBounds(value, null, null, null)
        }

    fun setIconResource(@DrawableRes resId: Int) {
        if (resId != 0) {
            setCompoundDrawablesWithIntrinsicBounds(resId, 0, 0, 0)
        } else {
            setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
        }
    }

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
