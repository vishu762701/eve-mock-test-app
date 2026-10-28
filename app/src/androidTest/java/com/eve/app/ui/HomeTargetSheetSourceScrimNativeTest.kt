package com.eve.app.ui

import android.os.SystemClock
import android.view.View
import android.view.WindowManager
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.eve.app.ui.home.TargetExamsBottomSheet
import com.eve.app.ui.home.TelegramBottomSheetSourceScrim
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Actual source-backed scrim and original alpha value. Only run in terminal
 * FULL native matrix; FAST merely compiles this instrumentation source.
 */
@RunWith(AndroidJUnit4::class)
class HomeTargetSheetSourceScrimNativeTest {
    @Test fun originalScrimIsDrawableBehindTargetSheetWithoutWindowDim() {
        ActivityScenario.launch(RepairVerificationActivity::class.java).use { scenario ->
            lateinit var dialog: com.google.android.material.bottomsheet.BottomSheetDialog
            scenario.onActivity { activity ->
                dialog = TargetExamsBottomSheet.show(activity, emptyList())
                val host = dialog.findViewById<View>(com.google.android.material.R.id.coordinator)
                assertTrue("Full-screen sheet coordinator must retain Telegram scrim",
                    host?.background is TelegramBottomSheetSourceScrim)
                assertEquals("Original Telegram sheet has no Window FLAG_DIM_BEHIND",
                    0, (dialog.window?.attributes?.flags ?: 0) and WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            }
            SystemClock.sleep(500)
            scenario.onActivity {
                val host = dialog.findViewById<View>(com.google.android.material.R.id.coordinator)
                val scrim = host?.background as TelegramBottomSheetSourceScrim
                assertEquals("Original Telegram default dimBehindAlpha is 51", 51, scrim.alpha)
                dialog.dismiss()
            }
        }
    }
}
