package com.eve.app.theme

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.view.ContextThemeWrapper
import androidx.core.graphics.ColorUtils
import com.eve.app.R
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers
import java.io.File
import java.security.MessageDigest

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class TelegramColorMigrationTest {
    private val context get() = RuntimeEnvironment.getApplication()
    private fun palette(dark: Boolean) = TelegramThemeEngine.resolvePalette(context,
        TelegramThemeEngine.getTheme(if (dark) "night" else "day"), 999) // retired IDs cannot recolor defaults

    @Test fun originalRoleResolutionAndMissingNightGlassKeys() {
        val light = palette(false)
        val night = palette(true)
        assertEquals(0xFFF1F1F3.toInt(), light.windowBackground)
        assertEquals(Color.WHITE, light.surface)
        assertEquals(0xFF1A1D21.toInt(), light.textPrimary)
        assertEquals(0xFF1A91E6.toInt(), light.glassTabSelected)
        assertEquals(0xFF0D7FCF.toInt(), light.glassTabSelectedText)
        assertEquals(0xFF000000.toInt(), night.windowBackground)
        assertEquals(0xFF181819.toInt(), night.surface)
        assertEquals(0xFF1E1E1E.toInt(), night.dialogSurface)
        assertEquals(0xFF232324.toInt(), night.glassTargetMainTabs)
        assertEquals(0xFF229AF0.toInt(), night.glassTabSelected)
        assertEquals(0xFF229AF0.toInt(), night.glassTabSelectedText)
        assertEquals(Color.WHITE, night.glassTabUnselected)
        assertEquals(-15198183, night.surface)
        assertEquals(23, Color.alpha(TelegramThemeEngine.multAlpha(light.glassTabSelected, .09f)))
        // ONE active fallback only; an absent dialogBackground does not inherit an arbitrary canvas.
        assertEquals(Color.WHITE, TelegramColorRoles.resolve(mapOf("windowBackgroundWhite" to Color.RED), "glass_targetMainTabs"))
    }

    @Test fun staticTokensAndRuntimeDefaultsAgreeAcrossBothConfigurations() {
        for (dark in listOf(false, true)) {
            val config = Configuration(context.resources.configuration).apply {
                uiMode = uiMode and Configuration.UI_MODE_NIGHT_MASK.inv() or
                    if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
            }
            val themed = ContextThemeWrapper(context.createConfigurationContext(config), R.style.Theme_Eve)
            val p = palette(dark)
            for ((id, expected) in listOf(R.color.bg_app to p.windowBackground, R.color.surface to p.surface,
                R.color.surface_inset to p.surfaceInset, R.color.text_primary to p.textPrimary,
                R.color.text_secondary to p.textSecondary, R.color.accent to p.accent, R.color.on_accent to p.onAccent,
                R.color.dialog_surface to p.dialogSurface)) assertEquals("resource $id, dark=$dark", expected, themed.getColor(id))
            for (bg in listOf(p.windowBackground, p.surface, p.dialogSurface)) {
                assertTrue(ColorUtils.calculateContrast(ColorUtils.compositeColors(p.textSecondary, bg), bg) >= 4.5)
                assertTrue(ColorUtils.calculateContrast(p.textPrimary, bg) >= 4.5)
            }
            assertTrue(ColorUtils.calculateContrast(p.onAccent, p.accent) >= 4.5)
            val source = TelegramThemeEngine.solveSrcColor(p.surface, p.glassTargetMainTabs, .76f)
            val composited = ColorUtils.compositeColors(source, p.surface)
            for (channel in listOf<(Int)->Int>(Color::red, Color::green, Color::blue))
                assertTrue(kotlin.math.abs(channel(composited) - channel(p.glassTargetMainTabs)) <= 1)
        }
    }

    @Test fun oldSelectionsMigrateAndImportedFilesSurviveColdStartsAndTransactions() {
        val prefs = context.getSharedPreferences(TelegramThemeEngine.PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().putString("day_theme_id", "arctic").putInt("day_accent_id", 9)
            .putString("night_theme_id", "darkblue").putInt("night_accent_id", 13)
            .putString("theme_mode", "SYSTEM").commit()
        fun restart() {
            ReflectionHelpers.setStaticField(TelegramThemeEngine::class.java, "initialized", false)
            TelegramThemeEngine.init(context)
        }
        restart()
        assertEquals("day", TelegramThemeEngine.getDayThemeId())
        assertEquals("night", TelegramThemeEngine.getNightThemeId())
        assertEquals(0, TelegramThemeEngine.getDayAccentId())
        assertEquals(TelegramThemeEngine.ThemeMode.SYSTEM, TelegramThemeEngine.getThemeMode())
        val file = File(context.filesDir, "custom_991234567.attheme")
        val bytes = "windowBackgroundGray=-1\nchat_messagePanelSend=-14509328\n".toByteArray()
        file.writeBytes(bytes)
        try {
            TelegramThemeEngine.registerCustomTheme(context, file)
            TelegramThemeEngine.beginPreview()
            TelegramThemeEngine.setPreviewDayTheme("custom_991234567", 0, context)
            TelegramThemeEngine.applyChanges(context)
            restart()
            assertEquals("custom_991234567", TelegramThemeEngine.getDayThemeId())
            TelegramThemeEngine.beginPreview()
            TelegramThemeEngine.resetPreviewToDefaults(context)
            TelegramThemeEngine.cancelPreview(context)
            assertEquals("custom_991234567", TelegramThemeEngine.getDayThemeId())
            assertArrayEquals(bytes, file.readBytes())
        } finally { file.delete(); prefs.edit().clear().commit(); restart() }
    }

    @Test fun liveBackgroundRolesKeepShapesAndCallerOverridesThroughRepeatedSwitches() {
        val light = palette(false)
        val night = palette(true)
        val themed = ContextThemeWrapper(context, R.style.Theme_Eve)
        val root = android.widget.LinearLayout(themed).apply { setBackgroundColor(light.windowBackground) }
        val card = android.view.View(themed).apply {
            background = android.graphics.drawable.GradientDrawable().apply { cornerRadius = 19f; setColor(light.surface) }
        }
        root.addView(card)
        TelegramThemeApplier.applyToViewTree(root, light)
        TelegramThemeApplier.applyToViewTree(root, night)
        assertEquals(night.windowBackground, (root.background as android.graphics.drawable.ColorDrawable).color)
        assertEquals(night.surface, (card.background as android.graphics.drawable.GradientDrawable).color!!.defaultColor)
        assertEquals(19f, (card.background as android.graphics.drawable.GradientDrawable).cornerRadius, 0f)
        TelegramThemeApplier.applyToViewTree(root, light)
        assertEquals(light.surface, (card.background as android.graphics.drawable.GradientDrawable).color!!.defaultColor)
        card.setBackgroundColor(Color.MAGENTA)
        TelegramThemeApplier.applyToViewTree(root, night)
        assertEquals(Color.MAGENTA, (card.background as android.graphics.drawable.ColorDrawable).color)
    }

    @Test
    @Config(sdk = [24], application = android.app.Application::class)
    fun minimumSupportedApiUsesExplicitNonLiquidCoatingWithoutShaderApis() {
        val themed = ContextThemeWrapper(context, R.style.Theme_Eve)
        val pill = com.eve.app.ui.navigation.EveBottomPillBar(themed)
        assertEquals(4, pill.tabsLayout.childCount)
        val coating = (pill.glassBackgroundView.background as android.graphics.drawable.ColorDrawable).color
        assertEquals(194, Color.alpha(coating))
        assertEquals(null, pill.glassBackgroundView.foreground?.colorFilter)
    }

    @Test fun packagedNightAssetMatchesTheIndependentlyVerifiedSourceFile() {
        val bytes = context.assets.open("themes/night.attheme").use { it.readBytes() }
        val sha = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        assertEquals("c93fb2934d2684888a5d9e72a36361c0789d6bc745db23043da7eb88155c5469", sha)
    }
}
