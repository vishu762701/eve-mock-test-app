/* EVE rendering adapter for Telegram ProfileActivity.java:6375-6450,8465-8520.
 * Copyright Nikolai Kudashov / Telegram contributors, GPL v2 or later. */
package com.eve.app.ui.profile

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.FrameLayout
import androidx.core.graphics.ColorUtils
import com.eve.app.databinding.ActivityProfileBinding
import com.eve.app.theme.TelegramThemeEngine
import kotlin.math.roundToInt

/** Single-photo EVE host; Telegram's gallery/account/network subsystem is excluded. */
internal object ProfileHeaderExpansion {
    fun apply(binding: ActivityProfileBinding, fraction: Float, statusTop: Int) {
        val f = fraction.coerceIn(0f, 1f)
        val density = binding.root.resources.displayMetrics.density
        val size = (100 * density + (binding.root.width.coerceAtLeast((100 * density).toInt()) - 100 * density) * f).roundToInt()
        val image = binding.avatarContainer
        val lp = image.layoutParams as FrameLayout.LayoutParams
        val top = ((66 * density) * (1 - f)).roundToInt() + statusTop
        if (lp.width != size || lp.height != size || lp.topMargin != top) {
            lp.width = size; lp.height = size; lp.topMargin = top; image.layoutParams = lp
        }
        val photoLp = binding.ivProfilePhoto.layoutParams
        if (photoLp.width != size || photoLp.height != size) { photoLp.width = size; photoLp.height = size; binding.ivProfilePhoto.layoutParams = photoLp }
        binding.ivProfilePhoto.shapeAppearanceModel = binding.ivProfilePhoto.shapeAppearanceModel.toBuilder()
            .setAllCornerSizes(size / 2f * (1 - f)).build()
        val outerLp = binding.avatarOuterContainer.layoutParams
        val height = (120 * density + binding.profileScrollView.maximumPull * f).roundToInt()
        if (outerLp.height != height) { outerLp.height = height; binding.avatarOuterContainer.layoutParams = outerLp }
        binding.profilePhotoScrim.visibility = if (f > 0) View.VISIBLE else View.GONE
        if (binding.profilePhotoScrim.background == null) binding.profilePhotoScrim.background = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(0x88000000.toInt(), Color.TRANSPARENT, 0xDD000000.toInt()))
        (binding.profilePhotoScrim.background as GradientDrawable).cornerRadius = size / 2f * (1 - f)
        binding.profilePhotoScrim.alpha = f
        val statusBottom = size - 10 * density
        val nameBottom = minOf(size - 30 * density, statusBottom - binding.tvStatus.height - 2 * density)
        binding.tvName.translationY = (nameBottom - binding.tvName.bottom) * f
        binding.tvStatus.translationY = (statusBottom - binding.tvStatus.bottom) * f
        val contentOffset = (size - binding.layoutActions.top) * f
        val column = binding.layoutActions.parent as android.view.ViewGroup
        var following = false
        for (index in 0 until column.childCount) {
            val child = column.getChildAt(index)
            if (child === binding.layoutActions) following = true
            if (following) child.translationY = contentOffset
        }
        val palette = TelegramThemeEngine.resolveCurrentPalette(binding.root.context)
        binding.tvName.setTextColor(ColorUtils.blendARGB(palette.textPrimary, Color.WHITE, f))
        binding.tvStatus.setTextColor(ColorUtils.blendARGB(palette.textSecondary, Color.WHITE, f))
        val ink = ColorStateList.valueOf(ColorUtils.blendARGB(palette.textPrimary, Color.WHITE, f))
        binding.btnBack.imageTintList = ink; binding.btnProfileMenu.imageTintList = ink
        if (f > 0) {
            image.scaleX = 1f; image.scaleY = 1f; image.translationY = 0f
            binding.avatarGooey.setGooeyEnabled(false); binding.avatarGooey.setBlurIntensity(0f)
            binding.tvName.alpha = 1f; binding.tvStatus.alpha = 1f; binding.tvToolbarTitle.alpha = 0f
        }
        binding.avatarTouchTarget.contentDescription = if (f > 0) "Collapse profile photo" else "Profile photo options"
    }
}
