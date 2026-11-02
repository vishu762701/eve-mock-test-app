package com.eve.app.ui.profile

import android.content.Context
import com.google.android.material.bottomsheet.BottomSheetDialog

/**
 * ADAPTED Telegram QR sheet close host. Back, drag and outside-tap
 * cancellation still belong to the existing Material dialog; its final
 * dismissal is deferred to the source Java/Kotlin closing compositor.
 */
internal class QrPickerMorphDialog(context: Context) : BottomSheetDialog(context) {
    private var sheetSurface: QrPickerSheetSurface? = null
    private var pickerGrid: QrEmojiPickerGrid? = null
    private var closing = false
    private var terminal = false

    fun bindSource(surface: QrPickerSheetSurface, grid: QrEmojiPickerGrid) {
        sheetSurface = surface
        pickerGrid = grid
    }

    override fun dismiss() = requestClose(false)
    override fun cancel() = requestClose(true)

    private fun requestClose(wasCancelled: Boolean) {
        if (terminal || !isShowing) {
            if (wasCancelled) super.cancel() else super.dismiss()
            return
        }
        if (closing) return
        closing = true
        pickerGrid?.animateSourceClose()
        val surface = sheetSurface
        if (surface == null) completeClose(wasCancelled)
        else surface.animateSourceClose { completeClose(wasCancelled) }
    }

    private fun completeClose(wasCancelled: Boolean) {
        if (terminal) return
        terminal = true
        sheetSurface?.dispose()
        // Android Dialog.cancel may invoke dismiss polymorphically.
        // The terminal flag prevents another source close animation.
        if (wasCancelled) super.cancel() else super.dismiss()
    }

    /** Never keep a dying Activity's window for the closing animation. */
    fun dismissImmediately() {
        terminal = true
        sheetSurface?.dispose()
        super.dismiss()
    }
}
