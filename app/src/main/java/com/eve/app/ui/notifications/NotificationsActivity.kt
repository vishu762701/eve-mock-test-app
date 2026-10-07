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
import kotlinx.coroutines.launch

class NotificationsActivity : EveBaseActivity() {

    private lateinit var binding: ActivityNotificationsBinding
    private val adapter = NotificationAdapter()

    private var hasEmptyPlayed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hasEmptyPlayed = savedInstanceState?.getBoolean("key_empty_played", false) ?: false
        binding = ActivityNotificationsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { com.eve.app.util.EveNavigationHelper.finishWithTransition(this) }
        binding.rvNotifications.layoutManager = LinearLayoutManager(this)
        binding.rvNotifications.adapter = adapter

        // Show cached local notifications immediately
        val localItems = NotificationStore.getAll(this)
        if (localItems.isNotEmpty()) {
            adapter.submit(localItems)
            updateEmptyState(false)
        } else {
            updateEmptyState(true)
        }

        listenToNotifications()

        NotificationStore.markAllRead(this)
        applyUiStudioConfig()
    }

    private fun applyUiStudioConfig() {
        com.eve.app.uistudio.StudioRenderer.apply(binding.root, "notifications", com.eve.app.data.repository.UiStudioRepository.getInstance().currentConfig)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("key_empty_played", hasEmptyPlayed)
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
        if (binding.lottieEmpty.isAnimating) {
            binding.lottieEmpty.pauseAnimation()
        }
    }

    override fun onResume() {
        super.onResume()
        if (binding.emptyGroup.visibility == View.VISIBLE && !hasEmptyPlayed && !binding.lottieEmpty.isAnimating) {
            binding.lottieEmpty.resumeAnimation()
        }
    }

    private fun listenToNotifications() {
        lifecycleScope.launch {
            try {
                val res = ApiClient.apiService.getBroadcasts(50)
                if (res.success && !res.data.isNullOrEmpty()) {
                    val notifList = res.data.map { b ->
                        StoredNotification(b.title, b.message, b.sentAt, true)
                    }
                    adapter.submit(notifList)
                    updateEmptyState(false)
                } else {
                    val local = NotificationStore.getAll(this@NotificationsActivity)
                    adapter.submit(local)
                    updateEmptyState(local.isEmpty())
                }
            } catch (_: Exception) {
                val local = NotificationStore.getAll(this@NotificationsActivity)
                adapter.submit(local)
                updateEmptyState(local.isEmpty())
            }
        }
    }
}
