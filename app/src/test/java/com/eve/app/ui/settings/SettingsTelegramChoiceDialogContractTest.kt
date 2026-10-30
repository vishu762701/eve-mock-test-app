package com.eve.app.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Original Telegram dialog background/typography on scoped EVE choice surfaces. */
class SettingsTelegramChoiceDialogContractTest {
    @Test fun dailyGoalCustomContentAndLanguageChoicesUseNativeTelegramDialog() {
        val settings = File("src/main/java/com/eve/app/ui/settings/SettingsActivity.kt").readText()
        val goal = settings.substringAfter("private fun setupDailyGoalSetting()")
            .substringBefore("private fun setupTargetExamsSetting()")
        val language = settings.substringAfter("private fun setupLanguageSetting()")
            .substringBefore("private fun setupReminderSetting()")
        assertTrue(goal.contains(".setTitle(\"Select Daily Goal\")"))
        assertTrue(goal.contains(".setView(dialogView)"))
        assertTrue(goal.contains("TelegramNativeAlertDialog.Builder(this)"))
        assertTrue(goal.contains("radioGroup.setCardOptions(options, currentGoal.toString())"))
        assertTrue(goal.contains("prefs.edit().putInt(StreakHelper.KEY_DAILY_GOAL, chosenGoal).apply()"))
        assertTrue(goal.contains(".show()"))
        assertTrue(language.contains(".setTitle(\"Select App Language\")"))
        assertTrue(language.contains(".setSingleChoiceItems(options, selectedIdx)"))
        assertTrue(language.contains("AppCompatDelegate.setApplicationLocales(locales)"))
        assertTrue(language.contains("TelegramNativeAlertDialog.Builder(this)"))
        assertTrue(language.contains(".show()"))
        assertTrue(!language.contains(".showTelegramSourceDialog()"))
    }

    @Test fun normalSourceHelperDoesNotChangeDestructiveFillOrCallbacks() {
        val central = File("src/main/java/com/eve/app/designsystem/DesignDialogs.kt").readText()
        val normal = central.substringAfter("fun AlertDialog.Builder.showTelegramSourceDialog()")
        assertTrue(normal.contains("show().also { dialog ->"))
        assertTrue(normal.contains("TelegramDialogSourceBackground.apply(dialog)"))
        assertTrue(!normal.contains("design_destructive_fill"))
        assertTrue(!normal.contains("setPositiveButton"))
        assertEquals(1, Regex("fun AlertDialog.Builder.showTelegramSourceDialog\\(").findAll(central).count())
        val helper = File("src/main/java/com/eve/app/ui/home/TelegramDialogSourceBackground.kt").readText()
        assertTrue(helper.contains("R.drawable.telegram_dialog_popup_fixed_alert4"))
        assertTrue(helper.contains("PorterDuff.Mode.MULTIPLY"))
    }
}
