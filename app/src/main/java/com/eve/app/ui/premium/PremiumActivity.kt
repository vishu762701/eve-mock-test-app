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
import com.eve.app.databinding.ActivityPremiumBinding
import com.eve.app.util.AppBulletin
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class PremiumActivity : EveBaseActivity() {

    private lateinit var binding: ActivityPremiumBinding

    private var currentPlan: PremiumPlanDto? = null
    private var currentStatus: PremiumStatusDto? = null

    private val checkoutLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            AppBulletin.showSuccess(this, "Premium activated successfully!")
            loadData()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPremiumBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnBuyPremium.setOnClickListener {
            showPaymentMethodPicker()
        }

        binding.btnExtendPlan.setOnClickListener {
            showPaymentMethodPicker()
        }

        binding.starViewNotPremium.fallbackView = binding.ivHeaderStarFallback
        binding.starViewNotPremium.lazyMode = true
        binding.starViewPremiumActive.fallbackView = binding.ivActiveStarFallback
        binding.starViewPremiumActive.lazyMode = true

        loadData()
    }

    override fun onPause() {
        super.onPause()
        binding.starViewNotPremium.setPaused(true)
        binding.starViewPremiumActive.setPaused(true)
    }

    override fun onResume() {
        super.onResume()
        if (binding.layoutNotPremium.visibility == View.VISIBLE) {
            binding.starViewNotPremium.setPaused(false)
            binding.starViewNotPremium.updateTheme()
        } else if (binding.layoutPremiumActive.visibility == View.VISIBLE) {
            binding.starViewPremiumActive.setPaused(false)
            binding.starViewPremiumActive.updateTheme()
        }
    }

    private fun loadData() {
        binding.progressBar.visibility = View.VISIBLE
        binding.scrollView.visibility = View.INVISIBLE

        lifecycleScope.launch {
            val planRes = PremiumRepository.getPlan()
            val statusRes = PremiumRepository.refreshStatus(this@PremiumActivity)

            binding.progressBar.visibility = View.GONE
            binding.scrollView.visibility = View.VISIBLE

            if (planRes.isSuccess) {
                currentPlan = planRes.getOrNull()
            }
            if (statusRes.isSuccess) {
                currentStatus = statusRes.getOrNull()
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

            binding.starViewNotPremium.setPaused(true)
            if (binding.starViewPremiumActive.isFailed) {
                binding.ivActiveStarFallback.visibility = View.VISIBLE
                binding.starViewPremiumActive.visibility = View.GONE
            } else {
                binding.starViewPremiumActive.setPaused(false)
                binding.starViewPremiumActive.updateTheme()
                binding.starViewPremiumActive.startEnterAnimation()
            }

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

            binding.starViewPremiumActive.setPaused(true)
            if (binding.starViewNotPremium.isFailed) {
                binding.ivHeaderStarFallback.visibility = View.VISIBLE
                binding.starViewNotPremium.visibility = View.GONE
            } else {
                binding.starViewNotPremium.setPaused(false)
                binding.starViewNotPremium.updateTheme()
                binding.starViewNotPremium.startEnterAnimation()
            }

            binding.tvPlanTitle.text = plan.planName.ifBlank { "EVE Premium" }
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
            binding.layoutBenefitsList.removeAllViews()
            val density = resources.displayMetrics.density
            val padY = (4 * density).toInt()
            val iconSize = (18 * density).toInt()
            val marginS = (10 * density).toInt()

            for (benefit in benefitsList) {
                val row = android.widget.LinearLayout(this).apply {
                    orientation = android.widget.LinearLayout.HORIZONTAL
                    gravity = android.view.Gravity.CENTER_VERTICAL
                    setPadding(0, padY, 0, padY)
                }
                val icon = android.widget.ImageView(this).apply {
                    layoutParams = android.widget.LinearLayout.LayoutParams(iconSize, iconSize)
                    setImageResource(R.drawable.ic_check_circle)
                    setColorFilter(androidx.core.content.ContextCompat.getColor(this@PremiumActivity, R.color.eve_text))
                }
                val text = android.widget.TextView(this).apply {
                    layoutParams = android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                        marginStart = marginS
                    }
                    this.text = benefit
                    setTextColor(androidx.core.content.ContextCompat.getColor(this@PremiumActivity, R.color.eve_text))
                    textSize = 13.5f
                }
                row.addView(icon)
                row.addView(text)
                binding.layoutBenefitsList.addView(row)
            }
        }
    }

    private fun showPaymentMethodPicker() {
        val plan = currentPlan ?: PremiumPlanDto()
        val sheet = PaymentMethodBottomSheet.newInstance(
            priceInr = plan.priceInr,
            qrEnabled = plan.qrEnabled,
            upiEnabled = plan.upiEnabled
        ) { selectedMethod ->
            createOrderAndCheckout(selectedMethod)
        }
        sheet.show(supportFragmentManager, "PaymentMethodSheet")
    }

    private fun createOrderAndCheckout(method: String) {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            val res = PremiumRepository.createOrder(method)
            binding.progressBar.visibility = View.GONE

            if (res.isSuccess) {
                val order = res.getOrNull()!!
                val intent = Intent(this@PremiumActivity, PaymentCheckoutActivity::class.java).apply {
                    putExtra(PaymentCheckoutActivity.EXTRA_ORDER_ID, order.orderId)
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
