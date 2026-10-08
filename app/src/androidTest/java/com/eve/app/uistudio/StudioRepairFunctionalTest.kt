package com.eve.app.uistudio

import android.graphics.Bitmap
import android.graphics.Color
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.eve.app.R
import com.eve.app.data.model.uistudio.*
import com.eve.app.ui.admin.uistudio.UiStudioActivity
import com.eve.app.ui.home.ExamAdapter
import com.eve.app.ui.home.HomeListItem
import com.eve.app.data.model.Exam
import com.eve.app.util.UiStudioEngine
import com.google.android.material.card.MaterialCardView
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Production layouts/adapters and visible pixels; this is not authenticated remote publication. */
@RunWith(AndroidJUnit4::class)
class StudioRepairFunctionalTest {
    private val instrumentation get()=InstrumentationRegistry.getInstrumentation()
    private fun shot(name: String): Bitmap {
        Thread.sleep(1500)
        instrumentation.waitForIdleSync()
        val bitmap=requireNotNull(instrumentation.uiAutomation.takeScreenshot())
        val dir=File(instrumentation.targetContext.getExternalFilesDir(null),"studio-repair-evidence").apply { mkdirs() }
        File(dir,"$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) }
        return bitmap
    }
    @Test fun homeAdapterRebindAndBannerFillReachPixelsAndReset() {
        ActivityScenario.launch(UiStudioActivity::class.java).use { scenario ->
            lateinit var root: View
            lateinit var card: MaterialCardView
            lateinit var config: UiStudioConfig
            lateinit var title: TextView
            var nativeColor=0
            var clicks=0
            scenario.onActivity { activity ->
                root=activity.layoutInflater.inflate(R.layout.activity_main,null)
                activity.setContentView(root)
                StudioFixtures.bind(root,"home",{UiStudioConfig()},{})
            }
            shot("01-home-baseline")
            scenario.onActivity {
                val elements=StudioRenderer.elements(root)
                val element=elements.first { it.repeated && it.view is MaterialCardView && it.view.id==View.NO_ID }
                assertEquals("native_path_rvExams_MaterialCardView_0",element.id)
                card=element.view as MaterialCardView
                nativeColor=card.cardBackgroundColor.defaultColor
                title=card.findViewById(R.id.tvExamName)
                card.setOnClickListener { clicks++ }
                val style=ComponentConfig(id=element.id,appearance=AppearanceProperties(backgroundColor="#FF0000",opacity=.35f,cornerRadius=16,strokeWidth=2,strokeColor="#00FF00"))
                val text=ComponentConfig(id="native_tvExamName",typography=TypographyProperties(textColor="#0000FF"))
                val banner=ComponentConfig(id="native_panelHomeBanner",appearance=AppearanceProperties(backgroundColor="#00FF00"))
                config=UiStudioConfig(screens=mapOf("home" to ScreenConfig(components=mapOf(style.id to style,text.id to text,banner.id to banner))))
                StudioRenderer.apply(root,"home",config)
                assertTrue(StudioRenderer.unsupported(root,"home",config).isEmpty())
                assertEquals(Color.RED,card.cardBackgroundColor.defaultColor)
                assertEquals(.35f,card.alpha,.001f)
                assertEquals(Color.BLUE,title.currentTextColor)
                card.performClick();assertEquals(1,clicks)
            }
            val styled=shot("02-home-published-fields-rendered")
            scenario.onActivity {
                val matte=root.findViewById<View>(R.id.homeBannerMatte)
                val point=IntArray(2);matte.getLocationOnScreen(point)
                assertEquals("Visible banner fill must cover the pager's native matte",Color.GREEN,styled.getPixel(point[0]+matte.width/2,point[1]+matte.height/2))
                // Production bind resets card fill, stroke and alpha. The next reconcile must repair all.
                val rv=root.findViewById<RecyclerView>(R.id.rvExams)
                val adapter=rv.adapter as ExamAdapter
                val holder=rv.getChildViewHolder(card)
                adapter.onBindViewHolder(holder,1)
                assertEquals(1f,card.alpha,.001f)
                StudioRenderer.apply(root,"home",config)
                val fingerprint=card.getTag(R.id.studio_render_fingerprint)
                StudioRenderer.apply(root,"home",config)
                assertSame("Unchanged pixels must not trigger another baseline/render pass",fingerprint,card.getTag(R.id.studio_render_fingerprint))
                assertEquals(Color.RED,card.cardBackgroundColor.defaultColor)
                assertEquals(.35f,card.alpha,.001f)
                assertEquals(Color.GREEN,card.strokeColor)
                assertEquals(Color.BLUE,title.currentTextColor)
            }
            shot("03-home-after-real-adapter-bind")
            var cardPoint=IntArray(2)
            scenario.onActivity {
                card.getLocationOnScreen(cardPoint)
                val components=config.screens.getValue("home").components
                val key=components.keys.first { it.startsWith("native_path_rvExams_MaterialCardView") }
                val opaque=components.getValue(key).let { it.copy(appearance=it.appearance.copy(opacity=1f)) }
                StudioRenderer.apply(root,"home",config.copy(screens=mapOf("home" to ScreenConfig(components=components+(key to opaque)))))
            }
            val opaquePixels=shot("09-home-opacity-reference")
            val x=cardPoint[0]+card.width-20;val y=cardPoint[1]+card.height/2
            val fadedPixel=styled.getPixel(x,y);val opaquePixel=opaquePixels.getPixel(x,y)
            assertTrue("Whole-element opacity must change actual screen pixels",kotlin.math.abs(Color.red(fadedPixel)-Color.red(opaquePixel))+kotlin.math.abs(Color.green(fadedPixel)-Color.green(opaquePixel))+kotlin.math.abs(Color.blue(fadedPixel)-Color.blue(opaquePixel))>40)
            opaquePixels.recycle()
            scenario.onActivity {
                StudioRenderer.apply(root,"home",UiStudioConfig())
                assertEquals(nativeColor,card.cardBackgroundColor.defaultColor)
                assertEquals(1f,card.alpha,.001f)
                // A color-only override must not reset newer native autofit size or native spacing.
                val text=ComponentConfig(id="native_tvExamName",typography=TypographyProperties(textColor="#FF0000"))
                UiStudioEngine.applyToView(title,text)
                title.textSize=29f;title.setPadding(7,9,11,13)
                UiStudioEngine.applyToView(title,text.copy(typography=TypographyProperties(textColor="#00FF00")))
                assertEquals(29*title.resources.displayMetrics.scaledDensity,title.textSize,.01f)
                assertEquals(9,title.paddingTop)
                UiStudioEngine.applyToView(title,ComponentConfig(id=text.id))
                assertEquals(29*title.resources.displayMetrics.scaledDensity,title.textSize,.01f)
            }
            shot("04-home-reset")
            styled.recycle()
        }
    }
    @Test fun productionNotificationsReconcileWithoutLayoutAndSurviveRecreation() {
        val target=instrumentation.targetContext
        val originalRepo=com.eve.app.data.repository.UiStudioRepository.getInstance()
        val text=ComponentConfig(id="native_tvTitle",typography=TypographyProperties(textColor="#FF0000",textSize=22),appearance=AppearanceProperties(opacity=.4f))
        val config=UiStudioConfig(version=1,screens=mapOf("notifications" to ScreenConfig(components=mapOf(text.id to text))))
        var published=config
        val api=java.lang.reflect.Proxy.newProxyInstance(com.eve.app.data.remote.EveApiService::class.java.classLoader,arrayOf(com.eve.app.data.remote.EveApiService::class.java)) { _,method,_ ->
            check(method.name=="getPublishedUiStudioConfig")
            com.eve.app.data.remote.ApiResponse(true,UiStudioPublishedResponse(version=published.version,config=published.takeIf { it.version>0 }))
        } as com.eve.app.data.remote.EveApiService
        val context=object: android.content.ContextWrapper(target) {
            override fun getSharedPreferences(name: String,mode: Int)=super.getSharedPreferences("runtime_repair_$name",mode)
        }
        context.getSharedPreferences("eve_ui_studio_prefs",android.content.Context.MODE_PRIVATE).edit().clear().commit()
        val repo=com.eve.app.data.repository.UiStudioRepository(api,context)
        kotlinx.coroutines.runBlocking { repo.fetchPublishedConfig(true) }
        val instanceField=repo.javaClass.getDeclaredField("INSTANCE").apply { isAccessible=true }
        instanceField.set(null,repo)
        com.eve.app.util.NotificationStore.add(target,"Repair title","Repair body")
        // Local API injection exercises the real production Activity observer, without remote mutation.
        try {
            ActivityScenario.launch(com.eve.app.ui.notifications.NotificationsActivity::class.java).use { scenario ->
                shot("05-notifications-production")
                scenario.onActivity { activity ->
                    val title=activity.findViewById<TextView>(R.id.tvTitle)
                    assertNotNull(title);assertEquals(Color.RED,title.currentTextColor)
                    assertEquals(.4f,title.alpha,.001f)
                    title.setTextColor(Color.BLUE);title.alpha=1f
                    // These setters invalidate pixels but do not require a global layout.
                }
                shot("06-notifications-without-layout-repaired")
                scenario.onActivity { activity ->
                    val title=activity.findViewById<TextView>(R.id.tvTitle)
                    assertEquals(Color.RED,title.currentTextColor);assertEquals(.4f,title.alpha,.001f)
                }
                scenario.recreate()
                shot("07-notifications-recreated")
                scenario.onActivity { activity ->
                    val title=activity.findViewById<TextView>(R.id.tvTitle)
                    assertEquals(Color.RED,title.currentTextColor);assertEquals(.4f,title.alpha,.001f)
                    published=UiStudioConfig()
                    kotlinx.coroutines.runBlocking { repo.fetchPublishedConfig(true) }
                }
                shot("08-notifications-reset")
                scenario.onActivity { activity -> assertEquals(1f,activity.findViewById<TextView>(R.id.tvTitle).alpha,.001f) }
            }
        } finally { instanceField.set(null,originalRepo) }
    }
    @Test fun timerPaintKeepsPublishedTrackAndIndependentTextThroughTicks() {
        ActivityScenario.launch(UiStudioActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val root=activity.layoutInflater.inflate(R.layout.activity_test,null)
                val timer=root.findViewById<com.eve.app.ui.common.CircularTimerView>(R.id.circularTimerView)
                val c=ComponentConfig(id="timer_pill",appearance=AppearanceProperties(strokeColor="#00FF00"),typography=TypographyProperties(textColor="#0000FF",textSize=20,fontFamily="serif"))
                val config=UiStudioConfig(screens=mapOf("test" to ScreenConfig(components=mapOf(c.id to c))))
                StudioRenderer.apply(root,"test",config)
                timer.layout(0,0,100,100);timer.setTime(300,600)
                val bitmap=Bitmap.createBitmap(100,100,Bitmap.Config.ARGB_8888)
                timer.draw(android.graphics.Canvas(bitmap))
                fun paint(name:String)=timer.javaClass.getDeclaredField(name).apply { isAccessible=true }.get(timer) as android.graphics.Paint
                assertEquals(Color.GREEN,paint("trackPaint").color)
                assertEquals(Color.BLUE,paint("textPaint").color)
                assertEquals(20*activity.resources.displayMetrics.scaledDensity,paint("textPaint").textSize,.01f)
                var bluePixels=0;var greenPixels=0
                for(y in 0 until 100)for(x in 0 until 100) {
                    val color=bitmap.getPixel(x,y)
                    if(Color.alpha(color)>100 && Color.blue(color)>200 && Color.red(color)<50)bluePixels++
                    if(Color.alpha(color)>100 && Color.green(color)>200 && Color.red(color)<50)greenPixels++
                }
                assertTrue("Configured timer text must draw blue pixels",bluePixels>10)
                assertTrue("Configured timer track must draw green pixels",greenPixels>10)
                timer.setTime(30,600);timer.draw(android.graphics.Canvas(bitmap))
                assertEquals(Color.BLUE,paint("textPaint").color)
                assertEquals(Color.GREEN,paint("trackPaint").color)
                StudioRenderer.apply(root,"test",UiStudioConfig())
                assertEquals(13*activity.resources.displayMetrics.scaledDensity,paint("textPaint").textSize,.01f)
                bitmap.recycle()
            }
        }
    }
    @Test fun resetUsesLatestNativeIconBindingInsteadOfFirstRecycledItem() {
        ActivityScenario.launch(UiStudioActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val root=android.widget.FrameLayout(activity)
                val icon=android.widget.ImageView(activity).apply {
                    id=R.id.ivExamImage;setImageResource(R.drawable.ic_star_filled);setColorFilter(Color.RED)
                }
                root.addView(icon)
                val c=ComponentConfig(id="native_ivExamImage",appearance=AppearanceProperties(iconTint="#0000FF"))
                val config=UiStudioConfig(screens=mapOf("home" to ScreenConfig(components=mapOf(c.id to c))))
                StudioRenderer.apply(root,"home",config)
                icon.setColorFilter(Color.GREEN) // Native category binding changes on recycle.
                val latestFilter=icon.colorFilter
                StudioRenderer.apply(root,"home",config)
                assertNull(icon.colorFilter);assertEquals(Color.BLUE,icon.imageTintList!!.defaultColor)
                StudioRenderer.apply(root,"home",UiStudioConfig())
                assertSame("Reset restores the current native category tint",latestFilter,icon.colorFilter)
                assertNull(icon.imageTintList)
            }
        }
    }
    @Test fun unsupportedImportedPropertiesCannotPassPublicationPreflight() {
        ActivityScenario.launch(UiStudioActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val root=activity.layoutInflater.inflate(R.layout.activity_notifications,null)
                val c=ComponentConfig(id="native_tvTitle",material=MaterialProperties(blurRadius=12))
                val config=UiStudioConfig(screens=mapOf("notifications" to ScreenConfig(components=mapOf(c.id to c))))
                StudioFixtures.bind(root,"notifications",{config},{})
                root.measure(View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(700,View.MeasureSpec.EXACTLY))
                root.layout(0,0,360,700)
                assertTrue(StudioRenderer.unsupported(root,"notifications",config).any { it.contains("backdrop blur") })
                val login=activity.layoutInflater.inflate(R.layout.activity_login,null)
                val label=ComponentConfig(id="native_tvLogo",typography=TypographyProperties(textColor="#00FF00"))
                val branded=UiStudioConfig(branding=BrandingConfig(enabled=true,brandColor="#FF0000"),screens=mapOf("login" to ScreenConfig(components=mapOf(label.id to label))))
                StudioRenderer.apply(login,"login",branded)
                assertEquals(Color.GREEN,login.findViewById<TextView>(R.id.tvLogo).currentTextColor)
                assertTrue(StudioRenderer.unsupported(root,"notifications",config.copy(screens=mapOf("notifications" to ScreenConfig(components=mapOf("native_missing" to c.copy(id="native_missing")))))).any { it.contains("no matching production view") })
            }
        }
    }
}
