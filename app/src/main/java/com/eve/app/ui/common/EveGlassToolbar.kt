package com.eve.app.ui.common

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import com.eve.app.R
import com.eve.app.util.ThemeSwitchAnimator

/**
 * Reusable Apple iOS 26 Liquid Glass Toolbar.
 * Provides translucent glass surface with optical hairline bottom separator border.
 */
class EveGlassToolbar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val separatorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = resources.displayMetrics.density * 0.5f
    }

    init {
        setWillNotDraw(false)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        separatorPaint.color = ContextCompat.getColor(context, R.color.eve_separator)
        val y = height - separatorPaint.strokeWidth / 2f
        canvas.drawLine(0f, y, width.toFloat(), y, separatorPaint)
    }
}
