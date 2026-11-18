/*
 * Telegram: TMessagesProj/src/main/java/org/telegram/ui/Cells/SharedDocumentCell.java:40-260 @ 3de37c8cfb03
 * Derived from Telegram Android ChatAttachAlert.java and DocumentObject.java
 * DrKLO/Telegram@21f7d109da6ab3506c9e9577f98c6a38771ab335
 * Copyright Nikolai Kudashov, 2013-2018; GNU GPL v2 or later. See THIRD_PARTY_NOTICES.md.
 */

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
import com.eve.app.telegram.animation.TelegramAnimationRuntime
import org.telegram.ui.Components.CubicBezierInterpolator
import java.util.Locale
import kotlin.math.roundToInt

/**
 * 1:1 Telegram file upload component.
 * Genuine state machine: IDLE -> READY -> UPLOADING -> DONE / FAILED (Retry).
 * Morphing transitions with CubicBezierInterpolator physics.
 */
open class TelegramFileUploadView @JvmOverloads constructor(
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

        if (!TelegramAnimationRuntime.isMotionEnabled(context)) {
            btnChoose?.visibility = View.GONE
            layoutSelected?.visibility = View.VISIBLE
            layoutSelected?.alpha = 1f
            layoutSelected?.translationY = 0f
            return
        }

        btnChoose?.animate()?.cancel()
        btnChoose?.animate()
            ?.scaleX(0.96f)
            ?.scaleY(0.96f)
            ?.alpha(0f)
            ?.setDuration(160L)
            ?.setInterpolator(CubicBezierInterpolator.DEFAULT)
            ?.setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    btnChoose.visibility = View.GONE
                    layoutSelected?.visibility = View.VISIBLE
                    layoutSelected?.alpha = 0f
                    layoutSelected?.translationY = 8f * density
                    layoutSelected?.animate()
                        ?.alpha(1f)
                        ?.translationY(0f)
                        ?.setDuration(240L)
                        ?.setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT)
                        ?.start()
                }
            })
            ?.start()
    }

    fun setUploading() {
        currentState = UploadState.UPLOADING
        val tvSize = tvFileSize ?: findViewById(R.id.tvNewPdfSize)
        val btnClear = btnClearSelectedPdf ?: findViewById(R.id.btnClearSelectedPdf)
        tvSize?.text = "${formatSize(currentFileSizeBytes)} · Uploading…"
        btnClear?.isEnabled = false
    }

    fun setUploadSuccess(onComplete: () -> Unit = {}) {
        currentState = UploadState.DONE
        val tvSize = tvFileSize ?: findViewById(R.id.tvNewPdfSize)
        val ivCheck = ivUploadCheckDone ?: findViewById(R.id.ivUploadCheckDone)
        val btnClear = btnClearSelectedPdf ?: findViewById(R.id.btnClearSelectedPdf)

        tvSize?.text = "${formatSize(currentFileSizeBytes)} · Uploaded"
        btnClear?.isEnabled = false

        if (!TelegramAnimationRuntime.isMotionEnabled(context)) {
            ivCheck?.visibility = View.VISIBLE
            ivCheck?.scaleX = 1f
            ivCheck?.scaleY = 1f
            ivCheck?.alpha = 1f
            onComplete()
            return
        }

        ivCheck?.visibility = View.VISIBLE
        ivCheck?.scaleX = 0.6f
        ivCheck?.scaleY = 0.6f
        ivCheck?.alpha = 0f
        ivCheck?.animate()?.cancel()
        ivCheck?.animate()
            ?.scaleX(1.0f)
            ?.scaleY(1.0f)
            ?.alpha(1.0f)
            ?.setStartDelay(CHECK_ENTRANCE_DELAY_MS)
            ?.setDuration(CHECK_ENTRANCE_DURATION_MS)
            ?.setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT)
            ?.setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    onComplete()
                }
            })
            ?.start()
    }

    fun setUploadFailed() {
        currentState = UploadState.FAILED
        val tvSize = tvFileSize ?: findViewById(R.id.tvNewPdfSize)
        val btnClear = btnClearSelectedPdf ?: findViewById(R.id.btnClearSelectedPdf)
        tvSize?.text = "${formatSize(currentFileSizeBytes)} · Failed (Tap Clear to retry)"
        btnClear?.isEnabled = true
    }

    fun clearSelectedFile(onCleared: () -> Unit = {}) {
        currentFileName = ""
        currentFileSizeBytes = 0L
        currentState = UploadState.IDLE

        val btnChoose = btnChoosePdf ?: findViewById(R.id.btnChoosePdf)
        val layoutSelected = layoutSelectedPdf ?: findViewById(R.id.layoutSelectedPdf)
        val ivCheck = ivUploadCheckDone ?: findViewById(R.id.ivUploadCheckDone)

        ivCheck?.visibility = View.GONE

        if (!TelegramAnimationRuntime.isMotionEnabled(context)) {
            layoutSelected?.visibility = View.GONE
            btnChoose?.visibility = View.VISIBLE
            btnChoose?.alpha = 1f
            btnChoose?.scaleX = 1f
            btnChoose?.scaleY = 1f
            onCleared()
            return
        }

        layoutSelected?.animate()?.cancel()
        layoutSelected?.animate()
            ?.alpha(0f)
            ?.translationY(-8f * density)
            ?.setDuration(160L)
            ?.setInterpolator(CubicBezierInterpolator.DEFAULT)
            ?.setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
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
                        ?.setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT)
                        ?.setListener(object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator) {
                                onCleared()
                            }
                        })
                        ?.start()
                }
            })
            ?.start()
    }

    fun reset() {
        clearSelectedFile()
    }
}
