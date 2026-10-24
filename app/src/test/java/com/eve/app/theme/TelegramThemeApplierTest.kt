package com.eve.app.theme

import android.graphics.Color
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import org.junit.Assert.assertEquals
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
}
