package com.eve.app.util

import com.eve.app.data.remote.AdminMeData
import com.eve.app.data.remote.ApiResponse
import com.eve.app.data.remote.EveApiService
import com.eve.app.data.repository.AdminRepository
import com.google.gson.Gson
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.lang.reflect.Proxy

class AdminPermissionSnapshotTest {
    private val email = "admin@example.invalid"
    private val rights = listOf("manage_exams", "manage_questions", "manage_generated_tests",
        "manage_syllabus", "manage_content", "manage_users", "manage_admins_rights", "manage_premium",
        "send_broadcasts", "moderate_feedback", "view_analytics_logs", "manage_config_maintenance", "export_data")

    @Test fun eachLimitedRoleOnlyEntersItsGrantedDestinations() {
        for (granted in rights) {
            val snapshot = AdminMeData(email = email, rights = listOf(granted))
            assertTrue(AdminPermissionSnapshot.allows(snapshot, email))
            for (requested in rights) assertEquals("$granted -> $requested", granted == requested,
                AdminPermissionSnapshot.allows(snapshot, email, requested))
        }
        val owner = AdminMeData(email = email, isOwner = true)
        rights.forEach { assertTrue(AdminPermissionSnapshot.allows(owner, email, it)) }
    }

    @Test fun actualJsonMissingOrMalformedFieldsCannotGrantOwnerOrLimitedAccess() {
        val malformed = listOf(
            "{}", "{\"email\":null,\"isOwner\":true,\"rights\":[]}",
            "{\"email\":\"$email\",\"isOwner\":true,\"rights\":null}",
            "{\"email\":\"$email\",\"isOwner\":true,\"rights\":[null]}",
            "{\"email\":\"$email\",\"rights\":[\"\"]}")
        for (json in malformed) {
            val response = Gson().fromJson(json, AdminMeData::class.java)
            assertFalse(json, AdminPermissionSnapshot.allows(response, email))
            assertFalse(json, AdminPermissionSnapshot.allows(response, email, "manage_users"))
        }
        assertFalse(AdminPermissionSnapshot.allows(null, email))
        assertFalse(AdminPermissionSnapshot.allows(AdminMeData(email=email,isOwner=true), ""))
        assertFalse(AdminPermissionSnapshot.allows(AdminMeData(email="other@example.invalid",isOwner=true), email))
        assertTrue(AdminPermissionSnapshot.allows(AdminMeData(email=email.uppercase()), email))
    }

    @Test fun permissionLoadMustCompleteBeforeAnyProtectedCallbackAndDenialDoesNotAuthorize() = runBlocking {
        val response = CompletableDeferred<AdminMeData>()
        val exposed = mutableListOf<AdminMeData>()
        val task = launch(start = CoroutineStart.UNDISPATCHED) {
            AdminPermissionSnapshot.load(email, "manage_users", { true }, { response.await() })
                ?.let { exposed += it }
        }
        assertTrue("No controls/data callback while rights are loading", exposed.isEmpty())
        response.complete(AdminMeData(email=email, rights=listOf("manage_questions")))
        task.join()
        assertTrue("Membership alone never authorizes the destination", exposed.isEmpty())
    }

    @Test fun cancelledOrFailedRightsRequestCannotProduceAnAuthorizedCallback() = runBlocking {
        val waiting = CompletableDeferred<AdminMeData>()
        var exposed = false
        val task = launch(start = CoroutineStart.UNDISPATCHED) {
            AdminPermissionSnapshot.load(email, null, { true }, { waiting.await() })?.let { exposed = true }
        }
        task.cancelAndJoin()
        waiting.complete(AdminMeData(email=email,isOwner=true))
        assertFalse(exposed)
        val failed = runCatching {
            AdminPermissionSnapshot.load(email, null, { true }, { error("fixture server unavailable") })
                ?.let { exposed = true }
        }
        assertTrue(failed.isFailure)
        assertFalse(exposed)
        var readRights = false
        assertNull(AdminPermissionSnapshot.load(email, null, { false }, {
            readRights = true; AdminMeData(email=email,isOwner=true)
        }))
        assertFalse("Rejected membership never reads privileged permissions", readRights)
    }

    @Test fun successfulRightsRequestStillRejectsAnotherAccountsSnapshot() = runBlocking {
        assertNull(AdminPermissionSnapshot.load(email, null, { true }, {
            AdminMeData(email="other@example.invalid", isOwner=true)
        }))
        val response = AdminMeData(email=email, rights=listOf("manage_questions"))
        assertSame(response, AdminPermissionSnapshot.load(email,"manage_questions",{true},{response}))
    }

    @Test fun repositoryRejectsMissingServerPermissionsInsteadOfCreatingDefaultSuccess() = runBlocking {
        val api = Proxy.newProxyInstance(EveApiService::class.java.classLoader,
            arrayOf(EveApiService::class.java)) { _, method, _ ->
            check(method.name == "getAdminMe")
            ApiResponse<AdminMeData>(success=true,data=null)
        } as EveApiService
        val failure = runCatching { AdminRepository(api).getAdminMe() }.exceptionOrNull()
        assertTrue(failure is IllegalStateException)
    }
}
