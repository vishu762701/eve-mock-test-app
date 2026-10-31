package com.eve.app.designsystem

import android.os.Parcelable
import android.view.ContextThemeWrapper
import org.robolectric.RuntimeEnvironment
import com.eve.app.R
import com.eve.app.ui.admin.tg.TextCheckCell
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MaintenanceSwitchRegressionTest {
    private fun cell() = TextCheckCell(ContextThemeWrapper(RuntimeEnvironment.getApplication(), R.style.Theme_Eve))

    @Test fun serverBindingAndRollbackNeverInvokeUserListener() {
        val row = cell()
        val events = mutableListOf<Boolean>()
        row.setOnCheckedChangeListener { _, checked -> events.add(checked) }
        row.isChecked = true
        row.isChecked = false
        row.setTextAndCheck("Maintenance", true)
        assertTrue(events.isEmpty())
        row.performClick()
        assertEquals(listOf(false), events)
        // Cancellation rollback must not recursively write or open another dialog.
        row.isChecked = true
        assertEquals(listOf(false), events)
        row.checkBox.performClick()
        assertEquals(listOf(false, false), events)
    }

    @Test fun disabledSwitchRejectsRepeatedClicksAndRestorationIsSilent() {
        val row = cell()
        var calls = 0
        row.setOnCheckedChangeListener { _, _ -> calls++ }
        row.isChecked = true
        val save = TelegramSwitch::class.java.getDeclaredMethod("onSaveInstanceState").apply { isAccessible = true }
        val state = save.invoke(row.checkBox) as Parcelable
        row.isChecked = false
        val restore = TelegramSwitch::class.java.getDeclaredMethod("onRestoreInstanceState", Parcelable::class.java).apply { isAccessible = true }
        restore.invoke(row.checkBox, state)
        assertTrue(row.isChecked)
        assertEquals(0, calls)
        row.isEnabled = false
        repeat(5) { assertFalse(row.checkBox.performClick()) }
        assertTrue(row.isChecked)
        assertEquals(0, calls)
    }
}
