package com.eve.app.data.repository

import android.app.Application
import com.eve.app.data.model.ExamAnalytics
import com.eve.app.data.model.QuestionAnalytics
import com.eve.app.data.remote.*
import java.lang.reflect.Proxy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35],application=Application::class)
class ReviewResponseIntegrityTest {
    private fun api(response:Any):EveApiService=Proxy.newProxyInstance(EveApiService::class.java.classLoader,arrayOf(EveApiService::class.java)) { _,_,_ ->
        if(response is Throwable)throw response
        response
    } as EveApiService
    @Test fun reportFailureAndMissingDataCannotClaimAllResolved()=runBlocking {
        for(response in listOf(ApiResponse<List<QuestionReportDto>>(false,null,error="Denied"),ApiResponse<List<QuestionReportDto>>(true,null))) {
            val error=runCatching { FlaggedQuestionRepository(api(response)).getPendingFlaggedQuestions() }.exceptionOrNull()
            assertNotNull("Failed report fetch is not a verified empty report list",error)
        }
        assertTrue(FlaggedQuestionRepository(api(ApiResponse(true,emptyList<QuestionReportDto>()))).getPendingFlaggedQuestions().isEmpty())
    }
    @Test fun legacySameTextReportsFromDifferentExamsMustNotResolveTogether()=runBlocking {
        val first=QuestionReportDto(id="one",examId="first-exam",questionText="Same",comment="First")
        val second=first.copy(id="two",examId="second-exam",comment="Second")
        val groups=FlaggedQuestionRepository(api(ApiResponse(true,listOf(first,second)))).getPendingFlaggedQuestions()
        assertEquals(2,groups.size)
        assertEquals(setOf("first-exam","second-exam"),groups.map {it.examId}.toSet())
        assertTrue(groups.all {it.flagIds.size==1})
    }
    @Test fun reportCancellationPropagatesInsteadOfEmptySuccess()=runBlocking {
        val error=runCatching { FlaggedQuestionRepository(api(CancellationException("Closed"))).getPendingFlaggedQuestions() }.exceptionOrNull()
        assertTrue(error is CancellationException)
    }
    private fun analytics(response:Any)=AdminAnalyticsRepository(api(response))
    @Test fun analyticsFailureCannotBecomeZeroStatistics()=runBlocking {
        for(response in listOf(ApiResponse<List<ExamAnalytics>>(false,null,error="Denied"),ApiResponse<List<ExamAnalytics>>(true,null))) {
            assertNotNull(runCatching { analytics(response).getExamAnalytics() }.exceptionOrNull())
        }
        for(response in listOf(ApiResponse<List<QuestionAnalytics>>(false,null,error="Denied"),ApiResponse<List<QuestionAnalytics>>(true,null))) {
            assertNotNull(runCatching { analytics(response).getQuestionAnalytics() }.exceptionOrNull())
        }
    }
    @Test fun validAnalyticsOrderingAndIndependentUnknownReportsRemainIntact()=runBlocking {
        val small=ExamAnalytics(examId="small",examName="Small",attemptCount=1)
        val large=small.copy(examId="large",examName="Large",attemptCount=5)
        assertEquals(listOf(large,small),analytics(ApiResponse(true,listOf(small,large))).getExamAnalytics())
        assertTrue(analytics(ApiResponse(true,emptyList<ExamAnalytics>())).getExamAnalytics().isEmpty())
        val report=QuestionReportDto(id="one",examId="exam",comment="First issue")
        val groups=FlaggedQuestionRepository(api(ApiResponse(true,listOf(report,report.copy(id="two",comment="Another issue"))))).getPendingFlaggedQuestions()
        assertEquals(2,groups.size)
        assertTrue(groups.all {it.flagCount==1})
    }

}
