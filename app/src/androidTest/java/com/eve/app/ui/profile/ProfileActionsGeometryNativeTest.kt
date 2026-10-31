package com.eve.app.ui.profile

import android.content.res.Configuration
import android.graphics.drawable.GradientDrawable
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.eve.app.R
import com.eve.app.databinding.ActivityProfileBinding
import org.junit.Assert.assertEquals
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
                val rowParams = row.layoutParams as LinearLayout.LayoutParams
                assertEquals(dp(14f), rowParams.marginStart)
                assertEquals(dp(14f), rowParams.marginEnd)

                val actions = listOf(b.btnSetPhoto, b.btnEditInfo, b.btnSettings)
                actions.forEach { action ->
                    assertEquals(dp(54f), action.layoutParams.height)
                    assertTrue(action.isClickable)
                    assertTrue(action.isFocusable)
                    val surface = action.background as GradientDrawable
                    assertEquals(16f * density, surface.cornerRadius, 1f)
                    val title = action.getChildAt(1) as TextView
                    assertTrue(title.maxLines == 2)
                    assertEquals(11f * context.resources.displayMetrics.scaledDensity, title.textSize, 1f)
                }
                val a = b.btnSetPhoto.layoutParams as LinearLayout.LayoutParams
                val b2 = b.btnEditInfo.layoutParams as LinearLayout.LayoutParams
                val c = b.btnSettings.layoutParams as LinearLayout.LayoutParams
                assertEquals(dp(7f).toFloat(), (a.marginEnd + b2.marginStart).toFloat(), dp(1f).toFloat())
                assertEquals(dp(7f).toFloat(), (b2.marginEnd + c.marginStart).toFloat(), dp(1f).toFloat())
            }
        }
    }
}
