package com.eve.app.uistudio

import android.graphics.Color
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.appcompat.widget.Toolbar
import com.eve.app.R
import com.eve.app.data.model.uistudio.ComponentConfig
import com.eve.app.util.UiStudioEngine
import com.google.android.material.card.MaterialCardView
import eightbitlab.com.blurview.BlurView
import eightbitlab.com.blurview.RenderEffectBlur
import java.lang.ref.WeakReference

/** Backdrop-only hosts draw before foreground. Native views/IDs/layout params and actions stay in place. */
object StudioMaterial {
    fun bannerMatte(view: View): View? = if(view.id==R.id.panelHomeBanner)
        view.findViewById(R.id.homeBannerMatte) else null
    private fun container(v:View?)=v is LinearLayout && v.showDividers==LinearLayout.SHOW_DIVIDER_NONE || v is ConstraintLayout || v is FrameLayout && v !is ScrollView && v !is androidx.core.widget.NestedScrollView && v !is BlurView && v !is androidx.recyclerview.widget.RecyclerView && v !is androidx.viewpager2.widget.ViewPager2
    fun supported(view:View?)=container(view) || view is Button && container(view.parent as? View)
    private class Effect(val blur:BlurView,val host:View,val parent:ViewGroup,target:View,val sibling:Boolean):ViewTreeObserver.OnPreDrawListener {
        val target=WeakReference(target);var config=ComponentConfig();var lastColor:Int?=null;var tintAnimator:android.animation.ValueAnimator?=null
        override fun onPreDraw():Boolean {
            val v=target.get() ?: return true
            val m=config.material;val states=config.states
            val stateColor=when{!v.isEnabled->states.disabledBackgroundColor;v.isPressed->states.pressedBackgroundColor;v.isSelected || v is android.widget.Checkable && v.isChecked->states.selectedBackgroundColor;v.isFocused->states.focusedBackgroundColor;else->null}
            val tint=UiStudioEngine.parseColorSafe(stateColor ?: m.tintColor) ?: if((v.resources.configuration.uiMode and 0x30)==0x20)Color.BLACK else Color.WHITE
            val tintLayer=(tint and 0xffffff) or ((Color.alpha(tint)*(if(stateColor!=null)1f else m.tintOpacity ?: .2f)).toInt().coerceIn(0,255) shl 24)
            val color=androidx.core.graphics.ColorUtils.compositeColors(tintLayer,UiStudioEngine.parseColorSafe(config.appearance.backgroundColor) ?: Color.TRANSPARENT)
            if(lastColor!=color){tintAnimator?.cancel();tintAnimator=StudioInteraction.transition(v,lastColor ?: color,color,config.animation.stateTransitionMs){blur.setOverlayColor(it)};lastColor=color}
            val width=v.width;val height=v.height
            if(host!==blur && width>0 && height>0){
                if(!(parent is ConstraintLayout && !sibling) && (host.layoutParams.width!=width || host.layoutParams.height!=height))host.layoutParams=parameters(parent,width,height,sibling)
                host.visibility=v.visibility
                host.alpha=(config.material.materialOpacity ?: 1f)*(if(sibling)v.alpha else 1f)
                host.translationX=(if(sibling)v.left else 0)-host.left+(if(sibling)v.translationX else 0f)
                host.translationY=(if(sibling)v.top else 0)-host.top+(if(sibling)v.translationY else 0f)
                if(sibling){host.scaleX=v.scaleX;host.scaleY=v.scaleY;host.pivotX=v.pivotX;host.pivotY=v.pivotY;host.elevation=(v.elevation-.01f).coerceAtLeast(0f)}
            }
            return true
        }
        fun remove(){
            if(parent.viewTreeObserver.isAlive)parent.viewTreeObserver.removeOnPreDrawListener(this)
            tintAnimator?.cancel();blur.setBlurAutoUpdate(false);parent.removeView(host)
        }
    }
    private fun parameters(parent:ViewGroup,width:Int,height:Int,sibling:Boolean):ViewGroup.LayoutParams {
        if(parent is ConstraintLayout)return if(!sibling)ConstraintLayout.LayoutParams(0,0).apply{leftToLeft=0;rightToRight=0;topToTop=0;bottomToBottom=0}else ConstraintLayout.LayoutParams(width,height)
        if(parent is LinearLayout)return LinearLayout.LayoutParams(width,height).apply{rightMargin=-width;bottomMargin=-height}
        return FrameLayout.LayoutParams(width,height)
    }
    fun apply(view:View,config:ComponentConfig){
        var effect=view.getTag(R.id.studio_material_effect) as? Effect
        val radius=config.material.blurRadius ?: 0
        if(radius==0 || !supported(view)){
            effect?.remove();view.setTag(R.id.studio_material_effect,null)
            bannerMatte(view)?.let { StudioBaseline.visibility(it,false) }
            (view as? ViewGroup)?.let { host->(0 until host.childCount).map{host.getChildAt(it)}.filterIsInstance<BlurView>().forEach{StudioBaseline.visibility(it,false)} }
            return
        }
        if(effect==null){
            val sibling=view is Button
            val parent=if(sibling)view.parent as? ViewGroup else view as? ViewGroup
            parent ?: return
            val root=generateSequence(parent as ViewGroup){it.parent as? ViewGroup}.last()
            val blur=BlurView(view.context).apply{tag="studio_backdrop";importantForAccessibility=View.IMPORTANT_FOR_ACCESSIBILITY_NO;isClickable=false;isFocusable=false}
            val direct=view is FrameLayout && !sibling
            val host=if(direct)blur else FrameLayout(view.context).apply{tag="studio_glass_host";importantForAccessibility=View.IMPORTANT_FOR_ACCESSIBILITY_NO;addView(blur,FrameLayout.LayoutParams(-1,-1))}
            val index=if(sibling)parent.indexOfChild(view) else bannerMatte(view)?.let { parent.indexOfChild(it) } ?: 0
            parent.addView(host,index,if(direct)FrameLayout.LayoutParams(-1,-1) else parameters(parent,view.width,view.height,sibling))
            if(Build.VERSION.SDK_INT>=31)blur.setupWith(root,RenderEffectBlur()) else blur.setupWith(root)
            effect=Effect(blur,host,parent,view,sibling)
            view.setTag(R.id.studio_material_effect,effect)
            parent.viewTreeObserver.addOnPreDrawListener(effect)
        }
        effect.config=config
        bannerMatte(view)?.let { StudioBaseline.visibility(it,true) }
        (view as? ViewGroup)?.let { host->(0 until host.childCount).map{host.getChildAt(it)}.filterIsInstance<BlurView>().filter{it!==effect.blur}.forEach{StudioBaseline.visibility(it,true)} }
        // Preserve MaterialButton's own ripple and shape; only its fill becomes transparent.
        if(view is MaterialCardView)view.setCardBackgroundColor(Color.TRANSPARENT)
        else if(view is com.google.android.material.button.MaterialButton)view.backgroundTintList=android.content.res.ColorStateList.valueOf(Color.TRANSPARENT)
        else view.backgroundTintList=android.content.res.ColorStateList.valueOf(Color.TRANSPARENT)
        val blur=effect.blur;val m=config.material
        blur.setBlurRadius(radius.coerceIn(1,25).toFloat())
        blur.alpha=if(effect.host===blur)m.materialOpacity ?: 1f else 1f
        blur.outlineProvider=object:android.view.ViewOutlineProvider(){override fun getOutline(v:View,o:android.graphics.Outline){o.setRoundRect(0,0,v.width,v.height,if(config.appearance.shape!=null || config.appearance.cornerRadius!=null)StudioVisual.radius(v,config) else 16*v.resources.displayMetrics.density)}}
        blur.clipToOutline=true;blur.setBlurAutoUpdate(true);effect.onPreDraw()
    }
}
