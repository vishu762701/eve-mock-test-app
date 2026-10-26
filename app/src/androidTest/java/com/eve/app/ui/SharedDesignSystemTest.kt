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
                val toggle = root.findViewById<com.eve.app.designsystem.DesignSwitch>(R.id.fixtureToggle)
                root.measure(View.MeasureSpec.makeMeasureSpec(dp(393), View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
                root.layout(0, 0, root.measuredWidth, root.measuredHeight)
                val switchBitmap = Bitmap.createBitmap(toggle.width, toggle.height, Bitmap.Config.ARGB_8888)
                toggle.draw(Canvas(switchBitmap))
                val thumbBounds = toggle.thumbDrawable.bounds
                val trackBounds = toggle.trackDrawable.bounds
                assertEquals(context.getColor(R.color.solid_white),
                    switchBitmap.getPixel(thumbBounds.centerX(), thumbBounds.centerY()))
                assertEquals(context.getColor(R.color.system_green),
                    switchBitmap.getPixel(trackBounds.left + trackBounds.height() / 2, trackBounds.centerY()))
                switchBitmap.recycle()
                for (button in buttons) {
                    val bounds = RectF(0f, 0f, button.width.toFloat(), button.height.toFloat())
                    assertEquals("Capsules follow actual height", button.height / 2f,
                        button.shapeAppearanceModel.topLeftCornerSize.getCornerSize(bounds), 1f)
                    assertTrue(button.height >= dp(48))
                    assertNotNull("Controls retain native pressed feedback", button.rippleColor)
                    assertTrue(android.graphics.Color.alpha(button.rippleColor!!.getColorForState(
                        intArrayOf(android.R.attr.state_enabled, android.R.attr.state_pressed), 0)) > 0)
                    assertTrue("Enabled normal-sized control labels meet 4.5:1, including destructive/semantic labels", ColorUtils.calculateContrast(
                        button.currentTextColor, ColorUtils.compositeColors(button.backgroundTintList!!.getColorForState(button.drawableState, 0), context.getColor(R.color.bg_app))) >= 4.5)
                }
                assertTrue(ColorUtils.calculateContrast(secondary.currentTextColor, context.getColor(R.color.bg_app)) >= 4.5)
                assertEquals(context.getColor(R.color.on_accent), buttons[0].currentTextColor)
                assertEquals(context.getColor(R.color.accent), buttons[0].backgroundTintList!!.defaultColor)
                for (button in buttons) {
                    button.isEnabled = false
                    assertEquals(context.getColor(R.color.text_secondary), button.currentTextColor)
                    val fill = ColorUtils.compositeColors(button.backgroundTintList!!.getColorForState(button.drawableState, 0),
                        context.getColor(R.color.bg_app))
                    assertEquals("Disabled fills keep the locked track token",
                        ColorUtils.compositeColors(context.getColor(R.color.track), context.getColor(R.color.bg_app)), fill)
                    button.isEnabled = true
                }
                val field = root.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.fixtureField)
                assertEquals(context.getColor(R.color.text_secondary), field.hintTextColor!!.defaultColor)
                assertEquals(context.getColor(R.color.surface), card.cardBackgroundColor.defaultColor)
                assertEquals(dp(26).toFloat(), card.radius, 1f)
                assertEquals(context.getColor(R.color.text_secondary), disabled.currentTextColor)
                assertEquals(context.getColor(R.color.track), disabled.backgroundTintList!!.getColorForState(disabled.drawableState, 0))
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
