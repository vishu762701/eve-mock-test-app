package com.eve.app.ui.profile

import android.app.Application
import android.content.Intent
import android.net.Uri
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class UsernameCardActivityLifecycleTest {

    @Test
    fun activityInitializesAndReleasesOnTeardown() {
        val intent = Intent().apply {
            data = Uri.parse("eve-mock-test://username/test_user")
        }
        val controller = Robolectric.buildActivity(UsernameCardActivity::class.java, intent)
        val activity = controller.create().start().resume().visible().get()
        assertNotNull(activity)
        assertFalse(activity.isFinishing)

        // Verify scene field is initialized
        val sceneField = UsernameCardActivity::class.java.getDeclaredField("scene").apply { isAccessible = true }
        val scene = sceneField.get(activity) as? TelegramQrSceneView
        assertNotNull(scene)

        // Verify gallerySaveJob tracking field exists
        val jobField = UsernameCardActivity::class.java.getDeclaredField("gallerySaveJob").apply { isAccessible = true }
        assertNull(jobField.get(activity))

        // Trigger finishWithMotion
        val finishMethod = UsernameCardActivity::class.java.getDeclaredMethod("finishWithMotion").apply { isAccessible = true }
        finishMethod.invoke(activity)

        val closingField = UsernameCardActivity::class.java.getDeclaredField("qrClosing").apply { isAccessible = true }
        assertTrue(closingField.getBoolean(activity))

        // Calling saveToGallery or shareImage after closing should be safely ignored
        val saveMethod = UsernameCardActivity::class.java.getDeclaredMethod("saveToGallery").apply { isAccessible = true }
        val shareMethod = UsernameCardActivity::class.java.getDeclaredMethod("shareImage").apply { isAccessible = true }
        saveMethod.invoke(activity)
        shareMethod.invoke(activity)

        // Teardown
        controller.pause().stop().destroy()
        assertTrue(activity.isDestroyed)
    }
}
