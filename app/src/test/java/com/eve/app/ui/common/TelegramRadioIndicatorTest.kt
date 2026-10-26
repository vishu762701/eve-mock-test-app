package com.eve.app.ui.common

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Looper
import android.provider.Settings
import android.view.ContextThemeWrapper
import android.widget.FrameLayout
import com.eve.app.R
import com.eve.app.theme.TelegramThemeApplier
import com.eve.app.theme.TelegramThemeEngine
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
@LooperMode(LooperMode.Mode.PAUSED)
class TelegramRadioIndicatorTest {
    @Test fun sourceTransitionCancelsReversesAndRestoresImmediately() {
        val indicator = TelegramRadioIndicator(1f, 24) { true }
        indicator.setChecked(true, true)
        val first = indicator.animator!!
        assertEquals(200L, first.duration)
        first.currentPlayTime = 100L
        assertEquals(0.5f, indicator.progress, 0.01f)
        indicator.setChecked(false, true)
        assertFalse(first.isRunning)
        assertNotSame(first, indicator.animator)
        indicator.jumpToCurrentState()
        assertEquals(0f, indicator.progress, 0f)
        assertNull(indicator.animator)
        indicator.setChecked(true, false)
        assertEquals(1f, indicator.progress, 0f)
        assertNull(indicator.animator)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1))
        assertEquals(1f, indicator.progress, 0f)
        indicator.setChecked(false, true)
        indicator.animator!!.end()
        assertEquals(0f, indicator.progress, 0f)
        assertNull(indicator.animator)
    }

    @Test fun reducedMotionSettlesEvenAnInterruptedTransition() {
        var motion = false
        val indicator = TelegramRadioIndicator(1f, 24) { motion }
        indicator.setChecked(true, true)
        assertEquals(1f, indicator.progress, 0f)
        assertNull(indicator.animator)
        motion = true
        indicator.setChecked(false, true)
        val interrupted = indicator.animator!!
        interrupted.currentPlayTime = 100L
        motion = false
        indicator.setChecked(false, true)
        assertFalse(interrupted.isRunning)
        assertNull(indicator.animator)
        assertEquals(0f, indicator.progress, 0f)
    }

    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Test fun renderingUsesSourceRingDotAndIsolatedClearLayer() {
        val a = TelegramRadioIndicator(1f, 24) { true }
        val b = TelegramRadioIndicator(1f, 24) { true }
        a.setColors(Color.RED, Color.BLUE)
        b.setColors(Color.GREEN, Color.YELLOW)
        a.setBounds(0, 0, 24, 24)
        val bitmap = Bitmap.createBitmap(24, 24, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.MAGENTA)
        a.draw(canvas)
        assertEquals(Color.MAGENTA, bitmap.getPixel(12, 12))
        // Sample the antialiased ring on transparency, so coverage cannot mix the
        // magenta background into the asserted source paint color.
        bitmap.eraseColor(Color.TRANSPARENT)
        a.draw(canvas)
        val edge = bitmap.getPixel(21, 12)
        assertEquals(Color.RED and 0x00ffffff, edge and 0x00ffffff)
        assertTrue("The 2dp source ring must cover its outer pixel", Color.alpha(edge) >= 230)
        assertEquals(Color.TRANSPARENT, bitmap.getPixel(23, 12))
        a.setChecked(true, false)
        a.draw(canvas)
        assertEquals(Color.BLUE, bitmap.getPixel(12, 12))
        assertEquals(0f, b.progress, 0f)
        assertEquals(Color.GREEN, b.uncheckedColor)
        assertEquals(Color.YELLOW, b.checkedColor)
        bitmap.recycle()
    }

    @Test fun nativeWidgetPaletteAndDetachPreserveCheckedTruth() {
        val activity = Robolectric.buildActivity(android.app.Activity::class.java).setup().get()
        val context = ContextThemeWrapper(activity, R.style.Theme_Eve)
        val root = FrameLayout(context)
        activity.setContentView(root)
        val radio = TelegramRadioButton(context)
        val other = TelegramRadioButton(context)
        root.addView(radio)
        root.addView(other)
        radio.isChecked = true
        val palette = TelegramThemeEngine.resolveCurrentPalette(context)
            .copy(textSecondary = Color.RED, onAccent = Color.YELLOW)
        val progress = radio.indicator!!.progress
        TelegramThemeApplier.applyToViewTree(root, palette)
        assertTrue(radio.isChecked)
        assertEquals(progress, radio.indicator!!.progress, 0f)
        assertEquals(Color.RED, radio.indicator!!.uncheckedColor)
        assertEquals(Color.YELLOW, radio.indicator!!.checkedColor)
        assertEquals(Color.YELLOW, radio.currentTextColor)
        TelegramThemeApplier.applyToViewTree(root, palette.copy(textSecondary = Color.BLUE, onAccent = Color.GREEN))
        assertEquals(Color.BLUE, radio.indicator!!.uncheckedColor)
        assertEquals(Color.GREEN, radio.indicator!!.checkedColor)
        assertTrue(radio.isChecked)
        assertEquals(progress, radio.indicator!!.progress, 0f)
        assertNotSame(radio.indicator, other.indicator)
        root.removeView(radio)
        assertEquals(1f, radio.indicator!!.progress, 0f)
        assertNull(radio.indicator!!.animator)
        root.addView(radio)
        assertEquals(1f, radio.indicator!!.progress, 0f)
        Settings.Global.putFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
        try {
            radio.isChecked = false
            assertEquals(0f, radio.indicator!!.progress, 0f)
            assertNull(radio.indicator!!.animator)
        } finally {
            Settings.Global.putFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
            activity.finish()
        }
    }
}
