package com.eve.app.designsystem

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.eve.app.R
import java.util.Locale
import kotlin.math.roundToInt

/**
 * UIArc File Upload Component.
 * Genuine native translation of official UIArc File Upload specification:
 * - Registry: https://uiarc.dev/r/file-upload.json
 * - Authoritative state machine: IDLE -> READY -> UPLOADING -> DONE / FAILED (Retry)
 * - Selection morph: Trigger scales down (0.96) and fades out (160ms, Ease.STANDARD),
 *   selected card rises (+8dp) and fades in (240ms, Ease.ENTER)
 * - Clearing morph: Selected card slides up (-8dp) and fades out (160ms), trigger restores (240ms)
 * - Completion checkmark: Scale 0.6 -> 1.0, alpha 0 -> 1 (240ms, Ease.ENTER, 80ms delay)
 * - Single cohesive production integration for Syllabus PDF uploads (max 20MB)
 * - Reduced motion instant fallback
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
        const val MAX_FILE_SIZE_BYTES = 20L * 1024L * 1024L
        const val PROGRESS_DURATION_MS = 500L
        const val CHECK_ENTRANCE_DURATION_MS = 240L
        const val CHECK_ENTRANCE_DELAY_MS = 80L

        fun formatSize(bytes: Long): String {
            if (bytes < 1024) return "$bytes B"
            if (bytes < 1024 * 1024) return "${(bytes / 1024.0).roundToInt()} KB"
            val mb = bytes / (1024.0 * 1024.0)
            return if (mb >= 10.0) "${mb.roundToInt()} MB" else String.format(Locale.US, "%.1f MB", mb)
        }
    }

    private val density = resources.displayMetrics.density
    private var currentState: UploadState = UploadState.IDLE
    private var currentFileName: String = ""
    private var currentFileSizeBytes: Long = 0L

    private var btnChoosePdf: View? = null
    private var layoutSelectedPdf: View? = null
    private var tvFileName: TextView? = null
    private var tvFileSize: TextView? = null
    private var ivUploadCheckDone: ImageView? = null
    private var btnClearSelectedPdf: View? = null

    override fun onFinishInflate() {
        super.onFinishInflate()
        btnChoosePdf = findViewById(R.id.btnChoosePdf)
        layoutSelectedPdf = findViewById(R.id.layoutSelectedPdf)
        tvFileName = findViewById(R.id.tvNewPdfName)
        tvFileSize = findViewById(R.id.tvNewPdfSize)
        ivUploadCheckDone = findViewById(R.id.ivUploadCheckDone)
        btnClearSelectedPdf = findViewById(R.id.btnClearSelectedPdf)
    }

    /**
     * Binds selected file metadata and morphs trigger to the selected file card.
     */
    fun bindSelectedFile(fileName: String, fileSizeBytes: Long) {
        currentFileName = fileName
        currentFileSizeBytes = fileSizeBytes
        currentState = UploadState.READY

        val btnChoose = btnChoosePdf ?: findViewById(R.id.btnChoosePdf)
        val layoutSelected = layoutSelectedPdf ?: findViewById(R.id.layoutSelectedPdf)
        val tvName = tvFileName ?: findViewById(R.id.tvNewPdfName)
        val tvSize = tvFileSize ?: findViewById(R.id.tvNewPdfSize)
        val ivCheck = ivUploadCheckDone ?: findViewById(R.id.ivUploadCheckDone)
        val btnClear = btnClearSelectedPdf ?: findViewById(R.id.btnClearSelectedPdf)

        tvName?.text = fileName
        tvSize?.text = "${formatSize(fileSizeBytes)} · Ready"
        ivCheck?.visibility = View.GONE
        btnClear?.isEnabled = true

        if (!UiArcMotion.isMotionEnabled(context)) {
            btnChoose?.visibility = View.GONE
            layoutSelected?.visibility = View.VISIBLE
            layoutSelected?.alpha = 1f
            layoutSelected?.translationY = 0f
            return
        }

        btnChoose?.animate()
            ?.alpha(0f)
            ?.scaleX(0.96f)
            ?.scaleY(0.96f)
            ?.setDuration(160L)
            ?.setInterpolator(UiArcMotion.Ease.STANDARD)
            ?.withEndAction {
                btnChoose.visibility = View.GONE
                layoutSelected?.visibility = View.VISIBLE
                layoutSelected?.alpha = 0f
                layoutSelected?.translationY = 8f * density
                layoutSelected?.animate()
                    ?.alpha(1f)
                    ?.translationY(0f)
                    ?.setDuration(240L)
                    ?.setInterpolator(UiArcMotion.Ease.ENTER)
                    ?.start()
            }
            ?.start()
    }

    /**
     * Morphs selected file card back to the choose button.
     */
    fun clearSelectedFile(onCleared: () -> Unit = {}) {
        currentState = UploadState.IDLE
        currentFileName = ""
        currentFileSizeBytes = 0L

        val btnChoose = btnChoosePdf ?: findViewById(R.id.btnChoosePdf)
        val layoutSelected = layoutSelectedPdf ?: findViewById(R.id.layoutSelectedPdf)

        if (!UiArcMotion.isMotionEnabled(context)) {
            layoutSelected?.visibility = View.GONE
            btnChoose?.visibility = View.VISIBLE
            btnChoose?.alpha = 1f
            btnChoose?.scaleX = 1f
            btnChoose?.scaleY = 1f
            onCleared()
            return
        }

        layoutSelected?.animate()
            ?.alpha(0f)
            ?.translationY(-8f * density)
            ?.setDuration(160L)
            ?.setInterpolator(UiArcMotion.Ease.STANDARD)
            ?.withEndAction {
                layoutSelected.visibility = View.GONE
                btnChoose?.visibility = View.VISIBLE
                btnChoose?.alpha = 0f
                btnChoose?.scaleX = 0.96f
                btnChoose?.scaleY = 0.96f
                btnChoose?.animate()
                    ?.alpha(1f)
                    ?.scaleX(1f)
                    ?.scaleY(1f)
                    ?.setDuration(240L)
                    ?.setInterpolator(UiArcMotion.Ease.ENTER)
                    ?.withEndAction { onCleared() }
                    ?.start()
            }
            ?.start()
    }

    fun setUploading() {
        currentState = UploadState.UPLOADING
        btnClearSelectedPdf?.isEnabled = false
        tvFileSize?.text = "${formatSize(currentFileSizeBytes)} · Uploading…"
    }

    fun setUploadSuccess(onAnimationComplete: () -> Unit = {}) {
        currentState = UploadState.DONE
        tvFileSize?.text = "${formatSize(currentFileSizeBytes)} · Uploaded"
        val ivCheck = ivUploadCheckDone ?: findViewById(R.id.ivUploadCheckDone)
        ivCheck?.visibility = View.VISIBLE

        if (!UiArcMotion.isMotionEnabled(context)) {
            ivCheck?.alpha = 1f
            ivCheck?.scaleX = 1f
            ivCheck?.scaleY = 1f
            onAnimationComplete()
            return
        }

        ivCheck?.scaleX = 0.6f
        ivCheck?.scaleY = 0.6f
        ivCheck?.alpha = 0f
        ivCheck?.animate()
            ?.scaleX(1f)
            ?.scaleY(1f)
            ?.alpha(1f)
            ?.setDuration(CHECK_ENTRANCE_DURATION_MS)
            ?.setStartDelay(CHECK_ENTRANCE_DELAY_MS)
            ?.setInterpolator(UiArcMotion.Ease.ENTER)
            ?.withEndAction {
                onAnimationComplete()
            }
            ?.start()
    }

    fun setUploadFailed() {
        currentState = UploadState.FAILED
        btnClearSelectedPdf?.isEnabled = true
        tvFileSize?.text = "${formatSize(currentFileSizeBytes)} · Upload failed"
    }

    fun reset() {
        currentState = UploadState.IDLE
        currentFileName = ""
        currentFileSizeBytes = 0L

        val btnChoose = btnChoosePdf ?: findViewById(R.id.btnChoosePdf)
        val layoutSelected = layoutSelectedPdf ?: findViewById(R.id.layoutSelectedPdf)
        val ivCheck = ivUploadCheckDone ?: findViewById(R.id.ivUploadCheckDone)

        btnChoose?.visibility = View.VISIBLE
        btnChoose?.alpha = 1f
        btnChoose?.scaleX = 1f
        btnChoose?.scaleY = 1f
        layoutSelected?.visibility = View.GONE
        ivCheck?.visibility = View.GONE
    }

    fun getState(): UploadState = currentState
    fun getFileName(): String = currentFileName
    fun getFileSizeBytes(): Long = currentFileSizeBytes
}
