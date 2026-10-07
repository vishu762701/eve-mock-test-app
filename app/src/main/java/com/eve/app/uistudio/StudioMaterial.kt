package com.eve.app.uistudio

import android.graphics.Color
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.eve.app.data.model.uistudio.ComponentConfig
import com.eve.app.util.UiStudioEngine
import com.google.android.material.card.MaterialCardView
import eightbitlab.com.blurview.BlurView
import eightbitlab.com.blurview.RenderEffectBlur

/** A first-child BlurView captures only the backdrop, before foreground children draw. */
object StudioMaterial {
    private const val TAG = "studio_backdrop"
    fun supported(view: View?) = view is FrameLayout && view !is android.widget.ScrollView && view !is BlurView
    fun apply(view: View, config: ComponentConfig) {
        val host = view as? FrameLayout ?: return
        if (!supported(host)) return
        val existing = (0 until host.childCount).map { host.getChildAt(it) }.filterIsInstance<BlurView>().firstOrNull { it.tag == TAG }
        val m = config.material
        val radius = m.blurRadius ?: 0
        (0 until host.childCount).map { host.getChildAt(it) }.filterIsInstance<BlurView>().filter { it.tag != TAG }.forEach { StudioBaseline.visibility(it,radius>0) }
        if (radius == 0) {
            existing?.setBlurAutoUpdate(false)
            existing?.let { host.removeView(it) }
            return
        }
        val root = generateSequence(host.parent) { it.parent }.filterIsInstance<ViewGroup>().lastOrNull() ?: return
        val blur = existing ?: BlurView(host.context).also {
            it.tag = TAG
            it.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            it.isClickable = false
            host.addView(it, 0, FrameLayout.LayoutParams(-1, -1))
            if (Build.VERSION.SDK_INT >= 31) it.setupWith(root, RenderEffectBlur()) else it.setupWith(root)
        }
        // A solid native card would hide the backdrop. Its baseline is retained by StudioBaseline.
        if (host is MaterialCardView) host.setCardBackgroundColor(Color.TRANSPARENT) else host.background = null
        blur.setBlurRadius(radius.coerceIn(1, 25).toFloat())
        val tint = UiStudioEngine.parseColorSafe(m.tintColor) ?: if ((host.resources.configuration.uiMode and 0x30) == 0x20) Color.BLACK else Color.WHITE
        val alpha = ((m.tintOpacity ?: .2f) * 255).toInt().coerceIn(0, 255)
        blur.setOverlayColor((tint and 0x00ffffff) or (alpha shl 24))
        blur.alpha = m.materialOpacity ?: 1f
        blur.setBlurAutoUpdate(true)
        blur.outlineProvider = object : android.view.ViewOutlineProvider() {
            override fun getOutline(v: View, outline: android.graphics.Outline) {
                outline.setRoundRect(0, 0, v.width, v.height, (config.appearance.cornerRadius ?: 16) * v.resources.displayMetrics.density)
            }
        }
        blur.clipToOutline = true
        if(host !is MaterialCardView && (config.appearance.strokeWidth ?: 0)>0) {
            val edge=android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.TRANSPARENT)
                cornerRadius=(config.appearance.cornerRadius ?: 16)*host.resources.displayMetrics.density
                setStroke(UiStudioEngine.dpToPx(host.context,config.appearance.strokeWidth!!),UiStudioEngine.parseColorSafe(config.appearance.strokeColor) ?: Color.TRANSPARENT)
            }
            host.foreground=host.foreground?.let { android.graphics.drawable.LayerDrawable(arrayOf(it,edge)) } ?: edge
        }
    }
}
