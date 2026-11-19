package com.eve.app.ui.about

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.ScrollView
import com.eve.app.R
import com.eve.app.data.repository.AppContentRepository
import com.eve.app.ui.common.EveBaseActivity
import com.eve.app.util.AppBulletin
import org.telegram.messenger.AndroidUtilities
import org.telegram.ui.ActionBar.ActionBar
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.Cells.HeaderCell
import org.telegram.ui.Cells.ShadowSectionCell
import org.telegram.ui.Cells.TextCell
import org.telegram.ui.Cells.TextInfoPrivacyCell
import org.telegram.ui.Components.IconBackgroundColors

/**
 * Settings > About screen powered by real ported Telegram UI components:
 * ActionBar, HeaderCell, TextCell with IconBackgroundColors, ShadowSectionCell,
 * and TextInfoPrivacyCell.
 */
class AboutActivity : EveBaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Theme.syncFromEvePalette(this)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray))
        }
        setContentView(root)

        val actionBar = ActionBar(this).apply {
            setBackgroundColor(Theme.getColor(Theme.key_actionBarDefault))
            setItemsBackgroundColor(Theme.getColor(Theme.key_actionBarDefaultSelector), false)
            setItemsColor(Theme.getColor(Theme.key_actionBarDefaultIcon), false)
            setTitleColor(Theme.getColor(Theme.key_actionBarDefaultTitle))
            setBackButtonImage(R.drawable.ic_ab_back)
            setTitle("About Eve")
            setActionBarMenuOnItemClick(object : ActionBar.ActionBarMenuOnItemClick() {
                override fun onItemClick(id: Int) {
                    if (id == -1) finish()
                }
            })
        }
        root.addView(actionBar, matchWrap())

        val scrollView = ScrollView(this).apply { isFillViewport = true }
        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, AndroidUtilities.dp(24f))
        }
        scrollView.addView(list, matchWrap())
        root.addView(scrollView, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))

        list.addView(HeaderCell(this).apply {
            setText("Legal & Support")
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite))
        }, matchWrap())

        val rowPrivacy = TextCell(this).apply {
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite))
            foreground = Theme.getSelectorDrawable(false)
            setTextAndColorfulIcon("Privacy Policy", R.drawable.msg_info, IconBackgroundColors.BLUE.bottom, true)
            setOnClickListener { openContent(AppContentRepository.TYPE_PRIVACY) }
        }
        list.addView(rowPrivacy, matchWrap())

        val rowTerms = TextCell(this).apply {
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite))
            foreground = Theme.getSelectorDrawable(false)
            setTextAndColorfulIcon("Terms of Service", R.drawable.msg_info, IconBackgroundColors.GREEN.bottom, true)
            setOnClickListener { openContent(AppContentRepository.TYPE_TERMS) }
        }
        list.addView(rowTerms, matchWrap())

        val rowContact = TextCell(this).apply {
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite))
            foreground = Theme.getSelectorDrawable(false)
            setTextAndColorfulIcon("Contact Us", R.drawable.msg_info, IconBackgroundColors.ORANGE.bottom, false)
            setOnClickListener { openContent(AppContentRepository.TYPE_CONTACT) }
        }
        list.addView(rowContact, matchWrap())

        list.addView(ShadowSectionCell(this), matchWrap())

        val versionName = try {
            packageManager.getPackageInfo(packageName, 0).versionName
        } catch (_: Exception) {
            "1.0"
        }

        val versionCell = TextInfoPrivacyCell(this).apply {
            setText("Eve Mock Test • Version $versionName")
            setOnLongClickListener {
                if ((applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
                    AppBulletin.show(this@AboutActivity, "Test crash triggering…")
                    throw RuntimeException("Eve: Crashlytics test crash (About screen long-press)")
                }
                true
            }
        }
        list.addView(versionCell, matchWrap())
    }

    private fun openContent(contentType: String) {
        startActivity(
            Intent(this, ContentDisplayActivity::class.java).apply {
                putExtra(ContentDisplayActivity.EXTRA_CONTENT_TYPE, contentType)
            }
        )
    }

    private fun matchWrap() = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    )
}
