/*
 * Telegram: TMessagesProj/src/main/java/org/telegram/ui/ActionBar/BottomSheet.java @ 3de37c8cfb039452be7398909461da6288babf15
 * Copyright Nikolai Kudashov, 2013-2018. GNU GPL v2 or later. See THIRD_PARTY_NOTICES.md.
 */

package com.eve.app.designsystem

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.Window
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.eve.app.R
import com.eve.app.theme.TelegramThemeApplier
import com.eve.app.theme.TelegramThemeEngine
import com.eve.app.util.FontManager
import org.telegram.ui.Components.CubicBezierInterpolator

/**
 * Authentic Telegram BottomSheet host port.
 * Upstream: TMessagesProj/src/main/java/org/telegram/ui/ActionBar/BottomSheet.java
 * - Extends android.app.Dialog (no Material BottomSheetDialog dependency)
 * - Container: sheet_shadow_round shape at 20dp corners
 * - Dim alpha: 51 / 255 (0.2)
 * - Open animation: 400ms EASE_OUT_QUINT (20ms delay)
 * - Drag-to-dismiss: 320ms dismiss animation, 250ms snap-back
 * - Normal dismiss: 180ms
 * - Tap-outside dismissal
 * - Insets handling for system navigation bar and keyboard
 * - Optional title (normal 16sp / big 20sp) and 48dp BottomSheetCell rows
 */
open class TelegramBottomSheet(
    context: Context,
    private val needCustomView: Boolean = false
) : Dialog(context, R.style.Theme_Eve_TelegramNativeDialog) {

    private val density = context.resources.displayMetrics.density
    private fun dp(value: Float): Int = (value * density + 0.5f).toInt()

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val palette = TelegramThemeEngine.resolveCurrentPalette(context)

    // Upstream dimBehindAlpha = 51 (BottomSheet.java:214)
    private val dimBehindAlpha = 51
    private val backDrawable = SheetBackDrawable()

    // Upstream open timing: 400ms EASE_OUT_QUINT (BottomSheet.java:210-211)
    private val openDuration = 400L
    private val openInterpolator = CubicBezierInterpolator.EASE_OUT_QUINT

    private var currentAnimation: AnimatorSet? = null
    private var isDismissed = false
    private var isOpening = false

    private val container: ContainerView = ContainerView(context)
    val sheetContainer: FrameLayout = FrameLayout(context)
    private val contentLayout: LinearLayout = LinearLayout(context)

    private val itemViews = mutableListOf<BottomSheetCell>()
    private var titleView: TextView? = null

    init {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window?.let { win ->
            win.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            win.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            win.setGravity(Gravity.BOTTOM or Gravity.FILL_HORIZONTAL)
            win.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
            win.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        }

        contentLayout.orientation = LinearLayout.VERTICAL

        // Authentic Telegram sheet_shadow_round 9-patch
        val shadow = AppCompatResources.getDrawable(context, R.drawable.sheet_shadow_round)?.mutate()
        val surfaceColor = if (palette.isDark) 0xFF262626.toInt() else palette.dialogSurface
        shadow?.colorFilter = PorterDuffColorFilter(surfaceColor, PorterDuff.Mode.MULTIPLY)

        sheetContainer.background = shadow
        val pad = Rect()
        shadow?.getPadding(pad)
        sheetContainer.setPadding(pad.left, pad.top, pad.right, pad.bottom)

        if (contentLayout.parent == null) {
            sheetContainer.addView(contentLayout, FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ))
        }

        val sheetLp = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        )
        // Responsive maxWidth: on tablets cap at 640dp
        val maxSheetWidth = dp(640f)
        val screenWidth = context.resources.displayMetrics.widthPixels
        if (screenWidth > maxSheetWidth) {
            sheetLp.width = maxSheetWidth
        }

        container.addView(sheetContainer, sheetLp)
        super.setContentView(container, ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ))
    }

    override fun setContentView(view: View) {
        if (view === container) {
            super.setContentView(view)
        } else {
            setCustomView(view)
        }
    }

    override fun setContentView(view: View, params: ViewGroup.LayoutParams?) {
        if (view === container) {
            super.setContentView(view, params)
        } else {
            setCustomView(view)
        }
    }

    override fun show() {
        super.show()
        startOpenAnimation()
    }

    override fun dismiss() {
        if (isDismissed) return
        isDismissed = true
        cancelAnimation()

        // Upstream normal dismiss: 180ms (BottomSheet.java:1902)
        val anim = ObjectAnimator.ofFloat(
            sheetContainer,
            View.TRANSLATION_Y,
            sheetContainer.height.toFloat().coerceAtLeast(dp(200f).toFloat())
        )
        val dimAnim = ValueAnimator.ofInt(backDrawable.alpha, 0)
        dimAnim.addUpdateListener {
            backDrawable.alpha = it.animatedValue as Int
            container.invalidate()
        }

        val set = AnimatorSet()
        set.playTogether(anim, dimAnim)
        set.duration = 180L
        set.interpolator = CubicBezierInterpolator.EASE_OUT
        set.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                try {
                    super@TelegramBottomSheet.dismiss()
                } catch (_: Exception) {}
            }
        })
        currentAnimation = set
        set.start()
    }

    private fun startOpenAnimation() {
        isOpening = true
        backDrawable.alpha = 0
        sheetContainer.post {
            val sheetHeight = sheetContainer.height.toFloat().coerceAtLeast(dp(200f).toFloat())
            sheetContainer.translationY = sheetHeight

            val anim = ObjectAnimator.ofFloat(sheetContainer, View.TRANSLATION_Y, 0f)
            val dimAnim = ValueAnimator.ofInt(0, dimBehindAlpha)
            dimAnim.addUpdateListener {
                backDrawable.alpha = it.animatedValue as Int
                container.invalidate()
            }

            val set = AnimatorSet()
            set.playTogether(anim, dimAnim)
            set.duration = openDuration
            set.startDelay = 20L // Telegram 20ms delay
            set.interpolator = openInterpolator
            set.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    isOpening = false
                    currentAnimation = null
                }
            })
            currentAnimation = set
            set.start()
        }
    }

    private fun cancelAnimation() {
        currentAnimation?.cancel()
        currentAnimation = null
    }

    fun setTitle(title: CharSequence?, isBig: Boolean = false) {
        if (title == null) {
            titleView?.let { contentLayout.removeView(it) }
            titleView = null
            return
        }
        val tv = titleView ?: TextView(context).also {
            it.tag = "telegram_bottom_sheet_title"
            it.gravity = Gravity.START or Gravity.CENTER_VERTICAL
            it.setPadding(dp(21f), 0, dp(21f), 0)
            it.isSingleLine = true
            it.ellipsize = TextUtils.TruncateAt.END
            contentLayout.addView(it, 0, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(if (isBig) 56f else 50f)
            ))
            titleView = it
        }
        tv.text = title
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, if (isBig) 20f else 16f)
        tv.typeface = FontManager.telegramMediumTypeface(context)
        tv.setTextColor(if (isBig) (if (palette.isDark) Color.WHITE else palette.textPrimary) else palette.textSecondary)
    }

    fun setCustomView(view: View) {
        (view.parent as? ViewGroup)?.removeView(view)
        TelegramThemeApplier.applyToViewTree(view, palette)
        contentLayout.addView(view, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ))
    }

    fun setItemText(index: Int, text: CharSequence) {
        if (index in itemViews.indices) {
            itemViews[index].setText(text)
        }
    }

    fun setItemColor(index: Int, color: Int) {
        if (index in itemViews.indices) {
            itemViews[index].setTextColor(color)
            itemViews[index].setIconColor(color)
        }
    }

    /**
     * Upstream SheetBackDrawable (BottomSheet.java:135-192).
     * Draws solid black scrim at dimBehindAlpha (51 / 255.0 = 0.2).
     */
    private inner class SheetBackDrawable : Drawable() {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)
            color = Color.BLACK
            alpha = 0
        }

        override fun draw(canvas: Canvas) {
            if (paint.alpha > 0) {
                canvas.drawRect(bounds, paint)
            }
        }

        override fun setAlpha(alpha: Int) {
            paint.alpha = alpha.coerceIn(0, 255)
            invalidateSelf()
        }

        override fun getAlpha(): Int = paint.alpha

        override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) {
            paint.colorFilter = colorFilter
            invalidateSelf()
        }

        override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
    }

    /**
     * Upstream ContainerView (BottomSheet.java:286-630).
     * Handles background drawing, touch outside dismissal, and drag-to-dismiss gestures.
     */
    private inner class ContainerView(context: Context) : FrameLayout(context) {

        private var velocityTracker: VelocityTracker? = null
        private var startY = 0f
        private var isTracking = false

        init {
            setWillNotDraw(false)
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            backDrawable.setBounds(0, 0, width, height)
            backDrawable.draw(canvas)
        }

        override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
            if (isDismissed || isOpening) return false
            when (ev.action) {
                MotionEvent.ACTION_DOWN -> {
                    startY = ev.y
                    isTracking = false
                    // If tap is outside the sheetContainer, intercept for outside-dismiss
                    val location = IntArray(2)
                    sheetContainer.getLocationOnScreen(location)
                    if (ev.rawY < location[1]) {
                        return true
                    }
                }
                MotionEvent.ACTION_MOVE -> {
                    val dy = ev.y - startY
                    if (dy > touchSlop) {
                        isTracking = true
                        return true
                    }
                }
            }
            return super.onInterceptTouchEvent(ev)
        }

        override fun onTouchEvent(ev: MotionEvent): Boolean {
            if (isDismissed || isOpening) return false

            if (velocityTracker == null) {
                velocityTracker = VelocityTracker.obtain()
            }
            velocityTracker?.addMovement(ev)

            when (ev.action) {
                MotionEvent.ACTION_DOWN -> {
                    startY = ev.y
                    val location = IntArray(2)
                    sheetContainer.getLocationOnScreen(location)
                    if (ev.rawY < location[1]) {
                        return true
                    }
                }
                MotionEvent.ACTION_MOVE -> {
                    val dy = ev.y - startY
                    if (!isTracking && dy > touchSlop) {
                        isTracking = true
                    }
                    if (isTracking && dy > 0) {
                        sheetContainer.translationY = dy
                        // Scale back scrim with drag
                        val progress = (1f - (dy / sheetContainer.height.toFloat().coerceAtLeast(1f))).coerceIn(0f, 1f)
                        backDrawable.alpha = (progress * dimBehindAlpha).toInt()
                        invalidate()
                    }
                }
                MotionEvent.ACTION_UP -> {
                    val location = IntArray(2)
                    sheetContainer.getLocationOnScreen(location)
                    if (!isTracking && ev.rawY < location[1]) {
                        dismiss()
                        return true
                    }

                    if (isTracking) {
                        velocityTracker?.computeCurrentVelocity(1000)
                        val velY = velocityTracker?.yVelocity ?: 0f
                        val translationY = sheetContainer.translationY
                        val threshold = dp(64f).toFloat()

                        if (translationY > threshold || velY > 1500f) {
                            // Upstream drag dismiss: 320ms EASE_OUT_QUINT (BottomSheet.java:510)
                            animateDragDismiss()
                        } else {
                            // Upstream snap back: 250ms (BottomSheet.java:413)
                            animateSnapBack()
                        }
                    }
                    velocityTracker?.recycle()
                    velocityTracker = null
                    isTracking = false
                }
                MotionEvent.ACTION_CANCEL -> {
                    if (isTracking) {
                        animateSnapBack()
                    }
                    velocityTracker?.recycle()
                    velocityTracker = null
                    isTracking = false
                }
            }
            return true
        }

        private fun animateDragDismiss() {
            isDismissed = true
            cancelAnimation()
            val sheetHeight = sheetContainer.height.toFloat().coerceAtLeast(dp(200f).toFloat())
            val anim = ObjectAnimator.ofFloat(sheetContainer, View.TRANSLATION_Y, sheetHeight)
            val dimAnim = ValueAnimator.ofInt(backDrawable.alpha, 0)
            dimAnim.addUpdateListener {
                backDrawable.alpha = it.animatedValue as Int
                invalidate()
            }

            val set = AnimatorSet()
            set.playTogether(anim, dimAnim)
            set.duration = 320L // Telegram 320ms drag dismiss
            set.interpolator = CubicBezierInterpolator.EASE_OUT_QUINT
            set.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    try {
                        super@TelegramBottomSheet.dismiss()
                    } catch (_: Exception) {}
                }
            })
            currentAnimation = set
            set.start()
        }

        private fun animateSnapBack() {
            cancelAnimation()
            val anim = ObjectAnimator.ofFloat(sheetContainer, View.TRANSLATION_Y, 0f)
            val dimAnim = ValueAnimator.ofInt(backDrawable.alpha, dimBehindAlpha)
            dimAnim.addUpdateListener {
                backDrawable.alpha = it.animatedValue as Int
                invalidate()
            }

            val set = AnimatorSet()
            set.playTogether(anim, dimAnim)
            set.duration = 250L // Telegram 250ms snap back
            set.interpolator = CubicBezierInterpolator.DEFAULT
            currentAnimation = set
            set.start()
        }
    }

    /**
     * BottomSheet.BottomSheetCell (BottomSheet.java:363-440).
     * 48dp height, 24dp icon, 16sp text, ripple feedback.
     */
    class BottomSheetCell(
        context: Context,
        palette: TelegramThemeEngine.ResolvedPalette
    ) : FrameLayout(context) {

        private fun dp(value: Float): Int = (value * context.resources.displayMetrics.density + 0.5f).toInt()

        private val textView = TextView(context).apply {
            setLines(1)
            isSingleLine = true
            gravity = Gravity.CENTER_VERTICAL
            ellipsize = TextUtils.TruncateAt.END
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTextColor(if (palette.isDark) Color.WHITE else palette.textPrimary)
            typeface = FontManager.telegramRegularTypeface(context)
        }

        private val imageView = ImageView(context).apply {
            scaleType = ImageView.ScaleType.CENTER
            colorFilter = PorterDuffColorFilter(palette.textSecondary, PorterDuff.Mode.MULTIPLY)
        }

        init {
            background = TelegramThemeSelectors.getSelectorDrawable(palette.actionSelector)
            setPadding(dp(16f), 0, dp(16f), 0)
            isClickable = true
            isFocusable = true

            addView(imageView, LayoutParams(dp(24f), dp(24f), Gravity.START or Gravity.CENTER_VERTICAL))
            addView(textView, LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.START or Gravity.CENTER_VERTICAL
            ))
        }

        fun setTextAndIcon(text: CharSequence?, @DrawableRes iconRes: Int) {
            textView.text = text
            val lp = textView.layoutParams as LayoutParams
            if (iconRes != 0) {
                imageView.setImageDrawable(ContextCompat.getDrawable(context, iconRes))
                imageView.visibility = VISIBLE
                lp.marginStart = dp(56f)
            } else {
                imageView.visibility = INVISIBLE
                lp.marginStart = 0
            }
            textView.layoutParams = lp
        }

        fun setText(text: CharSequence?) {
            textView.text = text
        }

        fun setTextColor(color: Int) {
            textView.setTextColor(color)
        }

        fun setIconColor(color: Int) {
            imageView.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.MULTIPLY)
        }
    }

    class Builder(private val context: Context) {
        private var title: CharSequence? = null
        private var isBigTitle: Boolean = false
        private var items: Array<out CharSequence>? = null
        private var icons: IntArray? = null
        private var onClickListener: DialogInterface.OnClickListener? = null
        private var customView: View? = null
        private var cancelable: Boolean = true
        private var dismissListener: DialogInterface.OnDismissListener? = null
        private var cancelListener: DialogInterface.OnCancelListener? = null

        fun setTitle(title: CharSequence?, isBig: Boolean = false) = apply {
            this.title = title
            this.isBigTitle = isBig
        }

        fun setItems(items: Array<out CharSequence>, listener: DialogInterface.OnClickListener?) = apply {
            this.items = items.copyOf()
            this.icons = null
            this.onClickListener = listener
        }

        fun setItems(
            items: Array<out CharSequence>,
            icons: IntArray,
            listener: DialogInterface.OnClickListener?
        ) = apply {
            this.items = items.copyOf()
            this.icons = icons.copyOf()
            this.onClickListener = listener
        }

        fun setCustomView(view: View) = apply { this.customView = view }
        fun setCancelable(value: Boolean) = apply { this.cancelable = value }
        fun setOnDismissListener(listener: DialogInterface.OnDismissListener?) = apply { this.dismissListener = listener }
        fun setOnCancelListener(listener: DialogInterface.OnCancelListener?) = apply { this.cancelListener = listener }

        fun create(): TelegramBottomSheet {
            val sheet = TelegramBottomSheet(context, needCustomView = customView != null)
            sheet.setTitle(title, isBigTitle)

            val palette = TelegramThemeEngine.resolveCurrentPalette(context)
            items?.let { itemList ->
                val scrollContent = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                }
                itemList.forEachIndexed { index, label ->
                    val iconRes = if (icons != null && index < icons!!.size) icons!![index] else 0
                    val cell = BottomSheetCell(context, palette).apply {
                        tag = "telegram_bottom_sheet_item_$index"
                        setTextAndIcon(label, iconRes)
                        setOnClickListener {
                            if (!sheet.isShowing) return@setOnClickListener
                            try {
                                onClickListener?.onClick(sheet, index)
                            } finally {
                                if (sheet.isShowing) sheet.dismiss()
                            }
                        }
                    }
                    sheet.itemViews.add(cell)
                    scrollContent.addView(
                        cell,
                        LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (48 * context.resources.displayMetrics.density + 0.5f).toInt())
                    )
                }
                sheet.contentLayout.addView(ScrollView(context).apply {
                    isVerticalScrollBarEnabled = false
                    addView(scrollContent, FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ))
                }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            }

            customView?.let { sheet.setCustomView(it) }

            sheet.setCancelable(cancelable)
            sheet.setCanceledOnTouchOutside(cancelable)
            sheet.setOnDismissListener(dismissListener)
            sheet.setOnCancelListener(cancelListener)
            return sheet
        }

        fun show(): TelegramBottomSheet = create().also { it.show() }
    }
}
