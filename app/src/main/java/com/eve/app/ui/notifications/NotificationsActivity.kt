package com.eve.app.ui.notifications

import com.eve.app.ui.common.EveBaseActivity

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.eve.app.data.model.BroadcastMessage
import com.eve.app.data.remote.ApiClient
import com.eve.app.databinding.ActivityNotificationsBinding
import com.eve.app.util.NotificationStore
import com.eve.app.util.StoredNotification
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class NotificationsActivity : EveBaseActivity() {

    private lateinit var binding: ActivityNotificationsBinding
    private val adapter = NotificationAdapter()

    private var hasEmptyPlayed = false
    private var notificationSyncJob: kotlinx.coroutines.Job? = null
    private lateinit var inboxOwner: NotificationInboxOwner
    private val auth = FirebaseAuth.getInstance()
    private val accountListener = FirebaseAuth.AuthStateListener {
        if (::inboxOwner.isInitialized && !hasOriginalAccount()) finish()
    }

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
        binding = ActivityNotificationsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        com.eve.app.util.SystemBarHelper.syncSystemBars(this)
        com.eve.app.ui.home.HomeAppearance.tintNotificationBell(binding.lottieEmpty)

        com.eve.app.designsystem.DesignPress.bind(binding.btnBack) {
            com.eve.app.util.EveNavigationHelper.finishWithTransition(this)
        }
        binding.rvNotifications.layoutManager = LinearLayoutManager(this)
        binding.rvNotifications.adapter = adapter

        // Opening the inbox acknowledges local unread items (not opening Home).
        NotificationStore.markAllRead(this)
        val localItems = NotificationStore.getAll(this)
        if (localItems.isNotEmpty()) {
            adapter.submit(localItems)
            updateEmptyState(false)
        } else {
            updateEmptyState(true)
        }

        listenToNotifications()
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

    private fun updateEmptyState(isEmpty: Boolean) {
        binding.emptyGroup.visibility = if (isEmpty) View.VISIBLE else View.GONE
        if (isEmpty) {
            com.eve.app.util.EmptyStateAnimationHelper.showEmptyState(
                lottieView = binding.lottieEmpty,
                hasPlayed = hasEmptyPlayed,
                rawResId = com.eve.app.R.raw.notification_bell,
                onAnimationEnd = {
                    hasEmptyPlayed = true
                }
            )
        } else {
            hasEmptyPlayed = false
            com.eve.app.util.EmptyStateAnimationHelper.stopEmptyState(binding.lottieEmpty)
        }
    }

    override fun onPause() {
        super.onPause()
        if (::binding.isInitialized && binding.lottieEmpty.isAnimating) {
            binding.lottieEmpty.pauseAnimation()
        }
    }

    override fun onResume() {
        super.onResume()
        if (!hasOriginalAccount()) {
            finish()
            return
        }
        if (binding.emptyGroup.visibility == View.VISIBLE && !hasEmptyPlayed && !binding.lottieEmpty.isAnimating) {
            binding.lottieEmpty.resumeAnimation()
        }
    }

    override fun onDestroy() {
        notificationSyncJob?.cancel()
        notificationSyncJob = null
        if (::binding.isInitialized) {
            com.eve.app.util.EmptyStateAnimationHelper.stopEmptyState(binding.lottieEmpty)
        }
        super.onDestroy()
    }

    private fun listenToNotifications() {
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
                    // Even an empty successful server page establishes a first-sync
                    // baseline. Otherwise the first later post could be confused
                    // with historical backfill when Home resumes.
                    NotificationStore.importBroadcasts(this@NotificationsActivity, res.data.orEmpty())
                    // The user is already viewing their inbox; acknowledge delivered posts here.
                    NotificationStore.markAllRead(this@NotificationsActivity)
                }
                val merged = NotificationStore.getAll(this@NotificationsActivity)
                adapter.submit(merged)
                updateEmptyState(merged.isEmpty())
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (_: Exception) {
                if (!requestOwner.accepts(auth.currentUser?.uid)) {
                    finish()
                    return@launch
                }
                val local = NotificationStore.getAll(this@NotificationsActivity)
                adapter.submit(local)
                updateEmptyState(local.isEmpty())
            }
        }
    }
}

/** Immutable login identity captured when the inbox was opened or restored. */
internal class NotificationInboxOwner(val uid: String?) {
    fun accepts(currentUid: String?): Boolean =
        !uid.isNullOrBlank() && uid == currentUid
}
