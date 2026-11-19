/*
 * Derived from Telegram Android PhotoCropActivity.java @ 21f7d109da6ab3506c9e9577f98c6a38771ab335
 * Copyright Nikolai Kudashov, 2013-2023. GNU GPL v2 or later. See THIRD_PARTY_NOTICES.md.
 */

package com.eve.app.ui.profile

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import androidx.core.content.FileProvider
import androidx.core.content.IntentCompat
import androidx.lifecycle.lifecycleScope
import com.eve.app.R
import com.eve.app.ui.common.TelegramHostActivity
import com.eve.app.util.AppBulletin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Hosts the ported [org.telegram.ui.PhotoCropActivity] and exports the cropped
 * square avatar as an up-to-800px quality-80 JPEG via FileProvider.
 */
open class PhotoCropActivity : TelegramHostActivity<org.telegram.ui.PhotoCropActivity>() {

    private var sourceUri: Uri? = null
    private var finishingCrop = false

    override fun onCreate(savedInstanceState: Bundle?) {
        sourceUri = IntentCompat.getParcelableExtra(intent, EXTRA_SOURCE_URI, Uri::class.java)
        if (sourceUri == null) {
            super.onCreate(savedInstanceState)
            finish()
            return
        }
        super.onCreate(savedInstanceState)
    }

    override fun createFragment(): org.telegram.ui.PhotoCropActivity {
        val args = Bundle().apply {
            sourceUri?.let { putParcelable("photoUri", it) }
            putBoolean("freeform", false)
        }
        return org.telegram.ui.PhotoCropActivity(args).apply {
            setDelegate { bitmap ->
                handleCroppedBitmap(bitmap)
            }
        }
    }

    private fun handleCroppedBitmap(bitmap: Bitmap?) {
        if (finishingCrop) return
        if (bitmap == null) {
            AppBulletin.showError(this, getString(R.string.profile_crop_save_failed))
            return
        }
        finishingCrop = true
        lifecycleScope.launch {
            val output = withContext(Dispatchers.IO) { writeCrop(bitmap) }
            if (isFinishing || isDestroyed) {
                output?.delete()
                return@launch
            }
            if (output == null) {
                finishingCrop = false
                AppBulletin.showError(
                    this@PhotoCropActivity,
                    getString(R.string.profile_crop_save_failed)
                )
                return@launch
            }
            val uri = try {
                FileProvider.getUriForFile(
                    this@PhotoCropActivity,
                    "$packageName.fileprovider",
                    output
                )
            } catch (_: Exception) {
                null
            }
            if (uri == null) {
                output.delete()
                finishingCrop = false
                AppBulletin.showError(
                    this@PhotoCropActivity,
                    getString(R.string.profile_crop_save_failed)
                )
                return@launch
            }
            val data = Intent().apply {
                data = uri
                putExtra(EXTRA_OUTPUT_URI, uri)
                putExtra(EXTRA_OUTPUT_PATH, output.absolutePath)
            }
            setResult(RESULT_OK, data)
            finish()
        }
    }

    private fun writeCrop(bitmap: Bitmap): File? {
        val output = File(cacheDir, "avatar_crop_${System.currentTimeMillis()}.jpg")
        val temp = File(output.parentFile, output.name + ".tmp")
        return try {
            val scaled = if (bitmap.width > MAX_EXPORT_PX || bitmap.height > MAX_EXPORT_PX) {
                val scale = MAX_EXPORT_PX.toFloat() / maxOf(bitmap.width, bitmap.height).toFloat()
                Bitmap.createScaledBitmap(
                    bitmap,
                    (bitmap.width * scale).toInt().coerceAtLeast(1),
                    (bitmap.height * scale).toInt().coerceAtLeast(1),
                    true
                )
            } else {
                bitmap
            }
            FileOutputStream(temp).use { out ->
                if (!scaled.compress(Bitmap.CompressFormat.JPEG, 80, out)) {
                    if (scaled !== bitmap) scaled.recycle()
                    temp.delete()
                    return null
                }
            }
            if (scaled !== bitmap) scaled.recycle()
            if (!temp.renameTo(output)) {
                temp.delete()
                return null
            }
            output
        } catch (_: Exception) {
            temp.delete()
            null
        }
    }

    companion object {
        private const val MAX_EXPORT_PX = 800
        const val EXTRA_SOURCE_URI = "com.eve.app.extra.AVATAR_CROP_SOURCE"
        const val EXTRA_OUTPUT_URI = "com.eve.app.extra.AVATAR_CROP_OUTPUT"
        const val EXTRA_OUTPUT_PATH = "com.eve.app.extra.AVATAR_CROP_OUTPUT_PATH"

        fun intent(activity: android.app.Activity, source: Uri): Intent =
            Intent(activity, PhotoCropActivity::class.java)
                .putExtra(EXTRA_SOURCE_URI, source)
    }
}
