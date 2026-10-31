package com.eve.app.ui.profile

import android.content.res.Configuration
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Button
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.eve.app.R
import com.eve.app.databinding.ActivityProfileBinding
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfileActionAccessibilityNativeTest {
    @Test fun dayNightProfileActionsExposeSingleButtonSemanticAndClickableNode() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            for (night in listOf(Configuration.UI_MODE_NIGHT_NO, Configuration.UI_MODE_NIGHT_YES)) {
                val base = instrumentation.targetContext
                val config = Configuration(base.resources.configuration).apply {
                    uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or night
                }
                val context = ContextThemeWrapper(base.createConfigurationContext(config), R.style.Theme_Eve)
                val binding = ActivityProfileBinding.inflate(LayoutInflater.from(context))
                val actions = listOf(binding.btnSetPhoto, binding.btnEditInfo, binding.btnSettings)
                ProfileActionAccessibility.install(actions)
                for (action in actions) {
                    val label = action.getChildAt(1) as TextView
                    assertEquals(View.IMPORTANT_FOR_ACCESSIBILITY_NO, label.importantForAccessibility)
                    val node = action.createAccessibilityNodeInfo()
                    try {
                        assertEquals(Button::class.java.name, node.className.toString())
                        assertEquals(label.text.toString(), node.text.toString())
                        assertTrue(node.isClickable)
                        assertTrue(node.isFocusable)
                        assertTrue(node.isEnabled)
                        assertTrue(node.actionList.any { it.id == AccessibilityNodeInfo.ACTION_CLICK })
                        action.isEnabled = false
                        val disabled = action.createAccessibilityNodeInfo()
                        try {
                            assertFalse(disabled.isEnabled)
                            assertFalse(disabled.actionList.any { it.id == AccessibilityNodeInfo.ACTION_CLICK })
                        } finally {
                            disabled.recycle()
                        }
                    } finally {
                        node.recycle()
                    }
                }
            }
        }
    }
}
