package com.eve.app.ui.profile

import android.content.Context
import android.animation.ValueAnimator
import android.graphics.drawable.Drawable
import com.airbnb.lottie.LottieCompositionFactory
import com.airbnb.lottie.LottieDrawable
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
    private val matrix = Encoder.encode(link, ErrorCorrectionLevel.H).matrix
    var systemTop = 0
        set(value) { field = value; invalidate() }
    var systemBottom = 0
        set(value) { field = value; invalidate() }
    // Original backdrop color references; EVE drawing/shaders are independently adapted.
    private val colors = arrayOf(
        intArrayOf(0xFF363738.toInt(), 0xFF0E0F0F.toInt(), 0xFF161819.toInt()),
        intArrayOf(0xFF3FB1FF.toInt(), 0xFF166ACE.toInt(), 0xFF0075F1.toInt()),
        intArrayOf(0xFF5C6985.toInt(), 0xFF354057.toInt(), 0xFF354057.toInt()),
        intArrayOf(0xFF7ECB82.toInt(), 0xFF459E5A.toInt(), 0xFF026B22.toInt()),
        intArrayOf(0xFFB789E4.toInt(), 0xFF8A5ABC.toInt(), 0xFF5B10AB.toInt()),
        intArrayOf(0xFFB77A77.toInt(), 0xFF814C52.toInt(), 0xFF551C22.toInt()),
        intArrayOf(0xFFD19A3A.toInt(), 0xFFC06F47.toInt(), 0xFF9D3907.toInt()),
        intArrayOf(0xFF78C585.toInt(), 0xFF42A171.toInt(), 0xFF006532.toInt())
    )
    val paletteCount: Int get() = colors.size
    fun palettePreviewColor(index: Int): Int = colors[index.coerceIn(0, colors.lastIndex)][0]
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
    private val animations = mutableMapOf<String, LottieDrawable>()
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
        for (entry in selectedEmojis) {
            if (entry.source.isBlank() || animations.containsKey(entry.key) ||
                !animationLoading.add(entry.key)) continue
            val task = when {
                entry.source.startsWith("res:raw/") -> {
                    val name = entry.source.removePrefix("res:raw/")
                    val id = resources.getIdentifier(name, "raw", context.packageName)
                    if (id == 0) {
                        animationLoading.remove(entry.key)
                        continue
                    }
                    LottieCompositionFactory.fromRawRes(context, id)
                }
                entry.source.startsWith("https://") ->
                    LottieCompositionFactory.fromUrl(context, entry.source)
                else -> {
                    animationLoading.remove(entry.key)
                    continue
                }
            }
            val key = entry.key
            task.addListener { composition ->
                animationLoading.remove(key)
                if (!isAttachedToWindow || selectedEmojis.none { it.key == key }) return@addListener
                val drawable = LottieDrawable().apply {
                    this.composition = composition
                    repeatCount = ValueAnimator.INFINITE
                    callback = object : Drawable.Callback {
                        override fun invalidateDrawable(who: Drawable) { postInvalidateOnAnimation() }
                        override fun scheduleDrawable(who: Drawable, what: Runnable, whenMillis: Long) {
                            postDelayed(what, (whenMillis - SystemClock.uptimeMillis()).coerceAtLeast(0L))
                        }
                        override fun unscheduleDrawable(who: Drawable, what: Runnable) {
                            removeCallbacks(what)
                        }
                    }
                    start()
                }
                animations.put(key, drawable)?.stop()
                invalidate()
            }.addFailureListener {
                animationLoading.remove(key)
                // No invented substitute for missing animated source frames.
                invalidate()
            }
        }
        animations.keys.toList().filter { key -> selectedEmojis.none { it.key == key } }
            .forEach { animations.remove(it)?.stop() }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
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
        animations.values.forEach { it.stop(); it.callback = null }
        animations.clear()
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
    private fun finder(x: Int, y: Int, n: Int) =
        (x < 7 && y < 7) || (x >= n - 7 && y < 7) || (x < 7 && y >= n - 7)
    private fun dark(x: Int, y: Int, n: Int) =
        x in 0 until n && y in 0 until n && matrix.get(x, y).toInt() == 1

    /** Source-inspired connected-corner and finder nests, adapted from TelegramQRCodeWriter. */
    private fun drawModules(canvas: Canvas, left: Float, top: Float, size: Float, build: Float) {
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
            if (build < 1f && ((cx * 31 + cy * 17) % 101) / 101f > build) continue
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
        drawScene(Canvas(bitmap), width.toFloat(), height.toFloat(), export = true)
        return bitmap
    }
}
