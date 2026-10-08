package com.eve.app.util

import com.eve.app.data.model.uistudio.*
import com.eve.app.data.remote.ApiResponse
import com.eve.app.data.remote.EveApiService
import com.eve.app.data.repository.UiStudioRepository
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException
import java.lang.reflect.Proxy
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Repository ordering contracts with an explicitly local API, not deployed publication. */
class UiStudioPublicationTest {
    private class LocalApi {
        var published: UiStudioConfig?=null
        var draft: UiStudioConfig?=null
        var offline=false
        var readback: ((UiStudioConfig?)->UiStudioPublishedResponse)?=null
        var revision=0
        val api=Proxy.newProxyInstance(EveApiService::class.java.classLoader,arrayOf(EveApiService::class.java)) { _,method,args ->
            if(offline)throw IOException("Local offline fixture")
            when(method.name) {
                "getPublishedUiStudioConfig" -> ApiResponse(true,readback?.invoke(published) ?: UiStudioPublishedResponse(version=published?.version ?: 0,config=published))
                "saveAdminUiStudioDraft" -> {
                    draft=(args!![0] as SaveDraftRequest).config.copy(revision="saved-${++revision}")
                    ApiResponse(true,UiStudioDraftResponse(config=draft,revision=draft!!.revision))
                }
                "restoreUiStudioVersion" -> {
                    val request=args!![1] as RestoreStudioRequest
                    if(request.target=="publish") {
                        published=draft!!.copy(version=(published?.version ?: 0)+1,status="published")
                        ApiResponse(true,mapOf("version" to published!!.version))
                    } else ApiResponse(true,emptyMap<String,Any>())
                }
                "getAdminUiStudioDraft" -> ApiResponse(true,UiStudioDraftResponse(config=draft))
                "publishUiStudioConfig" -> {
                    published=(args!![0] as PublishStudioRequest).config!!.copy(version=(published?.version ?: 0)+1,status="published")
                    ApiResponse(true,mapOf("version" to published!!.version,"publishedAt" to 1L))
                }
                else -> error("Unexpected ${method.name}")
            }
        } as EveApiService
    }
    private fun config(version: Int,color: String)=UiStudioConfig(version=version,screens=mapOf("home" to ScreenConfig(components=mapOf("native_panelHomeBanner" to ComponentConfig(id="native_panelHomeBanner",appearance=AppearanceProperties(backgroundColor=color,opacity=.35f),material=MaterialProperties(22,.6f,"#FFFFFF",.2f))))))
    @Test fun olderMalformedAndFailingRefreshRetainLastKnownGood()=runBlocking {
        val local=LocalApi();val repo=UiStudioRepository(local.api,context=null)
        val newer=config(2,"#FF0000");local.published=newer
        assertEquals(newer,repo.fetchPublishedConfig(true))
        local.published=config(1,"#0000FF")
        assertEquals(newer,repo.fetchPublishedConfig(true))
        local.readback={ UiStudioPublishedResponse(version=3,config=null) }
        assertEquals(newer,repo.fetchPublishedConfig(true))
        local.readback={ UiStudioPublishedResponse(version=3,config=config(4,"#00FF00")) }
        assertEquals(newer,repo.fetchPublishedConfig(true))
        local.offline=true
        assertEquals(newer,repo.fetchPublishedConfig(true))
        local.offline=false;local.readback={UiStudioPublishedResponse(version=0,config=null)}
        assertEquals(UiStudioConfig(),repo.fetchPublishedConfig(true))
    }
    @Test fun verificationChecksFieldsConfigVersionAndRetainsSavedRevisionForRetry()=runBlocking {
        val local=LocalApi();val repo=UiStudioRepository(local.api,context=null)
        val source=config(0,"#FF0000")
        local.readback={p->UiStudioPublishedResponse(version=p!!.version,config=p.copy(configVersion=p.configVersion+1))}
        assertTrue(repo.publishVerified("local mismatch",source) is PublishResult.VerificationFailed)
        assertEquals(UiStudioConfig(),repo.currentConfig)
        assertEquals("saved-1",repo.lastServerSavedDraft!!.revision)
        local.readback=null
        val verified=repo.publishVerified("local retry",repo.lastServerSavedDraft) as PublishResult.VerifiedSuccess
        assertEquals(source.screens,verified.config.screens)
        assertEquals(verified.config,repo.currentConfig)
    }
    @Test fun inFlightOldReadCannotFinishAfterVerifiedPublication()=runBlocking {
        val local=LocalApi();local.published=config(1,"#0000FF")
        val repo=UiStudioRepository(local.api,context=null)
        val started=CountDownLatch(1);val release=CountDownLatch(1)
        var first=true
        local.readback={ p ->
            if(first) { first=false;started.countDown();check(release.await(10,TimeUnit.SECONDS)) }
            UiStudioPublishedResponse(version=p!!.version,config=p)
        }
        val oldRead=async(Dispatchers.Default) { repo.fetchPublishedConfig(true) }
        assertTrue(started.await(10,TimeUnit.SECONDS))
        val publication=async(Dispatchers.Default) { repo.publishVerified("local ordered publication",config(0,"#FF0000")) }
        release.countDown()
        oldRead.await()
        val verified=publication.await() as PublishResult.VerifiedSuccess
        assertEquals(2,verified.version)
        assertEquals(verified.config,repo.currentConfig)
    }
    @Test fun unsupportedBlurCannotBeSavedOrPublished()=runBlocking {
        val local=LocalApi();val repo=UiStudioRepository(local.api,context=null)
        val source=config(0,"#FF0000")
        val component=source.screens.getValue("home").components.getValue("native_panelHomeBanner")
        val invalid=source.copy(screens=mapOf("home" to ScreenConfig(components=mapOf(component.id to component.copy(material=component.material.copy(blurRadius=50))))))
        assertTrue(repo.saveDraftDetailed(invalid) is SaveDraftResult.Failure)
        assertTrue(repo.publishVerified("unsupported",invalid) is PublishResult.NetworkFailure)
        assertNull(local.draft);assertNull(local.published)
        assertTrue(repo.validateConfig(invalid,allowLegacyBlur=true).first)
    }
    @Test fun publishedRestoreCannotClaimSuccessFromOfflineFallback()=runBlocking {
        val local=LocalApi();val repo=UiStudioRepository(local.api,context=null)
        val before=config(1,"#FF0000");local.published=before
        repo.fetchPublishedConfig(true)
        local.draft=config(0,"#0000FF")
        local.readback={ throw IOException("Restore readback unavailable") }
        assertTrue(repo.restoreVersion(1,"publish").isFailure)
        assertEquals(before,repo.currentConfig)
        local.readback=null
        assertTrue(repo.restoreVersion(1,"publish").isSuccess)
        assertEquals(local.draft!!.screens,repo.currentConfig.screens)
    }

}
