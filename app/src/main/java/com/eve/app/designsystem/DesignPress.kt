package com.eve.app.designsystem

import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View

/**
 * Pressed-state spring motion for XML and programmatic controls backed by Telegram's ButtonBounce.
 * Native click dispatch and touch targets stay intact.
 */
object DesignPress {
    fun bind(view: View, onClick: (() -> Unit)? = null) {
        val bounce = ButtonBounce(view)
        view.stateListAnimator = null
        if (onClick != null) view.setOnClickListener { onClick() }
        view.setOnTouchListener { target, event ->
            if (target.isEnabled) {
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        bounce.setPressed(true)
                        target.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        bounce.setPressed(false)
                    }
                }
            }
            false
        }
        TouchTargets.ensure(view)
    }
}
