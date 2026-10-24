package com.eve.app.ui

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.eve.app.data.local.PendingSubmission
import com.eve.app.data.local.PendingSubmissionStore
import com.eve.app.data.local.TestSession
import com.eve.app.data.local.TestSessionStore
import com.eve.app.util.AttemptLimitManager
import com.eve.app.util.NotificationStore
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/** Real sandboxed disk-backed cleanup test. No live auth, backend or production records. */
@RunWith(AndroidJUnit4::class)
class AccountLocalCleanupTest {
    @Test fun deletionClearsOnlyTheSpecifiedUid() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val suffix = UUID.randomUUID().toString()
        val deletedUid = "cleanup-a-$suffix"
        val otherUid = "cleanup-b-$suffix"
        val exam = "fixture-$suffix"
        val attempts = PendingSubmissionStore(ctx)
        val sessions = TestSessionStore(ctx)
        val attemptA = "clean-a-$suffix"
        val attemptB = "clean-b-$suffix"
        val itemA = PendingSubmission(clientAttemptId = attemptA, examId = exam,
            examName = "Test fixture", category = "Test", answers = emptyList(), userId = deletedUid)
        val itemB = itemA.copy(clientAttemptId = attemptB, userId = otherUid)
        val prefsA = ctx.getSharedPreferences(NotificationStore.namespaceForUser(deletedUid), Context.MODE_PRIVATE)
        val prefsB = ctx.getSharedPreferences(NotificationStore.namespaceForUser(otherUid), Context.MODE_PRIVATE)
        try {
            attempts.save(itemA)
            attempts.save(itemB)
            sessions.saveSession(TestSession(attemptKey = exam, userId = deletedUid, clientAttemptId = attemptA))
            sessions.saveSession(TestSession(attemptKey = exam, userId = otherUid, clientAttemptId = attemptB))
            AttemptLimitManager.setLocalAttemptCount(ctx, exam, deletedUid, 2)
            AttemptLimitManager.setLocalAttemptCount(ctx, exam, otherUid, 1)
            prefsA.edit().putString("fixture", "private a").commit()
            prefsB.edit().putString("fixture", "private b").commit()

            NotificationStore.clearDeletedUser(ctx, deletedUid)
            AttemptLimitManager.clearLocalForUser(ctx, deletedUid)
            sessions.clearAllForUser(deletedUid)
            attempts.clearAllForUser(deletedUid)

            assertNull(attempts.get(attemptA))
            assertNotNull(attempts.get(attemptB))
            assertNull(sessions.getSession(exam, deletedUid))
            assertNotNull(sessions.getSession(exam, otherUid))
            assertEquals(0, AttemptLimitManager.getLocalAttemptCount(ctx, exam, deletedUid))
            assertEquals(1, AttemptLimitManager.getLocalAttemptCount(ctx, exam, otherUid))
            assertNull(prefsA.getString("fixture", null))
            assertEquals("private b", prefsB.getString("fixture", null))
        } finally {
            attempts.remove(attemptA)
            attempts.remove(attemptB)
            sessions.clearAllForUser(deletedUid)
            sessions.clearAllForUser(otherUid)
            AttemptLimitManager.clearLocalForUser(ctx, deletedUid)
            AttemptLimitManager.clearLocalForUser(ctx, otherUid)
            NotificationStore.clearDeletedUser(ctx, deletedUid)
            NotificationStore.clearDeletedUser(ctx, otherUid)
        }
    }
}
