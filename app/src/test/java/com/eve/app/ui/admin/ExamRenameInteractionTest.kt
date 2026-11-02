package com.eve.app.ui.admin

import android.app.Activity
import android.app.Application
import android.content.DialogInterface
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.lifecycle.ViewModelStore
import com.eve.app.R
import com.eve.app.data.model.Exam
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
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** Production rename form -> real VM/repository; held transport replies, no live mutations. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35], application=Application::class)
class ExamRenameInteractionTest {
    private fun View.input(): EditText? = when (this) {
        is EditText -> this
        is ViewGroup -> (0 until childCount).firstNotNullOfOrNull { getChildAt(it).input() }
        else -> null
    }
    @Test fun invalidNamesStayOpenAndDoNotWrite() = exercise { dialog, input, _, requests ->
        input.setText(" ")
        dialog.getActionButton(DialogInterface.BUTTON_POSITIVE)!!.performClick()
        assertTrue("Invalid form must stay open", dialog.isShowing)
        assertNotNull(input.error)
        assertEquals(0, requests())
        input.setText("Duplicate")
        dialog.getActionButton(DialogInterface.BUTTON_POSITIVE)!!.performClick()
        assertTrue(dialog.isShowing)
        assertNotNull(input.error)
        assertEquals(0, requests())
    }
    @Test fun pendingDeniedRetryAndConfirmedSaveUseTheWorkerContract() = exercise { dialog, input, reply, requests ->
        input.setText("  New exam  ")
        val button = dialog.getActionButton(DialogInterface.BUTTON_POSITIVE)!!
        button.performClick(); idle()
        assertTrue("Unconfirmed save must stay open", dialog.isShowing)
        assertFalse(button.isEnabled)
        button.performClick(); idle()
        assertEquals(1, requests())
        reply(ApiResponse<Unit>(false,error="Permission revoked")); idle()
        assertTrue(dialog.isShowing)
        assertEquals("  New exam  ", input.text.toString())
        assertEquals("Permission revoked", input.error.toString())
        assertTrue(button.isEnabled)
        button.performClick(); idle()
        assertEquals(2, requests())
        reply(ApiResponse<Unit>(true)); idle()
        assertFalse(dialog.isShowing)
    }
    @Test
    @Config(qualifiers="hi-rIN-ldrtl-night")
    fun hindiRtlLargeFontRetainsErrorAndAllowsExplicitCancel() {
        RuntimeEnvironment.setFontScale(2f)
        exercise { dialog, input, _, requests ->
            val button = dialog.getActionButton(DialogInterface.BUTTON_POSITIVE)!!
            assertEquals(dialog.context.getString(R.string.exam_rename_action), button.text.toString())
            input.setText("")
            button.performClick()
            assertTrue(dialog.isShowing)
            assertEquals(dialog.context.getString(R.string.exam_rename_too_short), input.error.toString())
            assertEquals(0, requests())
            dialog.getActionButton(DialogInterface.BUTTON_NEGATIVE)!!.performClick()
            assertFalse(dialog.isShowing)
        }
    }
    private fun idle() = shadowOf(Looper.getMainLooper()).idle()
    private fun exercise(block: (com.eve.app.designsystem.TelegramNativeAlertDialog, EditText,
                                (Any) -> Unit, () -> Int) -> Unit) {
        val controller = Robolectric.buildActivity(Activity::class.java)
        controller.get().setTheme(R.style.Theme_Eve)
        controller.setup().visible()
        var writes = 0
        var pending: Continuation<Any?>? = null
        val api = Proxy.newProxyInstance(EveApiService::class.java.classLoader,
            arrayOf(EveApiService::class.java)) { _, method, args ->
            when (method.name) {
                "renameExam" -> {
                    writes++
                    // Mirrors the currently authenticated official EVE Worker route contract.
                    assertEquals(mapOf("newName" to "New exam"), args!![1])
                    @Suppress("UNCHECKED_CAST")
                    val continuation = args.last() as Continuation<Any?>
                    pending = continuation
                    COROUTINE_SUSPENDED
                }
                "getExams" -> ApiResponse(true, listOf(Exam(id="exam",examName="New exam")))
                else -> error("Unexpected request ${method.name}")
            }
        } as EveApiService
        val vm = AdminViewModel(ExamRepository(api))
        val store = ViewModelStore().apply { put("rename", vm) }
        val exam = Exam(id="exam",examName="Old",category="Other")
        val dialog = createRenameExamDialog(controller.get(), exam,
            { listOf(exam, Exam(id="duplicate",examName="Duplicate",category="Other")) }, vm)
        try {
            dialog.show()
            block(dialog, requireNotNull(dialog.window!!.decorView.input()),
                { result -> requireNotNull(pending).resume(result) }, { writes })
        } finally { dialog.dismiss(); store.clear(); controller.pause().stop().destroy() }
    }
}
