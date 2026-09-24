package com.eve.app.ui.notifications

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.eve.app.databinding.ActivityNotificationsBinding
import com.eve.app.util.NotificationStore
import com.eve.app.util.StoredNotification
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class NotificationsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNotificationsBinding
    private val adapter = NotificationAdapter()
    private var notificationsListener: ListenerRegistration? = null

    private var hasEmptyPlayed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hasEmptyPlayed = savedInstanceState?.getBoolean("key_empty_played", false) ?: false
        binding = ActivityNotificationsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
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

        // Real-time listener for broadcast notifications from Firestore notifications collection
        listenToNotifications()

        NotificationStore.markAllRead(this)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("key_empty_played", hasEmptyPlayed)
    }

    override fun onDestroy() {
        super.onDestroy()
        notificationsListener?.remove()
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        binding.emptyGroup.visibility = if (isEmpty) View.VISIBLE else View.GONE
        if (isEmpty) {
            hasEmptyPlayed = com.eve.app.util.EmptyStateAnimationHelper.showEmptyState(binding.lottieEmpty, hasEmptyPlayed)
        } else {
            hasEmptyPlayed = false
        }
    }

    private fun listenToNotifications() {
        notificationsListener = FirebaseFirestore.getInstance()
            .collection("notifications")
            .orderBy("sentAt", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    val local = NotificationStore.getAll(this@NotificationsActivity)
                    adapter.submit(local)
                    updateEmptyState(local.isEmpty())
                    return@addSnapshotListener
                }

                val firestoreList = snapshot?.documents?.mapNotNull { doc ->
                    val title = doc.getString("title") ?: return@mapNotNull null
                    val message = doc.getString("message") ?: return@mapNotNull null
                    val timestamp = doc.getTimestamp("sentAt")?.toDate()?.time
                        ?: doc.getLong("sentAt")
                        ?: System.currentTimeMillis()
                    StoredNotification(title, message, timestamp, true)
                }.orEmpty()

                if (firestoreList.isNotEmpty()) {
                    adapter.submit(firestoreList)
                    updateEmptyState(false)
                } else {
                    val local = NotificationStore.getAll(this@NotificationsActivity)
                    adapter.submit(local)
                    updateEmptyState(local.isEmpty())
                }
            }
    }
}
