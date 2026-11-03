package com.eve.app.ui.admin

import android.app.Application
import android.os.Looper
import android.view.View
import android.widget.TextView
import com.eve.app.R
import com.eve.app.data.remote.AdminMeData
import com.eve.app.data.remote.ApiResponse
import com.eve.app.data.remote.MonthlyApiUsageDto
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
class ApiUsageOwnershipTest {
    @Before fun before() {
        ProfileEditorAuthFixture.reset();ProfileEditorAuthFixture.setOwner("owner-a")
        EditExamBoundaryFixture.reset()
        EditExamBoundaryFixture.permissions=AdminMeData(email="owner@example.test",rights=listOf("view_analytics_logs"))
        ApiUsageBoundaryFixture.reset();AdminApiTransportFixture.current=ApiUsageBoundaryFixture.transport()
    }
    @After fun after() {AdminApiTransportFixture.current=null;ApiUsageBoundaryFixture.reset();EditExamBoundaryFixture.reset();ProfileEditorAuthFixture.reset()}
    private fun idle()=shadowOf(Looper.getMainLooper()).idle()
    private fun exercise(block:(ApiUsageActivity,org.robolectric.android.controller.ActivityController<ApiUsageActivity>)->Unit) {
        EditExamBoundaryFixture.holdMembership=true
        val c=Robolectric.buildActivity(ApiUsageActivity::class.java)
        c.get().setTheme(R.style.Theme_Eve);c.setup().visible();idle()
        EditExamBoundaryFixture.holdMembership=false;EditExamBoundaryFixture.pendingMembership.resume(true);idle()
        try{block(c.get(),c)}finally{
            if(c.get().lifecycle.currentState!=androidx.lifecycle.Lifecycle.State.DESTROYED)c.pause().stop().destroy()
            idle()
        }
    }
    private fun count(a:ApiUsageActivity)=a.findViewById<TextView>(R.id.tvGeminiCallsCount).text.toString()
    private fun refresh(a:ApiUsageActivity){a.findViewById<View>(R.id.btnRefresh).performClick();idle()}
    private fun answer(index:Int,calls:Long)=ApiUsageBoundaryFixture.reads[index].resume(ApiResponse(true,
        MonthlyApiUsageDto(yearMonth=ApiUsageBoundaryFixture.months[index],monthDisplayName="Confirmed month",
            geminiCalls=calls,geminiSuccessfulCalls=calls,geminiFailedCalls=0,documentsCreated=0,testSubmissions=0,lastUpdated=123)))
    @Test fun pendingReadCannotDisplayFabricatedZero()=exercise {a,_->
        assertEquals(1,ApiUsageBoundaryFixture.reads.size)
        assertFalse(count(a)=="0")
    }
    @Test fun confirmedZeroIsRenderedAsZero()=exercise {a,_->
        answer(0,0);idle();assertEquals("0",count(a))
    }
    @Test fun failedMissingAndDeniedResponsesRemainUnavailable() {
        for(kind in listOf("network","missing","revoked"))exercise {a,_->
            val index=ApiUsageBoundaryFixture.reads.lastIndex
            when(kind){
                "network"->ApiUsageBoundaryFixture.reads[index].resumeWithException(java.io.IOException("offline"))
                "missing"->ApiUsageBoundaryFixture.reads[index].resume(ApiResponse<MonthlyApiUsageDto>(true))
                else->ApiUsageBoundaryFixture.reads[index].resume(ApiResponse<MonthlyApiUsageDto>(false,error="rights revoked"))
            }
            idle();assertFalse(kind,count(a)=="0")
            assertNotNull(a.findViewById<TextView>(R.id.tvBulletinMessage))
        }
    }
    @Test fun pendingRefreshCoalescesDuplicateTaps()=exercise {a,_->
        refresh(a);refresh(a);assertEquals(1,ApiUsageBoundaryFixture.reads.size)
    }
    @Test fun accountChangeSuppressesPendingCountersEvenWithoutAuthCallback()=exercise {a,_->
        ProfileEditorAuthFixture.listeners.clear();ProfileEditorAuthFixture.setOwner("owner-b")
        answer(0,42);idle();assertFalse(count(a)=="42")
    }
    @Test fun stoppedOldResponseCannotOverwriteNewerRefresh()=exercise {a,c->
        c.pause().stop();c.start().resume();idle();refresh(a)
        assertEquals(2,ApiUsageBoundaryFixture.reads.size)
        answer(1,17);idle();assertEquals("17",count(a))
        answer(0,42);idle();assertEquals("17",count(a))
    }
    @Test fun cancelledReadDoesNotPublishAfterStop()=exercise {a,c->
        c.pause().stop();answer(0,42);idle();assertFalse(count(a)=="42")
    }
    @Test fun failedRefreshCannotLeaveConfirmedCountersLookingFresh()=exercise {a,_->
        answer(0,17);idle();assertEquals("17",count(a))
        refresh(a)
        ApiUsageBoundaryFixture.reads[1].resumeWithException(java.io.IOException("offline"));idle()
        assertFalse(count(a)=="17");assertFalse(count(a)=="0")
    }
    @Test fun recreatedOwnerReadsAgainAndRejectsOldResponse()=exercise {a,c->
        val state=android.os.Bundle();c.pause().saveInstanceState(state).stop().destroy()
        val restored=Robolectric.buildActivity(ApiUsageActivity::class.java,a.intent)
        restored.get().setTheme(R.style.Theme_Eve);restored.setup(state).visible();idle()
        try {
            assertFalse(restored.get().isFinishing);assertEquals(2,ApiUsageBoundaryFixture.reads.size)
            answer(1,17);idle();answer(0,42);idle();assertEquals("17",count(restored.get()))
        } finally {restored.pause().stop().destroy();idle()}
    }
    @Test fun restoredScreenCannotAdoptAnotherAccount()=exercise {a,c->
        val state=android.os.Bundle();c.pause().saveInstanceState(state).stop().destroy()
        ProfileEditorAuthFixture.setOwner("owner-b")
        val restored=Robolectric.buildActivity(ApiUsageActivity::class.java,a.intent)
        restored.get().setTheme(R.style.Theme_Eve);restored.setup(state).visible();idle()
        try {
            assertTrue(restored.get().isFinishing);assertEquals(1,ApiUsageBoundaryFixture.reads.size)
            answer(0,42);idle();assertFalse(count(restored.get())=="42")
        } finally {restored.pause().stop().destroy();idle()}
    }
    @Test fun deniedEntryDoesNotDispatchAUsageRead() {
        EditExamBoundaryFixture.permissions=AdminMeData(email="owner@example.test",rights=emptyList())
        exercise {a,_->assertTrue(a.isFinishing);assertEquals(0,ApiUsageBoundaryFixture.reads.size)}
    }
}
