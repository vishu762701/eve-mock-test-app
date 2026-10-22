package com.eve.app.designsystem

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import com.eve.app.R

/**
 * Read-only three-stage native adaptation of UIArc Stepper.
 * Progress is supplied only by validated form state and confirmed upload result.
 */
class UiArcUploadStepperView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {
    private val density = resources.displayMetrics.density
    private val labels = arrayOf("Exam", "PDF", "Upload")
    private val markers = mutableListOf<TextView>()
    private val connectors = mutableListOf<View>()
    private var completed = -1
    private var error = false
    private var busy = false

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        ViewCompat.setAccessibilityLiveRegion(this, ViewCompat.ACCESSIBILITY_LIVE_REGION_POLITE)
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
        contentDescription = "Upload progress: Select exam"
        for (index in 0..2) {
            if (index > 0) {
                val connector = View(context).apply {
                    setBackgroundColor(ContextCompat.getColor(context, R.color.text_secondary))
                    pivotX = 0f
                    scaleX = 0f
                    alpha = 0.65f
                    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                }
                addView(connector, LayoutParams(0, (2f * density).toInt().coerceAtLeast(1), 1f))
                connectors.add(connector)
            }
            val marker = TextView(context).apply {
                gravity = Gravity.CENTER
                textSize = 12f
                setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }
            addView(marker, LayoutParams((72f * density).toInt(), (48f * density).toInt()))
            markers.add(marker)
        }
        update(false, false, false, false, false)
    }

    fun update(
        examReady: Boolean,
        pdfReady: Boolean,
        uploading: Boolean,
        uploaded: Boolean,
        failed: Boolean
    ) {
        val progress = when {
            uploaded -> 3
            pdfReady && examReady -> 2
            examReady -> 1
            else -> 0
        }
        val changed = progress != completed || failed != error || uploading != busy
        val forward = completed >= 0 && progress > completed
        completed = progress
        error = failed
        busy = uploading
        val accent = DesignTokens.accent(context)
        val text = ContextCompat.getColor(context, R.color.text_primary)
        val onAccent = ContextCompat.getColor(context, R.color.on_accent)
        val muted = ContextCompat.getColor(context, R.color.text_secondary)
        val red = ContextCompat.getColor(context, R.color.system_red)
        markers.forEachIndexed { index, marker ->
            val done = index < progress && (index < 2 || uploaded)
            val active = index == progress.coerceAtMost(2) && !uploaded
            val failedMarker = failed && index == 2
            marker.text = when {
                done -> "✓\n" + labels[index]
                failedMarker -> "!\n" + labels[index]
                uploading && index == 2 -> "…\n" + labels[index]
                else -> (index + 1).toString() + "\n" + labels[index]
            }
            val filled = done || active || failedMarker
            marker.setTextColor(if (filled) onAccent else if (index > progress) muted else text)
            marker.background = GradientDrawable().apply {
                cornerRadius = 12f * density
                setColor(if (failedMarker) red else if (filled) accent else ContextCompat.getColor(context, R.color.bg_app))
            }
        }
        connectors.forEachIndexed { index, connector ->
            val fill = index < progress
            connector.setBackgroundColor(if (fill) accent else muted)
            connector.animate().cancel()
            if (fill && forward && UiArcMotion.isMotionEnabled(context)) {
                connector.scaleX = 0f
                connector.animate().scaleX(1f)
                    .setStartDelay(index * UiArcMotion.Duration.FAST)
                    .setDuration(UiArcMotion.Duration.FAST)
                    .setInterpolator(UiArcMotion.Ease.ENTER).start()
            } else connector.scaleX = if (fill) 1f else 0f
        }
        if (changed) {
            contentDescription = when {
                uploaded -> "Syllabus upload complete. Exam selected, PDF validated, upload confirmed."
                failed -> "Syllabus upload failed. Exam and PDF ready. Retry upload."
                uploading -> "Exam selected, PDF validated. Upload in progress."
                progress == 2 -> "Exam selected and PDF validated. Ready to upload."
                progress == 1 -> "Exam selected. Choose a PDF."
                else -> "Choose an exam to begin syllabus upload."
            }
        }
    }

    override fun onDetachedFromWindow() {
        connectors.forEach { it.animate().cancel() }
        super.onDetachedFromWindow()
    }
}
