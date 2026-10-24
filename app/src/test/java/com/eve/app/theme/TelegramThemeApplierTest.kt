package com.eve.app.theme

import android.graphics.Color
import android.view.ContextThemeWrapper
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.eve.app.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], theme = R.style.Theme_Eve)
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
        val context = ContextThemeWrapper(RuntimeEnvironment.getApplication(), R.style.Theme_Eve)
        val root = LinearLayout(context)

        val card = MaterialCardView(context).apply {
            strokeWidth = 2
        }
        val button = MaterialButton(context).apply {
            tag = "accent_button"
        }
        val textView = TextView(context).apply {
            setTextColor(Color.BLACK)
        }
        val semanticTextView = TextView(context).apply {
            tag = "status_correct"
            setTextColor(0xFF34C759.toInt())
        }
        val editText = EditText(context).apply {
            setText("Active exam response or user input")
        }

        root.addView(card)
        root.addView(button)
        root.addView(textView)
        root.addView(semanticTextView)
        root.addView(editText)

        TelegramThemeApplier.applyToViewTree(root, testPalette)

        assertEquals("Card surface must match palette", testPalette.surface, card.cardBackgroundColor.defaultColor)
        assertEquals("Card stroke must match hairline", testPalette.hairline, card.strokeColor)
        assertEquals("Accent button background must match palette accent", testPalette.accent, button.backgroundTintList?.defaultColor)
        assertEquals("Accent button text must match onAccent", testPalette.onAccent, button.currentTextColor)
        assertEquals("Text view must match textPrimary", testPalette.textPrimary, textView.currentTextColor)
        assertEquals("Semantic status text must preserve original semantic green", 0xFF34C759.toInt(), semanticTextView.currentTextColor)
        assertEquals("User input in EditText must remain undisturbed", "Active exam response or user input", editText.text.toString())
    }

    @Test
    fun testExemptViewsArePreservedWithoutModification() {
        val context = ContextThemeWrapper(RuntimeEnvironment.getApplication(), R.style.Theme_Eve)
        val exemptLayout = FrameLayout(context).apply {
            tag = "no_theme"
        }
        val childCard = MaterialCardView(context).apply {
            setCardBackgroundColor(0xFFFF0000.toInt())
        }
        exemptLayout.addView(childCard)

        TelegramThemeApplier.applyToViewTree(exemptLayout, testPalette)

        assertEquals("Exempt container child card must not be recolored", 0xFFFF0000.toInt(), childCard.cardBackgroundColor.defaultColor)
    }
}
