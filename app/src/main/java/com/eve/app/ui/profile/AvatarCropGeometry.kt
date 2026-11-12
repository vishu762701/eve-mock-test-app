/*
 * Derived from Telegram Android PhotoCropActivity.PhotoCropView
 * (`PhotoCropActivity.java:40-360`), Copyright Nikolai Kudashov.
 * GNU GPL v2 or later. See THIRD_PARTY_NOTICES.md.
 */

package com.eve.app.ui.profile

import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Pure crop geometry shared by the avatar crop view and JVM tests. Telegram's
 * PhotoCropView keeps the avatar selection square, clamps it inside the fitted
 * bitmap and maps view coordinates back to source pixels before cropping
 * (`PhotoCropActivity.java:220-335`). Floating point boxes make that contract
 * testable without an Android sandbox.
 */
internal object AvatarCropGeometry {

    data class Box(
        val left: Float,
        val top: Float,
        val right: Float,
        val bottom: Float
    ) {
        val width: Float get() = right - left
        val height: Float get() = bottom - top
        val isValid: Boolean get() = width > 1f && height > 1f
    }

    data class IntBox(val x: Int, val y: Int, val width: Int, val height: Int)

    enum class Corner { TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }

    /**
     * Source `updateBitmapSize`: scale the bitmap to fit the view with a 14dp
     * inset and center it, preserving aspect ratio
     * (`PhotoCropActivity.java:224-250`).
     */
    fun fitImage(
        imageWidth: Int,
        imageHeight: Int,
        viewWidth: Int,
        viewHeight: Int,
        inset: Float
    ): Box {
        if (imageWidth <= 0 || imageHeight <= 0) return Box(0f, 0f, 0f, 0f)
        val availableWidth = (viewWidth - inset * 2f).coerceAtLeast(1f)
        val availableHeight = (viewHeight - inset * 2f).coerceAtLeast(1f)
        val scale = min(availableWidth / imageWidth, availableHeight / imageHeight)
        val width = imageWidth * scale
        val height = imageHeight * scale
        val left = inset + (availableWidth - width) / 2f
        val top = inset + (availableHeight - height) / 2f
        return Box(left, top, left + width, top + height)
    }

    /** Source initial avatar square: the shorter fitted side, centered. */
    fun initialSquare(image: Box): Box {
        if (!image.isValid) return Box(0f, 0f, 0f, 0f)
        val side = min(image.width, image.height)
        val left = image.left + (image.width - side) / 2f
        val top = image.top + (image.height - side) / 2f
        return Box(left, top, left + side, top + side)
    }

    /** Source draggingState == 5: move the square without leaving the bitmap. */
    fun move(crop: Box, image: Box, dx: Float, dy: Float): Box {
        if (!crop.isValid || !image.isValid) return crop
        val left = (crop.left + dx).coerceIn(image.left, image.right - crop.width)
        val top = (crop.top + dy).coerceIn(image.top, image.bottom - crop.height)
        return Box(left, top, left + crop.width, top + crop.height)
    }

    /**
     * Source corner resize (`PhotoCropActivity.java:118-212`), keeping the
     * opposite corner fixed and the selection square. The dominant drag axis
     * chooses the new side so diagonal drags feel predictable.
     */
    fun resize(
        crop: Box,
        image: Box,
        corner: Corner,
        dx: Float,
        dy: Float,
        minimumSide: Float
    ): Box {
        if (!crop.isValid || !image.isValid) return crop
        val horizontal = when (corner) {
            Corner.TOP_LEFT, Corner.BOTTOM_LEFT -> crop.width - dx
            Corner.TOP_RIGHT, Corner.BOTTOM_RIGHT -> crop.width + dx
        }
        val vertical = when (corner) {
            Corner.TOP_LEFT, Corner.TOP_RIGHT -> crop.height - dy
            Corner.BOTTOM_LEFT, Corner.BOTTOM_RIGHT -> crop.height + dy
        }
        val proposed = if (abs(dx) >= abs(dy)) horizontal else vertical

        val anchorX = when (corner) {
            Corner.TOP_LEFT, Corner.BOTTOM_LEFT -> crop.right
            Corner.TOP_RIGHT, Corner.BOTTOM_RIGHT -> crop.left
        }
        val anchorY = when (corner) {
            Corner.TOP_LEFT, Corner.TOP_RIGHT -> crop.bottom
            Corner.BOTTOM_LEFT, Corner.BOTTOM_RIGHT -> crop.top
        }
        val maxSide = min(
            if (corner == Corner.TOP_LEFT || corner == Corner.BOTTOM_LEFT) {
                anchorX - image.left
            } else {
                image.right - anchorX
            },
            if (corner == Corner.TOP_LEFT || corner == Corner.TOP_RIGHT) {
                anchorY - image.top
            } else {
                image.bottom - anchorY
            }
        )
        if (maxSide <= 0f) return crop
        val side = proposed.coerceIn(min(minimumSide, maxSide), maxSide)

        return when (corner) {
            Corner.TOP_LEFT -> Box(anchorX - side, anchorY - side, anchorX, anchorY)
            Corner.TOP_RIGHT -> Box(anchorX, anchorY - side, anchorX + side, anchorY)
            Corner.BOTTOM_LEFT -> Box(anchorX - side, anchorY, anchorX, anchorY + side)
            Corner.BOTTOM_RIGHT -> Box(anchorX, anchorY, anchorX + side, anchorY + side)
        }
    }

    /**
     * Source `getBitmap` maps view coordinates to source pixels
     * (`PhotoCropActivity.java:288-310`). The upstream implementation divides
     * both axes by bitmap width; EVE maps each axis independently so non-square
     * photos cannot overshoot the source height.
     */
    fun mapToImage(crop: Box, image: Box, imageWidth: Int, imageHeight: Int): IntBox {
        if (!crop.isValid || !image.isValid || imageWidth <= 0 || imageHeight <= 0) {
            return IntBox(0, 0, 0, 0)
        }
        val scaleX = imageWidth / image.width
        val scaleY = imageHeight / image.height
        val x = ((crop.left - image.left) * scaleX).roundToInt().coerceIn(0, imageWidth - 1)
        val y = ((crop.top - image.top) * scaleY).roundToInt().coerceIn(0, imageHeight - 1)
        val width = (crop.width * scaleX).roundToInt().coerceIn(1, imageWidth - x)
        val height = (crop.height * scaleY).roundToInt().coerceIn(1, imageHeight - y)
        return IntBox(x, y, width, height)
    }
}
