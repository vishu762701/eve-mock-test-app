package com.eve.app.ui.bookmarks

import com.eve.app.util.SubmissionRetryPolicy
import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BookmarksAccountBoundaryTest {
    @Test fun hostRestorationAndDelayedUndoNeverAdoptAnotherAccount() {
        assertTrue(SubmissionRetryPolicy.ownsSubmission("student-a", "student-a"))
        assertFalse(SubmissionRetryPolicy.ownsSubmission("student-a", "student-b"))
        val host = File("src/main/java/com/eve/app/ui/bookmarks/BookmarksActivity.kt").readText()
        assertTrue(host.contains("savedInstanceState.getString(\"bookmarks_owner_uid\")"))
        assertTrue(host.contains("outState.putString(\"bookmarks_owner_uid\", bookmarksOwnerUid)"))
        assertTrue(host.contains("bookmarksAuth.addAuthStateListener(bookmarksAuthListener)"))
        assertTrue(host.contains("bookmarksAuth.removeAuthStateListener(bookmarksAuthListener)"))
        assertTrue(host.contains("if (!hasBookmarksOwner()) { finish(); return@onUnbookmark }"))
        assertTrue(host.contains("if (!hasBookmarksOwner()) { finish(); return@onUndo }"))
        assertTrue(host.contains("private fun renderState(state: UiState<List<BookmarkedQuestion>>) {\n        if (!hasBookmarksOwner())"))
        assertTrue(host.substringAfter("private fun openQuestionInTest(")
            .contains("if (!hasBookmarksOwner()) { finish(); return }"))
    }

    @Test fun observerAndMutationOwnersAreStableAcrossAsyncBoundaries() {
        val vm = File("src/main/java/com/eve/app/ui/bookmarks/BookmarksViewModel.kt").readText()
        assertTrue(vm.contains("private val originalOwnerUid = FirebaseAuth.getInstance().currentUser?.uid"))
        val obs = vm.substringAfter("repository.observeBookmarkedQuestions(uid)")
            .substringBefore("fun unbookmark(")
        assertTrue(obs.contains("if (!ownsOriginalAccount()) {"))
        assertTrue(obs.contains("observeJob?.cancel()"))
        assertTrue(obs.indexOf("if (!ownsOriginalAccount()) {") <
            obs.indexOf("_state.value = UiState.Success(list)"))
        val unbookmark = vm.substringAfter("fun unbookmark(").substringBefore("fun bookmark(")
        val bookmark = vm.substringAfter("fun bookmark(")
        assertTrue(unbookmark.contains("if (!ownsOriginalAccount()) return"))
        assertTrue(bookmark.contains("if (!ownsOriginalAccount()) return"))
        assertTrue(unbookmark.contains("repository.unbookmarkQuestion(uid, questionId)"))
        assertTrue(bookmark.contains("userId = uid"))
    }
}
