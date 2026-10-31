package com.eve.app.ui.profile

import android.graphics.Bitmap
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Small-buffer RGBA stack blur port of Telegram TMessagesProj/jni/image.cpp:620-871.
 * The two-pass weights and edge replication are preserved. Bitmap getPixels/setPixels
 * converts via Android ARGB rather than Telegram native RGBA, so pixel identity is ADAPTED.
 */
internal object ProfileStackBlur {
    fun blurBitmap(bitmap: Bitmap, radius: Int) {
        if (radius < 1 || bitmap.isRecycled) return
        val w = bitmap.width
        val h = bitmap.height
        if (w <= 0 || h <= 0) return
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        blurArgb(pixels, w, h, radius)
        bitmap.setPixels(pixels, 0, w, 0, 0, w, h)
    }

    /** Pure version for deterministic JVM tests without Android Bitmap. */
    fun blurArgb(pixels: IntArray, w: Int, h: Int, radius: Int) {
        if (radius < 1 || w <= 0 || h <= 0) return
        require(pixels.size >= w * h)
        val div = radius * 2 + 1
        val r1 = radius + 1
        val divsum = ((div + 1) / 2).let { it * it }
        val stack = IntArray(div * 4)
        val vmin = IntArray(max(w, h))
        val channel = Array(4) { IntArray(w * h) }
        val sums = IntArray(4)
        val ins = IntArray(4)
        val outs = IntArray(4)

        fun loadColor(offset: Int, pixel: Int) {
            stack[offset] = (pixel ushr 16) and 255
            stack[offset + 1] = (pixel ushr 8) and 255
            stack[offset + 2] = pixel and 255
            stack[offset + 3] = (pixel ushr 24) and 255
        }

        var index = 0
        for (y in 0 until h) {
            sums.fill(0)
            ins.fill(0)
            outs.fill(0)
            for (i in -radius..radius) {
                val pos = (i + radius) * 4
                loadColor(pos, pixels[y * w + min(w - 1, max(i, 0))])
                val weight = r1 - abs(i)
                for (k in 0 until 4) {
                    val value = stack[pos + k]
                    sums[k] += value * weight
                    if (i > 0) ins[k] += value else outs[k] += value
                }
            }
            var pointer = radius
            for (x in 0 until w) {
                for (k in 0 until 4) channel[k][index] = sums[k] / divsum
                for (k in 0 until 4) sums[k] -= outs[k]
                var pos = ((pointer - radius + div) % div) * 4
                for (k in 0 until 4) outs[k] -= stack[pos + k]
                if (y == 0) vmin[x] = min(x + r1, w - 1)
                loadColor(pos, pixels[y * w + vmin[x]])
                for (k in 0 until 4) {
                    ins[k] += stack[pos + k]
                    sums[k] += ins[k]
                }
                pointer = (pointer + 1) % div
                pos = pointer * 4
                for (k in 0 until 4) {
                    outs[k] += stack[pos + k]
                    ins[k] -= stack[pos + k]
                }
                index++
            }
        }

        for (x in 0 until w) {
            sums.fill(0)
            ins.fill(0)
            outs.fill(0)
            var yp = -radius * w
            for (i in -radius..radius) {
                val rowIndex = max(0, yp) + x
                val pos = (i + radius) * 4
                val weight = r1 - abs(i)
                for (k in 0 until 4) {
                    val value = channel[k][rowIndex]
                    stack[pos + k] = value
                    sums[k] += value * weight
                    if (i > 0) ins[k] += value else outs[k] += value
                }
                if (i < h - 1) yp += w
            }
            var pointer = radius
            for (y in 0 until h) {
                val rr = sums[0] / divsum
                val gg = sums[1] / divsum
                val bb = sums[2] / divsum
                val aa = sums[3] / divsum
                pixels[y * w + x] = (aa shl 24) or (rr shl 16) or (gg shl 8) or bb

                for (k in 0 until 4) sums[k] -= outs[k]
                var pos = ((pointer - radius + div) % div) * 4
                for (k in 0 until 4) outs[k] -= stack[pos + k]
                if (x == 0) vmin[y] = min(y + r1, h - 1) * w
                val rowIndex = x + vmin[y]
                for (k in 0 until 4) {
                    stack[pos + k] = channel[k][rowIndex]
                    ins[k] += stack[pos + k]
                    sums[k] += ins[k]
                }
                pointer = (pointer + 1) % div
                pos = pointer * 4
                for (k in 0 until 4) {
                    outs[k] += stack[pos + k]
                    ins[k] -= stack[pos + k]
                }
            }
        }
    }
}
