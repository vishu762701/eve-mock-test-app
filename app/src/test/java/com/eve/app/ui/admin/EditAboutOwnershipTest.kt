package com.eve.app.ui.admin

import android.app.Application
import android.content.DialogInterface
import android.os.Looper
import android.view.View
import android.widget.EditText
import android.widget.TextView
import com.eve.app.R
import com.eve.app.data.model.AppContent
import com.eve.app.data.remote.AdminMeData
import com.eve.app.data.remote.ApiResponse
import com.eve.app.designsystem.TelegramNativeAlertDialog
import com.eve.app.ui.profile.ProfileEditorAuthFixture
import com.google.android.material.tabs.TabLayout
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
@Config(sdk=[35],application=Application::class,
    shadows=[ProfileEditorAuthFixture.ShadowAuth::class,EditExamBoundaryFixture.AdminBoundary::class,
        AdminApiTransportFixture.Transport::class],
    instrumentedPackages=["com.eve.app.data.repository","com.eve.app.data.remote"])
class EditAboutOwnershipTest {
    private val original=AppContent(title="Confirmed privacy",body="Confirmed body",updatedAt=123)
    private val draft=original.copy(title="Draft title",body="Draft body")
    @Before fun before() {
        ProfileEditorAuthFixture.reset();ProfileEditorAuthFixture.setOwner("owner-a")
        EditExamBoundaryFixture.reset();rights("manage_content")
        AppContentBoundaryFixture.reset();AdminApiTransportFixture.current=AppContentBoundaryFixture.transport()
    }
    @After fun after(){AdminApiTransportFixture.current=null;AppContentBoundaryFixture.reset();EditExamBoundaryFixture.reset();ProfileEditorAuthFixture.reset()}
    private fun rights(vararg values:String){EditExamBoundaryFixture.permissions=AdminMeData(email="owner@example.test",rights=values.toList())}
    private fun idle()=shadowOf(Looper.getMainLooper()).idle()
    private fun exercise(block:(EditAboutActivity,org.robolectric.android.controller.ActivityController<EditAboutActivity>)->Unit) {
        EditExamBoundaryFixture.holdMembership=true
        val c=Robolectric.buildActivity(EditAboutActivity::class.java)
        c.get().setTheme(R.style.Theme_Eve);c.setup().visible();idle()
        EditExamBoundaryFixture.holdMembership=false;EditExamBoundaryFixture.pendingMembership.resume(true);idle()
        try{block(c.get(),c)}finally{
            ShadowDialog.getLatestDialog()?.dismiss()
            if(c.get().lifecycle.currentState!=androidx.lifecycle.Lifecycle.State.DESTROYED)c.pause().stop().destroy()
            idle()
        }
    }
    private fun read(index:Int,value:AppContent=original){AppContentBoundaryFixture.reads[index].resume(ApiResponse(true,value));idle()}
    private fun title(a:EditAboutActivity)=a.findViewById<EditText>(R.id.etTitle).text.toString()
    private fun edit(a:EditAboutActivity){a.findViewById<EditText>(R.id.etTitle).setText(draft.title);a.findViewById<EditText>(R.id.etBody).setText(draft.body)}
    private fun save(a:EditAboutActivity):View=a.findViewById<View>(R.id.btnSave).also{it.performClick();idle()}
    private fun message(a:EditAboutActivity)=a.findViewById<TextView>(R.id.tvBulletinMessage)?.text?.toString().orEmpty()
    private fun tab(a:EditAboutActivity,index:Int){a.findViewById<TabLayout>(R.id.tabLayout).getTabAt(index)!!.select();idle()}
    @Test fun failedReadCannotEnableAnEditableOfflineDefault()=exercise {a,_->
        AppContentBoundaryFixture.reads[0].resumeWithException(java.io.IOException("offline"));idle()
        assertFalse(a.findViewById<View>(R.id.btnSave).isEnabled)
        assertFalse(title(a)=="Privacy Policy")
    }
    @Test fun olderTabResponseCannotOverwriteNewerDocument()=exercise {a,_->
        tab(a,1);assertEquals(listOf("privacy","terms"),AppContentBoundaryFixture.readTypes)
        read(1,original.copy(title="Confirmed terms"));read(0)
        assertEquals("Confirmed terms",title(a))
    }
    @Test fun duplicateSaveWaitsForWorkerAndMatchingReadback()=exercise {a,_->
        read(0);edit(a);val button=save(a);button.performClick();idle()
        assertEquals(1,AppContentBoundaryFixture.writes.size)
        AppContentBoundaryFixture.writes[0].resume(ApiResponse<Unit>(true));idle()
        assertFalse(message(a).contains("Saved successfully"))
        assertEquals(2,AppContentBoundaryFixture.reads.size)
        read(1,draft);assertTrue(message(a).contains("Saved successfully"))
    }
    @Test fun revokedFreshPermissionDoesNotDispatchAWrite()=exercise {a,_->
        read(0);edit(a);rights();save(a)
        assertEquals(0,AppContentBoundaryFixture.writes.size)
        assertEquals(draft.title,title(a))
    }
    @Test fun suspendedFreshPermissionCannotDispatchAfterStopAndReturn()=exercise {a,c->
        read(0);edit(a);EditExamBoundaryFixture.holdMembership=true;save(a)
        assertEquals(0,AppContentBoundaryFixture.writes.size)
        val pending=EditExamBoundaryFixture.pendingMembership
        c.pause().stop();c.start().resume();idle()
        EditExamBoundaryFixture.holdMembership=false;pending.resume(true);idle()
        assertEquals(0,AppContentBoundaryFixture.writes.size)
    }
    @Test fun accountChangeCannotPublishAnOldDocument()=exercise {a,_->
        ProfileEditorAuthFixture.listeners.clear();ProfileEditorAuthFixture.setOwner("owner-b")
        read(0);assertFalse(title(a)==original.title)
    }
    @Test fun stoppedPutOnlyAllowsTheNewResumeReadAndCannotReportOldSuccess()=exercise {a,c->
        read(0);edit(a);save(a);assertEquals(1,AppContentBoundaryFixture.writes.size)
        c.pause().stop();c.start().resume();idle()
        assertEquals(1,AppContentBoundaryFixture.reads.size)
        AppContentBoundaryFixture.writes[0].resume(ApiResponse<Unit>(true));idle()
        // The new resumed read waits for the old PUT to settle; its result belongs to
        // the new epoch. The obsolete PUT cannot start its own confirmation readback.
        assertEquals(2,AppContentBoundaryFixture.reads.size)
        assertFalse(message(a).contains("Saved successfully"))
        read(1,draft)
        assertEquals(1,AppContentBoundaryFixture.writes.size)
        assertEquals(draft.title,title(a))
        assertTrue(a.findViewById<View>(R.id.btnSave).isEnabled)
    }
    @Test fun contradictoryReadbackCannotAcknowledgeOrReplaceTheDraft()=exercise {a,_->
        read(0);edit(a);save(a)
        AppContentBoundaryFixture.writes[0].resume(ApiResponse<Unit>(true));idle()
        assertEquals(2,AppContentBoundaryFixture.reads.size)
        read(1,original)
        assertFalse(message(a).contains("Saved successfully"));assertEquals(draft.title,title(a))
    }
    @Test fun failedWorkerAcknowledgmentKeepsTheDraft()=exercise {a,_->
        read(0);edit(a);save(a)
        AppContentBoundaryFixture.writes[0].resume(ApiResponse<Unit>(false,error="Worker denied"));idle()
        assertFalse(message(a).contains("Saved successfully"));assertEquals(draft.title,title(a))
        assertEquals(1,AppContentBoundaryFixture.reads.size)
    }
    @Test fun rapidDirtyTabClicksKeepOneOriginalTelegramConfirmation()=exercise {a,_->
        read(0);edit(a);tab(a,1)
        val first=ShadowDialog.getLatestDialog() as TelegramNativeAlertDialog
        tab(a,2);assertSame(first,ShadowDialog.getLatestDialog());assertTrue(first.isShowing)
    }
    @Test fun staleUnsavedConfirmationCannotWriteAfterStopAndReturn()=exercise {a,c->
        read(0);edit(a);tab(a,1)
        val dialog=ShadowDialog.getLatestDialog() as TelegramNativeAlertDialog
        val button=dialog.getActionButton(DialogInterface.BUTTON_POSITIVE)!!
        c.pause().stop();c.start().resume();idle();button.performClick();idle()
        assertFalse(dialog.isShowing);assertEquals(0,AppContentBoundaryFixture.writes.size)
    }
    @Test fun sameOwnerRecreationPreservesDirtyDraftAfterConfirmedReload()=exercise {a,c->
        read(0);edit(a)
        val state=android.os.Bundle();c.pause().saveInstanceState(state).stop().destroy()
        val restored=Robolectric.buildActivity(EditAboutActivity::class.java,a.intent)
        restored.get().setTheme(R.style.Theme_Eve);restored.setup(state).visible();idle()
        try{assertEquals(2,AppContentBoundaryFixture.reads.size);read(1);assertEquals(draft.title,title(restored.get()))}
        finally{restored.pause().stop().destroy();idle()}
    }
    @Test fun unchangedFormUsesFreshServerContentAfterStopAndReturn()=exercise {a,c->
        read(0);c.pause().stop();c.start().resume();idle()
        read(1,original.copy(title="New authoritative title"))
        assertEquals("New authoritative title",title(a))
    }
    @Test fun accountChangeDuringSuspendedRightsCannotDispatchWrite()=exercise {a,_->
        read(0);edit(a);EditExamBoundaryFixture.holdPermissions=true;save(a)
        assertNotNull(EditExamBoundaryFixture.pendingPermissions)
        ProfileEditorAuthFixture.listeners.clear();ProfileEditorAuthFixture.setOwner("owner-b")
        EditExamBoundaryFixture.pendingPermissions.resume(EditExamBoundaryFixture.permissions);idle()
        assertEquals(0,AppContentBoundaryFixture.writes.size)
    }
    @Test fun dirtyTabCancelAndDiscardPreserveOriginalNativeNavigation()=exercise {a,_->
        read(0);edit(a);tab(a,1)
        (ShadowDialog.getLatestDialog() as TelegramNativeAlertDialog).getActionButton(DialogInterface.BUTTON_NEUTRAL)!!.performClick();idle()
        assertEquals(0,a.findViewById<TabLayout>(R.id.tabLayout).selectedTabPosition)
        assertEquals(draft.title,title(a));assertEquals(1,AppContentBoundaryFixture.reads.size)
        tab(a,2)
        (ShadowDialog.getLatestDialog() as TelegramNativeAlertDialog).getActionButton(DialogInterface.BUTTON_NEGATIVE)!!.performClick();idle()
        assertEquals(listOf("privacy","contact"),AppContentBoundaryFixture.readTypes)
        assertEquals(0,AppContentBoundaryFixture.writes.size)
    }
    @Test fun dirtyTabSaveNavigatesOnlyAfterMatchingConfirmation()=exercise {a,_->
        read(0);edit(a);tab(a,1)
        (ShadowDialog.getLatestDialog() as TelegramNativeAlertDialog).getActionButton(DialogInterface.BUTTON_POSITIVE)!!.performClick();idle()
        assertEquals(1,AppContentBoundaryFixture.writes.size)
        AppContentBoundaryFixture.writes[0].resume(ApiResponse<Unit>(true));idle()
        assertEquals(listOf("privacy","privacy"),AppContentBoundaryFixture.readTypes)
        read(1,draft)
        assertEquals(listOf("privacy","privacy","terms"),AppContentBoundaryFixture.readTypes)
        read(2,original.copy(title="Confirmed terms"));assertEquals("Confirmed terms",title(a))
    }
    @Test fun backSaveReusesThePendingWriteAndWaitsForMatchingReadback()=exercise {a,_->
        read(0);edit(a);save(a);a.findViewById<View>(R.id.btnBack).performClick();idle()
        (ShadowDialog.getLatestDialog() as TelegramNativeAlertDialog).getActionButton(DialogInterface.BUTTON_POSITIVE)!!.performClick();idle()
        assertEquals(1,AppContentBoundaryFixture.writes.size)
        AppContentBoundaryFixture.writes[0].resume(ApiResponse<Unit>(true));idle();assertFalse(a.isFinishing)
        read(1,draft);assertTrue(a.isFinishing)
    }
    @Test fun restoredEditorCannotAdoptAnotherAccountsDraft()=exercise {a,c->
        read(0);edit(a);val state=android.os.Bundle();c.pause().saveInstanceState(state).stop().destroy()
        ProfileEditorAuthFixture.setOwner("owner-b")
        val restored=Robolectric.buildActivity(EditAboutActivity::class.java,a.intent)
        restored.get().setTheme(R.style.Theme_Eve);restored.setup(state).visible();idle()
        try{assertTrue(restored.get().isFinishing);assertEquals(1,AppContentBoundaryFixture.reads.size);assertEquals(0,AppContentBoundaryFixture.writes.size)}
        finally{restored.pause().stop().destroy();idle()}
    }
    @Test fun backCancelKeepsTheDraftAndBackDiscardLeavesWithoutAWrite()=exercise {a,_->
        read(0);edit(a);a.findViewById<View>(R.id.btnBack).performClick();idle()
        (ShadowDialog.getLatestDialog() as TelegramNativeAlertDialog).getActionButton(DialogInterface.BUTTON_NEUTRAL)!!.performClick();idle()
        assertFalse(a.isFinishing);assertEquals(draft.title,title(a));assertEquals(0,AppContentBoundaryFixture.writes.size)
        a.findViewById<View>(R.id.btnBack).performClick();idle()
        (ShadowDialog.getLatestDialog() as TelegramNativeAlertDialog).getActionButton(DialogInterface.BUTTON_NEGATIVE)!!.performClick();idle()
        assertTrue(a.isFinishing);assertEquals(0,AppContentBoundaryFixture.writes.size)
    }
    @Test fun backDiscardDuringPendingSaveNeverInventsARollbackOrOldSuccess()=exercise {a,_->
        read(0);edit(a);save(a);a.findViewById<View>(R.id.btnBack).performClick();idle()
        (ShadowDialog.getLatestDialog() as TelegramNativeAlertDialog).getActionButton(DialogInterface.BUTTON_NEGATIVE)!!.performClick();idle()
        assertTrue(a.isFinishing)
        AppContentBoundaryFixture.writes[0].resume(ApiResponse<Unit>(true));idle()
        assertEquals(1,AppContentBoundaryFixture.writes.size);assertEquals(1,AppContentBoundaryFixture.reads.size)
        assertFalse(message(a).contains("Saved successfully"))
    }
}
