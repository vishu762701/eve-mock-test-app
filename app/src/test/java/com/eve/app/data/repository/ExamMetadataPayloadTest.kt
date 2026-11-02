package com.eve.app.data.repository

import com.eve.app.data.model.Exam
import com.eve.app.data.remote.ApiResponse
import com.eve.app.data.remote.EveApiService
import java.lang.reflect.Proxy
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody

/** Real repository payload boundary: an ordinary metadata Save must not alter hidden settings. */
class ExamMetadataPayloadTest {
    @Test fun metadataRequestOmitsUneditedServerSettings()=runBlocking {
        var sent:Map<*,*>?=null
        val api=Proxy.newProxyInstance(EveApiService::class.java.classLoader,arrayOf(EveApiService::class.java)) { _,method,args ->
            when (method.name) {
                "updateExam" -> { sent=args!![1] as Map<*,*>; ApiResponse<Unit>(true) }
                "getExam" -> ApiResponse(true,Exam(id="exam",examName="Renamed",questionCount=100,negativeMarkingText="1/3",negativeMarkingValue=1.0/3,parentExamId="parent",publishMode="live"))
                else -> error("Unexpected request ${method.name}")
            }
        } as EveApiService
        ExamRepository(api).updateExam(Exam(id="exam",examName="Renamed",questionCount=100,negativeMarkingText="1/3",negativeMarkingValue=1.0/3,parentExamId="parent",publishMode="live"))
        val body=requireNotNull(sent)
        for(key in listOf("questionCount","negativeMarkingText","negativeMarkingValue","parentExamId","publishMode")) {
            assertFalse("Unshown setting $key must be left to the authoritative server",body.containsKey(key))
        }
        assertEquals("Renamed",body["examName"])
        assertTrue(body.containsKey("timeLimitMinutes")); assertTrue(body.containsKey("category")); assertTrue(body.containsKey("cutoffs"))
    }
    private fun httpApi(getJson:String):EveApiService {
        val client=okhttp3.OkHttpClient.Builder().addInterceptor { chain ->
            val request=chain.request()
            val json=if(request.method=="PUT") "{\"success\":true}" else getJson
            okhttp3.Response.Builder().request(request).protocol(okhttp3.Protocol.HTTP_1_1).code(200).message("OK")
                .body(json.toResponseBody("application/json".toMediaType())).build()
        }.build()
        return retrofit2.Retrofit.Builder().baseUrl("https://fixture.invalid/").client(client)
            .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create()).build().create(EveApiService::class.java)
    }
    @Test fun oldBackendIgnoringCutoffsCannotReportFullyConfirmedSuccess()=runBlocking {
        val desired=Exam(id="exam",examName="Name",category="Other",cutoffs=mapOf("General" to 70.0))
        val api=httpApi("""{"success":true,"data":{"id":"exam","examName":"Name","timeLimitMinutes":30,"category":"Other"}}""")
        assertTrue(runCatching { ExamRepository(api).updateExam(desired) }.exceptionOrNull() is ExamUpdateNotVerifiedException)
    }
    @Test fun exactHttpReadbackConfirmsConfiguredCutoffs()=runBlocking {
        val desired=Exam(id="exam",examName="Name",category="Other",cutoffs=mapOf("General" to 70.0))
        val api=httpApi("""{"success":true,"data":{"id":"exam","examName":"Name","timeLimitMinutes":30,"category":"Other","cutoffs":{"General":70}}}""")
        ExamRepository(api).updateExam(desired)
    }
    @Test fun failedMissingOrForeignReadbackCannotConfirmAnUpdate()=runBlocking {
        val desired=Exam(id="exam",examName="Name")
        for(reply in listOf(ApiResponse<Exam>(false,error="Denied"),ApiResponse<Exam>(true,null),ApiResponse(true,desired.copy(id="foreign")),ApiResponse(true,desired.copy(examName="Old")))) {
            val api=Proxy.newProxyInstance(EveApiService::class.java.classLoader,arrayOf(EveApiService::class.java)) { _,method,_ ->
                if(method.name=="updateExam") ApiResponse<Unit>(true) else reply
            } as EveApiService
            assertTrue(runCatching { ExamRepository(api).updateExam(desired) }.exceptionOrNull() is ExamUpdateNotVerifiedException)
        }
    }
    @Test fun failedPutIsNotFollowedByReadback()=runBlocking {
        var reads=0
        val api=Proxy.newProxyInstance(EveApiService::class.java.classLoader,arrayOf(EveApiService::class.java)) { _,method,_ ->
            if(method.name=="getExam") reads++
            ApiResponse<Unit>(false,error="Denied")
        } as EveApiService
        assertEquals("Denied",runCatching { ExamRepository(api).updateExam(Exam(id="exam")) }.exceptionOrNull()!!.message)
        assertEquals(0,reads)
    }

}
