package com.eve.app.data.repository

import com.eve.app.data.model.Exam
import com.eve.app.data.remote.ApiResponse
import com.eve.app.data.remote.EveApiService
import java.lang.reflect.Proxy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class ExamListResponseContractTest {
    private fun repository(response: Any): ExamRepository {
        val api = Proxy.newProxyInstance(EveApiService::class.java.classLoader,
            arrayOf(EveApiService::class.java)) { _, method, _ ->
            assertEquals("getExams", method.name)
            if (response is Throwable) throw response
            response
        } as EveApiService
        return ExamRepository(api)
    }
    @Test fun missingDatasetCannotBePublishedAsAnEmptyExamList() = runBlocking {
        try {
            repository(ApiResponse<List<Exam>>(true,null)).getExams()
            fail("Malformed success must not erase the caller's confirmed dataset/cache")
        } catch (error: Exception) { assertFalse(error.message.isNullOrBlank()) }
    }
    @Test fun confirmedEmptyAndNonemptyDatasetsKeepExistingOrdering() = runBlocking {
        assertEquals(emptyList<Exam>(),repository(ApiResponse(true,emptyList<Exam>())).getExams())
        val b=Exam(id="b",examName="Beta")
        val a=Exam(id="a",examName="Alpha")
        assertEquals(listOf(a,b),repository(ApiResponse(true,listOf(b,a))).getExams())
    }
    @Test fun deniedAndCancelledRequestsRemainFailuresInsteadOfStaleSuccess() = runBlocking {
        try {
            repository(ApiResponse(false,listOf(Exam(id="stale")),error="Denied")).getExams()
            fail("Server failure became a stale successful dataset")
        } catch (error: Exception) { assertEquals("Denied",error.message) }
        val cancellation=CancellationException("Closed")
        try { repository(cancellation).getExams(); fail("Cancellation swallowed") }
        catch (error: CancellationException) { assertSame(cancellation,error) }
    }
}
