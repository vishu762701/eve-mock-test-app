package com.eve.app.ui.profile

import android.app.Activity
import android.app.ActivityManager
import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RenderNode
import android.view.View
import android.widget.FrameLayout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ProfileGooeyLifecycleTest {
    private fun field(owner: Any, name: String): Any? = owner.javaClass.getDeclaredField(name)
        .apply { isAccessible = true }.get(owner)

    @Test fun actualCpuAvatarReleasesOnDetachAndRendersAfterSameSizeReattach() {
        val controller = Robolectric.buildActivity(Activity::class.java).setup().visible()
        val activity = controller.get()
        shadowOf(activity.getSystemService(ActivityManager::class.java)).setMemoryClass(128)
        val view = ProfileGooeyView(activity)
        val child = View(activity).apply { setBackgroundColor(Color.RED) }
        view.addView(child, FrameLayout.LayoutParams(100, 100))
        val container = FrameLayout(activity)
        activity.setContentView(container)
        val output = Bitmap.createBitmap(300, 400, Bitmap.Config.ARGB_8888)
        try {
            container.addView(view, FrameLayout.LayoutParams(300, 400))
            view.measure(View.MeasureSpec.makeMeasureSpec(300, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY))
            view.layout(0, 0, 300, 400)
            view.setGooeyEnabled(true)
            view.setBlurIntensity(.25f)
            val impl = requireNotNull(field(view, "impl"))
            val initial = field(impl, "bitmap") as Bitmap
            view.draw(Canvas(output))
            assertFalse(initial.isRecycled)
            container.removeView(view)
            assertTrue("Detach releases the actual source CPU buffer", initial.isRecycled)
            assertNull(field(impl, "bitmap"))
            assertNull(field(impl, "bitmapCanvas"))
            container.addView(view, FrameLayout.LayoutParams(300, 400))
            val restored = field(impl, "bitmap") as Bitmap
            assertNotSame(initial, restored)
            assertFalse(restored.isRecycled)
            output.eraseColor(Color.TRANSPARENT)
            view.draw(Canvas(output))
            val pixels = IntArray(300*400).also { output.getPixels(it,0,300,0,0,300,400) }
            assertTrue("Reattached same-size avatar still renders", pixels.any { Color.red(it) > 0 })
            container.removeView(view)
            assertTrue(restored.isRecycled)
        } finally { output.recycle(); controller.pause().stop().destroy() }
    }

    @Test fun gpuReleaseDiscardsEveryRecordedChildListAndCanRecordAgain() {
        val controller = Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val view = ProfileGooeyView(controller.get())
            // Exercise the actual production GPU owner without relying on host
            // core-count heuristics. This is display-list lifecycle, not HWUI parity.
            val type = view.javaClass.declaredClasses.single { it.simpleName == "GPUImpl" }
            val impl = type.declaredConstructors.single().apply { isAccessible = true }.newInstance(view, 1f)
            val nodes = listOf("node", "effectNotchNode", "effectNode", "blurNode")
                .map { field(impl, it) as RenderNode }
            for (node in nodes) {
                node.setPosition(0,0,40,40)
                node.beginRecording().drawColor(Color.RED)
                node.endRecording()
                assertTrue(node.hasDisplayList())
            }
            val release = type.getDeclaredMethod("release").apply { isAccessible = true }
            release.invoke(impl); release.invoke(impl)
            assertTrue(nodes.all { !it.hasDisplayList() })
            nodes.first().beginRecording().drawColor(Color.BLUE)
            nodes.first().endRecording()
            assertTrue(nodes.first().hasDisplayList())
            release.invoke(impl)
        } finally { controller.pause().stop().destroy() }
    }
}
