package com.eve.app.ui.profile

import android.app.Activity
import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.FrameLayout
import com.eve.app.R
import com.eve.app.theme.TelegramThemeApplier
import com.eve.app.theme.TelegramThemeEngine
import com.eve.app.ui.common.TelegramRadioIndicator
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Canonical theme traversal -> existing category row -> actual native indicator pixels. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35],application=Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ProfileCategoryThemeTraversalTest {
    private fun exercise(checked:Boolean) {
        val controller=Robolectric.buildActivity(Activity::class.java)
        controller.get().setTheme(R.style.Theme_Eve)
        controller.setup().visible()
        val activity=controller.get()
        val row=TelegramProfileRadioCell(activity,Robolectric.buildAttributeSet().addAttribute(android.R.attr.text,"General").build())
        val root=FrameLayout(activity).apply { addView(row) }
        activity.setContentView(root)
        var clicks=0
        row.setOnClickListener { clicks++ }
        row.isChecked=checked
        val indicator=row.getChildAt(1).background as TelegramRadioIndicator
        indicator.jumpToCurrentState()
        val progress=indicator.progress
        val night=TelegramThemeEngine.resolvePalette(activity,TelegramThemeEngine.BUILT_IN_THEMES[1],0)
        val expected=if(checked) night.accent else night.textSecondary
        val bitmap=Bitmap.createBitmap(22,22,Bitmap.Config.ARGB_8888)
        try {
            TelegramThemeApplier.applyToViewTree(root,night)
            assertEquals("Palette update must preserve selection progress",progress,indicator.progress,0f)
            indicator.setBounds(0,0,22,22)
            indicator.draw(Canvas(bitmap))
            val pixels=IntArray(22*22); bitmap.getPixels(pixels,0,22,0,0,22,22)
            assertTrue("Actual native radio pixels must use the newly applied palette",pixels.any { it==expected })
            val node=AccessibilityNodeInfo.obtain()
            row.onInitializeAccessibilityNodeInfo(node)
            assertEquals(checked,node.isChecked)
            assertEquals("android.widget.RadioButton",node.className)
            node.recycle()
            row.performClick()
            assertEquals(1,clicks)
            assertEquals("Theme traversal cannot toggle a persisted category",checked,row.isChecked)
        } finally { bitmap.recycle(); controller.pause().stop().destroy() }
    }
    @Test fun selectedDotTracksLivePaletteWithoutRebindingOrChangingSelection()=exercise(true)
    @Test fun unselectedRingTracksLivePaletteWithoutRebindingOrChangingSelection()=exercise(false)
}
