package com.eve.app.ui.profile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.view.View
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.google.zxing.qrcode.encoder.Encoder
import java.util.Locale
import kotlin.math.min

/**
 * EVE's local-only username QR renderer. Static card geometry and rounded QR
 * cells trace to Telegram QrShareView.java:1879-1881,2046-2061,3808-3819,
 * 3858-3869,4060-4099 and TelegramQRCodeWriter.java:100-166,184-233 at
 * DrKLO/Telegram@3de37c8cfb039452be7398909461da6288babf15.
 * ADAPTED: EVE link/center mark and Android Canvas, no Telegram account,
 * server backdrops, premium, marketplace or original QrShareView animation.
 */
internal class TelegramQrSceneView(
    context: Context, private val username: String, private val link: String
) : View(context) {
    private val density = resources.displayMetrics.density
    private fun dp(v: Float) = v * density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val white = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    private val bounds = RectF()
    private val modulePath = Path()
    private val matrix = Encoder.encode(link, ErrorCorrectionLevel.H).matrix
    var systemTop = 0
        set(value) { field = value; invalidate() }
    var systemBottom = 0
        set(value) { field = value; invalidate() }
    private var palette = 0
    val selectedPalette: Int get() = palette
    fun setPalette(index: Int) { palette = index.coerceIn(0, 2); invalidate() }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawScene(canvas, width.toFloat(), height.toFloat())
    }

    private fun drawScene(canvas: Canvas, w: Float, h: Float) {
        if (w <= 0 || h <= 0) return
        val bg = when (palette) {
            1 -> intArrayOf(0xFF203748.toInt(), 0xFF101C2B.toInt())
            2 -> intArrayOf(0xFF333031.toInt(), 0xFF161819.toInt())
            else -> intArrayOf(0xFF292B2B.toInt(), 0xFF131719.toInt())
        }
        paint.shader = RadialGradient(w / 2f, h * .47f, h * .72f,
            bg, null, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null
        val available = (h - systemTop - systemBottom - dp(174f)).coerceAtLeast(dp(120f))
        val cw = min(w * .745f, available / 1.07f)
        val ch = cw * 1.07f
        val x = (w - cw) / 2f
        val rest = h * .468f - ch / 2f - dp(15f) + dp(16f)
        val upper = (h - systemBottom - dp(100f) - ch)
            .coerceAtLeast(systemTop + dp(72f))
        val y = rest.coerceIn(systemTop + dp(72f), upper)
        bounds.set(x, y, x + cw, y + ch)
        paint.color = 0x25000000
        paint.setShadowLayer(dp(20f), 0f, dp(4f), 0x25000000)
        canvas.drawRoundRect(bounds, dp(30f), dp(30f), paint)
        paint.clearShadowLayer()
        canvas.drawRoundRect(bounds, dp(30f), dp(30f), white)

        val size = cw * .70f
        val qLeft = x + (cw - size) / 2f
        val qTop = y + cw * .12f
        drawModules(canvas, qLeft, qTop, size)
        val caption = "@" + username.uppercase(Locale.ROOT)
        labelPaint.color = qrColor()
        for (pt in floatArrayOf(30f, 25f, 19f)) {
            labelPaint.textSize = dp(pt)
            if (labelPaint.measureText(caption) <= cw - dp(40f) || pt == 19f) break
        }
        val middle = (qTop + size + y + ch) / 2f
        val baseline = middle - (labelPaint.ascent() + labelPaint.descent()) / 2f - dp(4f)
        canvas.drawText(caption, w / 2f, baseline, labelPaint)
    }

    private fun qrColor(): Int = when (palette) {
        1 -> 0xFF166ACE.toInt()
        2 -> 0xFF2D3032.toInt()
        else -> Color.BLACK
    }
    private fun finder(x: Int, y: Int, n: Int) =
        (x < 7 && y < 7) || (x >= n - 7 && y < 7) || (x < 7 && y >= n - 7)
    private fun dark(x: Int, y: Int, n: Int) =
        x in 0 until n && y in 0 until n && matrix.get(x, y).toInt() == 1

    /** Source-inspired connected-corner and finder nests, adapted from TelegramQRCodeWriter. */
    private fun drawModules(canvas: Canvas, left: Float, top: Float, size: Float) {
        val n = matrix.width
        if (n <= 0) return
        val cell = size / n
        val color = qrColor()
        paint.color = color
        val radius = cell * .33f
        val logoStart = (n - 5) / 2
        for (cy in 0 until n) for (cx in 0 until n) {
            if (!dark(cx, cy, n) || finder(cx, cy, n) ||
                (cx in logoStart until logoStart + 5 && cy in logoStart until logoStart + 5)) continue
            val a = FloatArray(8) { radius }
            if (dark(cx, cy - 1, n)) for (i in 0..3) a[i] = 0f
            if (dark(cx, cy + 1, n)) for (i in 4..7) a[i] = 0f
            if (dark(cx - 1, cy, n)) for (i in intArrayOf(0,1,6,7)) a[i] = 0f
            if (dark(cx + 1, cy, n)) for (i in 2..5) a[i] = 0f
            modulePath.rewind()
            modulePath.addRoundRect(RectF(left + cx * cell, top + cy * cell,
                left + (cx + 1) * cell + .05f, top + (cy + 1) * cell + .05f),
                a, Path.Direction.CW)
            canvas.drawPath(modulePath, paint)
        }
        drawFinder(canvas, left, top, cell, color)
        drawFinder(canvas, left + (n - 7) * cell, top, cell, color)
        drawFinder(canvas, left, top + (n - 7) * cell, cell, color)
        val mx = left + size / 2f
        val my = top + size / 2f
        canvas.drawCircle(mx, my, cell * 3f, white)
        paint.color = color
        canvas.drawCircle(mx, my, cell * 2.45f, paint)
        labelPaint.color = Color.WHITE
        labelPaint.typeface = Typeface.create("sans-serif", Typeface.BOLD)
        labelPaint.textSize = cell * 3f
        canvas.drawText("E", mx, my - (labelPaint.ascent() + labelPaint.descent()) / 2f, labelPaint)
        labelPaint.typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
    }

    private fun drawFinder(canvas: Canvas, x: Float, y: Float, cell: Float, color: Int) {
        paint.color = color
        bounds.set(x, y, x + 7 * cell, y + 7 * cell)
        canvas.drawRoundRect(bounds, cell * 1.65f, cell * 1.65f, paint)
        bounds.inset(cell, cell)
        canvas.drawRoundRect(bounds, cell * 1.1f, cell * 1.1f, white)
        bounds.inset(cell, cell)
        canvas.drawRoundRect(bounds, cell * .65f, cell * .65f, paint)
    }

    /** Local rendering without accessing the private profile or Telegram servers. */
    fun renderShareImage(): Bitmap? {
        if (width <= 0 || height <= 0) return null
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        drawScene(Canvas(bitmap), width.toFloat(), height.toFloat())
        return bitmap
    }
}
