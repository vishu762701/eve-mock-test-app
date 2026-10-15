package com.eve.app.ui.common

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import eightbitlab.com.blurview.BlurView
import androidx.core.content.ContextCompat
import com.eve.app.R

/**
 * Reusable Apple iOS 26 Liquid Glass Toolbar.
 * Provides translucent glass surface with optical hairline bottom separator border.
 */
class EveGlassToolbar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BlurView(context, attrs, defStyleAttr) {

    private val separatorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = resources.displayMetrics.density * 0.5f
    }

    init {
        setWillNotDraw(false)
        outlineProvider = object : android.view.ViewOutlineProvider() {
            override fun getOutline(view: android.view.View, outline: android.graphics.Outline) {
                outline.setRoundRect(0, 0, view.width, view.height,
                    com.eve.app.util.EveShapes.capsule(view.height.toFloat()))
            }
        }
        clipToOutline = true
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        val backdrop = rootView as? android.view.ViewGroup ?: return
        setBackgroundColor(android.graphics.Color.TRANSPARENT)
        com.eve.app.util.EveBlurHelper.setupBlurView(this, backdrop,
            radiusDp = resources.getDimension(R.dimen.eve_thin_material_blur_radius) / resources.displayMetrics.density,
            overlayColor = ContextCompat.getColor(context, R.color.eve_material_thin))
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        separatorPaint.color = ContextCompat.getColor(context, R.color.eve_separator)
        val y = height - separatorPaint.strokeWidth / 2f
        canvas.drawLine(0f, y, width.toFloat(), y, separatorPaint)
    }
}
