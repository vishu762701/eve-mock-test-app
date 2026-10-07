package com.eve.app.uistudio

import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.android.material.card.MaterialCardView
import java.util.WeakHashMap

/** Capture native style once; reset missing overrides before reapplying. Never cache business text. */
object StudioBaseline {
    private data class Style(
        val background: Drawable?, val foreground: Drawable?, val tint: ColorStateList?, val alpha: Float, val elevation: Float,
        val visibility: Int, val enabled: Boolean, val padding: List<Int>, val width: Int?, val height: Int?,
        val margins: List<Int>?, val textColors: ColorStateList?, val size: Float?, val typeface: Typeface?,
        val gravity: Int?, val maxLines: Int?, val cardColor: ColorStateList?, val radius: Float?,
        val stroke: Int?, val strokeColors: ColorStateList?, val imageFilter: android.graphics.ColorFilter?, val imageTint: ColorStateList?
    )
    private val styles = WeakHashMap<View, Style>()
    fun restore(v: View) {
        val lp = v.layoutParams
        val m = lp as? ViewGroup.MarginLayoutParams
        val t = v as? TextView
        val c = v as? MaterialCardView
        val s = styles.getOrPut(v) {
            Style(v.background?.constantState?.newDrawable()?.mutate() ?: v.background, v.foreground?.constantState?.newDrawable()?.mutate() ?: v.foreground, v.backgroundTintList,
                v.alpha, v.elevation, v.visibility, v.isEnabled, listOf(v.paddingLeft,v.paddingTop,v.paddingRight,v.paddingBottom),
                lp?.width, lp?.height, m?.let { listOf(it.leftMargin,it.topMargin,it.rightMargin,it.bottomMargin) },
                t?.textColors,t?.textSize,t?.typeface,t?.gravity,t?.maxLines,c?.cardBackgroundColor,c?.radius,c?.strokeWidth,c?.strokeColorStateList,(v as? android.widget.ImageView)?.colorFilter,(v as? android.widget.ImageView)?.imageTintList)
        }
        com.eve.app.util.UiStudioEngine.cancelMotion(v)
        v.background = s.background?.constantState?.newDrawable()?.mutate() ?: s.background
        v.foreground=s.foreground?.constantState?.newDrawable()?.mutate() ?: s.foreground
        v.backgroundTintList = s.tint
        v.alpha = s.alpha; v.elevation = s.elevation
        // Visibility/enabled belong to business logic unless Studio previously changed them.
        v.setPadding(s.padding[0],s.padding[1],s.padding[2],s.padding[3])
        if (lp != null) {
            s.width?.let { lp.width = it }; s.height?.let { lp.height = it }
            if (m != null && s.margins != null) { m.setMargins(s.margins[0],s.margins[1],s.margins[2],s.margins[3]) }
        }
        if (t != null) {
            s.textColors?.let { t.setTextColor(it) }; s.size?.let { t.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,it) }
            t.typeface=s.typeface; s.gravity?.let { t.gravity=it }; s.maxLines?.let { t.maxLines=it }
        }
        if (v is android.widget.ImageView) { v.colorFilter=s.imageFilter;v.imageTintList=s.imageTint }
        if (c != null) {
            s.cardColor?.let { c.setCardBackgroundColor(it) }; s.radius?.let { c.radius=it }
            s.stroke?.let { c.strokeWidth=it }; s.strokeColors?.let { c.setStrokeColor(it) }
        }
    }
    private val hidden = WeakHashMap<View, Int>()
    fun visibility(v: View, hide: Boolean) {
        if (hide) { hidden.putIfAbsent(v,v.visibility); v.visibility=View.GONE }
        else hidden.remove(v)?.let { v.visibility=it }
    }
}
