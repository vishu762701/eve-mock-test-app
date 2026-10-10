package com.eve.app.util

import android.graphics.Outline
import android.graphics.Path
import android.view.View
import android.view.ViewOutlineProvider

/**
 * Implements the Studio V10 approximate cubic squircle contour:
 * r = min(selected radius, w/2, h/2), t = 0.33 * r
 * M r,0 H w-r
 * C w-t,0 w,t w,r
 * V h-r
 * C w,h-t w-t,h w-r,h
 * H r
 * C t,h 0,h-t 0,h-r
 * V r
 * C 0,t t,0 r,0 Z
 */
object SquircleHelper {

    fun buildSquirclePath(path: Path, width: Float, height: Float, radius: Float) {
        path.reset()
        if (width <= 0f || height <= 0f) return
        val r = minOf(radius, width / 2f, height / 2f)
        val t = 0.33f * r
        path.moveTo(r, 0f)
        path.lineTo(width - r, 0f)
        path.cubicTo(width - t, 0f, width, t, width, r)
        path.lineTo(width, height - r)
        path.cubicTo(width, height - t, width - t, height, width - r, height)
        path.lineTo(r, height)
        path.cubicTo(t, height, 0f, height - t, 0f, height - r)
        path.lineTo(0f, r)
        path.cubicTo(0f, t, t, 0f, r, 0f)
        path.close()
    }

    fun applySquircleOutline(view: View, radiusPx: Float) {
        view.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(v: View, outline: Outline) {
                val r = minOf(radiusPx, v.width / 2f, v.height / 2f)
                outline.setRoundRect(0, 0, v.width, v.height, r)
            }
        }
        view.clipToOutline = true
    }
}
