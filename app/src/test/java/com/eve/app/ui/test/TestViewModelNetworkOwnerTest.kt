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

    @Test fun attemptEligibilityAwaitCannotPublishToReplacedAccount() {
        val src = File("src/main/java/com/eve/app/ui/test/TestViewModel.kt").readText()
        val start = src.substringAfter("fun start(").substringBefore("fun retry(")
        val initial = start.indexOf("val canAttempt =")
        val initialGuard = start.indexOf("if (!ownsActiveAccount()) return@launch", initial)
        val initialState = start.indexOf("_alreadyAttempted.value = true", initial)
        assertTrue(initial >= 0 && initialGuard > initial && initialState > initialGuard)
        val secondary = start.indexOf("val canStillAttempt =")
        val secondaryGuard = start.indexOf("if (!ownsActiveAccount()) return@launch", secondary)
        val secondaryState = start.indexOf("_alreadyAttempted.value = true", secondary)
        assertTrue(secondary >= 0 && secondaryGuard > secondary && secondaryState > secondaryGuard)
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

    @Test fun unconfirmedAttemptStartsFailClosedBeforeLoadingQuestions() {
        val src = File("src/main/java/com/eve/app/ui/test/TestViewModel.kt").readText()
        val start = src.substringAfter("fun start(").substringBefore("fun retry(")
        val initialNetwork = start.indexOf("val startRes =")
        val initialCheck = start.indexOf("if (!startRes.success || d == null)", initialNetwork)
        val initialError = start.indexOf("_questions.value = UiState.Error", initialCheck)
        val fallback = start.indexOf("val list = serverQuestions ?:", initialNetwork)
        assertTrue(initialCheck in (initialNetwork + 1)..<fallback)
        assertTrue(initialError in (initialCheck + 1)..<fallback)

        val secondNetwork = start.indexOf("val startResponse =")
        val secondCheck = start.indexOf("if (!startResponse.success || session == null)", secondNetwork)
        val secondError = start.indexOf("_questions.value = UiState.Error", secondCheck)
        assertTrue(secondCheck > secondNetwork)
        assertTrue(secondError > secondCheck)
    }

    @Test fun bookmarkOptimisticStateRollsBackOnFailure() {
        val src = File("src/main/java/com/eve/app/ui/test/TestViewModel.kt").readText()
        val toggle = src.substringAfter("fun toggleBookmark(").substringBefore("fun markVisited(")
        val initialOptimistic = toggle.indexOf("bookmarks[position] = !current")
        val repoCall = toggle.indexOf("bookmarkRepo.toggleBookmark(", initialOptimistic)
        val guard = toggle.indexOf("if (!ownsActiveAccount()) return@launch", repoCall)
        val rollback = toggle.indexOf("bookmarks[position] = current", guard)
        val sync = toggle.indexOf("_bookmarksSynced.value = true", rollback)
        assertTrue(initialOptimistic >= 0 && repoCall > initialOptimistic && guard > repoCall &&
            rollback > guard && sync > rollback)
    }

    @Test fun networkExceptionsOnNewAttemptStartsFailClosed() {
        val src = File("src/main/java/com/eve/app/ui/test/TestViewModel.kt").readText()
        val start = src.substringAfter("fun start(").substringBefore("fun retry(")
        val networkCatch = start.substringAfter("catch (e: Exception) {").substringBefore("val list = serverQuestions")
        assertTrue(networkCatch.contains("if (existingSession == null || existingSession.clientAttemptId.isBlank())"))
        assertTrue(networkCatch.contains("started = false"))
        assertTrue(networkCatch.contains("_questions.value = UiState.Error(e.toUserFriendlyMessage())"))
        assertTrue(networkCatch.contains("return@launch"))
        val httpCatch = start.substringAfter("catch (e: retrofit2.HttpException) {").substringBefore("catch (e: kotlinx.coroutines.CancellationException)")
        assertTrue(httpCatch.contains("AttemptLimitManager.canAttempt(com.eve.app.EveApplication.instance, effectiveExamId)"))
    }
}
