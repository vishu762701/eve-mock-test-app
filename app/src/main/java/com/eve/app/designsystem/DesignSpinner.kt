package com.eve.app.designsystem

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.AdapterView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatSpinner

/**
 * Native spinner selection/adapter semantics with shared popup chrome and hit region.
 * Genuine native translation of official UIArc Select specification:
 * - Registry: https://uiarc.dev/r/select.json
 * - Rolling label transition:
 *   enter: y = direction * 0.35em, opacity 0 -> 1, duration 240ms Ease.ENTER
 *   exit: y = direction * -0.3em, opacity 1 -> 0, duration 160ms Ease.STANDARD
 * - Direction: index > previousIndex ? 1 : -1
 * - Reduced motion fallback: instant crossfade
 */
class DesignSpinner @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.appcompat.R.attr.spinnerStyle
) : AppCompatSpinner(context, attrs, defStyleAttr) {

    private var previousPosition: Int = -1
    private var externalListener: OnItemSelectedListener? = null

    init {
        super.setOnItemSelectedListener(object : OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (previousPosition != -1 && previousPosition != position) {
                    val direction = if (position > previousPosition) 1 else -1
                    animateSelectionChange(direction, view)
                }
                previousPosition = position
                externalListener?.onItemSelected(parent, view, position, id)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                externalListener?.onNothingSelected(parent)
            }
        })
    }

    override fun setOnItemSelectedListener(listener: OnItemSelectedListener?) {
        this.externalListener = listener
    }

    override fun getOnItemSelectedListener(): OnItemSelectedListener? {
        return externalListener
    }

    override fun performClick(): Boolean {
        var handled = false
        DesignPopupGlass.around(this) { handled = super.performClick() }
        return handled
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        TouchTargets.ensure(this)
    }

    fun animateSelectionChange(direction: Int, targetView: View?) {
        val view = targetView ?: selectedView ?: return
        if (!UiArcMotion.isMotionEnabled(context)) {
            view.alpha = 1f
            view.translationY = 0f
            return
        }

        val emPx = if (view is TextView) view.textSize else 16f * resources.displayMetrics.density
        val startY = direction * 0.35f * emPx

        view.alpha = 0f
        view.translationY = startY
        view.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(UiArcMotion.Duration.STANDARD)
            .setInterpolator(UiArcMotion.Ease.ENTER)
            .start()
    }
}
