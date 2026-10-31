package com.eve.app.ui.profile

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Source-map contract. Native geometry/visual parity requires separate device evidence. */
class ProfileActionsGeometryContractTest {
    @Test fun profileActionRowUsesVerifiedTelegramStaticMetrics() {
        val xml = File("src/main/res/layout/activity_profile.xml").readText()
        val row = xml.substringAfter("android:id=\"@+id/layoutActions\"").substringBefore("</LinearLayout>")
        assertTrue(row.contains("android:layout_height=\"74dp\""))
        assertTrue(row.contains("android:layout_marginHorizontal=\"14dp\""))
        assertTrue(row.contains("android:paddingTop=\"8dp\""))
        assertTrue(row.contains("android:paddingBottom=\"12dp\""))

        val actions = listOf(
            "btnSetPhoto" to "android:layout_marginEnd=\"3.5dp\"",
            "btnEditInfo" to "android:layout_marginHorizontal=\"3.5dp\"",
            "btnSettings" to "android:layout_marginStart=\"3.5dp\""
        )
        for ((id, spacing) in actions) {
            val item = xml.substringAfter("android:id=\"@+id/$id\"").substringBefore("</LinearLayout>")
            assertTrue("$id uses 54dp", item.contains("android:layout_height=\"54dp\""))
            assertTrue("$id uses source spacing", item.contains(spacing))
            assertTrue("$id has isolated source-radius surface", item.contains("@drawable/telegram_profile_action_surface"))
            assertTrue("$id labels use source text role", item.contains("style=\"@style/TextAppearance.Eve.Telegram.ProfileAction.Source\""))
            assertFalse("$id text size must be defined by its role", item.contains("android:textSize="))
            assertFalse("$id must not use the global pill surface", item.contains("@drawable/design_pill_surface"))
        }
        val typography = File("src/main/res/values/telegram_typography.xml").readText()
        val role = typography.substringAfter("<style name=\"TextAppearance.Eve.Telegram.ProfileAction.Source\"").substringBefore("</style>")
        assertTrue(role.contains("<item name=\"android:textSize\">11sp</item>"))
        val surface = File("src/main/res/drawable/telegram_profile_action_surface.xml").readText()
        assertTrue(surface.contains("android:radius=\"16dp\""))
        assertTrue(surface.contains("@color/category_pill_unselected_bg"))
    }
}
