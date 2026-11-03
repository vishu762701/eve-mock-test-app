package com.eve.app.ui.admin

import android.app.Application
import android.content.DialogInterface
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.eve.app.R
import com.eve.app.data.remote.AdminMeData
import com.eve.app.data.remote.ApiResponse
import com.eve.app.designsystem.TelegramNativeAlertDialog
import com.eve.app.ui.profile.ProfileEditorAuthFixture
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDialog

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35], application=Application::class,
    shadows=[ProfileEditorAuthFixture.ShadowAuth::class, EditExamBoundaryFixture.AdminBoundary::class,
        SystemMonitorBoundaryFixture.Transport::class],
    instrumentedPackages=["com.eve.app.data.repository", "com.eve.app.data.remote"])
class SystemMonitorOwnershipTest {
    private val viewRight = "view_analytics_logs"
    private val retryRight = "manage_generated_tests"
    private fun response(json: String) = ApiResponse(success=true, data=JsonParser.parseString(json).asJsonObject)
    @Before fun before() {
        ProfileEditorAuthFixture.reset(); ProfileEditorAuthFixture.setOwner("owner-a")
        EditExamBoundaryFixture.reset(); rights(viewRight, retryRight)
        SystemMonitorBoundaryFixture.reset()
        SystemMonitorBoundaryFixture.overview = overview(3)
        SystemMonitorBoundaryFixture.health = response("""{"database":{"status":"verified","detail":"Local fixture"}}""")
        SystemMonitorBoundaryFixture.operations = response("""{"nextOffset":null,"jobs":[{
            "id":"failed-job","exam_name":"Exam","status":"failed","retryable":1}],
            "submissions":[],"failures":[]}""")
    }
    @After fun after() {
        ShadowDialog.getLatestDialog()?.dismiss()
        ProfileEditorAuthFixture.reset(); EditExamBoundaryFixture.reset(); SystemMonitorBoundaryFixture.reset()
    }
    private fun rights(vararg values: String) {
        EditExamBoundaryFixture.permissions = AdminMeData(email="owner@example.test",rights=values.toList())
    }
    private fun overview(users: Int) = response("""{"totals":{"users":$users,"exams":1,
        "publishedTests":1,"questions":1,"attempts":1},"activity":[]}""")
    private fun idle() = shadowOf(Looper.getMainLooper()).idle()
    private fun texts(root: View): List<TextView> = buildList {
        if (root is TextView) add(root)
        if (root is ViewGroup) for (i in 0 until root.childCount) addAll(texts(root.getChildAt(i)))
    }
    private fun root(a: SystemMonitorActivity): View = a.findViewById(R.id.layoutMonitorContent)
    private fun button(a: SystemMonitorActivity, label: String) = texts(root(a)).single { it.text.toString()==label }
    private fun confirm(a: SystemMonitorActivity): View {
        button(a,"Retry Exam").performClick(); idle()
        val dialog = ShadowDialog.getLatestDialog() as TelegramNativeAlertDialog
        assertTrue(dialog.isShowing)
        return requireNotNull(dialog.getActionButton(DialogInterface.BUTTON_POSITIVE))
    }
    private fun exercise(block: (SystemMonitorActivity, org.robolectric.android.controller.ActivityController<SystemMonitorActivity>) -> Unit) {
        val c=Robolectric.buildActivity(SystemMonitorActivity::class.java)
        c.get().setTheme(R.style.Theme_Eve); c.setup().visible(); idle()
        try { block(c.get(),c) } finally { c.pause().stop().destroy(); idle() }
    }
    @Test fun dispatchWaitsForFreshRightsAndRejectsDoubleTap() = exercise { a,_ ->
        val retry = confirm(a)
        EditExamBoundaryFixture.holdMembership=true
        retry.performClick(); idle()
        assertEquals(0,SystemMonitorBoundaryFixture.retries.size)
        retry.performClick(); idle()
        assertNotNull(EditExamBoundaryFixture.pendingMembership)
        EditExamBoundaryFixture.holdMembership=false
        EditExamBoundaryFixture.pendingMembership.resume(true); idle()
        assertEquals(listOf("failed-job"),SystemMonitorBoundaryFixture.retryIds)
        retry.performClick(); idle()
        assertEquals(1,SystemMonitorBoundaryFixture.retries.size)
    }
    @Test fun revokedMutationRightDoesNotDispatchEvenWithAnalyticsRight() = exercise { a,_ ->
        val retry=confirm(a); rights(viewRight)
        retry.performClick(); idle()
        assertEquals(0,SystemMonitorBoundaryFixture.retries.size)
    }
    @Test fun revokedParentRightDoesNotDispatchWithGenerationRightAlone() = exercise { a,_ ->
        val retry=confirm(a); rights(retryRight)
        retry.performClick(); idle()
        assertEquals(0,SystemMonitorBoundaryFixture.retries.size)
    }
    @Test fun failedFreshMembershipDoesNotDispatch() = exercise { a,_ ->
        val retry=confirm(a); EditExamBoundaryFixture.holdMembership=true
        retry.performClick(); idle()
        EditExamBoundaryFixture.pendingMembership.resume(false); idle()
        assertEquals(0,SystemMonitorBoundaryFixture.retries.size)
    }
    @Test fun cancelledConfirmationDoesNotDispatch() = exercise { a,_ ->
        confirm(a)
        val dialog=ShadowDialog.getLatestDialog() as TelegramNativeAlertDialog
        dialog.getActionButton(DialogInterface.BUTTON_NEGATIVE)?.performClick(); idle()
        assertEquals(0,SystemMonitorBoundaryFixture.retries.size)
    }
    @Test fun transportFailureKeepsRetryAvailableWithoutFakeRefresh() = exercise { a,_ ->
        val before=SystemMonitorBoundaryFixture.operationReads
        confirm(a).performClick(); idle()
        SystemMonitorBoundaryFixture.retries.last().resumeWithException(java.io.IOException("Offline")); idle()
        assertEquals(before,SystemMonitorBoundaryFixture.operationReads)
        assertTrue((ShadowDialog.getLatestDialog() as TelegramNativeAlertDialog).isShowing)
    }
    @Test fun accountSwitchDuringSuspendedPermissionCannotDispatch() = exercise { a,_ ->
        val retry=confirm(a); EditExamBoundaryFixture.holdMembership=true
        retry.performClick(); idle()
        ProfileEditorAuthFixture.listeners.clear(); ProfileEditorAuthFixture.setOwner("owner-b")
        EditExamBoundaryFixture.holdMembership=false
        EditExamBoundaryFixture.pendingMembership.resume(true); idle()
        assertEquals(0,SystemMonitorBoundaryFixture.retries.size)
    }
    @Test fun stoppedPermissionRequestCannotDispatchAfterReturn() = exercise { a,c ->
        val retry=confirm(a); EditExamBoundaryFixture.holdMembership=true
        retry.performClick(); idle()
        c.pause().stop(); idle()
        EditExamBoundaryFixture.holdMembership=false
        c.start().resume(); idle()
        EditExamBoundaryFixture.pendingMembership.resume(true); idle()
        assertEquals(0,SystemMonitorBoundaryFixture.retries.size)
    }
    @Test fun suspendedPermissionSnapshotMustStillBelongToInitiatingAccount() = exercise { a,_ ->
        val retry=confirm(a); EditExamBoundaryFixture.holdPermissions=true
        retry.performClick(); idle()
        assertNotNull(EditExamBoundaryFixture.pendingPermissions)
        assertEquals(0,SystemMonitorBoundaryFixture.retries.size)
        ProfileEditorAuthFixture.listeners.clear(); ProfileEditorAuthFixture.setOwner("owner-b")
        EditExamBoundaryFixture.pendingPermissions.resume(EditExamBoundaryFixture.permissions); idle()
        assertEquals(0,SystemMonitorBoundaryFixture.retries.size)
    }
    @Test fun suspendedPermissionSnapshotCannotGrantRevokedRight() = exercise { a,_ ->
        val retry=confirm(a); EditExamBoundaryFixture.holdPermissions=true
        retry.performClick(); idle(); rights(viewRight)
        EditExamBoundaryFixture.pendingPermissions.resume(EditExamBoundaryFixture.permissions); idle()
        assertEquals(0,SystemMonitorBoundaryFixture.retries.size)
    }
    @Test fun finishingNavigationRejectsRetainedConfirmation() = exercise { a,_ ->
        val retry=confirm(a); a.finish(); retry.performClick(); idle()
        assertEquals(0,SystemMonitorBoundaryFixture.retries.size)
    }
    @Test fun workerDenialAndMalformedSuccessShowFailureWithoutConfirmedRefresh() = exercise { a,_ ->
        val before=SystemMonitorBoundaryFixture.operationReads
        confirm(a).performClick(); idle()
        SystemMonitorBoundaryFixture.retries.last().resume(ApiResponse<JsonObject>(success=false,error="Worker denied")); idle()
        val denied=ShadowDialog.getLatestDialog() as TelegramNativeAlertDialog
        assertTrue(denied.isShowing)
        assertTrue(texts(requireNotNull(denied.window).decorView).any { it.text.toString().contains("Worker denied") })
        denied.dismiss(); idle()
        assertEquals(before,SystemMonitorBoundaryFixture.operationReads)
        confirm(a).performClick(); idle()
        SystemMonitorBoundaryFixture.retries.last().resume(response("{}")); idle()
        val malformed=ShadowDialog.getLatestDialog() as TelegramNativeAlertDialog
        assertTrue(malformed.isShowing)
        assertEquals(before,SystemMonitorBoundaryFixture.operationReads)
    }
    @Test fun confirmedWorkerRetryRefreshesOnceAndCannotRepeatSameJob() = exercise { a,_ ->
        val before=SystemMonitorBoundaryFixture.operationReads
        confirm(a).performClick(); idle()
        SystemMonitorBoundaryFixture.retries.last().resume(response("""{"testId":"test-1","jobId":"new-job","questionCount":10}""")); idle()
        assertEquals(before+1,SystemMonitorBoundaryFixture.operationReads)
        assertFalse(texts(root(a)).any { it.text.toString()=="Retry Exam" })
    }
    @Test fun delayedWorkerReplyAfterStopCannotRefreshOrShowFailure() = exercise { a,c ->
        confirm(a).performClick(); idle()
        val before=SystemMonitorBoundaryFixture.operationReads
        c.pause().stop(); idle()
        SystemMonitorBoundaryFixture.retries.last().resume(ApiResponse<JsonObject>(success=false,error="Old failure")); idle()
        assertEquals(before,SystemMonitorBoundaryFixture.operationReads)
        assertFalse(ShadowDialog.getLatestDialog()?.isShowing==true)
    }
    @Test fun newerRefreshOwnsRowsEvenWhenCancelledOldReadStillReturns() = exercise { a,_ ->
        SystemMonitorBoundaryFixture.holdOverview=true
        val load=SystemMonitorActivity::class.java.getDeclaredMethod("load").apply { isAccessible=true }
        load.invoke(a); idle(); load.invoke(a); idle()
        assertEquals(2,SystemMonitorBoundaryFixture.overviewReads.size)
        SystemMonitorBoundaryFixture.overviewReads[1].resume(overview(22)); idle()
        SystemMonitorBoundaryFixture.overviewReads[0].resume(overview(11)); idle()
        assertTrue(texts(root(a)).any { it.text.toString()=="Registered profiles: 22" })
        assertFalse(texts(root(a)).any { it.text.toString()=="Registered profiles: 11" })
    }
    @Test fun delayedReadCannotPublishAfterAccountChange() = exercise { a,_ ->
        SystemMonitorBoundaryFixture.holdOverview=true
        val load=SystemMonitorActivity::class.java.getDeclaredMethod("load").apply { isAccessible=true }
        load.invoke(a); idle()
        ProfileEditorAuthFixture.listeners.clear(); ProfileEditorAuthFixture.setOwner("owner-b")
        SystemMonitorBoundaryFixture.overviewReads.last().resume(overview(77)); idle()
        assertFalse(texts(root(a)).any { it.text.toString()=="Registered profiles: 77" })
    }
    @Test fun malformedWorkerAcknowledgmentsNeverConfirmGeneration() {
        for (json in listOf("{}", """{"testId":"t","jobId":"j"}""",
            """{"testId":"","jobId":"j","questionCount":10}""",
            """{"testId":true,"jobId":"j","questionCount":10}""",
            """{"testId":"t","jobId":null,"questionCount":10}""",
            """{"testId":"t","jobId":"j","questionCount":"10"}""",
            """{"testId":"t","jobId":"j","questionCount":0}""",
            """{"testId":"t","jobId":"j","questionCount":1.5}""")) {
            assertFalse(json,confirmedGenerationRetry(response(json)))
        }
        assertFalse(confirmedGenerationRetry(ApiResponse(success=true,data=null)))
        assertFalse(confirmedGenerationRetry(ApiResponse(success=false,
            data=JsonParser.parseString("""{"testId":"t","jobId":"j","questionCount":10}""").asJsonObject)))
    }
    @Test fun analyticsOnlyEntryHasNoMutationControl() {
        rights(viewRight)
        exercise { a,_ -> assertFalse(texts(root(a)).any { it.text.toString()=="Retry Exam" }) }
    }
}
