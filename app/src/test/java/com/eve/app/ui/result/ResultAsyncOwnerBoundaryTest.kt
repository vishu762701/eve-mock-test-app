package com.eve.app.ui.result

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/** Suspended server replies must not mutate a result from a former account. */
class ResultAsyncOwnerBoundaryTest {
    private val src = File("src/main/java/com/eve/app/ui/result/ResultActivity.kt").readText()

    @Test fun optionEnrichmentChecksOriginalOwnerBeforeWritingReviewAnswers() {
        val block = src.substringAfter("if (allItems.any { it.optionA.isBlank() }")
            .substringBefore("if (!currentExamName.isBlank())")
        val read = block.indexOf("val genTest = examRepo.getGeneratedTest(")
        val owner = block.indexOf("if (!resultIdentityCurrent) return@launch")
        val update = block.indexOf("viewModel.updateAnswers(enriched)")
        assertTrue(read >= 0 && owner > read && update > owner)
        assertTrue(block.contains("catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel }"))
    }

    @Test fun scoreFallbackNeverUpdatesAfterOwnerSwitch() {
        val block = src.substringAfter("if (intent.hasExtra(\"EXTRA_SCORE\"))")
            .substringBefore("// Section 1: Overview Breakdown Metrics")
        val read = block.indexOf("val exam = examRepo.getExam(")
        val owner = block.indexOf("if (!resultIdentityCurrent) return@launch", read)
        val score = block.indexOf("currentScore = updatedScore", owner)
        assertTrue(read >= 0 && owner > read && score > owner)
    }

    @Test fun questionStatsCutoffAndLeaderboardUseSameOriginalOwner() {
        val block = src.substringAfter("private fun loadDepthStatsAndCutoffs(")
            .substringBefore("private fun setupCutoffSelector()")
        for (fetch in listOf("getOrFetchQuestionStats(qIds)", "val exam = examRepo.getExam(sourceId)",
                "val statsResp = ApiClient.api.getLeaderboardStats(examId)")) {
            val idx = block.indexOf(fetch)
            val guard = block.indexOf("if (!resultIdentityCurrent) return@launch", idx)
            assertTrue("Missing original-owner guard after: $fetch", idx >= 0 && guard > idx)
        }
        assertTrue(block.contains("if (!resultIdentityCurrent) return@launch\n            setupCutoffSelector()"))
        assertTrue(block.contains("catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel }"))
    }

    @Test fun secondNetworkAwaitInReattemptCannotLaunchAnotherUsersTest() {
        val reset = src.substringAfter("private fun showReattemptDialog(")
        val first = reset.indexOf("resetAttemptPost(mapOf(\"examId\" to examId))")
        val lookup = reset.indexOf("examRepo.getExam(AttemptKey.sourceExamId(examId))", first)
        val guard = reset.indexOf("if (!resultIdentityCurrent) return@launch", lookup)
        val navigate = reset.indexOf("startActivity(testIntent)", guard)
        assertTrue(first >= 0 && lookup > first && guard > lookup && navigate > guard)
    }

    @Test fun oldResultCannotOpenReportOrCategorySheetAfterAccountChange() {
        assertTrue(src.contains("if (resultIdentityCurrent) {\n                ReportQuestionDialog.show("))
        assertTrue(src.contains("if (resultIdentityCurrent) showCategoryBottomSheet()"))
        assertTrue(src.contains("if (resultIdentityCurrent) selectCutoffCategory(item.category)"))
        assertTrue(src.contains("if (!resultIdentityCurrent) return@setOnClickListener"))
        assertTrue(src.contains("if (!resultIdentityCurrent || pos !in allItems.indices) return"))
        assertTrue(src.contains("private fun applyReviewFilters() {\n        if (!resultIdentityCurrent) return"))
    }
}
