package com.eve.app.ui.leaderboard

import com.eve.app.util.SubmissionRetryPolicy
import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LeaderboardAccountBoundaryTest {
    @Test fun restoredRankHostCannotAdoptAnotherIdentity() {
        assertTrue(SubmissionRetryPolicy.ownsSubmission("a", "a"))
        assertFalse(SubmissionRetryPolicy.ownsSubmission("a", "b"))
        val host = File("src/main/java/com/eve/app/ui/leaderboard/LeaderboardActivity.kt").readText()
        assertTrue(host.contains("savedInstanceState.getString(\"leaderboard_owner_uid\")"))
        assertTrue(host.contains("outState.putString(\"leaderboard_owner_uid\", leaderboardOwnerUid)"))
        assertTrue(host.contains("leaderboardAuth.addAuthStateListener(leaderboardAuthListener)"))
        assertTrue(host.contains("adapter = LeaderboardAdapter(leaderboardOwnerUid)"))
        assertFalse(host.contains("private val adapter = LeaderboardAdapter(FirebaseAuth.getInstance().currentUser?.uid)"))
        assertTrue(host.contains("leaderboardAuth.removeAuthStateListener(leaderboardAuthListener)"))
        assertTrue(host.contains("private fun renderRank(rankInfo: RankInfo?) {\n        if (!hasLeaderboardOwner())"))
    }

    @Test fun privateRankAfterPublicTopListCannotSwitchOwner() {
        val vm = File("src/main/java/com/eve/app/ui/leaderboard/LeaderboardViewModel.kt").readText()
        assertTrue(vm.contains("private val originalOwnerUid = FirebaseAuth.getInstance().currentUser?.uid"))
        assertTrue(vm.contains("fetchJob?.cancel()"))
        val top = vm.indexOf("repo.getTopScorers(examId)")
        val guardTop = vm.indexOf("if (!ownsLeaderboard()) return@launch", top)
        val topPublish = vm.indexOf("_state.value = UiState.Success(top)", guardTop)
        val rank = vm.indexOf("repo.getUserRank(examId, userId)", topPublish)
        val guardRank = vm.indexOf("if (!ownsLeaderboard()) return@launch", rank)
        val rankPublish = vm.indexOf("_rankInfo.value = rank", guardRank)
        assertTrue(top >= 0 && guardTop > top && topPublish > guardTop)
        assertTrue(rank > topPublish && guardRank > rank && rankPublish > guardRank)
        assertFalse("Rank request must not use current UID captured after first suspension",
            vm.contains("val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch"))
    }
}
