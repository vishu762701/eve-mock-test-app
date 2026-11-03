package com.eve.app.data.repository

import com.eve.app.data.remote.ApiResponse
import com.eve.app.data.remote.EveApiService
import com.eve.app.data.remote.MonthlyApiUsageDto
import com.google.gson.Gson
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.lang.reflect.Proxy

class ApiUsageRepositoryTest {
    private fun repo(reply: (String) -> ApiResponse<MonthlyApiUsageDto>): ApiUsageRepository {
        val api=Proxy.newProxyInstance(EveApiService::class.java.classLoader,arrayOf(EveApiService::class.java)) {_,method,args->
            check(method.name=="getApiUsage")
            reply(args!![0] as String)
        } as EveApiService
        return ApiUsageRepository(api)
    }
    private fun valid(month:String)=MonthlyApiUsageDto(yearMonth=month,monthDisplayName="Confirmed month",
        geminiCalls=3,geminiSuccessfulCalls=2,geminiFailedCalls=1,documentsCreated=4,testSubmissions=5,lastUpdated=123)
    @Test fun confirmedCountersAndGenuineZeroRemainValid()=runBlocking {
        val result=repo {ApiResponse(true,valid(it))}.getUsageForCurrentMonth()
        assertEquals(3,result.geminiCalls);assertEquals(4,result.documentsCreatedProxy);assertEquals(5,result.testSubmissionsProxy)
        val zero=repo {ApiResponse(true,valid(it).copy(geminiCalls=0,geminiSuccessfulCalls=0,geminiFailedCalls=0,
            documentsCreated=0,testSubmissions=0))}.getUsageForCurrentMonth()
        assertEquals(0,zero.geminiCalls);assertEquals(123,zero.lastUpdated)
    }
    @Test fun denialMissingDataAndNetworkFailureCannotBecomeFreshZeros()=runBlocking {
        for(kind in listOf("denied","missing","network","contradictory")) {
            val repository=repo {month->when(kind){
                "denied"->ApiResponse(false,error="rights revoked")
                "missing"->ApiResponse(true)
                "contradictory"->ApiResponse(true,valid(month),error="rights revoked")
                else->throw java.io.IOException("offline")
            }}
            val error=runCatching {repository.getUsageForCurrentMonth()}.exceptionOrNull()
            assertNotNull("$kind must remain an error",error)
        }
    }
    @Test fun cancellationPropagatesAsTheOriginalCancellation()=runBlocking {
        val expected=CancellationException("screen stopped")
        val error=runCatching {repo {throw expected}.getUsageForCurrentMonth()}.exceptionOrNull()
        assertSame(expected,error)
    }
    @Test fun wrongMonthNegativeCountersAndContradictoryTotalsAreRejected()=runBlocking {
        for(kind in listOf("month","negative","totals","timestamp")) {
            val repository=repo {val data=valid(it);ApiResponse(true,when(kind){
                "month"->data.copy(yearMonth="1900-01")
                "negative"->data.copy(documentsCreated=-1)
                "totals"->data.copy(geminiCalls=4)
                else->data.copy(lastUpdated=0)
            })}
            assertNotNull(kind,runCatching {repository.getUsageForCurrentMonth()}.exceptionOrNull())
        }
    }
    @Test fun wirePayloadRequiresTypedPresentIntegralFieldsWithoutDuplicateKeys() {
        val gson=Gson()
        val base=gson.toJsonTree(valid("2026-11")).asJsonObject
        val critical=listOf("yearMonth","monthDisplayName","geminiCalls","geminiSuccessfulCalls",
            "geminiFailedCalls","documentsCreated","testSubmissions","lastUpdated")
        for(field in critical) {
            for(nullValue in listOf(false,true)) {
                val bad=base.deepCopy();if(nullValue)bad.add(field,com.google.gson.JsonNull.INSTANCE)else bad.remove(field)
                assertNotNull("$field presence",runCatching {gson.fromJson(bad,MonthlyApiUsageDto::class.java)}.exceptionOrNull())
            }
        }
        for(token in listOf("true","\"1\"","1.5","-1","9223372036854775808","{}","[]")) {
            val bad=base.deepCopy();bad.add("documentsCreated",com.google.gson.JsonParser.parseString(token))
            assertNotNull(token,runCatching {gson.fromJson(bad,MonthlyApiUsageDto::class.java)}.exceptionOrNull())
        }
        val duplicate=gson.toJson(base).dropLast(1)+",\"geminiCalls\":0}"
        assertNotNull(runCatching {gson.fromJson(duplicate,MonthlyApiUsageDto::class.java)}.exceptionOrNull())
    }
}
