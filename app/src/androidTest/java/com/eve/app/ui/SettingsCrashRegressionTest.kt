package com.eve.app.ui

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.eve.app.R
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.telegram.messenger.AndroidUtilities
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.Cells.TextCell
import org.telegram.ui.Components.IconBackgroundColors

/**
 * Permanent instrumentation regression test for the SettingsActivity crash
 * (Theme.isCurrentThemeDark() NPE inside TextCell.setTextAndColorfulIcon()
 * and uninitialized AndroidUtilities display metrics).
 */
@RunWith(AndroidJUnit4::class)
class SettingsCrashRegressionTest {

    @Test
    fun themeCurrentThemeAndTextCellColorfulIconDoNotThrowNpe() {
        val context: Context = ApplicationProvider.getApplicationContext()
        AndroidUtilities.checkDisplaySize(context, context.resources.configuration)
        Theme.syncFromEvePalette(context)

        assertNotNull("Theme.getActiveTheme() must not be null after sync", Theme.getActiveTheme())
        // Must not throw NullPointerException
        Theme.isCurrentThemeDark()

        val cell = TextCell(context)
        cell.setTextAndColorfulIcon(
            "Target Exams",
            R.drawable.msg_settings,
            IconBackgroundColors.BLUE_DEEP.bottom,
            false
        )
        cell.setTextAndValueAndColorfulIcon(
            "Passcode Lock",
            "Off",
            false,
            R.drawable.ic_lock_header,
            IconBackgroundColors.GREEN.top,
            IconBackgroundColors.GREEN.bottom,
            true
        )
        assertTrue("AndroidUtilities.density must be positive", AndroidUtilities.density > 0f)
    }
}
