package com.eve.app.designsystem

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.eve.app.R
import eightbitlab.com.blurview.BlurView
import eightbitlab.com.blurview.BlurViewFacade
import eightbitlab.com.blurview.RenderEffectBlur

/** Backdrop-only glass. Foreground children stay sharp and are excluded from capture. */
open class DesignGlassLayout @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : BlurView(context, attrs) {
    private val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = ContextCompat.getColor(context, R.color.hairline)
        strokeWidth = resources.getDimension(R.dimen.design_hairline_width)
    }
    private val rect = RectF()
    private var facade: BlurViewFacade? = null
    private var capsule = false
    private var radius = resources.getDimension(R.dimen.design_radius_card)
    private val configureAfterAttach = Runnable { configureBackdrop() }
    var glassEnabled: Boolean = true
        set(value) {
            if (field == value) return
            field = value
            configureBackdrop()
            invalidate()
        }
    /** Popup chrome can point to its host window, rather than the popup's own tree. */
    var backdropRoot: ViewGroup? = null
        set(value) { field = value; if (isAttachedToWindow) configureBackdrop() }

    init {
        val attributes = context.obtainStyledAttributes(attrs, R.styleable.DesignGlassLayout)
        try {
            capsule = attributes.getBoolean(R.styleable.DesignGlassLayout_designGlassCapsule, false)
            radius = attributes.getDimension(R.styleable.DesignGlassLayout_designGlassRadius, radius)
        } finally { attributes.recycle() }
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, drawnRadius())
            }
        }
        clipToOutline = true
        setBackgroundColor(ContextCompat.getColor(context, R.color.glass_fallback))
    }
    protected fun useSquareCorners() { radius = resources.getDimension(R.dimen.design_zero) }
    private fun drawnRadius(): Float = if (capsule) minOf(width, height) / 2f else radius
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        post(configureAfterAttach)
    }
    override fun onDetachedFromWindow() {
        removeCallbacks(configureAfterAttach)
        facade?.setBlurAutoUpdate(false)
        super.onDetachedFromWindow()
    }
    private fun configureBackdrop() {
        facade?.setBlurEnabled(false)
        if (!glassEnabled) {
            setBackgroundColor(ContextCompat.getColor(context, R.color.transparent))
            return
        }
        val source = backdropRoot ?: activityContent() ?: (rootView as? ViewGroup)
        if (Build.VERSION.SDK_INT < 31 || !isHardwareAccelerated || source == null || source === this) {
            fallback()
            return
        }
        try {
            setBackgroundColor(ContextCompat.getColor(context, R.color.transparent))
            facade = setupWith(source, ExactBackdropBlur())
                .setFrameClearDrawable(ColorDrawable(ContextCompat.getColor(context, R.color.bg_app)))
                .setBlurRadius(resources.getDimension(R.dimen.design_glass_blur))
                .setOverlayColor(ContextCompat.getColor(context, R.color.transparent))
                .setBlurAutoUpdate(true)
        } catch (error: RuntimeException) {
            Log.w("DesignGlassLayout", "Backdrop unavailable; using locked glass fallback", error)
            fallback()
        }
    }
    private fun fallback() {
        facade?.setBlurEnabled(false)
        facade?.setBlurAutoUpdate(false)
        setBackgroundColor(ContextCompat.getColor(context, R.color.glass_fallback))
    }
    private fun activityContent(): ViewGroup? {
        var owner: Context = context
        while (owner is ContextWrapper) {
            if (owner is Activity) return owner.findViewById(android.R.id.content)
            val next = owner.baseContext
            if (next === owner) break
            owner = next
        }
        return null
    }
    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        if (glassEnabled) {
            val inset = border.strokeWidth / 2f
            rect.set(inset, inset, width - inset, height - inset)
            val corner = (drawnRadius() - inset).coerceAtLeast(0f)
            canvas.drawRoundRect(rect, corner, corner, border)
        }
    }
    @RequiresApi(31)
    private inner class ExactBackdropBlur : RenderEffectBlur() {
        private var failed = false
        // The library's default downsamples first. Pixel scale 1 keeps 14dp exact.
        override fun scaleFactor(): Float = 1f
        override fun blur(bitmap: Bitmap, blurRadius: Float): Bitmap {
            if (failed) return bitmap
            return try { super.blur(bitmap, blurRadius) } catch (error: RuntimeException) {
                failed = true
                Log.w("DesignGlassLayout", "Backdrop renderer failed; using locked fallback", error)
                post { fallback() }
                bitmap
            }
        }
        override fun render(canvas: Canvas, bitmap: Bitmap) {
            if (!failed && canvas.isHardwareAccelerated) {
                super.render(canvas, bitmap)
                canvas.drawColor(ContextCompat.getColor(context, R.color.glass_tint))
            } else {
                // Software screenshots must not enter the library's RenderScript fallback.
                canvas.drawColor(ContextCompat.getColor(context, R.color.glass_fallback))
            }
        }
    }
}
