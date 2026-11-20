/* EVE rendering adapter for Telegram ProfileActivity.java:6357-6450,8456-8520.
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
import kotlin.math.min
import kotlin.math.roundToInt

/** Single-photo EVE host; ports ProfileActivity.java:6357-6405 quadratic Bezier trajectory & 138dp pre-latch zoom. */
internal object ProfileHeaderExpansion {
    fun apply(binding: ActivityProfileBinding, fraction: Float, statusTop: Int) {
        val f = fraction.coerceIn(0f, 1f)
        binding.layoutActions.setParentExpanded(f)
        val density = binding.root.resources.displayMetrics.density
        val rootWidth = binding.root.width.coerceAtLeast((100 * density).toInt()).toFloat()
        // ProfileActivity.java:8457: pre-latch 0..0.33f zooms circular avatar toward 138dp, then morphs to full square.
        val preLatch = (f * 3f).coerceIn(0f, 1f)
        val morph = if (f <= 0.33f) 0f else ((f - 0.33f) / 0.67f).coerceIn(0f, 1f)
        val preLatchSize = 100f * density + 38f * density * preLatch
        val size = (preLatchSize + (rootWidth - preLatchSize) * morph).roundToInt()
        val image = binding.avatarContainer
        val lp = image.layoutParams as FrameLayout.LayoutParams
        val top = ((35 * density) * (1f - f)).roundToInt() + statusTop
        if (lp.width != size || lp.height != size || lp.topMargin != top) {
            lp.width = size; lp.height = size; lp.topMargin = top; image.layoutParams = lp
        }
        val photoLp = binding.ivProfilePhoto.layoutParams
        if (photoLp.width != size || photoLp.height != size) {
            photoLp.width = size; photoLp.height = size; binding.ivProfilePhoto.layoutParams = photoLp
        }
        val cornerRadius = (size / 2f) * (1f - morph)
        binding.ivProfilePhoto.shapeAppearanceModel = binding.ivProfilePhoto.shapeAppearanceModel.toBuilder()
            .setAllCornerSizes(cornerRadius).build()
        val outerLp = binding.avatarOuterContainer.layoutParams
        val height = (84 * density + binding.profileScrollView.maximumPull * f).roundToInt()
        if (outerLp.height != height) {
            outerLp.height = height; binding.avatarOuterContainer.layoutParams = outerLp
        }
        binding.profilePhotoScrim.visibility = if (morph > 0f) View.VISIBLE else View.GONE
        if (binding.profilePhotoScrim.background == null) {
            binding.profilePhotoScrim.background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(0x55000000, Color.TRANSPARENT, Color.TRANSPARENT, 0xCC000000.toInt(), Color.BLACK)
            )
        }
        (binding.profilePhotoScrim.background as GradientDrawable).cornerRadius = cornerRadius
        binding.profilePhotoScrim.alpha = morph

        // ProfileActivity.java:6357-6399: Quadratic Bezier (kx=8dp, ky=8dp) from centered header to bottom-left (18dp/16dp)
        val statusBottom = size - 10f * density
        val nameBottom = minOf(size - 30f * density, statusBottom - binding.tvStatus.height - 2f * density)
        val endNameY = nameBottom - binding.tvName.bottom
        val endStatusY = statusBottom - binding.tvStatus.bottom
        val isRtl = binding.root.layoutDirection == View.LAYOUT_DIRECTION_RTL
        val maxScale = if (binding.tvName.width > 0) {
            min(1.38f, ((rootWidth - 36f * density) / binding.tvName.width.toFloat()).coerceAtLeast(1f))
        } else 1.38f
        val targetScale = 1f + (maxScale - 1f) * morph
        binding.tvName.pivotX = if (isRtl) binding.tvName.width.toFloat() else 0f
        binding.tvName.pivotY = binding.tvName.height.toFloat() * morph
        binding.tvName.scaleX = targetScale
        binding.tvName.scaleY = targetScale

        val endNameX = if (isRtl) {
            (rootWidth - 18f * density) - binding.tvName.right
        } else {
            18f * density - binding.tvName.left
        }
        val endStatusX = if (isRtl) {
            (rootWidth - 16f * density) - binding.tvStatus.right
        } else {
            16f * density - binding.tvStatus.left
        }
        val kx = (if (isRtl) -8f else 8f) * density
        val ky = 8f * density
        val oneMinusM = 1f - morph
        val preLatchPushY = (size - 100f * density) * (1f - morph)
        val nameCx = kx + endNameX * 0.5f
        val nameCy = ky + endNameY * 0.5f
        val statusCx = kx + endStatusX * 0.5f
        val statusCy = ky + endStatusY * 0.5f
        binding.tvName.translationX = 2f * oneMinusM * morph * nameCx + morph * morph * endNameX
        binding.tvName.translationY = preLatchPushY + 2f * oneMinusM * morph * nameCy + morph * morph * endNameY
        binding.tvStatus.translationX = 2f * oneMinusM * morph * statusCx + morph * morph * endStatusX
        binding.tvStatus.translationY = preLatchPushY + 2f * oneMinusM * morph * statusCy + morph * morph * endStatusY

        val contentOffset = (size - binding.layoutActions.top) * f
        val column = binding.layoutActions.parent as android.view.ViewGroup
        var following = false
        for (index in 0 until column.childCount) {
            val child = column.getChildAt(index)
            if (child === binding.layoutActions) following = true
            if (following) child.translationY = contentOffset
        }
        val palette = TelegramThemeEngine.resolveCurrentPalette(binding.root.context)
        binding.tvName.setTextColor(ColorUtils.blendARGB(palette.textPrimary, Color.WHITE, morph))
        val baseStatusColor = palette.textSecondary
        // ProfileActivity.java:6394 blends subtitle to 0xB3FFFFFF on expanded photo
        binding.tvStatus.setTextColor(ColorUtils.blendARGB(baseStatusColor, 0xB3FFFFFF.toInt(), morph))
        val ink = ColorStateList.valueOf(ColorUtils.blendARGB(palette.textPrimary, Color.WHITE, morph))
        binding.btnBack.imageTintList = ink
        binding.btnProfileQr.imageTintList = ink
        binding.btnProfileMenu.imageTintList = ink
        val chipFade = (1f - morph * 2f).coerceIn(0f, 1f)
        if (f > 0f) {
            image.scaleX = 1f; image.scaleY = 1f; image.translationY = 0f
            if (f <= 0.33f) {
                binding.avatarGooey.setGooeyEnabled(true)
                binding.avatarGooey.setPullProgress(0.35f * (f / 0.33f))
                binding.avatarGooey.setBlurIntensity(0f)
            } else {
                binding.avatarGooey.setGooeyEnabled(false)
                binding.avatarGooey.setBlurIntensity(0f)
            }
            binding.tvName.alpha = 1f; binding.tvStatus.alpha = 1f
            binding.tvToolbarTitle.alpha = 0f; binding.tvToolbarSubtitle.alpha = 0f
            binding.chipAdmin.alpha = chipFade; binding.chipPremium.alpha = chipFade
        }
        binding.avatarTouchTarget.contentDescription = if (f > 0f) "Collapse profile photo" else "Profile photo options"
    }

    fun apply(binding: ProfileTelegramViewTree, fraction: Float, statusTop: Int) {
        val f = fraction.coerceIn(0f, 1f)
        binding.layoutActions.setParentExpanded(f)
        val density = binding.root.resources.displayMetrics.density
        val rootWidth = binding.root.width.coerceAtLeast((100 * density).toInt()).toFloat()
        // ProfileActivity.java:8457: pre-latch 0..0.33f zooms circular avatar toward 138dp, then morphs to full square.
        val preLatch = (f * 3f).coerceIn(0f, 1f)
        val morph = if (f <= 0.33f) 0f else ((f - 0.33f) / 0.67f).coerceIn(0f, 1f)
        val preLatchSize = 100f * density + 38f * density * preLatch
        val size = (preLatchSize + (rootWidth - preLatchSize) * morph).roundToInt()
        val image = binding.avatarContainer
        val lp = image.layoutParams as FrameLayout.LayoutParams
        val top = ((35 * density) * (1f - f)).roundToInt() + statusTop
        if (lp.width != size || lp.height != size || lp.topMargin != top) {
            lp.width = size; lp.height = size; lp.topMargin = top; image.layoutParams = lp
        }
        val photoLp = binding.ivProfilePhoto.layoutParams
        if (photoLp.width != size || photoLp.height != size) {
            photoLp.width = size; photoLp.height = size; binding.ivProfilePhoto.layoutParams = photoLp
        }
        val cornerRadius = (size / 2f) * (1f - morph)
        binding.ivProfilePhoto.shapeAppearanceModel = binding.ivProfilePhoto.shapeAppearanceModel.toBuilder()
            .setAllCornerSizes(cornerRadius).build()
        val outerLp = binding.avatarOuterContainer.layoutParams
        val height = (84 * density + binding.profileScrollView.maximumPull * f).roundToInt()
        if (outerLp.height != height) {
            outerLp.height = height; binding.avatarOuterContainer.layoutParams = outerLp
        }
        binding.profilePhotoScrim.visibility = if (morph > 0f) View.VISIBLE else View.GONE
        if (binding.profilePhotoScrim.background == null) {
            binding.profilePhotoScrim.background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(0x55000000, Color.TRANSPARENT, Color.TRANSPARENT, 0xCC000000.toInt(), Color.BLACK)
            )
        }
        (binding.profilePhotoScrim.background as GradientDrawable).cornerRadius = cornerRadius
        binding.profilePhotoScrim.alpha = morph

        // ProfileActivity.java:6357-6399: Quadratic Bezier (kx=8dp, ky=8dp) from centered header to bottom-left (18dp/16dp)
        val statusBottom = size - 10f * density
        val nameBottom = minOf(size - 30f * density, statusBottom - binding.tvStatus.height - 2f * density)
        val endNameY = nameBottom - binding.tvName.bottom
        val endStatusY = statusBottom - binding.tvStatus.bottom
        val isRtl = binding.root.layoutDirection == View.LAYOUT_DIRECTION_RTL
        val maxScale = if (binding.tvName.width > 0) {
            min(1.38f, ((rootWidth - 36f * density) / binding.tvName.width.toFloat()).coerceAtLeast(1f))
        } else 1.38f
        val targetScale = 1f + (maxScale - 1f) * morph
        binding.tvName.pivotX = if (isRtl) binding.tvName.width.toFloat() else 0f
        binding.tvName.pivotY = binding.tvName.height.toFloat() * morph
        binding.tvName.scaleX = targetScale
        binding.tvName.scaleY = targetScale

        val endNameX = if (isRtl) {
            (rootWidth - 18f * density) - binding.tvName.right
        } else {
            18f * density - binding.tvName.left
        }
        val endStatusX = if (isRtl) {
            (rootWidth - 16f * density) - binding.tvStatus.right
        } else {
            16f * density - binding.tvStatus.left
        }
        val kx = (if (isRtl) -8f else 8f) * density
        val ky = 8f * density
        val oneMinusM = 1f - morph
        val preLatchPushY = (size - 100f * density) * (1f - morph)
        val nameCx = kx + endNameX * 0.5f
        val nameCy = ky + endNameY * 0.5f
        val statusCx = kx + endStatusX * 0.5f
        val statusCy = ky + endStatusY * 0.5f
        binding.tvName.translationX = 2f * oneMinusM * morph * nameCx + morph * morph * endNameX
        binding.tvName.translationY = preLatchPushY + 2f * oneMinusM * morph * nameCy + morph * morph * endNameY
        binding.tvStatus.translationX = 2f * oneMinusM * morph * statusCx + morph * morph * endStatusX
        binding.tvStatus.translationY = preLatchPushY + 2f * oneMinusM * morph * statusCy + morph * morph * endStatusY

        val contentOffset = (size - binding.layoutActions.top) * f
        val column = binding.layoutActions.parent as android.view.ViewGroup
        var following = false
        for (index in 0 until column.childCount) {
            val child = column.getChildAt(index)
            if (child === binding.layoutActions) following = true
            if (following) child.translationY = contentOffset
        }
        val palette = TelegramThemeEngine.resolveCurrentPalette(binding.root.context)
        binding.tvName.setTextColor(ColorUtils.blendARGB(palette.textPrimary, Color.WHITE, morph))
        val baseStatusColor = palette.textSecondary
        // ProfileActivity.java:6394 blends subtitle to 0xB3FFFFFF on expanded photo
        binding.tvStatus.setTextColor(ColorUtils.blendARGB(baseStatusColor, 0xB3FFFFFF.toInt(), morph))
        val ink = ColorStateList.valueOf(ColorUtils.blendARGB(palette.textPrimary, Color.WHITE, morph))
        binding.btnBack.imageTintList = ink
        binding.btnProfileQr.imageTintList = ink
        binding.btnProfileMenu.imageTintList = ink
        val chipFade = (1f - morph * 2f).coerceIn(0f, 1f)
        if (f > 0f) {
            image.scaleX = 1f; image.scaleY = 1f; image.translationY = 0f
            if (f <= 0.33f) {
                binding.avatarGooey.setGooeyEnabled(true)
                binding.avatarGooey.setPullProgress(0.35f * (f / 0.33f))
                binding.avatarGooey.setBlurIntensity(0f)
            } else {
                binding.avatarGooey.setGooeyEnabled(false)
                binding.avatarGooey.setBlurIntensity(0f)
            }
            binding.tvName.alpha = 1f; binding.tvStatus.alpha = 1f
            binding.tvToolbarTitle.alpha = 0f; binding.tvToolbarSubtitle.alpha = 0f
            binding.chipAdmin.alpha = chipFade; binding.chipPremium.alpha = chipFade
        }
        binding.avatarTouchTarget.contentDescription = if (f > 0f) "Collapse profile photo" else "Profile photo options"
    }
}
