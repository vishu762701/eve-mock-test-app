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
        val stroke: Int?, val strokeColors: ColorStateList?, val imageFilter: android.graphics.ColorFilter?, val imageTint: ColorStateList?, val outline: android.view.ViewOutlineProvider?, val clip: Boolean, val buttonRadius: Int?, val buttonStroke: Int?, val buttonStrokeColors: ColorStateList?, val buttonIconTint: ColorStateList?, val compoundTint: ColorStateList?
    )
    private val overrides = WeakHashMap<View, com.eve.app.data.model.uistudio.ComponentConfig>()
    private fun capture(v: View): Style {
        val b=v as? com.google.android.material.button.MaterialButton
        val lp = v.layoutParams
        val m = lp as? ViewGroup.MarginLayoutParams
        val t = v as? TextView
        val c = v as? MaterialCardView
        return Style(v.background?.constantState?.newDrawable()?.mutate() ?: v.background, if(c!=null)v.foreground else v.foreground?.constantState?.newDrawable()?.mutate() ?: v.foreground, v.backgroundTintList,
                v.alpha, v.elevation, v.visibility, v.isEnabled, listOf(v.paddingStart,v.paddingTop,v.paddingEnd,v.paddingBottom),
                lp?.width, lp?.height, m?.let { listOf(it.leftMargin,it.topMargin,it.rightMargin,it.bottomMargin,it.marginStart,it.marginEnd) },
                t?.textColors,t?.textSize,t?.typeface,t?.gravity,t?.maxLines,c?.cardBackgroundColor,c?.radius,c?.strokeWidth,c?.strokeColorStateList,(v as? android.widget.ImageView)?.colorFilter,(v as? android.widget.ImageView)?.imageTintList,v.outlineProvider,v.clipToOutline,b?.cornerRadius,b?.strokeWidth,b?.strokeColor,b?.iconTint,t?.compoundDrawableTintList)
    }
    fun record(v: View, config: com.eve.app.data.model.uistudio.ComponentConfig) {
        overrides[v] = config
        v.setTag(com.eve.app.R.id.studio_baseline_rendered,capture(v))
    }
    /** Retain native data/theme values written by a binder beneath the active overrides. */
    fun refreshNative(v: View) {
        val original=v.getTag(com.eve.app.R.id.studio_baseline_style) as? Style ?: return
        val rendered=v.getTag(com.eve.app.R.id.studio_baseline_rendered) as? Style ?: return
        val current=capture(v)
        val previous=overrides[v] ?: return
        val visualColors=StudioInteraction.stableColors(v)
        val nativeColorsChanged=visualColors != (if(v is MaterialCardView)rendered.cardColor else rendered.tint)
        val updated=original.copy(
            alpha=if(previous.appearance.opacity!=null && com.eve.app.util.UiStudioEngine.stableAlpha(v)!=rendered.alpha)current.alpha else original.alpha,
            cardColor=if(v is MaterialCardView && nativeColorsChanged)current.cardColor else original.cardColor,
            tint=if(v !is MaterialCardView && nativeColorsChanged)current.tint else original.tint,
            stroke=if(current.stroke!=rendered.stroke)current.stroke else original.stroke,
            strokeColors=if(current.strokeColors!=rendered.strokeColors)current.strokeColors else original.strokeColors,
            textColors=if(current.textColors!=rendered.textColors)current.textColors else original.textColors,
            size=if(current.size!=rendered.size)current.size else original.size,
            typeface=if(current.typeface!=rendered.typeface)current.typeface else original.typeface,
            imageFilter=if(current.imageFilter!=rendered.imageFilter)current.imageFilter else original.imageFilter,
            imageTint=if(current.imageTint!=rendered.imageTint)current.imageTint else original.imageTint)
        v.setTag(com.eve.app.R.id.studio_baseline_style,updated)
    }

    fun restore(v: View) {
        com.eve.app.util.UiStudioEngine.cancelMotion(v)
        StudioInteraction.clear(v)
        val b=v as? com.google.android.material.button.MaterialButton
        val lp = v.layoutParams
        val m = lp as? ViewGroup.MarginLayoutParams
        val t = v as? TextView
        val c = v as? MaterialCardView
        val s = (v.getTag(com.eve.app.R.id.studio_baseline_style) as? Style) ?: capture(v).also {
            v.setTag(com.eve.app.R.id.studio_baseline_style,it)
        }
        val previous = overrides.remove(v) ?: return
        val a = previous.appearance
        val material = previous.material
        val typography = previous.typography
        val layout = previous.layout
        val fill = a.backgroundColor != null || material != com.eve.app.data.model.uistudio.MaterialProperties() ||
            previous.states != com.eve.app.data.model.uistudio.StateProperties()
        val shape = a.cornerRadius != null || a.shape != null
        if((fill || shape || a.strokeWidth != null || a.strokeColor != null) && v !is com.google.android.material.button.MaterialButton && v !is MaterialCardView)v.background = s.background?.constantState?.newDrawable()?.mutate() ?: s.background
        // MaterialCard owns the stroke/ripple drawable; retain its identity so native setters update the drawn edge.
        if (a.highlightColor != null || a.strokeWidth != null) v.foreground=if(c!=null)s.foreground else s.foreground?.constantState?.newDrawable()?.mutate() ?: s.foreground
        if (fill) v.backgroundTintList = s.tint
        if (shape) { v.outlineProvider=s.outline;v.clipToOutline=s.clip }
        if(b!=null){if(shape)s.buttonRadius?.let{b.cornerRadius=it};if(a.strokeWidth!=null)s.buttonStroke?.let{b.strokeWidth=it};if(a.strokeColor!=null)b.strokeColor=s.buttonStrokeColors;if(a.iconTint!=null)b.iconTint=s.buttonIconTint}
        if (a.opacity != null) v.alpha = s.alpha
        if (a.elevation != null) v.elevation = s.elevation
        // Visibility/enabled belong to business logic unless Studio previously changed them.
        v.setPaddingRelative(
            if(layout.paddingStart!=null)s.padding[0] else v.paddingStart,
            if(layout.paddingTop!=null)s.padding[1] else v.paddingTop,
            if(layout.paddingEnd!=null)s.padding[2] else v.paddingEnd,
            if(layout.paddingBottom!=null)s.padding[3] else v.paddingBottom)
        if (lp != null) {
            val before=listOf(lp.width,lp.height,m?.leftMargin,m?.topMargin,m?.rightMargin,m?.bottomMargin,m?.marginStart,m?.marginEnd)
            if(layout.width!=null)s.width?.let { lp.width = it }
            if(layout.height!=null)s.height?.let { lp.height = it }
            if (m != null && s.margins != null) {
                if(layout.marginStart!=null){m.leftMargin=s.margins[0];m.marginStart=s.margins[4]}
                if(layout.marginTop!=null)m.topMargin=s.margins[1]
                if(layout.marginEnd!=null){m.rightMargin=s.margins[2];m.marginEnd=s.margins[5]}
                if(layout.marginBottom!=null)m.bottomMargin=s.margins[3]
            }
            val changed=before!=listOf(lp.width,lp.height,m?.leftMargin,m?.topMargin,m?.rightMargin,m?.bottomMargin,m?.marginStart,m?.marginEnd)
            if(changed)v.layoutParams=lp
        }
        if (t != null) {
            if(a.iconTint!=null)t.compoundDrawableTintList=s.compoundTint
            if(typography.textColor!=null || previous.states.selectedTextColor!=null)s.textColors?.let { t.setTextColor(it) }
            if(typography.textSize!=null)s.size?.let { t.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,it) }
            if(typography.fontFamily!=null || typography.textStyle!=null)t.typeface=s.typeface
            if(typography.textAlign!=null)s.gravity?.let { t.gravity=it }
            if(typography.maxLines!=null)s.maxLines?.let { t.maxLines=it }
        }
        if (a.iconTint!=null && v is android.widget.ImageView) { v.colorFilter=s.imageFilter;v.imageTintList=s.imageTint }
        if (c != null) {
            if(fill)s.cardColor?.let { c.setCardBackgroundColor(it) }; if(shape)s.radius?.let { c.radius=it }
            if(a.strokeWidth!=null)s.stroke?.let { c.strokeWidth=it }; if(a.strokeColor!=null)s.strokeColors?.let { c.setStrokeColor(it) }
        }
    }
    private val hidden = WeakHashMap<View, Int>()
    fun visibility(v: View, hide: Boolean) {
        if (hide) { hidden.putIfAbsent(v,v.visibility); v.visibility=View.GONE }
        else hidden.remove(v)?.let { v.visibility=it }
    }
}
