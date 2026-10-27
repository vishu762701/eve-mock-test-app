package com.eve.app.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.ColorStateList
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewTreeObserver
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.graphics.ColorUtils
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import com.eve.app.R
import com.eve.app.designsystem.DesignMotion
import com.eve.app.theme.TelegramThemeEngine
import com.eve.app.theme.TelegramColorRoles
import com.eve.app.ui.navigation.EveBottomPillBar
import com.google.android.material.card.MaterialCardView
import java.lang.ref.WeakReference
import java.util.ArrayDeque
import kotlin.math.abs
import kotlin.math.max

/** Source-traced EVE adaptation. Uses Telegram's documented default transition fallback. */
class AppBulletin private constructor(
    activity: Activity,
    private val message: CharSequence,
    private val durationMs: Long,
    private val iconRes: Int,
    private val actionText: String?,
    private val onAction: (() -> Unit)?
) {
    private val activityRef = WeakReference(activity)
    private var host: Host? = null
    private val actionable get() = !actionText.isNullOrBlank() && onAction != null

    companion object {
        const val DURATION_SHORT = 1500L
        const val DURATION_LONG = 2750L
        const val DURATION_PROLONG = 5000L
        const val DURATION_DEFAULT = 5000L
        // Retain longer error visibility for readability.
        const val DURATION_ERROR = 8000L
        fun durationForMessage(message: CharSequence): Long = if (message.length < 20) DURATION_SHORT else DURATION_LONG
        internal fun remainingAfterElapsed(remainingMs: Long, elapsedMs: Long): Long =
            (remainingMs - elapsedMs.coerceAtLeast(0)).coerceAtLeast(0)
        fun findActivity(context: Context?): Activity? {
            var ctx = context
            while (ctx is ContextWrapper) {
                if (ctx is Activity) return ctx
                val next = ctx.baseContext
                if (next === ctx) break
                ctx = next
            }
            return null
        }
        fun make(activity: Activity, message: CharSequence, durationMs: Long = durationForMessage(message),
                 iconRes: Int = R.drawable.ic_check_circle, actionText: String? = null,
                 onAction: (() -> Unit)? = null): AppBulletin =
            AppBulletin(activity, message, durationMs, iconRes, actionText, onAction)
        fun show(context: Context, message: CharSequence, durationMs: Long = durationForMessage(message),
                 iconRes: Int = R.drawable.ic_check_circle, actionText: String? = null,
                 onAction: (() -> Unit)? = null) {
            val activity = findActivity(context)
            if (activity != null && !activity.isFinishing && !activity.isDestroyed)
                make(activity, message, durationMs, iconRes, actionText, onAction).show()
            else Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
        fun showSuccess(context: Context, message: CharSequence) = show(context, message)
        fun showError(context: Context, message: CharSequence) = show(context, message, DURATION_ERROR, R.drawable.ic_info)
    }

    fun show() {
        val activity = activityRef.get() ?: return
        if (activity.isFinishing || activity.isDestroyed) return
        val content = activity.findViewById<ViewGroup>(android.R.id.content) ?: return
        val current = content.getTag(R.id.eve_bulletin_host) as? Host
        val target = current ?: Host(content).also { content.setTag(R.id.eve_bulletin_host, it) }
        host = target
        target.enqueue(this)
    }
    fun dismiss() { host?.dismiss(this) }

    private class Host(private val content: ViewGroup) : View.OnAttachStateChangeListener {
        private val handler = Handler(Looper.getMainLooper())
        private val pending = ArrayDeque<AppBulletin>()
        private var active: AppBulletin? = null
        private var view: View? = null
        private var remaining = 0L
        private var started = 0L
        private var paused = false
        private var disposed = false
        private var themeListener: ((TelegramThemeEngine.ResolvedPalette) -> Unit)? = null
        private var layoutListener: View.OnLayoutChangeListener? = null
        private var drawListener: ViewTreeObserver.OnPreDrawListener? = null
        private var offsetSpring: SpringAnimation? = null
        private var offsetTarget = Float.NaN
        private var exiting = false
        private val lifecycle = (findActivity(content.context) as? LifecycleOwner)?.lifecycle
        private val lifecycleObserver = object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) { dispose() }
            override fun onDestroy(owner: LifecycleOwner) { dispose() }
        }
        private val timeout = Runnable { active?.let { dismiss(it) } }
        init {
            content.addOnAttachStateChangeListener(this)
            lifecycle?.addObserver(lifecycleObserver)
        }

        fun enqueue(item: AppBulletin) {
            if (disposed || active === item || pending.contains(item)) return
            val visible = active
            if (visible == null) { present(item); return }
            if (visible.actionable || pending.isNotEmpty()) {
                // Preserve all actionable callbacks; redundant informational events may be coalesced.
                if (item.actionable) pending.addLast(item)
                return
            }
            removeVisible()
            present(item)
        }

        private fun present(item: AppBulletin) {
            if (disposed) return
            val activity = item.activityRef.get() ?: return
            if (activity.isFinishing || activity.isDestroyed) return
            active = item
            exiting = false
            remaining = item.durationMs.coerceAtLeast(0)
            paused = false
            val root = LayoutInflater.from(activity).inflate(R.layout.layout_telegram_bulletin, content, false)
            view = root
            root.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(v: View) = Unit
                override fun onViewDetachedFromWindow(v: View) {
                    // setContentView can remove this overlay without detaching the content root.
                    if (view === v) dispose(overlayAlreadyDetaching = true)
                }
            })
            val card = root.findViewById<MaterialCardView>(R.id.cardBulletin)
            val text = root.findViewById<TextView>(R.id.tvBulletinMessage)
            val icon = root.findViewById<ImageView>(R.id.ivBulletinIcon)
            val action = root.findViewById<TextView>(R.id.btnBulletinAction)
            text.text = item.message
            icon.setImageResource(item.iconRes)
            if (item.actionable) {
                action.visibility = View.VISIBLE
                action.text = item.actionText
                action.setOnTouchListener { _, event ->
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN -> pauseTimer()
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> resumeTimer()
                    }
                    false
                }
                action.setOnClickListener {
                    if (active === item && !exiting) {
                        action.isEnabled = false
                        val callback = item.onAction
                        dismiss(item)
                        callback?.invoke()
                    }
                }
            }
            val listener: (TelegramThemeEngine.ResolvedPalette) -> Unit = { palette ->
                card.setCardBackgroundColor(palette.bulletinBackground)
                // Account for the translucent source fill over both theme backing roles.
                val backing = listOf(palette.windowBackground, palette.surface).map {
                    ColorUtils.compositeColors(palette.bulletinBackground, it)
                }
                val info = TelegramColorRoles.accessibleForeground(palette.bulletinInfo, backing)
                val actionColor = TelegramColorRoles.accessibleForeground(palette.bulletinAction, backing)
                text.setTextColor(info)
                icon.imageTintList = ColorStateList.valueOf(info)
                action.setTextColor(actionColor)
            }
            themeListener = listener
            TelegramThemeEngine.addThemeChangeListener(listener)
            listener(TelegramThemeEngine.resolveCurrentPalette(activity))
            root.accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
            content.addView(root)
            root.announceForAccessibility(item.message)
            val reposition = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> position(root) }
            layoutListener = reposition
            content.addOnLayoutChangeListener(reposition)
            val preDraw = ViewTreeObserver.OnPreDrawListener {
                if (view === root) position(root)
                true
            }
            drawListener = preDraw
            content.viewTreeObserver.addOnPreDrawListener(preDraw)
            root.post { if (view === root) { position(root); enter(card, activity) } }
            setupSwipe(card, item, activity)
            startTimer()
        }
        private fun position(root: View) {
            val inset = ViewCompat.getRootWindowInsets(content)
            val bottom = inset?.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout() or WindowInsetsCompat.Type.ime())?.bottom ?: 0
            val contentLocation = IntArray(2)
            content.getLocationOnScreen(contentLocation)
            val pill = content.findViewWithTag<EveBottomPillBar>("eve_bottom_pill_bar")
            val pillOffset = if (pill != null && pill.isShown) {
                val location = IntArray(2); pill.getLocationOnScreen(location)
                (contentLocation[1] + content.height - location[1]).coerceAtLeast(0)
            } else 0
            val target = -max(bottom, pillOffset).toFloat()
            if (target == offsetTarget) {
                if (!DesignMotion.enabled(root.context) && offsetSpring?.isRunning == true) {
                    offsetSpring?.cancel()
                    root.translationY = target
                }
                return
            }
            offsetTarget = target
            // Moving upward is immediate so the bulletin never covers a newly raised IME/pill.
            if (target < root.translationY || !DesignMotion.enabled(root.context)) {
                offsetSpring?.cancel()
                root.translationY = target
            } else {
                val animation = offsetSpring ?: SpringAnimation(root, DynamicAnimation.TRANSLATION_Y).also {
                    it.setSpring(SpringForce().setStiffness(900f).setDampingRatio(SpringForce.DAMPING_RATIO_NO_BOUNCY))
                    offsetSpring = it
                }
                animation.animateToFinalPosition(target)
            }
        }
        private fun enter(card: View, activity: Activity) {
            if (!DesignMotion.enabled(activity)) return
            card.translationY = card.height.toFloat()
            card.animate().translationY(0f).setDuration(255L).setInterpolator(DecelerateInterpolator()).start()
        }
        private fun startTimer() {
            handler.removeCallbacks(timeout)
            if (paused) return
            if (remaining <= 0L) { active?.let { dismiss(it) }; return }
            started = SystemClock.elapsedRealtime()
            handler.postDelayed(timeout, remaining)
        }
        private fun pauseTimer() {
            if (paused) return
            paused = true
            remaining = remainingAfterElapsed(remaining, SystemClock.elapsedRealtime() - started)
            handler.removeCallbacks(timeout)
        }
        private fun resumeTimer() { if (paused) { paused = false; startTimer() } }
        private fun setupSwipe(card: View, item: AppBulletin, activity: Activity) {
            var down = 0f
            var dragging = false
            val slop = ViewConfiguration.get(activity).scaledTouchSlop
            card.setOnTouchListener { v, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> { down = event.rawX; dragging = false; pauseTimer(); true }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = event.rawX - down
                        if (abs(dx) > slop) { dragging = true; v.translationX = dx }
                        true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        val dx = event.rawX - down
                        if (event.actionMasked == MotionEvent.ACTION_UP && dragging && abs(dx) > v.width / 3f) dismiss(item)
                        else {
                            if (!dragging && event.actionMasked == MotionEvent.ACTION_UP) v.performClick()
                            v.animate().translationX(0f).setDuration(if (DesignMotion.enabled(activity)) 200L else 0L).start()
                            resumeTimer()
                        }
                        true
                    }
                    else -> true
                }
            }
        }
        fun dismiss(item: AppBulletin) {
            if (active !== item) { pending.remove(item); item.host = null; return }
            if (exiting) return
            exiting = true
            val card = view?.findViewById<View>(R.id.cardBulletin)
            val activity = item.activityRef.get()
            handler.removeCallbacks(timeout)
            if (card == null || activity == null || !card.isLaidOut || card.height == 0 || !DesignMotion.enabled(activity)) {
                finish(item)
                return
            }
            card.animate().cancel()
            card.animate().translationY(card.height.toFloat()).setDuration(175L)
                .setInterpolator(AccelerateInterpolator()).withEndAction { finish(item) }.start()
        }
        private fun finish(item: AppBulletin) {
            if (active !== item) return
            removeVisible()
            if (!disposed && pending.isNotEmpty()) present(pending.removeFirst())
        }
        private fun removeVisible(removeRoot: Boolean = true) {
            handler.removeCallbacks(timeout)
            val old = view
            view = null
            old?.findViewById<View>(R.id.cardBulletin)?.animate()?.cancel()
            themeListener?.let { TelegramThemeEngine.removeThemeChangeListener(it) }
            themeListener = null
            layoutListener?.let { content.removeOnLayoutChangeListener(it) }
            layoutListener = null
            drawListener?.let { if (content.viewTreeObserver.isAlive) content.viewTreeObserver.removeOnPreDrawListener(it) }
            drawListener = null
            offsetSpring?.cancel()
            offsetSpring = null
            offsetTarget = Float.NaN
            if (removeRoot) (old?.parent as? ViewGroup)?.removeView(old)
            active?.host = null
            active = null
        }
        override fun onViewAttachedToWindow(v: View) = Unit
        override fun onViewDetachedFromWindow(v: View) {
            dispose()
        }
        private fun dispose(overlayAlreadyDetaching: Boolean = false) {
            if (disposed) return
            disposed = true
            removeVisible(removeRoot = !overlayAlreadyDetaching)
            pending.forEach { it.host = null }
            pending.clear()
            lifecycle?.removeObserver(lifecycleObserver)
            content.removeOnAttachStateChangeListener(this)
            content.setTag(R.id.eve_bulletin_host, null)
        }
    }
}
