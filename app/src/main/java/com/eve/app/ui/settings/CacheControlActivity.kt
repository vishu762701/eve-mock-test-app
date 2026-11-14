/*
 * Derived from Telegram Android CacheControlActivity.java:1-850
 * Copyright Nikolai Kudashov, 2013-2023. GNU GPL v2 or later. See THIRD_PARTY_NOTICES.md.
 */

package com.eve.app.ui.settings

import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.lifecycle.lifecycleScope
import com.eve.app.designsystem.HeaderCell
import com.eve.app.designsystem.ShadowSectionCell
import com.eve.app.designsystem.StorageUsageView
import com.eve.app.designsystem.TelegramActionBar
import com.eve.app.designsystem.TelegramBottomSheet
import com.eve.app.designsystem.TelegramNativeAlertDialog
import com.eve.app.designsystem.TextCell
import com.eve.app.designsystem.TextCheckCell
import com.eve.app.designsystem.TextInfoPrivacyCell
import com.eve.app.theme.TelegramThemeEngine
import com.eve.app.ui.common.EveBaseActivity
import com.eve.app.util.AppBulletin
import com.eve.app.util.CacheManager
import com.eve.app.util.VibrationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Exact Telegram Storage & Cache Control Screen.
 * Upstream: CacheControlActivity.java
 */
class CacheControlActivity : EveBaseActivity() {

    private fun dp(value: Float): Int = (value * resources.displayMetrics.density + 0.5f).toInt()

    private lateinit var rootContainer: FrameLayout
    private lateinit var actionBar: TelegramActionBar
    private lateinit var storageUsageView: StorageUsageView
    private lateinit var cellKeepMedia: TextCell
    private lateinit var cellMaxCache: TextCell

    private lateinit var cellDocuments: TextCell
    private lateinit var cellPhotos: TextCell
    private lateinit var cellDatabase: TextCell
    private lateinit var cellTemp: TextCell

    private lateinit var btnClearCache: AppCompatButton

    private var documentsSize: Long = 0L
    private var photosSize: Long = 0L
    private var databaseSize: Long = 0L
    private var tempSize: Long = 0L
    private var totalCacheSize: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val palette = TelegramThemeEngine.resolveCurrentPalette(this)

        rootContainer = FrameLayout(this).apply {
            setBackgroundColor(palette.windowBackground)
        }
        setContentView(rootContainer)

        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        rootContainer.addView(mainLayout)

        // 1. Telegram ActionBar (56dp)
        actionBar = TelegramActionBar(this).apply {
            setTitle("Storage Usage")
            setBackButtonVisible(true)
            onBackPressed = { finish() }
        }
        mainLayout.addView(actionBar)

        // 2. Scrollable Content
        val scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            isFillViewport = true
        }

        val contentLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        scrollView.addView(contentLayout)
        mainLayout.addView(scrollView)

        // Storage Usage Diagram Header
        storageUsageView = StorageUsageView(this)
        contentLayout.addView(storageUsageView)

        contentLayout.addView(ShadowSectionCell(this))

        // Section 1: Keep Media
        val headerKeepMedia = HeaderCell(this).apply {
            setText("KEEP MEDIA")
        }
        contentLayout.addView(headerKeepMedia)

        cellKeepMedia = TextCell(this).apply {
            setTextAndValue("Keep Media", getKeepMediaLabel(), needDivider = false)
            setOnClickListener {
                showKeepMediaDialog()
            }
        }
        contentLayout.addView(cellKeepMedia)

        val infoKeepMedia = TextInfoPrivacyCell(this).apply {
            setText("Photos, documents and other files that you have not accessed during this period will be removed from this device to save disk space.")
        }
        contentLayout.addView(infoKeepMedia)

        contentLayout.addView(ShadowSectionCell(this))

        // Section 2: Maximum Cache Size
        val headerMaxCache = HeaderCell(this).apply {
            setText("MAXIMUM CACHE SIZE")
        }
        contentLayout.addView(headerMaxCache)

        cellMaxCache = TextCell(this).apply {
            setTextAndValue("Maximum Cache Size", getMaxCacheLabel(), needDivider = false)
            setOnClickListener {
                showMaxCacheDialog()
            }
        }
        contentLayout.addView(cellMaxCache)

        val infoMaxCache = TextInfoPrivacyCell(this).apply {
            setText("If your cache size exceeds this limit, the oldest unused files will be deleted from this device.")
        }
        contentLayout.addView(infoMaxCache)

        contentLayout.addView(ShadowSectionCell(this))

        // Section 3: Storage Breakdown
        val headerBreakdown = HeaderCell(this).apply {
            setText("STORAGE BREAKDOWN")
        }
        contentLayout.addView(headerBreakdown)

        cellDocuments = TextCell(this).apply {
            setTextAndValue("Documents & Syllabi", "Calculating…", needDivider = true)
        }
        contentLayout.addView(cellDocuments)

        cellPhotos = TextCell(this).apply {
            setTextAndValue("Profile Photos & Media", "Calculating…", needDivider = true)
        }
        contentLayout.addView(cellPhotos)

        cellDatabase = TextCell(this).apply {
            setTextAndValue("Local Database", "Calculating…", needDivider = true)
            setOnClickListener {
                showClearDatabasePrompt()
            }
        }
        contentLayout.addView(cellDatabase)

        cellTemp = TextCell(this).apply {
            setTextAndValue("Temporary Files", "Calculating…", needDivider = false)
        }
        contentLayout.addView(cellTemp)

        // Section 4: Clear Cache Button
        val buttonContainer = FrameLayout(this).apply {
            setPadding(dp(16f), dp(24f), dp(16f), dp(32f))
        }

        btnClearCache = AppCompatButton(this).apply {
            text = "Clear Cache"
            textSize = 15f
            setTextColor(0xFFFFFFFF.toInt())
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dp(10f).toFloat()
                setColor(palette.accent)
            }
            isAllCaps = false
            setOnClickListener {
                showClearCacheSheet()
            }
        }
        buttonContainer.addView(btnClearCache, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            dp(48f)
        ))
        contentLayout.addView(buttonContainer)

        loadStorageData()
    }

    private fun getKeepMediaLabel(): String {
        return when (CacheManager.getKeepMediaPreference(this)) {
            CacheManager.KEEP_MEDIA_3_DAYS -> "3 Days"
            CacheManager.KEEP_MEDIA_1_WEEK -> "1 Week"
            CacheManager.KEEP_MEDIA_1_MONTH -> "1 Month"
            else -> "Forever"
        }
    }

    private fun getMaxCacheLabel(): String {
        return when (CacheManager.getMaxCacheSizePreference(this)) {
            CacheManager.MAX_CACHE_5_GB -> "5 GB"
            CacheManager.MAX_CACHE_16_GB -> "16 GB"
            CacheManager.MAX_CACHE_32_GB -> "32 GB"
            else -> "No limit"
        }
    }

    private fun loadStorageData() {
        lifecycleScope.launch(Dispatchers.IO) {
            documentsSize = CacheManager.getDocumentsSize(this@CacheControlActivity)
            photosSize = CacheManager.getPhotosSize(this@CacheControlActivity)
            databaseSize = CacheManager.getDatabaseSize(this@CacheControlActivity)
            tempSize = CacheManager.getTempSize(this@CacheControlActivity)
            totalCacheSize = documentsSize + photosSize + tempSize

            val freeSpace = CacheManager.getDeviceFreeSpace()
            val totalSpace = CacheManager.getDeviceTotalSpace()

            withContext(Dispatchers.Main) {
                storageUsageView.setStorageData(totalCacheSize, freeSpace, totalSpace)

                cellDocuments.setTextAndValue("Documents & Syllabi", CacheManager.formatFileSize(documentsSize), true)
                cellPhotos.setTextAndValue("Profile Photos & Media", CacheManager.formatFileSize(photosSize), true)
                cellDatabase.setTextAndValue("Local Database", CacheManager.formatFileSize(databaseSize), true)
                cellTemp.setTextAndValue("Temporary Files", CacheManager.formatFileSize(tempSize), false)

                btnClearCache.text = if (totalCacheSize > 0) {
                    "Clear Entire Cache (${CacheManager.formatFileSize(totalCacheSize)})"
                } else {
                    "Clear Entire Cache (0 B)"
                }
            }
        }
    }

    private fun showKeepMediaDialog() {
        val options = arrayOf("3 Days", "1 Week", "1 Month", "Forever")
        val current = CacheManager.getKeepMediaPreference(this)

        TelegramNativeAlertDialog.Builder(this)
            .setTitle("Keep Media")
            .setSingleChoiceItems(options, current) { dialog, which ->
                CacheManager.setKeepMediaPreference(this, which)
                cellKeepMedia.setTextAndValue("Keep Media", getKeepMediaLabel(), needDivider = false)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showMaxCacheDialog() {
        val options = arrayOf("5 GB", "16 GB", "32 GB", "No limit")
        val current = CacheManager.getMaxCacheSizePreference(this)

        TelegramNativeAlertDialog.Builder(this)
            .setTitle("Maximum Cache Size")
            .setSingleChoiceItems(options, current) { dialog, which ->
                CacheManager.setMaxCacheSizePreference(this, which)
                cellMaxCache.setTextAndValue("Maximum Cache Size", getMaxCacheLabel(), needDivider = false)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showClearDatabasePrompt() {
        TelegramNativeAlertDialog.Builder(this)
            .setTitle("Clear Database")
            .setMessage("Are you sure you want to optimize and clear local cached database entries? This will not delete your account or test results.")
            .setPositiveButton("Clear") { _, _ ->
                lifecycleScope.launch(Dispatchers.IO) {
                    CacheManager.clearCache(
                        this@CacheControlActivity,
                        clearDocuments = false,
                        clearPhotos = false,
                        clearDatabase = true,
                        clearTemp = false
                    )
                    withContext(Dispatchers.Main) {
                        VibrationHelper.vibrateLightHaptic(this@CacheControlActivity)
                        AppBulletin.showSuccess(this@CacheControlActivity, "Local database optimized")
                        loadStorageData()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showClearCacheSheet() {
        val palette = TelegramThemeEngine.resolveCurrentPalette(this)

        val customView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(8f), 0, dp(16f))
        }

        var checkDocs = true
        var checkPhotos = true
        var checkTemp = true

        val checkCellDocs = TextCheckCell(this).apply {
            setTextAndValueAndCheck(
                "Documents & Syllabi",
                CacheManager.formatFileSize(documentsSize),
                checked = true,
                divider = true
            )
            setOnClickListener {
                checkDocs = !isChecked
                isChecked = checkDocs
            }
        }
        customView.addView(checkCellDocs)

        val checkCellPhotos = TextCheckCell(this).apply {
            setTextAndValueAndCheck(
                "Profile Photos & Media",
                CacheManager.formatFileSize(photosSize),
                checked = true,
                divider = true
            )
            setOnClickListener {
                checkPhotos = !isChecked
                isChecked = checkPhotos
            }
        }
        customView.addView(checkCellPhotos)

        val checkCellTemp = TextCheckCell(this).apply {
            setTextAndValueAndCheck(
                "Temporary Files",
                CacheManager.formatFileSize(tempSize),
                checked = true,
                divider = false
            )
            setOnClickListener {
                checkTemp = !isChecked
                isChecked = checkTemp
            }
        }
        customView.addView(checkCellTemp)

        val buttonContainer = FrameLayout(this).apply {
            setPadding(dp(16f), dp(16f), dp(16f), 0)
        }

        var sheet: TelegramBottomSheet? = null

        val btnConfirm = AppCompatButton(this).apply {
            text = "Clear Selected"
            textSize = 15f
            setTextColor(0xFFFFFFFF.toInt())
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dp(10f).toFloat()
                setColor(palette.accent)
            }
            isAllCaps = false
            setOnClickListener {
                sheet?.dismiss()
                lifecycleScope.launch(Dispatchers.IO) {
                    CacheManager.clearCache(
                        this@CacheControlActivity,
                        clearDocuments = checkDocs,
                        clearPhotos = checkPhotos,
                        clearDatabase = false,
                        clearTemp = checkTemp
                    )
                    withContext(Dispatchers.Main) {
                        VibrationHelper.vibrateLightHaptic(this@CacheControlActivity)
                        AppBulletin.showSuccess(this@CacheControlActivity, "Cache cleared successfully")
                        loadStorageData()
                    }
                }
            }
        }
        buttonContainer.addView(btnConfirm, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            dp(48f)
        ))
        customView.addView(buttonContainer)

        sheet = TelegramBottomSheet.Builder(this)
            .setTitle("Clear Cache")
            .setCustomView(customView)
            .create()

        sheet.show()
    }
}
