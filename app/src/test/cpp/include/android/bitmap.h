#pragma once
// Test-only ABI declarations for the three AndroidBitmap calls used by the
// unchanged native raster. Implementations bridge actual Robolectric pixels.
#include <jni.h>
#include <cstdint>
struct AndroidBitmapInfo {
    uint32_t width, height, stride;
    int32_t format;
    uint32_t flags;
};
constexpr int ANDROID_BITMAP_RESULT_SUCCESS = 0;
constexpr int ANDROID_BITMAP_FORMAT_RGBA_8888 = 1;
int AndroidBitmap_getInfo(JNIEnv *, jobject, AndroidBitmapInfo *);
int AndroidBitmap_lockPixels(JNIEnv *, jobject, void **);
int AndroidBitmap_unlockPixels(JNIEnv *, jobject);
