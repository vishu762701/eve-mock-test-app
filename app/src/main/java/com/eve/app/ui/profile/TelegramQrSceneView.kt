package com.eve.app.ui.profile

import android.content.Context
import android.animation.ValueAnimator
import android.graphics.drawable.Drawable
import com.eve.app.telegram.animation.NativeQrDrawable
import com.eve.app.telegram.animation.QrAnimationAssets
import kotlinx.coroutines.*
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.os.SystemClock
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.animation.LinearInterpolator
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
import kotlin.math.hypot

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
    private var qrBitmap: Bitmap? = null
    private var qrBitmapColor: Int? = null
    private val qrBitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    var systemTop = 0
        set(value) { field = value; invalidate() }
    var systemBottom = 0
        set(value) { field = value; invalidate() }
    // Original backdrop color references; EVE drawing/shaders are independently adapted.
    private val colors = Array(com.eve.app.telegram.qr.QrPalettes.count()) { index ->
        intArrayOf(com.eve.app.telegram.qr.QrPalettes.center(index),
            com.eve.app.telegram.qr.QrPalettes.edge(index),
            com.eve.app.telegram.qr.QrPalettes.ink(index))
    }
    val paletteCount: Int get() = colors.size
    fun palettePreviewColor(index: Int): Int = colors[index.coerceIn(0, colors.lastIndex)][0]
    fun paletteName(index: Int): String = com.eve.app.telegram.qr.QrPalettes.name(index)
    private var palette = 0
    private var previousPalette = 0
    private var paletteBlend = 1f
    private var sceneScale = 1f
    private var qrBuild = 1f
    private var closing = false
    private var openingAnimator: ValueAnimator? = null
    private var paletteAnimator: ValueAnimator? = null
    private var glideAnimator: ValueAnimator? = null
    private var selectedEmojis: List<QrEmojiChoice> = emptyList()
    val selectedEmoji: String? get() = selectedEmojis.firstOrNull()?.alt
    val emojiChoices: List<QrEmojiChoice> get() = selectedEmojis.toList()
    private val animations = mutableMapOf<String, NativeQrDrawable>()
    private var assetScope: CoroutineScope? = null
    private val animationLoading = mutableSetOf<String>()
    private var fieldPauseStarted = 0L
    private var fieldPausedMs = 0L
    private var selectionStartedAt = SystemClock.uptimeMillis()
    private val emojiPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        alpha = 110
    }
    private val startedAt = SystemClock.uptimeMillis()
    private var panX = 0f
    private var panY = 0f
    private var startPanX = 0f
    private var startPanY = 0f
    private var downX = 0f
    private var downY = 0f
    private var dragging = false
    private var tracker: VelocityTracker? = null

    val selectedPalette: Int get() = palette
    fun setPalette(index: Int) {
        val target = index.coerceIn(0, colors.lastIndex)
        if (target == palette) return
        paletteAnimator?.cancel()
        previousPalette = palette
        palette = target
        paletteBlend = 0f
        paletteAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = TelegramQrMotion.WAVE_MS
            addUpdateListener { paletteBlend = it.animatedValue as Float; invalidate() }
            start()
        }
    }
    /** Legacy single-emoji entry remains supported for existing saved QR preferences. */
    fun setEmoji(value: String?) {
        setEmojiChoices(value?.takeIf { it.isNotBlank() }?.let {
            listOf(QrEmojiPattern.standard(it))
        } ?: emptyList())
    }

    fun toggleEmoji(choice: QrEmojiChoice) =
        setEmojiChoices(QrEmojiPattern.toggle(selectedEmojis, choice))

    fun setEmojiChoices(choices: List<QrEmojiChoice>) {
        selectedEmojis = choices.distinctBy { it.key }.takeLast(QrEmojiPattern.MAX_SELECTED)
        selectionStartedAt = SystemClock.uptimeMillis()
        if (isAttachedToWindow) loadSelectedAnimations()
        invalidate()
    }

    private fun loadSelectedAnimations() {
        val scope = assetScope ?: return
        for (entry in selectedEmojis) {
            if (entry.source.isBlank() || animations.containsKey(entry.key) ||
                !animationLoading.add(entry.key)) continue
            scope.launch {
                try {
                    val json = QrAnimationAssets.json(context.applicationContext, entry.source)
                    ensureActive()
                    if (!isAttachedToWindow || selectedEmojis.none { it.key == entry.key }) return@launch
                    val drawable = NativeQrDrawable.fromJson(json, dp(140f).toInt())
                    drawable.callback = object : Drawable.Callback {
                        override fun invalidateDrawable(who: Drawable) { postInvalidateOnAnimation() }
                        override fun scheduleDrawable(who: Drawable, what: Runnable, whenMillis: Long) {
                            postDelayed(what, (whenMillis - SystemClock.uptimeMillis()).coerceAtLeast(0L))
                        }
                        override fun unscheduleDrawable(who: Drawable, what: Runnable) { removeCallbacks(what) }
                    }
                    animations.put(entry.key, drawable)?.close()
                    if (com.eve.app.designsystem.DesignMotion.enabled(context)) drawable.start()
                    invalidate()
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    // Missing/invalid source remains unavailable; never fabricate animation frames.
                    invalidate()
                } finally {
                    animationLoading.remove(entry.key)
                }
            }
        }
        animations.keys.toList().filter { key -> selectedEmojis.none { it.key == key } }
            .forEach { animations.remove(it)?.close() }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        assetScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        loadSelectedAnimations()
    }

    private fun setReveal(value: Float, opening: Boolean) {
        val eased = TelegramQrMotion.reveal(value, opening)
        translationY = dp(64f) * (1f - eased)
        alpha = (eased / .35f).coerceIn(0f, 1f)
        sceneScale = if (opening) .9f + .1f * eased else .92f + .08f * eased
        qrBuild = if (opening) (value * TelegramQrMotion.OPEN_MS / TelegramQrMotion.QR_BUILD_MS).coerceIn(0f, 1f) else 1f
        invalidate()
    }

    fun animateOpen() {
        openingAnimator?.cancel()
        closing = false
        setReveal(0f, true)
        openingAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = TelegramQrMotion.OPEN_MS
            interpolator = LinearInterpolator()
            addUpdateListener { setReveal(it.animatedValue as Float, true) }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) { if (!closing) setReveal(1f, true) }
            })
            start()
        }
    }

    fun animateClosed(onClosed: () -> Unit) {
        if (closing) return
        closing = true
        openingAnimator?.cancel()
        openingAnimator = ValueAnimator.ofFloat(1f, 0f).apply {
            duration = TelegramQrMotion.CLOSE_MS
            interpolator = LinearInterpolator()
            addUpdateListener { setReveal(it.animatedValue as Float, false) }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) { onClosed() }
            })
            start()
        }
    }

    fun stopMotion() {
        openingAnimator?.removeAllListeners()
        openingAnimator?.cancel()
        paletteAnimator?.cancel()
        glideAnimator?.cancel()
        tracker?.recycle()
        tracker = null
        assetScope?.cancel()
        assetScope = null
        animations.values.forEach { it.close() }
        animations.clear()
        qrBitmap = null
        qrBitmapColor = null
        animationLoading.clear()
    }

    override fun onDetachedFromWindow() {
        stopMotion()
        super.onDetachedFromWindow()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                glideAnimator?.cancel()
                tracker?.recycle()
                tracker = VelocityTracker.obtain().also { it.addMovement(event) }
                downX = event.x; downY = event.y
                startPanX = panX; startPanY = panY
                dragging = false
                fieldPauseStarted = SystemClock.uptimeMillis()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                tracker?.addMovement(event)
                val dx = event.x - downX
                val dy = event.y - downY
                if (hypot(dx.toDouble(), dy.toDouble()) > dp(8f)) dragging = true
                if (dragging) {
                    panX = startPanX + TelegramQrMotion.rubber(dx, width.toFloat())
                    panY = startPanY + TelegramQrMotion.rubber(dy, height.toFloat())
                    invalidate()
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                tracker?.addMovement(event)
                if (dragging) {
                    tracker?.computeCurrentVelocity(1000)
                    val limit = dp(TelegramQrMotion.MAX_FLING_DP_PER_SEC)
                    val vx = (tracker?.xVelocity ?: 0f).coerceIn(-limit, limit)
                    val vy = (tracker?.yVelocity ?: 0f).coerceIn(-limit, limit)
                    val x0 = panX; val y0 = panY
                    glideAnimator = ValueAnimator.ofFloat(0f, .6f).apply {
                        duration = 600
                        addUpdateListener {
                            val t = it.animatedValue as Float
                            panX = x0 + TelegramQrMotion.glide(vx, t)
                            panY = y0 + TelegramQrMotion.glide(vy, t)
                            invalidate()
                        }
                        start()
                    }
                }
                tracker?.recycle()
                tracker = null
                if (fieldPauseStarted != 0L) {
                    fieldPausedMs += SystemClock.uptimeMillis() - fieldPauseStarted
                    fieldPauseStarted = 0L
                }
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                tracker?.recycle(); tracker = null
                dragging = false
                if (fieldPauseStarted != 0L) {
                    fieldPausedMs += SystemClock.uptimeMillis() - fieldPauseStarted
                    fieldPauseStarted = 0L
                }
                return true
            }
        }
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawScene(canvas, width.toFloat(), height.toFloat())
        if (selectedEmojis.isNotEmpty() && isShown) postInvalidateOnAnimation()
    }

    private fun mix(a: Int, b: Int, t: Float): Int {
        val k = t.coerceIn(0f, 1f)
        fun channel(shift: Int): Int {
            val av = a ushr shift and 255
            val bv = b ushr shift and 255
            return (av + (bv - av) * k).toInt() shl shift
        }
        return channel(24) or channel(16) or channel(8) or channel(0)
    }

    /**
     * Source-traced hex-staggered field, orbit, per-node breath and staggered pop.
     * Custom emoji draw actual Lottie frames on this Canvas; standard Unicode emoji
     * use the device glyph, as the upstream does for non-custom emoji.
     */
    private fun drawPattern(canvas: Canvas, w: Float, h: Float) {
        if (selectedEmojis.isEmpty()) return
        val now = SystemClock.uptimeMillis()
        val movingMs = now - startedAt - fieldPausedMs -
            (if (fieldPauseStarted != 0L) now - fieldPauseStarted else 0L)
        val motion = movingMs.coerceAtLeast(0L) / 1000f
        val elapsed = (now - selectionStartedAt).coerceAtLeast(0L).toFloat()
        val dx = dp(QrEmojiPattern.COLUMN_DP)
        val dy = dp(QrEmojiPattern.ROW_DP)
        val margin = dp(117f)
        val r0 = kotlin.math.floor((-margin - panY) / dy).toInt() - 1
        val r1 = kotlin.math.ceil((h + margin - panY) / dy).toInt() + 1
        for (row in r0..r1) {
            val shift = if ((row and 1) == 0) 0f else dx / 2f
            val c0 = kotlin.math.floor((-margin - panX - shift) / dx).toInt() - 1
            val c1 = kotlin.math.ceil((w + margin - panX - shift) / dx).toInt() + 1
            for (col in c0..c1) {
                val seed = QrEmojiPattern.hash(row, col)
                if (QrEmojiPattern.fraction(seed * 53.1) < .2f) continue
                val phase = (seed * 2.0 * Math.PI).toFloat()
                val radius = dp(3f + 7f * QrEmojiPattern.fraction(seed * 31.7))
                val period = 9f + 11f * QrEmojiPattern.fraction(seed * 37.1)
                val direction = if (QrEmojiPattern.fraction(seed * 43.3) < .5f) -1f else 1f
                val angle = direction * 2f * Math.PI.toFloat() * motion / period + phase
                val x = col * dx + shift + panX + radius * kotlin.math.cos(angle)
                val y = row * dy + panY + radius * kotlin.math.sin(angle) +
                    kotlin.math.sin(motion * .45f + phase) * dp(3f)
                if (x < -margin || x > w + margin || y < -margin || y > h + margin) continue
                val q = col - row / 2
                val index = Math.floorMod(q - row, selectedEmojis.size)
                val entry = selectedEmojis[index]
                val big = (row and 1) == 0
                val baseSize = dp(38f) * (if (big) 1.3f else .9f) *
                    (.92f + .16f * seed.toFloat()) * (if (big) .9f else 1f)
                val scale = QrEmojiPattern.appear(elapsed, seed) *
                    (1f + .03f * kotlin.math.sin(motion * 1.3f + phase * 2f))
                if (scale <= .01f) continue
                val rotate = (seed.toFloat() - .5f) * 26f +
                    4f * kotlin.math.sin(motion * .3f + phase) - .763f * motion
                canvas.save()
                canvas.translate(x, y)
                canvas.rotate(rotate)
                canvas.scale(scale, scale)
                val drawable = animations[entry.key]
                if (drawable != null) {
                    val half = (baseSize / 2f).toInt()
                    drawable.setBounds(-half, -half, half, half)
                    drawable.draw(canvas)
                } else if (entry.source.isBlank()) {
                    emojiPaint.textSize = baseSize
                    emojiPaint.alpha = 115
                    canvas.drawText(entry.alt, 0f, -(emojiPaint.ascent() + emojiPaint.descent()) / 2f, emojiPaint)
                }
                canvas.restore()
            }
        }
    }

    private fun drawScene(canvas: Canvas, w: Float, h: Float, export: Boolean = false) {
        if (w <= 0 || h <= 0) return
        val newColor = colors[palette]
        val oldColor = colors[previousPalette]
        val bg = intArrayOf(mix(oldColor[0], newColor[0], if (export) 1f else paletteBlend),
            mix(oldColor[1], newColor[1], if (export) 1f else paletteBlend))
        paint.shader = RadialGradient(w / 2f, h * .47f, h * .72f,
            bg, null, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null
        drawPattern(canvas, w, h)
        val available = (h - systemTop - systemBottom - dp(174f)).coerceAtLeast(dp(120f))
        val cw = min(w * .745f, available / 1.07f)
        val ch = cw * 1.07f
        val x = (w - cw) / 2f
        val rest = h * .468f - ch / 2f - dp(15f) + dp(16f)
        val upper = (h - systemBottom - dp(100f) - ch)
            .coerceAtLeast(systemTop + dp(72f))
        val y = rest.coerceIn(systemTop + dp(72f), upper)
        bounds.set(x, y, x + cw, y + ch)
        canvas.save()
        if (!export) canvas.scale(sceneScale, sceneScale, w / 2f, y + ch / 2f)
        paint.color = 0x25000000
        paint.setShadowLayer(dp(20f), 0f, dp(4f), 0x25000000)
        canvas.drawRoundRect(bounds, dp(30f), dp(30f), paint)
        paint.clearShadowLayer()
        canvas.drawRoundRect(bounds, dp(30f), dp(30f), white)

        val size = cw * .70f
        val qLeft = x + (cw - size) / 2f
        val qTop = y + cw * .12f
        drawModules(canvas, qLeft, qTop, size, if (export) 1f else qrBuild)
        val caption = "@" + username.uppercase(Locale.ROOT)
        labelPaint.color = qrColor()
        for (pt in floatArrayOf(30f, 25f, 19f)) {
            labelPaint.textSize = dp(pt)
            if (labelPaint.measureText(caption) <= cw - dp(40f) || pt == 19f) break
        }
        val middle = (qTop + size + y + ch) / 2f
        val baseline = middle - (labelPaint.ascent() + labelPaint.descent()) / 2f - dp(4f)
        canvas.drawText(caption, w / 2f, baseline, labelPaint)
        canvas.restore()
    }

    private fun qrColor(): Int = colors[palette][2]
    /** Upstream rounded-module renderer; only the center brand is supplied by EVE. */
    private fun drawModules(canvas: Canvas, left: Float, top: Float, size: Float, build: Float) {
        val color = qrColor()
        if (qrBitmap == null || qrBitmapColor != color) {
            val writer = com.eve.app.telegram.qr.TelegramQRCodeWriter()
            writer.setCenterDrawable(object : Drawable() {
                private val circle = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
                private val letter = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    this.color = Color.WHITE
                    typeface = Typeface.create("sans-serif", Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                }
                override fun draw(target: Canvas) {
                    val radius = bounds.width() / 2f
                    target.drawCircle(bounds.exactCenterX(), bounds.exactCenterY(), radius, circle)
                    letter.textSize = bounds.height() * .65f
                    target.drawText("E", bounds.exactCenterX(),
                        bounds.exactCenterY() - (letter.ascent() + letter.descent()) / 2f, letter)
                }
                override fun setAlpha(alpha: Int) { circle.alpha = alpha; letter.alpha = alpha }
                override fun setColorFilter(filter: android.graphics.ColorFilter?) { circle.colorFilter = filter }
                @Deprecated("Drawable opacity is not used")
                override fun getOpacity() = android.graphics.PixelFormat.TRANSLUCENT
            })
            val hints = mapOf<com.google.zxing.EncodeHintType, Any>(
                com.google.zxing.EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
                com.google.zxing.EncodeHintType.MARGIN to 0)
            // The old bitmap is released by GC, so an outstanding hardware display list never sees a recycled bitmap.
            qrBitmap = writer.encode(link, dp(240f).toInt(), dp(240f).toInt(), hints, null,
                .75f, Color.WHITE, color)
            qrBitmapColor = color
        }
        qrBitmapPaint.alpha = (build.coerceIn(0f, 1f) * 255).toInt()
        canvas.drawBitmap(requireNotNull(qrBitmap), null, RectF(left, top, left + size, top + size), qrBitmapPaint)
    }

    /** Local rendering without accessing the private profile or Telegram servers. */
    fun renderShareImage(): Bitmap? {
        if (width <= 0 || height <= 0) return null
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        drawScene(Canvas(bitmap), width.toFloat(), height.toFloat(), export = true)
        return bitmap
    }
}
