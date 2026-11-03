package com.eve.app.util

import android.app.Activity
import android.os.Looper
import android.view.View
import com.eve.app.R
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowChoreographer
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.PAUSED)
@Config(sdk = [35], application = android.app.Application::class)
class AppUndoBarDeadlineTest {
    private fun host(block: (Activity) -> Unit) {
        ShadowChoreographer.setPaused(true)
        val controller = Robolectric.buildActivity(Activity::class.java)
        controller.get().setTheme(R.style.Theme_Eve)
        controller.setup().visible()
        try { block(controller.get()) }
        finally {
            AppUndoBar.cancelDeferredForHost(controller.get())
            controller.pause().stop().destroy()
            advance(6000)
        }
    }
    private fun advance(ms: Long) = shadowOf(Looper.getMainLooper()).idleFor(ms, TimeUnit.MILLISECONDS)

    @Test fun animationCompletionCannotCommitBeforeDeadline() = host { activity ->
        var deletes = 0
        AppUndoBar.showDeferred(activity, "Pending", onUndo = {}, onExecuteDelete = { deletes++ })
        // A cancelled/disabled visual animator must not resolve the independent deadline.
        activity.findViewById<com.eve.app.ui.common.CircularCountdownView>(R.id.countdownView).cancel()
        advance(4999); assertEquals(0, deletes)
        advance(1); assertEquals(1, deletes)
        advance(6000); assertEquals(1, deletes)
    }

    @Test fun undoCancelsDeadlineExactlyOnce() = host { activity ->
        var deletes = 0; var undos = 0
        AppUndoBar.showDeferred(activity, "Pending", onUndo = { undos++ }, onExecuteDelete = { deletes++ })
        val button = activity.findViewById<View>(R.id.btnUndoAction)
        button.performClick(); button.performClick(); advance(6000)
        assertEquals(1, undos); assertEquals(0, deletes)
    }

    @Test fun replacingDeferredBarCancelsRatherThanCommitsPreviousAction() = host { activity ->
        var oldDeletes = 0; var oldUndos = 0; var newDeletes = 0
        AppUndoBar.showDeferred(activity, "First", onUndo = { oldUndos++ }, onExecuteDelete = { oldDeletes++ })
        advance(3000)
        AppUndoBar.showDeferred(activity, "Second", onUndo = {}, onExecuteDelete = { newDeletes++ })
        advance(2000)
        assertEquals(1, oldUndos); assertEquals(0, oldDeletes); assertEquals(0, newDeletes)
        advance(3000); assertEquals(1, newDeletes)
    }

    @Test fun legacyShowStillExecutesImmediatelyAndSupportsUndo() = host { activity ->
        var deletes = 0; var undos = 0
        AppUndoBar.show(activity, "Deleted", onUndo = { undos++ }, onExecuteDelete = { deletes++ })
        assertEquals(1, deletes)
        AppUndoBar.cancelDeferredForHost(activity)
        activity.findViewById<View>(R.id.btnUndoAction).performClick()
        advance(6000); assertEquals(1, deletes); assertEquals(1, undos)
    }
}
