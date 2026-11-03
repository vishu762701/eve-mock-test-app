package com.eve.app.ui.admin

import android.app.Activity
import android.app.Application
import android.content.ContentProvider
import android.content.ContentValues
import android.content.Intent
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import com.eve.app.R
import com.eve.app.data.model.Exam
import com.eve.app.data.model.Question
import com.eve.app.data.remote.AdminMeData
import com.eve.app.ui.profile.ProfileEditorAuthFixture
import com.eve.app.util.Constants
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume
import org.junit.Assert.*
import org.junit.Before
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.android.controller.ActivityController
import org.robolectric.shadows.ShadowContentResolver
import org.robolectric.shadows.ShadowDialog

/** Actual registered result callback, fresh gate, provider, parser and native preview. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35],application=Application::class,
 shadows=[ProfileEditorAuthFixture.ShadowAuth::class,EditExamBoundaryFixture.AdminBoundary::class,
 EditExamBoundaryFixture.ExamBoundary::class],instrumentedPackages=["com.eve.app.data.repository"])
class ExamPickerResultTest {
    class ImportProvider:ContentProvider() {
        override fun onCreate()=true
        override fun getType(uri:Uri)="text/csv"
        override fun query(uri:Uri,p:Array<out String>?,s:String?,a:Array<out String>?,o:String?):Cursor {
            queries.incrementAndGet()
            if(throwOnQuery)throw SecurityException("Fixture grant unavailable")
            return MatrixCursor(arrayOf(OpenableColumns.DISPLAY_NAME)).apply { addRow(arrayOf("questions.csv")) }
        }
        override fun openFile(uri:Uri,mode:String):ParcelFileDescriptor? {
            opens.incrementAndGet(); readOnMain=Looper.myLooper()==Looper.getMainLooper()
            if(nullAsset)return null
            blockedRead?.await(3,java.util.concurrent.TimeUnit.SECONDS)
            return ParcelFileDescriptor.open(file,ParcelFileDescriptor.MODE_READ_ONLY).also { lastDescriptor=it }
        }
        override fun insert(u:Uri,v:ContentValues?):Uri?=error("No writes")
        override fun delete(u:Uri,s:String?,a:Array<out String>?)=error("No writes")
        override fun update(u:Uri,v:ContentValues?,s:String?,a:Array<out String>?)=error("No writes")
    }
    companion object {
        private val uri=Uri.parse("content://fixture.import/questions")
        private lateinit var file:File
        private val opens=AtomicInteger(); private val queries=AtomicInteger()
        @Volatile private var throwOnQuery=false
        @Volatile private var readOnMain=false
        @Volatile private var nullAsset=false
        @Volatile private var blockedRead:java.util.concurrent.CountDownLatch?=null
        @Volatile private var lastDescriptor:ParcelFileDescriptor?=null
    }
    @Before fun before() {
        ProfileEditorAuthFixture.reset(); ProfileEditorAuthFixture.setOwner("owner")
        EditExamBoundaryFixture.reset()
        EditExamBoundaryFixture.permissions=AdminMeData(email="owner@example.test",rights=listOf("manage_exams","manage_questions"))
        EditExamBoundaryFixture.exams=listOf(Exam(id="exam",examName="Exam"))
        EditExamBoundaryFixture.questions=emptyList<Question>()
        opens.set(0); queries.set(0); throwOnQuery=false; readOnMain=false; nullAsset=false; blockedRead=null; lastDescriptor=null
        file=File(RuntimeEnvironment.getApplication().cacheDir,"question-picker-fixture.csv")
        file.writeText("questionText,optionA,optionB,optionC,optionD,correctAnswer\nQuestion,A,B,C,D,A\n")
        val provider=Robolectric.buildContentProvider(ImportProvider::class.java).create("fixture.import").get()
        ShadowContentResolver.registerProviderInternal("fixture.import",provider)
    }
    @After fun after() { blockedRead?.countDown(); file.delete(); ProfileEditorAuthFixture.reset(); EditExamBoundaryFixture.reset() }
    private fun idle()=shadowOf(Looper.getMainLooper()).idle()
    private fun open(state:Bundle?=null):ActivityController<EditExamActivity> {
        val intent=Intent(RuntimeEnvironment.getApplication(),EditExamActivity::class.java)
            .putExtra(Constants.EXTRA_EXAM_ID,"exam").putExtra(Constants.EXTRA_EXAM_NAME,"Exam")
        val c=Robolectric.buildActivity(EditExamActivity::class.java,intent); c.get().setTheme(R.style.Theme_Eve)
        if(state==null)c.setup()else c.setup(state)
        c.visible(); idle(); return c
    }
    private fun close(c:ActivityController<EditExamActivity>) { ShadowDialog.getLatestDialog()?.dismiss(); c.pause().stop().destroy(); idle() }
    private fun launch(c:ActivityController<EditExamActivity>):Int {
        c.get().findViewById<android.view.View>(R.id.btnBulkUpload).performClick(); idle()
        val request=requireNotNull(shadowOf(c.get()).nextStartedActivityForResult)
        assertEquals(Intent.ACTION_GET_CONTENT,request.intent.action)
        return request.requestCode
    }
    private fun deliver(c:ActivityController<EditExamActivity>,code:Int) {
        assertTrue(c.get().activityResultRegistry.dispatchResult(code,Activity.RESULT_OK,Intent().setData(uri)))
        idle()
    }
    private fun awaitState(message:String,predicate:()->Boolean) {
        val deadline=System.nanoTime()+3_000_000_000L
        while(!predicate() && System.nanoTime()<deadline) { idle(); Thread.sleep(10) }
        idle(); assertTrue(message,predicate())
    }
    private fun awaitPreview()=awaitState("Expected one native import preview; queries=${queries.get()} opens=${opens.get()} main=$readOnMain") {
        ShadowDialog.getLatestDialog()?.isShowing==true
    }
    @Test fun recreatedPickerResultWaitsForFreshGateInsteadOfBeingDiscarded() {
        val old=open(); val state=Bundle(); val code=launch(old)
        old.saveInstanceState(state); close(old)
        EditExamBoundaryFixture.holdMembership=true
        val current=open(state)
        try {
            deliver(current,code)
            assertEquals(0,opens.get()); assertNull(ShadowDialog.getLatestDialog())
            requireNotNull(EditExamBoundaryFixture.pendingMembership).resume(true); idle()
            awaitPreview(); assertEquals(1,opens.get()); assertEquals(0,EditExamBoundaryFixture.writes)
        } finally { close(current) }
    }
    @Test fun unavailableProviderDoesNotCrashTheRegisteredResultCallback() {
        val c=open(); val code=launch(c); throwOnQuery=true
        try {
            deliver(c,code)
            awaitState("Provider failure must finish the owned read") { queries.get()==1 && c.get().findViewById<android.view.View>(R.id.btnBulkUpload).isEnabled }
            assertEquals(0,opens.get()); assertNull(ShadowDialog.getLatestDialog())
        } finally { close(c) }
    }
    @Test fun actualProviderReadIsNotPerformedOnMain() {
        val c=open(); val code=launch(c)
        try { deliver(c,code); awaitPreview(); assertEquals(1,opens.get()); assertFalse("File reads must not block the UI",readOnMain) }
        finally { close(c) }
    }
    @Test fun providerAssetDescriptorReadsTheSyntheticCsv() {
        RuntimeEnvironment.getApplication().contentResolver.openAssetFileDescriptor(uri,"r",android.os.CancellationSignal())!!.use { descriptor ->
            descriptor.createInputStream().use { stream -> assertTrue(stream.bufferedReader().readText().contains("Question,A,B,C,D,A")) }
        }
    }

    @Test fun freshExamOnlyRoleCannotConsumeAFormerQuestionManagersSelection() {
        val old=open(); val state=Bundle(); val code=launch(old)
        old.saveInstanceState(state); close(old)
        EditExamBoundaryFixture.holdMembership=true
        EditExamBoundaryFixture.permissions=AdminMeData(email="owner@example.test",rights=listOf("manage_exams"))
        val c=open(state)
        try {
            deliver(c,code); requireNotNull(EditExamBoundaryFixture.pendingMembership).resume(true); idle()
            assertEquals(android.view.View.GONE,c.get().findViewById<android.view.View>(R.id.btnBulkUpload).visibility)
            assertEquals(0,queries.get()); assertEquals(0,opens.get()); assertNull(ShadowDialog.getLatestDialog())
        } finally { close(c) }
    }
    @Test fun resultCannotReadForReplacementUidBeforeAuthListenerDelivery() {
        val c=open(); val code=launch(c)
        try {
            val queued=ProfileEditorAuthFixture.listeners.toList(); ProfileEditorAuthFixture.listeners.clear()
            ProfileEditorAuthFixture.setOwner("replacement"); ProfileEditorAuthFixture.listeners.addAll(queued)
            assertFalse(c.get().isFinishing)
            deliver(c,code)
            assertEquals(0,queries.get()); assertEquals(0,opens.get()); assertNull(ShadowDialog.getLatestDialog())
        } finally { close(c) }
    }
    @Test fun duplicateResultsAndResumeDoNotRereadOrStackPreviews() {
        val c=open(); val code=launch(c)
        try {
            deliver(c,code); awaitPreview(); val preview=ShadowDialog.getLatestDialog()
            deliver(c,code); c.pause().resume(); idle()
            assertSame(preview,ShadowDialog.getLatestDialog()); assertEquals(1,opens.get())
        } finally { close(c) }
    }
    @Test fun previewSelectionRestoresWithoutAutomaticallyImporting() {
        val old=open(); val code=launch(old); val state=Bundle()
        deliver(old,code); awaitPreview(); old.saveInstanceState(state); close(old)
        val c=open(state)
        try { awaitPreview(); assertEquals(2,opens.get()); assertEquals(0,EditExamBoundaryFixture.batchWrites) }
        finally { close(c) }
    }
    @Test fun nullAssetFinishesWithNoPreviewOrMutation() {
        val c=open(); val code=launch(c); nullAsset=true
        try {
            deliver(c,code)
            awaitState("Null descriptor must finish the owned read") { opens.get()==1 && c.get().findViewById<android.view.View>(R.id.btnBulkUpload).isEnabled }
            assertNull(ShadowDialog.getLatestDialog()); assertEquals(0,EditExamBoundaryFixture.batchWrites)
        } finally { close(c) }
    }
    @Test fun completedReadWhileStoppedIsReusedOnReturn() {
        val c=open(); val code=launch(c); blockedRead=java.util.concurrent.CountDownLatch(1)
        try {
            deliver(c,code); awaitState("Read must start") { opens.get()==1 }
            c.pause().stop(); blockedRead!!.countDown()
            awaitState("Stopped read must complete without a window") { c.get().findViewById<android.view.View>(R.id.btnBulkUpload).isEnabled }
            assertNull(ShadowDialog.getLatestDialog())
            c.start().resume().visible(); idle(); awaitPreview(); assertEquals(1,opens.get())
        } finally { close(c) }
    }
    @Test fun destroyedHostCancelsReadAndClosesReturnedDescriptor() {
        val c=open(); val code=launch(c); blockedRead=java.util.concurrent.CountDownLatch(1)
        deliver(c,code); awaitState("Read must start") { opens.get()==1 }
        close(c); blockedRead!!.countDown()
        awaitState("Destroyed owner must close its descriptor") {
            val descriptor=lastDescriptor
            descriptor!=null && runCatching { descriptor.fd }.isFailure
        }
        assertTrue(runCatching { requireNotNull(lastDescriptor).fd }.exceptionOrNull() is IllegalStateException)
        assertNull(ShadowDialog.getLatestDialog()); assertEquals(0,EditExamBoundaryFixture.batchWrites)
    }
    @Test fun retainedImportButtonCannotSubmitTwiceBeforeOrAfterConfirmation() {
        val c=open(); val code=launch(c)
        try {
            deliver(c,code); awaitPreview()
            val dialog=ShadowDialog.getLatestDialog() as com.eve.app.designsystem.TelegramNativeAlertDialog
            val button=dialog.getActionButton(android.content.DialogInterface.BUTTON_POSITIVE)!!
            button.performClick(); idle(); button.performClick(); idle()
            assertEquals(1,EditExamBoundaryFixture.batchWrites)
            assertEquals(1,EditExamBoundaryFixture.lastBatch.size)
            assertEquals("exam",EditExamBoundaryFixture.lastBatch.single().examId)
            assertFalse(c.get().findViewById<android.view.View>(R.id.btnBulkUpload).isEnabled)
            requireNotNull(EditExamBoundaryFixture.pendingBatch).resume(1); idle()
            assertTrue(c.get().findViewById<android.view.View>(R.id.btnBulkUpload).isEnabled)
            button.performClick(); idle(); assertEquals(1,EditExamBoundaryFixture.batchWrites)
        } finally { close(c) }
    }
    @Test fun interruptedImportIsNotAutomaticallyReplayedOnRecreation() {
        val old=open(); val code=launch(old); val state=Bundle()
        deliver(old,code); awaitPreview()
        val previousPreview=ShadowDialog.getLatestDialog() as com.eve.app.designsystem.TelegramNativeAlertDialog
        previousPreview.getActionButton(android.content.DialogInterface.BUTTON_POSITIVE)!!.performClick(); idle()
        assertEquals(1,EditExamBoundaryFixture.batchWrites)
        val reply=requireNotNull(EditExamBoundaryFixture.pendingBatch)
        old.saveInstanceState(state); close(old)
        val c=open(state)
        try {
            assertSame(previousPreview,ShadowDialog.getLatestDialog()); assertFalse(previousPreview.isShowing)
            assertEquals(1,opens.get()); assertEquals(1,EditExamBoundaryFixture.batchWrites)
            reply.resume(1); idle(); assertEquals(1,EditExamBoundaryFixture.batchWrites)
        } finally { close(c) }
    }

    @Test fun rapidPickerButtonTapsLaunchOnlyOneExternalActivity() {
        val c=open()
        try {
            val button=c.get().findViewById<android.view.View>(R.id.btnBulkUpload)
            button.performClick(); button.performClick(); idle()
            assertNotNull(shadowOf(c.get()).nextStartedActivityForResult)
            assertNull("An outstanding picker must not be launched twice",shadowOf(c.get()).nextStartedActivityForResult)
        } finally { close(c) }
    }

    @Test fun cancelledPickerReenablesControlWithoutReadingOrImporting() {
        val c=open(); val code=launch(c)
        try {
            assertTrue(c.get().activityResultRegistry.dispatchResult(code,Activity.RESULT_CANCELED,null))
            idle(); assertEquals(0,opens.get()); assertEquals(0,EditExamBoundaryFixture.batchWrites)
            assertTrue(c.get().findViewById<android.view.View>(R.id.btnBulkUpload).isEnabled)
        } finally { close(c) }
    }
    @Test fun lateDuplicateResultCannotReopenADismissedPreview() {
        val c=open(); val code=launch(c)
        try {
            deliver(c,code); awaitPreview(); val preview=ShadowDialog.getLatestDialog()!!
            preview.dismiss(); idle()
            deliver(c,code); c.pause().resume(); idle()
            assertSame(preview,ShadowDialog.getLatestDialog()); assertFalse(preview.isShowing)
            assertEquals(1,opens.get()); assertEquals(0,EditExamBoundaryFixture.batchWrites)
        } finally { close(c) }
    }

}
