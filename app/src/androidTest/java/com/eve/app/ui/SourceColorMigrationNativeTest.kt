package com.eve.app.ui

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.graphics.ColorUtils
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.eve.app.R
import com.eve.app.designsystem.DesignSurfaces
import com.eve.app.theme.TelegramThemeApplier
import com.eve.app.theme.TelegramThemeEngine
import com.eve.app.ui.navigation.EveBottomPillBar
import com.google.android.material.card.MaterialCardView
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SourceColorMigrationNativeTest {
    @Test fun representativeScreensUseOriginalDefaultsWithDoubleFontAndRtl() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            val layouts = listOf(R.layout.activity_main, R.layout.activity_profile, R.layout.activity_settings,
                R.layout.activity_test, R.layout.activity_result, R.layout.activity_result_detail,
                R.layout.activity_admin, R.layout.activity_history, R.layout.activity_bookmarks, R.layout.activity_login)
            for (dark in listOf(false, true)) {
                val config = Configuration(instrumentation.targetContext.resources.configuration).apply {
                    uiMode = uiMode and Configuration.UI_MODE_NIGHT_MASK.inv() or
                        if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
                    fontScale = 2f
                    setLayoutDirection(java.util.Locale("ar"))
                }
                val context = ContextThemeWrapper(instrumentation.targetContext.createConfigurationContext(config), R.style.Theme_Eve)
                val palette = TelegramThemeEngine.resolvePalette(context, TelegramThemeEngine.getTheme(if (dark) "night" else "day"), 0)
                assertEquals(if (dark) 0xFF000000.toInt() else 0xFFF1F1F3.toInt(), context.getColor(R.color.bg_app))
                assertEquals(if (dark) 0xFF181819.toInt() else Color.WHITE, context.getColor(R.color.surface))
                assertEquals(palette.windowBackground, context.getColor(R.color.bg_app))
                assertEquals(palette.textSecondary, context.getColor(R.color.text_secondary))
                var cards = 0
                fun check(view: View) {
                    if (view is MaterialCardView && view.tag != "solid_color_card") {
                        assertEquals(palette.surface, view.cardBackgroundColor.defaultColor)
                        cards++
                    }
                    if (view is ViewGroup) for (i in 0 until view.childCount) check(view.getChildAt(i))
                }
                for (layout in layouts) {
                    val root = LayoutInflater.from(context).inflate(layout, FrameLayout(context), false)
                    TelegramThemeApplier.applyToViewTree(root, palette)
                    check(root)
                    assertEquals(View.LAYOUT_DIRECTION_RTL, context.resources.configuration.layoutDirection)
                }
                assertTrue("Representative screens include actual Material surfaces", cards > 0)
                val stableCard = MaterialCardView(context).apply {
                    radius = 0f
                    cardElevation = 0f
                    setCardBackgroundColor(palette.surface)
                }
                stableCard.measure(View.MeasureSpec.makeMeasureSpec(120, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(120, View.MeasureSpec.EXACTLY))
                stableCard.layout(0, 0, 120, 120)
                val bitmap = Bitmap.createBitmap(120, 120, Bitmap.Config.ARGB_8888)
                stableCard.draw(Canvas(bitmap))
                assertEquals("Stable interior pixel off edges/text/photos", palette.surface, bitmap.getPixel(60, 60))
                bitmap.recycle()
            }
        }
    }

    @Test fun modalAndPrimarySurfacesStayDistinctWithoutChangingShape() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            for (dark in listOf(false, true)) {
                val config = Configuration(instrumentation.targetContext.resources.configuration).apply {
                    uiMode = uiMode and Configuration.UI_MODE_NIGHT_MASK.inv() or
                        if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
                }
                val context = ContextThemeWrapper(instrumentation.targetContext.createConfigurationContext(config), R.style.Theme_Eve)
                val primary = context.getDrawable(R.drawable.design_surface_panel) as GradientDrawable
                val dialog = context.getDrawable(R.drawable.design_dialog_surface_panel) as GradientDrawable
                assertEquals(context.getColor(R.color.surface), primary.color!!.defaultColor)
                assertEquals(context.getColor(R.color.dialog_surface), dialog.color!!.defaultColor)
                assertEquals(primary.cornerRadius, dialog.cornerRadius, 0f)
                val sheet = FrameLayout(context)
                DesignSurfaces.sheet(sheet)
                assertEquals(context.getColor(R.color.dialog_surface),
                    (sheet.background as com.google.android.material.shape.MaterialShapeDrawable).fillColor!!.defaultColor)
            }
        }
    }

    @Test fun floatingTabTextAndIconStayAccessibleOverChangingCoatedBackdrops() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            for (dark in listOf(false, true)) {
                val context = ContextThemeWrapper(instrumentation.targetContext, R.style.Theme_Eve)
                val p = TelegramThemeEngine.resolvePalette(context, TelegramThemeEngine.getTheme(if (dark) "night" else "day"), 0)
                val pill = EveBottomPillBar(context)
                pill.updateThemePalette(p)
                pill.selectTab(0, false, false)
                val tab = pill.tabHome
                val labels = mutableListOf<android.widget.TextView>()
                fun find(view: View) {
                    if (view is android.widget.TextView) labels += view
                    if (view is ViewGroup) for (i in 0 until view.childCount) find(view.getChildAt(i))
                }
                find(tab)
                assertTrue(labels.isNotEmpty())
                val coating = TelegramThemeEngine.solveSrcColor(p.surface, p.glassTargetMainTabs, .76f)
                for (backdrop in listOf(Color.BLACK, Color.WHITE, p.surface, p.windowBackground)) {
                    val bg = ColorUtils.compositeColors(TelegramThemeEngine.multAlpha(p.glassTabSelected, .09f),
                        ColorUtils.compositeColors(coating, backdrop))
                    labels.forEach { assertTrue("selected label over changing backdrop", ColorUtils.calculateContrast(it.currentTextColor, bg) >= 4.5) }
                }
            }
        }
    }

    @Test fun semanticStatusTextRetainsMeaningWithAaContrastInBothModes() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            for (dark in listOf(false, true)) {
                val config = Configuration(instrumentation.targetContext.resources.configuration).apply {
                    uiMode = uiMode and Configuration.UI_MODE_NIGHT_MASK.inv() or
                        if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
                }
                val context = instrumentation.targetContext.createConfigurationContext(config)
                for ((text, fill) in listOf(R.color.system_green_text to R.color.system_green_container,
                    R.color.system_red_text to R.color.system_red_container, R.color.system_blue_text to R.color.system_blue_container,
                    R.color.system_orange_text to R.color.system_orange_container)) {
                    for (surface in listOf(R.color.bg_app, R.color.surface, R.color.dialog_surface)) {
                        val bg = context.getColor(surface)
                        for (backing in listOf(bg, ColorUtils.compositeColors(context.getColor(fill), bg)))
                            assertTrue(ColorUtils.calculateContrast(context.getColor(text), backing) >= 4.5)
                    }
                }
            }
        }
    }
}
