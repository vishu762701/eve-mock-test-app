package com.eve.app.ui.admin

import android.app.Application
import android.os.Looper
import androidx.lifecycle.ViewModelStore
import com.eve.app.data.model.ExamAnalytics
import com.eve.app.data.model.QuestionAnalytics
import com.eve.app.data.remote.ApiResponse
import com.eve.app.data.remote.EveApiService
import com.eve.app.data.repository.AdminAnalyticsRepository
import com.eve.app.util.UiState
import java.lang.reflect.Proxy
import kotlin.coroutines.Continuation
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.resume
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** Real ViewModel/repository with controlled transport completion ordering. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35],application=Application::class)
class AnalyticsReadOwnershipTest {
    private class Harness {
        data class Read(val method:String,val argument:Any?,val reply:Continuation<Any?>)
        val reads=mutableListOf<Read>()
        private val api=Proxy.newProxyInstance(EveApiService::class.java.classLoader,arrayOf(EveApiService::class.java)) { _,method,args ->
            @Suppress("UNCHECKED_CAST") val reply=args!!.last() as Continuation<Any?>
            reads.add(Read(method.name,args.first(),reply)); COROUTINE_SUSPENDED
        } as EveApiService
        val vm=AdminAnalyticsViewModel(AdminAnalyticsRepository(api))
        private val store=ViewModelStore().apply { put("analytics",vm) }
        fun idle()=shadowOf(Looper.getMainLooper()).idle()
        fun load(range:AnalyticsTimeRange=AnalyticsTimeRange.ALL_TIME,exam:String="") { vm.load(range,exam); idle() }
        fun exams(index:Int,name:String="Exam") { reads[index].reply.resume(ApiResponse(true,listOf(ExamAnalytics(examId=name,examName=name)))); idle() }
        fun questions(index:Int,name:String="Question") { reads[index].reply.resume(ApiResponse(true,listOf(QuestionAnalytics(id=name,examId=name,questionText=name)))); idle() }
        fun close() { store.clear(); idle() }
    }
    private fun exercise(block:Harness.()->Unit) {
        val h=Harness(); try { h.block() } finally { h.close() }
    }
    @Test fun olderRangeCannotOverwriteLatestSelection()=exercise {
        load(AnalyticsTimeRange.LAST_7_DAYS,"old"); exams(0,"Old")
        load(AnalyticsTimeRange.LAST_30_DAYS,"current"); exams(2,"Current"); questions(3,"Current")
        questions(1,"Old")
        val current=(vm.state.value as UiState.Success).data
        assertEquals(AnalyticsTimeRange.LAST_30_DAYS,current.timeRange)
        assertEquals("current",current.selectedExamId)
        assertEquals("Current",current.questions.single().questionText)
    }
    @Test fun obsoleteExamReadDoesNotStartQuestionRequest()=exercise {
        load(AnalyticsTimeRange.LAST_7_DAYS); load(AnalyticsTimeRange.LAST_30_DAYS)
        exams(0,"Old")
        assertEquals("Do not fetch an obsolete question batch",2,reads.size)
    }
    @Test fun obsoleteFailureCannotReplaceCurrentSuccessfulData()=exercise {
        load(AnalyticsTimeRange.LAST_7_DAYS)
        load(AnalyticsTimeRange.LAST_30_DAYS); exams(1,"Current"); questions(2,"Current")
        reads[0].reply.resume(ApiResponse<List<ExamAnalytics>>(false,error="Obsolete denied")); idle()
        assertTrue(vm.state.value is UiState.Success)
    }
    @Test fun sameRangeRefreshStillKeepsLatestRequest()=exercise {
        load(); exams(0,"Old")
        load(); exams(2,"Latest"); questions(3,"Latest"); questions(1,"Old")
        assertEquals("Latest",(vm.state.value as UiState.Success).data.questions.single().questionText)
    }
    @Test fun currentFailureIsReportedAndCancellationIsNotAUserError()=exercise {
        load()
        reads[0].reply.resume(ApiResponse<List<ExamAnalytics>>(false,error="Current denied")); idle()
        assertEquals("Current denied",(vm.state.value as UiState.Error).message)
        load()
        reads[1].reply.resumeWith(Result.failure(kotlinx.coroutines.CancellationException("Stopped"))); idle()
        assertTrue(vm.state.value is UiState.Loading)
    }
    @Test fun clearedOwnerCannotPublishOrStartAdditionalReads()=exercise {
        load(); close(); exams(0,"Late")
        assertEquals(1,reads.size)
        assertTrue(vm.state.value is UiState.Loading)
    }

}
