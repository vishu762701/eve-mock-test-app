package com.eve.app.ui.admin

import android.app.Application
import android.os.Looper
import android.view.View
import android.widget.EditText
import android.widget.TextView
import com.eve.app.R
import com.eve.app.data.model.AdminPremiumConfigDto
import com.eve.app.data.remote.AdminMeData
import com.eve.app.data.remote.ApiResponse
import com.eve.app.ui.profile.ProfileEditorAuthFixture
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35],application=Application::class,
    shadows=[ProfileEditorAuthFixture.ShadowAuth::class,EditExamBoundaryFixture.AdminBoundary::class,
        AdminApiTransportFixture.Transport::class],
    instrumentedPackages=["com.eve.app.data.repository","com.eve.app.data.remote"])
class PremiumMutationLifecycleTest {
    private val confirmed=AdminPremiumConfigDto(id="default",planName="EVE Premium",priceInr=199,
        currency="INR",durationDays=30,sessionExpiryMinutes=10,merchantVpa="fixture@bank",
        merchantName="EVE",description="Mock tests",benefits=listOf("Mock tests"))
    @Before fun before() {
        ProfileEditorAuthFixture.reset();ProfileEditorAuthFixture.setOwner("owner-a")
        EditExamBoundaryFixture.reset();rights("manage_premium")
        PremiumMutationBoundaryFixture.reset()
        AdminApiTransportFixture.current=PremiumMutationBoundaryFixture.transport()
        PremiumMutationBoundaryFixture.config=ApiResponse(success=true,data=confirmed)
        PremiumMutationBoundaryFixture.emptyList=ApiResponse(success=true,data=emptyList<Any>())
    }
    @After fun after() {AdminApiTransportFixture.current=null;ProfileEditorAuthFixture.reset();EditExamBoundaryFixture.reset();PremiumMutationBoundaryFixture.reset()}
    private fun idle()=shadowOf(Looper.getMainLooper()).idle()
    private fun rights(vararg values:String) { EditExamBoundaryFixture.permissions=AdminMeData(email="owner@example.test",rights=values.toList()) }
    private fun exercise(block:(ManagePremiumActivity,org.robolectric.android.controller.ActivityController<ManagePremiumActivity>)->Unit) {
        // Fresh Worker authorization completes after STARTED, as a real suspended gate does.
        EditExamBoundaryFixture.holdMembership=true
        val c=Robolectric.buildActivity(ManagePremiumActivity::class.java)
        c.get().setTheme(R.style.Theme_Eve);c.setup().visible();idle()
        EditExamBoundaryFixture.holdMembership=false
        EditExamBoundaryFixture.pendingMembership.resume(true);idle()
        try {block(c.get(),c)}finally{c.pause().stop().destroy();idle()}
    }
    private fun save(a:ManagePremiumActivity):View {
        a.findViewById<EditText>(R.id.etPlanName).setText("Updated plan")
        return a.findViewById<View>(R.id.btnSaveSettings).also{it.performClick();idle()}
    }
    private fun message(a:ManagePremiumActivity)=a.findViewById<TextView>(R.id.tvBulletinMessage)?.text?.toString().orEmpty()
    @Test fun suspendedPermissionCannotDispatchAcrossStopAndReturn()=exercise {a,c->
        EditExamBoundaryFixture.holdMembership=true;save(a)
        assertEquals(0,PremiumMutationBoundaryFixture.writes)
        c.pause().stop();c.start().resume();idle()
        EditExamBoundaryFixture.holdMembership=false
        EditExamBoundaryFixture.pendingMembership.resume(true);idle()
        assertEquals(0,PremiumMutationBoundaryFixture.writes)
    }
    @Test fun delayedPutAcknowledgmentCannotLaunchReadbackAfterStopAndReturn()=exercise {a,c->
        save(a);assertEquals(1,PremiumMutationBoundaryFixture.writes)
        val reads=PremiumMutationBoundaryFixture.configReads
        c.pause().stop();c.start().resume();idle()
        PremiumMutationBoundaryFixture.pendingWrite.resume(ApiResponse<Any>(success=true));idle()
        assertEquals(reads,PremiumMutationBoundaryFixture.configReads)
        assertFalse(message(a).contains("confirmed by the server"))
    }
    @Test fun pendingSaveRejectsDuplicateTapAndWaitsForConfirmedReadback()=exercise {a,_->
        val save=save(a);save.performClick();idle()
        assertEquals(1,PremiumMutationBoundaryFixture.writes)
        assertFalse(message(a).contains("confirmed by the server"))
        PremiumMutationBoundaryFixture.config=ApiResponse(success=true,data=PremiumMutationBoundaryFixture.submittedConfig)
        PremiumMutationBoundaryFixture.pendingWrite.resume(ApiResponse<Any>(success=true));idle()
        assertTrue(message(a).contains("confirmed by the server"))
    }
    @Test fun permissionRevokedAfterDialogEntryDoesNotWrite()=exercise {a,_->
        rights("view_analytics_logs");save(a);assertEquals(0,PremiumMutationBoundaryFixture.writes)
    }
    @Test fun accountReplacementDuringFreshPermissionReadDoesNotWrite()=exercise {a,_->
        EditExamBoundaryFixture.holdMembership=true;save(a)
        ProfileEditorAuthFixture.listeners.clear();ProfileEditorAuthFixture.setOwner("owner-b")
        EditExamBoundaryFixture.holdMembership=false;EditExamBoundaryFixture.pendingMembership.resume(true);idle()
        assertEquals(0,PremiumMutationBoundaryFixture.writes)
    }
    @Test fun failedWorkerAckCannotBecomeSuccessOrReadback()=exercise {a,_->
        save(a);val reads=PremiumMutationBoundaryFixture.configReads
        PremiumMutationBoundaryFixture.pendingWrite.resume(ApiResponse<Any>(success=false,error="Worker denied"));idle()
        assertEquals(reads,PremiumMutationBoundaryFixture.configReads)
        assertFalse(message(a).contains("confirmed by the server"))
    }
    @Test fun mismatchedWorkerReadbackCannotBecomeSuccess()=exercise {a,_->
        save(a);PremiumMutationBoundaryFixture.pendingWrite.resume(ApiResponse<Any>(success=true));idle()
        assertFalse(message(a).contains("confirmed by the server"))
        assertTrue(message(a).isNotBlank())
    }
    @Test fun networkFailureKeepsDraftWithoutReportingSuccess()=exercise {a,_->
        save(a);PremiumMutationBoundaryFixture.pendingWrite.resumeWithException(java.io.IOException("Offline"));idle()
        assertEquals("Updated plan",a.findViewById<EditText>(R.id.etPlanName).text.toString())
        assertFalse(message(a).contains("confirmed by the server"))
    }
}
