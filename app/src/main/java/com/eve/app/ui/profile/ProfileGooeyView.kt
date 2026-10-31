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

    /**
     * Telegram ProfileGooeyView.java:142-254 software path, with the original
     * 6x downsample, dual color-matrix pass and blur/fade composition.
     * Source native stack blur is adapted to Android Bitmap ARGB via ProfileStackBlur.
     */
    private inner class CPUImpl : Impl {
        private var bitmap: Bitmap? = null
        private var bitmapCanvas: Canvas? = null
        private val scaleConst = 6f
        private var optimizedW = 0
        private var optimizedH = 0
        private var bitmapOrigW = 0
        private var bitmapOrigH = 0

        private val bitmapPaint = Paint(
            Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG
        ).apply {
            isFilterBitmap = true
            colorFilter = ColorMatrixColorFilter(floatArrayOf(
                0f, 0f, 0f, 0f, 0f,
                0f, 0f, 0f, 0f, 0f,
                0f, 0f, 0f, 0f, 0f,
                0f, 0f, 0f, 60f, -7500f
            ))
        }
        private val bitmapPaint2 = Paint(
            Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG
        ).apply {
            isFilterBitmap = true
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
        }

        override fun onSizeChanged(w: Int, h: Int) {
            bitmap?.recycle()
            bitmap = null
            bitmapCanvas = null
            optimizedW = min(dp(120).toInt(), w).coerceAtLeast(0)
            optimizedH = min(dp(220).toInt(), h).coerceAtLeast(0)
            bitmapOrigW = optimizedW
            bitmapOrigH = optimizedH + dp(BLACK_KING_BAR).toInt()
            if (bitmapOrigW <= 0 || bitmapOrigH <= 0) return
            val buffer = Bitmap.createBitmap(
                (bitmapOrigW / scaleConst).toInt().coerceAtLeast(1),
                (bitmapOrigH / scaleConst).toInt().coerceAtLeast(1),
                Bitmap.Config.ARGB_8888
            )
            bitmap = buffer
            bitmapCanvas = Canvas(buffer)
        }

        override fun setIntensity(intensity: Float) {}
        override fun setBlurIntensity(intensity: Float) {}

        override fun draw(drawer: (Canvas) -> Unit, canvas: Canvas) {
            val buffer = bitmap
            val offscreen = bitmapCanvas
            if (buffer == null || buffer.isRecycled || offscreen == null) {
                drawer(canvas)
                return
            }

            val transition = (MathUtils.clamp(blurIntensity, 0.2f, 0.3f) - 0.2f) / 0.1f
            val alpha = ((1f - transition) * 255f).toInt()
            val offsetX = (width - optimizedW) / 2f

            canvas.save()
            canvas.translate(0f, -dp(BLACK_KING_BAR))
            if (alpha != 255) {
                buffer.eraseColor(0)
                offscreen.save()
                offscreen.scale(
                    buffer.width.toFloat() / bitmapOrigW,
                    buffer.height.toFloat() / bitmapOrigH
                )
                offscreen.translate(-offsetX, 0f)
                drawer(offscreen)
                offscreen.restore()

                offscreen.save()
                offscreen.scale(
                    buffer.width.toFloat() / bitmapOrigW,
                    buffer.height.toFloat() / bitmapOrigH
                )
                val info = notchInfo
                if (info != null) {
                    offscreen.save()
                    offscreen.translate(-offsetX, dp(BLACK_KING_BAR))
                    if (info.isLikelyCircle) {
                        val rad = min(info.bounds.width(), info.bounds.height()) / 2f
                        offscreen.drawCircle(
                            info.bounds.centerX(),
                            info.bounds.bottom - info.bounds.width() / 2f,
                            rad, blackPaint
                        )
                    } else if (info.isAccurate) {
                        offscreen.drawPath(info.path, blackPaint)
                    } else {
                        val rad = max(info.bounds.width(), info.bounds.height()) / 2f
                        offscreen.drawRoundRect(info.bounds, rad, rad, blackPaint)
                    }
                    offscreen.restore()
                } else {
                    offscreen.drawRect(
                        0f, 0f, optimizedW.toFloat(), dp(BLACK_KING_BAR), blackPaint
                    )
                }
                offscreen.restore()

                ProfileStackBlur.blurBitmap(
                    buffer, (this@ProfileGooeyView.intensity * 2f / scaleConst).toInt()
                )

                canvas.save()
                canvas.translate(offsetX, 0f)
                canvas.saveLayer(
                    0f, 0f, bitmapOrigW.toFloat(), bitmapOrigH.toFloat(), null
                )
                canvas.scale(
                    bitmapOrigW.toFloat() / buffer.width,
                    bitmapOrigH.toFloat() / buffer.height
                )
                canvas.drawBitmap(buffer, 0f, 0f, bitmapPaint)
                canvas.drawBitmap(buffer, 0f, 0f, bitmapPaint2)
                canvas.restore()
                canvas.restore()
            }
            if (alpha != 0) {
                if (alpha != 255) {
                    canvas.saveLayerAlpha(
                        offsetX, 0f, offsetX + optimizedW, optimizedH.toFloat(), alpha
                    )
                }
                drawer(canvas)
                if (alpha != 255) canvas.restore()
            }
            canvas.restore()
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
        private val temp = RectF()
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

            // Telegram ProfileGooeyView.java:342-348: raw avatar buffer and alpha.
            var c = node.beginRecording()
            c.translate(-left, -top)
            val imageAlphaNoClamp = ProfileGooeyMotionMath.imageAlphaUnclamped(pullProgress)
            val imageAlpha = MathUtils.clamp(imageAlphaNoClamp, 0, 255)
            drawer(c)
            node.endRecording()

            // Original shader pipeline: an independent blurred child RenderNode.
            val blurScaleFactor = factorMult / 4f + 1f +
                blurIntensity * 0.5f * factorMult + (factorMult - 1f) * 2f
            c = blurNode.beginRecording()
            c.scale(1f / blurScaleFactor, 1f / blurScaleFactor, 0f, 0f)
            c.drawRenderNode(node)
            blurNode.endRecording()

            // ProfileGooeyView.java:358-390: alpha-mask and reversible liquid bridge.
            val gooScaleFactor = 2f + factorMult
            c = effectNode.beginRecording()
            c.scale(1f / gooScaleFactor, 1f / gooScaleFactor, 0f, 0f)
            if (imageAlpha < 255) {
                c.saveLayer(wholeOptimized, null)
                c.drawRenderNode(node)
                c.drawRect(wholeOptimized, blackNodePaint)
                c.restore()
            }
            val hBridge = ProfileGooeyMotionMath.liquidBridgeHeight(
                pullProgress, dp(7) * gooScaleFactor
            )
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
                if (imageAlpha != 255) c.saveLayerAlpha(wholeOptimized, imageAlpha)
                c.drawRenderNode(node)
                if (imageAlpha != 255) c.restore()
            }
            effectNode.endRecording()

            // ProfileGooeyView.java:392-432: circular / accurate / rounded /
            // notchless bar mask, with the matching liquid cusp.
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

                    path.rewind()
                    path.moveTo(info.bounds.centerX() - hBridge / 2f, cy)
                    path.lineTo(info.bounds.centerX(), cy + rad + hBridge)
                    path.lineTo(info.bounds.centerX() + hBridge / 2f, cy)
                    path.close()
                    c.drawPath(path, blackPaint)
                } else if (info.isAccurate) {
                    c.drawPath(info.path, blackPaint)
                } else {
                    val rad = max(info.bounds.width(), info.bounds.height()) / 2f
                    temp.set(info.bounds)
                    c.drawRoundRect(temp, rad, rad, blackPaint)

                    path.rewind()
                    path.moveTo(temp.centerX() - hBridge / 2f, temp.bottom)
                    path.lineTo(temp.centerX(), temp.bottom + hBridge)
                    path.lineTo(temp.centerX() + hBridge / 2f, temp.bottom)
                    path.close()
                    c.drawPath(path, blackPaint)
                }
            } else {
                c.drawRect(0f, 0f, rw.toFloat(), dp(BLACK_KING_BAR), blackPaint)
                path.rewind()
                path.moveTo((rw - hBridge) / 2f, dp(BLACK_KING_BAR))
                path.lineTo(rw / 2f, dp(BLACK_KING_BAR) + hBridge)
                path.lineTo((rw + hBridge) / 2f, dp(BLACK_KING_BAR))
                path.close()
                c.drawPath(path, blackPaint)
            }
            effectNotchNode.endRecording()

            // ProfileGooeyView.java:434-482: filtered alpha composition and
            // blurred image fade; a bare drawRenderNode loses the source effect.
            canvas.save()
            canvas.translate(left, top - dp(BLACK_KING_BAR))
            if (info != null) {
                canvas.clipRect(0f, info.bounds.top, rw.toFloat(), rh.toFloat())
            }
            canvas.saveLayer(wholeOptimized, filter)
            canvas.scale(gooScaleFactor, gooScaleFactor)
            canvas.drawRenderNode(effectNotchNode)
            canvas.drawRenderNode(effectNode)
            canvas.restore()

            val blurImageAlpha = MathUtils.clamp(imageAlphaNoClamp * 3 / 4, 0, 255)
            if (blurImageAlpha < 255) {
                canvas.saveLayer(wholeOptimized, null)
                if (blurIntensity != 0f) {
                    canvas.saveLayer(wholeOptimized, filter)
                    canvas.scale(blurScaleFactor, blurScaleFactor)
                    canvas.drawRenderNode(blurNode)
                    canvas.restore()
                } else {
                    canvas.drawRenderNode(node)
                }
                canvas.drawRect(wholeOptimized, blackNodePaint)
                canvas.restore()
            }
            if (blurImageAlpha > 0) {
                if (blurImageAlpha != 255) canvas.saveLayerAlpha(wholeOptimized, blurImageAlpha)
                if (blurIntensity != 0f) {
                    canvas.saveLayer(wholeOptimized, filter)
                    canvas.scale(blurScaleFactor, blurScaleFactor)
                    canvas.drawRenderNode(blurNode)
                    canvas.restore()
                } else {
                    canvas.drawRenderNode(node)
                }
                if (blurImageAlpha != 255) canvas.restore()
            }
            canvas.restore()
        }
    }
}
