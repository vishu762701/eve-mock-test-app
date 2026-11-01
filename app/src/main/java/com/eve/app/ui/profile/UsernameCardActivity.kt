package com.eve.app.ui.profile

import android.content.ClipData
import android.widget.ScrollView
import com.airbnb.lottie.LottieAnimationView
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
    private val dp: Float get() = resources.displayMetrics.density
    private fun px(value: Int) = (value * dp).toInt()

    private val saveDocument = registerForActivityResult(
        ActivityResultContracts.CreateDocument("image/png")
    ) { uri ->
        if (uri == null || !::scene.isInitialized) return@registerForActivityResult
        val image = scene.renderShareImage()
        if (image == null) {
            AppBulletin.showError(this, "QR image is not ready")
            return@registerForActivityResult
        }
        try {
            contentResolver.openOutputStream(uri)?.use { out ->
                check(image.compress(Bitmap.CompressFormat.PNG, 100, out))
            } ?: error("No output stream available")
            AppBulletin.showSuccess(this, "QR image saved")
        } catch (_: Exception) {
            AppBulletin.showError(this, "Couldn't save QR image")
        } finally {
            image.recycle()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        username = ProfileQrPayload.parse(intent?.dataString.orEmpty()) ?: run {
            finish()
            return
        }
        qrLink = requireNotNull(ProfileQrPayload.encode(username))
        applyQrSystemBars()

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
            saveDocument.launch("EVE-" + username + "-QR.png")
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
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(px(14), px(18), px(14), px(24))
            background = glassBackground(24)
        }
        val title = TextView(this).apply {
            text = if (emojis) "QR Emojis" else "QR Colors"
            setTextColor(Color.WHITE)
            textSize = 19f
            gravity = Gravity.CENTER
        }
        panel.addView(title, LinearLayout.LayoutParams(-1, px(48)))
        val grid = GridLayout(this).apply { columnCount = 4 }
        val cellWidth = (resources.displayMetrics.widthPixels - px(48)) / 4
        val symbols = listOf("None", "✨", "⭐", "🔥", "🌸", "🦋", "💎", "💙",
            "🌙", "🎓", "📚", "🎯", "🌈", "☀️", "🌿", "🌊", "🎵", "⚡", "🍀", "🌟")
        val names = listOf("Black", "Neon Blue", "Midnight", "Mint",
            "Lavender", "Rosewood", "Orange", "Emerald")
        val count = if (emojis) symbols.size else scene.paletteCount
        for (index in 0 until count) {
            val item = TextView(this).apply {
                gravity = Gravity.CENTER
                setTextColor(Color.WHITE)
                text = if (emojis) symbols[index] else names[index]
                textSize = if (emojis) 26f else 11f
                background = GradientDrawable().apply {
                    cornerRadius = px(18).toFloat()
                    setColor(if (emojis) 0xFF303338.toInt() else scene.palettePreviewColor(index))
                    setStroke(px(1), 0x556D7072)
                }
                contentDescription = if (emojis) "Use " + symbols[index] + " QR emoji" else "Use " + names[index] + " QR color"
                setOnClickListener {
                    if (emojis) scene.setEmoji(symbols[index].takeUnless { it == "None" })
                    else scene.setPalette(index)
                    sheet.dismiss()
                }
            }
            grid.addView(item, GridLayout.LayoutParams().apply {
                width = cellWidth - px(5)
                height = px(54)
                setMargins(px(2), px(3), px(2), px(3))
            })
        }
        panel.addView(grid)
        sheet.setContentView(panel)
        sheet.show()
    }

    private fun showAnimatedEmojiPicker() {
        val sheet = BottomSheetDialog(this)
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
        panel.addView(title, LinearLayout.LayoutParams(-1, px(44)))
        val count = TextView(this).apply {
            setTextColor(Color.WHITE)
            textSize = 13f
            gravity = Gravity.CENTER
        }
        fun updateCount() {
            count.text = "${scene.emojiChoices.size}/${QrEmojiPattern.MAX_SELECTED} selected"
        }
        updateCount()
        panel.addView(count, LinearLayout.LayoutParams(-1, px(30)))
        val clear = TextView(this).apply {
            text = "Clear selection"
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            contentDescription = "Clear all selected QR emojis"
            isClickable = true
            isFocusable = true
            setOnClickListener { scene.setEmojiChoices(emptyList()); updateCount() }
        }
        panel.addView(clear, LinearLayout.LayoutParams(-1, px(48)))
        val grid = GridLayout(this).apply { columnCount = 4 }
        val width = (resources.displayMetrics.widthPixels - px(44)) / 4
        fun addChoice(choice: QrEmojiChoice, animated: Boolean) {
            val host = FrameLayout(this).apply {
                isClickable = true
                isFocusable = true
                contentDescription = "Toggle " + choice.alt + " QR emoji"
                background = GradientDrawable().apply {
                    cornerRadius = px(14).toFloat()
                    setColor(0xFF303338.toInt())
                }
            }
            val widget: View = if (animated) {
                LottieAnimationView(this).apply {
                    repeatCount = android.animation.ValueAnimator.INFINITE
                    if (choice.source.startsWith("res:raw/")) {
                        val id = resources.getIdentifier(
                            choice.source.removePrefix("res:raw/"), "raw", packageName)
                        if (id != 0) setAnimation(id)
                    } else if (choice.source.startsWith("https://")) {
                        setAnimationFromUrl(choice.source)
                    }
                    playAnimation()
                }
            } else {
                TextView(this).apply {
                    text = choice.alt
                    textSize = 27f
                    gravity = Gravity.CENTER
                    setTextColor(Color.WHITE)
                }
            }
            host.addView(widget, FrameLayout.LayoutParams(-1, -1))
            host.alpha = if (scene.emojiChoices.any { it.key == choice.key }) 1f else .78f
            host.setOnClickListener {
                scene.toggleEmoji(choice)
                host.alpha = if (scene.emojiChoices.any { it.key == choice.key }) 1f else .78f
                updateCount()
            }
            grid.addView(host, GridLayout.LayoutParams().apply {
                this.width = width - px(5)
                height = px(55)
                setMargins(px(2), px(3), px(2), px(3))
            })
        }
        listOf("❤️","🔥","😎","🤩","❄️","🌸","🎉","🚀","🗿","🏆","👻","💸",
            "🎮","💎","🦄","🧸","🐶","🦊","🐱","🐵","🐥").forEach {
            addChoice(QrEmojiPattern.standard(it), false)
        }
        val loading = TextView(this).apply {
            text = "Loading EVE animated emoji catalogue"
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
        }
        panel.addView(loading, LinearLayout.LayoutParams(-1, px(42)))
        val scroll = ScrollView(this).apply { addView(grid) }
        panel.addView(scroll, LinearLayout.LayoutParams(-1, px(340)))
        sheet.setContentView(panel)
        sheet.setOnDismissListener {
            // Stop all offscreen picker animations; scene owns selected live frames.
            for (i in 0 until grid.childCount) {
                val host = grid.getChildAt(i) as? FrameLayout ?: continue
                (host.getChildAt(0) as? LottieAnimationView)?.cancelAnimation()
            }
        }
        sheet.show()
        lifecycleScope.launch {
            val items = EmojiRepository().getPublicQrItems(limit = 24).getOrElse {
                loading.text = "Animated catalogue unavailable. Standard emoji motion still works."
                return@launch
            }
            if (!sheet.isShowing) return@launch
            var added = 0
            for (item in items) {
                if (!item.enabled || item.kind != "lottie" || item.url.isBlank()) continue
                if (!item.url.startsWith("https://") && !item.url.startsWith("res:raw/")) continue
                addChoice(QrEmojiPattern.custom(item.id, item.altEmoji, item.url), true)
                added++
                if (added >= 12) break
            }
            loading.text = if (added == 0) "No animated EVE emoji assets are available" else
                "$added animated EVE emojis"
        }
    }

    private fun glassBackground(radius: Int): GradientDrawable = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = px(radius).toFloat()
        setColor(0xBB343638.toInt())
        setStroke(px(1), 0x776D7072)
    }

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
    }

    override fun onSaveInstanceState(outState: Bundle) {
        if (::scene.isInitialized) {
            outState.putInt("qr_palette", scene.selectedPalette)
            outState.putString("qr_emoji", scene.selectedEmoji)
            outState.putStringArrayList("qr_emoji_keys", ArrayList(scene.emojiChoices.map { it.key }))
            outState.putStringArrayList("qr_emoji_alts", ArrayList(scene.emojiChoices.map { it.alt }))
            outState.putStringArrayList("qr_emoji_sources", ArrayList(scene.emojiChoices.map { it.source }))
        }
        super.onSaveInstanceState(outState)
    }
}
