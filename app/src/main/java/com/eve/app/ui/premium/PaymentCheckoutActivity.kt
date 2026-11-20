package com.eve.app.ui.premium

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.lifecycle.lifecycleScope
import com.eve.app.R
import com.eve.app.data.repository.PremiumRepository
import com.eve.app.ui.common.EveBaseActivity
import com.eve.app.util.AppBulletin
import com.eve.app.util.HapticHelper
import com.eve.app.util.QrCodeGenerator
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.telegram.messenger.AndroidUtilities
import org.telegram.ui.ActionBar.ActionBar
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.Cells.HeaderCell
import org.telegram.ui.Cells.ShadowSectionCell
import org.telegram.ui.Cells.TextCell
import org.telegram.ui.Cells.TextDetailSettingsCell
import org.telegram.ui.Cells.TextInfoPrivacyCell
import org.telegram.ui.Components.IconBackgroundColors
import org.telegram.ui.Stories.recorder.ButtonWithCounterView
import java.util.Locale

/**
 * Payment Checkout screen powered by real ported Telegram UI components:
 * ActionBar, HeaderCell, TextDetailSettingsCell, TextCell, ShadowSectionCell,
 * TextInfoPrivacyCell, and ButtonWithCounterView.
 */
class PaymentCheckoutActivity : EveBaseActivity() {

    private var orderId: String = ""
    private var amount: Int = 99
    private var currency: String = "INR"
    private var paymentMethod: String = "qr"
    private var expiresAt: Long = 0L
    private var upiUri: String = ""
    private var qrData: String = ""
    private var planName: String = "Premium Pro"

    private var timerJob: Job? = null
    private var pollJob: Job? = null
    private var isCompleted = false
    private var checkoutOwnerUid: String? = null
    private val checkoutAuth = FirebaseAuth.getInstance()

    private lateinit var rootView: LinearLayout
    private lateinit var actionBar: ActionBar
    private lateinit var statusCell: TextDetailSettingsCell
    private lateinit var timerCell: TextDetailSettingsCell
    private lateinit var qrSection: LinearLayout
    private lateinit var qrImageView: ImageView
    private lateinit var upiSection: LinearLayout
    private lateinit var expiredSection: LinearLayout
    private lateinit var sandboxSection: LinearLayout
    private lateinit var btnSimulateSandbox: ButtonWithCounterView

    private fun hasOriginalAccount(): Boolean =
        !checkoutOwnerUid.isNullOrBlank() &&
            checkoutOwnerUid == checkoutAuth.currentUser?.uid &&
            !isFinishing && !isDestroyed

    private val checkoutAuthListener = FirebaseAuth.AuthStateListener {
        if (checkoutOwnerUid != null && !hasOriginalAccount()) finish()
    }

    companion object {
        const val EXTRA_ORDER_ID = "extra_order_id"
        const val EXTRA_OWNER_UID = "extra_owner_uid"
        const val EXTRA_AMOUNT = "extra_amount"
        const val EXTRA_CURRENCY = "extra_currency"
        const val EXTRA_METHOD = "extra_method"
        const val EXTRA_EXPIRES_AT = "extra_expires_at"
        const val EXTRA_UPI_URI = "extra_upi_uri"
        const val EXTRA_QR_DATA = "extra_qr_data"
        const val EXTRA_PLAN_NAME = "extra_plan_name"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val launchedOwner = intent.getStringExtra(EXTRA_OWNER_UID)
        val restoredOwner = savedInstanceState?.getString("checkout_owner_uid")
        if (launchedOwner.isNullOrBlank() ||
            launchedOwner != FirebaseAuth.getInstance().currentUser?.uid ||
            (restoredOwner != null && restoredOwner != launchedOwner)
        ) {
            finish()
            return
        }
        checkoutOwnerUid = launchedOwner

        orderId = intent.getStringExtra(EXTRA_ORDER_ID) ?: ""
        amount = intent.getIntExtra(EXTRA_AMOUNT, 99)
        currency = intent.getStringExtra(EXTRA_CURRENCY) ?: "INR"
        paymentMethod = intent.getStringExtra(EXTRA_METHOD) ?: "qr"
        expiresAt = intent.getLongExtra(EXTRA_EXPIRES_AT, System.currentTimeMillis() + 600000L)
        upiUri = intent.getStringExtra(EXTRA_UPI_URI) ?: ""
        qrData = intent.getStringExtra(EXTRA_QR_DATA) ?: upiUri
        planName = intent.getStringExtra(EXTRA_PLAN_NAME) ?: "Premium Pro"

        setupViews()
        startCountdownTimer()
        startAutomaticPaymentPolling()
    }

    override fun onStart() {
        super.onStart()
        if (!hasOriginalAccount()) { finish(); return }
        checkoutAuth.addAuthStateListener(checkoutAuthListener)
    }

    override fun onStop() {
        checkoutAuth.removeAuthStateListener(checkoutAuthListener)
        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("checkout_owner_uid", checkoutOwnerUid)
    }

    override fun onResume() {
        super.onResume()
        if (!hasOriginalAccount()) { finish(); return }
    }

    private fun setupViews() {
        Theme.syncFromEvePalette(this)

        rootView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray))
        }
        setContentView(rootView)
        com.eve.app.util.SystemBarHelper.syncSystemBars(this)

        actionBar = ActionBar(this).apply {
            setBackgroundColor(Theme.getColor(Theme.key_actionBarDefault))
            setItemsBackgroundColor(Theme.getColor(Theme.key_actionBarDefaultSelector), false)
            setItemsColor(Theme.getColor(Theme.key_actionBarDefaultIcon), false)
            setTitleColor(Theme.getColor(Theme.key_actionBarDefaultTitle))
            setSubtitleColor(Theme.getColor(Theme.key_actionBarDefaultSubtitle))
            setBackButtonImage(R.drawable.ic_ab_back)
            setTitle("Checkout")
            setSubtitle("₹$amount • $planName")
            setActionBarMenuOnItemClick(object : ActionBar.ActionBarMenuOnItemClick() {
                override fun onItemClick(id: Int) {
                    if (id == -1) finish()
                }
            })
        }
        rootView.addView(actionBar, matchWrap())

        val scrollView = ScrollView(this).apply { isFillViewport = true }
        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, AndroidUtilities.dp(24f))
        }
        scrollView.addView(list, matchWrap())
        rootView.addView(scrollView, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))

        // Order Summary section
        list.addView(HeaderCell(this).apply {
            setText("Order Summary")
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite))
        }, matchWrap())

        list.addView(TextDetailSettingsCell(this).apply {
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite))
            setTextAndValue("₹$amount ($currency)", planName, true)
        }, matchWrap())

        timerCell = TextDetailSettingsCell(this).apply {
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite))
            setTextAndValue("Complete payment within --:--", "Session countdown", true)
        }
        list.addView(timerCell, matchWrap())

        statusCell = TextDetailSettingsCell(this).apply {
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite))
            setMultilineDetail(true)
            setTextAndValue("Waiting for payment confirmation…", "Automatic verification active", false)
        }
        list.addView(statusCell, matchWrap())
        list.addView(ShadowSectionCell(this), matchWrap())

        // QR section
        qrSection = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite))
            visibility = View.GONE
        }
        qrSection.addView(HeaderCell(this).apply {
            setText("Scan QR Code to Pay")
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite))
        }, matchWrap())

        qrImageView = ImageView(this).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            val pad = AndroidUtilities.dp(16f)
            setPadding(pad, pad, pad, pad)
        }
        qrSection.addView(
            qrImageView,
            LinearLayout.LayoutParams(AndroidUtilities.dp(240f), AndroidUtilities.dp(240f)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                topMargin = AndroidUtilities.dp(8f)
                bottomMargin = AndroidUtilities.dp(8f)
            }
        )

        qrSection.addView(TextCell(this).apply {
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite))
            foreground = Theme.getSelectorDrawable(false)
            setTextAndColorfulIcon("Copy UPI Payment Link", R.drawable.msg_info, IconBackgroundColors.BLUE.bottom, false)
            setOnClickListener { copyToClipboard(upiUri, "UPI payment link copied") }
        }, matchWrap())
        qrSection.addView(ShadowSectionCell(this), matchWrap())
        list.addView(qrSection, matchWrap())

        // UPI App section
        upiSection = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite))
            visibility = View.GONE
        }
        upiSection.addView(HeaderCell(this).apply {
            setText("Pay via UPI App")
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite))
        }, matchWrap())
        val upiBtnWrap = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val padH = AndroidUtilities.dp(16f)
            val padV = AndroidUtilities.dp(12f)
            setPadding(padH, padV, padH, padV)
        }
        val btnLaunchUpi = ButtonWithCounterView(this, null).apply {
            setRound()
            setText("Open UPI App • ₹$amount", false)
            setOnClickListener { launchUpiIntent() }
        }
        upiBtnWrap.addView(
            btnLaunchUpi,
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, AndroidUtilities.dp(48f))
        )
        upiSection.addView(upiBtnWrap, matchWrap())
        upiSection.addView(ShadowSectionCell(this), matchWrap())
        list.addView(upiSection, matchWrap())

        // Expired section
        expiredSection = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite))
            visibility = View.GONE
        }
        expiredSection.addView(TextDetailSettingsCell(this).apply {
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite))
            setTextAndValue("Payment Session Expired", "Please start a new checkout session to continue.", false)
        }, matchWrap())
        val restartWrap = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val padH = AndroidUtilities.dp(16f)
            val padV = AndroidUtilities.dp(12f)
            setPadding(padH, padV, padH, padV)
        }
        val btnRestart = ButtonWithCounterView(this, null).apply {
            setRound()
            setText("Restart Payment", false)
            setOnClickListener { finish() }
        }
        restartWrap.addView(
            btnRestart,
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, AndroidUtilities.dp(48f))
        )
        expiredSection.addView(restartWrap, matchWrap())
        expiredSection.addView(ShadowSectionCell(this), matchWrap())
        list.addView(expiredSection, matchWrap())

        // Sandbox verification section (Requirement 53)
        sandboxSection = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite))
            val padH = AndroidUtilities.dp(16f)
            val padV = AndroidUtilities.dp(12f)
            setPadding(padH, padV, padH, padV)
        }
        btnSimulateSandbox = ButtonWithCounterView(this, false, null).apply {
            setRound()
            setNeutral()
            setText("Simulate Sandbox Payment", false)
            setOnClickListener {
                if (!hasOriginalAccount()) { finish(); return@setOnClickListener }
                isEnabled = false
                setLoading(true)
                lifecycleScope.launch {
                    statusCell.setTextAndValue("Simulating verified payment webhook…", "Please wait", false)
                    val res = PremiumRepository.simulateSandboxPayment(orderId, this@PaymentCheckoutActivity)
                    if (!hasOriginalAccount()) { finish(); return@launch }
                    setLoading(false)
                    if (res.isSuccess) {
                        onPaymentSuccess()
                    } else {
                        isEnabled = true
                        AppBulletin.showError(
                            this@PaymentCheckoutActivity,
                            res.exceptionOrNull()?.message ?: "Simulation failed"
                        )
                    }
                }
            }
        }
        sandboxSection.addView(
            btnSimulateSandbox,
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, AndroidUtilities.dp(48f))
        )
        list.addView(sandboxSection, matchWrap())

        list.addView(TextInfoPrivacyCell(this).apply {
            setText("Keep this screen open until payment confirmation completes. Premium activates automatically.")
        }, matchWrap())

        if (paymentMethod == "qr") {
            qrSection.visibility = View.VISIBLE
            upiSection.visibility = View.GONE
            lifecycleScope.launch(Dispatchers.Default) {
                val bitmap = QrCodeGenerator.generateBitmap(qrData, 512)
                withContext(Dispatchers.Main) {
                    if (!hasOriginalAccount()) { finish(); return@withContext }
                    qrImageView.setImageBitmap(bitmap)
                }
            }
        } else {
            qrSection.visibility = View.GONE
            upiSection.visibility = View.VISIBLE
            launchUpiIntent()
        }
    }

    private fun launchUpiIntent() {
        if (!hasOriginalAccount()) { finish(); return }
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(upiUri)
            }
            val chooser = Intent.createChooser(intent, "Pay ₹$amount with")
            startActivity(chooser)
        } catch (_: Exception) {
            AppBulletin.showError(this, "No UPI app found on device")
        }
    }

    private fun copyToClipboard(text: String, message: String) {
        if (!hasOriginalAccount()) { finish(); return }
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("UPI Payment", text)
        clipboard.setPrimaryClip(clip)
        AppBulletin.showSuccess(this, message)
    }

    private fun startCountdownTimer() {
        timerJob?.cancel()
        timerJob = lifecycleScope.launch {
            while (isActive && !isCompleted) {
                if (!hasOriginalAccount()) { finish(); break }
                val now = System.currentTimeMillis()
                val remainingMs = expiresAt - now
                if (remainingMs <= 0) {
                    onSessionExpired()
                    break
                }

                val totalSeconds = (remainingMs / 1000).toInt()
                val minutes = totalSeconds / 60
                val seconds = totalSeconds % 60
                timerCell.setTextAndValue(
                    String.format(Locale.US, "Complete payment within %02d:%02d", minutes, seconds),
                    "Session countdown",
                    true
                )
                delay(1000)
            }
        }
    }

    private fun startAutomaticPaymentPolling() {
        pollJob?.cancel()
        pollJob = lifecycleScope.launch {
            while (isActive && !isCompleted) {
                delay(3000)
                if (!hasOriginalAccount()) { finish(); break }
                val result = PremiumRepository.checkOrderStatus(orderId, this@PaymentCheckoutActivity)
                if (!hasOriginalAccount()) { finish(); break }
                if (result.isSuccess) {
                    val status = result.getOrNull()
                    if (status != null) {
                        if (status.status == "SUCCESS" || status.isPremium) {
                            onPaymentSuccess()
                            break
                        } else if (status.status == "EXPIRED") {
                            onSessionExpired()
                            break
                        } else if (status.status == "FAILED" || status.status == "CANCELLED") {
                            statusCell.setTextAndValue("Payment was not completed.", "Try again or choose another method", false)
                        }
                    }
                }
            }
        }
    }

    private fun onPaymentSuccess() {
        if (!hasOriginalAccount()) { finish(); return }
        if (isCompleted) return
        isCompleted = true
        timerJob?.cancel()
        pollJob?.cancel()

        HapticHelper.performSubmitSuccess(rootView)
        timerCell.visibility = View.GONE
        qrSection.visibility = View.GONE
        upiSection.visibility = View.GONE
        expiredSection.visibility = View.GONE
        sandboxSection.visibility = View.GONE

        statusCell.setTextAndValue("Payment successful! Premium activated ✓", "Redirecting…", false)

        lifecycleScope.launch {
            delay(1500)
            if (!hasOriginalAccount()) { finish(); return@launch }
            setResult(RESULT_OK)
            finish()
        }
    }

    private fun onSessionExpired() {
        if (!hasOriginalAccount()) { finish(); return }
        if (isCompleted) return
        isCompleted = true
        timerJob?.cancel()
        pollJob?.cancel()

        timerCell.setTextAndValue("Session expired", "Start a new payment to continue", true)
        qrSection.visibility = View.GONE
        upiSection.visibility = View.GONE
        sandboxSection.visibility = View.GONE
        expiredSection.visibility = View.VISIBLE
    }

    override fun onDestroy() {
        super.onDestroy()
        timerJob?.cancel()
        pollJob?.cancel()
    }

    private fun matchWrap() = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    )
}
