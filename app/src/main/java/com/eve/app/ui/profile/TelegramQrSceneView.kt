package com.eve.app.ui.profile

import android.content.Context
import android.animation.ValueAnimator
import android.graphics.drawable.Drawable
import com.eve.app.telegram.animation.NativeQrDrawable
import com.eve.app.telegram.animation.QrAnimationAssets
import kotlinx.coroutines.*
import android.os.SystemClock
import android.view.MotionEvent
import android.view.VelocityTracker
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
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
    private val qrLayer = com.eve.app.telegram.qr.QrCodeLayer()
    private var qrCode: com.eve.app.telegram.qr.QrCodeLayer.Code? = null
    private val qrLogoInk = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif", Typeface.BOLD)
    }
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
    private val backdrop = com.eve.app.telegram.qr.QrBackdropScene(this)
    private var sceneScale = 1f
    private var qrBuild = 1f
    private var closing = false
    private var reveal: com.eve.app.telegram.qr.QrSceneReveal? = null
    private val shadow = com.eve.app.telegram.qr.QrCardShadow(this)
    private var revealing = false
    private val cardMotion = com.eve.app.telegram.qr.QrCardMotion()
    private var glideAnimator: ValueAnimator? = null
    private var selectedEmojis: List<QrEmojiChoice> = emptyList()
    val selectedEmoji: String? get() = selectedEmojis.firstOrNull()?.alt
    val emojiChoices: List<QrEmojiChoice> get() = selectedEmojis.toList()
    private val animations = mutableMapOf<String, NativeQrDrawable>()
    private val legacyGlyphs = mutableMapOf<String, Drawable>()
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
        palette = target
        val cy = if (bounds.isEmpty) height * .468f else bounds.centerY()
        val now = SystemClock.uptimeMillis()
        val animate = isLaidOut && isAttachedToWindow && com.eve.app.designsystem.DesignMotion.enabled(context)
        backdrop.setPalette(target, width / 2f, cy, now, animate)
        cardMotion.recolor(now, animate)
        invalidate()
    }

    fun drawControlBackdrop(canvas: Canvas, owner: View): Boolean = backdrop.drawControlBackdrop(canvas, owner)

    /** Legacy single-emoji entry remains supported for existing saved QR preferences. */
    fun setEmoji(value: String?) {
        setEmojiChoices(value?.takeIf { it.isNotBlank() }?.let {
            listOf(QrEmojiPattern.standard(it))
        } ?: emptyList())
    }

    fun toggleEmoji(choice: QrEmojiChoice) =
        setEmojiChoices(QrEmojiPattern.toggle(selectedEmojis, choice))

    fun setEmojiChoices(choices: List<QrEmojiChoice>) {
        val next = choices.distinctBy { it.key }.takeLast(QrEmojiPattern.MAX_SELECTED)
        if (selectedEmojis == next) return
        selectedEmojis = next
        cardMotion.changeEmojis(SystemClock.uptimeMillis(),
            isLaidOut && isAttachedToWindow && com.eve.app.designsystem.DesignMotion.enabled(context))
        selectionStartedAt = SystemClock.uptimeMillis()
        legacyGlyphs.keys.retainAll(selectedEmojis.map { it.key }.toSet())
        if (isAttachedToWindow) loadSelectedAnimations()
        invalidate()
    }

    /** Shared read-only frame for the original picked-items overlay; scene retains ownership. */
    fun emojiDrawable(key: String): Drawable? = animations[key] ?: selectedEmojis
        .firstOrNull { it.key == key && it.source.isBlank() }?.let { legacy ->
            // Existing saved standard glyphs remain visibly standard; never an animated document.
            legacyGlyphs.getOrPut(key) { object : Drawable() {
                private val glyph = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
                override fun draw(canvas: Canvas) {
                    glyph.textSize = bounds.height().toFloat()
                    canvas.drawText(legacy.alt, bounds.exactCenterX(),
                        bounds.exactCenterY() - (glyph.ascent() + glyph.descent()) / 2f, glyph)
                }
                override fun setAlpha(alpha: Int) { glyph.alpha = alpha }
                override fun setColorFilter(filter: android.graphics.ColorFilter?) { glyph.colorFilter = filter }
                @Deprecated("Drawable opacity is not used")
                override fun getOpacity() = android.graphics.PixelFormat.TRANSLUCENT
            } }
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
                    refreshEmojiPlayback()
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
        shadow.prepare(colors[palette][2])
    }

    fun bindSceneLayer(layer: View, controls: Array<View>) {
        reveal?.close()
        reveal = com.eve.app.telegram.qr.QrSceneReveal(layer, controls) { scale, build, active ->
            sceneScale = scale
            qrBuild = build
            revealing = active
            invalidate()
        }
    }

    fun refreshReveal() { reveal?.refresh() }

    fun animateOpen() {
        closing = false
        reveal?.open(com.eve.app.designsystem.DesignMotion.enabled(context))
    }

    fun animateClosed(onClosed: () -> Unit) {
        if (closing) return
        closing = true
        reveal?.close(com.eve.app.designsystem.DesignMotion.enabled(context), onClosed) ?: onClosed()
    }

    fun stopMotion() {
        reveal?.close()
        shadow.close()
        cardMotion.clear()
        backdrop.close()
        glideAnimator?.cancel()
        tracker?.recycle()
        tracker = null
        assetScope?.cancel()
        assetScope = null
        animations.values.forEach { it.close() }
        animations.clear()
        legacyGlyphs.clear()
        // Source Code atlas is owned by QrCodeLayer; discard our handle only.
        qrCode = null
        animationLoading.clear()
    }

    fun refreshEmojiPlayback() {
        val run = isShown && windowVisibility == VISIBLE && com.eve.app.designsystem.DesignMotion.enabled(context)
        animations.values.forEach { if (run) it.start() else it.stop() }
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        if (isAttachedToWindow) refreshEmojiPlayback()
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
        val motionEnabled = com.eve.app.designsystem.DesignMotion.enabled(context)
        refreshEmojiPlayback()
        drawScene(canvas, width.toFloat(), height.toFloat())
        if (((motionEnabled && selectedEmojis.isNotEmpty()) || qrLayer.isAnimating() || backdrop.isAnimating || cardMotion.isAnimating(SystemClock.uptimeMillis())) && isShown) postInvalidateOnAnimation()
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
        val motionEnabled = com.eve.app.designsystem.DesignMotion.enabled(context)
        val motion = if (motionEnabled) movingMs.coerceAtLeast(0L) / 1000f else 0f
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
                val scale = if (!motionEnabled) 1f else QrEmojiPattern.appear(elapsed, seed) *
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

    private fun updateCardBounds(w: Float, h: Float) {
        val available = (h - systemTop - systemBottom - dp(174f)).coerceAtLeast(dp(120f))
        val cw = min(w * .745f, available / 1.07f)
        val ch = cw * 1.07f
        val x = (w - cw) / 2f
        val rest = h * .468f - ch / 2f - dp(15f) + dp(16f)
        val upper = (h - systemBottom - dp(100f) - ch)
            .coerceAtLeast(systemTop + dp(72f))
        val y = rest.coerceIn(systemTop + dp(72f), upper)
        bounds.set(x, y, x + cw, y + ch)
    }

    private fun drawScene(canvas: Canvas, w: Float, h: Float, export: Boolean = false) {
        if (w <= 0 || h <= 0) return
        updateCardBounds(w, h)
        val cw = bounds.width()
        val ch = bounds.height()
        val x = bounds.left
        val y = bounds.top
        val renderer = if (export) backdrop.snapshot() else backdrop
        renderer.setGeometry(w.toInt(), h.toInt(), bounds.centerY())
        if (!export) {
            renderer.step(SystemClock.uptimeMillis(), com.eve.app.designsystem.DesignMotion.enabled(context))
        }
        try {
            renderer.draw(canvas, { target -> drawPattern(target, w, h) }, !revealing && !export,
                if (!export && revealing) com.eve.app.telegram.qr.QrRender.screenCornerRadius(this) else 0f)
            frameQrColor = renderer.qrColor()
        } finally {
            if (export) renderer.close()
        }
        canvas.save()
        if (!export) {
            val scale = sceneScale * cardMotion.scale(SystemClock.uptimeMillis(),
                com.eve.app.designsystem.DesignMotion.enabled(context))
            canvas.scale(scale, scale, w / 2f, y + ch / 2f)
        }
        shadow.draw(canvas, bounds, qrColor(), export)
        canvas.drawRoundRect(bounds, dp(30f), dp(30f), white)

        val size = cw * .70f
        val qLeft = x + (cw - size) / 2f
        val qTop = y + cw * .12f
        drawModules(canvas, qLeft, qTop, size, if (export) 1f else qrBuild, export)
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

    private var frameQrColor = colors[0][2]
    private fun qrColor(): Int = frameQrColor
    /** Cache the source writer's white QR mask so palette changes only recolor it. */
    private fun ensureQrCode(): com.eve.app.telegram.qr.QrCodeLayer.Code? {
        qrCode?.let { return it }
        val mark = object : Drawable() {
            private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
            override fun draw(target: Canvas) {
                target.drawCircle(bounds.exactCenterX(), bounds.exactCenterY(),
                    bounds.width() / 2f, fill)
            }
            override fun setAlpha(alpha: Int) { fill.alpha = alpha }
            override fun setColorFilter(filter: android.graphics.ColorFilter?) { fill.colorFilter = filter }
            @Deprecated("Drawable opacity is not used")
            override fun getOpacity() = android.graphics.PixelFormat.TRANSLUCENT
        }
        val data = com.eve.app.telegram.qr.QrCodeLayer.build(
            link, dp(240f).toInt(), ErrorCorrectionLevel.M, .75f, mark
        ) ?: return null
        qrCode = data
        qrLayer.assemble(data)
        return data
    }

    /** Java source module/halftone animation with EVE's white E drawn above the mask. */
    private fun drawModules(canvas: Canvas, left: Float, top: Float, size: Float, build: Float,
                            export: Boolean = false) {
        val data = ensureQrCode() ?: return
        val layer = if (export) com.eve.app.telegram.qr.QrCodeLayer().apply { set(data) } else qrLayer
        layer.setColor(qrColor())
        layer.setCardBounds(bounds.left, bounds.top, bounds.right, bounds.bottom)
        if (!export) layer.setBuildOverride(build)
        layer.draw(canvas, left, top, size)
        val markSide = size * com.eve.app.telegram.qr.QrCodeLayer.logoShare(data)
        qrLogoInk.textSize = markSide * .65f
        canvas.drawText("E", left + size / 2f,
            top + size / 2f - (qrLogoInk.ascent() + qrLogoInk.descent()) / 2f, qrLogoInk)
    }

    /** Local rendering without accessing the private profile or Telegram servers. */
    fun renderShareImage(): Bitmap? {
        if (width <= 0 || height <= 0) return null
        updateCardBounds(width.toFloat(), height.toFloat())
        val shareHeight = min(height, (width * 1.92f).toInt())
        val cardCenter = if (bounds.isEmpty) height * .468f else bounds.centerY()
        val top = (cardCenter - shareHeight / 2f).toInt().coerceIn(0, height - shareHeight)
        val bitmap = try { Bitmap.createBitmap(width, shareHeight, Bitmap.Config.ARGB_8888) }
            catch (_: OutOfMemoryError) { return null }
        try {
            val canvas = Canvas(bitmap)
            canvas.translate(0f, -top.toFloat())
            drawScene(canvas, width.toFloat(), height.toFloat(), export = true)
            canvas.setBitmap(null)
            return bitmap
        } catch (failure: Throwable) {
            bitmap.recycle()
            throw failure
        }
    }
}
