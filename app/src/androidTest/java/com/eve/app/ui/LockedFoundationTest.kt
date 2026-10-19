package com.eve.app.ui

import android.content.res.Configuration
import android.view.ContextThemeWrapper
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.eve.app.R
import com.eve.app.designsystem.DesignTokens
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Exercise actual theme inheritance and constructor defaults in both themes. */
@RunWith(AndroidJUnit4::class)
class LockedFoundationTest {
    @Test fun defaultWidgetsResolveTheLockedRootInBothThemes() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            for (dark in listOf(false, true)) {
                val config = Configuration(instrumentation.targetContext.resources.configuration).apply {
                    uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                        if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
                    fontScale = 1f
                }
                val context = ContextThemeWrapper(
                    instrumentation.targetContext.createConfigurationContext(config), R.style.Theme_Eve
                )
                val resources = context.resources
                val primary = context.getColor(R.color.text_primary)
                assertEquals(primary, DesignTokens.primary(context))
                assertEquals(context.getColor(R.color.accent), DesignTokens.accent(context))
                assertEquals(context.getColor(R.color.accent), DesignTokens.color(context, com.google.android.material.R.attr.colorPrimary))
                val text = TextView(context)
                assertFalse(text.includeFontPadding)
                assertEquals(primary, text.currentTextColor)
                assertEquals(resources.getDimension(R.dimen.design_type_body), text.textSize, .01f)
                val button = MaterialButton(context)
                assertFalse(button.includeFontPadding)
                assertEquals(context.getColor(R.color.on_accent), button.currentTextColor)
                assertEquals(resources.getDimension(R.dimen.design_type_button), button.textSize, .01f)
                assertEquals(context.getColor(R.color.accent), button.backgroundTintList!!.defaultColor)
                button.isEnabled = false
                assertEquals(context.getColor(R.color.text_secondary), button.currentTextColor)
                assertEquals(context.getColor(R.color.track), button.backgroundTintList!!.getColorForState(button.drawableState, 0))
                val card = MaterialCardView(context)
                assertEquals(resources.getDimension(R.dimen.design_radius_card), card.radius, .01f)
                assertEquals(resources.getDimension(R.dimen.design_card_elevation), card.cardElevation, .01f)
                assertEquals(context.getColor(R.color.surface), card.cardBackgroundColor.defaultColor)
                assertEquals(0, card.strokeWidth)
                assertEquals(resources.getDimensionPixelSize(R.dimen.design_card_padding), card.contentPaddingLeft)
            }
        }
    }
}
