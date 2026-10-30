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

    @SuppressLint("DiscouragedApi")
    fun getInfo(ctx: Context): NotchInfo? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return null
        }

        val res = ctx.resources
        val r = res.getIdentifier("config_mainBuiltInDisplayCutout", "string", "android")
        if (r == 0) return null

        var spec = ctx.getString(r).trim()
        if (spec.isEmpty()) return null

        val m = res.displayMetrics
        val displayWidth = m.widthPixels
        val density = m.density

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

        val p: Path
        try {
            val n = PathParser.createNodesFromPathData(spec)
            p = Path()
            PathParser.PathDataNode.nodesToPath(n, p)
        } catch (_: Throwable) {
            return null
        }

        val matrix = Matrix()
        if (inDp) {
            matrix.postScale(density, density)
        }
        matrix.postTranslate(offsetX, 0f)
        p.transform(matrix)

        val bounds = RectF()
        p.computeBounds(bounds, true)

        val dp2 = 2f * density
        val dp32 = 32f * density

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
