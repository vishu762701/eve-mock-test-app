/*
 * Telegram: TMessagesProj/src/main/java/org/telegram/ui/Components/AvatarDrawable.java:20-150 @ 3de37c8cfb03
 * Telegram: TMessagesProj/src/main/java/org/telegram/ui/Components/BackupImageView.java:30-180 @ 3de37c8cfb03
 * Copyright Nikolai Kudashov, 2013-2018. GNU GPL v2 or later. See THIRD_PARTY_NOTICES.md.
 */

package com.eve.app.designsystem

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatImageView
import com.eve.app.theme.TelegramThemeEngine
import com.google.android.material.shape.ShapeAppearanceModel

/**
 * 1:1 Telegram avatar image view with authentic circular / squircle canvas clipping,
 * touch bounce animation, stroke ring, and Telegram profile transition physics.
 */
class TelegramAvatarImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    private val clipPath = Path()
    private val rect = RectF()
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val bounce = ButtonBounce(this)

    var strokeWidth: Float = 0f
        set(value) {
            field = value
            strokePaint.strokeWidth = value
            invalidate()
        }

    var strokeColor: Int = 0
        set(value) {
            field = value
            strokePaint.color = value
            invalidate()
        }

    var cornerRadius: Float = 0f
        set(value) {
            field = value
            invalidate()
        }

    var shapeAppearanceModel: ShapeAppearanceModel = ShapeAppearanceModel.builder().build()
        set(value) {
            field = value
            cornerRadius = value.topLeftCornerSize.getCornerSize(rect)
            invalidate()
        }

    init {
        val palette = TelegramThemeEngine.resolveCurrentPalette(context)
        strokePaint.color = palette.hairline
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (isClickable) {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> bounce.setPressed(true)
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> bounce.setPressed(false)
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onDraw(canvas: Canvas) {
        val scale = if (isClickable) bounce.getScale(0.04f) else 1f
        rect.set(0f, 0f, width.toFloat(), height.toFloat())
        clipPath.reset()
        val r = if (cornerRadius > 0f) cornerRadius else width / 2f
        clipPath.addRoundRect(rect, r, r, Path.Direction.CW)

        if (scale != 1f) {
            canvas.save()
            canvas.scale(scale, scale, width / 2f, height / 2f)
            canvas.clipPath(clipPath)
            super.onDraw(canvas)
            if (strokeWidth > 0f) {
                canvas.drawRoundRect(rect, r, r, strokePaint)
            }
            canvas.restore()
        } else {
            canvas.save()
            canvas.clipPath(clipPath)
            super.onDraw(canvas)
            if (strokeWidth > 0f) {
                canvas.drawRoundRect(rect, r, r, strokePaint)
            }
            canvas.restore()
        }
    }
}
