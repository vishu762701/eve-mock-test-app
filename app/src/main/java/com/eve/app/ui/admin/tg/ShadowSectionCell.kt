/*
 * Derived from Telegram Android ShadowSectionCell.java, Copyright Nikolai Kudashov, 2013-2018.
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
 * Official Telegram ShadowSectionCell ported from org/telegram/ui/Cells/ShadowSectionCell.java.
 * - Default height: 12dp
 * - Background color: windowBackgroundGray
 * - Top/bottom 1px shadow hairlines matching greydivider
 */
class ShadowSectionCell @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density
    private fun dp(v: Float): Int = (v * density).toInt()

    private var heightDp: Int = 12
    private var topShadow: Boolean = true
    private var bottomShadow: Boolean = true
    private val shadowPaint = Paint().apply { strokeWidth = 1f }

    init {
        updatePalette(TelegramThemeEngine.resolveCurrentPalette(context))
    }

    fun setHeightDp(dpValue: Int) {
        heightDp = dpValue
        requestLayout()
    }

    fun setTopBottom(top: Boolean, bottom: Boolean) {
        topShadow = top
        bottomShadow = bottom
        invalidate()
    }

    fun updatePalette(palette: TelegramThemeEngine.ResolvedPalette) {
        setBackgroundColor(palette.windowBackground)
        shadowPaint.color = palette.hairline
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(width, dp(heightDp.toFloat()))
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (topShadow) {
            canvas.drawLine(0f, 0.5f, width.toFloat(), 0.5f, shadowPaint)
        }
        if (bottomShadow && height > 1) {
            val y = height - 0.5f
            canvas.drawLine(0f, y, width.toFloat(), y, shadowPaint)
        }
    }
}
