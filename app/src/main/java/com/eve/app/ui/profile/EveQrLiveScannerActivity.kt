package com.eve.app.ui.profile

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.provider.Settings
import com.eve.app.designsystem.TelegramNativeAlertDialog
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
import androidx.camera.core.UseCaseGroup
import androidx.camera.view.transform.CoordinateTransform
import androidx.camera.view.transform.ImageProxyTransformFactory
import androidx.camera.view.transform.OutputTransform
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withResumed
import kotlinx.coroutines.launch
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.eve.app.R
import com.eve.app.telegram.qr.QrScanFound
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
    private lateinit var frameOverlay: QrScannerFrameOverlay
    @Volatile private var sharedViewPort = false
    private var closing = false
    private var cameraRequestStarted = false
    private var openingFinished = false
    private var permissionRecoveryPending = false
    private var permissionRequestInFlight = false
    private var cameraBinding = false
    private val cameraSession = QrCameraSessionGate()
    private var analysis: ImageAnalysis? = null
    private var foundNavigation: Runnable? = null
    private var galleryGeneration = 0L
    private var galleryRequested = false
    private var permissionDialog: TelegramNativeAlertDialog? = null
    private val torchState = QrTorchGate()
    private lateinit var torchButton: TextView
    private var lastScanAt = 0L
    private var lastRejectedAt = 0L
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private val permission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permissionRequestInFlight = false
        if (closing || completed.get() || isFinishing || isDestroyed) return@registerForActivityResult
        if (granted) startLiveCamera()
        else showCameraPermissionRecovery()
    }


    /** EVE-owned denial recovery: never request access or open settings silently. */
    private fun showCameraPermissionRecovery() {
        if (!cameraSession.isResumed()) {
            permissionRecoveryPending = true
            return
        }
        permissionRecoveryPending = false
        if (closing || completed.get() || isFinishing || isDestroyed ||
            permissionDialog?.isShowing == true) return
        val settings = !shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)
        permissionDialog = TelegramNativeAlertDialog.Builder(this)
            .setTitle("Camera permission")
            .setMessage(if (settings)
                "Camera access is unavailable. Enable it in Android App Settings or scan an image from Gallery."
                else "Allow camera access to scan QR codes live, or use an image from Gallery.")
            .setPositiveButton(if (settings) "App settings" else "Try again") { _, _ ->
                if (!closing && !isFinishing && !isDestroyed) {
                    if (settings) {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", packageName, null)
                        }
                        runCatching { startActivity(intent) }.onFailure {
                            AppBulletin.showError(this, "Unable to open App Settings")
                        }
                    } else launchCameraPermission()
                }
            }
            .setNegativeButton("Gallery") { _, _ ->
                if (!closing && !isFinishing && !isDestroyed) launchGallery()
            }
            .setOnDismissListener { permissionDialog = null }
            .show()
    }

    /** QrScanOverlay.java:453-485 starts the camera after the 390ms opening. */
    private fun requestCameraAfterOpening() {
        openingFinished = true
        if (cameraRequestStarted || closing || completed.get() || isFinishing || isDestroyed ||
            !cameraSession.isResumed()) return
        cameraRequestStarted = true
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED) startLiveCamera()
        else launchCameraPermission()
    }
    private fun launchCameraPermission() {
        if (permissionRequestInFlight || !cameraSession.isResumed() || closing) return
        permissionRequestInFlight = true
        try { permission.launch(Manifest.permission.CAMERA) } catch (_: Exception) {
            permissionRequestInFlight = false
            showCameraPermissionRecovery()
        }
    }

    private val gallery = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        galleryRequested = false
        if (uri != null && !closing && !isDestroyed && !isFinishing) scanGallery(uri)
    }

    private fun launchGallery() {
        if (!cameraSession.isResumed() || galleryRequested || closing || completed.get() ||
            isFinishing || isDestroyed) return
        galleryRequested = true
        try { gallery.launch("image/*") } catch (_: Exception) {
            galleryRequested = false
            AppBulletin.showError(this, "Unable to open Gallery")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraRequestStarted = savedInstanceState?.getBoolean("camera_requested") ?: false
        permissionRequestInFlight = savedInstanceState?.getBoolean("camera_permission_pending") ?: false
        galleryRequested = savedInstanceState?.getBoolean("gallery_pending") ?: false
        openingFinished = savedInstanceState != null
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
        preview.previewStreamState.observe(this) { stream ->
            root.setPreviewStreaming(stream == PreviewView.StreamState.STREAMING && !closing && camera != null)
        }

        // QrScanOverlay.java:579-599,659-708. Source geometry, EVE CameraX host.
        frameOverlay = QrScannerFrameOverlay(this)
        root.addView(frameOverlay, FrameLayout.LayoutParams(-1, -1))
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
            launchGallery()
        }
        torchButton = actionButton("Flashlight", "Turn on camera flashlight") {
            toggleTorch()
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

    // Original scanner-specific glass fill, sheen/rim and camera RenderNode.
    private fun glass() = root.scannerGlass(dp(20).toFloat())
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

    /** Source checks camera session; EVE CameraX futures own the resulting UI state. */
    private fun toggleTorch() {
        val active = camera
        if (closing || isDestroyed || isFinishing ||
            active?.cameraInfo?.hasFlashUnit() != true) {
            if (!closing) AppBulletin.showError(this, "Flashlight unavailable")
            return
        }
        val request = torchState.request() ?: return
        val future = try {
            active.cameraControl.enableTorch(request.enable)
        } catch (_: Exception) {
            torchState.complete(request, false)
            AppBulletin.showError(this, "Unable to change flashlight")
            return
        }
        future.addListener({
            val success = runCatching { future.get() }.isSuccess
            if (!torchState.complete(request, success)) return@addListener
            if (!closing && !isDestroyed && !isFinishing) {
                updateTorchButton()
                if (!success) AppBulletin.showError(this, "Unable to change flashlight")
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun updateTorchButton() {
        if (!::torchButton.isInitialized) return
        torchButton.isSelected = torchState.enabled
        torchButton.contentDescription = if (torchState.enabled)
            "Turn off camera flashlight" else "Turn on camera flashlight"
    }

    private fun startLiveCamera() {
        if (cameraBinding || provider != null || closing || completed.get() ||
            isFinishing || isDestroyed) return
        val ticket = cameraSession.beginBind() ?: return
        cameraBinding = true
        val future = try {
            ProcessCameraProvider.getInstance(this)
        } catch (_: Exception) {
            cameraBinding = false
            cameraSession.completeBind(ticket, false)
            AppBulletin.showError(this, "Unable to start camera. Gallery is still available.")
            return
        }
        future.addListener({
            // An old provider future must not clear a newer in-flight binding.
            if (!cameraSession.isBinding(ticket) || isDestroyed || isFinishing ||
                closing || completed.get()) return@addListener
            cameraBinding = false
            try {
                val service = future.get()
                service.unbindAll()
                val cameraPreview = Preview.Builder().build()
                cameraPreview.setSurfaceProvider(preview.surfaceProvider)
                val analyzer = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
                analyzer.setAnalyzer(worker) { frame -> analyzeFrame(frame, ticket) }
                analysis = analyzer
                // CameraX's shared ViewPort makes analysis-to-preview geometry
                // well-defined across crop, rotation and FILL_CENTER.
                val viewPort = preview.viewPort
                val group = UseCaseGroup.Builder().addUseCase(cameraPreview).addUseCase(analyzer)
                sharedViewPort = viewPort != null
                if (viewPort != null) group.setViewPort(viewPort)
                camera = service.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, group.build())
                provider = service
                cameraSession.completeBind(ticket, true)
                torchState.reset()
                updateTorchButton()
            } catch (_: Exception) {
                cameraSession.completeBind(ticket, false)
                analysis?.clearAnalyzer()
                analysis = null
                sharedViewPort = false
                AppBulletin.showError(this, "Unable to start camera. Gallery is still available.")
            }
        }, ContextCompat.getMainExecutor(this))
    }

    /** ZXing returns four corners only if it actually detects four points. */
    private data class DecodedQr(val text: String, val originalPoints: FloatArray?)

    private fun decode(source: LuminanceSource): DecodedQr? {
        var rotated = source
        repeat(4) { turns ->
            val result = runCatching {
                QRCodeReader().decode(BinaryBitmap(HybridBinarizer(rotated)))
            }.getOrNull()
            if (result != null) {
                val points = result.resultPoints?.takeIf { it.size == 4 }?.let { found ->
                    val xy = FloatArray(8) { i ->
                        if (i % 2 == 0) found[i / 2].x else found[i / 2].y
                    }
                    QrScanFound.toUnrotated(xy, source.width, source.height, turns)
                }
                return DecodedQr(result.text, points)
            }
            if (!rotated.isRotateSupported) return null
            rotated = rotated.rotateCounterClockwise()
        }
        return null
    }

    @androidx.annotation.OptIn(androidx.camera.view.TransformExperimental::class)
    private fun analyzeFrame(frame: ImageProxy, ticket: Long) {
        try {
            if (!cameraSession.acceptsFrame(ticket) || completed.get() || closing) return
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
            decode(source)?.let { detected ->
                // ImageProxy is closed in finally. Snapshot its transform while
                // valid; only map actual four-point results on the UI thread.
                val transform: OutputTransform? =
                    if (sharedViewPort && detected.originalPoints != null) {
                        runCatching {
                            ImageProxyTransformFactory().apply {
                                isUsingCropRect = true
                                isUsingRotationDegrees = false
                            }.getOutputTransform(frame)
                        }.getOrNull()
                    } else null
                runOnUiThread {
                    if (!cameraSession.acceptsFrame(ticket) || isDestroyed || closing ||
                        isFinishing || completed.get()) return@runOnUiThread
                    val local = if (transform != null && detected.originalPoints != null) {
                        runCatching {
                            val target = preview.outputTransform ?: error("Preview transform unavailable")
                            val xy = detected.originalPoints.copyOf()
                            CoordinateTransform(transform, target).mapPoints(xy)
                            xy
                        }.getOrNull()
                    } else null
                    handleLink(detected.text, local)
                }
            }
        } catch (_: Exception) {
            // Malformed camera frames are not user-visible authentication failures.
        } finally {
            frame.close()
        }
    }

    private fun scanGallery(uri: Uri) {
        if (worker.isShutdown || closing || completed.get()) return
        val request = ++galleryGeneration
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
                deliverGallery(request) {
                    if (result == null) AppBulletin.showError(this, "No QR detected in image")
                    else handleLink(result.text)
                }
            } catch (_: Exception) {
                deliverGallery(request) { AppBulletin.showError(this, "Unable to scan image") }
            } finally {
                bitmap?.recycle()
            }
        }
    }

    private fun deliverGallery(request: Long, action: () -> Unit) {
        // ActivityResult may arrive at STARTED. Wait for RESUMED rather than
        // losing a legitimate gallery result or navigating in the background.
        lifecycleScope.launch {
            lifecycle.withResumed {
                if (request == galleryGeneration && !closing && !completed.get() &&
                    !isFinishing && !isDestroyed) action()
            }
        }
    }

    private fun handleLink(link: String, localPoints: FloatArray? = null) {
        if (!cameraSession.isResumed() || isFinishing || closing || completed.get()) return
        if (ProfileQrPayload.parse(link) == null) {
            val now = SystemClock.elapsedRealtime()
            if (now - lastRejectedAt > 4500L) {
                lastRejectedAt = now
                AppBulletin.showError(this, "Only EVE username QR links are supported")
            }
            return
        }
        if (!completed.compareAndSet(false, true)) return
        val destination = Uri.parse(link)
        val outlined = localPoints != null && ::frameOverlay.isInitialized &&
            frameOverlay.showFound(localPoints)
        if (outlined) {
            // Exact source delay only when four genuine finder/alignment points
            // can be rendered. Back/destroy during delay must cancel navigation.
            val navigation = Runnable {
                foundNavigation = null
                if (cameraSession.isResumed() && !closing && !isFinishing && !isDestroyed)
                    closeScanner(destination)
            }
            foundNavigation = navigation
            root.postDelayed(navigation, QrScanFound.FOUND_DELAY_MS)
        } else closeScanner(destination)
    }

    private fun closeWithMotion() = closeScanner(null)

    private fun closeScanner(destination: Uri?) {
        if (closing || isFinishing) return
        closing = true
        completed.set(true)
        // Original QrScanOverlay captures the final texture before releasing
        // its camera. Keep only an in-memory half-sized preview until closed.
        if (::root.isInitialized) root.captureCloseFrame(preview)
        cancelFoundNavigation()
        galleryGeneration++
        releaseCamera(close = true)
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

    private fun cancelFoundNavigation() {
        foundNavigation?.let { if (::root.isInitialized) root.removeCallbacks(it) }
        foundNavigation = null
    }

    private fun releaseCamera(close: Boolean = false) {
        if (close) cameraSession.close() else cameraSession.pause()
        analysis?.clearAnalyzer()
        analysis = null
        cameraBinding = false
        sharedViewPort = false
        torchState.reset()
        updateTorchButton()
        runCatching { camera?.cameraControl?.enableTorch(false) }
        runCatching { provider?.unbindAll() }
        camera = null
        provider = null
        if (::root.isInitialized) root.setPreviewStreaming(false)
    }

    override fun onPause() {
        // QrShareActivity.java:1070-1077 destroys its camera on pause.
        // EVE keeps the Activity so Gallery/Settings can return, but releases
        // CameraX and invalidates old bind/decode/torch generations immediately.
        cancelFoundNavigation()
        if (!closing) {
            completed.set(false)
            if (::frameOverlay.isInitialized) frameOverlay.clearFound()
        }
        releaseCamera()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        cameraSession.resume()
        if (openingFinished && !cameraRequestStarted) requestCameraAfterOpening()
        if (permissionRecoveryPending) {
            permissionRecoveryPending = false
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) !=
                PackageManager.PERMISSION_GRANTED) showCameraPermissionRecovery()
        }
        // User may return from Android App Settings having granted permission.
        if (cameraRequestStarted && !closing && !completed.get()) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED) startLiveCamera()
            else if (!permissionRequestInFlight) showCameraPermissionRecovery()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("camera_requested", cameraRequestStarted)
        outState.putBoolean("camera_permission_pending", permissionRequestInFlight)
        outState.putBoolean("gallery_pending", galleryRequested)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        permissionDialog?.dismiss()
        permissionDialog = null
        cancelFoundNavigation()
        galleryGeneration++
        releaseCamera(close = true)
        if (::root.isInitialized) root.dispose()
        worker.shutdownNow()
        super.onDestroy()
    }
}
