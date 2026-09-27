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
import kotlin.math.abs

/**
 * Telegram-style Auto-Generated Gradient Avatar (matching AvatarDrawable.java).
 * Deterministically picks a signature Telegram gradient based on user name/ID hash,
 * and renders centered bold initials in white over a circle.
 */
class AvatarDrawable(
    private val initials: String,
    private val colorIndex: Int
) : Drawable() {

    companion object {
        // Telegram's 7 signature avatar gradient color pairs
        private val GRADIENTS = arrayOf(
            intArrayOf(Color.parseColor("#E17076"), Color.parseColor("#FF885E")), // 0: Red
            intArrayOf(Color.parseColor("#FAA774"), Color.parseColor("#FF7E40")), // 1: Orange
            intArrayOf(Color.parseColor("#A695E7"), Color.parseColor("#7B62D9")), // 2: Violet
            intArrayOf(Color.parseColor("#7BC862"), Color.parseColor("#5FA946")), // 3: Green
            intArrayOf(Color.parseColor("#6EC9CB"), Color.parseColor("#45AEB1")), // 4: Cyan
            intArrayOf(Color.parseColor("#65AADD"), Color.parseColor("#3F82C6")), // 5: Blue
            intArrayOf(Color.parseColor("#EE7AAE"), Color.parseColor("#D45591"))  // 6: Pink
        )

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
            return abs(key.hashCode()) % GRADIENTS.size
        }

        fun create(name: String?, id: String? = null): AvatarDrawable {
            val key = id?.takeIf { it.isNotBlank() } ?: name ?: ""
            val initials = extractInitials(name)
            val index = getColorIndex(key)
            return AvatarDrawable(initials, index)
        }

        fun createBitmap(context: Context, sizePx: Int, name: String?, id: String? = null): Bitmap {
            val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            val drawable = create(name, id)
            drawable.setBounds(0, 0, sizePx, sizePx)
            drawable.draw(canvas)
            return bmp
        }
    }

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

    override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)
        if (bounds.width() <= 0 || bounds.height() <= 0) return

        val colors = GRADIENTS[colorIndex % GRADIENTS.size]
        bgPaint.shader = LinearGradient(
            bounds.left.toFloat(), bounds.top.toFloat(),
            bounds.right.toFloat(), bounds.bottom.toFloat(),
            colors, null, Shader.TileMode.CLAMP
        )

        // Text size proportional to avatar radius
        val diameter = minOf(bounds.width(), bounds.height()).toFloat()
        textPaint.textSize = if (initials.length > 1) diameter * 0.40f else diameter * 0.46f
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
