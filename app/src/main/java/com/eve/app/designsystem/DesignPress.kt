package com.eve.app.designsystem

import android.animation.AnimatorInflater
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import com.eve.app.R

/** One pressed-state animator for XML and programmatic controls. Native click dispatch stays intact. */
object DesignPress {
    fun bind(view: View, onClick: (() -> Unit)? = null) {
        view.stateListAnimator = AnimatorInflater.loadStateListAnimator(view.context, R.animator.design_press_scale)
        if (onClick != null) view.setOnClickListener { onClick() }
        view.setOnTouchListener { target, event ->
            if (target.isEnabled && event.actionMasked == MotionEvent.ACTION_DOWN) {
                target.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            }
            false
        }
        TouchTargets.ensure(view)
    }
}
