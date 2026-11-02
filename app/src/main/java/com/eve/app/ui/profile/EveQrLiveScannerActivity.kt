package com.eve.app.ui.profile

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.eve.app.R
import com.eve.app.ui.common.EveBaseActivity
import com.eve.app.util.AppBulletin
import com.google.zxing.BinaryBitmap
import com.google.zxing.LuminanceSource
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * EVE-only live scanner, source-timed to Telegram QrScanOverlay.java:101-102,370-396.
 * ADAPTED: CameraX/ZXing, secure EVE deep-link routing; never opens Telegram or external URLs.
 * Reference: DrKLO/Telegram@3de37c8cfb039452be7398909461da6288babf15
 */
class EveQrLiveScannerActivity : EveBaseActivity() {
    private val worker = Executors.newSingleThreadExecutor()
    private val completed = AtomicBoolean(false)
    private var provider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private lateinit var root: QrScanMorphLayout
    private lateinit var preview: PreviewView
    private var closing = false
    private var cameraRequestStarted = false
    private var torchOn = false
    private var lastScanAt = 0L
    private var lastRejectedAt = 0L
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private val permission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (closing || completed.get() || isFinishing || isDestroyed) return@registerForActivityResult
        if (granted) startLiveCamera()
        else AppBulletin.showError(this, "Camera access is needed for live scanning; Gallery remains available")
    }

    /** QrScanOverlay.java:453-485 starts the camera after the 390ms opening. */
    private fun requestCameraAfterOpening() {
        if (cameraRequestStarted || closing || completed.get() || isFinishing || isDestroyed) return
        cameraRequestStarted = true
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED) startLiveCamera()
        else permission.launch(Manifest.permission.CAMERA)
    }
    private val gallery = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) scanGallery(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.BLACK

        window.setBackgroundDrawable(
            android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
        root = QrScanMorphLayout(this).apply {
            setOrigin(intent?.getFloatArrayExtra(QrScanMorphLayout.EXTRA_ORIGIN))
        }
        preview = PreviewView(this).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
            contentDescription = "Live EVE QR camera preview"
        }
        root.addView(preview, FrameLayout.LayoutParams(-1, -1))

        // QrScanOverlay.java:579-599,659-708. Source geometry, EVE CameraX host.
        root.addView(QrScannerFrameOverlay(this), FrameLayout.LayoutParams(-1, -1))
        val caption = TextView(this).apply {
            text = "Scan an EVE username QR"
            gravity = Gravity.CENTER
            textSize = 17f
            setTextColor(Color.WHITE)
            background = glass()
            setPadding(dp(12), dp(9), dp(12), dp(9))
        }
        root.addView(caption, FrameLayout.LayoutParams(-2, -2, Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM).apply {
            bottomMargin = dp(114)
        })

        val close = ImageView(this).apply {
            setImageResource(R.drawable.eve_qr_close)
            imageTintList = ColorStateList.valueOf(Color.WHITE)
            contentDescription = "Close scanner"
            setPadding(dp(12), dp(12), dp(12), dp(12))
            isClickable = true
            isFocusable = true
            background = glass()
            setOnClickListener { closeWithMotion() }
        }
        val closeParams = FrameLayout.LayoutParams(dp(48), dp(48), Gravity.TOP or Gravity.START).apply {
            leftMargin = dp(12)
        }
        root.addView(close, closeParams)

        val buttons = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        val galleryButton = actionButton("Gallery", "Scan QR from gallery") {
            gallery.launch("image/*")
        }
        val torchButton = actionButton("Flashlight", "Toggle camera torch") {
            val current = camera
            if (current?.cameraInfo?.hasFlashUnit() == true) {
                torchOn = !torchOn
                current.cameraControl.enableTorch(torchOn)
            } else {
                AppBulletin.showError(this, "Flashlight unavailable")
            }
        }
        buttons.addView(galleryButton, LinearLayout.LayoutParams(dp(126), dp(48)))
        buttons.addView(torchButton, LinearLayout.LayoutParams(dp(126), dp(48)).apply { leftMargin = dp(12) })
        val buttonsParams = FrameLayout.LayoutParams(-2, dp(48), Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL)
        root.addView(buttons, buttonsParams)
        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            closeParams.topMargin = bars.top + dp(10)
            buttonsParams.bottomMargin = bars.bottom + dp(20)
            close.layoutParams = closeParams
            buttons.layoutParams = buttonsParams
            insets
        }
        setContentView(root)
        ViewCompat.requestApplyInsets(root)
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
        // Real source geometry expands from the supplying QR screen's scan
        // button. Restored/rotated screens start at their settled target.
        if (savedInstanceState != null) {
            root.setAlreadyOpened()
            root.post { requestCameraAfterOpening() }
        } else root.post {
            if (!closing && !isFinishing && !isDestroyed)
                root.animateSourceOpen { requestCameraAfterOpening() }
        }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { closeWithMotion() }
        })
    }

    // One EVE adapter uses the existing Telegram QrGlass sheen/rim implementation.
    // The camera-backdrop blur remains separate and NOT MET.
    private fun glass() = com.eve.app.telegram.qr.QrControlBackground(dp(20).toFloat())
    private fun actionButton(title: String, description: String, onTap: () -> Unit) = TextView(this).apply {
        text = title
        textSize = 15f
        gravity = Gravity.CENTER
        setTextColor(Color.WHITE)
        contentDescription = description
        background = glass()
        isClickable = true
        isFocusable = true
        setOnClickListener { onTap() }
    }

    private fun startLiveCamera() {
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener({
            if (isDestroyed || isFinishing || closing) return@addListener
            try {
                val service = future.get()
                service.unbindAll()
                val cameraPreview = Preview.Builder().build()
                cameraPreview.setSurfaceProvider(preview.surfaceProvider)
                val analyzer = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
                analyzer.setAnalyzer(worker) { frame -> analyzeFrame(frame) }
                camera = service.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, cameraPreview, analyzer)
                provider = service
            } catch (_: Exception) {
                AppBulletin.showError(this, "Unable to start camera. Gallery is still available.")
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun decode(source: LuminanceSource): String? {
        var rotated = source
        repeat(4) {
            val result = runCatching {
                QRCodeReader().decode(BinaryBitmap(HybridBinarizer(rotated))).text
            }.getOrNull()
            if (result != null) return result
            if (!rotated.isRotateSupported) return null
            rotated = rotated.rotateCounterClockwise()
        }
        return null
    }

    private fun analyzeFrame(frame: ImageProxy) {
        try {
            if (completed.get() || closing) return
            val now = SystemClock.elapsedRealtime()
            if (now - lastScanAt < 150L) return
            lastScanAt = now
            val plane = frame.planes.firstOrNull() ?: return
            val stride = plane.rowStride
            val w = frame.width
            val h = frame.height
            if (w <= 0 || h <= 0 || stride < w) return
            val bytes = ByteArray(stride * h)
            val buffer = plane.buffer.duplicate()
            buffer.rewind()
            buffer.get(bytes, 0, minOf(bytes.size, buffer.remaining()))
            val source = PlanarYUVLuminanceSource(bytes, stride, h, 0, 0, w, h, false)
            decode(source)?.let { result -> runOnUiThread { handleLink(result) } }
        } catch (_: Exception) {
            // Malformed camera frames are not user-visible authentication failures.
        } finally {
            frame.close()
        }
    }

    private fun scanGallery(uri: Uri) {
        if (worker.isShutdown || closing || completed.get()) return
        worker.execute {
            var bitmap: android.graphics.Bitmap? = null
            try {
                // Read only image metadata first. A fixed sample size either wastes
                // memory for huge gallery files or destroys small QR modules.
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                val boundsInput = contentResolver.openInputStream(uri)
                    ?: throw IllegalArgumentException("Unable to read image bounds")
                boundsInput.use { input ->
                    BitmapFactory.decodeStream(input, null, bounds)
                }
                val sample = QrScanImageSizing.sampleSize(bounds.outWidth, bounds.outHeight)
                if (sample == 0) throw IllegalArgumentException("Unsafe image dimensions")
                bitmap = contentResolver.openInputStream(uri)?.use { input ->
                    BitmapFactory.decodeStream(input, null, BitmapFactory.Options().apply {
                        inSampleSize = sample
                        inScaled = false
                        inPreferredConfig = android.graphics.Bitmap.Config.RGB_565
                    })
                }
                val image = bitmap ?: throw IllegalArgumentException("Unsupported image")
                if (image.width <= 0 || image.height <= 0 ||
                    image.width.toLong() * image.height > QrScanImageSizing.MAX_PIXELS) {
                    throw IllegalArgumentException("Decoded image exceeds QR scan limits")
                }
                val pixels = IntArray(image.width * image.height)
                image.getPixels(pixels, 0, image.width, 0, 0, image.width, image.height)
                val result = decode(RGBLuminanceSource(image.width, image.height, pixels))
                runOnUiThread {
                    if (isDestroyed || isFinishing || closing || completed.get()) return@runOnUiThread
                    if (result == null) AppBulletin.showError(this, "No QR detected in image")
                    else handleLink(result)
                }
            } catch (_: Exception) {
                runOnUiThread {
                    if (!isDestroyed && !isFinishing && !closing && !completed.get()) {
                        AppBulletin.showError(this, "Unable to scan image")
                    }
                }
            } finally {
                bitmap?.recycle()
            }
        }
    }

    private fun handleLink(link: String) {
        if (isFinishing || closing || completed.get()) return
        if (ProfileQrPayload.parse(link) == null) {
            val now = SystemClock.elapsedRealtime()
            if (now - lastRejectedAt > 4500L) {
                lastRejectedAt = now
                AppBulletin.showError(this, "Only EVE username QR links are supported")
            }
            return
        }
        if (!completed.compareAndSet(false, true)) return
        closeScanner(Uri.parse(link))
    }

    private fun closeWithMotion() = closeScanner(null)

    private fun closeScanner(destination: Uri?) {
        if (closing || isFinishing) return
        closing = true
        completed.set(true)
        // Original QrScanOverlay captures the final texture before releasing
        // its camera. Keep only an in-memory half-sized preview until closed.
        if (::root.isInitialized) root.captureCloseFrame(preview)
        camera?.cameraControl?.enableTorch(false)
        provider?.unbindAll()
        camera = null
        root.animateSourceClose {
            if (!isDestroyed) {
                if (destination != null)
                    startActivity(Intent(this, UsernameCardActivity::class.java)
                        .setData(destination))
                finish()
                @Suppress("DEPRECATION")
                overridePendingTransition(0, 0)
            }
        }
    }

    override fun onDestroy() {
        if (::root.isInitialized) root.dispose()
        provider?.unbindAll()
        worker.shutdownNow()
        super.onDestroy()
    }
}
