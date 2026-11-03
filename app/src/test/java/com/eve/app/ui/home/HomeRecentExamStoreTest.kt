package com.eve.app.ui.home

import android.app.Application
import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class HomeRecentExamStoreTest {
    @Test fun recentExamIsPartitionedByUidAndLegacyGlobalValueIsNotAdopted() {
        val context = RuntimeEnvironment.getApplication()
        val prefs = context.getSharedPreferences("eve_app_prefs", Context.MODE_PRIVATE)
        try {
            prefs.edit().putString("last_exam_id", "legacy-exam").commit()
            assertNull(HomeRecentExamStore.load(context, "a"))
            assertNull(HomeRecentExamStore.load(context, null))
            HomeRecentExamStore.save(context, "a", "exam-a")
            HomeRecentExamStore.save(context, "b", "exam-b")
            assertEquals("exam-a", HomeRecentExamStore.load(context, "a"))
            assertEquals("exam-b", HomeRecentExamStore.load(context, "b"))
            assertNull(HomeRecentExamStore.load(context, ""))
            HomeRecentExamStore.save(context, "", "unowned")
            assertNull(HomeRecentExamStore.load(context, ""))
            HomeRecentExamStore.clearDeletedUser(context, "a")
            assertNull(HomeRecentExamStore.load(context, "a"))
            assertEquals("exam-b", HomeRecentExamStore.load(context, "b"))
            assertEquals("legacy-exam", prefs.getString("last_exam_id", null))
        } finally {
            prefs.edit().remove("last_exam_id").remove("last_exam_id_a").remove("last_exam_id_b").apply()
        }
    }
}
