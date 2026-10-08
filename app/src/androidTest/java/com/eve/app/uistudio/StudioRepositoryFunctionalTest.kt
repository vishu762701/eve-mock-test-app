package com.eve.app.uistudio

import android.content.Context
import android.content.ContextWrapper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.eve.app.data.model.uistudio.*
import com.eve.app.data.remote.ApiResponse
import com.eve.app.data.remote.EveApiService
import com.eve.app.data.repository.UiStudioRepository
import com.google.gson.Gson
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.lang.reflect.Proxy

/** Android disk caches + real repository flow against an explicitly local fake API. */
@RunWith(AndroidJUnit4::class)
class StudioRepositoryFunctionalTest {
    @Test fun offlineReconnectConflictPublicationReadbackAndStudentIsolation() = runBlocking {
        val target=InstrumentationRegistry.getInstrumentation().targetContext
        val context=object: ContextWrapper(target) {
            override fun getSharedPreferences(name: String,mode: Int)=super.getSharedPreferences("functional_$name",mode)
        }
        context.getSharedPreferences("eve_ui_studio_prefs",Context.MODE_PRIVATE).edit().clear().commit()
        var draft: UiStudioConfig?=null
        var published: UiStudioConfig?=null
        var offline=false
        var httpCode=0
        var corruptReadback=false
        var revision=0
        val api=Proxy.newProxyInstance(EveApiService::class.java.classLoader,arrayOf(EveApiService::class.java)) { _,method,args ->
            if(offline)throw IOException("Local test offline")
            if(httpCode!=0)throw retrofit2.HttpException(retrofit2.Response.error<Any>(httpCode,okhttp3.ResponseBody.create(null,"Local rejection")))
            when(method.name) {
                "saveAdminUiStudioDraft" -> {
                    val req=args!![0] as SaveDraftRequest
                    if(draft!=null && req.baseRevision!=draft!!.revision) ApiResponse(false,data=UiStudioDraftResponse(serverDraft=draft),error="NEWER_DRAFT_EXISTS")
                    else { draft=Gson().fromJson(Gson().toJson(req.config),UiStudioConfig::class.java).copy(revision="local-${++revision}");ApiResponse(true,UiStudioDraftResponse(config=draft,revision=draft!!.revision)) }
                }
                "publishUiStudioConfig" -> {
                    published=(args!![0] as PublishStudioRequest).config!!.copy(version=(published?.version ?: 0)+1,status="published")
                    ApiResponse(true,mapOf("version" to published!!.version,"publishedAt" to 1L))
                }
                "resetUiStudioConfig" -> {
                    val target=(args!![0] as ResetStudioRequest).target
                    if(target!="draft")published=null
                    if(target!="published")draft=null
                    ApiResponse<Any>(true)
                }
                "getPublishedUiStudioConfig" -> ApiResponse(true,UiStudioPublishedResponse(version=published?.version ?: 0,config=if(corruptReadback)published?.copy(screens=emptyMap())else published))
                else -> error("Unexpected local API method ${method.name}")
            }
        } as EveApiService
        val admin=UiStudioRepository(api,context)
        val element=ComponentConfig(id="custom_notice",type="button",content=ContentProperties(title="Read syllabus"),layout=LayoutProperties(paddingTop=12,height="64"),appearance=AppearanceProperties(backgroundColor="#007AFF",cornerRadius=16,strokeColor="#FFFFFF",strokeWidth=1,elevation=4,opacity=.9f,shape="capsule",iconTint="#FFFFFF",highlightColor="#FFFFFF",highlightOpacity=.4f),material=MaterialProperties(12,.8f,"#FFFFFF",.25f),typography=TypographyProperties(textColor="#000000",textSize=20,fontFamily="serif"),actions=ActionProperties("navigate","syllabus"),animation=AnimationProperties(enabled=true,type="fade",pressScale=.94f,springRelease=true,hapticFeedback=true),states=StateProperties(pressedBackgroundColor="#34C759",focusedBackgroundColor="#FF9500"))
        val config=UiStudioConfig(screens=mapOf("home" to ScreenConfig(id="home",components=mapOf(element.id to element))))
        assertTrue(admin.validateConfig(config).first)
        offline=true
        assertTrue(admin.saveDraftDetailed(config) is SaveDraftResult.LocalOfflineSuccess)
        assertEquals(config,admin.loadCachedDraft())
        assertTrue(admin.currentConfig.screens.isEmpty())
        offline=false
        val saved=admin.saveDraftDetailed(config) as SaveDraftResult.ServerSuccess
        assertTrue(admin.saveDraftDetailed(config) is SaveDraftResult.Conflict)
        for(code in listOf(401,403)) { httpCode=code;assertTrue(admin.saveDraftDetailed(saved.config) is SaveDraftResult.Failure) }
        httpCode=0
        val verified=admin.publishVerified("Local functional scenario",saved.config) as PublishResult.VerifiedSuccess
        assertEquals(element,verified.config.screens.getValue("home").components.getValue(element.id))
        val student=UiStudioRepository(api,context)
        assertEquals(verified.config,student.fetchPublishedConfig(true))
        // Fresh student readback renders through the same production path, not only DTO equality.
        androidx.test.core.app.ActivityScenario.launch(com.eve.app.ui.admin.uistudio.UiStudioActivity::class.java).use { scenario->
            scenario.onActivity { activity->
                val root=android.widget.LinearLayout(activity).apply{orientation=android.widget.LinearLayout.VERTICAL;addView(android.widget.TextView(activity).apply{text="Native business data"})}
                activity.setContentView(root)
                val item=StudioRenderer.apply(root,"home",student.currentConfig).first{it.id==element.id}.view as android.widget.Button
                assertEquals(element.content.title,item.text.toString());assertEquals(.9f,item.alpha,.001f)
                assertNotNull(item.backgroundTintList ?: item.background)
                assertEquals(.94f,student.currentConfig.screens.getValue("home").components.getValue(element.id).animation.pressScale!!,.001f)
            }
        }
        val edited=verified.config.copy(screens=mapOf("home" to verified.config.screens.getValue("home").copy(backgroundColor="#FFFFFF")))
        admin.saveDraftToLocalCache(edited)
        assertEquals(verified.config,student.currentConfig)
        offline=true
        val restarted=UiStudioRepository(api,context)
        assertEquals(verified.config,restarted.currentConfig)
        assertEquals(verified.config,restarted.fetchPublishedConfig(true))
        assertEquals(verified.config,student.fetchPublishedConfig(true))
        // Fresh student readback renders through the same production path, not only DTO equality.
        androidx.test.core.app.ActivityScenario.launch(com.eve.app.ui.admin.uistudio.UiStudioActivity::class.java).use { scenario->
            scenario.onActivity { activity->
                val root=android.widget.LinearLayout(activity).apply{orientation=android.widget.LinearLayout.VERTICAL;addView(android.widget.TextView(activity).apply{text="Native business data"})}
                activity.setContentView(root)
                val item=StudioRenderer.apply(root,"home",student.currentConfig).first{it.id==element.id}.view as android.widget.Button
                assertEquals(element.content.title,item.text.toString());assertEquals(.9f,item.alpha,.001f)
                assertNotNull(item.backgroundTintList ?: item.background)
                assertEquals(.94f,student.currentConfig.screens.getValue("home").components.getValue(element.id).animation.pressScale!!,.001f)
            }
        }
        offline=false;corruptReadback=true
        assertTrue(admin.publishVerified("Deliberately mismatched readback",edited) is PublishResult.VerificationFailed)
        assertEquals(verified.config,admin.currentConfig)
        corruptReadback=false
        val retainedDraft=admin.loadCachedDraft()
        assertTrue(admin.reset("published").isSuccess)
        assertEquals(UiStudioConfig(),admin.currentConfig)
        assertEquals(retainedDraft,admin.loadCachedDraft())
        assertTrue(admin.reset("draft").isSuccess)
        assertNull(admin.loadCachedDraft())
    }
}
