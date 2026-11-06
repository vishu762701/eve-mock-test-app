package com.eve.app.ui.common

import android.graphics.Color
import android.os.Looper
import android.view.LayoutInflater
import androidx.test.core.app.ApplicationProvider
import com.eve.app.R
import com.eve.app.databinding.ItemSentBroadcastBinding
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
@LooperMode(LooperMode.Mode.PAUSED)
class TelegramCheckBoxSquareTest {

    @Test
    fun defaultStateIsUncheckedWithZeroProgress() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val checkBox = TelegramCheckBoxSquare(context)
        assertFalse("Default checkbox must be unchecked", checkBox.isChecked)
        assertEquals("Initial progress must be 0", 0f, checkBox.getProgress(), 0.001f)
    }

    @Test
    fun setCheckedUpdatesStateAndFiresListener() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val checkBox = TelegramCheckBoxSquare(context)
        var callbackFired = false
        var lastState = false
        checkBox.onCheckedChangeListener = TelegramCheckBoxSquare.OnCheckedChangeListener { _, isChecked ->
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
        val checkBox = TelegramCheckBoxSquare(context)
        assertFalse(checkBox.isChecked)

        checkBox.toggle()
        assertTrue(checkBox.isChecked)

        checkBox.toggle()
        assertFalse(checkBox.isChecked)
    }

    @Test
    fun disabledStatePreventsToggle() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val checkBox = TelegramCheckBoxSquare(context)
        checkBox.setDisabled(true)

        checkBox.toggle()
        assertFalse("Disabled checkbox must not toggle", checkBox.isChecked)
    }

    @Test
    fun customColorsAreStoredAndApplied() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val checkBox = TelegramCheckBoxSquare(context)
        checkBox.setColors(Color.GRAY, Color.BLUE, Color.WHITE)

        assertEquals(Color.GRAY, checkBox.colorUnchecked)
        assertEquals(Color.BLUE, checkBox.colorBackground)
        assertEquals(Color.WHITE, checkBox.colorCheck)
    }

    @Test
    fun inflationInSentBroadcastCardResolvesAsTelegramCheckBoxSquare() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val inflater = LayoutInflater.from(context)
        val binding = ItemSentBroadcastBinding.inflate(inflater, null, false)

        assertNotNull("cbSelect must be inflated", binding.cbSelect)
        assertTrue("cbSelect must be TelegramCheckBoxSquare", binding.cbSelect is TelegramCheckBoxSquare)
        assertFalse(binding.cbSelect.isChecked)

        binding.cbSelect.isChecked = true
        assertTrue(binding.cbSelect.isChecked)
    }
}
