package com.eve.app.ui.settings

import android.app.Application
import android.content.Context
import com.eve.app.ui.home.HomeRecentExamStore
import com.eve.app.ui.home.HomeSearchHistory
import com.eve.app.ui.mistakes.MistakesPreferenceScope
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class DeletedAccountLocalDataCleanupTest {
    @Test fun onlyDeletedUsersLearnedSearchAndRecentExamStateIsCleared() {
        val context = RuntimeEnvironment.getApplication()
        val global = context.getSharedPreferences("eve_app_prefs", Context.MODE_PRIVATE)
        val learnedA = context.getSharedPreferences(MistakesPreferenceScope.namespaceForUser("uid-a"), Context.MODE_PRIVATE)
        val learnedB = context.getSharedPreferences(MistakesPreferenceScope.namespaceForUser("uid-b"), Context.MODE_PRIVATE)
        try {
            HomeRecentExamStore.save(context, "uid-a", "exam-a")
            HomeRecentExamStore.save(context, "uid-b", "exam-b")
            HomeSearchHistory.record(context, "uid-a", "algebra")
            HomeSearchHistory.record(context, "uid-b", "geometry")
            learnedA.edit().putStringSet("learned_ids", setOf("qa")).commit()
            learnedB.edit().putStringSet("learned_ids", setOf("qb")).commit()
            global.edit().putString("last_exam_id", "legacy").commit()

            MistakesPreferenceScope.clearDeletedUser(context, "uid-a")
            HomeRecentExamStore.clearDeletedUser(context, "uid-a")
            HomeSearchHistory.clear(context, "uid-a")

            assertTrue(learnedA.getStringSet("learned_ids", emptySet()).isNullOrEmpty())
            assertEquals(setOf("qb"), learnedB.getStringSet("learned_ids", emptySet()))
            assertNull(HomeRecentExamStore.load(context, "uid-a"))
            assertEquals("exam-b", HomeRecentExamStore.load(context, "uid-b"))
            assertTrue(HomeSearchHistory.load(context, "uid-a").isEmpty())
            assertEquals(listOf("geometry"), HomeSearchHistory.load(context, "uid-b"))
            assertEquals("legacy", global.getString("last_exam_id", null))
        } finally {
            MistakesPreferenceScope.clearDeletedUser(context, "uid-a")
            MistakesPreferenceScope.clearDeletedUser(context, "uid-b")
            HomeRecentExamStore.clearDeletedUser(context, "uid-a")
            HomeRecentExamStore.clearDeletedUser(context, "uid-b")
            HomeSearchHistory.clear(context, "uid-a")
            HomeSearchHistory.clear(context, "uid-b")
            global.edit().remove("last_exam_id").apply()
        }
    }

    @Test fun confirmedDeletionMustCallAllThreeOwnerScopedCleanups() {
        val settings = File("src/main/java/com/eve/app/ui/settings/SettingsActivity.kt").readText()
        val operation = settings.substringAfter("private fun executeAccountDeletion()")
            .substringBefore("private fun promptReauthentication()")
        val server = operation.indexOf("ApiClient.apiService.deleteAccount()")
        val credential = operation.indexOf("user.delete().await()", server)
        val learned = operation.indexOf("MistakesPreferenceScope.clearDeletedUser(", credential)
        val recent = operation.indexOf("HomeRecentExamStore.clearDeletedUser(", learned)
        val search = operation.indexOf("HomeSearchHistory.clear(", recent)
        val global = operation.indexOf("SessionManager.clear()", search)
        assertTrue(server >= 0 && credential > server && learned > credential &&
            recent > learned && search > recent && global > search)
    }
}
