/*
 * Derived from Telegram Android CacheControlActivity.java and StorageUsageView.java
 * Copyright Nikolai Kudashov, 2013-2023. GNU GPL v2 or later. See THIRD_PARTY_NOTICES.md.
 */

package com.eve.app.util

import android.content.Context
import android.os.Environment
import android.os.StatFs
import java.io.File
import java.text.DecimalFormat

object CacheManager {

    private const val PREFS_NAME = "eve_cache_prefs"
    private const val KEY_KEEP_MEDIA = "keep_media_duration"
    private const val KEY_MAX_CACHE_SIZE = "max_cache_size"

    const val KEEP_MEDIA_3_DAYS = 0
    const val KEEP_MEDIA_1_WEEK = 1
    const val KEEP_MEDIA_1_MONTH = 2
    const val KEEP_MEDIA_FOREVER = 3

    const val MAX_CACHE_5_GB = 0
    const val MAX_CACHE_16_GB = 1
    const val MAX_CACHE_32_GB = 2
    const val MAX_CACHE_UNLIMITED = 3

    fun getKeepMediaPreference(context: Context): Int {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sp.getInt(KEY_KEEP_MEDIA, KEEP_MEDIA_FOREVER)
    }

    fun setKeepMediaPreference(context: Context, value: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_KEEP_MEDIA, value)
            .apply()
    }

    fun getMaxCacheSizePreference(context: Context): Int {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sp.getInt(KEY_MAX_CACHE_SIZE, MAX_CACHE_UNLIMITED)
    }

    fun setMaxCacheSizePreference(context: Context, value: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_MAX_CACHE_SIZE, value)
            .apply()
    }

    fun getDirSize(dir: File?): Long {
        if (dir == null || !dir.exists()) return 0L
        var size = 0L
        val files = dir.listFiles() ?: return 0L
        for (f in files) {
            size += if (f.isDirectory) {
                getDirSize(f)
            } else {
                f.length()
            }
        }
        return size
    }

    fun deleteDirContent(dir: File?): Boolean {
        if (dir == null || !dir.exists()) return false
        val files = dir.listFiles() ?: return true
        var success = true
        for (f in files) {
            if (f.isDirectory) {
                success = success && deleteDirContent(f) && f.delete()
            } else {
                success = success && f.delete()
            }
        }
        return success
    }

    /**
     * Syllabi and downloaded PDF documents.
     */
    fun getDocumentsSize(context: Context): Long {
        var size = 0L
        val extDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        size += getDirSize(extDir)
        val filesDir = File(context.filesDir, "syllabi")
        size += getDirSize(filesDir)
        return size
    }

    /**
     * Profile photos, avatars, and image cache.
     */
    fun getPhotosSize(context: Context): Long {
        var size = 0L
        val imgCache = File(context.cacheDir, "image_manager_disk_cache")
        size += getDirSize(imgCache)
        val qrCache = File(context.cacheDir, "qr")
        size += getDirSize(qrCache)
        val emojiCache = File(context.cacheDir, "emoji_cache")
        size += getDirSize(emojiCache)
        return size
    }

    /**
     * Local database and room / sqlite / shared preferences.
     */
    fun getDatabaseSize(context: Context): Long {
        var size = 0L
        val dbDir = context.getDatabasePath("placeholder").parentFile
        if (dbDir != null && dbDir.exists()) {
            size += getDirSize(dbDir)
        }
        return size
    }

    /**
     * Temporary cache directory (excluding specific subdirs handled above).
     */
    fun getTempSize(context: Context): Long {
        var size = getDirSize(context.cacheDir)
        val photos = getPhotosSize(context)
        size = (size - photos).coerceAtLeast(0L)
        return size
    }

    fun getTotalCacheSize(context: Context): Long {
        return getDocumentsSize(context) + getPhotosSize(context) + getTempSize(context)
    }

    fun getDeviceFreeSpace(): Long {
        return try {
            val stat = StatFs(Environment.getDataDirectory().path)
            stat.availableBlocksLong * stat.blockSizeLong
        } catch (_: Throwable) {
            0L
        }
    }

    fun getDeviceTotalSpace(): Long {
        return try {
            val stat = StatFs(Environment.getDataDirectory().path)
            stat.blockCountLong * stat.blockSizeLong
        } catch (_: Throwable) {
            1L
        }
    }

    /**
     * Formats bytes to Telegram standard sizes (B, KB, MB, GB).
     * Source: AndroidUtilities.formatFileSize
     */
    fun formatFileSize(size: Long): String {
        if (size <= 0) return "0 B"
        val df = DecimalFormat("#.##")
        return when {
            size >= 1024L * 1024L * 1024L -> "${df.format(size / (1024.0 * 1024.0 * 1024.0))} GB"
            size >= 1024L * 1024L -> "${df.format(size / (1024.0 * 1024.0))} MB"
            size >= 1024L -> "${df.format(size / 1024.0)} KB"
            else -> "$size B"
        }
    }

    fun clearCache(
        context: Context,
        clearDocuments: Boolean,
        clearPhotos: Boolean,
        clearDatabase: Boolean,
        clearTemp: Boolean
    ) {
        if (clearDocuments) {
            deleteDirContent(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS))
            deleteDirContent(File(context.filesDir, "syllabi"))
        }
        if (clearPhotos) {
            deleteDirContent(File(context.cacheDir, "image_manager_disk_cache"))
            deleteDirContent(File(context.cacheDir, "qr"))
            deleteDirContent(File(context.cacheDir, "emoji_cache"))
        }
        if (clearTemp) {
            // Clear cacheDir files that are not image_manager_disk_cache or qr
            val files = context.cacheDir.listFiles() ?: emptyArray()
            for (f in files) {
                if (f.name != "image_manager_disk_cache" && f.name != "qr" && f.name != "emoji_cache") {
                    if (f.isDirectory) {
                        deleteDirContent(f)
                        f.delete()
                    } else {
                        f.delete()
                    }
                }
            }
        }
        if (clearDatabase) {
            val dbDir = context.getDatabasePath("placeholder").parentFile
            if (dbDir != null && dbDir.exists()) {
                val dbFiles = dbDir.listFiles() ?: emptyArray()
                for (f in dbFiles) {
                    if (f.name.endsWith("-wal") || f.name.endsWith("-shm")) {
                        f.delete()
                    }
                }
            }
        }
    }
}
