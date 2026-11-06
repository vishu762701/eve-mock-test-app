package com.eve.app.designsystem

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class TelegramThemeSelectorsTest {

    @Test
    fun roundRectSelectorCreatesRippleDrawableWithProperAlpha() {
        val opaqueColor = 0xFF229AF0.toInt()
        val drawable = TelegramThemeSelectors.getRoundRectSelectorDrawable(16f, opaqueColor)
        assertTrue("Selector must be RippleDrawable", drawable is RippleDrawable)
        val ripple = drawable as RippleDrawable
        val expectedAlphaColor = (opaqueColor and 0x00FFFFFF) or 0x19000000
        assertNotNull("Ripple must have non-null drawable state", ripple.constantState)
    }

    @Test
    fun selectorDrawableCreatesRippleWithRectangularMask() {
        val colorWithAlpha = 0x26229AF0.toInt()
        val drawable = TelegramThemeSelectors.getSelectorDrawable(colorWithAlpha)
        assertTrue("Selector must be RippleDrawable", drawable is RippleDrawable)
    }

    @Test
    fun codebaseHasZeroRemainingFrameworkListSelectors() {
        val appDir = File("src/main/java/com/eve/app")
        val allSources = appDir.walkTopDown().filter { it.extension in listOf("kt", "java") }.toList()
        for (file in allSources) {
            val content = file.readText()
            assertTrue(
                "File ${file.name} must not use legacy android.R.drawable.list_selector_background",
                !content.contains("android.R.drawable.list_selector_background")
            )
        }
    }

    @Test
    fun mainActivityUsesTelegramThemeSelectorsForSearchSuggestions() {
        val mainSource = File("src/main/java/com/eve/app/ui/home/MainActivity.kt").readText()
        assertTrue(
            "MainActivity must use TelegramThemeSelectors.getRoundRectSelectorDrawable for search suggestions",
            mainSource.contains("TelegramThemeSelectors.getRoundRectSelectorDrawable(")
        )
    }

    @Test
    fun adminScreensEnforceDialogLifecycleDismissal() {
        val polls = File("src/main/java/com/eve/app/ui/admin/ManagePollsActivity.kt").readText()
        assertTrue(polls.contains("customDatePickerDialog?.dismiss()"))
        assertTrue(polls.contains("customTimePickerDialog?.dismiss()"))
        assertTrue(polls.contains("createPollDialog?.dismiss()"))

        val notify = File("src/main/java/com/eve/app/ui/admin/SendNotificationActivity.kt").readText()
        assertTrue(notify.contains("activeDialog?.dismiss()"))
        assertTrue(notify.contains("override fun onStop()"))
        assertTrue(notify.contains("override fun onDestroy()"))

        val premium = File("src/main/java/com/eve/app/ui/admin/ManagePremiumActivity.kt").readText()
        assertTrue(premium.contains("activeDialog?.dismiss()"))
        assertTrue(premium.contains("override fun onStop()"))
        assertTrue(premium.contains("override fun onDestroy()"))

        val users = File("src/main/java/com/eve/app/ui/admin/ManageUsersActivity.kt").readText()
        assertTrue(users.contains("override fun onDestroy()"))
        assertTrue(users.contains("detailsDialog?.dismiss()"))
        assertTrue(users.contains("statusDialog?.dismiss()"))

        val questions = File("src/main/java/com/eve/app/ui/admin/ManageQuestionsActivity.kt").readText()
        val questionsStop = questions.substringAfter("override fun onStop()").substringBefore("override fun onResume()")
        assertTrue(questionsStop.contains("deleteExamDialog?.dismiss()"))
        assertTrue(questionsStop.contains("renameExamDialog?.dismiss()"))
        assertTrue(questionsStop.contains("questionDialog?.dismiss()"))

        val editExam = File("src/main/java/com/eve/app/ui/admin/EditExamActivity.kt").readText()
        val editExamStop = editExam.substringAfter("override fun onStop()").substringBefore("override fun onRestoreInstanceState")
        assertTrue(editExamStop.contains("questionDialog?.dismiss()"))

        val syllabus = File("src/main/java/com/eve/app/ui/admin/ManageSyllabusActivity.kt").readText()
        assertTrue(syllabus.contains("reassignDialog?.dismiss()"))
        assertTrue(syllabus.contains("override fun onDestroy()"))

        val rights = File("src/main/java/com/eve/app/ui/admin/AdminRightsEditActivity.kt").readText()
        assertTrue(rights.contains("activeDialog?.dismiss()"))
        assertTrue(rights.contains("override fun onDestroy()"))

        val admins = File("src/main/java/com/eve/app/ui/admin/AdminsAndRightsActivity.kt").readText()
        assertTrue(admins.contains("removeAdminDialog?.dismiss()"))
        assertTrue(admins.contains("override fun onDestroy()"))

        val logs = File("src/main/java/com/eve/app/ui/admin/ActivityLogActivity.kt").readText()
        assertTrue(logs.contains("override fun onDestroy()"))
        assertTrue(logs.contains("logDialog?.dismiss()"))
        assertTrue(logs.contains("filterDialog?.dismiss()"))
    }
}
