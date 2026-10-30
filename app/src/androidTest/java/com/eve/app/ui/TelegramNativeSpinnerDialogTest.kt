package com.eve.app.ui

import android.widget.FrameLayout
import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.eve.app.designsystem.TelegramNativeSpinnerDialog
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TelegramNativeSpinnerDialogTest {
    @Test fun sourceSizedModalSpinnerCreatesItsNativeProgressView() {
        ActivityScenario.launch(RepairVerificationActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val dialog = TelegramNativeSpinnerDialog(activity)
                dialog.show()
                try {
                    assertTrue(dialog.isShowing)
                    val panel = dialog.window!!.decorView.findViewWithTag<FrameLayout>(
                        "telegram_native_spinner_panel"
                    )
                    assertNotNull(panel)
                    val progress = dialog.window!!.decorView.findViewWithTag<View>(
                        "telegram_native_spinner_progress"
                    )
                    assertNotNull(progress)
                    val density = activity.resources.displayMetrics.density
                    assertEquals((86 * density + .5f).toInt(), panel.layoutParams.width)
                    assertEquals((86 * density + .5f).toInt(), panel.layoutParams.height)
                } finally {
                    dialog.dismiss()
                }
            }
        }
    }
}
