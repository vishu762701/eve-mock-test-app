package com.eve.app.uistudio

import com.eve.app.data.model.uistudio.*

/** Shortcuts composed entirely of the same editable renderer fields. */
object StudioPresets {
    val names=listOf("Clear Glass","Frosted Glass","Tinted Glass","Floating Glass","Light Glass","Dark Glass","iOS 26-style surface")
    fun apply(c:ComponentConfig,name:String,dark:Boolean):ComponentConfig {
        val neutral=if(dark)"#000000" else "#FFFFFF"
        val material=when(name){
            "Clear Glass"->MaterialProperties(6,1f,neutral,.08f)
            "Frosted Glass"->MaterialProperties(22,1f,neutral,.6f)
            "Tinted Glass"->MaterialProperties(14,1f,"#007AFF",.25f)
            "Light Glass"->MaterialProperties(18,1f,"#FFFFFF",.45f)
            "Dark Glass"->MaterialProperties(18,1f,"#000000",.5f)
            else->MaterialProperties(18,.95f,neutral,.3f)
        }
        return c.copy(material=material,appearance=c.appearance.copy(shape="rounded",cornerRadius=24,strokeWidth=1,strokeColor=if(dark)"#40FFFFFF" else "#40000000",highlightColor="#FFFFFF",highlightOpacity=.4f,elevation=if(name=="Floating Glass")12 else 4))
    }
}
