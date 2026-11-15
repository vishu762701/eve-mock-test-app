/*
 * Telegram: TMessagesProj/src/main/java/org/telegram/ui/ActionBar/ActionBarMenuItem.java:400-550 @ 3de37c8cfb03
 * Telegram: TMessagesProj/src/main/java/org/telegram/ui/Components/PopupSwipeBackLayout.java:30-140 @ 3de37c8cfb03
 * Copyright Nikolai Kudashov, 2013-2018. GNU GPL v2 or later. See THIRD_PARTY_NOTICES.md.
 */

package com.eve.app.designsystem

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.AdapterView
import androidx.appcompat.widget.AppCompatSpinner
import com.eve.app.theme.TelegramThemeEngine
import org.telegram.ui.Components.CubicBezierInterpolator

/**
 * 1:1 Telegram spinner selector with authentic Telegram theme popup background,
 * animated arrow indicator, and ButtonBounce physics.
 */
class TelegramSpinner @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.appcompat.R.attr.spinnerStyle
) : AppCompatSpinner(context, attrs, defStyleAttr) {

    private val bounce = ButtonBounce(this)
    private var externalListener: OnItemSelectedListener? = null
    private val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = resources.displayMetrics.density * 1.5f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val arrowPath = Path()
    private var arrowRotation = 0f
    private var arrowAnimator: ValueAnimator? = null
    private var isPopupShowing = false

    init {
        val palette = TelegramThemeEngine.resolveCurrentPalette(context)
        arrowPaint.color = palette.textSecondary
        setPopupBackgroundDrawable(TelegramThemeSelectors.getRoundRectSelectorDrawable(
            resources.displayMetrics.density * 10f,
            palette.dialogSurface
        ))

        super.setOnItemSelectedListener(object : OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                bounce.bounce()
                externalListener?.onItemSelected(parent, view, position, id)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
                externalListener?.onNothingSelected(parent)
            }
        })
    }

    override fun setOnItemSelectedListener(listener: OnItemSelectedListener?) {
        this.externalListener = listener
    }

    override fun performClick(): Boolean {
        setArrowExpanded(true)
        val result = super.performClick()
        postDelayed({ setArrowExpanded(false) }, 400L)
        return result
    }

    private fun setArrowExpanded(expanded: Boolean) {
        if (isPopupShowing == expanded) return
        isPopupShowing = expanded
        arrowAnimator?.cancel()
        val target = if (expanded) 180f else 0f
        arrowAnimator = ValueAnimator.ofFloat(arrowRotation, target).apply {
            duration = 220L
            interpolator = CubicBezierInterpolator.EASE_OUT_QUINT
            addUpdateListener {
                arrowRotation = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> bounce.setPressed(true)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> bounce.setPressed(false)
        }
        return super.onTouchEvent(event)
    }

    override fun onDraw(canvas: Canvas) {
        val scale = bounce.getScale(0.04f)
        if (scale != 1f) {
            canvas.save()
            canvas.scale(scale, scale, width / 2f, height / 2f)
            super.onDraw(canvas)
            drawArrow(canvas)
            canvas.restore()
        } else {
            super.onDraw(canvas)
            drawArrow(canvas)
        }
    }

    private fun drawArrow(canvas: Canvas) {
        val density = resources.displayMetrics.density
        val arrowSize = density * 4f
        val cx = width - paddingRight - arrowSize
        val cy = height / 2f

        canvas.save()
        canvas.translate(cx, cy)
        canvas.rotate(arrowRotation)
        arrowPath.reset()
        arrowPath.moveTo(-arrowSize, -arrowSize / 2f)
        arrowPath.lineTo(0f, arrowSize / 2f)
        arrowPath.lineTo(arrowSize, -arrowSize / 2f)
        canvas.drawPath(arrowPath, arrowPaint)
        canvas.restore()
    }
}
