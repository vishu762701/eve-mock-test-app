package com.eve.app.ui.home

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.eve.app.R
import com.eve.app.databinding.PopupTelegramMenuBinding
import com.eve.app.util.FastBlurHelper
import com.eve.app.util.GlassmorphismHelper
import com.eve.app.util.ThemeManager
import com.eve.app.util.ThemeSwitchAnimator

class TelegramMenuPopup(
    private val context: Context,
    private val onThemeToggle: (originX: Int, originY: Int, iconWidth: Int, iconHeight: Int) -> Unit,
    private val onHistory: () -> Unit,
    private val onBookmarks: () -> Unit,
    private val onTopic: () -> Unit,
    private val onPyq: () -> Unit,
    private val onLogout: () -> Unit
) : PopupWindow(context) {

    private val binding: PopupTelegramMenuBinding =
        PopupTelegramMenuBinding.inflate(LayoutInflater.from(context))
    private var isDismissing = false
    private var anchorViewRef: View? = null
    private var blurBitmap: Bitmap? = null

    init {
        contentView = binding.root
        width = ViewGroup.LayoutParams.WRAP_CONTENT
        height = ViewGroup.LayoutParams.WRAP_CONTENT
        isFocusable = true
        isOutsideTouchable = true
        elevation = 10f
        setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        setupThemeCard()
        setupListeners()

        setOnDismissListener {
            binding.ivGlassBlurBackground.setImageDrawable(null)
            blurBitmap = null
        }
    }

    private fun setupThemeCard() {
        val isDark = ThemeSwitchAnimator.isDarkMode(context)
        if (isDark) {
            binding.ivThemeIcon.setImageResource(R.drawable.ic_sun)
            binding.tvThemeTitle.text = "Day Mode"
        } else {
            binding.ivThemeIcon.setImageResource(R.drawable.ic_moon)
            binding.tvThemeTitle.text = "Dark Mode"
        }
    }

    private fun setupListeners() {
        binding.ivThemeIcon.setOnClickListener {
            binding.cardTheme.performClick()
        }
        binding.tvThemeTitle.setOnClickListener {
            binding.cardTheme.performClick()
        }

        binding.cardTheme.setOnClickListener { v ->
            try {
                if (ThemeSwitchAnimator.isTransitioning) {
                    android.util.Log.w("ThemeClickDiag", "Ignored click: ThemeSwitchAnimator.isTransitioning is true")
                    return@setOnClickListener
                }
                val isDark = ThemeSwitchAnimator.isDarkMode(context)
                binding.ivThemeIcon.setImageResource(if (isDark) R.drawable.ic_moon else R.drawable.ic_sun)

                // Telegram's exact approach: always use fixed icon position via getLocationInWindow
                val targetIcon: View = anchorViewRef ?: binding.ivThemeIcon
                val pos = IntArray(2)
                targetIcon.getLocationInWindow(pos)
                val iconW = if (targetIcon.measuredWidth > 0) targetIcon.measuredWidth else targetIcon.width
                val iconH = if (targetIcon.measuredHeight > 0) targetIcon.measuredHeight else targetIcon.height
                val cx = pos[0] + iconW / 2
                val cy = pos[1] + iconH / 2

                android.util.Log.d("ThemeClickDiag", "Fixed icon position calculated: cx=$cx, cy=$cy, size=${iconW}x${iconH}")

                GlassmorphismHelper.removeWindowBlur(binding.root, animate = false)
                super.dismiss()
                onThemeToggle(cx, cy, iconW, iconH)
            } catch (t: Throwable) {
                android.util.Log.e("ThemeClickDiag", "EXCEPTION in cardTheme onClick", t)
            }
        }
        binding.menuRowHistory.setOnClickListener {
            dismissWithAction { onHistory() }
        }
        binding.menuRowBookmarks.setOnClickListener {
            dismissWithAction { onBookmarks() }
        }
        binding.menuRowTopic.setOnClickListener {
            dismissWithAction { onTopic() }
        }
        binding.menuRowPyq.setOnClickListener {
            dismissWithAction { onPyq() }
        }
        binding.menuRowLogout.setOnClickListener {
            dismissWithAction { onLogout() }
        }
    }

    fun show(anchorView: View) {
        if (isShowing) return
        isDismissing = false
        anchorViewRef = anchorView
        setupThemeCard()

        // Measure content precisely so PopupWindow height is exact and matches content
        binding.root.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val measuredW = binding.root.measuredWidth
        val measuredH = binding.root.measuredHeight
        width = measuredW
        height = measuredH

        // Align right edge of popup with right edge of 3-dot anchor icon, right beneath it
        val xOffset = -(measuredW - anchorView.width)

        // Capture background slice directly behind popup rect for strong frosted blur
        val activity = (anchorView.context as? Activity) ?: (context as? Activity)
        if (activity != null) {
            try {
                val decor = activity.window.decorView
                if (decor.width > 0 && decor.height > 0) {
                    val anchorLoc = IntArray(2)
                    anchorView.getLocationOnScreen(anchorLoc)
                    val popupX = (anchorLoc[0] + xOffset).coerceIn(0, (decor.width - measuredW).coerceAtLeast(0))
                    val popupY = (anchorLoc[1] + anchorView.height + 4).coerceIn(0, (decor.height - measuredH).coerceAtLeast(0))

                    val scale = 4
                    val sampleW = (measuredW / scale).coerceAtLeast(1)
                    val sampleH = (measuredH / scale).coerceAtLeast(1)
                    val bmp = Bitmap.createBitmap(sampleW, sampleH, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bmp)
                    canvas.scale(1f / scale, 1f / scale)
                    canvas.translate(-popupX.toFloat(), -popupY.toFloat())
                    decor.draw(canvas)

                    val blurred = FastBlurHelper.blur(bmp, radius = 20, canReuseInBitmap = true)
                    blurBitmap = blurred
                    binding.ivGlassBlurBackground.setImageBitmap(blurred)
                }
            } catch (_: Throwable) {
                // Fallback gracefully
            }
        }

        showAsDropDown(anchorView, xOffset, 4)

        // Task C: Apply animated background blur on supported Android versions
        GlassmorphismHelper.applyWindowBlur(binding.root, blurRadius = GlassmorphismHelper.DEFAULT_BLUR_RADIUS, animate = true)

        binding.root.post {
            binding.root.pivotX = binding.root.width.toFloat()
            binding.root.pivotY = 0f
            binding.root.scaleX = 0.85f
            binding.root.scaleY = 0.85f
            binding.root.alpha = 0f

            binding.root.animate()
                .scaleX(1.0f)
                .scaleY(1.0f)
                .alpha(1.0f)
                .setDuration(200)
                .setInterpolator(FastOutSlowInInterpolator())
                .start()
        }
    }

    private fun dismissWithAction(action: () -> Unit) {
        if (isDismissing) return
        isDismissing = true
        GlassmorphismHelper.removeWindowBlur(binding.root, animate = true)

        binding.root.pivotX = binding.root.width.toFloat()
        binding.root.pivotY = 0f
        binding.root.animate()
            .scaleX(0.85f)
            .scaleY(0.85f)
            .alpha(0f)
            .setDuration(150)
            .setInterpolator(FastOutSlowInInterpolator())
            .withEndAction {
                isDismissing = false
                super.dismiss()
                binding.ivGlassBlurBackground.setImageDrawable(null)
                blurBitmap = null
                action()
            }
            .start()
    }

    override fun dismiss() {
        if (isDismissing) return
        isDismissing = true
        GlassmorphismHelper.removeWindowBlur(binding.root, animate = true)

        binding.root.pivotX = binding.root.width.toFloat()
        binding.root.pivotY = 0f
        binding.root.animate()
            .scaleX(0.85f)
            .scaleY(0.85f)
            .alpha(0f)
            .setDuration(150)
            .setInterpolator(FastOutSlowInInterpolator())
            .withEndAction {
                isDismissing = false
                super.dismiss()
                binding.ivGlassBlurBackground.setImageDrawable(null)
                blurBitmap = null
            }
            .start()
    }
}
