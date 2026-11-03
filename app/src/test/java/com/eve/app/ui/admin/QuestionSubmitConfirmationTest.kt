package com.eve.app.ui.admin

import android.app.Application
import android.os.Looper
import androidx.lifecycle.ViewModelStore
import com.eve.app.data.model.Question
import com.eve.app.data.remote.ApiResponse
import com.eve.app.data.remote.EveApiService
import com.eve.app.data.repository.ExamRepository
import java.lang.reflect.Proxy
import kotlinx.coroutines.CancellationException
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35],application=Application::class)
class QuestionSubmitConfirmationTest {
    private fun exercise(response:Any,verify:(AdminViewModel,Int,String?)->Unit) {
        var calls=0
        val api=Proxy.newProxyInstance(EveApiService::class.java.classLoader,arrayOf(EveApiService::class.java)) { _,method,_ ->
            when(method.name) {
                "addQuestion" -> { calls++; if(response is Throwable) throw response; response }
                "getQuestions" -> ApiResponse(true,emptyList<Question>())
                else -> error("Unexpected request ${method.name}")
            }
        } as EveApiService
        val vm=AdminViewModel(ExamRepository(api))
        val store=ViewModelStore().apply { put("question",vm) }
        var completed=0
        var failure:String?=null
        try {
            assertEquals("No request before authorized caller submits",0,calls)
            vm.addQuestion(Question(examId="exam",questionText="Q",optionA="A",optionB="B",optionC="C",optionD="D",correctAnswer="A"),onFailure={failure=it}) { completed++ }
            shadowOf(Looper.getMainLooper()).idle()
            assertEquals(1,calls)
            assertFalse(vm.busy.value)
            verify(vm,completed,failure)
        } finally { store.clear() }
    }
    @Test fun deniedSubmissionCallsFailureWithoutSuccess()=exercise(ApiResponse<Any>(false,null,error="Permission denied")) { _,done,error ->
        assertEquals(0,done); assertEquals("Permission denied",error)
    }
    @Test fun serverConfirmedSubmissionCallsSuccessOnce()=exercise(ApiResponse<Any>(true)) { _,done,error ->
        assertEquals(1,done); assertNull(error)
    }
    @Test fun cancellationDoesNotBecomeAnUploadFailureOrSuccess()=exercise(CancellationException("Screen closed")) { vm,done,error ->
        assertEquals(0,done); assertNull(error); assertNull(vm.message.value)
    }
    private fun exerciseDeletion(response:Any,verify:(Int,String?,AdminViewModel)->Unit) {
        var calls=0
        val api=Proxy.newProxyInstance(EveApiService::class.java.classLoader,arrayOf(EveApiService::class.java)) { _,method,_ ->
            when(method.name) {
                "deleteQuestion" -> { calls++; if(response is Throwable) throw response; response }
                "getQuestions" -> ApiResponse(true,emptyList<Question>())
                else -> error("Unexpected request ${method.name}")
            }
        } as EveApiService
        val vm=AdminViewModel(ExamRepository(api)); val store=ViewModelStore().apply {put("delete",vm)}
        var done=0; var failure:String?=null
        try {
            vm.deleteQuestion(Question(id="question",examId="exam"),onFailure={failure=it}) {done++}
            shadowOf(Looper.getMainLooper()).idle()
            assertEquals(1,calls); assertFalse(vm.busy.value)
            verify(done,failure,vm)
        } finally {store.clear()}
    }
    @Test fun deniedDeletionIsNeverConfirmed()=exerciseDeletion(ApiResponse<Unit>(false,error="Permission denied")) {done,failure,_ ->
        assertEquals(0,done); assertEquals("Permission denied",failure)
    }
    @Test fun missingDeletionSuccessIsNeverConfirmed()=exerciseDeletion(com.google.gson.Gson().fromJson<ApiResponse<Unit>>("{}",
        object:com.google.gson.reflect.TypeToken<ApiResponse<Unit>>(){}.type)) {done,failure,_ ->
        assertEquals(0,done); assertNotNull(failure)
    }
    @Test fun confirmedDeletionNotifiesExactlyOnce()=exerciseDeletion(ApiResponse<Unit>(true)) {done,failure,_ ->
        assertEquals(1,done); assertNull(failure)
    }
    @Test fun cancelledDeletionReleasesBusyWithoutFalseResult()=exerciseDeletion(CancellationException("Stopped")) {done,failure,vm ->
        assertEquals(0,done); assertNull(failure); assertNull(vm.message.value)
    }

}
