package com.eve.app.ui.profile

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.View
import androidx.core.graphics.ColorUtils
import com.eve.app.theme.TelegramThemeEngine
import kotlin.math.max

/**
 * Source-backed EVE adapter for Telegram's profile menu blur scene.
 *
 * DrKLO/Telegram@3de37c8cfb039452be7398909461da6288babf15
 * ProfileActivity.java:4073-4090,16979-16996
 * ScrimOptions.java:349-368; AndroidUtilities.java:6032-6094
 * BlurredBackgroundProviderImpl.java:97-110
 * ActionBarMenuItem.java:757-765 (12dp corners, 8dp padding).
 *
 * ADAPTED: The source captures ALL global Telegram windows and tracks its own
 * Blur3 drawable factory. EVE captures only its current attached window before
 * popup creation and uses the already imported Telegram stack-blur math.
 * Original Blur3 source/drawable factory, account/glass layers and live
 * position watcher are NOT integrated. Never claim complete source parity.
 */
internal class ProfilePopupBlurBackdrop private constructor(
    private val popupContent: View,
    private var snapshot: Bitmap?,
    private val sourceWidth: Int,
    private val sourceHeight: Int,
    private val sourceOriginX: Int,
    private val sourceOriginY: Int,
    private val surfaceColor: Int,
    private val dark: Boolean
) : Drawable() {
    companion object {
        const val SOURCE_DOWNSCALE = 15
        const val SOURCE_BLUR_RADIUS = 15
        const val SOURCE_RADIUS_DP = 12f
        const val SOURCE_PADDING_DP = 8f
        const val LIGHT_ALPHA = 0.825f
        const val DARK_ALPHA = 0.85f
        private const val LOG_TAG = "ProfilePopupBlur"

        fun capture(anchor: View, popupContent: View, palette: TelegramThemeEngine.ResolvedPalette): ProfilePopupBlurBackdrop? {
            val scene = anchor.rootView
            if (!scene.isAttachedToWindow || scene.width < 2 || scene.height < 2) return null
            val w = max(1, scene.width / SOURCE_DOWNSCALE)
            val h = max(1, scene.height / SOURCE_DOWNSCALE)
            var raw: Bitmap? = null
            var altered: Bitmap? = null
            try {
                // Source AndroidUtilities.makeGlobalBlurBitmap: 15x downscale,
                // snapshot before popup appears, stack blur radius max(15, size/180).
                raw = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                Canvas(raw).apply {
                    scale(w.toFloat() / scene.width, h.toFloat() / scene.height)
                    drawColor(palette.windowBackground)
                    scene.draw(this)
                }
                ProfileStackBlur.blurBitmap(raw, max(SOURCE_BLUR_RADIUS, max(w, h) / 180))
                // Source ScrimOptions: options bitmap saturation is 2 in Night,
                // 3 in Day; Day brightness is decreased by .07 * 255.
                val matrix = ColorMatrix().apply {
                    setSaturation(if (palette.isDark) 2f else 3f)
                    if (!palette.isDark) {
                        val shift = -0.07f * 255f
                        postConcat(ColorMatrix(floatArrayOf(
                            1f, 0f, 0f, 0f, shift,
                            0f, 1f, 0f, 0f, shift,
                            0f, 0f, 1f, 0f, shift,
                            0f, 0f, 0f, 1f, 0f
                        )))
                    }
                }
                altered = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                Canvas(altered).drawBitmap(raw, 0f, 0f, Paint(Paint.FILTER_BITMAP_FLAG).apply {
                    colorFilter = ColorMatrixColorFilter(matrix)
                })
                val location = IntArray(2).also(scene::getLocationOnScreen)
                val result = ProfilePopupBlurBackdrop(popupContent, altered,
                    scene.width, scene.height, location[0], location[1],
                    palette.submenuBackground, palette.isDark)
                altered = null // transferred to drawable; recycled on dismissal
                return result
            } catch (error: RuntimeException) {
                Log.w(LOG_TAG, "Unable to capture popup scene; retain original nine-patch", error)
                return null
            } catch (error: OutOfMemoryError) {
                Log.w(LOG_TAG, "Popup capture memory fallback", error)
                return null
            } finally {
                raw?.recycle()
                altered?.recycle()
            }
        }
    }

    private val dp = popupContent.resources.displayMetrics.density
    private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp * (2f / 3f)
    }
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.TRANSPARENT
        if (!dark) setShadowLayer(dp * 3.5f, 0f, 0f, 0x38000000)
    }
    private val shaderMatrix = Matrix()
    private val clipPath = Path()
    private val rect = RectF()
    private val origin = IntArray(2)
    private var bitmapShader: BitmapShader? = snapshot?.let {
        BitmapShader(it, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
    }
    private var alphaValue = 255
    val isReleased: Boolean get() = snapshot == null

    override fun getPadding(padding: Rect): Boolean {
        val p = (SOURCE_PADDING_DP * dp).toInt()
        padding.set(p, p, p, p)
        return true
    }

    override fun draw(canvas: Canvas) {
        val b = bounds
        if (b.isEmpty) return
        rect.set(b)
        val r = SOURCE_RADIUS_DP * dp
        val save = canvas.save()
        clipPath.rewind()
        clipPath.addRoundRect(rect, r, r, Path.Direction.CW)
        if (!dark) {
            shadowPaint.alpha = alphaValue
            canvas.drawRoundRect(rect, r, r, shadowPaint)
        }
        canvas.clipPath(clipPath)

        val bmp = snapshot
        val shader = bitmapShader
        if (bmp != null && !bmp.isRecycled && shader != null) {
            popupContent.getLocationOnScreen(origin)
            shaderMatrix.reset()
            shaderMatrix.setScale(sourceWidth.toFloat() / bmp.width,
                sourceHeight.toFloat() / bmp.height)
            shaderMatrix.postTranslate(
                (sourceOriginX - origin[0]).toFloat(),
                (sourceOriginY - origin[1]).toFloat())
            shader.setLocalMatrix(shaderMatrix)
            bitmapPaint.shader = shader
            bitmapPaint.alpha = alphaValue
            canvas.drawRoundRect(rect, r, r, bitmapPaint)
        }

        // Telegram BlurredBackgroundProviderImpl.messageMenuBackground: opacity
        // of the themed submenu surface over blurred sampled content.
        val tint = if (dark) DARK_ALPHA else LIGHT_ALPHA
        fillPaint.color = ColorUtils.setAlphaComponent(surfaceColor,
            (Color.alpha(surfaceColor) * tint * alphaValue / 255f).toInt().coerceIn(0, 255))
        canvas.drawRoundRect(rect, r, r, fillPaint)
        if (!dark) {
            strokePaint.color = 0x44FFFFFF
            strokePaint.alpha = alphaValue
            canvas.save()
            canvas.clipRect(b.left.toFloat(), b.top.toFloat(), b.right.toFloat(), b.exactCenterY())
            canvas.drawRoundRect(rect, r, r, strokePaint)
            canvas.restore()
            strokePaint.color = 0x22FFFFFF
            strokePaint.alpha = alphaValue
            canvas.save()
            canvas.clipRect(b.left.toFloat(), b.exactCenterY(), b.right.toFloat(), b.bottom.toFloat())
            canvas.drawRoundRect(rect, r, r, strokePaint)
            canvas.restore()
        }
        canvas.restoreToCount(save)
    }

    fun release() {
        bitmapPaint.shader = null
        bitmapShader = null
        snapshot?.recycle()
        snapshot = null
        invalidateSelf()
    }

    override fun setAlpha(alpha: Int) {
        alphaValue = alpha.coerceIn(0, 255)
        invalidateSelf()
    }

    override fun getAlpha(): Int = alphaValue
    override fun setColorFilter(colorFilter: ColorFilter?) {
        // Source colors are authoritative; rendering hosts do not tint captured pixels.
        fillPaint.colorFilter = colorFilter
        invalidateSelf()
    }
    @Deprecated("Required Android Drawable API")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}
