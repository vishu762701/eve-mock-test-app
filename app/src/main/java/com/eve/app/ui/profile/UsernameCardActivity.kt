package com.eve.app.ui.profile

import android.content.ClipData
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
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.eve.app.R
import com.eve.app.ui.common.EveBaseActivity
import com.eve.app.util.AppBulletin
import com.google.zxing.BinaryBitmap
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
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

    // Android's camera Activity returns a still; this is not Telegram's live scanner.
    private val scanPhoto = registerForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { photo ->
        if (photo == null) return@registerForActivityResult
        try {
            val pixels = IntArray(photo.width * photo.height)
            photo.getPixels(pixels, 0, photo.width, 0, 0, photo.width, photo.height)
            val source = RGBLuminanceSource(photo.width, photo.height, pixels)
            val code = QRCodeReader().decode(BinaryBitmap(HybridBinarizer(source))).text
            if (ProfileQrPayload.parse(code) == null) {
                AppBulletin.showError(this, "Only EVE username QR links are supported")
            } else {
                startActivity(Intent(this, UsernameCardActivity::class.java).setData(Uri.parse(code)))
            }
        } catch (_: Exception) {
            AppBulletin.showError(this, "QR not detected. Try a clearer photo.")
        } finally {
            photo.recycle()
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
                ". Username cards do not prove account ownership."
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
            setPalette(savedInstanceState?.getInt("qr_palette", 0) ?: 0)
        }
        root.addView(scene, FrameLayout.LayoutParams(-1, -1))

        val close = icon(R.drawable.eve_qr_close, "Close QR screen") { finish() }
        val scan = scanButton {
            try { scanPhoto.launch(null) }
            catch (_: Exception) { AppBulletin.showError(this, "No camera available") }
        }
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
        tools.addView(tool("Colors", "Change QR color") {
            scene.setPalette((scene.selectedPalette + 1) % 3)
        }, LinearLayout.LayoutParams(0, px(48), 1f))
        tools.addView(tool("Copy", "Copy EVE profile link") {
            (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager)
                .setPrimaryClip(ClipData.newPlainText("EVE profile link", qrLink))
            AppBulletin.showSuccess(this, "Profile link copied")
        }, LinearLayout.LayoutParams(0, px(48), 1f))

        val closeParams = FrameLayout.LayoutParams(px(48), px(48), Gravity.TOP or Gravity.START)
        val scanParams = FrameLayout.LayoutParams(-2, px(48), Gravity.TOP or Gravity.END)
        val shareParams = FrameLayout.LayoutParams(px(48), px(48), Gravity.BOTTOM or Gravity.START)
        val saveParams = FrameLayout.LayoutParams(px(48), px(48), Gravity.BOTTOM or Gravity.END)
        val toolsParams = FrameLayout.LayoutParams(px(152), px(48), Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL)
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
            contentDescription = "Scan EVE QR using camera photo"
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
        if (::scene.isInitialized) outState.putInt("qr_palette", scene.selectedPalette)
        super.onSaveInstanceState(outState)
    }
}
