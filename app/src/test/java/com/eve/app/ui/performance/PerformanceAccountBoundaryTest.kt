package com.eve.app.ui.performance

import com.eve.app.util.SubmissionRetryPolicy
import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PerformanceAccountBoundaryTest {
    @Test fun performanceHostClosesOnAccountSwitchOrForeignRecreation() {
        assertTrue(SubmissionRetryPolicy.ownsSubmission("a", "a"))
        assertFalse(SubmissionRetryPolicy.ownsSubmission("a", "b"))
        assertFalse(SubmissionRetryPolicy.ownsSubmission("a", null))
        val a = File("src/main/java/com/eve/app/ui/performance/PerformanceActivity.kt").readText()
        assertTrue(a.contains("savedInstanceState.getString(\"performance_owner_uid\")"))
        assertTrue(a.contains("outState.putString(\"performance_owner_uid\", performanceOwnerUid)"))
        assertTrue(a.contains("performanceAuth.addAuthStateListener(performanceAuthListener)"))
        assertTrue(a.contains("performanceAuth.removeAuthStateListener(performanceAuthListener)"))
        assertTrue(a.contains("private fun render(state: UiState<PerformanceData>) {\n        if (!hasPerformanceOwner())"))
    }

    @Test fun personalHistoryAndComputedStatsRequireSameOriginalOwner() {
        val vm = File("src/main/java/com/eve/app/ui/performance/PerformanceViewModel.kt").readText()
        assertTrue(vm.contains("private val originalOwnerUid = FirebaseAuth.getInstance().currentUser?.uid"))
        assertTrue(vm.contains("fetchJob?.cancel()"))
        val network = vm.indexOf("val attempts = repo.getAttempts(userId)")
        val guard = vm.indexOf("if (!ownsPerformance()) return@launch", network)
        val aggregation = vm.indexOf("val summary = compute(attempts)", guard)
        val secondGuard = vm.indexOf("if (!ownsPerformance()) return@launch", aggregation)
        val publish = vm.indexOf("_state.value = UiState.Success(summary)", secondGuard)
        assertTrue(network >= 0 && guard > network && aggregation > guard &&
            secondGuard > aggregation && publish > secondGuard)
        assertTrue(vm.contains("catch (cancel: CancellationException)"))
    }
}
