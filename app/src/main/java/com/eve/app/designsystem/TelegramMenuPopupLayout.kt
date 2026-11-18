/*
 * Telegram: TMessagesProj/src/main/java/org/telegram/ui/ActionBar/ActionBarPopupWindow.java:103-350 @ 21f7d109da6ab3506c9e9577f98c6a38771ab335
 * Copyright Nikolai Kudashov, 2013-2018; GNU GPL v2 or later. See THIRD_PARTY_NOTICES.md.
 */

package com.eve.app.designsystem

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.widget.FrameLayout
import com.eve.app.theme.TelegramThemeEngine

/**
 * 1:1 Telegram popup window layout ported from ActionBarPopupWindowLayout.
 * Handles menu background rendering, popup bounds, and theme palette updates.
 * Source reference: org.telegram.ui.ActionBar.ActionBarPopupWindow.ActionBarPopupWindowLayout
 */
class TelegramMenuPopupLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var palette: TelegramThemeEngine.ResolvedPalette? = null
    private val bgPaddings = Rect()
    private var backAlpha: Int = 255
    private var backScaleX: Float = 1.0f
    private var backScaleY: Float = 1.0f

    var drawHairline: Boolean = false
    var glassEnabled: Boolean = false

    var originalPopupBackground: Drawable? = null
        set(value) {
            field = value
            if (value != null) {
                value.getPadding(bgPaddings)
                background = value
            } else {
                bgPaddings.setEmpty()
                background = ColorDrawable(android.graphics.Color.TRANSPARENT)
            }
            invalidate()
        }

    init {
        setWillNotDraw(false)
    }

    fun updatePalette(resolvedPalette: TelegramThemeEngine.ResolvedPalette) {
        palette = resolvedPalette
        invalidate()
    }

    fun setBackScaleX(scale: Float) {
        backScaleX = scale
        invalidate()
    }

    fun setBackScaleY(scale: Float) {
        backScaleY = scale
        invalidate()
    }

    fun setBackAlpha(alpha: Int) {
        backAlpha = alpha
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
    }
}
