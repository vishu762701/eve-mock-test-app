package com.eve.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import androidx.core.content.res.ResourcesCompat
import com.eve.app.R
import java.util.Locale

/**
 * Telegram-style Auto-Generated Gradient Avatar (matching AvatarDrawable.java).
 * Deterministically picks a signature Telegram gradient based on user name/ID hash,
 * and renders centered bold initials with accessible foreground contrast over a circle.
 */
class AvatarDrawable(
    private val context: Context,
    private val initials: String,
    private val colorIndex: Int
) : Drawable() {

    companion object {
        fun extractInitials(name: String?): String {
            val clean = name?.trim() ?: ""
            if (clean.isBlank()) return "?"
            val parts = clean.split("\\s+".toRegex()).filter { it.isNotBlank() }
            return when {
                parts.size >= 2 -> {
                    val first = parts[0].take(1).uppercase(Locale.ROOT)
                    val second = parts[1].take(1).uppercase(Locale.ROOT)
                    first + second
                }
                parts.isNotEmpty() -> parts[0].take(1).uppercase(Locale.ROOT)
                else -> "?"
            }
        }

        fun getColorIndex(key: String?): Int {
            if (key.isNullOrBlank()) return 5 // Default blue
            return Math.floorMod(key.hashCode(), 7)
        }

        fun create(context: Context, name: String?, id: String? = null): AvatarDrawable {
            val key = id?.takeIf { it.isNotBlank() } ?: name ?: ""
            val initials = extractInitials(name)
            val index = getColorIndex(key)
            return AvatarDrawable(context, initials, index)
        }

        fun createBitmap(context: Context, sizePx: Int, name: String?, id: String? = null): Bitmap {
            val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            val drawable = create(context, name, id)
            drawable.setTextTypeface(FontManager.typeface(context, bold = true))
            drawable.setBounds(0, 0, sizePx, sizePx)
            drawable.draw(canvas)
            return bmp
        }
    }

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = com.eve.app.designsystem.DesignTokens.color(context, R.attr.designOnAccent)
        textAlign = Paint.Align.CENTER
        typeface = FontManager.typeface(context, bold = true)
    }

    fun setTextTypeface(tf: Typeface) {
        textPaint.typeface = tf
        invalidateSelf()
    }

    override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)
        if (bounds.width() <= 0 || bounds.height() <= 0) return

        bgPaint.shader = null
        bgPaint.color = com.eve.app.designsystem.DesignTokens.accent(context)
        textPaint.color = com.eve.app.designsystem.DesignTokens.color(context, R.attr.designOnAccent)
        textPaint.textSize = context.resources.getDimension(R.dimen.design_type_heading)

    }

    override fun draw(canvas: Canvas) {
        val b = bounds
        if (b.isEmpty) return

        val cx = b.exactCenterX()
        val cy = b.exactCenterY()
        val radius = minOf(b.width(), b.height()) / 2f

        // Draw gradient circular background
        canvas.drawCircle(cx, cy, radius, bgPaint)

        // Draw centered initials
        val textY = cy - ((textPaint.descent() + textPaint.ascent()) / 2f)
        canvas.drawText(initials, cx, textY, textPaint)
    }

    override fun setAlpha(alpha: Int) {
        bgPaint.alpha = alpha
        textPaint.alpha = alpha
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        bgPaint.colorFilter = colorFilter
        textPaint.colorFilter = colorFilter
        invalidateSelf()
    }

    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}
