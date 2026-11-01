/* Derived from Telegram RecyclerListView.java:3285-3292,3587-3615.
 * Copyright Nikolai Kudashov / Telegram contributors. GNU GPL v2 or later.
 * EVE adapter: a fixed grouped section instead of RecyclerView item decoration.
 * See THIRD_PARTY_NOTICES.md. */
package com.eve.app.ui.profile

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.widget.LinearLayout
import com.eve.app.theme.TelegramThemeEngine

/** Original section geometry and paint pipeline; EVE owns the rows and theme binding. */
class TelegramProfileSection @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {
    private val density = resources.displayMetrics.density
    private val radius = 16f * density
    private val rect = RectF()
    private val clip = Path()
    private val surfacePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    // SharedConfig.java:684 defaults this upstream preference to false.
    var shadowsInSections = false
        set(value) { field = value; invalidate() }
    private val themeListener: (TelegramThemeEngine.ResolvedPalette) -> Unit = { invalidate() }

    init {
        orientation = VERTICAL
        setWillNotDraw(false)
        clipChildren = false
        clipToPadding = false
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        TelegramThemeEngine.addThemeChangeListener(themeListener)
    }

    override fun onDetachedFromWindow() {
        TelegramThemeEngine.removeThemeChangeListener(themeListener)
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        rect.set(0f, 0f, width.toFloat(), height.toFloat())
        val palette = TelegramThemeEngine.resolveCurrentPalette(context)
        // RecyclerListView.drawBackgroundRect: source's optional section shadows,
        // substantially subtler than the former global Material elevated card.
        if (shadowsInSections) {
            strokePaint.setShadowLayer(.33f * density, 0f, 0f, 0x0c000000)
            strokePaint.color = 0
            surfacePaint.setShadowLayer(2f * density, 0f, .33f * density, 0x0a000000)
        } else {
            surfacePaint.setShadowLayer(0f, 0f, 0f, 0)
        }
        surfacePaint.color = palette.surface
        if (shadowsInSections) canvas.drawRoundRect(rect, radius, radius, strokePaint)
        canvas.drawRoundRect(rect, radius, radius, surfacePaint)
    }

    override fun dispatchDraw(canvas: Canvas) {
        rect.set(0f, 0f, width.toFloat(), height.toFloat())
        clip.rewind()
        clip.addRoundRect(rect, radius, radius, Path.Direction.CW)
        val save = canvas.save()
        canvas.clipPath(clip)
        super.dispatchDraw(canvas)
        canvas.restoreToCount(save)
    }
}
