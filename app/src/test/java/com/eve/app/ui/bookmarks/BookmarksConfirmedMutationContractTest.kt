package com.eve.app.ui.bookmarks

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Protects the actual production callback / repository handoff.
 * Real Worker responses and lifecycle/device swipes need integration acceptance.
 */
class BookmarksConfirmedMutationContractTest {
    private val activity = File("src/main/java/com/eve/app/ui/bookmarks/BookmarksActivity.kt").readText()
    private val vm = File("src/main/java/com/eve/app/ui/bookmarks/BookmarksViewModel.kt").readText()

    @Test fun workerDenialCannotRemoveRowOrOfferUndo() {
        val action = activity.substringAfter("onUnbookmark = onUnbookmark@{")
            .substringBefore("isHindi = { LanguageManager.isHindi(this) }")
        val dedup = action.indexOf("pendingRemovalIds.add(id)")
        val network = action.indexOf("viewModel.unbookmark(id)")
        val failure = action.indexOf("if (result.isFailure)")
        val restored = action.indexOf("adapter.notifyItemChanged(failedIndex)")
        val error = action.indexOf("Couldn't remove bookmark. Try again.")
        val undo = action.indexOf("AppUndoBar.show(")
        assertTrue("Prevent duplicate swipes before starting the server request",
            dedup >= 0 && network > dedup)
        assertTrue("A failed API result must rebind the collapsed swipe row and show an error",
            failure > network && restored > failure && error > restored && undo > error)
        assertFalse("Do not remove the row locally before the Worker confirms",
            action.substring(0, undo).contains("adapter.submitList("))
        assertTrue("Always release the pending per-question gate", action.contains("pendingRemovalIds.remove(id)"))
    }

    @Test fun onlyConfirmedUndoShowsRestoreSuccess() {
        val action = activity.substringAfter("onUnbookmark = onUnbookmark@{")
            .substringBefore("isHindi = { LanguageManager.isHindi(this) }")
        val undoCallback = action.substringAfter("onUndo = onUndo@{")
        val restore = undoCallback.indexOf("val restored = viewModel.bookmark(bookmark)")
        val acknowledgment = undoCallback.indexOf("if (restored.isSuccess)")
        val refresh = undoCallback.indexOf("viewModel.load()", acknowledgment)
        val success = undoCallback.indexOf("Bookmark restored", acknowledgment)
        val error = undoCallback.indexOf("Couldn't restore bookmark. Try again.", acknowledgment)
        assertTrue(restore >= 0 && acknowledgment > restore &&
            refresh > acknowledgment && success > acknowledgment && error > success)
        assertTrue("Do not execute Undo for a different signed-in user",
            undoCallback.contains("if (!hasBookmarksOwner() || isFinishing || isDestroyed)"))
    }

    @Test fun sameViewModelReturnsRepositoryAcknowledgmentsInsteadOfFireAndForget() {
        val remove = vm.substringAfter("suspend fun unbookmark(").substringBefore("suspend fun bookmark(")
        val restore = vm.substringAfter("suspend fun bookmark(")
        assertTrue(remove.contains("val result = repository.unbookmarkQuestion(uid, questionId)"))
        assertTrue(remove.contains("return result"))
        assertTrue(restore.contains("val result = repository.bookmarkQuestion("))
        assertTrue(restore.contains("return result"))
        assertTrue(remove.contains("if (!ownsOriginalAccount()) return Result.failure"))
        assertTrue(restore.contains("if (!ownsOriginalAccount()) return Result.failure"))
    }

    @Test fun returningFromQuizRefreshesTheCanonicalServerBackedList() {
        val resume = activity.substringAfter("override fun onResume()")
            .substringBefore("override fun onPause()")
        assertTrue(resume.contains("if (hasResumedOnce) viewModel.load() else hasResumedOnce = true"))
    }
}
