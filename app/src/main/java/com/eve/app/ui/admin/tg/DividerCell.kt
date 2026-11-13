/*
 * Derived from Telegram Android DividerCell.java, Copyright Nikolai Kudashov, 2013-2018.
 * GNU GPL v2 or later. See THIRD_PARTY_NOTICES.md.
 */

package com.eve.app.ui.admin.tg

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.eve.app.theme.TelegramThemeEngine

/**
 * Official Telegram DividerCell ported from org/telegram/ui/Cells/DividerCell.java.
 * - Top/bottom padding: 8dp each (16dp + 1px total height)
 * - Hairline: 1px stroke at y = 8dp using palette.divider
 */
class DividerCell @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density
    private fun dp(v: Float): Int = (v * density).toInt()

    private val dividerPaint = Paint().apply {
        strokeWidth = 1f
    }

    init {
        setPadding(0, dp(8f), 0, dp(8f))
        updatePalette(TelegramThemeEngine.resolveCurrentPalette(context))
    }

    fun updatePalette(palette: TelegramThemeEngine.ResolvedPalette) {
        dividerPaint.color = palette.divider
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(
            MeasureSpec.getSize(widthMeasureSpec),
            paddingTop + paddingBottom + 1
        )
    }

    override fun onDraw(canvas: Canvas) {
        val y = paddingTop.toFloat()
        canvas.drawLine(paddingLeft.toFloat(), y, (width - paddingRight).toFloat(), y, dividerPaint)
    }
}
