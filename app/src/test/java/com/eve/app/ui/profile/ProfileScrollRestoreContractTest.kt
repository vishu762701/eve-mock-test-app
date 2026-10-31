package com.eve.app.ui.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ProfileScrollRestoreContractTest {
    @Test fun scrollProgressClampsOverscrollAcrossScreenDensities() {
        for (density in listOf(1f, 2f, 3.5f)) {
            val hundredDp = (100f * density).toInt()
            assertEquals(0f, ProfileScrollState.progress(-80, density), 0.0001f)
            assertEquals(0f, ProfileScrollState.progress(0, density), 0.0001f)
            assertEquals(1f, ProfileScrollState.progress(hundredDp, density), 0.0001f)
            assertEquals(1f, ProfileScrollState.progress(hundredDp + 200, density), 0.0001f)
        }
        assertEquals(0.5f, ProfileScrollState.progress(100, 2f), 0.0001f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidDisplayDensityDoesNotProduceNaNState() {
        ProfileScrollState.progress(100, 0f)
    }

    @Test fun restoredScrollUsesSameFullRendererAsUserGestures() {
        val source = File("src/main/java/com/eve/app/ui/profile/ProfileActivity.kt").readText()
        val setup = source.substringAfter("private fun setupScrollAndGooey()")
            .substringBefore("private fun renderProfileScrollState(")
        assertTrue(setup.contains("setOnScrollChangeListener"))
        assertTrue(setup.contains("renderProfileScrollState(scrollY, density)"))
        assertTrue(setup.contains("profileScrollView.doOnLayout"))
        assertTrue(setup.contains("renderProfileScrollState(binding.profileScrollView.scrollY, density)"))
        assertFalse(setup.contains("profileScrollView.post {\n            ProfileActionsCollapse.applyTo("))

        val render = source.substringAfter("private fun renderProfileScrollState(")
            .substringBefore("private fun setupActions(")
        for (effect in listOf(
            "ProfileScrollState.progress(scrollY, density)",
            "avatarGooey.setPullProgress(progress)",
            "avatarGooey.setBlurIntensity(blur)",
            "avatarGooey.setGooeyEnabled(",
            "avatarContainer.scaleX =",
            "avatarContainer.translationY =",
            "tvToolbarTitle.alpha = progress",
            "tvName.alpha =",
            "tvStatus.alpha =",
            "ProfileActionsCollapse.applyTo("
        )) {
            assertTrue("Restored scroll must update $effect", render.contains(effect))
        }
    }
}
