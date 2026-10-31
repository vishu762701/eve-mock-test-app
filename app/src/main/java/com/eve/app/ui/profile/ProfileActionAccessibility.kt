package com.eve.app.ui.profile

import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Source semantics from Telegram ProfileActionsView.java:1247-1328, adapted
 * from virtual Canvas nodes to the existing native View action containers.
 */
internal object ProfileActionAccessibility {
    fun install(buttons: List<LinearLayout>) {
        require(buttons.size == 3)
        // EVE accessibility accommodation: source 54dp buttons clip at 2x fonts.
        // Keep the original dimensions at normal font sizes; reserve two real lines.
        if (buttons.first().resources.configuration.fontScale > 1.4f) {
            val density = buttons.first().resources.displayMetrics.density
            val required = buttons.maxOf { button ->
                val label = button.getChildAt(1) as TextView
                val metrics = label.paint.fontMetricsInt
                val textHeight = metrics.bottom - metrics.top + label.lineHeight + label.compoundPaddingTop + label.compoundPaddingBottom
                val margins = (label.layoutParams as android.view.ViewGroup.MarginLayoutParams).topMargin
                (button.getChildAt(0).layoutParams.height + margins + textHeight + button.paddingTop + button.paddingBottom)
                    .coerceAtLeast((54 * density).toInt())
            }
            for (button in buttons) button.layoutParams = button.layoutParams.apply { height = required }
            (buttons.first().parent as? LinearLayout)?.let { row ->
                row.layoutParams = row.layoutParams.apply { height = required + row.paddingTop + row.paddingBottom }
            }
        }
        for (button in buttons) {
            val label = button.getChildAt(1) as TextView
            // A single TalkBack focus/click target, not a duplicate text child.
            label.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            button.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
            button.setAccessibilityDelegate(object : View.AccessibilityDelegate() {
                override fun onInitializeAccessibilityNodeInfo(
                    host: View,
                    info: AccessibilityNodeInfo
                ) {
                    super.onInitializeAccessibilityNodeInfo(host, info)
                    info.className = Button::class.java.name
                    info.text = label.text
                    info.isClickable = host.isClickable
                    info.isFocusable = host.isFocusable
                    info.isEnabled = host.isEnabled
                    if (host.isEnabled && host.isClickable) {
                        info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_CLICK)
                    } else {
                        // Android's default View delegate may have advertised a
                        // click action before collapse disabled this control.
                        info.removeAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_CLICK)
                    }
                }
            })
        }
    }
}
