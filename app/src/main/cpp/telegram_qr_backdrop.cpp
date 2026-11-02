/*
 * ADAPTED isolated Telegram QR backdrop renderer, kept in its original C++.
 * DrKLO/Telegram@3de37c8cfb039452be7398909461da6288babf15
 * TMessagesProj/jni/image.cpp:1211-1377.
 * Only JNI binding and null-allocation cleanup are adapted. Original pixel,
 * gradient, antialias and premultiplied ring algorithms are retained.
 * Telegram contributors; root GPLv2 and release obligations in THIRD_PARTY_NOTICES.
 */
#include <jni.h>
#include <android/bitmap.h>
#include <cmath>
#include <cstdint>

// ---- QR share backdrop: a low-res picture of a 3-stop gradient (linear or radial), upscaled by the GPU as a plain
// bitmap (no gradient shader to compile or run per pixel). During a recolour wave the new scene grows in a circle over
// the old one; the wave's glowing ring goes to a second (transparent) bitmap drawn over the pattern.

namespace {

struct QrScene {
    int type;              // 0 linear, 1 radial
    float px, py, dx, dy;  // linear: origin + unit direction; radial: centre
    float scale;           // 1 / length (linear) or 1 / radius (radial)
    float mid;             // the middle stop
    float c[3][3];         // the stops' colours, 0..255 (r, g, b)
};

inline void qrSceneColor(const QrScene &s, float x, float y, float *out) {
    float t;
    if (s.type == 1) {
        const float ox = x - s.px, oy = y - s.py;
        t = sqrtf(ox * ox + oy * oy) * s.scale;
    } else {
        t = ((x - s.px) * s.dx + (y - s.py) * s.dy) * s.scale;
    }
    t = t < 0.0f ? 0.0f : (t > 1.0f ? 1.0f : t);
    const float *a, *b;
    float k;
    if (t < s.mid) {
        a = s.c[0]; b = s.c[1]; k = s.mid > 0.0f ? t / s.mid : 1.0f;
    } else {
        a = s.c[1]; b = s.c[2]; k = s.mid < 1.0f ? (t - s.mid) / (1.0f - s.mid) : 1.0f;
    }
    out[0] = a[0] + (b[0] - a[0]) * k;
    out[1] = a[1] + (b[1] - a[1]) * k;
    out[2] = a[2] + (b[2] - a[2]) * k;
}

inline void qrReadScene(QrScene &s, const float *g, const jint *colors) {
    s.type = (int) g[0];
    s.px = g[1]; s.py = g[2]; s.dx = g[3]; s.dy = g[4]; s.scale = g[5]; s.mid = g[6];
    for (int i = 0; i < 3; i++) {
        const uint32_t c = (uint32_t) colors[i];
        s.c[i][0] = (float) ((c >> 16) & 0xFF);
        s.c[i][1] = (float) ((c >> 8) & 0xFF);
        s.c[i][2] = (float) (c & 0xFF);
    }
}

// the ring's alpha at distance d: piecewise linear through 6 (position, alpha) stops, 0 outside
inline float qrRingAlpha(const float *pos, const float *alpha, float d) {
    if (d <= pos[0] || d >= pos[5]) return 0.0f;
    for (int i = 1; i < 6; i++) {
        if (d <= pos[i]) {
            const float span = pos[i] - pos[i - 1];
            const float k = span > 0.0f ? (d - pos[i - 1]) / span : 1.0f;
            return alpha[i - 1] + (alpha[i] - alpha[i - 1]) * k;
        }
    }
    return 0.0f;
}

inline uint8_t qrByte(float v) {
    v += 0.5f;
    return (uint8_t) (v < 0.0f ? 0.0f : (v > 255.0f ? 255.0f : v));
}

}

/**
 * bitmap: the backdrop, opaque; texel (i, j) is the view point (i * cell, j * cell).
 * geom: 7 floats per scene (type, px, py, dx, dy, scale, mid); colors: 3 per scene, then the ring's colour.
 * count 2 = a wave: scene 1 inside the circle wave[0..2] (x, y, r) over scene 0, its ring into ringBitmap
 * (premultiplied) from wave[3..8] (positions) and wave[9..14] (alphas, 0..1).
 */
extern "C" JNIEXPORT void Java_com_eve_app_telegram_qr_QrBackdropNative_generateNative(JNIEnv *env, jclass clazz, jobject bitmap, jobject ringBitmap, jfloat cell, jint count, jfloatArray geom, jintArray colors, jfloatArray wave) {
    if (!bitmap || count < 1) {
        return;
    }
    AndroidBitmapInfo info{};
    if (AndroidBitmap_getInfo(env, bitmap, &info) != ANDROID_BITMAP_RESULT_SUCCESS || info.format != ANDROID_BITMAP_FORMAT_RGBA_8888 || !info.width || !info.height) {
        return;
    }
    const bool waving = count > 1 && wave != nullptr;
    // the arrays hold what count asks for: 7 floats per scene, 3 colours per scene (+ the ring's), 15 wave floats
    if (geom == nullptr || colors == nullptr || env->GetArrayLength(geom) < (count > 1 ? 14 : 7)
        || env->GetArrayLength(colors) < (waving ? 7 : 3) || (waving && env->GetArrayLength(wave) < 15)) {
        return;
    }
    AndroidBitmapInfo ringInfo{};
    const bool ring = waving && ringBitmap != nullptr && AndroidBitmap_getInfo(env, ringBitmap, &ringInfo) == ANDROID_BITMAP_RESULT_SUCCESS
        && ringInfo.format == ANDROID_BITMAP_FORMAT_RGBA_8888 && ringInfo.width == info.width && ringInfo.height == info.height;

    jfloat *g = env->GetFloatArrayElements(geom, nullptr);
    jint *cs = g ? env->GetIntArrayElements(colors, nullptr) : nullptr;
    jfloat *wv = cs && waving ? env->GetFloatArrayElements(wave, nullptr) : nullptr;
    // EVE JNI boundary: allocation failure must retain the pending JVM error,
    // release already pinned arrays and return before dereferencing null.
    if (!g || !cs || (waving && !wv)) {
        if (wv) env->ReleaseFloatArrayElements(wave, wv, JNI_ABORT);
        if (cs) env->ReleaseIntArrayElements(colors, cs, JNI_ABORT);
        if (g) env->ReleaseFloatArrayElements(geom, g, JNI_ABORT);
        if (!env->ExceptionCheck()) {
            jclass error = env->FindClass("java/lang/OutOfMemoryError");
            if (error) env->ThrowNew(error, "QR backdrop array allocation failed");
        }
        return;
    }


    QrScene a{}, b{};
    qrReadScene(a, g, cs);
    if (waving) qrReadScene(b, g + 7, cs + 3);
    const uint32_t rc = waving ? (uint32_t) cs[6] : 0;
    const float rr = (float) ((rc >> 16) & 0xFF), rg = (float) ((rc >> 8) & 0xFF), rb = (float) (rc & 0xFF);

    uint8_t *px = nullptr, *rpx = nullptr;
    if (AndroidBitmap_lockPixels(env, bitmap, (void **) &px) >= 0) {
        if (ring && AndroidBitmap_lockPixels(env, ringBitmap, (void **) &rpx) < 0) rpx = nullptr;
        const float h4 = cell * 0.25f;
        float c0[3], c1[3];
        for (uint32_t j = 0; j < info.height; j++) {
            const float y = (float) j * cell;
            uint8_t *row = px + j * info.stride;
            uint8_t *ringRow = rpx ? rpx + j * ringInfo.stride : nullptr;
            for (uint32_t i = 0; i < info.width; i++) {
                const float x = (float) i * cell;
                qrSceneColor(a, x, y, c0);
                float ra = 0.0f;
                if (waving) {
                    const float ox = x - wv[0], oy = y - wv[1];
                    const float d = sqrtf(ox * ox + oy * oy);
                    // the circle's edge covers one texel (a pixel-exact edge would alias at this resolution)
                    float k = (wv[2] - d) / cell + 0.5f;
                    k = k < 0.0f ? 0.0f : (k > 1.0f ? 1.0f : k);
                    if (k > 0.0f) {
                        qrSceneColor(b, x, y, c1);
                        c0[0] += (c1[0] - c0[0]) * k;
                        c0[1] += (c1[1] - c0[1]) * k;
                        c0[2] += (c1[2] - c0[2]) * k;
                    }
                    if (ringRow) {
                        // the ring's thin rim is narrower than a texel: averaged over the texel across the radius
                        ra = 0.25f * (qrRingAlpha(wv + 3, wv + 9, d - 1.5f * h4) + qrRingAlpha(wv + 3, wv + 9, d - 0.5f * h4)
                            + qrRingAlpha(wv + 3, wv + 9, d + 0.5f * h4) + qrRingAlpha(wv + 3, wv + 9, d + 1.5f * h4));
                    }
                }
                uint8_t *o = row + i * 4;
                o[0] = qrByte(c0[0]);
                o[1] = qrByte(c0[1]);
                o[2] = qrByte(c0[2]);
                o[3] = 0xFF;
                if (ringRow) {
                    uint8_t *q = ringRow + i * 4;
                    q[0] = qrByte(rr * ra);
                    q[1] = qrByte(rg * ra);
                    q[2] = qrByte(rb * ra);
                    q[3] = qrByte(255.0f * ra);
                }
            }
        }
        if (rpx) AndroidBitmap_unlockPixels(env, ringBitmap);
        AndroidBitmap_unlockPixels(env, bitmap);
    }

    if (wv) env->ReleaseFloatArrayElements(wave, wv, JNI_ABORT);
    env->ReleaseIntArrayElements(colors, cs, JNI_ABORT);
    env->ReleaseFloatArrayElements(geom, g, JNI_ABORT);
}
