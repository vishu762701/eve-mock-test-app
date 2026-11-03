package com.eve.app.ui.admin

import android.app.Application
import android.os.Looper
import androidx.lifecycle.ViewModelStore
import com.eve.app.data.model.Exam
import com.eve.app.data.remote.ApiResponse
import com.eve.app.data.remote.EveApiService
import com.eve.app.data.repository.ExamRepository
import java.lang.reflect.Proxy
import kotlin.coroutines.Continuation
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** Drives the real AdminViewModel -> ExamRepository path; only Retrofit responses are mocked. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class ExamDeleteCurrentOwnerTest {
    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    private class Harness {
        var requests = 0
        var reads = 0
        var pending: Continuation<Any?>? = null
        val api = Proxy.newProxyInstance(
            EveApiService::class.java.classLoader,
            arrayOf(EveApiService::class.java)
        ) { _, method, args ->
            when (method.name) {
                "deleteExam" -> {
                    requests++
                    @Suppress("UNCHECKED_CAST")
                    val continuation = args!!.last() as Continuation<Any?>
                    pending = continuation
                    COROUTINE_SUSPENDED
                }
                "getExams" -> {
                    reads++
                    ApiResponse(success = true, data = listOf(Exam(id = "other", examName = "Other")))
                }
                else -> error("Unexpected request: " + method.name)
            }
        } as EveApiService
        val vm = AdminViewModel(ExamRepository(api))
        val store = ViewModelStore().apply { put("admin-delete", vm) }
        fun close() { store.clear() }
        fun finishSuccess() {
            requireNotNull(pending).resume(ApiResponse<Unit>(success = true))
            pending = null
        }
    }

    @Test fun noPermissionDoesNotDispatchDelete() {
        val h = Harness()
        var done = 0
        var error: String? = null
        try {
            h.vm.deleteExam("exam", ownerCurrent = { true }, authorize = { false },
                onFailure = { error = it }) { done++ }
            idle()
            assertEquals(0, h.requests)
            assertEquals(0, done)
            assertNotNull(error)
            assertFalse(h.vm.busy.value)
        } finally { h.close() }
    }

    @Test fun repeatedTapsDispatchOnceAndOnlyConfirmedResponseSucceeds() {
        val h = Harness()
        var done = 0
        try {
            val start = { h.vm.deleteExam("exam", { true }, { true }) { done++ } }
            start()
            assertTrue("Duplicate protection is acquired before coroutine launch", h.vm.busy.value)
            start()
            idle()
            assertEquals(1, h.requests)
            assertEquals(0, done)
            h.finishSuccess()
            idle()
            assertEquals(1, done)
            assertEquals(1, h.reads)
            assertFalse(h.vm.busy.value)
        } finally { h.close() }
    }

    @Test fun switchedOwnerWhilePermissionRequestPendingCannotSendDelete() {
        val h = Harness()
        var owner = true
        var permission: Continuation<Boolean>? = null
        var done = 0
        try {
            h.vm.deleteExam("exam", ownerCurrent = { owner },
                authorize = { suspendCoroutine { permission = it } }) { done++ }
            idle()
            assertNotNull(permission)
            assertEquals(0, h.requests)
            owner = false
            requireNotNull(permission).resume(true)
            idle()
            assertEquals(0, h.requests)
            assertEquals(0, done)
            assertFalse(h.vm.busy.value)
        } finally { h.close() }
    }

    @Test fun lateReplyAfterAccountChangeNeverPublishesOldAccountSuccess() {
        val h = Harness()
        var owner = true
        var done = 0
        try {
            h.vm.deleteExam("exam", ownerCurrent = { owner }, authorize = { true }) { done++ }
            idle()
            assertEquals(1, h.requests)
            owner = false
            h.finishSuccess()
            idle()
            assertEquals(0, done)
            assertEquals(0, h.reads)
            assertFalse(h.vm.busy.value)
        } finally { h.close() }
    }

    @Test fun cancelledTransportReleasesBusyWithoutFalseConfirmation() {
        val h = Harness()
        var completed = 0
        var failed = 0
        try {
            h.vm.deleteExam("exam", ownerCurrent = { true }, authorize = { true },
                onFailure = { failed++ }) { completed++ }
            idle()
            assertEquals(1, h.requests)
            requireNotNull(h.pending).resumeWith(
                Result.failure(kotlinx.coroutines.CancellationException("Navigated away"))
            )
            idle()
            assertFalse(h.vm.busy.value)
            assertEquals(0, completed)
            assertEquals(0, failed)
            assertEquals(0, h.reads)
        } finally { h.close() }
    }

    @Test fun unconfirmedDeleteHasNoSuccessCallback() {
        val h = Harness()
        var done = 0
        var failure: String? = null
        try {
            h.vm.deleteExam("exam", ownerCurrent = { true }, authorize = { true },
                onFailure = { failure = it }) { done++ }
            idle()
            requireNotNull(h.pending).resume(ApiResponse<Unit>(
                success = false, error = "Permission revoked"
            ))
            idle()
            assertEquals(0, done)
            assertNotNull(failure)
            assertEquals(0, h.reads)
            assertFalse(h.vm.busy.value)
        } finally { h.close() }
    }
}
