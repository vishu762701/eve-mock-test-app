package com.eve.app.ui.common

import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.AbsListView
import android.widget.ImageButton
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.RecyclerView
import com.eve.app.R
import com.eve.app.designsystem.BackDrawable
import com.eve.app.designsystem.DesignGlassLayout
import com.eve.app.theme.TelegramThemeEngine
import com.eve.app.util.FontManager

/**
 * Pinned transparent chrome at scroll top delegating height, background, title,
 * and back button metrics to TelegramActionBar and TelegramThemeEngine.
 */
class EveGlassToolbar @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : DesignGlassLayout(context, attrs), ViewTreeObserver.OnGlobalLayoutListener,
    ViewTreeObserver.OnPreDrawListener {

    private var observer: ViewTreeObserver? = null
    private val scrollHosts = ArrayList<View>()
    private val hairlinePaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = resources.displayMetrics.density
    }

    var scrollBackdropEnabled: Boolean = false
        set(value) {
            if (field == value) return
            field = value
            if (!value) {
                scrollHosts.clear()
                glassEnabled = false
            }
            invalidate()
        }

    init {
        useSquareCorners()
        drawHairline = false
        glassEnabled = false
        applyTelegramMetrics()
    }

    private fun applyTelegramMetrics() {
        val density = resources.displayMetrics.density
        val palette = TelegramThemeEngine.resolveCurrentPalette(context)
        hairlinePaint.color = palette.hairline

        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val targetHeight = if (isLandscape) (48f * density + 0.5f).toInt() else (56f * density + 0.5f).toInt()
        minimumHeight = targetHeight

        setBackgroundColor(if (glassEnabled) palette.glassTargetMainTopPanel else palette.surface)
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        val density = resources.displayMetrics.density
        val palette = TelegramThemeEngine.resolveCurrentPalette(context)

        // Upgrade any child Back button to 54dp width with BackDrawable
        findViewById<View>(R.id.btnBack)?.let { backBtn ->
            val lp = backBtn.layoutParams
            if (lp != null) {
                lp.width = (54f * density + 0.5f).toInt()
                backBtn.layoutParams = lp
            }
            if (backBtn is ImageButton) {
                val backDrawable = BackDrawable().apply {
                    setColor(palette.textPrimary)
                }
                backBtn.setImageDrawable(backDrawable)
            }
        }

        // Upgrade title TextView
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child is TextView) {
                child.setTextAppearance(R.style.TextAppearance_Eve_Telegram_NavigationTitle)
                child.setTextColor(palette.textPrimary)
                child.typeface = FontManager.telegramMediumTypeface(context)
            }
        }
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        // Draw 1px bottom hairline
        val y = height - hairlinePaint.strokeWidth / 2f
        canvas.drawLine(0f, y, width.toFloat(), y, hairlinePaint)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        applyTelegramMetrics()
        observer = rootView.viewTreeObserver.also {
            it.addOnGlobalLayoutListener(this)
            it.addOnPreDrawListener(this)
        }
        onGlobalLayout()
    }

    override fun onDetachedFromWindow() {
        observer?.takeIf { it.isAlive }?.let {
            it.removeOnGlobalLayoutListener(this)
            it.removeOnPreDrawListener(this)
        }
        observer = null
        scrollHosts.clear()
        super.onDetachedFromWindow()
    }

    override fun onGlobalLayout() {
        scrollHosts.clear()
        if (!scrollBackdropEnabled) return
        fun visit(view: View) {
            if (view === this) return
            if (view is RecyclerView || view is ScrollView || view is NestedScrollView || view is AbsListView)
                scrollHosts.add(view)
            if (view is ViewGroup) for (i in 0 until view.childCount) visit(view.getChildAt(i))
        }
        visit(rootView)
    }

    override fun onPreDraw(): Boolean {
        if (!scrollBackdropEnabled) {
            if (glassEnabled) glassEnabled = false
            return true
        }
        val glassNeeded = scrollHosts.any { host ->
            if (!host.isShown || !host.canScrollVertically(-1)) return@any false
            when (host) {
                is RecyclerView -> host.computeVerticalScrollOffset() > 2
                is ScrollView -> host.scrollY > 2
                is NestedScrollView -> host.scrollY > 2
                else -> true
            }
        }
        if (glassEnabled != glassNeeded) {
            glassEnabled = glassNeeded
            val palette = TelegramThemeEngine.resolveCurrentPalette(context)
            setBackgroundColor(if (glassEnabled) palette.glassTargetMainTopPanel else palette.surface)
        }
        return true
    }
}
