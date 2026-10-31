package com.eve.app.ui.profile

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ProfileCategorySourceContractTest {
    @Test fun profileUsesSettingsRadioCellsNotExamAnswerTiles() {
        val xml = File("src/main/res/layout/activity_profile.xml").readText()
        val row = File("src/main/java/com/eve/app/ui/profile/TelegramProfileRadioCell.kt").readText()
        assertFalse(xml.contains("com.eve.app.ui.common.TelegramRadioButton"))
        assertTrue(Regex("com.eve.app.ui.profile.TelegramProfileRadioCell").findAll(xml).count() == 4)
        assertTrue(row.contains("minimumHeight = dp(50)"))
        assertTrue(row.contains("maxLines = 2"))
        assertTrue(xml.contains("android:layout_height=\"wrap_content\""))
        assertTrue(row.contains("TelegramRadioIndicator"))
        assertTrue(row.contains("info.isCheckable = true"))
        assertTrue(row.contains("info.isChecked = isChecked"))
    }

    @Test fun selectionOnlyChangesAfterConfirmedOwnerReadback() {
        val code = File("src/main/java/com/eve/app/ui/profile/ProfileActivity.kt").readText()
        val selection = code.substringAfter("private fun selectCategory(cat: String)").substringBefore("private fun updateCategoryRadios")
        assertTrue(selection.indexOf("ApiClient.apiService.getProfile()") < selection.indexOf("currentCategory = cat"))
        assertTrue(selection.contains("categorySaveInFlight"))
        assertTrue(selection.contains("ProfileSaveConfirmation.matches("))
        assertTrue(selection.contains("userId, FirebaseAuth.getInstance().currentUser?.uid"))
        assertTrue(selection.contains("confirmed.success, cat, confirmed.data?.category"))
        assertTrue(selection.contains("ignoreCase = true"))
        assertTrue(selection.contains("finally {"))
        assertFalse(selection.contains("currentCategory = cat\n        updateCategoryRadios"))
    }
}
