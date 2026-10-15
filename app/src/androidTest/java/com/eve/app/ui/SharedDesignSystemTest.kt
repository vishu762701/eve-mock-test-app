package com.eve.app.ui

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.RectF
import android.view.ContextThemeWrapper
import android.view.View
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.eve.app.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Native production-style fixtures, intentionally independent of auth and network data. */
@RunWith(AndroidJUnit4::class)
class SharedDesignSystemTest {
    @Test fun sharedComponentsRenderWithReadableRolesAndAdaptiveShapes() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            for (dark in listOf(false, true)) for (high in listOf(false, true)) {
                val config = Configuration(instrumentation.targetContext.resources.configuration).apply {
                    uiMode = uiMode and Configuration.UI_MODE_NIGHT_MASK.inv() or
                        if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
                    fontScale = 1f
                }
                val context = ContextThemeWrapper(instrumentation.targetContext.createConfigurationContext(config), R.style.Theme_Eve)
                if (high) context.theme.applyStyle(R.style.ThemeOverlay_Eve_HighContrast, true)
                val density = context.resources.displayMetrics.density
                fun dp(value: Int) = (value * density + .5f).toInt()
                val root = android.view.LayoutInflater.from(context).inflate(R.layout.shared_design_components, null)
                val secondary = root.findViewById<TextView>(R.id.fixtureSecondary)
                val buttons = listOf(R.id.fixturePrimary, R.id.fixtureRecovery, R.id.fixtureNeutral,
                    R.id.fixtureSoft, R.id.fixtureOutlined).map { root.findViewById<MaterialButton>(it) }
                val disabled = root.findViewById<MaterialButton>(R.id.fixtureDisabled)
                val card = root.findViewById<MaterialCardView>(R.id.fixtureCard)
                val toggle = root.findViewById<com.google.android.material.materialswitch.MaterialSwitch>(R.id.fixtureToggle)
                val thumb = toggle.thumbTintList!!.getColorForState(toggle.drawableState, 0)
                val track = toggle.trackTintList!!.getColorForState(toggle.drawableState, 0)
                assertEquals(context.getColor(R.color.eve_white), thumb)
                assertTrue("The graphical switch handle meets 3:1", ColorUtils.calculateContrast(thumb, track) >= 3.0)
                root.measure(View.MeasureSpec.makeMeasureSpec(dp(393), View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
                root.layout(0, 0, root.measuredWidth, root.measuredHeight)
                for (button in buttons) {
                    val bounds = RectF(0f, 0f, button.width.toFloat(), button.height.toFloat())
                    assertEquals("Capsules follow actual height", button.height / 2f,
                        button.shapeAppearanceModel.topLeftCornerSize.getCornerSize(bounds), 1f)
                    assertTrue(button.height >= dp(48))
                    assertNotNull("Controls retain native pressed feedback", button.rippleColor)
                    assertTrue(android.graphics.Color.alpha(button.rippleColor!!.getColorForState(
                        intArrayOf(android.R.attr.state_enabled, android.R.attr.state_pressed), 0)) > 0)
                    val minContrast = if (button.currentTextColor == context.getColor(R.color.eve_on_primary)) 3.0 else 4.5
                    if (button.currentTextColor == context.getColor(R.color.eve_system_blue)) {
                        // ADR-009 exact-blue exception; all other labels retain their AA gate.
                        assertEquals(context.getColor(R.color.eve_system_blue), button.currentTextColor)
                    } else assertTrue("Enabled control labels meet AA", ColorUtils.calculateContrast(
                        button.currentTextColor, ColorUtils.compositeColors(button.backgroundTintList!!.getColorForState(button.drawableState, 0), context.getColor(R.color.eve_canvas))) >= minContrast)
                }
                assertTrue(ColorUtils.calculateContrast(secondary.currentTextColor, context.getColor(R.color.eve_canvas)) >= 4.5)
                assertEquals(context.getColor(R.color.eve_on_primary), buttons[0].currentTextColor)
                assertEquals(context.getColor(if (high) R.color.eve_hc_primary else R.color.eve_primary), buttons[0].backgroundTintList!!.defaultColor)
                for (button in buttons) {
                    button.isEnabled = false
                    assertEquals(context.getColor(R.color.eve_disabled_text), button.currentTextColor)
                    val fill = ColorUtils.compositeColors(button.backgroundTintList!!.getColorForState(button.drawableState, 0),
                        context.getColor(R.color.eve_canvas))
                    assertTrue("Every disabled button label remains readable", ColorUtils.calculateContrast(button.currentTextColor, fill) >= 4.5)
                    button.isEnabled = true
                }
                val field = root.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.fixtureField)
                val expectedAccent = context.getColor(if (high) R.color.eve_hc_accent else R.color.eve_accent_text)
                assertEquals(expectedAccent, field.hintTextColor!!.defaultColor)
                assertEquals("Focused hint follows the user-selected blue exception",
                    context.getColor(R.color.eve_system_blue), field.hintTextColor!!.defaultColor)
                assertEquals(context.getColor(R.color.eve_card_bg), card.cardBackgroundColor.defaultColor)
                assertEquals(dp(26).toFloat(), card.radius, 1f)
                assertTrue(ColorUtils.calculateContrast(disabled.currentTextColor,
                    disabled.backgroundTintList!!.getColorForState(disabled.drawableState, 0)) >= 4.5)
                val bitmap = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
                try {
                    root.draw(Canvas(bitmap))
                    val values = android.content.ContentValues().apply {
                        put(android.provider.MediaStore.Images.Media.DISPLAY_NAME,
                            "shared-components${if (high) "-high-contrast" else ""}-${if (dark) "dark" else "light"}.png")
                        put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/png")
                        put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, "Pictures/eve-approved-ui")
                    }
                    val resolver = instrumentation.targetContext.contentResolver
                    val uri = resolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)!!
                    resolver.openOutputStream(uri)!!.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                } finally { bitmap.recycle() }
            }
        }
    }
}
