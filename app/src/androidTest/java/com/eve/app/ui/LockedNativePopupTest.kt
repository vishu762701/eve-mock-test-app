package com.eve.app.ui

import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.inspector.WindowInspector
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.RootMatchers.isPlatformPopup
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import com.eve.app.designsystem.DesignGlassLayout
import com.eve.app.designsystem.DesignSpinner
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test

/** Real native dropdown selection survives the shared popup surface and dismissal lifecycle. */
class LockedNativePopupTest {
    @Test fun spinnerKeepsNativeSelectionAndReleasesGlassOnDismiss() {
        assumeTrue(Build.VERSION.SDK_INT >= 31)
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        ActivityScenario.launch(RepairVerificationActivity::class.java).use { scenario ->
            lateinit var spinner: DesignSpinner
            var selected = -1
            var callbacks = 0
            scenario.onActivity { activity ->
                spinner = DesignSpinner(activity).apply {
                    adapter = ArrayAdapter(activity, android.R.layout.simple_spinner_dropdown_item, listOf("First choice", "Second choice"))
                    onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onNothingSelected(parent: AdapterView<*>?) = Unit
                        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                            selected = position
                            if (position == 1) callbacks++
                        }
                    }
                }
                activity.setContentView(FrameLayout(activity).apply {
                    addView(spinner, FrameLayout.LayoutParams(600, 132).apply { topMargin = 160 })
                })
            }
            instrumentation.waitForIdleSync()
            scenario.onActivity { spinner.performClick() }
            instrumentation.waitForIdleSync()
            lateinit var glass: DesignGlassLayout
            scenario.onActivity {
                fun find(view: View): DesignGlassLayout? {
                    if (view is DesignGlassLayout) return view
                    if (view is ViewGroup) for (i in 0 until view.childCount) find(view.getChildAt(i))?.let { return it }
                    return null
                }
                glass = requireNotNull(WindowInspector.getGlobalWindowViews().firstNotNullOfOrNull { find(it) })
                assertTrue(glass.isAttachedToWindow)
                assertTrue(glass.isHardwareAccelerated)
            }
            // The suite keeps system animations enabled for the protected reveal checks.
            // Wait for the native popup window/input position to settle before injecting a tap.
            instrumentation.uiAutomation.waitForIdle(500, 5000)
            try {
                onView(withText("Second choice")).inRoot(isPlatformPopup()).perform(click())
            } catch (error: Exception) {
                var geometry = ""
                scenario.onActivity {
                    fun inspect(view: View) {
                        if (view is android.widget.TextView && view.text == "Second choice") {
                            val visible = android.graphics.Rect()
                            val shown = view.getGlobalVisibleRect(visible)
                            geometry = "size=${view.width}x${view.height}; visible=$shown/$visible; root=${view.rootView.width}x${view.rootView.height}; attached=${view.isAttachedToWindow}"
                        }
                        if (view is ViewGroup) for (i in 0 until view.childCount) inspect(view.getChildAt(i))
                    }
                    inspect(glass)
                }
                throw AssertionError("Native popup click: $geometry; cause=${error.cause}", error)
            }
            instrumentation.waitForIdleSync()
            scenario.onActivity {
                assertEquals(1, selected)
                assertEquals(1, spinner.selectedItemPosition)
                assertEquals(1, callbacks)
                assertFalse("Dismissed popup releases its attached glass observer", glass.isAttachedToWindow)
            }
        }
    }
}
