package com.eve.app.ui

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.eve.app.theme.TelegramThemeEngine
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * A host-side source-file check cannot prove that Android packages the themes.
 * Verify the actual installed APK's AssetManager in both native UI suites.
 */
@RunWith(AndroidJUnit4::class)
class TelegramBundledThemeAssetsTest {
    @Test
    fun allBuiltInThemeAssetsArePackagedAndReadable() {
        val assets = InstrumentationRegistry.getInstrumentation().targetContext.assets
        val themes = TelegramThemeEngine.BUILT_IN_THEMES
        assertTrue("Built-in Telegram theme catalog must not be empty", themes.isNotEmpty())

        for (theme in themes) {
            val fileName = theme.assetName
                ?: throw AssertionError("Built-in theme ${theme.id} has no asset name")
            assets.open("themes/$fileName").use { stream ->
                assertTrue("Bundled theme ${theme.id} must not be empty", stream.read() != -1)
            }
        }
    }
}
