package com.eve.app.ui.home

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import com.eve.app.R
import com.eve.app.databinding.PopupTelegramMenuBinding
import com.eve.app.util.FastBlurHelper
import com.eve.app.util.GlassmorphismHelper
import com.eve.app.util.ThemeFrameCoordinator
import com.eve.app.util.ThemeManager
import com.eve.app.util.ThemeSwitchAnimator
import com.eve.app.util.TelegramPopupHelper

class TelegramMenuPopup(
    private val context: Context,
    private val onThemeToggle: (originX: Int, originY: Int, iconWidth: Int, iconHeight: Int) -> Unit,
    private val onHistory: () -> Unit,
    private val onBookmarks: () -> Unit,
    private val onMistakes: () -> Unit = {},
    private val onTopic: () -> Unit,
    private val onPyq: () -> Unit,
    private val onLogout: () -> Unit
) : PopupWindow(context) {

    private val binding: PopupTelegramMenuBinding =
        PopupTelegramMenuBinding.inflate(LayoutInflater.from(context))
    private val ordinaryAnimationStyle = animationStyle
    private val ordinaryExitTransition = exitTransition
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
            TelegramPopupHelper.cancel(binding.root)
            GlassmorphismHelper.removeWindowBlur(binding.root, animate = false)
            binding.ivGlassBlurBackground.setImageDrawable(null)
            blurBitmap = null
        }
    }

    private fun setupThemeCard() {
        val isDark = ThemeSwitchAnimator.isDarkMode(context)
        binding.ivThemeIcon.visibility = View.VISIBLE
        if (isDark) {
            binding.ivThemeIcon.setImageResource(R.drawable.ic_theme_sun)
            binding.tvThemeTitle.text = "Day Mode"
        } else {
            binding.ivThemeIcon.setImageResource(R.drawable.ic_theme_moon)
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

        binding.cardTheme.setOnClickListener {
            try {
                if (isDismissing || !isShowing || ThemeSwitchAnimator.isTransitioning) {
                    android.util.Log.w("ThemeClickDiag", "Ignored click: ThemeSwitchAnimator.isTransitioning is true")
                    return@setOnClickListener
                }
                val anchor = anchorViewRef
                if (anchor == null) {
                    android.util.Log.e("ThemeOrigin", "anchorViewRef is null, cannot start theme reveal")
                    return@setOnClickListener
                }
                val loc = IntArray(2)
                anchor.getLocationOnScreen(loc)
                val iconW = if (anchor.measuredWidth > 0) anchor.measuredWidth else anchor.width
                val iconH = if (anchor.measuredHeight > 0) anchor.measuredHeight else anchor.height
                val cx = loc[0] + iconW / 2
                val cy = loc[1] + iconH / 2

                val handoffOwner = ThemeManager.reservePopupHandoff() ?: return@setOnClickListener
                isDismissing = true
                val handoffId = ThemeManager.activeTransitionId + 1
                ThemeManager.trace("tap_accepted", anchor, handoffId)
                TelegramPopupHelper.cancel(binding.root)
                GlassmorphismHelper.removeWindowBlur(binding.root, animate = false)
                val popupRoot = binding.root.rootView
                animationStyle = 0
                exitTransition = null
                super.dismiss()
                ThemeManager.trace("popup_work_cancelled", anchor, handoffId)
                // PopupWindow removes its window immediately; the host must submit a
                // clean frame after that detach before ThemeManager requests PixelCopy.
                val detach = object : View.OnAttachStateChangeListener {
                    override fun onViewAttachedToWindow(v: View) {}
                    override fun onViewDetachedFromWindow(v: View) {
                        ThemeManager.releasePopupHandoff(handoffOwner)
                        v.removeOnAttachStateChangeListener(this)
                    }
                }
                anchor.addOnAttachStateChangeListener(detach)
                ThemeFrameCoordinator.afterFrame(anchor.rootView, ready = { !popupRoot.isAttachedToWindow },
                    valid = { ThemeManager.ownsPopupHandoff(handoffOwner) }) {
                    anchor.removeOnAttachStateChangeListener(detach)
                    if (!ThemeManager.releasePopupHandoff(handoffOwner)) return@afterFrame
                    val activity = context as? Activity
                    if (anchor.isAttachedToWindow && activity != null && !activity.isFinishing &&
                        !activity.isDestroyed && activity.hasWindowFocus()) {
                        ThemeManager.trace("clean_host_frame", anchor, handoffId)
                        onThemeToggle(cx, cy, iconW, iconH)
                    }
                }
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
        binding.menuRowMistakes.setOnClickListener {
            dismissWithAction { onMistakes() }
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
        animationStyle = ordinaryAnimationStyle
        exitTransition = ordinaryExitTransition
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

        // Upgrade 7: Telegram-style anchored scale+fade popup entrance (ActionBarPopupWindow pattern)
        TelegramPopupHelper.animateEntrance(binding.root, anchorView)
    }

    private fun dismissWithAction(action: () -> Unit) {
        if (isDismissing) return
        isDismissing = true
        GlassmorphismHelper.removeWindowBlur(binding.root, animate = true)

        // Upgrade 7: Telegram-style reverse scale+fade popup exit
        TelegramPopupHelper.animateExit(binding.root) {
            isDismissing = false
            super.dismiss()
            binding.ivGlassBlurBackground.setImageDrawable(null)
            blurBitmap = null
            action()
        }
    }

    override fun dismiss() {
        if (isDismissing) return
        isDismissing = true
        GlassmorphismHelper.removeWindowBlur(binding.root, animate = true)

        // Upgrade 7: Telegram-style reverse scale+fade popup exit
        TelegramPopupHelper.animateExit(binding.root) {
            isDismissing = false
            super.dismiss()
            binding.ivGlassBlurBackground.setImageDrawable(null)
            blurBitmap = null
        }
    }
}
