package com.eve.app.ui.home

import android.app.Application
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.eve.app.R
import com.eve.app.data.model.AnswerItem
import com.eve.app.data.model.TestAttempt
import com.eve.app.ui.profile.ProfileEditorAuthFixture
import com.eve.app.ui.result.ResultDataHolder
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
 shadows=[ProfileEditorAuthFixture.ShadowAuth::class,AttemptHandoffBoundaryFixture.OfflineApi::class],
 instrumentedPackages=["com.eve.app.data.remote"])
class AttemptAccountIsolationTest {
    private val exam="isolated-exam"
    private val attempt=TestAttempt(id="attempt-a",userId="account-a",examId=exam,answers=listOf(answer()))
    @Before fun before() {AttemptHandoffBoundaryFixture.reset();ProfileEditorAuthFixture.reset();ProfileEditorAuthFixture.setOwner("account-a");ResultDataHolder.clear("account-a",exam);ResultDataHolder.clear("account-b",exam)}
    @After fun after() {AttemptHandoffBoundaryFixture.reset();ResultDataHolder.clear("account-a",exam);ResultDataHolder.clear("account-b",exam);ProfileEditorAuthFixture.reset()}
    @Test fun anotherAccountCannotReadCachedAttempt() {
        HomeViewModel.markAttemptSubmitted(exam,attempt)
        ProfileEditorAuthFixture.setOwner("account-b")
        assertNull(HomeViewModel.getCachedAttempt(exam))
    }
    @Test fun anotherAccountCannotInheritSubmittedFlag() {
        HomeViewModel.markAttemptSubmitted(exam,attempt)
        ProfileEditorAuthFixture.setOwner("account-b")
        assertFalse(HomeViewModel.isAttemptSubmitted(exam))
    }
    @Test fun anotherAccountCannotInheritClearedFlag() {
        HomeViewModel.markAttemptCleared(exam)
        ProfileEditorAuthFixture.setOwner("account-b")
        assertFalse(HomeViewModel.isAttemptCleared(exam))
    }
    @Test fun anotherAccountCannotConsumePendingResultAnswers() {
        ResultDataHolder.setAnswers(listOf(AnswerItem(number=1,questionText="Synthetic question",selected="A",selectedText="A",correct="B",correctText="B")),"account-a",exam)
        ProfileEditorAuthFixture.setOwner("account-b")
        assertTrue(ResultDataHolder.consumeAnswers("account-b",exam).isEmpty())
    }
    @Test fun anotherAccountCannotReadResultDetailAnswers() {
        ResultDataHolder.setDetailItems(listOf(AnswerItem(number=1,questionText="Synthetic question",selected="A",selectedText="A",correct="B",correctText="B")),"account-a",exam)
        ProfileEditorAuthFixture.setOwner("account-b")
        assertTrue(ResultDataHolder.getDetailItems("account-b",exam).isEmpty())
    }
    private fun fallback(current:String,expectedLaunch:Boolean) {
        HomeViewModel.markAttemptSubmitted(exam,attempt)
        ProfileEditorAuthFixture.setOwner(current)
        val c=Robolectric.buildActivity(AppCompatActivity::class.java)
        c.get().setTheme(R.style.Theme_Eve);c.setup().visible()
        try {
            ExamLaunchHelper.openPreviousAttemptResult(c.get(),c.get().lifecycleScope,exam,"Synthetic exam")
            shadowOf(Looper.getMainLooper()).idle()
            val intent=shadowOf(c.get()).nextStartedActivity
            assertEquals(expectedLaunch,intent!=null)
            if(expectedLaunch)assertEquals("com.eve.app.ui.result.ResultActivity",intent.component?.className)
        } finally {c.pause().stop().destroy();shadowOf(Looper.getMainLooper()).idle()}
    }
    @Test fun actualFallbackCannotOpenAnotherAccountsResult()=fallback("account-b",false)
    @Test fun actualSameAccountFallbackStillOpensResult()=fallback("account-a",true)
    @Test
    fun testSubmittedAttemptImmediateTracking() {
        val testExamId = "test_exam_mock_42"
        HomeViewModel.markAttemptCleared(testExamId)
        assertFalse(HomeViewModel.isAttemptSubmitted(testExamId))

        val attempt = TestAttempt(
            id = "att_1",
            userId = "account-a",
            examId = testExamId,
            score = 80.0,
            total = 100,
            correct = 80,
            wrong = 20,
            unattempted = 0
        )

        HomeViewModel.markAttemptSubmitted(testExamId, attempt)
        assertTrue(HomeViewModel.isAttemptSubmitted(testExamId))
        assertEquals(attempt, HomeViewModel.getCachedAttempt(testExamId))

        HomeViewModel.markAttemptCleared(testExamId)
        assertFalse(HomeViewModel.isAttemptSubmitted(testExamId))
        assertNull(HomeViewModel.getCachedAttempt(testExamId))
    }


    private fun answer()=AnswerItem(number=1,questionText="Synthetic question",selected="A",selectedText="A",correct="B",correctText="B")
    @Test fun ownResultConsumesOnceAndKeepsOwnDetailPayload() {
        val items=listOf(answer());assertTrue(ResultDataHolder.setAnswers(items,"account-a",exam))
        assertEquals(items,ResultDataHolder.consumeAnswers("account-a",exam))
        assertTrue(ResultDataHolder.consumeAnswers("account-a",exam).isEmpty())
        assertEquals(items,ResultDataHolder.getDetailItems("account-a",exam))
    }
    @Test fun wrongExamCannotConsumeOrClearCorrectExamPayload() {
        val items=listOf(answer());ResultDataHolder.setAnswers(items,"account-a",exam)
        assertTrue(ResultDataHolder.consumeAnswers("account-a","another-exam").isEmpty())
        ResultDataHolder.clear("account-a","another-exam")
        assertEquals(items,ResultDataHolder.consumeAnswers("account-a",exam))
    }
    @Test fun lateForeignProducerCannotReplaceCurrentPayloadOrAttempt() {
        ProfileEditorAuthFixture.setOwner("account-b")
        val own=listOf(answer().copy(questionText="Current account"))
        assertTrue(ResultDataHolder.setAnswers(own,"account-b",exam))
        assertFalse(ResultDataHolder.setAnswers(listOf(answer()),"account-a",exam))
        assertEquals(own,ResultDataHolder.consumeAnswers("account-b",exam))
        val current=attempt.copy(userId="account-b",id="attempt-b")
        HomeViewModel.markAttemptSubmitted(exam,current)
        HomeViewModel.markAttemptSubmitted(exam,attempt)
        assertEquals(current,HomeViewModel.getCachedAttempt(exam))
        HomeViewModel.markAttemptCleared(exam,"account-a")
        assertTrue(HomeViewModel.isAttemptSubmitted(exam))
    }
    @Test fun oldScreenClearCannotEraseAnotherAccountsNewPayload() {
        ResultDataHolder.setAnswers(listOf(answer()),"account-a",exam)
        ProfileEditorAuthFixture.setOwner("account-b")
        val own=listOf(answer().copy(questionText="Current account"))
        ResultDataHolder.setAnswers(own,"account-b",exam)
        ResultDataHolder.clear("account-a",exam)
        assertEquals(own,ResultDataHolder.consumeAnswers("account-b",exam))
    }
    @Test fun signedOutReadCannotExposeCachedResultsOrFlags() {
        HomeViewModel.markAttemptSubmitted(exam,attempt)
        ResultDataHolder.setAnswers(listOf(answer()),"account-a",exam)
        ProfileEditorAuthFixture.setOwner(null)
        assertNull(HomeViewModel.getCachedAttempt(exam));assertFalse(HomeViewModel.isAttemptSubmitted(exam))
        assertTrue(ResultDataHolder.consumeAnswers("account-a",exam).isEmpty())
    }
    @Test fun resultAndDetailActivitiesRejectForeignOrMissingLaunchOwnerBeforeContent() {
        for(type in listOf(com.eve.app.ui.result.ResultActivity::class.java,com.eve.app.ui.result.ResultDetailActivity::class.java)) {
            for(owner in listOf("account-b","")) {
                val intent=android.content.Intent(org.robolectric.RuntimeEnvironment.getApplication(),type)
                    .putExtra(com.eve.app.util.Constants.EXTRA_EXAM_ID,exam)
                    .putExtra(com.eve.app.util.Constants.EXTRA_RESULT_OWNER_UID,owner)
                val c=Robolectric.buildActivity(type,intent)
                c.get().setTheme(R.style.Theme_Eve);c.setup().visible()
                try {assertTrue(c.get().isFinishing)} finally {c.pause().stop().destroy()}
            }
        }
    }

    @Test fun ownResultSurvivesRecreationAndClosesWhenIdentityChanges() {
        val items=listOf(answer().copy(optionA="A",optionB="B",optionC="C",optionD="D"))
        assertTrue(ResultDataHolder.setAnswers(items,"account-a",exam))
        val intent=android.content.Intent(org.robolectric.RuntimeEnvironment.getApplication(),com.eve.app.ui.result.ResultActivity::class.java)
            .putExtra(com.eve.app.util.Constants.EXTRA_EXAM_ID,exam)
            .putExtra(com.eve.app.util.Constants.EXTRA_RESULT_OWNER_UID,"account-a")
        val c=Robolectric.buildActivity(com.eve.app.ui.result.ResultActivity::class.java,intent)
        c.get().setTheme(R.style.Theme_Eve);c.setup().visible();shadowOf(Looper.getMainLooper()).idle()
        try {
            assertFalse(c.get().isFinishing)
            c.recreate();shadowOf(Looper.getMainLooper()).idle()
            assertFalse(c.get().isFinishing)
            ProfileEditorAuthFixture.setOwner("account-b");shadowOf(Looper.getMainLooper()).idle()
            assertTrue(c.get().isFinishing)
            assertEquals(android.view.View.INVISIBLE,c.get().findViewById<android.view.View>(android.R.id.content).let { (it as android.view.ViewGroup).getChildAt(0).visibility })
        } finally {c.pause().stop().destroy();shadowOf(Looper.getMainLooper()).idle()}
        assertTrue(ProfileEditorAuthFixture.listeners.isEmpty())
    }
    @Test fun ownDetailLaunchCarriesOwnerAndClosesOnIdentityChange() {
        val host=Robolectric.buildActivity(AppCompatActivity::class.java)
        host.get().setTheme(R.style.Theme_Eve);host.setup().visible()
        try {
            com.eve.app.ui.result.ResultDetailActivity.launch(host.get(),"account-a",
                com.eve.app.ui.result.ResultDetailActivity.TYPE_STATISTICS,exam,"Synthetic exam",70.0,100,70,20,10,70.0)
            val intent=requireNotNull(shadowOf(host.get()).nextStartedActivity)
            assertEquals("account-a",intent.getStringExtra(com.eve.app.util.Constants.EXTRA_RESULT_OWNER_UID))
            val c=Robolectric.buildActivity(com.eve.app.ui.result.ResultDetailActivity::class.java,intent)
            c.get().setTheme(R.style.Theme_Eve);c.setup().visible();shadowOf(Looper.getMainLooper()).idle()
            try {
                assertFalse(c.get().isFinishing)
                ProfileEditorAuthFixture.setOwner("account-b");shadowOf(Looper.getMainLooper()).idle()
                assertTrue(c.get().isFinishing)
            } finally {c.pause().stop().destroy();shadowOf(Looper.getMainLooper()).idle()}
        } finally {host.pause().stop().destroy();shadowOf(Looper.getMainLooper()).idle()}
        assertTrue(ProfileEditorAuthFixture.listeners.isEmpty())
    }

    private fun delayedFallback(cancel:Boolean) {
        HomeViewModel.markAttemptSubmitted(exam,attempt)
        AttemptHandoffBoundaryFixture.holdLock=true
        val c=Robolectric.buildActivity(AppCompatActivity::class.java)
        c.get().setTheme(R.style.Theme_Eve);c.setup().visible()
        val loading=mutableListOf<Boolean>()
        try {
            ExamLaunchHelper.openPreviousAttemptResult(c.get(),c.get().lifecycleScope,exam,"Synthetic exam",onLoading={loading.add(it)})
            shadowOf(Looper.getMainLooper()).idle()
            val request=requireNotNull(AttemptHandoffBoundaryFixture.pendingLock)
            if(cancel)request.resumeWith(Result.failure(kotlinx.coroutines.CancellationException("Host stopped")))
            else {
                ProfileEditorAuthFixture.setOwner("account-b")
                HomeViewModel.markAttemptSubmitted(exam,attempt.copy(userId="account-b",id="attempt-b"))
                request.resumeWith(Result.success(com.eve.app.data.remote.ApiResponse<Any>(false)))
            }
            shadowOf(Looper.getMainLooper()).idle()
            assertNull("No stale/cancelled navigation",shadowOf(c.get()).nextStartedActivity)
            assertEquals(listOf(true,false),loading)
        } finally {c.pause().stop().destroy();shadowOf(Looper.getMainLooper()).idle()}
    }
    @Test fun latePreviousResultLookupCannotNavigateReplacementIdentity()=delayedFallback(false)
    @Test fun cancelledLookupCannotBecomeCachedSuccessNavigation()=delayedFallback(true)

}
