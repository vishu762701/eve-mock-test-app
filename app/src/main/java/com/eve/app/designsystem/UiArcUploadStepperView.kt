package com.eve.app.designsystem

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import com.eve.app.R

/**
 * Read-only UIArc Stepper adaptation for EVE's existing single-screen workflow.
 * Completion comes exclusively from validated Exam ID, bounded PDF and backend success.
 */
class UiArcUploadStepperView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {
    private val density = resources.displayMetrics.density
    private val labels = arrayOf("Exam", "PDF", "Upload")
    private val row = LinearLayout(context).apply {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }
    private val markers = mutableListOf<TextView>()
    private val rings = mutableListOf<View>()
    private val stageLabels = mutableListOf<TextView>()
    private val connectors = mutableListOf<View>()
    private val caption = TextView(context).apply {
        gravity = Gravity.CENTER
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        setTextColor(DesignTokens.secondary(context))
    }
    private var completed = -1
    private var error = false
    private var busy = false

    private fun dp(value: Float): Int = (value * density).toInt().coerceAtLeast(1)

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER
        clipChildren = false
        clipToPadding = false
        ViewCompat.setAccessibilityLiveRegion(this, ViewCompat.ACCESSIBILITY_LIVE_REGION_POLITE)
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
        isFocusable = true
        addView(row, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        addView(caption, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(4f)
        })

        repeat(3) { index ->
            if (index > 0) {
                val line = View(context).apply {
                    pivotX = 0f
                    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                }
                row.addView(line, LayoutParams(0, dp(2f), 1f))
                connectors.add(line)
            }
            val stage = LinearLayout(context).apply {
                orientation = VERTICAL
                gravity = Gravity.CENTER
            }
            row.addView(stage, LayoutParams(dp(72f), LayoutParams.WRAP_CONTENT))
            val frame = FrameLayout(context)
            stage.addView(frame, LayoutParams(dp(48f), dp(48f)))
            val marker = TextView(context).apply {
                gravity = Gravity.CENTER
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }
            frame.addView(marker, FrameLayout.LayoutParams(dp(40f), dp(40f), Gravity.CENTER))
            val ring = View(context).apply {
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                isClickable = false
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.TRANSPARENT)
                    setStroke(dp(2f), DesignTokens.accent(context))
                }
            }
            frame.addView(ring, FrameLayout.LayoutParams(dp(48f), dp(48f), Gravity.CENTER))
            val label = TextView(context).apply {
                text = labels[index]
                gravity = Gravity.CENTER
                maxLines = 2
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                setTextColor(DesignTokens.secondary(context))
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }
            stage.addView(label, LayoutParams(dp(72f), LayoutParams.WRAP_CONTENT))
            markers.add(marker)
            rings.add(ring)
            stageLabels.add(label)
        }
        update(false, false, false, false, false)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // UIArc's <30rem behavior: markers and a single active caption on phones.
        val compact = w.toFloat() / density < 480f
        for (label in stageLabels) label.visibility = if (compact) View.GONE else View.VISIBLE
        caption.visibility = if (compact) View.VISIBLE else View.GONE
    }

    fun update(
        examReady: Boolean, pdfReady: Boolean, uploading: Boolean,
        uploaded: Boolean, failed: Boolean
    ) {
        val state = UploadStepState.from(examReady, pdfReady, uploading, uploaded, failed)
        val progress = state.progress
        val changed = progress != completed || state.failed != error || state.uploading != busy
        val forward = completed >= 0 && progress > completed
        completed = progress
        error = state.failed
        busy = state.uploading
        val motion = UiArcMotion.isMotionEnabled(context)
        val accent = DesignTokens.accent(context)
        val mainText = DesignTokens.primary(context)
        val onAccent = ContextCompat.getColor(context, R.color.on_accent)
        val muted = DesignTokens.secondary(context)
        val red = ContextCompat.getColor(context, R.color.system_red)

        markers.forEachIndexed { index, marker ->
            val done = index < progress && (index < 2 || state.uploaded)
            val active = index == progress.coerceAtMost(2) && !state.uploaded
            val failedStage = state.failed && index == 2
            val symbol = when {
                done -> "✓"
                failedStage -> "!"
                state.uploading && index == 2 -> "…"
                else -> (index + 1).toString()
            }
            val previous = marker.text.toString()
            marker.animate().cancel()
            marker.text = symbol
            val filled = done || active || failedStage
            marker.setTextColor(if (filled) onAccent else if (index > progress) muted else mainText)
            marker.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(if (failedStage) red else if (filled) accent
                    else ContextCompat.getColor(context, R.color.bg_app))
            }
            marker.alpha = 1f
            marker.scaleX = 1f
            marker.scaleY = 1f
            if (motion && changed && previous != symbol) {
                marker.scaleX = 0.82f
                marker.scaleY = 0.82f
                marker.alpha = 0.65f
                marker.animate().scaleX(1f).scaleY(1f).alpha(1f)
                    .setDuration(UiArcMotion.Spring.SNAPPY.settlingDurationMs)
                    .setInterpolator(UiArcMotion.Spring.SNAPPY).start()
            }
            val ring = rings[index]
            ring.animate().cancel()
            ring.visibility = if (active || failedStage) View.VISIBLE else View.INVISIBLE
            ring.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.TRANSPARENT)
                setStroke(dp(2f), if (failedStage) red else accent)
            }
            ring.scaleX = 1f
            ring.scaleY = 1f
            if (motion && changed && (active || failedStage)) {
                ring.scaleX = 0.72f
                ring.scaleY = 0.72f
                ring.animate().scaleX(1f).scaleY(1f)
                    .setStartDelay(if (forward) UiArcMotion.Duration.FAST else 0L)
                    .setDuration(UiArcMotion.Spring.SNAPPY.settlingDurationMs)
                    .setInterpolator(UiArcMotion.Spring.SNAPPY).start()
            }
        }
        connectors.forEachIndexed { index, line ->
            val filled = index < progress
            line.setBackgroundColor(if (filled) accent else muted)
            line.animate().cancel()
            if (motion && filled && forward) {
                line.scaleX = 0f
                line.animate().scaleX(1f)
                    .setStartDelay(index * UiArcMotion.Duration.FAST)
                    .setDuration(UiArcMotion.Duration.FAST)
                    .setInterpolator(UiArcMotion.Ease.ENTER).start()
            } else line.scaleX = if (filled) 1f else 0f
        }
        val message = state.message
        caption.text = message
        if (changed) contentDescription = message
    }

    override fun onDetachedFromWindow() {
        markers.forEach { it.animate().cancel() }
        rings.forEach { it.animate().cancel() }
        connectors.forEach { it.animate().cancel() }
        super.onDetachedFromWindow()
    }
}
