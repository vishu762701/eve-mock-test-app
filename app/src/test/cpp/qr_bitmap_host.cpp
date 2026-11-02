// Test-only AndroidBitmap API bridge. No renderer implementation is duplicated:
// telegram_qr_backdrop.cpp is compiled unchanged into this host JNI library.
#include <android/bitmap.h>
#include <algorithm>
#include <vector>

struct LockedBitmap {
    jobject bitmap;
    AndroidBitmapInfo info;
    std::vector<uint8_t> rgba;
};
static thread_local std::vector<LockedBitmap> locked;

int AndroidBitmap_getInfo(JNIEnv *env, jobject bitmap, AndroidBitmapInfo *info) {
    if (!bitmap || !info) return -1;
    jclass type = env->GetObjectClass(bitmap);
    const jmethodID width = env->GetMethodID(type, "getWidth", "()I");
    const jmethodID height = env->GetMethodID(type, "getHeight", "()I");
    if (!width || !height || env->ExceptionCheck()) return -1;
    const int w = env->CallIntMethod(bitmap, width), h = env->CallIntMethod(bitmap, height);
    env->DeleteLocalRef(type);
    if (env->ExceptionCheck() || w <= 0 || h <= 0 || w > 162 || h > 162) return -1;
    *info = {static_cast<uint32_t>(w), static_cast<uint32_t>(h), static_cast<uint32_t>(w * 4), ANDROID_BITMAP_FORMAT_RGBA_8888, 0};
    return 0;
}

int AndroidBitmap_lockPixels(JNIEnv *env, jobject bitmap, void **out) {
    AndroidBitmapInfo info{};
    if (!out || AndroidBitmap_getInfo(env, bitmap, &info) != 0) return -1;
    jclass type = env->GetObjectClass(bitmap);
    const jmethodID getPixels = env->GetMethodID(type, "getPixels", "([IIIIIII)V");
    jintArray pixels = env->NewIntArray(info.width * info.height);
    if (!getPixels || !pixels || env->ExceptionCheck()) return -1;
    env->CallVoidMethod(bitmap, getPixels, pixels, 0, info.width, 0, 0, info.width, info.height);
    if (env->ExceptionCheck()) return -1;
    jint *argb = env->GetIntArrayElements(pixels, nullptr);
    if (!argb) return -1;
    LockedBitmap value{bitmap, info, std::vector<uint8_t>(info.width * info.height * 4)};
    for (uint32_t i = 0; i < info.width * info.height; i++) {
        const uint32_t color = static_cast<uint32_t>(argb[i]), alpha = color >> 24;
        value.rgba[i * 4] = (((color >> 16) & 255) * alpha + 127) / 255;
        value.rgba[i * 4 + 1] = (((color >> 8) & 255) * alpha + 127) / 255;
        value.rgba[i * 4 + 2] = ((color & 255) * alpha + 127) / 255;
        value.rgba[i * 4 + 3] = alpha;
    }
    env->ReleaseIntArrayElements(pixels, argb, JNI_ABORT);
    env->DeleteLocalRef(pixels);
    env->DeleteLocalRef(type);
    locked.push_back(std::move(value));
    *out = locked.back().rgba.data();
    return 0;
}

int AndroidBitmap_unlockPixels(JNIEnv *env, jobject bitmap) {
    auto it = std::find_if(locked.begin(), locked.end(), [&](const LockedBitmap &v) { return env->IsSameObject(bitmap, v.bitmap); });
    if (it == locked.end()) return -1;
    const auto info = it->info;
    std::vector<jint> argb(info.width * info.height);
    for (uint32_t i = 0; i < info.width * info.height; i++) {
        const auto *rgba = it->rgba.data() + i * 4;
        const uint32_t alpha = rgba[3];
        const auto unpremultiply = [alpha](uint8_t c) { return alpha ? std::min(255u, (static_cast<uint32_t>(c) * 255u + alpha / 2u) / alpha) : 0u; };
        argb[i] = static_cast<jint>((alpha << 24) | (unpremultiply(rgba[0]) << 16) | (unpremultiply(rgba[1]) << 8) | unpremultiply(rgba[2]));
    }
    jintArray pixels = env->NewIntArray(argb.size());
    if (!pixels) { locked.erase(it); return -1; }
    env->SetIntArrayRegion(pixels, 0, argb.size(), argb.data());
    jclass type = env->GetObjectClass(bitmap);
    const jmethodID setPixels = env->GetMethodID(type, "setPixels", "([IIIIIII)V");
    if (setPixels && !env->ExceptionCheck()) env->CallVoidMethod(bitmap, setPixels, pixels, 0, info.width, 0, 0, info.width, info.height);
    env->DeleteLocalRef(type);
    env->DeleteLocalRef(pixels);
    locked.erase(it);
    return env->ExceptionCheck() ? -1 : 0;
}

extern "C" void Java_com_eve_app_telegram_qr_QrBackdropNative_generateNative(JNIEnv *, jclass, jobject, jobject, jfloat, jint, jfloatArray, jintArray, jfloatArray);
extern "C" JNIEXPORT void JNICALL Java_com_eve_app_telegram_qr_QrBackdropHostBridge_generate(JNIEnv *env, jclass type, jobject bitmap, jobject ring, jfloat cell, jint count, jfloatArray geom, jintArray colors, jfloatArray wave) {
    Java_com_eve_app_telegram_qr_QrBackdropNative_generateNative(env, type, bitmap, ring, cell, count, geom, colors, wave);
}
