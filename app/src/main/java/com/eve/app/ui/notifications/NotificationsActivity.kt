package com.eve.app.ui.notifications

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.eve.app.R
import com.eve.app.data.remote.ApiClient
import com.eve.app.ui.common.EveBaseActivity
import com.eve.app.util.AppBulletin
import com.eve.app.util.NotificationStore
import com.eve.app.util.StoredNotification
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.telegram.messenger.AndroidUtilities
import org.telegram.ui.ActionBar.ActionBar
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.Cells.HeaderCell
import org.telegram.ui.Cells.ShadowSectionCell
import org.telegram.ui.Cells.TextDetailSettingsCell
import org.telegram.ui.Cells.TextInfoPrivacyCell
import org.telegram.ui.Components.EmptyTextProgressView
import org.telegram.ui.Components.LayoutHelper
import org.telegram.ui.Components.RecyclerListView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Notifications Inbox screen powered by real ported Telegram UI components:
 * ActionBar, RecyclerListView, EmptyTextProgressView, HeaderCell,
 * TextDetailSettingsCell, ShadowSectionCell, and TextInfoPrivacyCell.
 * Telegram counterpart: NotificationsSettingsActivity / ActionBar + RecyclerListView.
 */
class NotificationsActivity : EveBaseActivity() {

    companion object {
        private const val MENU_REFRESH = 1
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_ITEM = 1
        private const val VIEW_TYPE_SHADOW = 2
        private const val VIEW_TYPE_INFO = 3
    }

    private val adapter = TelegramNotificationAdapter()
    private val dateFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())

    private var hasEmptyPlayed = false
    private var notificationSyncJob: kotlinx.coroutines.Job? = null
    private lateinit var inboxOwner: NotificationInboxOwner
    private val auth = FirebaseAuth.getInstance()
    private val accountListener = FirebaseAuth.AuthStateListener {
        if (::inboxOwner.isInitialized && !hasOriginalAccount()) finish()
    }

    private lateinit var actionBar: ActionBar
    private lateinit var listView: RecyclerListView
    private lateinit var emptyView: EmptyTextProgressView

    private fun hasOriginalAccount(): Boolean =
        ::inboxOwner.isInitialized && inboxOwner.accepts(auth.currentUser?.uid)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // A restored inbox must not adopt a different account just because
        // FirebaseAuth has changed while Android kept the Activity task alive.
        val originalUid = if (savedInstanceState?.containsKey("inbox_owner_uid") == true) {
            savedInstanceState.getString("inbox_owner_uid")
        } else {
            auth.currentUser?.uid
        }
        inboxOwner = NotificationInboxOwner(originalUid)
        if (!hasOriginalAccount()) {
            finish()
            return
        }
        hasEmptyPlayed = savedInstanceState?.getBoolean("key_empty_played", false) ?: false

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
            setTitle("Notifications")
            createMenu().addItem(MENU_REFRESH, R.drawable.msg_retry)
            setActionBarMenuOnItemClick(object : ActionBar.ActionBarMenuOnItemClick() {
                override fun onItemClick(id: Int) {
                    when (id) {
                        -1 -> com.eve.app.util.EveNavigationHelper.finishWithTransition(this@NotificationsActivity)
                        MENU_REFRESH -> listenToNotifications(userTriggered = true)
                    }
                }
            })
        }
        root.addView(
            actionBar,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        // 2. Content container with RecyclerListView + EmptyTextProgressView
        val contentFrame = FrameLayout(this)
        root.addView(
            contentFrame,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        listView = RecyclerListView(this).apply {
            layoutManager = LinearLayoutManager(this@NotificationsActivity)
            adapter = this@NotificationsActivity.adapter
            isVerticalScrollBarEnabled = true
            scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY
            setScrollbarFadingEnabled(true)
            setScrollBarDefaultDelayBeforeFade(900)
            clipToPadding = false
            setPadding(0, 0, 0, AndroidUtilities.dp(24f))
        }
        contentFrame.addView(
            listView,
            LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT.toFloat())
        )

        emptyView = EmptyTextProgressView(this).apply {
            setShowAtCenter(true)
            setTextColor(Theme.getColor(Theme.key_emptyListPlaceholder))
            setProgressBarColor(Theme.getColor(Theme.key_progressCircle))
            setText("No notifications yet\nImportant exam updates and announcements will appear here")
            showProgress()
        }
        contentFrame.addView(
            emptyView,
            LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT.toFloat())
        )

        // Opening the inbox acknowledges local unread items (not opening Home).
        NotificationStore.markAllRead(this)
        val localItems = NotificationStore.getAll(this)
        if (localItems.isNotEmpty()) {
            renderItems(localItems)
        } else {
            listView.visibility = View.GONE
            emptyView.visibility = View.VISIBLE
            emptyView.showProgress()
        }

        listenToNotifications(userTriggered = false)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("key_empty_played", hasEmptyPlayed)
        outState.putString("inbox_owner_uid", inboxOwner.uid)
    }

    override fun onStart() {
        super.onStart()
        if (!hasOriginalAccount()) {
            finish()
            return
        }
        auth.addAuthStateListener(accountListener)
    }

    override fun onStop() {
        auth.removeAuthStateListener(accountListener)
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        if (!hasOriginalAccount()) {
            finish()
            return
        }
    }

    override fun onDestroy() {
        notificationSyncJob?.cancel()
        notificationSyncJob = null
        super.onDestroy()
    }

    private fun renderItems(items: List<StoredNotification>) {
        if (items.isEmpty()) {
            hasEmptyPlayed = true
            listView.visibility = View.GONE
            emptyView.visibility = View.VISIBLE
            emptyView.showTextView()
            actionBar.setSubtitle(null)
            adapter.submit(emptyList())
        } else {
            hasEmptyPlayed = false
            emptyView.visibility = View.GONE
            listView.visibility = View.VISIBLE
            actionBar.setSubtitle("${items.size} ${if (items.size == 1) "notification" else "notifications"}")
            adapter.submit(items)
        }
    }

    private fun listenToNotifications(userTriggered: Boolean) {
        val requestOwner = inboxOwner
        notificationSyncJob?.cancel()
        notificationSyncJob = lifecycleScope.launch {
            try {
                val res = ApiClient.apiService.getBroadcasts(50)
                // Auth may switch while the network request is suspended.
                // Never import an old response into the newly current account.
                if (!requestOwner.accepts(auth.currentUser?.uid)) {
                    finish()
                    return@launch
                }
                if (res.success) {
                    NotificationStore.importBroadcasts(this@NotificationsActivity, res.data.orEmpty())
                    NotificationStore.markAllRead(this@NotificationsActivity)
                }
                val merged = NotificationStore.getAll(this@NotificationsActivity)
                renderItems(merged)
                if (userTriggered) {
                    AppBulletin.show(this@NotificationsActivity, "Notifications updated")
                }
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (_: Exception) {
                if (!requestOwner.accepts(auth.currentUser?.uid)) {
                    finish()
                    return@launch
                }
                val local = NotificationStore.getAll(this@NotificationsActivity)
                renderItems(local)
                if (userTriggered) {
                    AppBulletin.showError(this@NotificationsActivity, "Couldn't refresh notifications")
                }
            }
        }
    }

    private inner class TelegramNotificationAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private val items = mutableListOf<StoredNotification>()

        fun submit(newItems: List<StoredNotification>) {
            items.clear()
            items.addAll(newItems)
            notifyDataSetChanged()
        }

        override fun getItemCount(): Int = if (items.isEmpty()) 0 else items.size + 3

        override fun getItemViewType(position: Int): Int = when {
            position == 0 -> VIEW_TYPE_HEADER
            position <= items.size -> VIEW_TYPE_ITEM
            position == items.size + 1 -> VIEW_TYPE_SHADOW
            else -> VIEW_TYPE_INFO
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val view: View = when (viewType) {
                VIEW_TYPE_HEADER -> HeaderCell(parent.context).apply {
                    setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite))
                }
                VIEW_TYPE_ITEM -> TextDetailSettingsCell(parent.context).apply {
                    setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite))
                    foreground = Theme.getSelectorDrawable(false)
                    setMultilineDetail(true)
                }
                VIEW_TYPE_SHADOW -> ShadowSectionCell(parent.context)
                else -> TextInfoPrivacyCell(parent.context)
            }
            view.layoutParams = RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.MATCH_PARENT,
                RecyclerView.LayoutParams.WRAP_CONTENT
            )
            return object : RecyclerView.ViewHolder(view) {}
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when (getItemViewType(position)) {
                VIEW_TYPE_HEADER -> {
                    (holder.itemView as HeaderCell).setText("Recent Announcements")
                }
                VIEW_TYPE_ITEM -> {
                    val item = items[position - 1]
                    val divider = position < items.size
                    val cell = holder.itemView as TextDetailSettingsCell
                    val timestamp = if (item.timestampMillis > 0L) {
                        dateFormat.format(Date(item.timestampMillis))
                    } else {
                        ""
                    }
                    val detail = buildString {
                        if (item.body.isNotBlank()) append(item.body.trim())
                        if (timestamp.isNotBlank()) {
                            if (isNotEmpty()) append("\n")
                            append(timestamp)
                        }
                    }
                    cell.setTextAndValue(item.title.ifBlank { "Announcement" }, detail, divider)
                }
                VIEW_TYPE_INFO -> {
                    (holder.itemView as TextInfoPrivacyCell).setText(
                        "Broadcasts and exam schedule alerts from Eve appear here automatically."
                    )
                }
            }
        }
    }
}

/** Immutable login identity captured when the inbox was opened or restored. */
internal class NotificationInboxOwner(val uid: String?) {
    fun accepts(currentUid: String?): Boolean =
        !uid.isNullOrBlank() && uid == currentUid
}
