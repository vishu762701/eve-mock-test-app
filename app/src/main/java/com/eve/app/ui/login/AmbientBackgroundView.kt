package com.eve.app.ui.login

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.provider.Settings
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.cos
import kotlin.math.sin

/**
 * Animated 3D Liquid-Metal / Mercury Blob Background.
 * Renders smooth morphing polished liquid chrome / mercury blobs positioned
 * partially behind and overlapping the modal corners (top-right and bottom-left areas)
 * on a solid black background.
 *
 * Polished liquid chrome features:
 * 1. Ambient soft blur/glow halo extending around edges.
 * 2. High-contrast multi-stop specular chrome gradient (horizon reflection + silver rim).
 * 3. 3D surface depth curvature highlight.
 * 4. Liquid surface tension highlight droplet for molten mercury appearance.
 * 5. Hardware-accelerated, zero battery drain when paused or detached.
 */
class AmbientBackgroundView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var progress: Float = 0f
    private var animator: ValueAnimator? = null
    private var isPlaying = false

    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val chromeBodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val specularGlintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val rimGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = Color.argb(90, 240, 248, 255)
    }

    private val blob1Path = Path()
    private val blob2Path = Path()
    private val highlight1Path = Path()
    private val highlight2Path = Path()

    // 16 morphing nodes for each organic fluid blob
    private val nodeCount = 16
    private val blob1X = FloatArray(nodeCount)
    private val blob1Y = FloatArray(nodeCount)
    private val blob2X = FloatArray(nodeCount)
    private val blob2Y = FloatArray(nodeCount)

    // Pre-allocated chrome reflection palette (Dark Mode)
    private val chromeColors = intArrayOf(
        Color.parseColor("#F8FAFC"), // Ultra-bright specular edge
        Color.parseColor("#E2E8F0"), // Polished silver rim
        Color.parseColor("#94A3B8"), // Platinum metallic body
        Color.parseColor("#334155"), // Deep metallic contrast
        Color.parseColor("#0F172A"), // Horizon shadow line (chrome contrast)
        Color.parseColor("#1E293B"), // Reflected ground shadow
        Color.parseColor("#64748B"), // Steel midtone
        Color.parseColor("#CBD5E1"), // Secondary platinum gleam
        Color.parseColor("#FFFFFF")  // Pure white specular glint
    )
    private val chromeStops = floatArrayOf(
        0.00f, 0.14f, 0.30f, 0.44f, 0.50f, 0.62f, 0.74f, 0.88f, 1.00f
    )

    private val glowColors = intArrayOf(
        Color.argb(95, 210, 230, 255), // Soft cool-chrome luminous core
        Color.argb(55, 140, 180, 220), // Frosted mid glow
        Color.argb(20, 60, 95, 135),   // Diffused blur bleed edge
        Color.TRANSPARENT
    )
    private val glowStops = floatArrayOf(0.0f, 0.45f, 0.75f, 1.0f)

    // Pre-allocated pearlescent liquid glass palette (Light Mode)
    private val pearlColors = intArrayOf(
        Color.parseColor("#FFFFFF"), // Pure white specular gleam
        Color.parseColor("#F1F5F9"), // Luminous soft silver rim
        Color.parseColor("#E2E8F0"), // Soft platinum body
        Color.parseColor("#CBD5E1"), // Cool pearl refraction
        Color.parseColor("#94A3B8"), // Subtle liquid depth
        Color.parseColor("#CBD5E1"), // Reflected sky tint
        Color.parseColor("#E2E8F0"), // Pearl gleam
        Color.parseColor("#F8FAFC"), // High platinum sheen
        Color.parseColor("#FFFFFF")  // Pure white specular glint
    )

    private val pearlGlowColors = intArrayOf(
        Color.argb(90, 255, 255, 255), // Luminous core glow
        Color.argb(50, 203, 213, 225), // Soft pearl bleed
        Color.argb(20, 148, 163, 184), // Diffused edge
        Color.TRANSPARENT
    )

    private fun isDarkMode(): Boolean {
        return try {
            val nightModeFlags = context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
            nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES
        } catch (_: Throwable) {
            true
        }
    }

    init {
        isClickable = false
        isFocusable = false
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    private fun areAnimationsEnabled(): Boolean {
        return try {
            val scale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1.0f
            )
            scale > 0f
        } catch (_: Throwable) {
            true
        }
    }

    fun startAmbientMotion() {
        if (!areAnimationsEnabled()) {
            stopAmbientMotion()
            invalidate()
            return
        }
        if (animator == null) {
            animator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 14000L // 14 seconds slow hypnotic liquid loop
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.RESTART
                interpolator = LinearInterpolator()
                addUpdateListener { va ->
                    progress = va.animatedValue as Float
                    invalidate()
                }
            }
        }
        if (animator?.isStarted != true) {
            animator?.start()
            isPlaying = true
        }
    }

    fun pauseAmbientMotion() {
        if (animator?.isRunning == true) {
            animator?.pause()
            isPlaying = false
        }
    }

    fun resumeAmbientMotion() {
        if (areAnimationsEnabled() && animator?.isPaused == true) {
            animator?.resume()
            isPlaying = true
        } else if (areAnimationsEnabled() && animator?.isRunning != true) {
            startAmbientMotion()
        }
    }

    fun stopAmbientMotion() {
        animator?.cancel()
        animator = null
        isPlaying = false
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (visibility == VISIBLE) {
            startAmbientMotion()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopAmbientMotion()
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        if (visibility == VISIBLE) {
            resumeAmbientMotion()
        } else {
            pauseAmbientMotion()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        // 1. Theme-appropriate canvas background behind everything
        val isDark = isDarkMode()
        if (isDark) {
            canvas.drawColor(Color.BLACK)
        } else {
            canvas.drawColor(Color.parseColor("#EDF2F7"))
        }

        val omega = (progress * 2.0 * Math.PI).toFloat()

        // 2. BLOB 1 (Top-Right Area, partially overlapping modal top-right)
        val cx1 = w * 0.82f + (cos(omega.toDouble()) * (w * 0.035f)).toFloat()
        val cy1 = h * 0.18f + (sin(omega.toDouble()) * (h * 0.025f)).toFloat()
        val baseR1 = (w * 0.30f).coerceAtLeast(100f)

        buildMorphingPath(
            cx = cx1,
            cy = cy1,
            baseR = baseR1,
            omega = omega,
            freqMul = 1.0f,
            phaseOffset = 0.0f,
            outPath = blob1Path,
            outX = blob1X,
            outY = blob1Y
        )

        drawLiquidChromeBlob(
            canvas = canvas,
            cx = cx1,
            cy = cy1,
            radius = baseR1,
            path = blob1Path,
            highlightPath = highlight1Path,
            isTopRight = true,
            isDark = isDark
        )

        // 3. BLOB 2 (Bottom-Left Area, partially overlapping modal bottom-left)
        val cx2 = w * 0.16f + (sin((omega * 1.15f).toDouble()) * (w * 0.035f)).toFloat()
        val cy2 = h * 0.82f + (cos((omega * 0.95f).toDouble()) * (h * 0.028f)).toFloat()
        val baseR2 = (w * 0.33f).coerceAtLeast(110f)

        buildMorphingPath(
            cx = cx2,
            cy = cy2,
            baseR = baseR2,
            omega = omega,
            freqMul = 1.25f,
            phaseOffset = 2.1f,
            outPath = blob2Path,
            outX = blob2X,
            outY = blob2Y
        )

        drawLiquidChromeBlob(
            canvas = canvas,
            cx = cx2,
            cy = cy2,
            radius = baseR2,
            path = blob2Path,
            highlightPath = highlight2Path,
            isTopRight = false,
            isDark = isDark
        )
    }

    /**
     * Builds an organic, continuous C1-smooth closed Bezier path around 16 morphing harmonic nodes.
     */
    private fun buildMorphingPath(
        cx: Float,
        cy: Float,
        baseR: Float,
        omega: Float,
        freqMul: Float,
        phaseOffset: Float,
        outPath: Path,
        outX: FloatArray,
        outY: FloatArray
    ) {
        val step = (2.0 * Math.PI / nodeCount).toFloat()
        for (i in 0 until nodeCount) {
            val theta = i * step
            // Multi-frequency harmonic radius perturbation
            val morph = 1.0f +
                0.16f * sin((2 * theta + omega * freqMul + phaseOffset).toDouble()).toFloat() +
                0.12f * cos((3 * theta - omega * 1.4f * freqMul).toDouble()).toFloat() +
                0.08f * sin((4 * theta + omega * 2.1f + phaseOffset * 0.5f).toDouble()).toFloat()

            val r = baseR * morph
            outX[i] = cx + (r * cos(theta.toDouble())).toFloat()
            outY[i] = cy + (r * sin(theta.toDouble())).toFloat()
        }

        outPath.rewind()
        // Mid-point quadratic Bezier curve interpolation for silky-smooth fluid curvature
        val startMidX = (outX[0] + outX[nodeCount - 1]) / 2f
        val startMidY = (outY[0] + outY[nodeCount - 1]) / 2f
        outPath.moveTo(startMidX, startMidY)

        for (i in 0 until nodeCount) {
            val nextIdx = (i + 1) % nodeCount
            val midX = (outX[i] + outX[nextIdx]) / 2f
            val midY = (outY[i] + outY[nextIdx]) / 2f
            outPath.quadTo(outX[i], outY[i], midX, midY)
        }
        outPath.close()
    }

    /**
     * Renders a single polished 3D liquid mercury chrome blob with glow, metallic reflections,
     * depth shading, and high-gloss glints.
     */
    private fun drawLiquidChromeBlob(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        radius: Float,
        path: Path,
        highlightPath: Path,
        isTopRight: Boolean,
        isDark: Boolean
    ) {
        // Layer A: Outer Frosted Ambient Glow Halo
        val glowRadius = radius * 2.2f
        glowPaint.shader = RadialGradient(
            cx, cy, glowRadius,
            if (isDark) glowColors else pearlGlowColors,
            glowStops,
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, glowRadius, glowPaint)

        // Layer B: 3D Polished Chrome Fluid Body (inclined metallic horizon gradient)
        val angle = if (isTopRight) 0.785f else 2.356f // 45 deg or 135 deg light incident
        val dx = (radius * 1.2f * cos(angle.toDouble())).toFloat()
        val dy = (radius * 1.2f * sin(angle.toDouble())).toFloat()

        chromeBodyPaint.shader = LinearGradient(
            cx - dx, cy - dy,
            cx + dx, cy + dy,
            if (isDark) chromeColors else pearlColors,
            chromeStops,
            Shader.TileMode.CLAMP
        )
        canvas.drawPath(path, chromeBodyPaint)

        // Layer C: Subtle Rim Light Edge Stroke
        rimGlowPaint.color = if (isDark) Color.argb(90, 240, 248, 255) else Color.argb(140, 255, 255, 255)
        canvas.drawPath(path, rimGlowPaint)

        // Layer D: 3D Surface Depth Curvature Glow (Fresnel volume)
        val hlOffsetX = if (isTopRight) -radius * 0.28f else radius * 0.25f
        val hlOffsetY = -radius * 0.26f
        val hlRadius = radius * 0.85f

        specularGlintPaint.shader = RadialGradient(
            cx + hlOffsetX, cy + hlOffsetY, hlRadius,
            if (isDark) {
                intArrayOf(
                    Color.argb(190, 255, 255, 255), // High specular white
                    Color.argb(70, 220, 235, 255),  // Soft spread bloom
                    Color.TRANSPARENT
                )
            } else {
                intArrayOf(
                    Color.argb(230, 255, 255, 255),
                    Color.argb(110, 255, 255, 255),
                    Color.TRANSPARENT
                )
            },
            floatArrayOf(0.0f, 0.40f, 1.0f),
            Shader.TileMode.CLAMP
        )

        // Draw specular gleam inside the blob contour
        canvas.save()
        canvas.clipPath(path)
        canvas.drawCircle(cx + hlOffsetX, cy + hlOffsetY, hlRadius, specularGlintPaint)

        // Layer E: Molten Mercury Liquid Droplet Glint
        highlightPath.rewind()
        val glintX = cx + hlOffsetX * 1.15f
        val glintY = cy + hlOffsetY * 1.15f
        val glintW = radius * 0.35f
        val glintH = radius * 0.18f

        highlightPath.addOval(
            glintX - glintW, glintY - glintH,
            glintX + glintW, glintY + glintH,
            Path.Direction.CW
        )
        specularGlintPaint.shader = RadialGradient(
            glintX, glintY, glintW,
            if (isDark) {
                intArrayOf(
                    Color.argb(230, 255, 255, 255),
                    Color.argb(100, 255, 255, 255),
                    Color.TRANSPARENT
                )
            } else {
                intArrayOf(
                    Color.argb(255, 255, 255, 255),
                    Color.argb(140, 255, 255, 255),
                    Color.TRANSPARENT
                )
            },
            floatArrayOf(0.0f, 0.35f, 1.0f),
            Shader.TileMode.CLAMP
        )
        canvas.drawPath(highlightPath, specularGlintPaint)
        canvas.restore()
    }
}
