package com.eve.app.ui.history

import com.eve.app.util.SubmissionRetryPolicy
import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryAccountBoundaryTest {
    @Test fun originalOwnerRequiredForHistoryAndReviewNavigation() {
        assertTrue(SubmissionRetryPolicy.ownsSubmission("student-a", "student-a"))
        assertFalse(SubmissionRetryPolicy.ownsSubmission("student-a", "student-b"))
        assertFalse(SubmissionRetryPolicy.ownsSubmission("student-a", null))
        val host = File("src/main/java/com/eve/app/ui/history/HistoryActivity.kt").readText()
        assertTrue(host.contains("savedInstanceState.getString(\"history_owner_uid\")"))
        assertTrue(host.contains("outState.putString(\"history_owner_uid\", historyOwnerUid)"))
        assertTrue(host.contains("historyAuth.addAuthStateListener(historyAuthListener)"))
        assertTrue(host.contains("historyAuth.removeAuthStateListener(historyAuthListener)"))
        val view = host.substringAfter("private fun render(").substringBefore("private fun openReview(")
        assertTrue(view.contains("if (!hasHistoryOwner())"))
        val review = host.substringAfter("private fun openReview(")
        assertTrue(review.indexOf("attempt.userId != historyOwnerUid") <
            review.indexOf("ResultDataHolder.setAnswers("))
    }

    @Test fun staleNetworkResultCannotReplaceDifferentUsersHistory() {
        val vm = File("src/main/java/com/eve/app/ui/history/HistoryViewModel.kt").readText()
        assertTrue(vm.contains("private val originalOwnerUid = FirebaseAuth.getInstance().currentUser?.uid"))
        assertTrue(vm.contains("loadJob?.cancel()"))
        val fetch = vm.indexOf("repo.getAttempts(userId)")
        val post = vm.indexOf("if (!ownsOriginalAccount()) return@launch", fetch)
        val publish = vm.indexOf("_state.value = UiState.Success(attempts)")
        assertTrue(fetch > 0 && post > fetch && publish > post)
        assertTrue(vm.substringAfter("catch (e: Exception)").contains(
            "if (!ownsOriginalAccount()) return@launch"))
        assertTrue(vm.contains("catch (cancel: CancellationException)"))
    }
}
