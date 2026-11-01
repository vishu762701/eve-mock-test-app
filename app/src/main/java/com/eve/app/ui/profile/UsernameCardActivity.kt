package com.eve.app.ui.profile

import android.content.ClipData
import android.widget.ScrollView
import com.eve.app.data.repository.EmojiRepository
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import android.content.ClipboardManager
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.material.bottomsheet.BottomSheetDialog
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.eve.app.R
import com.eve.app.ui.common.EveBaseActivity
import com.eve.app.util.AppBulletin
import java.io.File

/**
 * Telegram static QR scene geometry with EVE's registered username-only link.
 * This is an ADAPTED screen; Telegram QrShareView's 7k-line scene transitions,
 * account/network-backed gifts, paid links and live scanner are NOT ported.
 */
class UsernameCardActivity : EveBaseActivity() {
    private lateinit var scene: TelegramQrSceneView
    private lateinit var username: String
    private lateinit var qrLink: String
    private var qrClosing = false
    private var gallerySaveInProgress = false
    private var emojiPicker: BottomSheetDialog? = null
    private var emojiPickerGrid: QrEmojiPickerGrid? = null
    private var emojiPickerScroll: android.os.Parcelable? = null
    private val galleryPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) saveToGallery() else AppBulletin.showError(this, "Storage access is needed to save the QR image")
    }
    private val dp: Float get() = resources.displayMetrics.density
    private fun px(value: Int) = (value * dp).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        username = ProfileQrPayload.parse(intent?.dataString.orEmpty()) ?: run {
            finish()
            return
        }
        qrLink = requireNotNull(ProfileQrPayload.encode(username))
        applyQrSystemBars()
        @Suppress("DEPRECATION")
        emojiPickerScroll = savedInstanceState?.getParcelable("qr_picker_scroll")

        val root = FrameLayout(this)
        scene = TelegramQrSceneView(this, username, qrLink).apply {
            contentDescription = "QR code for public EVE username, at " + username +
                ". This shared card does not verify account ownership."
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
            setPalette(savedInstanceState?.getInt("qr_palette", 0) ?: 0)
            val keys = savedInstanceState?.getStringArrayList("qr_emoji_keys")
            val alts = savedInstanceState?.getStringArrayList("qr_emoji_alts")
            val sources = savedInstanceState?.getStringArrayList("qr_emoji_sources")
            if (keys != null && alts != null && sources != null &&
                keys.size == alts.size && alts.size == sources.size) {
                setEmojiChoices(keys.indices.map { QrEmojiChoice(keys[it], alts[it], sources[it]) })
            } else {
                setEmoji(savedInstanceState?.getString("qr_emoji"))
            }
        }
        root.addView(scene, FrameLayout.LayoutParams(-1, -1))

        val close = icon(R.drawable.eve_qr_close, "Close QR screen") { finishWithMotion() }
        val scan = scanButton { startActivity(Intent(this, EveQrLiveScannerActivity::class.java)) }
        val share = icon(R.drawable.eve_qr_share, "Share QR image") { shareImage() }
        val save = icon(R.drawable.eve_qr_download, "Save QR image") {
            saveToGallery()
        }
        val tools = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            orientation = LinearLayout.HORIZONTAL
            background = glassBackground(24)
        }
        fun tool(label: String, description: String, click: () -> Unit): TextView =
            TextView(this).apply {
                text = label
                textSize = 13f
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                contentDescription = description
                isClickable = true
                isFocusable = true
                setOnClickListener { click() }
            }
        tools.addView(tool("Colors", "Change QR color") { showQrPicker(false) },
            LinearLayout.LayoutParams(0, px(48), 1f))
        tools.addView(tool("Emoji", "Change QR emoji") { showQrPicker(true) },
            LinearLayout.LayoutParams(0, px(48), 1f))
        tools.addView(tool("Copy", "Copy EVE profile link") {
            (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager)
                .setPrimaryClip(ClipData.newPlainText("EVE profile link", qrLink))
            AppBulletin.showSuccess(this, "Profile link copied")
        }, LinearLayout.LayoutParams(0, px(48), 1f))

        val closeParams = FrameLayout.LayoutParams(px(48), px(48), Gravity.TOP or Gravity.START)
        val scanParams = FrameLayout.LayoutParams(-2, px(48), Gravity.TOP or Gravity.END)
        val shareParams = FrameLayout.LayoutParams(px(48), px(48), Gravity.BOTTOM or Gravity.START)
        val saveParams = FrameLayout.LayoutParams(px(48), px(48), Gravity.BOTTOM or Gravity.END)
        val toolsParams = FrameLayout.LayoutParams(px(224), px(48), Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL)
        root.addView(close, closeParams)
        root.addView(scan, scanParams)
        root.addView(share, shareParams)
        root.addView(save, saveParams)
        root.addView(tools, toolsParams)
        fun layout(top: Int, bottom: Int) {
            scene.systemTop = top
            scene.systemBottom = bottom
            closeParams.leftMargin = px(5)
            closeParams.topMargin = top + px(4)
            scanParams.rightMargin = px(8)
            scanParams.topMargin = top + px(4)
            shareParams.leftMargin = px(18)
            shareParams.bottomMargin = bottom + px(16)
            saveParams.rightMargin = px(18)
            saveParams.bottomMargin = bottom + px(16)
            toolsParams.bottomMargin = bottom + px(16)
            for (v in arrayOf(close, scan, share, save, tools)) v.requestLayout()
        }
        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            layout(bars.top, bars.bottom)
            insets
        }
        setContentView(root)
        ViewCompat.requestApplyInsets(root)
        if (savedInstanceState == null) scene.post { if (!isFinishing) scene.animateOpen() }
        if (savedInstanceState?.getBoolean("qr_picker_open") == true) root.post {
            if (!isFinishing && !isDestroyed) showAnimatedEmojiPicker()
        }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { finishWithMotion() }
        })
    }

    private fun finishWithMotion() {
        if (qrClosing || isFinishing) return
        qrClosing = true
        if (::scene.isInitialized) scene.animateClosed { finish() } else finish()
    }

    private fun showQrPicker(emojis: Boolean) {
        if (emojis) {
            showAnimatedEmojiPicker()
            return
        }
        val sheet = BottomSheetDialog(this)
        val grid = GridLayout(this).apply {
            columnCount = 6
            setPadding(px(10), px(12), px(10), px(12))
        }
        val cell = (resources.displayMetrics.widthPixels - px(40)) / 6
        for (index in 0 until scene.paletteCount) {
            val swatch = View(this).apply {
                isClickable = true
                isFocusable = true
                contentDescription = scene.paletteName(index)
                isSelected = scene.selectedPalette == index
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(scene.palettePreviewColor(index))
                    setStroke(px(if (isSelected) 3 else 1), if (isSelected) Color.WHITE else 0x336D7072)
                }
                com.eve.app.telegram.components.ScaleStateListAnimator.apply(this, .025f, 1.2f)
                setOnClickListener {
                    scene.setPalette(index)
                    sheet.dismiss()
                }
            }
            grid.addView(swatch, GridLayout.LayoutParams().apply {
                width = cell - px(8)
                height = cell - px(8)
                setMargins(px(4), px(4), px(4), px(4))
            })
        }
        sheet.setContentView(ScrollView(this).apply {
            background = glassBackground(24)
            addView(grid)
        })
        sheet.show()
    }

    private fun showAnimatedEmojiPicker() {
        if (emojiPicker?.isShowing == true) return
        val sheet = BottomSheetDialog(this)
        emojiPicker = sheet
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(px(12), px(12), px(12), px(20))
            background = glassBackground(24)
        }
        val title = TextView(this).apply {
            text = "EVE Animated Emojis"
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            textSize = 19f
        }
        title.minimumHeight = px(44)
        panel.addView(title, LinearLayout.LayoutParams(-1, -2))
        val count = TextView(this).apply {
            setTextColor(Color.WHITE)
            textSize = 13f
            gravity = Gravity.CENTER
        }
        fun updateCount() {
            count.text = "${scene.emojiChoices.size}/${QrEmojiPattern.MAX_SELECTED} selected"
        }
        updateCount()
        count.accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
        count.minimumHeight = px(30)
        panel.addView(count, LinearLayout.LayoutParams(-1, -2))
        val clear = TextView(this).apply {
            text = "Clear selection"
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            contentDescription = "Clear all selected QR emojis"
            isClickable = true
            isFocusable = true
        }
        clear.minimumHeight = px(48)
        panel.addView(clear, LinearLayout.LayoutParams(-1, -2))
        val grid = QrEmojiPickerGrid(this, { scene.emojiChoices }, { choice ->
            scene.toggleEmoji(choice)
            updateCount()
        }, scene::emojiDrawable, tint = { com.eve.app.telegram.qr.QrPalettes.shade(scene.selectedPalette) })
        emojiPickerGrid = grid
        grid.restoreScrollState(emojiPickerScroll)
        clear.setOnClickListener {
            scene.setEmojiChoices(emptyList())
            grid.updatePicks()
            updateCount()
        }
        val loading = TextView(this).apply {
            text = "Loading EVE animated emoji catalogue"
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
        }
        loading.accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
        loading.minimumHeight = px(42)
        panel.addView(loading, LinearLayout.LayoutParams(-1, -2))
        panel.addView(grid, LinearLayout.LayoutParams(-1, px(340)))
        sheet.setContentView(panel)
        var catalogueJob: kotlinx.coroutines.Job? = null
        sheet.setOnDismissListener {
            catalogueJob?.cancel()
            emojiPickerScroll = grid.saveScrollState()
            if (emojiPicker === sheet) {
                emojiPicker = null
                emojiPickerGrid = null
            }
        }
        sheet.show()
        sheet.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.apply {
            backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
            setBackgroundColor(Color.TRANSPARENT)
        }
        panel.post {
            if (!sheet.isShowing) return@post
            val bars = ViewCompat.getRootWindowInsets(panel)?.getInsets(WindowInsetsCompat.Type.systemBars())
            val available = resources.displayMetrics.heightPixels - (bars?.top ?: 0) - (bars?.bottom ?: 0)
            val fixed = panel.height - grid.height
            grid.layoutParams = (grid.layoutParams as LinearLayout.LayoutParams).apply {
                height = minOf(px(340), (available - fixed - px(24)).coerceAtLeast(px(48)))
            }
        }
        catalogueJob = lifecycleScope.launch {
            val items = EmojiRepository().getPublicQrItems(limit = 24).getOrElse {
                loading.text = "Animated catalogue unavailable"
                return@launch
            }
            if (!sheet.isShowing) return@launch
            val available = items.filter { it.enabled && it.kind == "lottie" &&
                (it.url.startsWith("https://") || it.url.startsWith("res:raw/")) }
                .map { QrEmojiPattern.custom(it.id, it.altEmoji, it.url) }
            grid.setItems(available)
            loading.text = if (available.isEmpty()) "No animated EVE emoji assets are available" else
                "${available.size} animated EVE emojis"
        }
    }

    private fun glassBackground(radius: Int): android.graphics.drawable.Drawable =
        com.eve.app.telegram.qr.QrControlBackground(px(radius).toFloat())

    private fun icon(drawable: Int, description: String, action: () -> Unit): FrameLayout {
        val host = FrameLayout(this).apply {
            contentDescription = description
            isFocusable = true
            isClickable = true
            setOnClickListener { action() }
        }
        val image = ImageView(this).apply {
            setImageResource(drawable)
            imageTintList = ColorStateList.valueOf(Color.WHITE)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            setPadding(px(8), px(8), px(8), px(8))
            background = glassBackground(20)
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        host.addView(image, FrameLayout.LayoutParams(px(40), px(40), Gravity.CENTER))
        return host
    }

    private fun scanButton(onClick: () -> Unit): FrameLayout {
        val host = FrameLayout(this).apply {
            contentDescription = "Scan EVE QR using live camera"
            isFocusable = true
            isClickable = true
            setOnClickListener { onClick() }
        }
        val pill = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            orientation = LinearLayout.HORIZONTAL
            setPadding(px(11), 0, px(13), 0)
            background = glassBackground(20)
        }
        pill.addView(ImageView(this).apply {
            setImageResource(R.drawable.eve_qr_qr_scan)
            imageTintList = ColorStateList.valueOf(Color.WHITE)
            scaleType = ImageView.ScaleType.FIT_CENTER
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }, LinearLayout.LayoutParams(px(20), px(20)))
        pill.addView(TextView(this).apply {
            text = "Scan QR"
            textSize = 14f
            setTextColor(Color.WHITE)
            setPadding(px(7), 0, 0, 0)
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        })
        host.addView(pill, FrameLayout.LayoutParams(-2, px(40), Gravity.CENTER))
        return host
    }

    private fun saveToGallery() {
        if (gallerySaveInProgress) return
        if (android.os.Build.VERSION.SDK_INT <= 28 &&
            androidx.core.content.ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            galleryPermission.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            return
        }
        val bitmap = scene.renderShareImage() ?: run {
            AppBulletin.showError(this, "QR image is not ready")
            return
        }
        gallerySaveInProgress = true
        lifecycleScope.launch {
            try {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    QrGalleryWriter.save(applicationContext, bitmap, username)
                }
                AppBulletin.showSuccess(this@UsernameCardActivity, "QR code saved to Gallery")
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                AppBulletin.showError(this@UsernameCardActivity, "Couldn't save QR image")
            } finally {
                bitmap.recycle()
                gallerySaveInProgress = false
            }
        }
    }

    private fun shareImage() {
        val image = scene.renderShareImage() ?: run {
            AppBulletin.showError(this, "QR image is not ready")
            return
        }
        try {
            val dir = File(cacheDir, "qr").apply { mkdirs() }
            val file = File(dir, "eve-username-qr.png")
            file.outputStream().use { out ->
                check(image.compress(Bitmap.CompressFormat.PNG, 100, out))
            }
            val uri = FileProvider.getUriForFile(this, packageName + ".fileprovider", file)
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                clipData = ClipData.newUri(contentResolver, "EVE QR image", uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(send, "Share EVE QR"))
        } catch (_: Exception) {
            AppBulletin.showError(this, "Couldn't share QR image")
        } finally {
            image.recycle()
        }
    }

    private fun applyQrSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.BLACK
    }

    override fun onApplyThemePalette(palette: com.eve.app.theme.TelegramThemeEngine.ResolvedPalette) {
        // The QR viewer is always a dark immersive scene, independent of EVE's global palette.
        applyQrSystemBars()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) applyQrSystemBars()
        if (::scene.isInitialized) scene.refreshEmojiPlayback()
    }

    override fun onDestroy() {
        emojiPicker?.dismiss()
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        if (::scene.isInitialized) {
            outState.putInt("qr_palette", scene.selectedPalette)
            outState.putString("qr_emoji", scene.selectedEmoji)
            outState.putStringArrayList("qr_emoji_keys", ArrayList(scene.emojiChoices.map { it.key }))
            outState.putStringArrayList("qr_emoji_alts", ArrayList(scene.emojiChoices.map { it.alt }))
            outState.putStringArrayList("qr_emoji_sources", ArrayList(scene.emojiChoices.map { it.source }))
        }
        outState.putBoolean("qr_picker_open", emojiPicker?.isShowing == true)
        outState.putParcelable("qr_picker_scroll", emojiPickerGrid?.saveScrollState() ?: emojiPickerScroll)
        super.onSaveInstanceState(outState)
    }
}
