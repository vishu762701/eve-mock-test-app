package com.eve.app.data.repository

import com.eve.app.data.model.Exam
import com.eve.app.data.model.Question
import com.eve.app.data.remote.ApiResponse
import com.eve.app.data.remote.EveApiService
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Proxy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class QuestionListResponseContractTest {
    private fun repository(response:Any):ExamRepository {
        val api=Proxy.newProxyInstance(EveApiService::class.java.classLoader,arrayOf(EveApiService::class.java)) { _,method,_ ->
            assertEquals("getQuestions",method.name)
            if(response is Throwable) throw response
            response
        } as EveApiService
        return ExamRepository(api)
    }
    @Test fun missingOrNullJsonCannotBecomeEmptySuccessThroughAnyQuestionCaller()=runBlocking {
        for(json in listOf("{\"success\":true}","{\"success\":true,\"data\":null}")) {
            val response=Gson().fromJson<ApiResponse<List<Question>>>(json,object:TypeToken<ApiResponse<List<Question>>>(){}.type)
            val repo=repository(response)
            val calls:List<suspend ()->Any> = listOf(
                {repo.getQuestions("exam")},{repo.getMockQuestions("exam")},
                {repo.getQuestionsForTopic("exam","History")},
                {repo.getPyqQuestions("exam",2024)},{repo.getPyqSets(Exam(id="exam"))})
            for(call in calls) {
                val error=runCatching { call() }.exceptionOrNull()
                assertNotNull("Malformed response cannot be a legitimate empty category/test",error)
                assertEquals("Failed to fetch questions",error!!.message)
            }
        }
    }
    @Test fun nullQuestionRowsAreRejectedBeforeRendering()=runBlocking {
        val response=Gson().fromJson<ApiResponse<List<Question>>>(
            "{\"success\":true,\"data\":[null]}",object:TypeToken<ApiResponse<List<Question>>>(){}.type)
        val error=runCatching { repository(response).getQuestions("exam") }.exceptionOrNull()
        assertNotNull("Gson can violate Kotlin nonnull element types; never publish null rows",error)
        assertEquals("Failed to fetch questions",error!!.message)
    }
    @Test fun validEmptyAndOrderedRowsPreserveMockTopicAndPyqContracts()=runBlocking {
        assertEquals(emptyList<Question>(),repository(ApiResponse(true,emptyList<Question>())).getQuestions("exam"))
        val math=Question(id="math",examId="exam",topic="Math")
        val history=Question(id="history",examId="exam",topic=" History ")
        val pyq=Question(id="pyq",examId="exam",isPyq=true,pyqYear=2024,pyqPaper="Paper I")
        val rows=listOf(math,history,pyq)
        val repo=repository(ApiResponse(true,rows))
        assertEquals(rows,repo.getQuestions("exam"))
        assertEquals(listOf(math,history),repo.getMockQuestions("exam"))
        assertEquals(listOf(history),repo.getQuestionsForTopic("exam","history"))
        assertEquals(listOf(pyq),repo.getPyqQuestions("exam",2024,"paper i"))
    }
    @Test fun deniedOrCancelledResponsesCannotBecomeStaleSuccess()=runBlocking {
        val denied=runCatching { repository(ApiResponse(false,listOf(Question(id="stale")),error="Denied")).getQuestions("exam") }.exceptionOrNull()
        assertEquals("Denied",denied?.message)
        val cancel=CancellationException("Closed")
        val actual=runCatching { repository(cancel).getMockQuestions("exam") }.exceptionOrNull()
        assertSame(cancel,actual)
    }
}
