/*
 * Derived from Telegram Android NotchInfoUtils.java, Copyright Nikolai Kudashov.
 * GNU GPL v2 or later. See THIRD_PARTY_NOTICES.md.
 */

package com.eve.app.ui.profile

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Matrix
import android.graphics.Path
import android.graphics.RectF
import android.os.Build
import android.view.Gravity
import androidx.core.graphics.PathParser
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object NotchInfoUtils {
    private const val BOTTOM_MARKER = "@bottom"
    private const val DP_MARKER = "@dp"
    private const val RIGHT_MARKER = "@right"
    private const val LEFT_MARKER = "@left"

    class NotchInfo {
        var gravity: Int = Gravity.CENTER
        var isAccurate: Boolean = false
        var isLikelyCircle: Boolean = false
        var path: Path = Path()
        var bounds: RectF = RectF()
        var rawPath: String = ""
    }

    @SuppressLint("DiscouragedApi", "RestrictedApi")
    fun getInfo(ctx: Context, displayCutout: Any? = null): NotchInfo? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return null
        }

        val res = ctx.resources
        val m = res.displayMetrics
        val displayWidth = m.widthPixels
        val density = m.density
        val dp2 = 2f * density
        val dp32 = 32f * density

        val r = res.getIdentifier("config_mainBuiltInDisplayCutout", "string", "android")
        if (r != 0) {
            var spec = ctx.getString(r).trim()
            if (spec.isNotEmpty()) {
                var gravity: Int
                val offsetX: Float
                if (spec.endsWith(RIGHT_MARKER)) {
                    offsetX = displayWidth.toFloat()
                    spec = spec.substring(0, spec.length - RIGHT_MARKER.length).trim()
                    gravity = Gravity.RIGHT
                } else if (spec.endsWith(LEFT_MARKER)) {
                    offsetX = 0f
                    spec = spec.substring(0, spec.length - LEFT_MARKER.length).trim()
                    gravity = Gravity.LEFT
                } else {
                    offsetX = displayWidth / 2f
                    gravity = Gravity.CENTER
                }

                val inDp = spec.endsWith(DP_MARKER)
                if (inDp) {
                    spec = spec.substring(0, spec.length - DP_MARKER.length)
                }

                if (spec.contains(BOTTOM_MARKER)) {
                    val splits = spec.split(BOTTOM_MARKER, limit = 2)
                    spec = splits[0].trim()
                }

                val p: Path? = try {
                    PathParser.createPathFromPathData(spec)
                } catch (_: Throwable) {
                    null
                }

                if (p != null) {
                    val matrix = Matrix()
                    if (inDp) {
                        matrix.postScale(density, density)
                    }
                    matrix.postTranslate(offsetX, 0f)
                    p.transform(matrix)

                    val bounds = RectF()
                    p.computeBounds(bounds, true)

                    if (gravity != Gravity.CENTER && abs(bounds.centerX() - displayWidth / 2f) <= dp2) {
                        gravity = Gravity.CENTER
                    }
                    if (gravity == Gravity.CENTER && bounds.left < displayWidth / 4f) {
                        gravity = Gravity.LEFT
                    }
                    if (gravity == Gravity.CENTER && bounds.right > displayWidth / 4f * 3f) {
                        gravity = Gravity.RIGHT
                    }

                    return NotchInfo().apply {
                        this.gravity = gravity
                        this.path = p
                        this.bounds = bounds
                        this.rawPath = spec
                        this.isAccurate = spec.contains("C") || spec.contains("S") || spec.contains("Q")
                        this.isLikelyCircle = bounds.width() <= dp32 || bounds.width() <= bounds.height()
                    }
                }
            }
        }

        // Fallback: Query Android P+ DisplayCutout directly for OEM devices (Samsung, Pixel, etc.)
        val cutout = (displayCutout as? android.view.DisplayCutout)
            ?: (ctx as? android.app.Activity)?.window?.decorView?.rootWindowInsets?.displayCutout

        if (cutout != null) {
            val rects = cutout.boundingRects
            if (!rects.isNullOrEmpty()) {
                val topCutout = rects.firstOrNull { it.top == 0 || it.centerY() < m.heightPixels / 4 }
                    ?: rects.first()
                val bounds = RectF(topCutout)
                var gravity = Gravity.CENTER
                if (abs(bounds.centerX() - displayWidth / 2f) > dp2) {
                    gravity = if (bounds.centerX() < displayWidth / 2f) Gravity.LEFT else Gravity.RIGHT
                }
                val path = Path()
                var isAccurate = false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val cp = cutout.cutoutPath
                    if (cp != null) {
                        path.set(cp)
                        isAccurate = true
                    }
                }
                if (!isAccurate) {
                    if (bounds.width() <= dp32 || bounds.width() <= bounds.height()) {
                        path.addOval(bounds, Path.Direction.CW)
                    } else {
                        val rad = max(bounds.width(), bounds.height()) / 2f
                        path.addRoundRect(bounds, rad, rad, Path.Direction.CW)
                    }
                }

                return NotchInfo().apply {
                    this.gravity = gravity
                    this.path = path
                    this.bounds = bounds
                    this.rawPath = "DisplayCutout"
                    this.isAccurate = isAccurate
                    this.isLikelyCircle = bounds.width() <= dp32 || bounds.width() <= bounds.height()
                }
            }
        }

        return null
    }
}
