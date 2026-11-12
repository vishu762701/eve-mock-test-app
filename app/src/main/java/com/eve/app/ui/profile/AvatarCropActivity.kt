/*
 * Derived from Telegram Android PhotoCropActivity
 * (`PhotoCropActivity.java:40-520`), Copyright Nikolai Kudashov.
 * GNU GPL v2 or later. See THIRD_PARTY_NOTICES.md.
 */

package com.eve.app.ui.profile

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.core.content.FileProvider
import androidx.core.content.IntentCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.eve.app.R
import com.eve.app.databinding.ActivityAvatarCropBinding
import com.eve.app.ui.common.EveBaseActivity
import com.eve.app.util.AppBulletin
import com.eve.app.util.ProfilePhotoManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * ADAPTED PhotoCropActivity host. Telegram shows a crop square over the picked
 * bitmap and returns the cropped bitmap to ImageUpdater. EVE writes the square
 * as an up-to-800px quality-80 JPEG (the source `bigPhoto` export) into its
 * cache and returns that URI to the profile preview.
 */
class AvatarCropActivity : EveBaseActivity() {

    private lateinit var binding: ActivityAvatarCropBinding
    private var sourceUri: Uri? = null
    private var decoded: Bitmap? = null
    private var finishingCrop = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAvatarCropBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            view.setPadding(0, bars.top, 0, bars.bottom)
            insets
        }
        WindowCompat.getInsetsController(window, binding.root).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }

        sourceUri = IntentCompat.getParcelableExtra(intent, EXTRA_SOURCE_URI, Uri::class.java)
        if (sourceUri == null) {
            finish()
            return
        }

        binding.btnCancel.setOnClickListener { finish() }
        binding.btnDone.setOnClickListener { completeCrop() }
        binding.btnDone.isEnabled = false
        loadPhoto()
    }

    private fun loadPhoto() {
        val uri = sourceUri ?: return
        binding.progress.visibility = View.VISIBLE
        binding.cropView.visibility = View.INVISIBLE
        lifecycleScope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                ProfilePhotoManager.decodeForCrop(
                    this@AvatarCropActivity,
                    uri,
                    AvatarCropView.DECODE_MAX_DIMENSION
                )
            }
            if (isFinishing || isDestroyed) {
                bitmap?.recycle()
                return@launch
            }
            if (bitmap == null) {
                binding.progress.visibility = View.GONE
                AppBulletin.showError(
                    this@AvatarCropActivity,
                    getString(R.string.profile_crop_load_failed)
                )
                finish()
                return@launch
            }
            decoded = bitmap
            binding.cropView.setBitmap(bitmap)
            binding.cropView.visibility = View.VISIBLE
            binding.progress.visibility = View.GONE
            binding.btnDone.isEnabled = true
        }
    }

    private fun completeCrop() {
        if (finishingCrop || !binding.cropView.hasBitmap()) return
        val cropped = binding.cropView.cropBitmap()
        if (cropped == null) {
            AppBulletin.showError(this, getString(R.string.profile_crop_save_failed))
            return
        }
        finishingCrop = true
        binding.btnCancel.isEnabled = false
        binding.btnDone.isEnabled = false
        binding.progress.visibility = View.VISIBLE

        lifecycleScope.launch {
            val output = withContext(Dispatchers.IO) { writeCrop(cropped) }
            cropped.recycle()
            if (isFinishing || isDestroyed) {
                output?.delete()
                return@launch
            }
            if (output == null) {
                finishingCrop = false
                binding.btnCancel.isEnabled = true
                binding.btnDone.isEnabled = true
                binding.progress.visibility = View.GONE
                AppBulletin.showError(
                    this@AvatarCropActivity,
                    getString(R.string.profile_crop_save_failed)
                )
                return@launch
            }
            val uri = try {
                FileProvider.getUriForFile(
                    this@AvatarCropActivity,
                    "$packageName.fileprovider",
                    output
                )
            } catch (_: Exception) {
                null
            }
            if (uri == null) {
                output.delete()
                finishingCrop = false
                binding.btnCancel.isEnabled = true
                binding.btnDone.isEnabled = true
                binding.progress.visibility = View.GONE
                AppBulletin.showError(
                    this@AvatarCropActivity,
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
            FileOutputStream(temp).use { out ->
                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)) {
                    temp.delete()
                    return null
                }
            }
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

    override fun onDestroy() {
        binding.cropView.release()
        decoded?.recycle()
        decoded = null
        super.onDestroy()
    }

    companion object {
        const val EXTRA_SOURCE_URI = "com.eve.app.extra.AVATAR_CROP_SOURCE"
        const val EXTRA_OUTPUT_URI = "com.eve.app.extra.AVATAR_CROP_OUTPUT"
        const val EXTRA_OUTPUT_PATH = "com.eve.app.extra.AVATAR_CROP_OUTPUT_PATH"

        fun intent(activity: android.app.Activity, source: Uri): Intent =
            Intent(activity, AvatarCropActivity::class.java)
                .putExtra(EXTRA_SOURCE_URI, source)
    }
}
