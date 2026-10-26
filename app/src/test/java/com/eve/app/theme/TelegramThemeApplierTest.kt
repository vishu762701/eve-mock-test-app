package com.eve.app.theme

import android.graphics.Color
import android.view.ContextThemeWrapper
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import com.eve.app.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TelegramThemeApplierTest {

    private val testPalette = TelegramThemeEngine.ResolvedPalette(
        windowBackground = 0xFF18222D.toInt(),
        surface = 0xFF212D3B.toInt(),
        surfaceInset = 0xFF1D2733.toInt(),
        textPrimary = 0xFFFFFFFF.toInt(),
        textSecondary = 0xFF7E8B99.toInt(),
        accent = 0xFF64B5EF.toInt(),
        onAccent = 0xFF000000.toInt(),
        hairline = 0x2EFFFFFF,
        glassTargetMainTabs = 0x8C18222D.toInt(),
        glassTargetMainTopPanel = 0x8C212D3B.toInt(),
        glassTabSelected = 0xFF64B5EF.toInt(),
        glassTabSelectedText = 0xFF000000.toInt(),
        glassTabUnselected = 0xFF7E8B99.toInt(),
        actionSelector = 0x1F64B5EF,
        drawerIcon = 0xFF64B5EF.toInt(),
        isDark = true
    )

    @Test
    fun testApplyToViewTreeRecursivelyBindsPaletteAndPreservesInput() {
        val context = RuntimeEnvironment.getApplication()
        val root = LinearLayout(context)

        val nestedLayout = LinearLayout(context)
        val textViewPrimary = TextView(context).apply {
            setTextColor(Color.BLACK)
        }
        val textViewSecondary = TextView(context).apply {
            setTextColor(0xFF6C6C70.toInt())
        }
        nestedLayout.addView(textViewPrimary)
        nestedLayout.addView(textViewSecondary)

        val semanticTextView = TextView(context).apply {
            tag = "status_correct"
            setTextColor(0xFF34C759.toInt())
        }
        val editText = EditText(context).apply {
            setText("Active exam response or user input")
        }

        root.addView(nestedLayout)
        root.addView(semanticTextView)
        root.addView(editText)

        TelegramThemeApplier.applyToViewTree(root, testPalette)

        assertEquals("Text view primary must match textPrimary", testPalette.textPrimary, textViewPrimary.currentTextColor)
        assertEquals("Text view secondary must match textSecondary", testPalette.textSecondary, textViewSecondary.currentTextColor)
        assertEquals("Semantic status text must preserve original semantic green", 0xFF34C759.toInt(), semanticTextView.currentTextColor)
        assertEquals("User input in EditText must remain undisturbed", "Active exam response or user input", editText.text.toString())
        assertEquals("EditText text color must match textPrimary", testPalette.textPrimary, editText.currentTextColor)
        assertEquals("EditText hint color must match textSecondary", testPalette.textSecondary, editText.hintTextColors?.defaultColor)
    }

    @Test
    fun testRepeatedPaletteChangesRetainTextRolesWithoutOverridingCallerColors() {
        val context = RuntimeEnvironment.getApplication()
        val root = LinearLayout(context)
        val primary = TextView(context).apply { setTextColor(Color.BLACK) }
        val secondary = TextView(context).apply { setTextColor(0xFF6C6C70.toInt()) }
        val semantic = TextView(context).apply {
            tag = "status_correct"
            setTextColor(0xFF34C759.toInt())
        }
        root.addView(primary)
        root.addView(secondary)
        root.addView(semantic)

        val dayPalette = testPalette.copy(
            textPrimary = 0xFF1A1D21.toInt(),
            textSecondary = 0xFF707579.toInt(),
            isDark = false
        )
        TelegramThemeApplier.applyToViewTree(root, dayPalette)
        assertEquals(dayPalette.textPrimary, primary.currentTextColor)
        assertEquals(dayPalette.textSecondary, secondary.currentTextColor)

        TelegramThemeApplier.applyToViewTree(root, testPalette)
        assertEquals(testPalette.textPrimary, primary.currentTextColor)
        assertEquals(testPalette.textSecondary, secondary.currentTextColor)

        TelegramThemeApplier.applyToViewTree(root, dayPalette)
        assertEquals(dayPalette.textPrimary, primary.currentTextColor)
        assertEquals(dayPalette.textSecondary, secondary.currentTextColor)
        assertEquals(0xFF34C759.toInt(), semantic.currentTextColor)

        primary.setTextColor(0xFFAA3300.toInt())
        TelegramThemeApplier.applyToViewTree(root, testPalette)
        assertEquals("Caller-owned custom text color must be preserved", 0xFFAA3300.toInt(), primary.currentTextColor)
        assertEquals(testPalette.textSecondary, secondary.currentTextColor)
    }

    @Test
    fun testExemptViewsArePreservedWithoutModification() {
        val context = RuntimeEnvironment.getApplication()
        val exemptLayout = FrameLayout(context).apply {
            tag = "no_theme"
        }
        val childText = TextView(context).apply {
            setTextColor(0xFFFF0000.toInt())
        }
        exemptLayout.addView(childText)

        TelegramThemeApplier.applyToViewTree(exemptLayout, testPalette)

        assertEquals("Exempt container child view must not be recolored", 0xFFFF0000.toInt(), childText.currentTextColor)

        val statusView = TextView(context).apply {
            tag = "status_color"
            setTextColor(0xFF00FF00.toInt())
        }
        TelegramThemeApplier.applyToViewTree(statusView, testPalette)
        assertEquals("status_color tagged view must not be recolored", 0xFF00FF00.toInt(), statusView.currentTextColor)

        val semanticStatusView = TextView(context).apply {
            tag = "semantic_status"
            setTextColor(0xFF0000FF.toInt())
        }
        TelegramThemeApplier.applyToViewTree(semanticStatusView, testPalette)
        assertEquals("semantic_status tagged view must not be recolored", 0xFF0000FF.toInt(), semanticStatusView.currentTextColor)
    }

    @Test
    fun testCategoryChipsDistinctStatefulColors() {
        val themed = ContextThemeWrapper(RuntimeEnvironment.getApplication(), R.style.Theme_Eve)
        val chip = Chip(themed).apply { isCheckable = true }

        // Test in dark mode
        TelegramThemeApplier.applyToViewTree(chip, testPalette)
        val checkedState = intArrayOf(android.R.attr.state_checked)
        val uncheckedState = intArrayOf(-android.R.attr.state_checked)

        assertEquals("Checked background must be accent", testPalette.accent, chip.chipBackgroundColor?.getColorForState(checkedState, 0))
        val expectedDarkUncheckedBg = if (testPalette.isDark) 0x1AFFFFFF else testPalette.surface
        assertEquals("Unchecked background must be source-backed", expectedDarkUncheckedBg, chip.chipBackgroundColor?.getColorForState(uncheckedState, 0))
        assertEquals("Checked text must be onAccent", testPalette.onAccent, chip.textColors.getColorForState(checkedState, 0))
        assertEquals("Unchecked text must be textPrimary", testPalette.textPrimary, chip.textColors.getColorForState(uncheckedState, 0))

        // Dynamic checking without re-applying must change effective colors
        chip.isChecked = true
        assertEquals(testPalette.accent, chip.chipBackgroundColor?.getColorForState(chip.drawableState, 0))
        assertEquals(testPalette.onAccent, chip.textColors.getColorForState(chip.drawableState, 0))

        chip.isChecked = false
        assertEquals(expectedDarkUncheckedBg, chip.chipBackgroundColor?.getColorForState(chip.drawableState, 0))
        assertEquals(testPalette.textPrimary, chip.textColors.getColorForState(chip.drawableState, 0))

        // Test in light mode
        val dayPalette = testPalette.copy(
            windowBackground = 0xFFF1F1F3.toInt(),
            surface = Color.WHITE,
            textPrimary = 0xFF1A1D21.toInt(),
            accent = 0xFF229AF0.toInt(),
            onAccent = Color.BLACK,
            isDark = false
        )
        TelegramThemeApplier.applyToViewTree(chip, dayPalette)
        assertEquals("Checked background in day mode", dayPalette.accent, chip.chipBackgroundColor?.getColorForState(checkedState, 0))
        assertEquals("Unchecked background in day mode", dayPalette.surface, chip.chipBackgroundColor?.getColorForState(uncheckedState, 0))
        assertEquals("Checked text in day mode", dayPalette.onAccent, chip.textColors.getColorForState(checkedState, 0))
        assertEquals("Unchecked text in day mode", dayPalette.textPrimary, chip.textColors.getColorForState(uncheckedState, 0))

        // Contrast compliance
        assertTrue(ColorUtils.calculateContrast(dayPalette.onAccent, dayPalette.accent) >= 4.5)
        assertTrue(ColorUtils.calculateContrast(dayPalette.textPrimary, dayPalette.surface) >= 4.5)
    }

    @Test
    fun testAdminDashboardActionContrast() {
        val themed = ContextThemeWrapper(RuntimeEnvironment.getApplication(), R.style.Theme_Eve)
        val btnAdmin = MaterialButton(themed).apply { id = R.id.btnAdmin }

        // Test in dark mode
        TelegramThemeApplier.applyToViewTree(btnAdmin, testPalette)
        assertEquals("Admin button background is actionSelector", testPalette.actionSelector, btnAdmin.backgroundTintList?.defaultColor)
        val enabledState = intArrayOf(android.R.attr.state_enabled)
        assertEquals("Admin button text is textPrimary", testPalette.textPrimary, btnAdmin.textColors.getColorForState(enabledState, 0))

        val darkBacking = ColorUtils.compositeColors(testPalette.actionSelector, testPalette.windowBackground)
        val darkContrast = ColorUtils.calculateContrast(testPalette.textPrimary, darkBacking)
        assertTrue("Admin button contrast in night mode ($darkContrast >= 4.5)", darkContrast >= 4.5)

        // Test in light mode
        val dayPalette = testPalette.copy(
            windowBackground = 0xFFF1F1F3.toInt(),
            surface = Color.WHITE,
            textPrimary = 0xFF1A1D21.toInt(),
            textSecondary = 0xFF6C6E6F.toInt(),
            actionSelector = 0x121A1D21,
            isDark = false
        )
        TelegramThemeApplier.applyToViewTree(btnAdmin, dayPalette)
        assertEquals("Admin button background in day mode", dayPalette.actionSelector, btnAdmin.backgroundTintList?.defaultColor)
        assertEquals("Admin button text in day mode", dayPalette.textPrimary, btnAdmin.textColors.getColorForState(enabledState, 0))

        val lightBacking = ColorUtils.compositeColors(dayPalette.actionSelector, dayPalette.windowBackground)
        val lightContrast = ColorUtils.calculateContrast(dayPalette.textPrimary, lightBacking)
        assertTrue("Admin button contrast in day mode ($lightContrast >= 4.5)", lightContrast >= 4.5)
    }
}
