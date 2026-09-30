package com.eve.app.ui.premium

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.eve.app.data.repository.PremiumRepository
import com.eve.app.databinding.ActivityPaymentCheckoutBinding
import com.eve.app.util.AppBulletin
import com.eve.app.util.HapticHelper
import com.eve.app.util.QrCodeGenerator
import kotlinx.coroutines.*

class PaymentCheckoutActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPaymentCheckoutBinding

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

    companion object {
        const val EXTRA_ORDER_ID = "extra_order_id"
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
        binding = ActivityPaymentCheckoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

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

    private fun setupViews() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.tvPlanName.text = planName
        binding.tvAmount.text = "₹$amount"

        if (paymentMethod == "qr") {
            binding.layoutQrSection.visibility = View.VISIBLE
            binding.layoutUpiSection.visibility = View.GONE

            lifecycleScope.launch(Dispatchers.Default) {
                val bitmap = QrCodeGenerator.generateBitmap(qrData, 512)
                withContext(Dispatchers.Main) {
                    binding.ivQrCode.setImageBitmap(bitmap)
                }
            }

            binding.btnCopyUpi.setOnClickListener {
                copyToClipboard(upiUri, "UPI payment link copied")
            }
        } else {
            binding.layoutQrSection.visibility = View.GONE
            binding.layoutUpiSection.visibility = View.VISIBLE

            binding.btnLaunchUpi.setOnClickListener {
                launchUpiIntent()
            }

            // Auto-launch UPI chooser on entry
            launchUpiIntent()
        }

        binding.btnRestartPayment.setOnClickListener {
            finish()
        }

        // Sandbox test verification button (Requirement 53)
        binding.btnSimulateSandbox.setOnClickListener {
            binding.btnSimulateSandbox.isEnabled = false
            lifecycleScope.launch {
                binding.tvStatusText.text = "Simulating verified payment webhook..."
                val res = PremiumRepository.simulateSandboxPayment(orderId, this@PaymentCheckoutActivity)
                if (res.isSuccess) {
                    onPaymentSuccess()
                } else {
                    binding.btnSimulateSandbox.isEnabled = true
                    AppBulletin.showError(this@PaymentCheckoutActivity, res.exceptionOrNull()?.message ?: "Simulation failed")
                }
            }
        }
    }

    private fun launchUpiIntent() {
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
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("UPI Payment", text)
        clipboard.setPrimaryClip(clip)
        AppBulletin.showSuccess(this, message)
    }

    private fun startCountdownTimer() {
        timerJob?.cancel()
        timerJob = lifecycleScope.launch {
            while (isActive && !isCompleted) {
                val now = System.currentTimeMillis()
                val remainingMs = expiresAt - now
                if (remainingMs <= 0) {
                    onSessionExpired()
                    break
                }

                val totalSeconds = (remainingMs / 1000).toInt()
                val minutes = totalSeconds / 60
                val seconds = totalSeconds % 60
                binding.tvTimer.text = String.format("Complete payment within %02d:%02d", minutes, seconds)

                delay(1000)
            }
        }
    }

    private fun startAutomaticPaymentPolling() {
        pollJob?.cancel()
        pollJob = lifecycleScope.launch {
            while (isActive && !isCompleted) {
                delay(3000)
                val result = PremiumRepository.checkOrderStatus(orderId, this@PaymentCheckoutActivity)
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
                            binding.tvStatusText.text = "Payment was not completed."
                        }
                    }
                }
            }
        }
    }

    private fun onPaymentSuccess() {
        if (isCompleted) return
        isCompleted = true
        timerJob?.cancel()
        pollJob?.cancel()

        HapticHelper.performSubmitSuccess(binding.root)
        binding.chipTimer.visibility = View.GONE
        binding.layoutQrSection.visibility = View.GONE
        binding.layoutUpiSection.visibility = View.GONE
        binding.layoutExpired.visibility = View.GONE
        binding.btnSimulateSandbox.visibility = View.GONE

        binding.cardStatus.visibility = View.VISIBLE
        binding.progressStatus.visibility = View.GONE
        binding.tvStatusText.text = "Payment successful! Premium activated ✓"
        binding.tvStatusText.setTextColor(getColor(com.eve.app.R.color.eve_status_success))

        lifecycleScope.launch {
            delay(1500)
            setResult(RESULT_OK)
            finish()
        }
    }

    private fun onSessionExpired() {
        if (isCompleted) return
        isCompleted = true
        timerJob?.cancel()
        pollJob?.cancel()

        binding.tvTimer.text = "Session expired"
        binding.cardStatus.visibility = View.GONE
        binding.layoutQrSection.visibility = View.GONE
        binding.layoutUpiSection.visibility = View.GONE
        binding.btnSimulateSandbox.visibility = View.GONE
        binding.layoutExpired.visibility = View.VISIBLE
    }

    override fun onDestroy() {
        super.onDestroy()
        timerJob?.cancel()
        pollJob?.cancel()
    }
}
