package com.eve.app.data.repository

import com.eve.app.data.model.AppContent
import com.eve.app.data.remote.ApiResponse
import com.eve.app.data.remote.EveApiService
import com.google.gson.Gson
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.lang.reflect.Proxy

class AppContentRepositoryTest {
    private val document=AppContent(title="Confirmed title",body="Confirmed body")
    private fun repo(reply:(String,Array<out Any?>)->Any?):AppContentRepository {
        val api=Proxy.newProxyInstance(EveApiService::class.java.classLoader,arrayOf(EveApiService::class.java)){_,method,args->
            reply(method.name,args?:emptyArray())
        } as EveApiService
        return AppContentRepository(api)
    }
    @Test fun publicOfflineFallbackIsPreservedForEveryDocument()=runBlocking {
        for(type in listOf("privacy_policy","terms_of_service","contact_us")) {
            for(kind in listOf("denied","missing","network")) {
                val repository=repo {_,_->when(kind){
                    "denied"->ApiResponse<AppContent>(false,error="denied")
                    "missing"->ApiResponse<AppContent>(true)
                    else->throw java.io.IOException("offline")
                }}
                assertEquals(repository.getDefaultContent(type),repository.getContent(type))
            }
        }
    }
    @Test fun adminReadAcceptsConfirmedDefaultsButRejectsUnconfirmedOrMalformedContent()=runBlocking {
        assertEquals(document,repo {_,_->ApiResponse(true,document)}.getConfirmedContent("privacy_policy"))
        for(kind in listOf("denied","missing","network","contradictory","title","body")) {
            val repository=repo {_,_->when(kind){
                "denied"->ApiResponse<AppContent>(false,error="denied")
                "missing"->ApiResponse<AppContent>(true)
                "network"->throw java.io.IOException("offline")
                "contradictory"->ApiResponse(true,document,error="denied")
                "title"->ApiResponse(true,document.copy(title=""))
                else->ApiResponse(true,document.copy(body=""))
            }}
            assertNotNull(kind,runCatching {repository.getConfirmedContent("privacy")}.exceptionOrNull())
        }
    }
    @Test fun cancellationIsNeverConvertedToAConfirmedDocumentOrPublicFallback()=runBlocking {
        val expected=CancellationException("owner stopped")
        val repository=repo {_,_->throw expected}
        assertSame(expected,runCatching {repository.getContent("privacy")}.exceptionOrNull())
        assertSame(expected,runCatching {repository.getConfirmedContent("privacy")}.exceptionOrNull())
    }
    @Test fun saveUsesExistingNormalizedContractAndLeavesReadbackToTheOwnerBoundActivity()=runBlocking {
        for((alias,canonical) in listOf("privacy_policy" to "privacy","terms_of_service" to "terms","contact_us" to "contact")) {
            var calls=0
            repo {method,args->
                calls++;assertEquals("updateAppContent",method);assertEquals(canonical,args[0])
                val fields=args[1] as Map<*,*>
                assertEquals(document.title,fields["title"]);assertEquals(document.body,fields["body"])
                assertEquals("",fields["supportEmail"]);ApiResponse<Unit>(true)
            }.saveContent(alias,document)
            assertEquals(1,calls)
        }
    }
    @Test fun deniedMissingAndContradictoryWriteAcknowledgmentsFailClosed()=runBlocking {
        for(kind in listOf("denied","missing","contradictory","network")) {
            val repository=repo {_,_->when(kind){
                "denied"->ApiResponse<Unit>(false,error="denied")
                "missing"->Gson().fromJson<ApiResponse<Unit>>("{}",object:com.google.gson.reflect.TypeToken<ApiResponse<Unit>>(){}.type)
                "contradictory"->ApiResponse<Unit>(true,error="denied")
                else->throw java.io.IOException("offline")
            }}
            assertNotNull(kind,runCatching {repository.saveContent("privacy",document)}.exceptionOrNull())
        }
    }
    @Test fun strictWireTypesPreserveLegacyOptionalDefaultsAndRejectMalformedDocuments() {
        val gson=Gson()
        val base=gson.toJsonTree(document).asJsonObject
        for(field in listOf("title","body","updatedBy","supportEmail","phone","website","address")) {
            for(token in listOf("42","true","[]","{}")) {
                val bad=base.deepCopy();bad.add(field,com.google.gson.JsonParser.parseString(token))
                assertNotNull(field,runCatching {gson.fromJson(bad,AppContent::class.java)}.exceptionOrNull())
            }
        }
        for(field in listOf("title","body"))for(token in listOf("null","\"\"")) {
            val bad=base.deepCopy();bad.add(field,com.google.gson.JsonParser.parseString(token))
            assertNotNull(field,runCatching {gson.fromJson(bad,AppContent::class.java)}.exceptionOrNull())
        }
        for(field in listOf("title","body")) {
            val missing=base.deepCopy();missing.remove(field)
            assertNotNull(field,runCatching {gson.fromJson(missing,AppContent::class.java)}.exceptionOrNull())
        }
        for(token in listOf("-1","1.5","\"1\"","null","{}","9223372036854775808")) {
            val bad=base.deepCopy();bad.add("updatedAt",com.google.gson.JsonParser.parseString(token))
            assertNotNull(token,runCatching {gson.fromJson(bad,AppContent::class.java)}.exceptionOrNull())
        }
        val legacy=gson.fromJson("""{"title":"Confirmed title","body":"Confirmed body","phone":null}""",AppContent::class.java)
        assertEquals(document,legacy)
        val duplicate=gson.toJson(base).dropLast(1)+",\"body\":\"other\"}"
        assertNotNull(runCatching {gson.fromJson(duplicate,AppContent::class.java)}.exceptionOrNull())
    }
}
