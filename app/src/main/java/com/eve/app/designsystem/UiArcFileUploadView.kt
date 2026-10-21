package com.eve.app.designsystem

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.eve.app.R
import kotlin.math.roundToInt

/**
 * UIArc File Upload Component.
 * Genuine native translation of official UIArc File Upload specification:
 * - Registry: https://uiarc.dev/r/file-upload.json
 * - States: Idle/Dropzone -> Ready -> Uploading (Spring.SMOOTH progress fill) -> Uploaded / Failed (Retry)
 * - File row with icon, name, formatted size (formatSize: B, KB, MB)
 * - Synchronized live percentage counter and bar fill: x = (percent - 100)%
 * - Check mark reveal with Ease.ENTER (240ms, 80ms delay)
 * - Retry button entrance with Spring.SNAPPY
 * - Instant updates under reduced motion
 */
class UiArcFileUploadView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    enum class UploadState {
        IDLE,
        READY,
        UPLOADING,
        DONE,
        FAILED
    }

    companion object {
        const val PROGRESS_DURATION_MS = 500L
        const val CHECK_ENTRANCE_DURATION_MS = 240L
        const val CHECK_ENTRANCE_DELAY_MS = 80L

        fun formatSize(bytes: Long): String {
            if (bytes < 1024) return "$bytes B"
            if (bytes < 1024 * 1024) return "${(bytes / 1024.0).roundToInt()} KB"
            val mb = bytes / (1024.0 * 1024.0)
            return if (mb >= 10.0) "${mb.roundToInt()} MB" else String.format("%.1f MB", mb)
        }
    }

    private val density = resources.displayMetrics.density

    // Dropzone / Idle UI
    private val dropzoneLayout: LinearLayout
    private val ivDropzoneIcon: ImageView
    private val tvDropzoneTitle: TextView
    private val tvDropzoneSubtitle: TextView

    // Selected File UI
    private val fileRowLayout: LinearLayout
    private val ivFileIcon: ImageView
    private val tvFileName: TextView
    private val tvFileMeta: TextView
    private val tvPhaseStatus: TextView
    private val progressFrame: FrameLayout
    private val progressBarTrack: FrameLayout
    private val progressBarFill: View
    private val btnRemove: ImageButton
    private val btnRetry: ImageButton
    private val ivCheckDone: ImageView

    private var currentState: UploadState = UploadState.IDLE
    private var currentProgress: Float = 0f
    private var fileName: String = ""
    private var fileSizeBytes: Long = 0L

    private var progressAnimator: ValueAnimator? = null

    var onBrowseClickListener: (() -> Unit)? = null
    var onRemoveClickListener: (() -> Unit)? = null
    var onRetryClickListener: (() -> Unit)? = null

    init {
        // Dropzone container
        dropzoneLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            val pad = Math.round(18f * density)
            setPadding(pad, pad, pad, pad)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 14f * density
                setColor(ContextCompat.getColor(context, R.color.surface))
                setStroke(Math.round(1f * density), ContextCompat.getColor(context, R.color.hairline))
            }
            isClickable = true
            isFocusable = true
            setOnClickListener { onBrowseClickListener?.invoke() }
        }

        ivDropzoneIcon = ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(Math.round(36f * density), Math.round(36f * density))
            setImageResource(R.drawable.ic_upload)
            setColorFilter(ContextCompat.getColor(context, R.color.system_blue))
        }

        tvDropzoneTitle = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = Math.round(8f * density)
            }
            text = "Choose PDF Document"
            setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            textSize = 15f
            typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
        }

        tvDropzoneSubtitle = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = Math.round(4f * density)
            }
            text = "Drop files here or browse from your device (PDF up to 20MB)"
            setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            textSize = 13f
        }

        dropzoneLayout.addView(ivDropzoneIcon)
        dropzoneLayout.addView(tvDropzoneTitle)
        dropzoneLayout.addView(tvDropzoneSubtitle)
        addView(dropzoneLayout)

        // Selected File Row container
        fileRowLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            val pad = Math.round(14f * density)
            setPadding(pad, pad, pad, pad)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 14f * density
                setColor(ContextCompat.getColor(context, R.color.surface))
                setStroke(Math.round(1f * density), ContextCompat.getColor(context, R.color.hairline))
            }
            visibility = View.GONE
        }

        val topRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        ivFileIcon = ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(Math.round(24f * density), Math.round(24f * density))
            setImageResource(R.drawable.ic_description)
            setColorFilter(ContextCompat.getColor(context, R.color.system_blue))
        }

        val textDetailsLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = Math.round(10f * density)
                marginEnd = Math.round(8f * density)
            }
        }

        tvFileName = TextView(context).apply {
            textSize = 14f
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.MIDDLE
            setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
        }

        val metaRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = Math.round(2f * density)
            }
        }

        tvFileMeta = TextView(context).apply {
            textSize = 12f
            setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
        }

        tvPhaseStatus = TextView(context).apply {
            textSize = 12f
            setTextColor(ContextCompat.getColor(context, R.color.system_blue))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                marginStart = Math.round(6f * density)
            }
        }

        metaRow.addView(tvFileMeta)
        metaRow.addView(tvPhaseStatus)

        textDetailsLayout.addView(tvFileName)
        textDetailsLayout.addView(metaRow)

        ivCheckDone = ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(Math.round(20f * density), Math.round(20f * density))
            setImageResource(R.drawable.ic_check_circle)
            setColorFilter(ContextCompat.getColor(context, R.color.system_green))
            visibility = View.GONE
        }

        btnRetry = ImageButton(context).apply {
            layoutParams = LinearLayout.LayoutParams(Math.round(36f * density), Math.round(36f * density))
            setImageResource(R.drawable.ic_refresh)
            setColorFilter(ContextCompat.getColor(context, R.color.system_orange))
            background = null
            contentDescription = "Retry upload"
            visibility = View.GONE
            setOnClickListener { onRetryClickListener?.invoke() }
        }

        btnRemove = ImageButton(context).apply {
            layoutParams = LinearLayout.LayoutParams(Math.round(36f * density), Math.round(36f * density))
            setImageResource(R.drawable.ic_close)
            setColorFilter(ContextCompat.getColor(context, R.color.text_secondary))
            background = null
            contentDescription = "Remove file"
            setOnClickListener { onRemoveClickListener?.invoke() }
        }

        topRow.addView(ivFileIcon)
        topRow.addView(textDetailsLayout)
        topRow.addView(ivCheckDone)
        topRow.addView(btnRetry)
        topRow.addView(btnRemove)
        fileRowLayout.addView(topRow)

        // Progress bar container
        progressFrame = FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                Math.round(6f * density)
            ).apply {
                topMargin = Math.round(10f * density)
            }
            visibility = View.GONE
        }

        progressBarTrack = FrameLayout(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            setBackgroundColor(ContextCompat.getColor(context, R.color.fill_quaternary))
        }

        progressBarFill = View(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            setBackgroundColor(ContextCompat.getColor(context, R.color.system_blue))
        }

        progressBarTrack.addView(progressBarFill)
        progressFrame.addView(progressBarTrack)
        fileRowLayout.addView(progressFrame)

        addView(fileRowLayout)
    }

    fun setFile(name: String, sizeBytes: Long) {
        this.fileName = name
        this.fileSizeBytes = sizeBytes
        tvFileName.text = name
        tvFileMeta.text = formatSize(sizeBytes)
        btnRemove.contentDescription = "Remove $name"
        setState(UploadState.READY)
    }

    fun setProgress(percent: Int) {
        val target = percent.coerceIn(0, 100).toFloat()
        if (currentState != UploadState.UPLOADING) {
            setState(UploadState.UPLOADING)
        }

        if (!UiArcMotion.isMotionEnabled(context)) {
            currentProgress = target
            applyProgress(target)
            return
        }

        progressAnimator?.cancel()
        progressAnimator = ValueAnimator.ofFloat(currentProgress, target).apply {
            duration = PROGRESS_DURATION_MS
            interpolator = UiArcMotion.Spring.SMOOTH
            addUpdateListener { va ->
                val v = va.animatedValue as Float
                currentProgress = v
                applyProgress(v)
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (target >= 100f && currentState == UploadState.UPLOADING) {
                        setState(UploadState.DONE)
                    }
                }
            })
            start()
        }
    }

    private fun applyProgress(value: Float) {
        val w = progressBarTrack.width.toFloat().coerceAtLeast(1f)
        val shift = (value - 100f) / 100f * w
        progressBarFill.translationX = shift
        tvPhaseStatus.text = "· Uploading ${value.roundToInt()}%"
    }

    fun setState(state: UploadState) {
        currentState = state
        when (state) {
            UploadState.IDLE -> {
                dropzoneLayout.visibility = View.VISIBLE
                fileRowLayout.visibility = View.GONE
                progressFrame.visibility = View.GONE
                ivCheckDone.visibility = View.GONE
                btnRetry.visibility = View.GONE
            }
            UploadState.READY -> {
                dropzoneLayout.visibility = View.GONE
                fileRowLayout.visibility = View.VISIBLE
                progressFrame.visibility = View.GONE
                tvPhaseStatus.text = "· Ready"
                tvPhaseStatus.setTextColor(ContextCompat.getColor(context, R.color.system_blue))
                ivCheckDone.visibility = View.VISIBLE
                ivCheckDone.setColorFilter(ContextCompat.getColor(context, R.color.text_secondary))
                btnRetry.visibility = View.GONE
            }
            UploadState.UPLOADING -> {
                dropzoneLayout.visibility = View.GONE
                fileRowLayout.visibility = View.VISIBLE
                progressFrame.visibility = View.VISIBLE
                tvPhaseStatus.setTextColor(ContextCompat.getColor(context, R.color.system_blue))
                ivCheckDone.visibility = View.GONE
                btnRetry.visibility = View.GONE
            }
            UploadState.DONE -> {
                dropzoneLayout.visibility = View.GONE
                fileRowLayout.visibility = View.VISIBLE
                progressFrame.visibility = View.GONE
                tvPhaseStatus.text = "· Uploaded"
                tvPhaseStatus.setTextColor(ContextCompat.getColor(context, R.color.system_green))
                ivCheckDone.visibility = View.VISIBLE
                ivCheckDone.setColorFilter(ContextCompat.getColor(context, R.color.system_green))
                btnRetry.visibility = View.GONE

                if (UiArcMotion.isMotionEnabled(context)) {
                    ivCheckDone.scaleX = 0.6f
                    ivCheckDone.scaleY = 0.6f
                    ivCheckDone.alpha = 0f
                    ivCheckDone.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .alpha(1f)
                        .setDuration(CHECK_ENTRANCE_DURATION_MS)
                        .setStartDelay(CHECK_ENTRANCE_DELAY_MS)
                        .setInterpolator(UiArcMotion.Ease.ENTER)
                        .start()
                }
            }
            UploadState.FAILED -> {
                dropzoneLayout.visibility = View.GONE
                fileRowLayout.visibility = View.VISIBLE
                progressFrame.visibility = View.GONE
                tvPhaseStatus.text = "· Upload failed"
                tvPhaseStatus.setTextColor(ContextCompat.getColor(context, R.color.system_red))
                ivCheckDone.visibility = View.GONE
                btnRetry.visibility = View.VISIBLE

                if (UiArcMotion.isMotionEnabled(context)) {
                    btnRetry.scaleX = 0.6f
                    btnRetry.scaleY = 0.6f
                    btnRetry.alpha = 0f
                    btnRetry.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .alpha(1f)
                        .setDuration(UiArcMotion.Spring.SNAPPY.settlingDurationMs)
                        .setInterpolator(UiArcMotion.Spring.SNAPPY)
                        .start()
                }
            }
        }
    }

    fun reset() {
        progressAnimator?.cancel()
        currentProgress = 0f
        fileName = ""
        fileSizeBytes = 0L
        setState(UploadState.IDLE)
    }

    fun getState(): UploadState = currentState
    fun getFileName(): String = fileName
}
