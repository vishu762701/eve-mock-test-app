package com.eve.app.ui.mistakes

import android.app.Application
import android.content.Context
import com.eve.app.util.SubmissionRetryPolicy
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class MistakesAccountIsolationTest {
    @Test fun learnedPreferencesAreActuallyIsolatedAndLegacyIsUnowned() {
        val first = MistakesPreferenceScope.namespaceForUser("uid-a")
        val second = MistakesPreferenceScope.namespaceForUser("uid-b")
        assertEquals(first, MistakesPreferenceScope.namespaceForUser("uid-a"))
        assertNotEquals(first, second)
        assertTrue(first.startsWith("eve_mistakes_u_v2_"))
        assertFalse(first.contains("uid-a"))
        assertNotEquals("eve_mistakes", first)

        val context = RuntimeEnvironment.getApplication()
        val legacy = context.getSharedPreferences("eve_mistakes", Context.MODE_PRIVATE)
        val a = context.getSharedPreferences(first, Context.MODE_PRIVATE)
        val b = context.getSharedPreferences(second, Context.MODE_PRIVATE)
        try {
            legacy.edit().putStringSet("learned_ids", setOf("legacy-question")).commit()
            a.edit().putStringSet("learned_ids", setOf("student-a-question")).commit()
            assertEquals(setOf("student-a-question"), a.getStringSet("learned_ids", emptySet()))
            assertTrue(b.getStringSet("learned_ids", emptySet()).isNullOrEmpty())
            assertEquals(setOf("legacy-question"), legacy.getStringSet("learned_ids", emptySet()))
        } finally {
            legacy.edit().clear().commit()
            a.edit().clear().commit()
            b.edit().clear().commit()
        }
    }

    @Test fun accountDeletionClearsOnlyOwnerLearnedFlags() {
        val context = RuntimeEnvironment.getApplication()
        val keyA = MistakesPreferenceScope.namespaceForUser("student-a")
        val keyB = MistakesPreferenceScope.namespaceForUser("student-b")
        val a = context.getSharedPreferences(keyA, Context.MODE_PRIVATE)
        val b = context.getSharedPreferences(keyB, Context.MODE_PRIVATE)
        val legacy = context.getSharedPreferences("eve_mistakes", Context.MODE_PRIVATE)
        try {
            a.edit().putStringSet("learned_ids", setOf("a-only")).commit()
            b.edit().putStringSet("learned_ids", setOf("b-only")).commit()
            legacy.edit().putStringSet("learned_ids", setOf("legacy")).commit()
            MistakesPreferenceScope.clearDeletedUser(context, "student-a")
            assertTrue(a.getStringSet("learned_ids", emptySet()).isNullOrEmpty())
            assertEquals(setOf("b-only"), b.getStringSet("learned_ids", emptySet()))
            assertEquals(setOf("legacy"), legacy.getStringSet("learned_ids", emptySet()))
            MistakesPreferenceScope.clearDeletedUser(context, "")
            assertEquals(setOf("b-only"), b.getStringSet("learned_ids", emptySet()))
        } finally {
            a.edit().clear().commit()
            b.edit().clear().commit()
            legacy.edit().clear().commit()
        }
    }

    @Test fun ownerChangeAndRestorationAreRejected() {
        assertTrue(SubmissionRetryPolicy.ownsSubmission("uid-a", "uid-a"))
        assertFalse(SubmissionRetryPolicy.ownsSubmission("uid-a", "uid-b"))
        assertFalse(SubmissionRetryPolicy.ownsSubmission("uid-a", null))
        val src = File("src/main/java/com/eve/app/ui/mistakes/MistakesActivity.kt").readText()
        assertTrue(src.contains("savedInstanceState.getString(\"mistakes_owner_uid\")"))
        assertTrue(src.contains("outState.putString(\"mistakes_owner_uid\", mistakesOwnerUid)"))
        assertTrue(src.contains("mistakesAuth.addAuthStateListener(mistakesAccountListener)"))
        assertTrue(src.contains("mistakesAuth.removeAuthStateListener(mistakesAccountListener)"))
        assertTrue(src.contains("MistakesPreferenceScope.namespaceForUser(requireNotNull(mistakesOwnerUid))"))
        assertFalse(src.contains("getSharedPreferences(\"eve_mistakes\""))
        assertTrue(src.contains("if (!hasMistakesOwner()) { finish(); return }"))
    }

    @Test fun missingOrFailedServerPageCannotPretendAllMistakesAreResolved() {
        val src = File("src/main/java/com/eve/app/ui/mistakes/MistakesActivity.kt").readText()
        val load = src.substringAfter("private fun loadMistakes()")
            .substringBefore("private fun showMistakeReadFailure()")
        val network = load.indexOf("ApiClient.api.getMistakes()")
        val guard = load.indexOf("if (!hasMistakesOwner() || requestUid != mistakesOwnerUid)", network)
        val write = load.indexOf("allMistakes = resp.data", guard)
        assertTrue(network >= 0 && guard > network && write > guard)
        assertTrue(load.contains("if (!resp.success || resp.data == null)"))
        assertTrue(load.contains("throw IllegalStateException"))
        assertTrue(load.contains("catch (cancel: CancellationException)"))
        assertTrue(load.contains("showMistakeReadFailure()"))
        assertFalse(load.contains("allMistakes = emptyList()"))
        val failure = src.substringAfter("private fun showMistakeReadFailure()")
            .substringBefore("private fun updateFilterCounts()")
        assertTrue(failure.contains("mistakeLoadFailed = true"))
        assertTrue(failure.contains("Check your connection and tap to retry"))
        assertTrue(failure.contains("binding.messageGroup.isFocusable = true"))
    }
    @Test fun deletionCleanupIsAfterServerAndFirebaseConfirmation() {
        val settings = File("src/main/java/com/eve/app/ui/settings/SettingsActivity.kt").readText()
        val delete = settings.substringAfter("private fun executeAccountDeletion()")
            .substringBefore("private fun promptReauthentication()")
        val server = delete.indexOf("ApiClient.apiService.deleteAccount()")
        val credential = delete.indexOf("user.delete().await()", server)
        val learnedCleanup = delete.indexOf("MistakesPreferenceScope.clearDeletedUser(", credential)
        val sessionClear = delete.indexOf("SessionManager.clear()", learnedCleanup)
        assertTrue(server >= 0 && credential > server &&
            learnedCleanup > credential && sessionClear > learnedCleanup)
    }

}
