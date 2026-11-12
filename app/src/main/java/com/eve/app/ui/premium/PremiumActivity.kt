package com.eve.app.ui.premium

import com.eve.app.ui.common.EveBaseActivity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.eve.app.R
import com.eve.app.data.model.PremiumPlanDto
import com.eve.app.data.model.PremiumStatusDto
import com.eve.app.data.repository.PremiumRepository
import com.google.firebase.auth.FirebaseAuth
import com.eve.app.databinding.ActivityPremiumBinding
import com.eve.app.util.AppBulletin
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class PremiumActivity : EveBaseActivity() {

    private lateinit var binding: ActivityPremiumBinding

    private var currentPlan: PremiumPlanDto? = null
    private var currentStatus: PremiumStatusDto? = null
    private var premiumOwnerUid: String? = null
    private val premiumAuth = FirebaseAuth.getInstance()
    private var loadJob: kotlinx.coroutines.Job? = null
    private var checkoutJob: kotlinx.coroutines.Job? = null
    private var paymentSheet: PaymentMethodBottomSheet? = null

    private fun hasOriginalAccount(): Boolean =
        !premiumOwnerUid.isNullOrBlank() &&
            premiumOwnerUid == premiumAuth.currentUser?.uid &&
            !isFinishing && !isDestroyed

    private val premiumAuthListener = FirebaseAuth.AuthStateListener {
        if (premiumOwnerUid != null && !hasOriginalAccount()) finish()
    }

    private val checkoutLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (!hasOriginalAccount()) { finish(); return@registerForActivityResult }
        if (result.resultCode == Activity.RESULT_OK) {
            AppBulletin.showSuccess(this, "Premium activated successfully!")
            loadData()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPremiumBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid
        val restoredUid = savedInstanceState?.getString("premium_owner_uid")
        if (currentUid.isNullOrBlank() || (restoredUid != null && restoredUid != currentUid)) {
            finish()
            return
        }
        premiumOwnerUid = currentUid
        com.eve.app.util.SystemBarHelper.syncSystemBars(this)

        com.eve.app.designsystem.DesignPress.bind(binding.btnBack) {
            finish()
        }

        com.eve.app.designsystem.DesignPress.bind(binding.btnBuyPremium) {
            showPaymentMethodPicker()
        }

        com.eve.app.designsystem.DesignPress.bind(binding.btnExtendPlan) {
            showPaymentMethodPicker()
        }


        loadData()
    }

    override fun onStart() {
        super.onStart()
        if (!hasOriginalAccount()) { finish(); return }
        premiumAuth.addAuthStateListener(premiumAuthListener)
    }

    override fun onStop() {
        premiumAuth.removeAuthStateListener(premiumAuthListener)
        paymentSheet?.dismissAllowingStateLoss()
        paymentSheet = null
        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("premium_owner_uid", premiumOwnerUid)
    }

    override fun onResume() {
        super.onResume()
        if (!hasOriginalAccount()) finish()
    }

    override fun onDestroy() {
        loadJob?.cancel()
        loadJob = null
        checkoutJob?.cancel()
        checkoutJob = null
        paymentSheet?.dismissAllowingStateLoss()
        paymentSheet = null
        super.onDestroy()
    }

    private fun loadData() {
        if (!hasOriginalAccount()) { finish(); return }
        val requestUid = premiumOwnerUid
        val hasData = currentPlan != null || currentStatus != null
        if (!hasData) {
            binding.progressBar.visibility = View.VISIBLE
            binding.scrollView.visibility = View.GONE
        }

        loadJob?.cancel()
        loadJob = lifecycleScope.launch {
            val planRes = PremiumRepository.getPlan()
            if (!hasOriginalAccount() || requestUid != premiumOwnerUid) return@launch
            val statusRes = PremiumRepository.refreshStatus(this@PremiumActivity)
            if (!hasOriginalAccount() || requestUid != premiumOwnerUid) return@launch

            binding.progressBar.visibility = View.GONE
            binding.scrollView.visibility = View.VISIBLE

            if (planRes.isSuccess) {
                currentPlan = planRes.getOrNull()
            }
            if (statusRes.isSuccess) {
                currentStatus = statusRes.getOrNull()
            }
            if (planRes.isFailure && statusRes.isFailure && hasData) {
                com.eve.app.util.AppBulletin.showError(this@PremiumActivity, "Couldn't refresh premium status")
            }

            renderUi()
        }
    }

    private fun renderUi() {
        val status = currentStatus
        val plan = currentPlan ?: PremiumPlanDto()

        if (status != null && status.isPremium) {
            // User is already Premium
            binding.layoutNotPremium.visibility = View.GONE
            binding.layoutPremiumActive.visibility = View.VISIBLE
            binding.tvActiveTitle.revealText("Premium Active")

            if (status.isLifetime) {
                binding.tvActiveExpiry.text = "Lifetime Premium Active"
            } else if (status.expiresAt != null && status.expiresAt > 0) {
                val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(status.expiresAt))
                binding.tvActiveExpiry.text = "Expires on: $dateStr"
            } else {
                binding.tvActiveExpiry.text = "Premium Active"
            }
        } else {
            // User is not Premium
            binding.layoutNotPremium.visibility = View.VISIBLE
            binding.layoutPremiumActive.visibility = View.GONE

            binding.tvPlanTitle.revealText(plan.planName.ifBlank { "EVE Premium" })
            if (plan.description.isNotBlank()) {
                binding.tvPlanDescription.text = plan.description
            }

            binding.tvPrice.text = "₹${plan.priceInr}"
            binding.tvValidity.text = if (plan.isLifetime) {
                "Lifetime Validity"
            } else {
                "Validity: ${plan.durationDays} Days"
            }

            val methodsText = buildList {
                if (plan.upiEnabled) add("UPI App")
                if (plan.qrEnabled) add("QR Code")
            }.joinToString(" • ")

            binding.tvPaymentMethodsNote.text = if (methodsText.isNotBlank()) {
                "Supported: $methodsText"
            } else {
                "Instant Automatic Verification"
            }

            // Dynamically populate benefits from backend configuration
            val benefitsList = if (plan.benefits.isNotEmpty()) {
                plan.benefits
            } else {
                listOf(
                    "Access to all eligible tests",
                    "Unlimited eligible reattempts",
                    "No normal 3-attempt restriction while Premium is active"
                )
            }
            PremiumBenefitRows.bind(binding.layoutBenefitsList, benefitsList)
        }
    }

    private fun showPaymentMethodPicker() {
        if (!hasOriginalAccount()) { finish(); return }
        val plan = currentPlan ?: PremiumPlanDto()
        val sheet = PaymentMethodBottomSheet.newInstance(
            priceInr = plan.priceInr,
            qrEnabled = plan.qrEnabled,
            upiEnabled = plan.upiEnabled
        ) { selectedMethod ->
            createOrderAndCheckout(selectedMethod)
        }
        paymentSheet = sheet
        sheet.show(supportFragmentManager, "PaymentMethodSheet")
    }

    private fun createOrderAndCheckout(method: String) {
        if (!hasOriginalAccount()) { finish(); return }
        val requestUid = premiumOwnerUid
        binding.progressBar.visibility = View.VISIBLE
        checkoutJob?.cancel()
        checkoutJob = lifecycleScope.launch {
            val res = PremiumRepository.createOrder(method)
            if (!hasOriginalAccount() || requestUid != premiumOwnerUid) return@launch
            binding.progressBar.visibility = View.GONE

            if (res.isSuccess) {
                val order = res.getOrNull()!!
                val intent = Intent(this@PremiumActivity, PaymentCheckoutActivity::class.java).apply {
                    putExtra(PaymentCheckoutActivity.EXTRA_ORDER_ID, order.orderId)
                    putExtra(PaymentCheckoutActivity.EXTRA_OWNER_UID, requestUid)
                    putExtra(PaymentCheckoutActivity.EXTRA_AMOUNT, order.amount)
                    putExtra(PaymentCheckoutActivity.EXTRA_CURRENCY, order.currency)
                    putExtra(PaymentCheckoutActivity.EXTRA_METHOD, order.paymentMethod)
                    putExtra(PaymentCheckoutActivity.EXTRA_EXPIRES_AT, order.expiresAt)
                    putExtra(PaymentCheckoutActivity.EXTRA_UPI_URI, order.upiUri)
                    putExtra(PaymentCheckoutActivity.EXTRA_QR_DATA, order.qrData)
                    putExtra(PaymentCheckoutActivity.EXTRA_PLAN_NAME, order.planName)
                }
                checkoutLauncher.launch(intent)
            } else {
                AppBulletin.showError(
                    this@PremiumActivity,
                    res.exceptionOrNull()?.message ?: "Could not create payment order"
                )
            }
        }
    }
}
