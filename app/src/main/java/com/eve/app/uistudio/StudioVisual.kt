package com.eve.app.uistudio

import android.content.res.ColorStateList
import android.graphics.*
import android.graphics.drawable.*
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.ImageView
import android.widget.TextView
import com.eve.app.data.model.uistudio.ComponentConfig
import com.eve.app.util.UiStudioEngine
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

/** Visual application preserves native ripple/state drawables and never installs business listeners. */
object StudioVisual {
    fun radius(view: View, c: ComponentConfig): Float = when(c.appearance.shape) {
        "capsule","circle" -> minOf(view.width,view.height)/2f
        else -> (c.appearance.cornerRadius ?: 0)*view.resources.displayMetrics.density
    }
    private fun alpha(color: Int, multiplier: Float) = (color and 0xffffff) or ((Color.alpha(color)*multiplier).toInt().coerceIn(0,255) shl 24)
    fun apply(v: View,c: ComponentConfig) {
        val a=c.appearance;val m=c.material;val s=c.states
        // Home's images and matte draw above the parent background. Edit the visible matte surface.
        StudioMaterial.bannerMatte(v)?.let { matte ->
            UiStudioEngine.applyToView(matte, c.copy(
                id="native_homeBannerMatte",
                appearance=com.eve.app.data.model.uistudio.AppearanceProperties(backgroundColor=a.backgroundColor),
                material=m.copy(blurRadius=null), typography=com.eve.app.data.model.uistudio.TypographyProperties(),
                layout=com.eve.app.data.model.uistudio.LayoutProperties(),
                animation=com.eve.app.data.model.uistudio.AnimationProperties()))
        }
        val blur=(m.blurRadius ?: 0)>0 && StudioMaterial.supported(v)
        val fill=UiStudioEngine.parseColorSafe(a.backgroundColor)
        val tint=UiStudioEngine.parseColorSafe(m.tintColor) ?: if(m.tintOpacity!=null)
            if((v.resources.configuration.uiMode and 0x30)==0x20)Color.BLACK else Color.WHITE else null
        val existing=(v as? MaterialCardView)?.cardBackgroundColor?.defaultColor ?: v.backgroundTintList?.defaultColor ?: (v.background?.current as? ColorDrawable)?.color ?: (v.background?.current as? GradientDrawable)?.color?.defaultColor ?: (v.background?.current as? com.google.android.material.shape.MaterialShapeDrawable)?.fillColor?.defaultColor ?: Color.TRANSPARENT
        val tinted=tint?.let{androidx.core.graphics.ColorUtils.compositeColors(alpha(it,m.tintOpacity ?: .2f),fill ?: existing)} ?: (fill ?: existing)
        val surface=alpha(tinted,m.materialOpacity ?: 1f)
        val entries=mutableListOf<Pair<IntArray,Int>>()
        val nativeColors=(v as? MaterialCardView)?.cardBackgroundColor ?: v.backgroundTintList
        fun state(ids: IntArray,hex: String?) {
            val custom=UiStudioEngine.parseColorSafe(hex)
            if(custom!=null)entries+=ids to alpha(custom,m.materialOpacity ?: 1f)
            else nativeColors?.let {
                val lookup=if(ids.contains(-android.R.attr.state_enabled))ids else ids+android.R.attr.state_enabled
                val original=it.getColorForState(lookup,it.defaultColor)
                val retained=if(fill!=null || tint!=null)alpha(surface,(Color.alpha(original).toFloat()/Color.alpha(it.defaultColor).coerceAtLeast(1)).coerceIn(0f,1f))else alpha(original,m.materialOpacity ?: 1f)
                entries+=ids to retained
            }
        }
        state(intArrayOf(-android.R.attr.state_enabled),s.disabledBackgroundColor)
        state(intArrayOf(android.R.attr.state_pressed),s.pressedBackgroundColor)
        state(intArrayOf(android.R.attr.state_selected),s.selectedBackgroundColor)
        state(intArrayOf(android.R.attr.state_checked),s.selectedBackgroundColor)
        state(intArrayOf(android.R.attr.state_focused),s.focusedBackgroundColor)
        val changesFill=fill!=null || tint!=null || m.materialOpacity!=null || m.tintOpacity!=null || s!=com.eve.app.data.model.uistudio.StateProperties()
        if(changesFill && !blur) {
            entries+=intArrayOf() to surface
            val colors=ColorStateList(entries.map{it.first}.toTypedArray(),entries.map{it.second}.toIntArray())
            when(v) {
                is MaterialCardView -> v.setCardBackgroundColor(colors)
                else -> {
                    if(v.background==null)v.background=GradientDrawable().apply{setColor(Color.WHITE)}
                    v.backgroundTintList=colors
                }
            }
        }
        val density=v.resources.displayMetrics.density
        if(v !is MaterialButton && v !is MaterialCardView) {
            fun shape(drawable:Drawable?) {
                when(drawable) {
                    is GradientDrawable -> {
                        a.cornerRadius?.let{drawable.cornerRadius=it*density}
                        // GradientDrawable has no public stroke getter; explicit borders use the foreground edge.
                        if(a.strokeWidth!=null)drawable.setStroke(0,Color.TRANSPARENT)
                    }
                    is com.google.android.material.shape.MaterialShapeDrawable -> {
                        a.cornerRadius?.let{drawable.shapeAppearanceModel=drawable.shapeAppearanceModel.toBuilder().setAllCornerSizes(it*density).build()}
                        a.strokeWidth?.let{drawable.strokeWidth=it*density}
                        UiStudioEngine.parseColorSafe(a.strokeColor)?.let{drawable.strokeColor=ColorStateList.valueOf(it)}
                    }
                    is InsetDrawable -> shape(drawable.drawable)
                    is LayerDrawable -> (0 until drawable.numberOfLayers).forEach{shape(drawable.getDrawable(it))}
                    is StateListDrawable -> if(drawable.current!==drawable)shape(drawable.current)
                }
            }
            shape(v.background)
        }
        if(v is MaterialButton) {
            a.cornerRadius?.let { v.cornerRadius=(it*density).toInt() }
            a.strokeWidth?.let { v.strokeWidth=(it*density).toInt() }
            UiStudioEngine.parseColorSafe(a.strokeColor)?.let{v.strokeColor=ColorStateList.valueOf(it)}
        } else if(v is MaterialCardView) {
            a.cornerRadius?.let{v.radius=it*density}
            a.strokeWidth?.let{v.strokeWidth=(it*density).toInt()}
            UiStudioEngine.parseColorSafe(a.strokeColor)?.let{v.strokeColor=it}
        }
        if(a.cornerRadius!=null || a.shape!=null) {
            v.outlineProvider=object:ViewOutlineProvider(){override fun getOutline(view:View,outline:Outline){outline.setRoundRect(0,0,view.width,view.height,radius(view,c))}}
            v.clipToOutline=true
            if(a.shape in listOf("capsule","circle")) {
                fun updateRadius() {
                    if(v is MaterialButton)v.cornerRadius=radius(v,c).toInt()
                    if(v is MaterialCardView)v.radius=radius(v,c)
                }
                if(v.width>0 && v.height>0)updateRadius() else v.post { updateRadius() }
            }
        }
        if(a.highlightColor!=null || (a.strokeWidth ?: 0)>0 && v !is MaterialCardView && v !is MaterialButton) {
            val edge=Edge(c,density)
            v.foreground=v.foreground?.let{LayerDrawable(arrayOf(it,edge))} ?: edge
        }
        UiStudioEngine.parseColorSafe(a.iconTint)?.let {
            if(v is ImageView){v.clearColorFilter();v.imageTintList=ColorStateList.valueOf(it)}
            if(v is MaterialButton)v.iconTint=ColorStateList.valueOf(it)
            if(v is TextView && v !is MaterialButton)v.compoundDrawableTintList=ColorStateList.valueOf(it)
        }
        if(v is TextView)UiStudioEngine.parseColorSafe(s.selectedTextColor)?.let {
            v.setTextColor(ColorStateList(arrayOf(intArrayOf(android.R.attr.state_selected),intArrayOf(android.R.attr.state_checked),intArrayOf()),intArrayOf(it,it,v.currentTextColor)))
        }
    }
    /** Two restrained static gradient edges approximate specular depth; no optical refraction claim. */
    private class Edge(val c:ComponentConfig,val density:Float):Drawable(){
        val paint=Paint(Paint.ANTI_ALIAS_FLAG)
        override fun draw(canvas:Canvas){
            val r=RectF(bounds);val a=c.appearance
            val radius=if(a.shape in listOf("capsule","circle"))minOf(r.width(),r.height())/2 else (a.cornerRadius ?: 0)*density
            val width=(a.strokeWidth ?: 0)*density
            if(width>0){paint.style=Paint.Style.STROKE;paint.strokeWidth=width;paint.shader=null;paint.color=UiStudioEngine.parseColorSafe(a.strokeColor) ?: Color.TRANSPARENT;r.inset(width/2,width/2);canvas.drawRoundRect(r,radius,radius,paint)}
            UiStudioEngine.parseColorSafe(a.highlightColor)?.let { color->
                paint.style=Paint.Style.STROKE;paint.strokeWidth=density
                val bright=alpha(color,a.highlightOpacity ?: .35f)
                paint.shader=LinearGradient(0f,r.top,0f,r.bottom,intArrayOf(bright,Color.TRANSPARENT,alpha(color,(a.highlightOpacity ?: .35f)*.25f)),floatArrayOf(0f,.55f,1f),Shader.TileMode.CLAMP)
                r.inset(density/2,density/2);canvas.drawRoundRect(r,radius,radius,paint);paint.shader=null
            }
        }
        override fun setAlpha(alpha:Int){}
        override fun setColorFilter(filter:ColorFilter?){}
        @Deprecated("Drawable compatibility") override fun getOpacity()=PixelFormat.TRANSLUCENT
    }
}
