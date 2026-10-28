package com.eve.app.ui.home

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Rect
import android.content.res.ColorStateList
import androidx.core.widget.ImageViewCompat
import androidx.core.content.ContextCompat
import com.eve.app.designsystem.TelegramPopupSourceBackground
import com.eve.app.theme.TelegramThemeEngine
import android.graphics.drawable.ColorDrawable
import android.view.KeyEvent
import android.view.WindowManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.LinearLayout
import com.eve.app.R
import com.eve.app.databinding.PopupTelegramMenuBinding
import com.eve.app.util.GlassmorphismHelper
import com.eve.app.util.ThemeFrameCoordinator
import com.eve.app.util.ThemeManager
import com.eve.app.util.ThemeSwitchAnimator

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
    // ActionBarMenuItem.java:841-845 sets animation style to 0 whenever
    // ActionBarPopupWindow drives its own source-backed row cascade.
    private var isDismissing = false
    private var anchorViewRef: View? = null
    private fun actionRows(): List<LinearLayout> = listOf(
        binding.cardTheme, binding.menuRowHistory, binding.menuRowBookmarks,
        binding.menuRowMistakes, binding.menuRowTopic, binding.menuRowPyq, binding.menuRowLogout
    )

    init {
        contentView = binding.root
        width = ViewGroup.LayoutParams.WRAP_CONTENT
        height = ViewGroup.LayoutParams.WRAP_CONTENT
        isFocusable = true
        isOutsideTouchable = true
        // ActionBarMenuItem.java:855-863: Telegram popup content owns its
        // keyboard focus, not the Activity IME or an Android window transition.
        inputMethodMode = PopupWindow.INPUT_METHOD_NOT_NEEDED
        softInputMode = WindowManager.LayoutParams.SOFT_INPUT_STATE_UNSPECIFIED
        binding.root.isFocusableInTouchMode = true
        binding.root.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_MENU && event.repeatCount == 0 &&
                event.action == KeyEvent.ACTION_UP && isShowing) {
                dismiss()
                true
            } else {
                false
            }
        }
        // Original Telegram 9-patch supplies its own perimeter shadow.
        elevation = 0f
        setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        setupThemeCard()
        setupListeners()

        setOnDismissListener {
            dismissDim()
            TelegramPopupSourceMotion.cancel(binding.root, actionRows())
            GlassmorphismHelper.removeWindowBlur(binding.root, animate = false)
        }
    }

    private fun setupThemeCard() {
        val isDark = ThemeSwitchAnimator.isDarkMode(context)
        binding.ivThemeIcon.visibility = View.VISIBLE
        if (isDark) {
            binding.ivThemeIcon.setImageResource(R.drawable.telegram_menu_day_mode_24)
            binding.tvThemeTitle.text = "Day Mode"
        } else {
            binding.ivThemeIcon.setImageResource(R.drawable.telegram_menu_night_mode_24)
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
                TelegramPopupSourceMotion.cancel(binding.root, actionRows())
                GlassmorphismHelper.removeWindowBlur(binding.root, animate = false)
                dismissDim()
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
                // A PopupWindow can temporarily steal Activity focus on gesture navigation;
                // waiting for focus inside an onPreDraw callback can deadlock the
                // handoff because focus transfer need not schedule another draw.
                ThemeFrameCoordinator.afterFrame(anchor.rootView, ready = { !popupRoot.isAttachedToWindow && anchor.isAttachedToWindow },
                    valid = { ThemeManager.ownsPopupHandoff(handoffOwner) }) {
                    anchor.removeOnAttachStateChangeListener(detach)
                    if (!ThemeManager.releasePopupHandoff(handoffOwner)) return@afterFrame
                    val activity = context as? Activity
                    if (anchor.isAttachedToWindow && activity != null && !activity.isFinishing &&
                        !activity.isDestroyed &&
                        (activity as? androidx.lifecycle.LifecycleOwner)?.lifecycle?.currentState?.isAtLeast(
                            androidx.lifecycle.Lifecycle.State.RESUMED) != false) {
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
        // Keep the platform PopupWindow from applying a second slide/fade
        // on top of TelegramPopupSourceMotion. The custom 150+16*n entrance
        // and 150ms dismissal retain sole ownership of the popup's motion.
        animationStyle = 0
        exitTransition = null
        anchorViewRef = anchorView
        setupThemeCard()
        applyOriginalMenuRoles()

        // PopupWindow content must be measured inside the current status/nav/IME-safe display frame.
        val frame = Rect()
        anchorView.getWindowVisibleDisplayFrame(frame)
        val gap = (8f * context.resources.displayMetrics.density).toInt().coerceAtLeast(1)
        val maxWidth = (frame.width() - 2 * gap).coerceAtLeast(1)
        val padding = (8f * context.resources.displayMetrics.density).toInt()
        // Original ActionBarMenuItem.java:2467 uses 196dp minimum item width.
        val minRowWidth = (196f * context.resources.displayMetrics.density).toInt()
        val minPopupWidth = minRowWidth + 2 * padding
        val maxHeight = (frame.height() - 2 * gap).coerceAtLeast(1)
        binding.root.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        binding.root.measure(
            View.MeasureSpec.makeMeasureSpec(maxWidth, View.MeasureSpec.AT_MOST),
            View.MeasureSpec.makeMeasureSpec(maxHeight, View.MeasureSpec.AT_MOST)
        )
        val targetWidth = maxOf(binding.root.measuredWidth, minPopupWidth).coerceAtMost(maxWidth)
        binding.root.measure(
            View.MeasureSpec.makeMeasureSpec(targetWidth, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(maxHeight, View.MeasureSpec.AT_MOST)
        )
        width = targetWidth
        height = minOf(binding.root.measuredHeight, maxHeight)

        val anchorLocation = IntArray(2)
        anchorView.getLocationOnScreen(anchorLocation)
        val minLeft = frame.left + gap
        val maxLeft = (frame.right - gap - width).coerceAtLeast(minLeft)
        // ItemOptions.java:1400: X = container.getX() + x + scrimViewBounds.width() - layoutBounds.right
        // Where layoutBounds.right = width - padding. Right edge aligns to anchor right + padding (9-patch shadow).
        val desiredLeft = (anchorLocation[0] + anchorView.width - width + padding).coerceIn(minLeft, maxLeft)
        val xOffset = desiredLeft - anchorLocation[0]

        // DialogsActivity.java:13839: io.setTranslationY(-dp(64)).
        // Scrim height is ~56dp, so translateY = -56dp - 8dp = top of anchor - 8dp padding.
        // showAsDropDown default places popup at anchorLocation[1] + anchorView.height.
        // Offsetting by -anchorView.height - padding places top of popup at anchorLocation[1] - padding,
        // so the internal menu items start exactly at anchorLocation[1] (covering the 3-dot anchor).
        val yOffset = -anchorView.height - padding
        val desiredTop = (anchorLocation[1] + anchorView.height + yOffset).coerceAtLeast(frame.top + gap)
        val effectiveYOffset = desiredTop - (anchorLocation[1] + anchorView.height)

        // Keep the existing circular-reveal handoff; menu body scrolls within the available viewport.
        isClippingEnabled = true
        showAsDropDown(anchorView, xOffset, effectiveYOffset)
        applyDimBehind()

        // Source-backed ActionBarPopupWindow.java:843-913 visible-row cascade.
        // Original background-only backScaleY is not re-created using whole-popup scale.
        TelegramPopupSourceMotion.entrance(binding.root, actionRows())
    }

    /** Upstream DialogsActivity.java:13678 io.setDimAlpha(0x08) & ActionBarPopupWindow.java:787 dimBehind(amount) */
    private fun applyDimBehind() {
        try {
            val root = binding.root.rootView
            val lp = root.layoutParams as? WindowManager.LayoutParams ?: return
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return
            lp.flags = lp.flags or WindowManager.LayoutParams.FLAG_DIM_BEHIND
            lp.dimAmount = 8f / 255f
            wm.updateViewLayout(root, lp)
        } catch (_: Exception) {}
    }

    /** Upstream ActionBarPopupWindow.java:816 dismissDim() */
    private fun dismissDim() {
        try {
            val root = binding.root.rootView
            val lp = root.layoutParams as? WindowManager.LayoutParams ?: return
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return
            if ((lp.flags and WindowManager.LayoutParams.FLAG_DIM_BEHIND) != 0) {
                lp.flags = lp.flags and WindowManager.LayoutParams.FLAG_DIM_BEHIND.inv()
                lp.dimAmount = 0f
                wm.updateViewLayout(root, lp)
            }
        } catch (_: Exception) {}
    }

    /** Source: ActionBarMenuSubItem.java:78-84 and ThemeColors.java:51,203-206. */
    private fun applyOriginalMenuRoles() {
        val palette = TelegramThemeEngine.resolveCurrentPalette(context)
        binding.root.updatePalette(palette)
        // Original ActionBarPopupWindowLayout.java:148-170,294-297.
        val padding = (8f * context.resources.displayMetrics.density).toInt()
        binding.root.setPadding(padding, padding, padding, padding)
        binding.root.clipToOutline = false
        binding.root.drawHairline = false
        binding.root.glassEnabled = false
        // Original ActionBarMenuItem.createPopupLayout():418 uses
        // popup_fixed_alert4 for a normal menu (not the alert2 topView variant).
        val originalNinePatch = ContextCompat.getDrawable(context, R.drawable.telegram_dialog_popup_fixed_alert4)
            ?.mutate()
        originalNinePatch?.colorFilter = PorterDuffColorFilter(
            palette.submenuBackground, PorterDuff.Mode.MULTIPLY
        )
        val sourceBackground = originalNinePatch?.let { TelegramPopupSourceBackground(it) }
        binding.root.originalPopupBackground = sourceBackground
        // Accessibility: one focus target per clickable action, rather than reading the
        // original decorative icon and the label separately. This does not change
        // Telegram artwork, popup motion or any action's existing callback.
        val rows = actionRows()
        for ((index, row) in rows.withIndex()) {
            row.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
            for (index in 0 until row.childCount) {
                when (val child = row.getChildAt(index)) {
                    is android.widget.TextView -> {
                        // Original ActionBarMenuSubItem.java:91-98: a single title
                        // line with END ellipsis. Keep the COMPLETE text on its
                        // 48dp accessibility row even if the visual label truncates.
                        child.setSingleLine(true)
                        child.ellipsize = android.text.TextUtils.TruncateAt.END
                        child.setTextColor(palette.submenuText)
                        row.contentDescription = child.text
                        child.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                    }
                    is android.widget.ImageView -> {
                        ImageViewCompat.setImageTintList(child, ColorStateList.valueOf(palette.submenuIcon))
                        child.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                    }
                }
            }
            // Source: ActionBarPopupWindow.java:604-609. Top and bottom rows
            // have original 6dp corners, while middle rows are square.
            // Remove XML's default foreground to avoid a duplicate ripple.
            row.foreground = null
            row.background = TelegramPopupRadialSelector.create(
                context, palette.submenuSelector,
                if (index == 0) 6 else 0,
                if (index == rows.lastIndex) 6 else 0
            )
        }
    }

    private fun dismissWithAction(action: () -> Unit) {
        if (isDismissing) return
        isDismissing = true
        dismissDim()
        GlassmorphismHelper.removeWindowBlur(binding.root, animate = true)

        // Original ActionBarPopupWindow.java:1024-1069 default -5dp/150ms exit.
        TelegramPopupSourceMotion.exit(binding.root, actionRows()) {
            isDismissing = false
            super.dismiss()
            action()
        }
    }

    /** Lifecycle-only immediate teardown; user-triggered dismissal retains Telegram 150ms exit. */
    fun dismissForHostTeardown() {
        isDismissing = true
        dismissDim()
        TelegramPopupSourceMotion.cancel(binding.root, actionRows())
        GlassmorphismHelper.removeWindowBlur(binding.root, animate = false)
        animationStyle = 0
        exitTransition = null
        if (isShowing) super.dismiss()
        anchorViewRef = null
    }

    override fun dismiss() {
        if (isDismissing) return
        isDismissing = true
        dismissDim()
        GlassmorphismHelper.removeWindowBlur(binding.root, animate = true)

        // Original ActionBarPopupWindow.java:1024-1069 default -5dp/150ms exit.
        TelegramPopupSourceMotion.exit(binding.root, actionRows()) {
            isDismissing = false
            super.dismiss()
        }
    }
}
