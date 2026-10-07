package com.eve.app.uistudio

import android.graphics.*
import android.view.View
import android.widget.*
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.eve.app.R
import com.eve.app.data.model.uistudio.*
import com.eve.app.ui.admin.uistudio.UiStudioActivity
import com.eve.app.ui.notifications.NotificationAdapter
import com.eve.app.util.StoredNotification
import com.eve.app.util.UiStudioEngine
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class StudioVisualFunctionalTest {
    private val instrumentation get()=InstrumentationRegistry.getInstrumentation()
    private fun shot(name:String):Bitmap? {
        Thread.sleep(3000);instrumentation.waitForIdleSync()
        var bitmap=instrumentation.uiAutomation.takeScreenshot()
        repeat(2){if(bitmap==null){Thread.sleep(500);bitmap=instrumentation.uiAutomation.takeScreenshot()}}
        val captured=bitmap ?: run{android.util.Log.w("StudioEvidence","Screenshot unavailable: $name");return null}
        val dir=File(instrumentation.targetContext.getExternalFilesDir(null),"studio-visual-evidence").apply{mkdirs()}
        File(dir,"$name.png").outputStream().use{captured.compress(Bitmap.CompressFormat.PNG,100,it)}
        return captured
    }
    @Test fun nativeVisualsKeepActionsDataAndTrueBaseline(){
        ActivityScenario.launch(UiStudioActivity::class.java).use { scenario->
            lateinit var root:View;lateinit var card:MaterialCardView;lateinit var button:MaterialButton
            var clicks=0;var originalColor=0;var originalRadius=0f
            scenario.onActivity { activity->
                root=activity.layoutInflater.inflate(R.layout.activity_login,null);activity.setContentView(root)
                val result=activity.layoutInflater.inflate(R.layout.activity_result,null)
                card=result.findViewById(R.id.cardPerformanceStanding)
                (card.parent as android.view.ViewGroup).removeView(card)
                root.findViewById<LinearLayout>(R.id.cardLogin).addView(card,0)
                button=root.findViewById(R.id.btnSubmit)
                originalColor=card.cardBackgroundColor.defaultColor;originalRadius=card.radius
                button.setOnClickListener{clicks++}
                val components=mapOf(
                    "native_cardPerformanceStanding" to ComponentConfig(id="native_cardPerformanceStanding",isProtected=true,appearance=AppearanceProperties(backgroundColor="#FF0000",opacity=.2f,cornerRadius=24,strokeWidth=2,strokeColor="#00FF00")),
                    "native_tvLogo" to ComponentConfig(id="native_tvLogo",typography=TypographyProperties(textColor="#00FF00",textSize=28,fontFamily="serif")),
                    "native_btnSubmit" to ComponentConfig(id="native_btnSubmit",isProtected=true,appearance=AppearanceProperties(backgroundColor="#0000FF",cornerRadius=18),states=StateProperties(pressedBackgroundColor="#00FF00",selectedBackgroundColor="#FF0000",focusedBackgroundColor="#FFFF00"),animation=AnimationProperties(pressScale=.94f,springRelease=true))
                )
                val config=UiStudioConfig(screens=mapOf("login" to ScreenConfig(components=components)))
                assertTrue(StudioPolicy.validate(config).isEmpty())
                val elements=StudioRenderer.apply(root,"login",config)
                assertTrue(elements.any{it.id=="native_tvLogo" && it.parent!=null})
                assertEquals(Color.RED,card.cardBackgroundColor.defaultColor);assertEquals(.2f,card.alpha,.001f)
                assertEquals(24*activity.resources.displayMetrics.density,card.radius,.01f);assertEquals(Color.GREEN,card.strokeColor)
                assertEquals(Color.GREEN,root.findViewById<TextView>(R.id.tvLogo).currentTextColor)
                button.performClick();assertEquals(1,clicks)
                button.isSelected=true;assertEquals(Color.RED,button.backgroundTintList!!.getColorForState(button.drawableState,0));button.isSelected=false
                button.isPressed=true;assertEquals(Color.GREEN,button.backgroundTintList!!.getColorForState(button.drawableState,0))
            }
            instrumentation.waitForIdleSync();Thread.sleep(350)
            scenario.onActivity{
                assertEquals(.94f,button.scaleX,.01f);button.isPressed=false
                val pixels=Bitmap.createBitmap(card.width,card.height,Bitmap.Config.ARGB_8888)
                card.draw(Canvas(pixels))
                var greenEdge=0
                for(y in 0 until pixels.height)for(x in 0 until pixels.width)if(x<4 || y<4 || x>=pixels.width-4 || y>=pixels.height-4){val color=pixels.getPixel(x,y);if(Color.green(color)>200 && Color.red(color)<100 && Color.blue(color)<100)greenEdge++}
                assertTrue("Native MaterialCard border must actually draw, not only report a property",greenEdge>10)
                pixels.recycle()
            }
            shot("01-native-visuals")
            scenario.onActivity {
                StudioRenderer.apply(root,"login",UiStudioConfig())
                assertEquals(originalColor,card.cardBackgroundColor.defaultColor);assertEquals(originalRadius,card.radius,.001f);assertEquals(1f,card.alpha,.001f);assertEquals(1f,button.scaleX,.001f)
                button.performClick();assertEquals(2,clicks)
                val pill=View(root.context).apply{background=android.graphics.drawable.GradientDrawable().apply{setColor(Color.WHITE);cornerRadius=32f;setStroke(3,Color.RED)}}
                UiStudioEngine.applyToView(pill,ComponentConfig(id="native_pill",appearance=AppearanceProperties(cornerRadius=0,strokeWidth=0)))
                assertEquals(0f,(pill.background as android.graphics.drawable.GradientDrawable).cornerRadius,.001f)
                UiStudioEngine.applyToView(pill,ComponentConfig(id="native_pill",appearance=AppearanceProperties(backgroundColor="#0000FF")))
                assertEquals(32f,(pill.background as android.graphics.drawable.GradientDrawable).cornerRadius,.001f)
            }
            shot("02-native-reset")
        }
    }
    @Test fun linearGlassAndButtonHostKeepSharpForegroundAndIndependentOpacity(){
        ActivityScenario.launch(UiStudioActivity::class.java).use { scenario->
            lateinit var root:FrameLayout;lateinit var panel:LinearLayout;lateinit var label:TextView;lateinit var button:MaterialButton
            lateinit var glass:ComponentConfig
            scenario.onActivity { activity->
                root=FrameLayout(activity)
                root.addView(TextView(activity).apply{text=("BACKDROP STRIPES ").repeat(250);textSize=24f;setTextColor(Color.RED);setBackgroundColor(Color.WHITE)},FrameLayout.LayoutParams(-1,-1))
                panel=LinearLayout(activity).apply{orientation=LinearLayout.VERTICAL;setBackgroundColor(Color.WHITE)}
                label=TextView(activity).apply{text="Sharp foreground";textSize=24f;setTextColor(Color.BLACK)}
                panel.addView(label);root.addView(panel,FrameLayout.LayoutParams(280,180).apply{leftMargin=20;topMargin=120})
                button=MaterialButton(activity).apply{text="Native action"};root.addView(button,FrameLayout.LayoutParams(280,64).apply{leftMargin=20;topMargin=350})
                activity.setContentView(root)
            }
            val before=requireNotNull(shot("03-linear-before"))
            scenario.onActivity {
                val c=StudioPresets.apply(ComponentConfig(id="native_panel"),"Floating Glass",false).copy(appearance=AppearanceProperties(opacity=.8f,cornerRadius=24,strokeWidth=2,strokeColor="#FFFFFF",highlightColor="#FFFFFF",highlightOpacity=.6f,elevation=8),material=MaterialProperties(22,.6f,"#007AFF",.2f))
                assertTrue(StudioMaterial.supported(panel));assertTrue(StudioMaterial.supported(button));assertFalse(StudioMaterial.supported(label))
                glass=c
                UiStudioEngine.applyToView(panel,c.copy(material=c.material.copy(blurRadius=1)));UiStudioEngine.applyToView(button,c.copy(id="native_button",material=c.material.copy(blurRadius=1)))
            }
            val low=requireNotNull(shot("08-glass-low-radius"))
            scenario.onActivity{UiStudioEngine.applyToView(panel,glass);UiStudioEngine.applyToView(button,glass.copy(id="native_button"))}
            val after=requireNotNull(shot("04-linear-and-button-glass"))
            var radiusChanged=0;for(y in 190..290)for(x in 30..260)if(low.getPixel(x,y)!=after.getPixel(x,y))radiusChanged++
            assertTrue("Blur radius alone must change backdrop pixels, with tint/opacity fixed ($radiusChanged)",radiusChanged>100)
            scenario.onActivity {
                val host=panel.findViewWithTag<View>("studio_glass_host")
                assertEquals(.8f,panel.alpha,.001f);assertEquals(.6f,host.alpha,.001f);assertEquals(1f,label.alpha,.001f)
                assertEquals("Sharp foreground",label.text.toString());assertNotNull(panel.foreground)
                assertTrue(host.width>0 && host.height>0)
                assertEquals(1,label.parent.let{(it as LinearLayout).indexOfChild(label)})
            }
            var changed=0;for(y in 190..290)for(x in 30..260)if(before.getPixel(x,y)!=after.getPixel(x,y))changed++
            assertTrue("Actual backdrop pixels change ($changed)",changed>100)
            scenario.onActivity {
                UiStudioEngine.applyToView(panel,ComponentConfig(id="native_panel"));UiStudioEngine.applyToView(button,ComponentConfig(id="native_button"))
                assertNull(panel.findViewWithTag<View>("studio_glass_host"));assertNull(root.findViewWithTag<View>("studio_glass_host"));assertNull(panel.foreground);assertEquals(1f,panel.alpha,.001f);assertEquals(3,root.childCount);assertEquals(1,panel.childCount)
            }
            shot("05-linear-reset")
        }
    }
    @Test fun realRepeatedAdapterTextAndAnonymousIconsShareTemplateStyle(){
        ActivityScenario.launch(UiStudioActivity::class.java).use { scenario->
            lateinit var root:View
            scenario.onActivity { activity->
                root=activity.layoutInflater.inflate(R.layout.activity_notifications,null);activity.setContentView(root)
                StudioFixtures.bind(root,"notifications",{UiStudioConfig()},{})
                (root.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvNotifications).adapter as NotificationAdapter).submit(listOf(StoredNotification("First title","First body",1L,false),StoredNotification("Second title","Second body",2L,false)))
            }
            instrumentation.waitForIdleSync()
            scenario.onActivity {
                val elements=StudioRenderer.elements(root)
                val titles=elements.filter{it.id=="native_tvTitle"};assertEquals(2,titles.size);assertTrue(titles.all{it.repeated})
                val icons=elements.filter{it.view is ImageView && it.repeated};assertEquals(2,icons.size);assertEquals(icons[0].id,icons[1].id)
                val text=ComponentConfig(id="native_tvTitle",typography=TypographyProperties(textColor="#FF0000",textSize=22))
                val icon=ComponentConfig(id=icons[0].id,appearance=AppearanceProperties(iconTint="#34C759"))
                StudioRenderer.apply(root,"notifications",UiStudioConfig(screens=mapOf("notifications" to ScreenConfig(components=mapOf(text.id to text,icon.id to icon)))))
                titles.forEach{assertEquals(Color.RED,(it.view as TextView).currentTextColor)}
                icons.forEach{assertEquals(Color.parseColor("#34C759"),(it.view as ImageView).imageTintList!!.defaultColor)}
                assertEquals("First title",(titles[0].view as TextView).text.toString());assertEquals("Second title",(titles[1].view as TextView).text.toString())
                StudioRenderer.apply(root,"notifications",UiStudioConfig());icons.forEach{assertNotEquals(Color.parseColor("#34C759"),(it.view as ImageView).imageTintList?.defaultColor)}
            }
            shot("06-real-repeated-templates")
        }
    }
    @Test fun visualColorPickerAndLowOpacityReachOnlySelectedNativeItem(){
        val repo=com.eve.app.data.repository.UiStudioRepository.getInstance();repo.saveDraftToLocalCache(UiStudioConfig())
        fun views(v:View):List<View> = listOf(v)+(if(v is android.view.ViewGroup)(0 until v.childCount).flatMap{views(v.getChildAt(it))}else emptyList())
        ActivityScenario.launch(UiStudioActivity::class.java).use { scenario->
            instrumentation.waitForIdleSync()
            scenario.onActivity { activity->views(activity.findViewById(android.R.id.content)).filterIsInstance<Button>().first{it.text=="Colors"}.performClick() }
            instrumentation.waitForIdleSync()
            scenario.onActivity { activity->
                UiStudioActivity::class.java.getDeclaredField("selected").apply{isAccessible=true}.set(activity,"native_tvStreakSummary")
                views(activity.findViewById(android.R.id.content)).filterIsInstance<Button>().first{it.text=="Controls"}.performClick()
                val sheet=UiStudioActivity::class.java.getDeclaredField("sheet").apply{isAccessible=true}.get(activity) as com.google.android.material.bottomsheet.BottomSheetDialog
                views(sheet.window!!.decorView).filterIsInstance<Button>().first{it.text=="Pick Item background"}.performClick()
            }
            instrumentation.waitForIdleSync()
            scenario.onActivity { activity->
                val picker=UiStudioActivity::class.java.getDeclaredField("colorDialog").apply{isAccessible=true}.get(activity) as androidx.appcompat.app.AlertDialog
                val hue=views(picker.window!!.decorView).filterIsInstance<SeekBar>().first()
                val args=android.os.Bundle().apply{putFloat(android.view.accessibility.AccessibilityNodeInfo.ACTION_ARGUMENT_PROGRESS_VALUE,0f)}
                assertTrue(hue.performAccessibilityAction(android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS.id,args))
            }
            shot("07-visual-color-picker")
            scenario.onActivity { activity->
                val picker=UiStudioActivity::class.java.getDeclaredField("colorDialog").apply{isAccessible=true}.get(activity) as androidx.appcompat.app.AlertDialog
                picker.getButton(android.content.DialogInterface.BUTTON_POSITIVE).performClick()
            }
            instrumentation.waitForIdleSync()
            scenario.onActivity { activity->
                val sheet=UiStudioActivity::class.java.getDeclaredField("sheet").apply{isAccessible=true}.get(activity) as com.google.android.material.bottomsheet.BottomSheetDialog
                val opacity=views(sheet.window!!.decorView).filterIsInstance<SeekBar>().first()
                val bundle=android.os.Bundle().apply{putFloat(android.view.accessibility.AccessibilityNodeInfo.ACTION_ARGUMENT_PROGRESS_VALUE,5f)}
                assertTrue(opacity.performAccessibilityAction(android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS.id,bundle))
                val config=repo.loadCachedDraft()!!;val edited=config.screens.getValue("home").components.getValue("native_tvStreakSummary")
                assertEquals(Color.RED,UiStudioEngine.parseColorSafe(edited.appearance.backgroundColor));assertEquals(.05f,edited.appearance.opacity!!,.001f);assertNull(edited.material.blurRadius)
                assertEquals(1,config.screens.getValue("home").components.size)
                assertEquals(.05f,activity.findViewById<View>(R.id.tvStreakSummary).alpha,.001f)
                sheet.dismiss()
            }
            scenario.recreate()
            scenario.onActivity { assertEquals(.05f,repo.loadCachedDraft()!!.screens.getValue("home").components.getValue("native_tvStreakSummary").appearance.opacity!!,.001f) }
        }
    }

}
