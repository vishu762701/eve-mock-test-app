package com.eve.app.ui.profile

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.eve.app.R
import com.eve.app.databinding.ActivityProfileBinding
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.roundToInt

/** Real-device test source; FAST compiles but does not execute AndroidTest. */
@RunWith(AndroidJUnit4::class)
class ProfileActionsGeometryNativeTest {
    @Test fun telegramDerivedMetricsAndActionsInflateAcrossDayNight() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            for (night in listOf(Configuration.UI_MODE_NIGHT_NO, Configuration.UI_MODE_NIGHT_YES)) {
                val base = instrumentation.targetContext
                val configuration = Configuration(base.resources.configuration).apply {
                    uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or night
                }
                val context = ContextThemeWrapper(base.createConfigurationContext(configuration), R.style.Theme_Eve)
                val b = ActivityProfileBinding.inflate(LayoutInflater.from(context))
                val density = context.resources.displayMetrics.density
                fun dp(n: Float) = (n * density).roundToInt()

                val row = b.layoutActions
                assertEquals(dp(74f), row.layoutParams.height)
                assertEquals(dp(8f), row.paddingTop)
                assertEquals(dp(12f), row.paddingBottom)
                assertEquals(dp(14f), row.paddingLeft)
                assertEquals(dp(14f), row.paddingRight)

                val actions = listOf(b.btnSetPhoto, b.btnEditInfo, b.btnSettings)
                actions.forEach { action ->
                    assertEquals(dp(54f), action.layoutParams.height)
                    assertTrue(action.isClickable)
                    assertTrue(action.isFocusable)
                    assertTrue(action.background is ColorDrawable)
                    assertEquals(Color.TRANSPARENT, (action.background as ColorDrawable).color)
                    val title = action.getChildAt(1) as TextView
                    assertTrue(title.maxLines == 2)
                    assertEquals(11f * context.resources.displayMetrics.scaledDensity, title.textSize, 1f)
                }
                val a = b.btnSetPhoto.layoutParams as LinearLayout.LayoutParams
                val b2 = b.btnEditInfo.layoutParams as LinearLayout.LayoutParams
                val c = b.btnSettings.layoutParams as LinearLayout.LayoutParams
                assertEquals(dp(7f).toFloat(), (a.marginEnd + b2.marginStart).toFloat(), dp(1f).toFloat())
                assertEquals(dp(7f).toFloat(), (b2.marginEnd + c.marginStart).toFloat(), dp(1f).toFloat())
                // Real Canvas-rendered pixels, not only XML dimensions:
                // native hit buttons are transparent and their contents invisible.
                row.measure(
                    View.MeasureSpec.makeMeasureSpec(dp(360f), View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(dp(74f), View.MeasureSpec.EXACTLY)
                )
                row.layout(0, 0, row.measuredWidth, row.measuredHeight)
                fun render(): Bitmap {
                    val bitmap = Bitmap.createBitmap(row.width, row.height, Bitmap.Config.ARGB_8888)
                    row.draw(Canvas(bitmap))
                    return bitmap
                }
                val expanded = render()
                val x = row.paddingLeft + dp(4f)
                val y = dp(14f)
                assertTrue("Source Canvas should paint the first action", Color.alpha(expanded.getPixel(x, y)) > 0)
                // Source ButtonBounce scales the actually painted action,
                // not the transparent native click/accessibility wrapper.
                row.setPressScale(actions[0], 0.96f)
                val pressed = render()
                assertFalse("0.04 source bounce must change real Canvas pixels", expanded.sameAs(pressed))
                assertEquals(1f, actions[0].scaleX, 0.001f)
                pressed.recycle()
                row.setPressScale(actions[0], 1f)
                // Day/Night surface and icon repaint must not be a stale XML bitmap.
                row.setActionColor(Color.BLACK)
                val dark = render()
                row.setActionColor(Color.WHITE)
                val light = render()
                assertFalse("Canvas reacts to the resolved theme surface", dark.sameAs(light))
                dark.recycle()
                light.recycle()
                expanded.recycle()
                ProfileActionsCollapse.applyTo(actions, ProfileActionsCollapse.atScroll(dp(74f).toFloat(), density))
                val collapsed = render()
                assertEquals("Collapsed source Canvas should paint no action", 0, Color.alpha(collapsed.getPixel(x, y)))
                collapsed.recycle()

            }
        }
    }
}
