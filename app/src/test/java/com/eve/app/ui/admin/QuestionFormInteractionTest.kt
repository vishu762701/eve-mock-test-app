package com.eve.app.ui.admin

import android.app.Activity
import android.app.Application
import android.content.DialogInterface
import android.os.Looper
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.lifecycle.ViewModelStore
import com.eve.app.R
import com.eve.app.data.model.Exam
import com.eve.app.data.model.Question
import com.eve.app.data.remote.ApiResponse
import com.eve.app.data.remote.EveApiService
import com.eve.app.data.repository.ExamRepository
import java.lang.reflect.Proxy
import kotlin.coroutines.Continuation
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.resume
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** Actual production editor + ViewModel + repository; only the API transport is isolated. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35],application=Application::class)
class QuestionFormInteractionTest {
    @Test fun validationPendingDenialRetryAndSuccessPreserveTheActualDraft() {
        val controller=Robolectric.buildActivity(Activity::class.java)
        controller.get().setTheme(R.style.Theme_Eve)
        controller.setup().visible()
        var requests=0
        var pending:Continuation<Any?>?=null
        val api=Proxy.newProxyInstance(EveApiService::class.java.classLoader,arrayOf(EveApiService::class.java)) { _,method,args ->
            when(method.name) {
                "addQuestion" -> {
                    requests++
                    @Suppress("UNCHECKED_CAST")
                    val continuation=args!!.last() as Continuation<Any?>
                    pending=continuation
                    COROUTINE_SUSPENDED
                }
                "getQuestions" -> ApiResponse(true,emptyList<Question>())
                else -> error("Unexpected request ${method.name}")
            }
        } as EveApiService
        val vm=AdminViewModel(ExamRepository(api))
        val store=ViewModelStore().apply { put("editor",vm) }
        val dialog=createQuestionEditorDialog(controller.get(),Exam(id="exam",examName="Exam"),isBusy={vm.busy.value},submit=vm::addQuestion)
        try {
            dialog.show()
            val add=requireNotNull(dialog.getActionButton(DialogInterface.BUTTON_POSITIVE))
            add.performClick()
            assertTrue(dialog.isShowing)
            assertEquals(0,requests)
            val error=dialog.findViewById<TextView>(R.id.tvQuestionFormError)
            assertEquals(View.VISIBLE,error.visibility)
            val question=dialog.findViewById<EditText>(R.id.etQuestionText)
            question.setText("Draft question")
            listOf(R.id.etOptionA,R.id.etOptionB,R.id.etOptionC,R.id.etOptionD).forEachIndexed { i,id ->
                dialog.findViewById<EditText>(id).setText("Option $i")
            }
            add.performClick()
            shadowOf(Looper.getMainLooper()).idle()
            assertEquals(1,requests)
            assertFalse(add.isEnabled)
            assertEquals(controller.get().getString(R.string.question_form_submitting),add.text.toString())
            add.performClick()
            assertEquals(1,requests)
            assertTrue(dialog.isShowing)
            requireNotNull(pending).resume(ApiResponse<Any>(false,null,error="Permission denied"))
            shadowOf(Looper.getMainLooper()).idle()
            assertTrue(dialog.isShowing)
            assertEquals("Draft question",question.text.toString())
            assertEquals("Permission denied",error.text.toString())
            assertEquals(View.VISIBLE,error.visibility)
            assertTrue(add.isEnabled)
            add.performClick()
            shadowOf(Looper.getMainLooper()).idle()
            assertEquals(2,requests)
            requireNotNull(pending).resume(ApiResponse<Any>(true))
            shadowOf(Looper.getMainLooper()).idle()
            assertFalse("Only server confirmation closes the editor",dialog.isShowing)
        } finally { dialog.dismiss(); store.clear(); controller.pause().stop().destroy() }
    }
}
