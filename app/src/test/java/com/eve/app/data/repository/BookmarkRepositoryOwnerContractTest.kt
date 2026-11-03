package com.eve.app.data.repository

import com.eve.app.util.SubmissionRetryPolicy
import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Source-integrated owner checks complement signed-in device testing. */
class BookmarkRepositoryOwnerContractTest {
    @Test fun constructionDoesNotRequireDefaultFirebaseApp() {
        // Regression for QuestionCumulativeTimerTest / TestViewModel() construction.
        assertNotNull(BookmarkRepository())
    }

    @Test fun accountPolicyDoesNotAuthorizeDifferentOrSignedOutUsers() {
        assertTrue(SubmissionRetryPolicy.ownsSubmission("student-A", "student-A"))
        assertFalse(SubmissionRetryPolicy.ownsSubmission("student-A", "student-B"))
        assertFalse(SubmissionRetryPolicy.ownsSubmission("student-A", null))
        assertFalse(SubmissionRetryPolicy.ownsSubmission("", "student-A"))
    }

    @Test fun asynchronousReadsCannotPublishForeignBookmarkRows() {
        val repo = File("src/main/java/com/eve/app/data/repository/BookmarkRepository.kt").readText()
        assertTrue(repo.contains("private var initialOwnerUid: String? = null"))
        assertTrue(repo.contains("@Synchronized\n    private fun accepts(userId: String)"))
        assertTrue(repo.contains("if (initialOwnerUid == null) initialOwnerUid = userId"))
        assertTrue(repo.contains("return initialOwnerUid == userId"))
        assertTrue(repo.contains("SubmissionRetryPolicy.ownsSubmission("))
        assertFalse("Never touch FirebaseAuth during BookmarkRepository initialization",
            repo.contains("private val initialOwnerUid = FirebaseAuth.getInstance()"))
        for (call in listOf("api.getBookmarkIds()", "api.getBookmarks()")) {
            val start = repo.indexOf("val res = $call")
            val guard = repo.indexOf("if (!accepts(userId)) return@flow", start)
            assertTrue("$call must verify session after await", start >= 0 && guard > start)
        }
        assertTrue(repo.contains("if (accepts(userId)) emitAll(_bookmarkIdsFlow)"))
        assertTrue(repo.contains("if (accepts(userId)) emitAll(_bookmarksFlow)"))
    }

    @Test fun mutationResultAndLocalCacheRequireOriginalOwner() {
        val repo = File("src/main/java/com/eve/app/data/repository/BookmarkRepository.kt").readText()
        val add = repo.substringAfter("suspend fun bookmarkQuestion(").substringBefore("suspend fun unbookmarkQuestion(")
        val remove = repo.substringAfter("suspend fun unbookmarkQuestion(").substringBefore("suspend fun toggleBookmark(")
        assertTrue(add.contains("if (!accepts(userId)) return Result.failure"))
        assertTrue(remove.contains("if (!accepts(userId)) return Result.failure"))
        assertTrue(add.indexOf("if (!accepts(userId)) throw IllegalStateException") >
            add.indexOf("api.addBookmark(body)"))
        assertTrue(remove.indexOf("if (!accepts(userId)) throw IllegalStateException") >
            remove.indexOf("api.deleteBookmark(questionId)"))
        assertTrue(add.contains("catch (cancel: CancellationException)"))
        assertTrue(remove.contains("catch (cancel: CancellationException)"))
    }
}
