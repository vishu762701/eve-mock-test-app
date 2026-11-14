/*
 * Derived from Telegram Android StorageUsageView.java:1-350
 * Copyright Nikolai Kudashov, 2013-2023. GNU GPL v2 or later. See THIRD_PARTY_NOTICES.md.
 */

package com.eve.app.designsystem

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.eve.app.theme.TelegramThemeEngine
import com.eve.app.util.CacheManager
import com.eve.app.util.CubicBezierInterpolator

/**
 * Authentic Telegram Storage Diagram View.
 * Displays app cache, other apps/system usage, and free space in an animated segmented bar.
 * Upstream: StorageUsageView.java
 */
class StorageUsageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private fun dp(value: Float): Int = (value * resources.displayMetrics.density + 0.5f).toInt()

    private val progressView = ProgressBarView(context)
    private val legendLayout = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }

    private val dotEve = View(context)
    private val tvEve = TextView(context)
    private val dotOther = View(context)
    private val tvOther = TextView(context)
    private val dotFree = View(context)
    private val tvFree = TextView(context)

    init {
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20f), dp(16f), dp(20f), dp(16f))
        }

        root.addView(progressView, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(24f)
        ))

        setupLegend()
        val legendParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = dp(12f)
        }
        root.addView(legendLayout, legendParams)

        addView(root, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
    }

    private fun setupLegend() {
        val palette = TelegramThemeEngine.resolveCurrentPalette(context)

        // Eve App item
        val eveContainer = createLegendItem(dotEve, tvEve, palette.accent, "Eve Cache: 0 B")
        legendLayout.addView(eveContainer, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        // Other apps item
        val otherContainer = createLegendItem(dotOther, tvOther, 0xFF8699AA.toInt(), "Other: 0 B")
        legendLayout.addView(otherContainer, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        // Free space item
        val freeContainer = createLegendItem(dotFree, tvFree, 0xFF4CAF50.toInt(), "Free: 0 B")
        legendLayout.addView(freeContainer, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
    }

    private fun createLegendItem(dot: View, tv: TextView, color: Int, initialText: String): LinearLayout {
        val palette = TelegramThemeEngine.resolveCurrentPalette(context)
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        dot.background = android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.OVAL
            setColor(color)
            setSize(dp(8f), dp(8f))
        }
        container.addView(dot, LinearLayout.LayoutParams(dp(8f), dp(8f)))

        tv.apply {
            text = initialText
            textSize = 12f
            setTextColor(palette.textSecondary)
            setPadding(dp(6f), 0, 0, 0)
            maxLines = 1
        }
        container.addView(tv, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ))

        return container
    }

    fun setStorageData(appCacheSize: Long, deviceFreeSpace: Long, deviceTotalSpace: Long) {
        val total = deviceTotalSpace.coerceAtLeast(1L)
        val otherUsed = (total - deviceFreeSpace - appCacheSize).coerceAtLeast(0L)

        val appRatio = (appCacheSize.toFloat() / total).coerceIn(0.005f, 1f)
        val otherRatio = ((appCacheSize + otherUsed).toFloat() / total).coerceIn(appRatio, 1f)

        progressView.setProgress(appRatio, otherRatio)

        tvEve.text = "Eve: ${CacheManager.formatFileSize(appCacheSize)}"
        tvOther.text = "Other: ${CacheManager.formatFileSize(otherUsed)}"
        tvFree.text = "Free: ${CacheManager.formatFileSize(deviceFreeSpace)}"
    }

    private inner class ProgressBarView(context: Context) : View(context) {

        private val paintTrack = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            strokeCap = Paint.Cap.ROUND
        }
        private val paintOther = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            strokeCap = Paint.Cap.ROUND
        }
        private val paintApp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            strokeCap = Paint.Cap.ROUND
        }

        private var appProgress = 0f
        private var otherProgress = 0f

        private var targetAppProgress = 0f
        private var targetOtherProgress = 0f

        private var animator: ValueAnimator? = null

        fun setProgress(app: Float, other: Float) {
            targetAppProgress = app
            targetOtherProgress = other

            animator?.cancel()
            val startApp = appProgress
            val startOther = otherProgress

            animator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 450
                interpolator = CubicBezierInterpolator.EASE_OUT_QUINT
                addUpdateListener { va ->
                    val f = va.animatedFraction
                    appProgress = startApp + (targetAppProgress - startApp) * f
                    otherProgress = startOther + (targetOtherProgress - startOther) * f
                    invalidate()
                }
                start()
            }
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val palette = TelegramThemeEngine.resolveCurrentPalette(context)
            val strokeH = dp(8f).toFloat()
            val radius = strokeH / 2f
            val y = height / 2f
            val left = dp(4f).toFloat()
            val right = (width - dp(4f)).toFloat()
            val totalW = right - left

            // 1. Background track (Free space)
            paintTrack.color = palette.hairline
            paintTrack.strokeWidth = strokeH
            canvas.drawLine(left + radius, y, right - radius, y, paintTrack)

            // 2. Other apps segment
            if (otherProgress > 0) {
                paintOther.color = 0xFF8699AA.toInt()
                paintOther.strokeWidth = strokeH
                val otherRight = (left + totalW * otherProgress).coerceIn(left + radius, right - radius)
                canvas.drawLine(left + radius, y, otherRight, y, paintOther)
            }

            // 3. Eve App segment
            if (appProgress > 0) {
                paintApp.color = palette.accent
                paintApp.strokeWidth = strokeH
                val appRight = (left + totalW * appProgress).coerceIn(left + radius, right - radius)
                canvas.drawLine(left + radius, y, appRight, y, paintApp)
            }
        }
    }
}
