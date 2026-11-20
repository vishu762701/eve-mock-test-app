package com.eve.app.ui.premium

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.eve.app.R
import com.eve.app.data.model.PremiumPlanDto
import com.eve.app.data.model.PremiumStatusDto
import com.eve.app.data.repository.PremiumRepository
import com.eve.app.ui.common.EveBaseActivity
import com.eve.app.util.AppBulletin
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import org.telegram.messenger.AndroidUtilities
import org.telegram.ui.ActionBar.ActionBar
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.Cells.HeaderCell
import org.telegram.ui.Cells.ShadowSectionCell
import org.telegram.ui.Cells.TextCell
import org.telegram.ui.Cells.TextDetailSettingsCell
import org.telegram.ui.Cells.TextInfoPrivacyCell
import org.telegram.ui.Components.EmptyTextProgressView
import org.telegram.ui.Components.IconBackgroundColors
import org.telegram.ui.Components.LayoutHelper
import org.telegram.ui.Stories.recorder.ButtonWithCounterView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * EVE Premium screen powered by real ported Telegram UI components:
 * ActionBar, HeaderCell, TextCell with IconBackgroundColors, TextDetailSettingsCell,
 * ShadowSectionCell, TextInfoPrivacyCell, EmptyTextProgressView, and ButtonWithCounterView.
 * Telegram counterpart: PremiumPreviewFragment.java.
 */
class PremiumActivity : EveBaseActivity() {

    private var currentPlan: PremiumPlanDto? = null
    private var currentStatus: PremiumStatusDto? = null
    private var premiumOwnerUid: String? = null
    private val premiumAuth = FirebaseAuth.getInstance()
    private var loadJob: kotlinx.coroutines.Job? = null
    private var checkoutJob: kotlinx.coroutines.Job? = null
    private var paymentSheet: PaymentMethodBottomSheet? = null

    private lateinit var actionBar: ActionBar
    private lateinit var scrollView: ScrollView
    private lateinit var listContainer: LinearLayout
    private lateinit var loadingView: EmptyTextProgressView
    private var actionCtaButton: ButtonWithCounterView? = null

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
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid
        val restoredUid = savedInstanceState?.getString("premium_owner_uid")
        if (currentUid.isNullOrBlank() || (restoredUid != null && restoredUid != currentUid)) {
            finish()
            return
        }
        premiumOwnerUid = currentUid

        Theme.syncFromEvePalette(this)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray))
        }
        setContentView(root)
        com.eve.app.util.SystemBarHelper.syncSystemBars(this)

        // 1. Real Telegram ActionBar
        actionBar = ActionBar(this).apply {
            setBackgroundColor(Theme.getColor(Theme.key_actionBarDefault))
            setItemsBackgroundColor(Theme.getColor(Theme.key_actionBarDefaultSelector), false)
            setItemsColor(Theme.getColor(Theme.key_actionBarDefaultIcon), false)
            setTitleColor(Theme.getColor(Theme.key_actionBarDefaultTitle))
            setSubtitleColor(Theme.getColor(Theme.key_actionBarDefaultSubtitle))
            setBackButtonImage(R.drawable.ic_ab_back)
            setTitle("EVE Premium")
            setActionBarMenuOnItemClick(object : ActionBar.ActionBarMenuOnItemClick() {
                override fun onItemClick(id: Int) {
                    if (id == -1) finish()
                }
            })
        }
        root.addView(actionBar, matchWrap())

        // 2. Content frame with ScrollView + EmptyTextProgressView
        val contentFrame = FrameLayout(this)
        root.addView(
            contentFrame,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        scrollView = ScrollView(this).apply {
            isFillViewport = true
            visibility = View.GONE
        }
        listContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, AndroidUtilities.dp(24f))
        }
        scrollView.addView(listContainer, matchWrap())
        contentFrame.addView(
            scrollView,
            LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT.toFloat())
        )

        loadingView = EmptyTextProgressView(this).apply {
            setShowAtCenter(true)
            setTextColor(Theme.getColor(Theme.key_emptyListPlaceholder))
            setProgressBarColor(Theme.getColor(Theme.key_progressCircle))
            showProgress()
        }
        contentFrame.addView(
            loadingView,
            LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT.toFloat())
        )

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
        if (!hasOriginalAccount()) { finish(); return }
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
            loadingView.visibility = View.VISIBLE
            loadingView.showProgress()
            scrollView.visibility = View.GONE
        }

        loadJob?.cancel()
        loadJob = lifecycleScope.launch {
            val planRes = PremiumRepository.getPlan()
            if (!hasOriginalAccount() || requestUid != premiumOwnerUid) return@launch
            val statusRes = PremiumRepository.refreshStatus(this@PremiumActivity)
            if (!hasOriginalAccount() || requestUid != premiumOwnerUid) return@launch

            loadingView.visibility = View.GONE
            scrollView.visibility = View.VISIBLE

            if (planRes.isSuccess) {
                currentPlan = planRes.getOrNull()
            }
            if (statusRes.isSuccess) {
                currentStatus = statusRes.getOrNull()
            }
            if (planRes.isFailure && statusRes.isFailure && hasData) {
                AppBulletin.showError(this@PremiumActivity, "Couldn't refresh premium status")
            }

            renderUi()
        }
    }

    private fun renderUi() {
        listContainer.removeAllViews()
        actionCtaButton = null

        val status = currentStatus
        val plan = currentPlan ?: PremiumPlanDto()

        if (status != null && status.isPremium) {
            actionBar.setSubtitle("Active")
            val expiryText = when {
                status.isLifetime -> "Lifetime Premium Active"
                status.expiresAt != null && status.expiresAt > 0 -> {
                    val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(status.expiresAt))
                    "Expires on $dateStr"
                }
                else -> "Premium Active"
            }

            addHeader("Subscription Status")
            addDetailRow("Premium Active", expiryText, false)
            addShadow()

            addCtaButton("Extend Plan") { showPaymentMethodPicker() }
            addInfo("Extending your plan adds validity on top of your existing Premium period.")
        } else {
            val planTitle = plan.planName.ifBlank { "EVE Premium" }
            val validityText = if (plan.isLifetime) "Lifetime Validity" else "Validity: ${plan.durationDays} Days"
            val methodsText = buildList {
                if (plan.upiEnabled) add("UPI App")
                if (plan.qrEnabled) add("QR Code")
            }.joinToString(" • ")

            actionBar.setSubtitle("₹${plan.priceInr}")

            addHeader(planTitle)
            addDetailRow("₹${plan.priceInr}", validityText, plan.description.isNotBlank())
            if (plan.description.isNotBlank()) {
                addDetailRow("Plan Details", plan.description, false)
            }
            addShadow()

            addHeader("Included Benefits")
            val benefitsList = if (plan.benefits.isNotEmpty()) {
                plan.benefits
            } else {
                listOf(
                    "Access to all eligible tests",
                    "Unlimited eligible reattempts",
                    "No normal 3-attempt restriction while Premium is active"
                )
            }
            val colors = listOf(
                IconBackgroundColors.BLUE.bottom,
                IconBackgroundColors.PURPLE.bottom,
                IconBackgroundColors.GREEN.bottom,
                IconBackgroundColors.ORANGE.bottom
            )
            benefitsList.forEachIndexed { idx, benefit ->
                val cell = TextCell(this).apply {
                    setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite))
                    setTextAndColorfulIcon(
                        benefit,
                        R.drawable.msg_fave,
                        colors[idx % colors.size],
                        idx < benefitsList.lastIndex
                    )
                }
                listContainer.addView(cell, matchWrap())
            }
            addShadow()

            addCtaButton("Get Premium • ₹${plan.priceInr}") { showPaymentMethodPicker() }
            addInfo(
                if (methodsText.isNotBlank()) "Supported payment methods: $methodsText • Instant automatic verification."
                else "Instant automatic verification after payment."
            )
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
        actionCtaButton?.setLoading(true)
        checkoutJob?.cancel()
        checkoutJob = lifecycleScope.launch {
            val res = PremiumRepository.createOrder(method)
            if (!hasOriginalAccount() || requestUid != premiumOwnerUid) return@launch
            actionCtaButton?.setLoading(false)

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

    private fun addHeader(title: String) {
        listContainer.addView(HeaderCell(this).apply {
            setText(title)
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite))
        }, matchWrap())
    }

    private fun addDetailRow(text: String, value: String, divider: Boolean) {
        listContainer.addView(TextDetailSettingsCell(this).apply {
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite))
            setMultilineDetail(true)
            setTextAndValue(text, value, divider)
        }, matchWrap())
    }

    private fun addShadow() {
        listContainer.addView(ShadowSectionCell(this), matchWrap())
    }

    private fun addInfo(text: String) {
        listContainer.addView(TextInfoPrivacyCell(this).apply {
            setText(text)
        }, matchWrap())
    }

    private fun addCtaButton(label: String, onClick: () -> Unit) {
        val wrap = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite))
            val padH = AndroidUtilities.dp(16f)
            val padV = AndroidUtilities.dp(14f)
            setPadding(padH, padV, padH, padV)
        }
        val btn = ButtonWithCounterView(this, null).apply {
            setRound()
            setText(label, false)
            setOnClickListener { onClick() }
        }
        actionCtaButton = btn
        wrap.addView(
            btn,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                AndroidUtilities.dp(48f)
            ).apply { gravity = Gravity.CENTER_HORIZONTAL }
        )
        listContainer.addView(wrap, matchWrap())
    }

    private fun matchWrap() = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    )
}
