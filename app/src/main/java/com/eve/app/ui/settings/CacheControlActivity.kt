/*
 * Derived from Telegram Android CacheControlActivity.java:1-850
 * Copyright Nikolai Kudashov, 2013-2023. GNU GPL v2 or later. See THIRD_PARTY_NOTICES.md.
 */

package com.eve.app.ui.settings

import android.os.Bundle
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import org.telegram.ui.Stories.recorder.ButtonWithCounterView
import androidx.lifecycle.lifecycleScope
import com.eve.app.R
import com.eve.app.designsystem.StorageUsageView
import com.eve.app.theme.TelegramThemeEngine
import com.eve.app.ui.common.EveBaseActivity
import com.eve.app.util.AppBulletin
import com.eve.app.util.CacheManager
import com.eve.app.util.VibrationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.telegram.ui.ActionBar.ActionBar
import org.telegram.ui.ActionBar.AlertDialog
import org.telegram.ui.ActionBar.BottomSheet
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.Cells.HeaderCell
import org.telegram.ui.Cells.ShadowSectionCell
import org.telegram.ui.Cells.TextCell
import org.telegram.ui.Cells.TextCheckCell
import org.telegram.ui.Cells.TextInfoPrivacyCell
import org.telegram.ui.Components.LayoutHelper
import org.telegram.ui.Components.SlideChooseView

/**
 * Storage & Cache Control Screen powered by ported Telegram ActionBar,
 * Cells, SlideChooseView sliders, AlertDialog, and BottomSheet.
 */
class CacheControlActivity : EveBaseActivity() {

    private fun dp(value: Float): Int = (value * resources.displayMetrics.density + 0.5f).toInt()

    private lateinit var rootContainer: FrameLayout
    private lateinit var actionBar: ActionBar
    private lateinit var storageUsageView: StorageUsageView
    private lateinit var keepMediaSlider: SlideChooseView
    private lateinit var maxCacheSlider: SlideChooseView

    private lateinit var cellDocuments: TextCell
    private lateinit var cellPhotos: TextCell
    private lateinit var cellDatabase: TextCell
    private lateinit var cellTemp: TextCell

    private lateinit var btnClearCache: ButtonWithCounterView

    private var documentsSize: Long = 0L
    private var photosSize: Long = 0L
    private var databaseSize: Long = 0L
    private var tempSize: Long = 0L
    private var totalCacheSize: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val palette = TelegramThemeEngine.resolveCurrentPalette(this)
        Theme.syncFromEvePalette(this)

        rootContainer = FrameLayout(this).apply {
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray))
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

        // 1. Real Telegram ActionBar
        actionBar = ActionBar(this).apply {
            setBackgroundColor(Theme.getColor(Theme.key_actionBarDefault))
            setItemsBackgroundColor(Theme.getColor(Theme.key_actionBarDefaultSelector), false)
            setItemsColor(Theme.getColor(Theme.key_actionBarDefaultIcon), false)
            setTitleColor(Theme.getColor(Theme.key_actionBarDefaultTitle))
            setBackButtonImage(R.drawable.ic_ab_back)
            setTitle("Storage Usage")
            setAllowOverlayTitle(true)
            setActionBarMenuOnItemClick(object : ActionBar.ActionBarMenuOnItemClick() {
                override fun onItemClick(id: Int) {
                    if (id == -1) {
                        finish()
                    }
                }
            })
        }
        mainLayout.addView(actionBar, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))

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

        val cardBg = Theme.getColor(Theme.key_windowBackgroundWhite)

        // Storage Usage Diagram Header
        storageUsageView = StorageUsageView(this).apply {
            setBackgroundColor(cardBg)
        }
        contentLayout.addView(storageUsageView)
        contentLayout.addView(ShadowSectionCell(this))

        // Section 1: Keep Media (Real Telegram SlideChooseView)
        val headerKeepMedia = HeaderCell(this).apply {
            setBackgroundColor(cardBg)
            setText("Keep Media")
        }
        contentLayout.addView(headerKeepMedia)

        keepMediaSlider = SlideChooseView(this).apply {
            setBackgroundColor(cardBg)
            setOptions(
                CacheManager.getKeepMediaPreference(this@CacheControlActivity).coerceIn(0, 3),
                "3 Days", "1 Week", "1 Month", "Forever"
            )
            setCallback { index ->
                CacheManager.setKeepMediaPreference(this@CacheControlActivity, index)
            }
        }
        contentLayout.addView(keepMediaSlider, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 50))

        val infoKeepMedia = TextInfoPrivacyCell(this).apply {
            setText("Photos, documents and other files that you have not accessed during this period will be removed from this device to save disk space.")
        }
        contentLayout.addView(infoKeepMedia)

        // Section 2: Maximum Cache Size (Real Telegram SlideChooseView)
        val headerMaxCache = HeaderCell(this).apply {
            setBackgroundColor(cardBg)
            setText("Maximum Cache Size")
        }
        contentLayout.addView(headerMaxCache)

        maxCacheSlider = SlideChooseView(this).apply {
            setBackgroundColor(cardBg)
            setOptions(
                CacheManager.getMaxCacheSizePreference(this@CacheControlActivity).coerceIn(0, 3),
                "5 GB", "16 GB", "32 GB", "No limit"
            )
            setCallback { index ->
                CacheManager.setMaxCacheSizePreference(this@CacheControlActivity, index)
            }
        }
        contentLayout.addView(maxCacheSlider, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 50))

        val infoMaxCache = TextInfoPrivacyCell(this).apply {
            setText("If your cache size exceeds this limit, the oldest unused files will be deleted from this device.")
        }
        contentLayout.addView(infoMaxCache)

        // Section 3: Storage Breakdown (Real Telegram TextCell)
        val headerBreakdown = HeaderCell(this).apply {
            setBackgroundColor(cardBg)
            setText("Storage Breakdown")
        }
        contentLayout.addView(headerBreakdown)

        cellDocuments = TextCell(this).apply {
            setBackgroundColor(cardBg)
            setTextAndValue("Documents & Syllabi", "Calculating…", true)
        }
        contentLayout.addView(cellDocuments)

        cellPhotos = TextCell(this).apply {
            setBackgroundColor(cardBg)
            setTextAndValue("Profile Photos & Media", "Calculating…", true)
        }
        contentLayout.addView(cellPhotos)

        cellDatabase = TextCell(this).apply {
            setBackgroundColor(cardBg)
            setTextAndValue("Local Database", "Calculating…", true)
            setOnClickListener {
                showClearDatabasePrompt()
            }
        }
        contentLayout.addView(cellDatabase)

        cellTemp = TextCell(this).apply {
            setBackgroundColor(cardBg)
            setTextAndValue("Temporary Files", "Calculating…", false)
        }
        contentLayout.addView(cellTemp)

        // Section 4: Clear Cache Button
        val buttonContainer = FrameLayout(this).apply {
            setPadding(dp(16f), dp(24f), dp(16f), dp(32f))
        }

        btnClearCache = ButtonWithCounterView(this, null).apply {
            setText("Clear Cache", false)
            setOnClickListener {
                showClearCacheSheet()
            }
        }
        buttonContainer.addView(
            btnClearCache,
            FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, dp(48f))
        )
        contentLayout.addView(buttonContainer)

        loadStorageData()
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

                btnClearCache.setText(
                    if (totalCacheSize > 0) {
                        "Clear Entire Cache (${CacheManager.formatFileSize(totalCacheSize)})"
                    } else {
                        "Clear Entire Cache (0 B)"
                    },
                    true
                )
            }
        }
    }

    private fun showClearDatabasePrompt() {
        AlertDialog.Builder(this)
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
                true,
                false,
                true
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
                true,
                false,
                true
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
                true,
                false,
                false
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

        var sheet: BottomSheet? = null

        val btnConfirm = ButtonWithCounterView(this, null).apply {
            setText("Clear Selected", false)
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
        buttonContainer.addView(
            btnConfirm,
            FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, dp(48f))
        )
        customView.addView(buttonContainer)

        sheet = BottomSheet.Builder(this)
            .setTitle("Clear Cache", true)
            .setCustomView(customView)
            .create()

        sheet.show()
    }
}
