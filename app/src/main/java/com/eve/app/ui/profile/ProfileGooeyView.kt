/*
 * Derived from Telegram Android ProfileGooeyView.java, Copyright Nikolai Kudashov.
 * GNU GPL v2 or later. See THIRD_PARTY_NOTICES.md.
 */

package com.eve.app.ui.profile

import android.app.ActivityManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BlendMode
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.RenderEffect
import android.graphics.RenderNode
import android.graphics.Shader
import android.os.Build
import android.provider.Settings
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.RequiresApi
import androidx.core.math.MathUtils
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min

class ProfileGooeyView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    companion object {
        private const val AVATAR_SIZE_DP = 100f
        private const val BLACK_KING_BAR = 32f

        private fun isReducedMotion(context: Context): Boolean {
            return try {
                Settings.Global.getFloat(
                    context.contentResolver,
                    Settings.Global.ANIMATOR_DURATION_SCALE,
                    1f
                ) == 0f
            } catch (_: Exception) {
                false
            }
        }

        private fun isHighPerformance(context: Context): Boolean {
            return try {
                val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
                val memoryClass = am?.memoryClass ?: 192
                val cores = Runtime.getRuntime().availableProcessors()
                memoryClass >= 256 && cores >= 4
            } catch (_: Exception) {
                true
            }
        }
    }

    private val density = resources.displayMetrics.density
    private fun dp(v: Float): Float = v * density
    private fun dp(v: Int): Float = v * density

    private val blackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK }
    private val path = Path()

    private var intensity: Float = 15f
    private var pullProgress: Float = 0f
    private var blurIntensity: Float = 0f
    private var gooeyEnabled: Boolean = false

    var notchInfo: NotchInfoUtils.NotchInfo? = null

    private interface Impl {
        fun onSizeChanged(w: Int, h: Int)
        fun setIntensity(intensity: Float)
        fun setBlurIntensity(intensity: Float)
        fun draw(drawer: (Canvas) -> Unit, canvas: Canvas)
    }

    private val impl: Impl

    init {
        setWillNotDraw(false)
        val reduced = isReducedMotion(context)
        val highPerf = isHighPerformance(context)

        impl = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !reduced && highPerf) {
            GPUImpl(1f)
        } else {
            CPUImpl()
        }
        impl.setIntensity(intensity)
        impl.setBlurIntensity(0f)
    }

    fun getEndOffset(occupyStatusBar: Boolean, avatarScale: Float): Float {
        val info = notchInfo
        return if (info != null) {
            val h = if (info.isLikelyCircle) {
                info.bounds.width() + info.bounds.width() * getAvatarEndScale()
            } else {
                info.bounds.height() - info.bounds.top
            }
            -(dp(16) + h)
        } else {
            val sb = if (occupyStatusBar) dp(24) else 0f
            -(sb + dp(16) + dp(AVATAR_SIZE_DP))
        }
    }

    fun getAvatarEndScale(): Float {
        val info = notchInfo
        return if (info != null) {
            val f = if (info.isLikelyCircle) {
                (info.bounds.width() - dp(2)) / dp(AVATAR_SIZE_DP)
            } else {
                min(info.bounds.width(), info.bounds.height()) / dp(AVATAR_SIZE_DP)
            }
            min(0.8f, max(0.2f, f))
        } else {
            0.8f
        }
    }

    fun hasNotchInfo(): Boolean = notchInfo != null

    fun setIntensity(value: Float) {
        intensity = value
        impl.setIntensity(value)
        invalidate()
    }

    fun setPullProgress(value: Float) {
        pullProgress = value
        invalidate()
    }

    fun setBlurIntensity(value: Float) {
        blurIntensity = value
        impl.setBlurIntensity(value)
        invalidate()
    }

    fun setGooeyEnabled(enabled: Boolean) {
        if (gooeyEnabled == enabled) return
        gooeyEnabled = enabled
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val info = NotchInfoUtils.getInfo(context)
        notchInfo = if (info != null && info.gravity == Gravity.CENTER && w <= h) {
            info
        } else {
            null
        }
        impl.onSizeChanged(w, h)
    }

    override fun draw(canvas: Canvas) {
        if (!gooeyEnabled) {
            super.draw(canvas)
            return
        }
        impl.draw({ c ->
            c.save()
            c.translate(0f, dp(BLACK_KING_BAR))
            super.draw(c)
            c.restore()
        }, canvas)
    }

    private inner class CPUImpl : Impl {
        override fun onSizeChanged(w: Int, h: Int) {}
        override fun setIntensity(intensity: Float) {}
        override fun setBlurIntensity(intensity: Float) {}
        override fun draw(drawer: (Canvas) -> Unit, canvas: Canvas) {
            drawer(canvas)
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private inner class GPUImpl(private val factorMult: Float) : Impl {
        private val filter = Paint(Paint.ANTI_ALIAS_FLAG)
        private val node = RenderNode("render")
        private val effectNotchNode = RenderNode("effectNotch")
        private val effectNode = RenderNode("effect")
        private val blurNode = RenderNode("blur")

        private val whole = RectF()
        private val wholeOptimized = RectF()
        private val blackNodePaint = Paint().apply {
            color = Color.BLACK
            blendMode = BlendMode.SRC_IN
        }

        init {
            setIntensity(15f)
        }

        override fun onSizeChanged(w: Int, h: Int) {}

        override fun setIntensity(intensity: Float) {
            effectNode.setRenderEffect(RenderEffect.createBlurEffect(intensity, intensity, Shader.TileMode.CLAMP))
            effectNotchNode.setRenderEffect(RenderEffect.createBlurEffect(intensity, intensity, Shader.TileMode.CLAMP))
            filter.colorFilter = ColorMatrixColorFilter(floatArrayOf(
                1f, 0f, 0f, 0f, 0f,
                0f, 1f, 0f, 0f, 0f,
                0f, 0f, 1f, 0f, 0f,
                0f, 0f, 0f, 51f, 51f * -125f
            ))
        }

        override fun setBlurIntensity(intensity: Float) {
            if (intensity == 0f) {
                blurNode.setRenderEffect(null)
            } else {
                val b = intensity * this@ProfileGooeyView.intensity / factorMult
                blurNode.setRenderEffect(RenderEffect.createBlurEffect(b, b, Shader.TileMode.DECAL))
            }
        }

        override fun draw(drawer: (Canvas) -> Unit, canvas: Canvas) {
            if (!canvas.isHardwareAccelerated) {
                drawer(canvas)
                return
            }

            whole.set(0f, 0f, width.toFloat(), height.toFloat())
            if (childCount > 0) {
                val child = getChildAt(0)
                val w = child.width * child.scaleX
                val h = child.height * child.scaleY
                val l = child.x
                val t = child.y

                wholeOptimized.set(l, t, l + w, t + h)
                val info = notchInfo
                if (info != null) {
                    wholeOptimized.union(info.bounds)
                }
                wholeOptimized.inset(-dp(20), -dp(20))
                wholeOptimized.intersect(whole)
                wholeOptimized.top = 0f
            } else {
                wholeOptimized.set(whole)
            }
            wholeOptimized.bottom += dp(BLACK_KING_BAR)

            val rw = Math.ceil(wholeOptimized.width().toDouble()).toInt().coerceAtLeast(1)
            val rh = Math.ceil(wholeOptimized.height().toDouble()).toInt().coerceAtLeast(1)
            val left = wholeOptimized.left
            val top = wholeOptimized.top

            node.setPosition(0, 0, rw, rh)
            blurNode.setPosition(0, 0, rw, rh)
            effectNode.setPosition(0, 0, rw, rh)
            effectNotchNode.setPosition(0, 0, rw, rh)
            wholeOptimized.set(0f, 0f, rw.toFloat(), rh.toFloat())

            // 1. Record child into render buffer
            var c = node.beginRecording()
            c.translate(-left, -top)
            val imageAlpha = MathUtils.clamp(((1f - (pullProgress - 0.5f) / 0.5f) * 255).toInt(), 0, 255)
            drawer(c)
            node.endRecording()

            // 2. Blur buffer
            val blurScaleFactor = factorMult / 4f + 1f + blurIntensity * 0.5f * factorMult + (factorMult - 1f) * 2f
            c = blurNode.beginRecording()
            c.scale(1f / blurScaleFactor, 1f / blurScaleFactor, 0f, 0f)
            c.drawRenderNode(node)
            blurNode.endRecording()

            // 3. Effect node with liquid bridge
            val gooScaleFactor = 2f + factorMult
            c = effectNode.beginRecording()
            c.scale(1f / gooScaleFactor, 1f / gooScaleFactor, 0f, 0f)

            val hBridge = MathUtils.clamp(pullProgress / 0.5f, 0f, 1f) * dp(7) * gooScaleFactor
            if (childCount > 0) {
                val child = getChildAt(0)
                val cx = child.x + child.width * child.scaleX / 2f - left
                val cy = child.y + child.height * child.scaleY / 2f + dp(BLACK_KING_BAR) - top
                val r = child.width / 2f * child.scaleX

                path.rewind()
                path.moveTo(cx - r, cy - cos(Math.PI / 4).toFloat() * r)
                path.lineTo(cx, cy - r - hBridge * 0.25f)
                path.lineTo(cx + r, cy - cos(Math.PI / 4).toFloat() * r)
                path.close()
                c.drawPath(path, blackPaint)
            }
            if (imageAlpha > 0) {
                c.drawRenderNode(node)
            }
            effectNode.endRecording()

            // 4. Notch cutout mask node
            c = effectNotchNode.beginRecording()
            c.scale(1f / gooScaleFactor, 1f / gooScaleFactor, 0f, 0f)
            val info = notchInfo
            if (info != null) {
                c.translate(-left, -top)
                c.translate(0f, dp(BLACK_KING_BAR))
                if (info.isLikelyCircle) {
                    val rad = min(info.bounds.width(), info.bounds.height()) / 2f
                    val cy = info.bounds.bottom - info.bounds.width() / 2f
                    c.drawCircle(info.bounds.centerX(), cy, rad, blackPaint)
                } else {
                    c.drawPath(info.path, blackPaint)
                }
            }
            effectNotchNode.endRecording()

            // 5. Draw to canvas
            canvas.save()
            canvas.translate(left, top - dp(BLACK_KING_BAR))
            canvas.scale(gooScaleFactor, gooScaleFactor, 0f, 0f)
            canvas.drawRenderNode(effectNode)
            canvas.drawRenderNode(effectNotchNode)
            canvas.restore()
        }
    }
}
