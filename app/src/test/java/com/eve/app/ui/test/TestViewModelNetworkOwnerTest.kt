package com.eve.app.ui.test

import com.eve.app.util.SubmissionRetryPolicy
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

/** Own the existing ViewModel's suspended responses before state publication. */
class TestViewModelNetworkOwnerTest {
    @Test fun currentAccountPredicateIsStrict() {
        assertTrue(SubmissionRetryPolicy.ownsSubmission("a", "a"))
        assertFalse(SubmissionRetryPolicy.ownsSubmission("a", "b"))
        assertFalse(SubmissionRetryPolicy.ownsSubmission("a", null))
        assertFalse(SubmissionRetryPolicy.ownsSubmission("", "a"))
    }

    @Test fun suspendedAttemptStartsAreCheckedBeforeMutatingSession() {
        val src = File("src/main/java/com/eve/app/ui/test/TestViewModel.kt").readText()
        val start = src.substringAfter("fun start(").substringBefore("fun retry(")
        val initialNetwork = start.indexOf("val startRes =")
        val initialGuard = start.indexOf("if (!ownsActiveAccount()) return@launch", initialNetwork)
        val initialMutation = start.indexOf("if (d.alreadySubmitted)", initialGuard)
        assertTrue(initialNetwork >= 0 && initialGuard > initialNetwork && initialMutation > initialGuard)
        val secondNetwork = start.indexOf("val startResponse =")
        val secondGuard = start.indexOf("if (!ownsActiveAccount()) return@launch", secondNetwork)
        val secondMutation = start.indexOf("if (session.alreadySubmitted)", secondGuard)
        assertTrue(secondNetwork > initialNetwork && secondGuard > secondNetwork && secondMutation > secondGuard)
    }

    @Test fun questionPublicationAndFailureCannotReachNewAccount() {
        val src = File("src/main/java/com/eve/app/ui/test/TestViewModel.kt").readText()
        val start = src.substringAfter("fun start(").substringBefore("fun retry(")
        val questions = start.indexOf("_questions.value = UiState.Success(list)")
        val guard = start.lastIndexOf("if (!ownsActiveAccount()) return@launch", questions)
        assertTrue(questions > guard && guard > 0)
        assertTrue(start.substringAfter("catch (e: Exception) {").contains(
            "if (!ownsActiveAccount()) return@launch"))
        assertTrue(start.contains("if (!ownsActiveAccount()) return@collect"))
        val helper = src.substringAfter("private fun ownsActiveAccount()")
            .substringBefore("var currentExamId")
        assertTrue(helper.contains("SubmissionRetryPolicy.ownsSubmission("))
    }
}
