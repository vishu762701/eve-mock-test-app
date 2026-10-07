package com.eve.app.uistudio

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import com.eve.app.util.UiStudioEngine

/** Visual HSV picker; HEX is an optional precise entry, not the main editing path. */
object StudioColorPicker {
    fun show(context:Context,initial:String?,apply:(String)->Unit):AlertDialog{
        val hsv=FloatArray(3);val initialColor=UiStudioEngine.parseColorSafe(initial) ?: Color.rgb(0,122,255)
        Color.colorToHSV(initialColor,hsv)
        val alpha=Color.alpha(initialColor)
        val box=LinearLayout(context).apply{orientation=LinearLayout.VERTICAL;setPadding(24,16,24,16)}
        val sample=TextView(context).apply{textSize=18f;minHeight=64;gravity=android.view.Gravity.CENTER};box.addView(sample)
        fun update(){val c=Color.HSVToColor(alpha,hsv);sample.text=String.format("#%08X",c);sample.setBackgroundColor(c);sample.setTextColor(if(hsv[2]>.65f && hsv[1]<.6f)Color.BLACK else Color.WHITE)}
        val plane=object:View(context){
            val paint=Paint(Paint.ANTI_ALIAS_FLAG)
            override fun onDraw(canvas:Canvas){
                paint.shader=LinearGradient(0f,0f,width.toFloat(),0f,Color.WHITE,Color.HSVToColor(floatArrayOf(hsv[0],1f,1f)),Shader.TileMode.CLAMP);canvas.drawRect(0f,0f,width.toFloat(),height.toFloat(),paint)
                paint.shader=LinearGradient(0f,0f,0f,height.toFloat(),Color.TRANSPARENT,Color.BLACK,Shader.TileMode.CLAMP);canvas.drawRect(0f,0f,width.toFloat(),height.toFloat(),paint)
                paint.shader=null;paint.color=Color.WHITE;paint.style=Paint.Style.STROKE;paint.strokeWidth=3f;canvas.drawCircle(hsv[1]*width,(1-hsv[2])*height,10f,paint);paint.style=Paint.Style.FILL
            }
            override fun onTouchEvent(e:MotionEvent):Boolean{if(e.actionMasked==MotionEvent.ACTION_DOWN || e.actionMasked==MotionEvent.ACTION_MOVE){parent.requestDisallowInterceptTouchEvent(true);hsv[1]=(e.x/width).coerceIn(0f,1f);hsv[2]=(1-e.y/height).coerceIn(0f,1f);invalidate();update()};if(e.actionMasked==MotionEvent.ACTION_UP)performClick();return true}
            override fun performClick():Boolean{super.performClick();return true}
        }.apply{contentDescription="Color saturation and brightness"}
        box.addView(plane,LinearLayout.LayoutParams(-1,(180*context.resources.displayMetrics.density).toInt()))
        box.addView(TextView(context).apply{text="Hue"})
        box.addView(SeekBar(context).apply{max=360;progress=hsv[0].toInt();contentDescription="Color hue";setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener{override fun onStartTrackingTouch(s:SeekBar?){};override fun onStopTrackingTouch(s:SeekBar?){};override fun onProgressChanged(s:SeekBar?,p:Int,user:Boolean){if(user){hsv[0]=p.toFloat();plane.invalidate();update()}}})})
        update()
        return AlertDialog.Builder(context).setTitle("Pick a color").setView(box).setPositiveButton("Apply"){_,_->apply(String.format("#%08X",Color.HSVToColor(alpha,hsv)))}.setNegativeButton("Cancel",null).show()
    }
}
