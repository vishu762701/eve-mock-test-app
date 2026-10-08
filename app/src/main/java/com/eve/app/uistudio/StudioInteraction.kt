package com.eve.app.uistudio

import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewTreeObserver
import android.view.animation.OvershootInterpolator
import android.view.animation.DecelerateInterpolator
import com.eve.app.data.model.uistudio.AnimationProperties
import java.lang.ref.WeakReference
import java.util.WeakHashMap

/** Observe native pressed state without replacing touch/click listeners or consuming business gestures. */
object StudioInteraction {
    private class Binding(v:View,val c:AnimationProperties):ViewTreeObserver.OnPreDrawListener {
        val target=WeakReference(v);val x=v.scaleX;val y=v.scaleY;var pressed=false
        val colors=if(v is com.google.android.material.card.MaterialCardView)v.cardBackgroundColor else v.backgroundTintList
        var lastColor=colors?.getColorForState(v.drawableState,colors.defaultColor)
        var tintAnimator:android.animation.ValueAnimator?=null
        var renderedColor: Int?=null
        override fun onPreDraw():Boolean {
            val v=target.get() ?: return true
            val desired=colors?.getColorForState(v.drawableState,colors.defaultColor)
            if(c.stateTransitionMs>0 && desired!=null && desired!=lastColor){
                tintAnimator?.cancel()
                val start=lastColor ?: desired;lastColor=desired
                tintAnimator=transition(v,start,desired,c.stateTransitionMs){color->
                    val target=target.get() ?: return@transition
                    renderedColor=color
                    if(target is com.google.android.material.card.MaterialCardView)target.setCardBackgroundColor(color) else target.backgroundTintList=android.content.res.ColorStateList.valueOf(color)
                }
            }
            if(pressed!=v.isPressed){
                pressed=v.isPressed
                val scale=if(pressed)c.pressScale ?: 1f else 1f
                val durationScale=Settings.Global.getFloat(v.context.contentResolver,Settings.Global.ANIMATOR_DURATION_SCALE,1f)
                if(durationScale==0f){v.scaleX=x*scale;v.scaleY=y*scale}
                else v.animate().setStartDelay(0).scaleX(x*scale).scaleY(y*scale).setDuration(if(pressed)100 else 220).setInterpolator(if(!pressed && c.springRelease)OvershootInterpolator(.8f) else DecelerateInterpolator()).start()
                if(pressed && c.hapticFeedback)v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            }
            return true
        }
    }
    private val bindings=WeakHashMap<View,Binding>()
    fun transition(v:View,from:Int,to:Int,duration:Long,apply:(Int)->Unit):android.animation.ValueAnimator? {
        val scale=Settings.Global.getFloat(v.context.contentResolver,Settings.Global.ANIMATOR_DURATION_SCALE,1f)
        if(duration==0L || scale==0f){apply(to);return null}
        return android.animation.ValueAnimator.ofArgb(from,to).apply{this.duration=duration;addUpdateListener{apply(it.animatedValue as Int)};start()}
    }
    // State transitions temporarily install single colors; those are renderer-owned, not a rebind.
    fun stableColors(v: View): android.content.res.ColorStateList? {
        val actual=(v as? com.google.android.material.card.MaterialCardView)?.cardBackgroundColor ?: v.backgroundTintList
        val binding=bindings[v]
        return if(binding!=null && binding.renderedColor!=null && actual?.defaultColor==binding.renderedColor)
            binding.colors else actual
    }
    fun clear(v:View){bindings.remove(v)?.let { if(v.viewTreeObserver.isAlive)v.viewTreeObserver.removeOnPreDrawListener(it);it.tintAnimator?.cancel();v.animate().cancel();v.scaleX=it.x;v.scaleY=it.y }}
    fun apply(v:View,c:AnimationProperties){
        if(c.pressScale==null && !c.hapticFeedback && c.stateTransitionMs==0L)return
        val b=Binding(v,c);bindings[v]=b;v.viewTreeObserver.addOnPreDrawListener(b)
    }
}
