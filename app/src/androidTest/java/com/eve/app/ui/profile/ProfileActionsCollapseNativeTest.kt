package com.eve.app.ui.profile

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.view.View
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.eve.app.R
import com.eve.app.databinding.ActivityProfileBinding
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfileActionsCollapseNativeTest {
    @Test fun dayAndNightActionCellsUseSeparateSourceAlphaStagesAndTouchGate() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            for (night in listOf(Configuration.UI_MODE_NIGHT_NO, Configuration.UI_MODE_NIGHT_YES)) {
                val base = instrumentation.targetContext
                val config = Configuration(base.resources.configuration).apply {
                    uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or night
                }
                val context = ContextThemeWrapper(base.createConfigurationContext(config), R.style.Theme_Eve)
                val b = ActivityProfileBinding.inflate(LayoutInflater.from(context))
                val buttons = listOf(b.btnSetPhoto, b.btnEditInfo, b.btnSettings)
                val density = context.resources.displayMetrics.density
                val width = (360f * density).toInt()
                val height = (74f * density).toInt()
                val row = b.layoutActions
                row.measure(
                    View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
                )
                row.layout(0, 0, row.measuredWidth, row.measuredHeight)
                fun paintAlpha(): Int {
                    val bmp = Bitmap.createBitmap(row.width, row.height, Bitmap.Config.ARGB_8888)
                    try {
                        row.draw(Canvas(bmp))
                        return Color.alpha(bmp.getPixel(
                            row.paddingLeft + (10f * density).toInt(),
                            (14f * density).toInt()
                        ))
                    } finally {
                        bmp.recycle()
                    }
                }
                val expandedAlpha = paintAlpha()
                assertTrue("Source Canvas paints opaque expanded action", expandedAlpha > 0)

                val mid = ProfileActionsCollapse.atScroll(20f * density, density)
                ProfileActionsCollapse.applyTo(buttons, mid)
                assertEquals(mid, b.layoutActions.renderedState)
                assertEquals(mid.backgroundAlpha, row.renderedState.backgroundAlpha, 0.0001f)
                val middleAlpha = paintAlpha()
                assertTrue("Actual Canvas alpha must fade while scrolling",
                    middleAlpha > 0 && middleAlpha < expandedAlpha)
                buttons.forEach { button ->
                    assertTrue(button.isEnabled)
                    for (index in 0 until button.childCount) {
                        assertEquals(mid.contentAlpha, button.getChildAt(index).alpha, 0.001f)
                    }
                }

                ProfileActionsCollapse.applyTo(
                    buttons, ProfileActionsCollapse.atScroll(74f * density, density)
                )
                assertEquals(0f, row.renderedState.backgroundAlpha, 0.0001f)
                assertEquals("Source Canvas terminal collapse must be transparent", 0, paintAlpha())
                buttons.forEach { button ->
                    assertFalse(button.isEnabled)
                }

                ProfileActionsCollapse.applyTo(
                    buttons, ProfileActionsCollapse.atScroll(0f, density)
                )
                assertEquals(1f, row.renderedState.contentAlpha, 0.0001f)
                assertEquals(1f, row.renderedState.backgroundAlpha, 0.0001f)
                assertEquals("Reverse scroll must restore real Canvas opacity",
                    expandedAlpha, paintAlpha())
                buttons.forEach { button ->
                    assertTrue(button.isEnabled)
                    for (index in 0 until button.childCount) {
                        assertEquals(1f, button.getChildAt(index).alpha, 0.001f)
                    }
                }
            }
        }
    }
}
