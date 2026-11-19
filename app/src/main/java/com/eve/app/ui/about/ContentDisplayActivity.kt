package com.eve.app.ui.about

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.eve.app.R
import com.eve.app.data.model.AppContent
import com.eve.app.data.repository.AppContentRepository
import com.eve.app.theme.TelegramThemeEngine
import com.eve.app.ui.common.EveBaseActivity
import com.eve.app.util.AppBulletin
import kotlinx.coroutines.launch
import org.telegram.messenger.AndroidUtilities
import org.telegram.ui.ActionBar.ActionBar
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.Cells.HeaderCell
import org.telegram.ui.Cells.ShadowSectionCell
import org.telegram.ui.Cells.TextDetailSettingsCell
import org.telegram.ui.Cells.TextInfoPrivacyCell
import org.telegram.ui.Components.LayoutHelper
import org.telegram.ui.Components.RadialProgressView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Settings > About > Privacy Policy / Terms of Service / Contact Us,
 * powered by real ported Telegram ActionBar, RadialProgressView, HeaderCell,
 * TextDetailSettingsCell, ShadowSectionCell, and TextInfoPrivacyCell.
 */
class ContentDisplayActivity : EveBaseActivity() {

    companion object {
        const val EXTRA_CONTENT_TYPE = "extra_content_type"
    }

    private val repo = AppContentRepository()
    private val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
    private var loadJob: kotlinx.coroutines.Job? = null

    private lateinit var rootContainer: FrameLayout
    private lateinit var actionBar: ActionBar
    private lateinit var progressView: RadialProgressView
    private lateinit var scrollView: ScrollView
    private lateinit var contentLayout: LinearLayout
    private lateinit var headerSection: HeaderCell
    private lateinit var tvBody: TextView
    private lateinit var infoLastUpdated: TextInfoPrivacyCell
    private lateinit var contactSectionContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Theme.syncFromEvePalette(this)
        val palette = TelegramThemeEngine.resolveCurrentPalette(this)

        rootContainer = FrameLayout(this).apply {
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray))
        }
        setContentView(rootContainer)

        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        rootContainer.addView(mainLayout)

        val type = intent.getStringExtra(EXTRA_CONTENT_TYPE) ?: AppContentRepository.TYPE_PRIVACY
        val defaultTitle = when (type) {
            AppContentRepository.TYPE_PRIVACY -> "Privacy Policy"
            AppContentRepository.TYPE_TERMS -> "Terms of Service"
            AppContentRepository.TYPE_CONTACT -> "Contact Us"
            else -> "Content"
        }

        // 1. Real Telegram ActionBar
        actionBar = ActionBar(this).apply {
            setBackgroundColor(Theme.getColor(Theme.key_actionBarDefault))
            setItemsBackgroundColor(Theme.getColor(Theme.key_actionBarDefaultSelector), false)
            setItemsColor(Theme.getColor(Theme.key_actionBarDefaultIcon), false)
            setTitleColor(Theme.getColor(Theme.key_actionBarDefaultTitle))
            setBackButtonImage(R.drawable.ic_ab_back)
            setTitle(defaultTitle)
            setAllowOverlayTitle(true)
            setActionBarMenuOnItemClick(object : ActionBar.ActionBarMenuOnItemClick() {
                override fun onItemClick(id: Int) {
                    if (id == -1) {
                        finish()
                    }
                }
            })
        }
        mainLayout.addView(actionBar, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))

        // 2. Content Frame (ScrollView + Real Telegram RadialProgressView)
        val bodyFrame = FrameLayout(this)
        mainLayout.addView(bodyFrame, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 0, 1f))

        scrollView = ScrollView(this).apply {
            isFillViewport = true
            visibility = View.GONE
        }
        contentLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        scrollView.addView(contentLayout)
        bodyFrame.addView(scrollView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT.toFloat()))

        val cardBg = Theme.getColor(Theme.key_windowBackgroundWhite)

        // Document Card
        val docCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(cardBg)
        }
        headerSection = HeaderCell(this).apply {
            setText(defaultTitle)
        }
        docCard.addView(headerSection, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))

        tvBody = TextView(this).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15f)
            setTextColor(palette.textPrimary)
            setLineSpacing(AndroidUtilities.dp(4f).toFloat(), 1f)
            setPadding(
                AndroidUtilities.dp(21f),
                AndroidUtilities.dp(8f),
                AndroidUtilities.dp(21f),
                AndroidUtilities.dp(20f)
            )
        }
        docCard.addView(tvBody, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))
        contentLayout.addView(docCard, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))

        infoLastUpdated = TextInfoPrivacyCell(this).apply {
            visibility = View.GONE
        }
        contentLayout.addView(infoLastUpdated, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))

        // Contact Actions Container (populated for TYPE_CONTACT)
        contactSectionContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
        }
        contentLayout.addView(contactSectionContainer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))

        progressView = RadialProgressView(this).apply {
            setProgressColor(palette.accent)
        }
        bodyFrame.addView(progressView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER))

        loadContent(type)
    }

    override fun onDestroy() {
        loadJob?.cancel()
        loadJob = null
        super.onDestroy()
    }

    private fun loadContent(type: String) {
        progressView.visibility = View.VISIBLE
        scrollView.visibility = View.GONE

        loadJob?.cancel()
        loadJob = lifecycleScope.launch {
            val content = repo.getContent(type)
            if (isFinishing || isDestroyed) return@launch
            displayContent(type, content)
            progressView.visibility = View.GONE
            scrollView.visibility = View.VISIBLE
        }
    }

    private fun displayContent(type: String, content: AppContent) {
        val title = content.title.ifBlank {
            when (type) {
                AppContentRepository.TYPE_PRIVACY -> "Privacy Policy"
                AppContentRepository.TYPE_TERMS -> "Terms of Service"
                AppContentRepository.TYPE_CONTACT -> "Contact Us"
                else -> "Content"
            }
        }
        actionBar.setTitle(title)
        headerSection.setText(title)

        if (content.updatedAt > 0) {
            infoLastUpdated.setText("Last updated: ${dateFormat.format(Date(content.updatedAt))}")
            infoLastUpdated.visibility = View.VISIBLE
        } else {
            infoLastUpdated.visibility = View.GONE
        }

        tvBody.text = content.body

        contactSectionContainer.removeAllViews()
        if (type == AppContentRepository.TYPE_CONTACT) {
            val actions = mutableListOf<Triple<String, String, () -> Unit>>()
            if (content.supportEmail.isNotBlank()) {
                actions.add(Triple(content.supportEmail, "Support Email") { openEmail(content.supportEmail) })
            }
            if (content.phone.isNotBlank()) {
                actions.add(Triple(content.phone, "Support Phone") { openDialer(content.phone) })
            }
            if (content.website.isNotBlank()) {
                actions.add(Triple(content.website, "Official Website") { openUrl(content.website) })
            }
            if (actions.isNotEmpty()) {
                val cardBg = Theme.getColor(Theme.key_windowBackgroundWhite)
                contactSectionContainer.addView(ShadowSectionCell(this))
                val header = HeaderCell(this).apply {
                    setBackgroundColor(cardBg)
                    setText("Contact Channels")
                }
                contactSectionContainer.addView(header)
                actions.forEachIndexed { idx, (value, label, onClick) ->
                    val cell = TextDetailSettingsCell(this).apply {
                        setBackgroundColor(cardBg)
                        setTextAndValue(value, label, idx < actions.lastIndex)
                        setOnClickListener { onClick() }
                    }
                    contactSectionContainer.addView(cell)
                }
                contactSectionContainer.visibility = View.VISIBLE
            } else {
                contactSectionContainer.visibility = View.GONE
            }
        } else {
            contactSectionContainer.visibility = View.GONE
        }
    }

    private fun openEmail(email: String) {
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:$email")
                putExtra(Intent.EXTRA_SUBJECT, "Eve App Support")
            }
            startActivity(intent)
        } catch (_: Exception) {
            AppBulletin.showError(this, "No email app found")
        }
    }

    private fun openDialer(phone: String) {
        try {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
            startActivity(intent)
        } catch (_: Exception) {
            AppBulletin.showError(this, "Cannot open dialer")
        }
    }

    private fun openUrl(url: String) {
        try {
            val formatted = if (!url.startsWith("http://") && !url.startsWith("https://")) {
                "https://$url"
            } else url
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(formatted)))
        } catch (_: Exception) {
            AppBulletin.showError(this, "Cannot open link")
        }
    }
}
