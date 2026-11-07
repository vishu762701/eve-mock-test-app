package com.eve.app.ui.common

import android.graphics.Color
import android.view.LayoutInflater
import androidx.test.core.app.ApplicationProvider
import com.eve.app.R
import com.eve.app.databinding.BottomSheetCategoryPickerBinding
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
@LooperMode(LooperMode.Mode.PAUSED)
class TelegramCheckBox2Test {

    @Test
    fun defaultStateIsUncheckedWithZeroProgress() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val checkBox = TelegramCheckBox2(context)
        assertFalse("Default checkbox must be unchecked", checkBox.isChecked)
        assertEquals("Initial progress must be 0", 0f, checkBox.getProgress(), 0.001f)
    }

    @Test
    fun setCheckedUpdatesStateAndFiresListener() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val checkBox = TelegramCheckBox2(context)
        var callbackFired = false
        var lastState = false
        checkBox.onCheckedChangeListener = TelegramCheckBox2.OnCheckedChangeListener { _, isChecked ->
            callbackFired = true
            lastState = isChecked
        }

        checkBox.setChecked(true, animated = false)
        assertTrue(checkBox.isChecked)
        assertEquals(1f, checkBox.getProgress(), 0.001f)
        assertTrue(callbackFired)
        assertTrue(lastState)

        callbackFired = false
        checkBox.setChecked(false, animated = false)
        assertFalse(checkBox.isChecked)
        assertEquals(0f, checkBox.getProgress(), 0.001f)
        assertTrue(callbackFired)
        assertFalse(lastState)
    }

    @Test
    fun toggleFlipsCheckedState() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val checkBox = TelegramCheckBox2(context)
        assertFalse(checkBox.isChecked)

        checkBox.toggle()
        assertTrue(checkBox.isChecked)

        checkBox.toggle()
        assertFalse(checkBox.isChecked)
    }

    @Test
    fun disabledStatePreventsToggle() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val checkBox = TelegramCheckBox2(context)
        checkBox.setDisabled(true)

        checkBox.toggle()
        assertFalse("Disabled checkbox must not toggle", checkBox.isChecked)
    }

    @Test
    fun customColorsAreStoredAndApplied() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val checkBox = TelegramCheckBox2(context)
        checkBox.setColors(Color.GRAY, Color.BLUE, Color.WHITE)

        assertEquals(Color.GRAY, checkBox.colorUnchecked)
        assertEquals(Color.BLUE, checkBox.colorBackground)
        assertEquals(Color.WHITE, checkBox.colorCheck)
    }

    @Test
    fun inflationInBottomSheetCategoryPickerResolvesAsTelegramCheckBox2() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val inflater = LayoutInflater.from(context)
        val binding = BottomSheetCategoryPickerBinding.inflate(inflater, null, false)

        assertNotNull("ivSheetCheckGeneral must be inflated", binding.ivSheetCheckGeneral)
        assertTrue("ivSheetCheckGeneral must be TelegramCheckBox2", binding.ivSheetCheckGeneral is TelegramCheckBox2)
        assertFalse(binding.ivSheetCheckGeneral.isChecked)

        binding.ivSheetCheckGeneral.isChecked = true
        assertTrue(binding.ivSheetCheckGeneral.isChecked)
    }
}
