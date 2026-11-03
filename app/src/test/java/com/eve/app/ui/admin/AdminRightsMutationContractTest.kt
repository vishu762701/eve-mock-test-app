package com.eve.app.ui.admin

import android.app.Application
import android.os.Looper
import androidx.lifecycle.ViewModelStore
import com.eve.app.data.remote.ApiResponse
import com.eve.app.data.remote.DynamicAdminItem
import com.eve.app.data.remote.EveApiService
import com.eve.app.data.repository.AdminRepository
import com.eve.app.data.repository.ExamRepository
import com.google.gson.Gson
import java.lang.reflect.Proxy
import kotlin.coroutines.Continuation
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.resume
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** Exercises canonical AdminRepository and AdminViewModel, replacing only API transport. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class AdminRightsMutationContractTest {
    private val target = "moderator@example.invalid"
    private val granted = listOf("manage_users")
    private val confirmed = DynamicAdminItem(
        email = target, rights = granted, customTitle = "Moderator"
    )

    private fun repository(answer: ApiResponse<DynamicAdminItem>): AdminRepository {
        val api = Proxy.newProxyInstance(
            EveApiService::class.java.classLoader,
            arrayOf(EveApiService::class.java)
        ) { _, method, _ ->
            check(method.name == "updateAdminRights")
            answer
        } as EveApiService
        return AdminRepository(api)
    }

    @Test fun confirmedWorkerPayloadMustMatchRequestedIdentityRightsAndTitle() = runBlocking {
        assertEquals(confirmed,
            repository(ApiResponse(success = true, data = confirmed))
                .updateAdminRights(target, granted, "Moderator"))
        for (response in listOf(
            ApiResponse<DynamicAdminItem>(success = true, data = null),
            ApiResponse(success = false, data = confirmed, error = "Denied"),
            ApiResponse(success = true, data = confirmed.copy(email = "wrong@example.invalid")),
            ApiResponse(success = true, data = confirmed.copy(rights = listOf("manage_questions"))),
            ApiResponse(success = true, data = confirmed.copy(customTitle = "Other"))
        )) {
            val result = runCatching { repository(response).updateAdminRights(target, granted, "Moderator") }
            assertTrue("Malformed or denied Worker ack cannot become success", result.isFailure)
        }
    }

    @Test fun gsonNullRightsNeverGrantAnOptimisticAdminUpdate() = runBlocking {
        val malformed = Gson().fromJson(
            """{"email":"moderator@example.invalid","rights":null,"customTitle":"Moderator"}""",
            DynamicAdminItem::class.java
        )
        val result = runCatching {
            repository(ApiResponse(success = true, data = malformed))
                .updateAdminRights(target, granted, "Moderator")
        }
        assertTrue(result.isFailure)
    }

    @Test fun malformedLocalInputsFailBeforeTransportMutation() = runBlocking {
        val repo = repository(ApiResponse(success = true, data = confirmed))
        assertTrue(runCatching { repo.updateAdminRights("", granted, "Moderator") }.isFailure)
        assertTrue(runCatching { repo.updateAdminRights(target, granted + granted, "Moderator") }.isFailure)
        assertTrue(runCatching { repo.updateAdminRights(target, granted, "X".repeat(65)) }.isFailure)
    }

    private class HeldTransport {
        var rightsWrites = 0
        var pending: Continuation<Any?>? = null
        val api = Proxy.newProxyInstance(
            EveApiService::class.java.classLoader,
            arrayOf(EveApiService::class.java)
        ) { _, method, args ->
            when (method.name) {
                "updateAdminRights" -> {
                    rightsWrites++
                    @Suppress("UNCHECKED_CAST")
                    val continuation = args!!.last() as Continuation<Any?>
                    pending = continuation
                    COROUTINE_SUSPENDED
                }
                "getDynamicAdmins" -> ApiResponse(success = true, data = emptyList<DynamicAdminItem>())
                else -> error("Unexpected API request: ${method.name}")
            }
        } as EveApiService
        val vm = AdminViewModel(ExamRepository(api), AdminRepository(api))
        val store = ViewModelStore().apply { put("admin-rights", vm) }
        fun finish(response: ApiResponse<DynamicAdminItem>) {
            requireNotNull(pending).resume(response)
            pending = null
        }
        fun close() = store.clear()
    }

    @Test fun viewModelAcquiresBusyBeforeCoroutineAndPreventsDuplicatePrivilegeWrites() {
        val h = HeldTransport()
        var successes = 0
        var failures = 0
        try {
            val attempt = {
                h.vm.updateAdminRights("moderator@example.invalid", listOf("manage_users"), "Moderator") {
                    if (it.isSuccess) successes++ else failures++
                }
            }
            attempt()
            assertTrue("Busy must be set before the second tap", h.vm.busy.value)
            attempt()
            shadowOf(Looper.getMainLooper()).idle()
            assertEquals(1, h.rightsWrites)
            h.finish(ApiResponse(success = true, data = DynamicAdminItem(
                email = "moderator@example.invalid", rights = listOf("manage_users"), customTitle = "Moderator"
            )))
            shadowOf(Looper.getMainLooper()).idle()
            assertEquals(1, successes)
            assertEquals(0, failures)
            assertFalse(h.vm.busy.value)
        } finally { h.close() }
    }

    @Test fun missingWorkerAckCannotBecomeViewModelSuccess() {
        val h = HeldTransport()
        var successes = 0
        var failures = 0
        try {
            h.vm.updateAdminRights("moderator@example.invalid", listOf("manage_users"), "Moderator") {
                if (it.isSuccess) successes++ else failures++
            }
            shadowOf(Looper.getMainLooper()).idle()
            h.finish(ApiResponse(success = true, data = null))
            shadowOf(Looper.getMainLooper()).idle()
            assertEquals(0, successes)
            assertEquals(1, failures)
            assertFalse(h.vm.busy.value)
        } finally { h.close() }
    }
}
