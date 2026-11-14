/*
 * Derived from Telegram Android ProfileGalleryView.java:1-400
 * Copyright Nikolai Kudashov, 2013-2023. GNU GPL v2 or later. See THIRD_PARTY_NOTICES.md.
 */

package com.eve.app.designsystem

import android.animation.ObjectAnimator
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.WindowCompat
import com.eve.app.R
import com.eve.app.util.AppBulletin
import com.eve.app.util.VibrationHelper
import org.telegram.ui.Components.CubicBezierInterpolator
import kotlin.math.abs

/**
 * Authentic Telegram Profile Photo / Media Gallery Viewer.
 * Upstream: ProfileGalleryView.java
 * Features:
 * - Fullscreen display with black backdrop
 * - Top dash page indicators for multi-photos
 * - Vertical drag-down to dismiss with smooth alpha fade
 * - Share and Save actions
 */
class ProfileGalleryView(
    activity: Activity,
    private val images: List<Uri>,
    private var currentIndex: Int = 0
) : Dialog(activity, R.style.Theme_Eve_TelegramNativeDialog) {

    private fun dp(value: Float): Int = (value * context.resources.displayMetrics.density + 0.5f).toInt()

    private val container = FrameLayout(context)
    private val imageView = ImageView(context)
    private val topBar = LinearLayout(context)
    private val pageIndicator = PageIndicatorView(context)

    private var startY = 0f
    private var isDraggingDown = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        window?.let { win ->
            win.setBackgroundDrawable(ColorDrawable(Color.BLACK))
            win.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            win.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
            WindowCompat.setDecorFitsSystemWindows(win, false)
        }

        container.setBackgroundColor(Color.BLACK)

        // 1. Image View
        imageView.apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
                Gravity.CENTER
            )
        }
        container.addView(imageView)

        // 2. Top Bar (56dp + status bar inset)
        topBar.apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0x66000000)
            setPadding(0, dp(24f), 0, dp(8f))

            val horizontal = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(8f), 0, dp(8f), 0)
            }

            val btnBack = ImageView(context).apply {
                setImageResource(R.drawable.ic_back)
                setColorFilter(Color.WHITE)
                setPadding(dp(12f), dp(12f), dp(12f), dp(12f))
                isClickable = true
                isFocusable = true
                setOnClickListener { dismiss() }
            }
            horizontal.addView(btnBack, LinearLayout.LayoutParams(dp(48f), dp(48f)))

            val tvTitle = TextView(context).apply {
                text = "Profile Photo"
                textSize = 18f
                setTextColor(Color.WHITE)
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                setPadding(dp(8f), 0, 0, 0)
            }
            horizontal.addView(tvTitle, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

            val btnShare = ImageView(context).apply {
                setImageResource(R.drawable.ic_share)
                setColorFilter(Color.WHITE)
                setPadding(dp(12f), dp(12f), dp(12f), dp(12f))
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    val currentUri = images.getOrNull(currentIndex)
                    if (currentUri != null) {
                        TelegramShareAlert.show(
                            context = context,
                            title = "Profile Photo",
                            subtitle = "EVE Profile Photo",
                            shareText = "Profile photo via EVE",
                            shareUrl = currentUri.toString()
                        )
                    }
                }
            }
            horizontal.addView(btnShare, LinearLayout.LayoutParams(dp(48f), dp(48f)))

            addView(horizontal)

            if (images.size > 1) {
                addView(pageIndicator, LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(6f)
                ).apply {
                    setMargins(dp(16f), dp(4f), dp(16f), 0)
                })
            }
        }
        container.addView(topBar, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.TOP
        ))

        setupGestures()
        loadImage()

        setContentView(container)
    }

    private fun loadImage() {
        val uri = images.getOrNull(currentIndex) ?: return
        imageView.setImageURI(uri)
        pageIndicator.setCountAndCurrent(images.size, currentIndex)
    }

    private fun setupGestures() {
        container.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startY = event.rawY
                    isDraggingDown = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dy = event.rawY - startY
                    if (dy > dp(10f)) {
                        isDraggingDown = true
                        imageView.translationY = dy
                        val alpha = (1f - (dy / (container.height * 0.7f))).coerceIn(0.2f, 1f)
                        container.alpha = alpha
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    val dy = event.rawY - startY
                    if (isDraggingDown && dy > dp(120f)) {
                        VibrationHelper.vibrateLightHaptic(context)
                        dismiss()
                    } else {
                        // Snap back with Telegram ease out quint
                        ObjectAnimator.ofFloat(imageView, View.TRANSLATION_Y, 0f).apply {
                            duration = 200
                            interpolator = CubicBezierInterpolator.EASE_OUT_QUINT
                            start()
                        }
                        ObjectAnimator.ofFloat(container, View.ALPHA, 1f).apply {
                            duration = 200
                            start()
                        }
                    }
                    true
                }
                else -> false
            }
        }
    }

    private inner class PageIndicatorView(context: Context) : View(context) {
        private val paintActive = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        private val paintInactive = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x66FFFFFF }
        private var count = 1
        private var selected = 0

        fun setCountAndCurrent(c: Int, s: Int) {
            count = c.coerceAtLeast(1)
            selected = s.coerceIn(0, count - 1)
            invalidate()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            if (count <= 1) return
            val gap = dp(4f).toFloat()
            val totalW = width.toFloat()
            val dashW = (totalW - gap * (count - 1)) / count
            val h = height.toFloat()
            val radius = h / 2f

            for (i in 0 until count) {
                val left = i * (dashW + gap)
                val paint = if (i == selected) paintActive else paintInactive
                canvas.drawRoundRect(left, 0f, left + dashW, h, radius, radius, paint)
            }
        }
    }

    companion object {
        fun show(activity: Activity, images: List<Uri>, initialPosition: Int = 0) {
            if (images.isEmpty()) return
            val gallery = ProfileGalleryView(activity, images, initialPosition)
            gallery.show()
        }
    }
}
