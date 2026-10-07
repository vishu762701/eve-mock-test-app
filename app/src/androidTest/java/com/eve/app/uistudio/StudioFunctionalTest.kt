package com.eve.app.uistudio

import android.graphics.Bitmap
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.view.children
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.eve.app.data.model.uistudio.*
import com.eve.app.data.repository.UiStudioRepository
import com.eve.app.ui.admin.uistudio.UiStudioActivity
import com.eve.app.util.UiStudioEngine
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class StudioFunctionalTest {
    private val instrumentation get()=InstrumentationRegistry.getInstrumentation()
    private fun screenshot(name: String): Bitmap {
        Thread.sleep(3000)
        instrumentation.waitForIdleSync()
        instrumentation.uiAutomation.executeShellCommand("input keyevent KEYCODE_WAKEUP").close()
        var capture: Bitmap? = null
        repeat(5) { if(capture==null) { Thread.sleep(800);capture=instrumentation.uiAutomation.takeScreenshot() } }
        val bitmap=requireNotNull(capture) { "Emulator display screenshot unavailable" }
        val dir=File(instrumentation.targetContext.getExternalFilesDir(null),"studio-evidence").apply{mkdirs()}
        File(dir,"$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) }
        return bitmap
    }
    private fun views(root: View): List<View> = listOf(root)+(if(root is ViewGroup)root.children.flatMap { views(it) }.toList() else emptyList())
    @Test fun toolMenuAndDraftSurviveRecreation() {
        UiStudioRepository.getInstance().saveDraftToLocalCache(UiStudioConfig())
        ActivityScenario.launch(UiStudioActivity::class.java).use { scenario ->
            instrumentation.waitForIdleSync()
            screenshot("01-tools")
            scenario.onActivity { activity ->
                val root=activity.findViewById<View>(android.R.id.content)
                assertTrue(views(root).filterIsInstance<Button>().any{it.text=="Blur & Glass"})
                assertFalse(views(root).any{it is SeekBar})
                views(root).filterIsInstance<Button>().first{it.text=="Blur & Glass"}.performClick()
            }
            instrumentation.waitForIdleSync()
            screenshot("02-native-preview")
            scenario.recreate()
            scenario.onActivity { assertNotNull(UiStudioRepository.getInstance().loadCachedDraft()) }
        }
    }
    @Test fun sharedRendererInsertArrangeHideResetAndNestedSelection() {
        UiStudioRepository.getInstance().saveDraftToLocalCache(UiStudioConfig())
        ActivityScenario.launch(UiStudioActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val root=LinearLayout(activity).apply { orientation=LinearLayout.VERTICAL;addView(TextView(activity).apply{text="Native score: 18 / 20"}) }
                activity.setContentView(root)
                val card=ComponentConfig(id="custom_card",type="card",appearance=AppearanceProperties(backgroundColor="#007AFF",cornerRadius=20),layout=LayoutProperties(height="120"))
                val title=ComponentConfig(id="custom_title",type="text",parentId=card.id,content=ContentProperties(title="Static announcement"),typography=TypographyProperties(textColor="#FFFFFF",textSize=22))
                var config=UiStudioConfig(screens=mapOf("home" to ScreenConfig(id="home",components=mapOf(card.id to card,title.id to title))))
                var items=StudioRenderer.apply(root,"home",config)
                val text=items.first{it.id==title.id}.view as TextView
                assertEquals("Static announcement",text.text.toString())
                assertEquals(card.id,items.first{it.id==title.id}.parent)
                val frame=items.first{it.id==card.id}.view
                assertFalse("A label without an action must not swallow its card's action",text.isClickable)
                var actions=0
                val linked=config.copy(screens=mapOf("home" to config.screens.getValue("home").copy(components=mapOf(card.id to card.copy(actions=ActionProperties("navigate","about")),title.id to title))))
                StudioRenderer.apply(root,"home",linked,sandbox={actions++})
                frame.performClick();assertEquals(1,actions)
                StudioRenderer.apply(root,"home",config)
                val huge=config.copy(screens=mapOf("home" to config.screens.getValue("home").copy(components=mapOf(card.id to card.copy(layout=LayoutProperties(height="600")),title.id to title))))
                StudioRenderer.apply(root,"home",huge)
                val viewport=root.findViewWithTag<View>("studio_insertion_viewport")
                root.measure(View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(700,View.MeasureSpec.EXACTLY))
                root.layout(0,0,360,700)
                assertTrue("Insertion must leave room for native content",viewport.measuredHeight<=activity.resources.displayMetrics.heightPixels/3)
                StudioRenderer.apply(root,"home",config)
                config=config.copy(screens=mapOf("home" to config.screens.getValue("home").copy(components=mapOf(card.id to card.copy(visible=false),title.id to title))))
                StudioRenderer.apply(root,"home",config);assertEquals(View.GONE,frame.visibility)
                config=config.copy(screens=mapOf("home" to config.screens.getValue("home").copy(components=mapOf(card.id to card,title.id to title))))
                StudioRenderer.apply(root,"home",config);assertEquals(View.VISIBLE,frame.visibility)
                assertEquals("Native score: 18 / 20",root.children.filterIsInstance<TextView>().first().text.toString())
                UiStudioRepository.getInstance().saveDraftToLocalCache(config)
                assertEquals(config,UiStudioRepository.getInstance().loadCachedDraft())
                assertNotEquals(config,UiStudioRepository.getInstance().currentConfig)
                StudioRenderer.apply(root,"home",UiStudioConfig());assertNull(root.findViewWithTag<View>("custom_card"))
            }
            screenshot("03-reset-native")
        }
    }
    @Test fun materialKeepsForegroundAndZeroRemovesEffect() {
        ActivityScenario.launch(UiStudioActivity::class.java).use { scenario ->
            lateinit var surface: FrameLayout
            lateinit var foreground: TextView
            var baseline=0f
            scenario.onActivity { activity ->
                val root=FrameLayout(activity)
                val background=TextView(activity).apply { text=("BACKDROP • STRIPES • ").repeat(200);textSize=24f;setTextColor(Color.RED);setBackgroundColor(Color.WHITE) }
                root.addView(background,FrameLayout.LayoutParams(-1,-1))
                surface=FrameLayout(activity)
                foreground=TextView(activity).apply { text="Sharp foreground 123";textSize=24f;setTextColor(Color.BLACK) }
                surface.addView(foreground,FrameLayout.LayoutParams(-1,-2))
                root.addView(surface,FrameLayout.LayoutParams(280,180).apply { topMargin=120;leftMargin=20 })
                activity.setContentView(root)
                baseline=foreground.textSize
            }
            val before=screenshot("04-material-before")
            scenario.onActivity {
                UiStudioEngine.applyToView(surface,ComponentConfig(id="custom_surface",material=MaterialProperties(20,1f,"#FFFFFF",.25f)))
                assertEquals(2,surface.childCount)
                assertEquals(baseline,foreground.textSize,.001f)
                assertEquals(1f,foreground.alpha,.001f)
            }
            val blurred=screenshot("05-material-blur")
            scenario.onActivity {
                assertTrue("Backdrop must have a laid out width",surface.getChildAt(0).width>0)
                assertTrue("Backdrop must have a laid out height",surface.getChildAt(0).height>0)
            }
            var changed=0
            for(y in 190..290) for(x in 30..260) if(before.getPixel(x,y)!=blurred.getPixel(x,y))changed++
            assertTrue("Backdrop pixels must change, not just the view hierarchy ($changed)",changed>100)
            scenario.onActivity {
                UiStudioEngine.applyToView(surface,ComponentConfig(id="custom_surface",material=MaterialProperties(0,1f)))
                assertEquals(1,surface.childCount)
                assertEquals("Sharp foreground 123",foreground.text.toString())
            }
            screenshot("06-material-zero")
        }
    }
    @Test fun typographyControlUndoAndReopenUseRecoverableDraft() {
        UiStudioRepository.getInstance().saveDraftToLocalCache(UiStudioConfig())
        ActivityScenario.launch(UiStudioActivity::class.java).use { scenario ->
            instrumentation.waitForIdleSync()
            scenario.onActivity { activity ->
                val root=activity.findViewById<View>(android.R.id.content)
                views(root).filterIsInstance<Button>().first{it.text=="Text & Fonts"}.performClick()
            }
            instrumentation.waitForIdleSync()
            scenario.onActivity { activity ->
                val field=UiStudioActivity::class.java.getDeclaredField("selected").apply{isAccessible=true}
                field.set(activity,"native_tvStreakSummary")
                views(activity.findViewById(android.R.id.content)).filterIsInstance<Button>().first{it.text=="Controls"}.performClick()
                val sheet=UiStudioActivity::class.java.getDeclaredField("sheet").apply{isAccessible=true}.get(activity) as com.google.android.material.bottomsheet.BottomSheetDialog
                val seek=views(sheet.window!!.decorView).filterIsInstance<SeekBar>().first()
                val args=android.os.Bundle().apply { putFloat(android.view.accessibility.AccessibilityNodeInfo.ACTION_ARGUMENT_PROGRESS_VALUE,20f) }
                assertTrue(seek.performAccessibilityAction(android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS.id,args))
                val config=UiStudioRepository.getInstance().loadCachedDraft()!!
                val c=config.screens.getValue("home").components.getValue("native_tvStreakSummary")
                assertEquals(30,c.typography.textSize)
                assertNull(c.material.blurRadius)
                assertEquals(30*activity.resources.displayMetrics.scaledDensity,activity.findViewById<TextView>(com.eve.app.R.id.tvStreakSummary).textSize,.1f)
                sheet.dismiss()
            }
            screenshot("07-typography-control")
            scenario.recreate()
            scenario.onActivity { activity ->
                assertEquals(30,UiStudioRepository.getInstance().loadCachedDraft()!!.screens.getValue("home").components.getValue("native_tvStreakSummary").typography.textSize)
                views(activity.findViewById(android.R.id.content)).filterIsInstance<Button>().first{it.text=="Undo"}.performClick()
                assertFalse(UiStudioRepository.getInstance().loadCachedDraft()!!.screens["home"]?.components?.containsKey("native_tvStreakSummary") ?: false)
                views(activity.findViewById(android.R.id.content)).filterIsInstance<Button>().first{it.text=="Redo"}.performClick()
            }
        }
        ActivityScenario.launch(UiStudioActivity::class.java).use { scenario ->
            scenario.onActivity { assertEquals(30,UiStudioRepository.getInstance().loadCachedDraft()!!.screens.getValue("home").components.getValue("native_tvStreakSummary").typography.textSize) }
        }
    }
    @Test fun nestedPointerSelectionUsesTransformedBoundsAndIgnoresDrag() {
        ActivityScenario.launch(UiStudioActivity::class.java).use { scenario ->
            lateinit var preview: StudioPreview
            lateinit var card: FrameLayout
            lateinit var label: TextView
            var selected=""
            scenario.onActivity { activity ->
                preview=StudioPreview(activity)
                val root=FrameLayout(activity)
                card=FrameLayout(activity).apply{id=com.eve.app.R.id.cardLogin;setBackgroundColor(Color.LTGRAY)}
                label=TextView(activity).apply{id=com.eve.app.R.id.tvLogo;text="Nested label";textSize=20f}
                card.addView(label,FrameLayout.LayoutParams(200,60).apply{leftMargin=10;topMargin=10})
                root.addView(card,FrameLayout.LayoutParams(260,200).apply{leftMargin=30;topMargin=40})
                root.pivotX=0f;root.pivotY=0f;root.scaleX=.75f;root.scaleY=.75f
                preview.addView(root,FrameLayout.LayoutParams(340,500))
                preview.candidates={StudioRenderer.elements(root)}
                preview.onSelect={view -> selected=StudioRenderer.elements(root).firstOrNull{it.view===view}?.id.orEmpty();preview.selected=view}
                activity.setContentView(preview)
            }
            fun tapLabel() {
                val rect=android.graphics.Rect()
                scenario.onActivity { assertTrue(label.getGlobalVisibleRect(rect)) }
                val now=android.os.SystemClock.uptimeMillis()
                instrumentation.sendPointerSync(android.view.MotionEvent.obtain(now,now,android.view.MotionEvent.ACTION_DOWN,rect.exactCenterX(),rect.exactCenterY(),0))
                instrumentation.sendPointerSync(android.view.MotionEvent.obtain(now,now+50,android.view.MotionEvent.ACTION_UP,rect.exactCenterX(),rect.exactCenterY(),0))
                instrumentation.waitForIdleSync()
                assertEquals("native_tvLogo",selected)
            }
            instrumentation.waitForIdleSync();tapLabel()
            scenario.onActivity { (card.parent as View).apply{scaleX=1.2f;scaleY=1.2f};selected="" }
            instrumentation.waitForIdleSync();tapLabel()
            scenario.onActivity { assertNull(card.foreground);assertNull(label.foreground);selected="" }
            val now=android.os.SystemClock.uptimeMillis()
            instrumentation.sendPointerSync(android.view.MotionEvent.obtain(now,now,android.view.MotionEvent.ACTION_DOWN,50f,70f,0))
            instrumentation.sendPointerSync(android.view.MotionEvent.obtain(now,now+50,android.view.MotionEvent.ACTION_MOVE,50f,150f,0))
            instrumentation.sendPointerSync(android.view.MotionEvent.obtain(now,now+100,android.view.MotionEvent.ACTION_UP,50f,150f,0))
            instrumentation.waitForIdleSync();assertEquals("",selected)
            scenario.onActivity {
                val root=card.parent as View
                val unsafe=UiStudioConfig(screens=mapOf("login" to ScreenConfig(components=mapOf("native_tvLogo" to ComponentConfig(id="native_tvLogo",appearance=AppearanceProperties(opacity=0f))))))
                assertFalse(UiStudioRepository.getInstance().validateConfig(unsafe).first)
                StudioRenderer.apply(root,"login",unsafe)
                assertEquals(.3f,label.alpha,.001f)
                StudioRenderer.apply(root,"login",UiStudioConfig())
                assertEquals(1f,label.alpha,.001f)
                val button=com.google.android.material.button.MaterialButton(root.context).apply { id=android.R.id.button1;text="Protected action" }
                (root as ViewGroup).addView(button,FrameLayout.LayoutParams(220,64).apply{topMargin=300})
                val baselineTint=button.backgroundTintList
                val rounded=ComponentConfig(id="native_button1",type="button",appearance=AppearanceProperties(cornerRadius=20))
                StudioRenderer.apply(root,"login",UiStudioConfig(screens=mapOf("login" to ScreenConfig(components=mapOf(rounded.id to rounded)))))
                assertEquals(baselineTint!!.defaultColor,(button.background.current as android.graphics.drawable.GradientDrawable).color!!.defaultColor)
                val style=ComponentConfig(id="native_button1",type="button",appearance=AppearanceProperties(backgroundColor="#FF0000"),states=StateProperties(pressedBackgroundColor="#00FF00"))
                StudioRenderer.apply(root,"login",UiStudioConfig(screens=mapOf("login" to ScreenConfig(components=mapOf(style.id to style)))))
                assertNull("Native theme tint must not mask edited fill",button.backgroundTintList)
                assertEquals(Color.RED,(button.background.current as android.graphics.drawable.GradientDrawable).color!!.defaultColor)
                button.isPressed=true
                assertEquals(Color.GREEN,(button.background.current as android.graphics.drawable.GradientDrawable).color!!.defaultColor)
                button.isPressed=false
                StudioRenderer.apply(root,"login",UiStudioConfig())
                assertEquals(baselineTint,button.backgroundTintList)
            }
            screenshot("08-zoom-selection-overlay")
        }
    }
    @Test fun everyProductionLayoutInflatesInSandbox() {
        ActivityScenario.launch(UiStudioActivity::class.java).use { scenario ->
            StudioScreens.all.forEach { screen ->
                scenario.onActivity { activity ->
                    val parent=FrameLayout(activity)
                    val root=activity.layoutInflater.inflate(screen.layout,parent,false)
                    parent.addView(root)
                    StudioFixtures.bind(root,screen.key,{UiStudioConfig()},{})
                    StudioRenderer.apply(root,screen.key,UiStudioConfig())
                    assertNotNull(screen.key,root)
                }
            }
        }
    }
}
