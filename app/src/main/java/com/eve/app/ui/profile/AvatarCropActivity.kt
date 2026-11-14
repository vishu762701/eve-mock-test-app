package com.eve.app.ui.profile

import android.net.Uri
import android.content.Intent

/**
 * Compatibility subclass for PhotoCropActivity.
 */
open class AvatarCropActivity : PhotoCropActivity() {
    companion object {
        const val EXTRA_SOURCE_URI = PhotoCropActivity.EXTRA_SOURCE_URI
        const val EXTRA_OUTPUT_URI = PhotoCropActivity.EXTRA_OUTPUT_URI
        const val EXTRA_OUTPUT_PATH = PhotoCropActivity.EXTRA_OUTPUT_PATH

        fun intent(activity: android.app.Activity, source: Uri): Intent =
            PhotoCropActivity.intent(activity, source)
    }
}
