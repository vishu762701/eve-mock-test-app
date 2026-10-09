package com.eve.app.ui

import android.view.View
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.eve.app.data.remote.EveApiService
import com.eve.app.ui.admin.AdminDeleteFlow
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Production dialogs + Retrofit contracts; responses are isolated fixtures. */
@RunWith(AndroidJUnit4::class)
class AdminDeletionRegressionTest {
    private fun waitForDialog(text: String) {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        val deadline = System.currentTimeMillis() + 5000
        while (System.currentTimeMillis() < deadline) {
            if (automation.rootInActiveWindow?.findAccessibilityNodeInfosByText(text)
                    ?.any { it.text?.toString() == text } == true) return
            Thread.sleep(50)
        }
        fail("Dialog did not appear: $text")
    }
    @Test fun bannerRepositoryFailureIsUnwrappedAndNeverAnnouncedAsDeleted() {
        val api = Retrofit.Builder().baseUrl("https://isolated.invalid/")
            .client(OkHttpClient.Builder().addInterceptor { chain ->
                Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(500).message("Fixture")
                    .body("{\"success\":false,\"error\":\"Storage unavailable\"}".toResponseBody("application/json".toMediaType())).build()
            }.build()).addConverterFactory(GsonConverterFactory.create()).build().create(EveApiService::class.java)
        ActivityScenario.launch(RepairVerificationActivity::class.java).use { scenario ->
            val refreshed = AtomicInteger()
            scenario.onActivity { activity ->
                val repository = com.eve.app.data.repository.HomeBannerRepository(api)
                AdminDeleteFlow(activity, api).confirmRemoval("banner:original", "Banner",
                    { repository.deleteBanner("original").getOrThrow() }, { refreshed.incrementAndGet(); Unit })
            }
            waitForDialog("Delete")
            onView(withText("Delete")).inRoot(isDialog()).perform(click())
            val deadline = System.currentTimeMillis() + 5000
            while (refreshed.get() == 0 && System.currentTimeMillis() < deadline) Thread.sleep(50)
            assertEquals(1, refreshed.get())
            waitForDialog("Operation could not be completed")
            onView(withText("Operation could not be completed")).inRoot(isDialog()).check(matches(isDisplayed()))
            onView(withText("Undo")).inRoot(isDialog()).check(doesNotExist())
            onView(withText("OK")).inRoot(isDialog()).perform(click())
        }
    }

    @Test fun confirmedDelete409OfflineAnd500KeepHonestListStateAndNeverCreateUndoReplacement() {
        for (status in listOf(200, 409, 500, -1)) {
            val deletes = AtomicInteger(); val creates = AtomicInteger(); val refreshes = AtomicInteger()
            val releaseDelete = CountDownLatch(1)
            var confirmed = false
            val client = OkHttpClient.Builder().addInterceptor { chain ->
                val request = chain.request()
                if (request.method == "POST") creates.incrementAndGet()
                val deleting = request.method == "DELETE"
                if (deleting) {
                    deletes.incrementAndGet()
                    check(releaseDelete.await(5, TimeUnit.SECONDS)) { "Delete fixture was not released" }
                    if (status == -1) throw IOException("Isolated offline failure")
                    confirmed = status == 200
                }
                val code = if (deleting) status else 200
                val body = if (!deleting) """{"success":true,"data":{"canDelete":true,"attempts":0,"sessions":0,"subExams":0}}"""
                    else if (status == 200) """{"success":true}"""
                    else """{"success":false,"code":"DELETE_BLOCKED_ATTEMPTS","error":"Saved student attempts depend on this record. Unpublish tests instead."}"""
                Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(code).message("Fixture")
                    .header("X-Request-ID", "12345678-1234-1234-1234-123456789abc")
                    .body(body.toResponseBody("application/json".toMediaType())).build()
            }.build()
            val api = Retrofit.Builder().baseUrl("https://isolated.invalid/").client(client)
                .addConverterFactory(GsonConverterFactory.create()).build().create(EveApiService::class.java)
            ActivityScenario.launch(RepairVerificationActivity::class.java).use { scenario ->
                lateinit var flow: AdminDeleteFlow
                lateinit var record: TextView
                val refresh = { refreshes.incrementAndGet(); record.visibility = if (confirmed) View.GONE else View.VISIBLE; Unit }
                scenario.onActivity { activity ->
                    record = TextView(activity).apply { text = "Original exam ID: exam-1" }
                    activity.setContentView(record)
                    flow = AdminDeleteFlow(activity, api)
                    flow.confirm("exam-1", "Exam", true, refresh)
                    flow.confirm("exam-1", "Exam", true, refresh)
                }
                // Retrofit preflight is asynchronous. Do not let Espresso pick the
                // activity root just before the dialog takes its window focus.
                waitForDialog("Delete")
                onView(withText("Delete")).inRoot(isDialog()).check(matches(isDisplayed())).perform(click())
                scenario.onActivity {
                    assertEquals("Keep the record until server confirmation", View.VISIBLE, record.visibility)
                    flow.confirm("exam-1", "Exam", true, refresh)
                }
                releaseDelete.countDown()
                val deadline = System.currentTimeMillis() + 5000
                while (refreshes.get() == 0 && System.currentTimeMillis() < deadline) Thread.sleep(50)
                assertEquals(1, refreshes.get()); assertEquals(1, deletes.get()); assertEquals(0, creates.get())
                scenario.onActivity { assertEquals(if (status == 200) View.GONE else View.VISIBLE, record.visibility) }
                if (status != 200) waitForDialog("Operation could not be completed")
                if (status == 200) onView(withText("Undo")).check(doesNotExist())
                else onView(withText("Undo")).inRoot(isDialog()).check(doesNotExist())
                if (status == 409) {
                    onView(withSubstring("Unpublish tests instead")).inRoot(isDialog()).check(matches(isDisplayed()))
                    onView(withText("Copy request ID")).inRoot(isDialog()).check(matches(isDisplayed()))
                }
                if (status != 200) onView(withText("OK")).inRoot(isDialog()).perform(click())
            }
        }
    }
}
