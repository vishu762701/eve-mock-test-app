package com.eve.app.ui.profile

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Assert real Canvas rendering is used instead of three XML pill visuals. */
class ProfileActionsCanvasContractTest {
    @Test fun realModeMyProfileCanvasPaintsSourceGeometryAndKeepsNativeActionHandlers() {
        val xml = File("src/main/res/layout/activity_profile.xml").readText()
        val row = xml.substringAfter("<com.eve.app.ui.profile.ProfileActionsCanvasLayout")
            .substringBefore("</com.eve.app.ui.profile.ProfileActionsCanvasLayout>")
        for (id in listOf("btnSetPhoto", "btnEditInfo", "btnSettings")) {
            assertTrue(row.contains("android:id=\"@+id/$id\""))
        }
        assertFalse(row.contains("@drawable/telegram_profile_action_surface"))
        val canvas = File("src/main/java/com/eve/app/ui/profile/ProfileActionsCanvasLayout.kt").readText()
        for (term in listOf(
            "override fun onDraw(canvas: Canvas)",
            "canvas.drawRoundRect(rect, radius, radius, surfacePaint)",
            "textLayout.draw(canvas)",
            "paintIcon(outlineIcon, 1f - mix)",
            "paintIcon(filledIcon, mix)",
            "setPressScale(target: View, scale: Float)",
            "setParentExpanded(value: Float)",
            "setActionColor(value: Int)",
            "fun setDrawingBlur(drawing: Boolean)",
            "override fun onDetachedFromWindow()",
            "avatar.getLocationInWindow(locAvatar)",
            "getLocationInWindow(locActions)",
            "// Stage 1: Button surface backgrounds & radial highlights",
            "// Stage 2: RenderNode avatar blur shader",
            "// Stage 3: Text labels and action icons or loading state on top"
        )) assertTrue("Missing source Canvas stage: $term", canvas.contains(term))
        val activity = File("src/main/java/com/eve/app/ui/profile/ProfileActivity.kt").readText()
        assertTrue(activity.contains("ProfileActionIcons.applyTo("))
        assertTrue(activity.contains("binding.layoutActions.setActionColor(surface)"))
        assertTrue(activity.contains("binding.layoutActions.setDrawingBlur(false)"))
        assertTrue(activity.contains("lifecycleScope.launch(Dispatchers.IO)"))
        val motion = File("src/main/java/com/eve/app/ui/profile/ProfileActionBounce.kt").readText()
        assertTrue(motion.contains("target.setPressScale(view, scale)"))
        assertTrue(File("src/main/java/com/eve/app/ui/profile/ProfileHeaderExpansion.kt")
            .readText().contains("binding.layoutActions.setParentExpanded(f)"))
    }
}
