package com.eve.app.ui.notifications

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.eve.app.databinding.ActivityNotificationsBinding
import com.eve.app.util.NotificationStore
import com.eve.app.util.StoredNotification
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class NotificationsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNotificationsBinding
    private val adapter = NotificationAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
        binding.rvNotifications.layoutManager = LinearLayoutManager(this)
        binding.rvNotifications.adapter = adapter

        // Show cached local notifications immediately
        val localItems = NotificationStore.getAll(this)
        if (localItems.isNotEmpty()) {
            adapter.submit(localItems)
            binding.emptyGroup.visibility = View.GONE
        } else {
            binding.emptyGroup.visibility = View.VISIBLE
        }

        // Fetch latest broadcast notifications from Firestore notifications collection
        loadNotifications()

        NotificationStore.markAllRead(this)
    }

    private fun loadNotifications() {
        lifecycleScope.launch {
            try {
                val snapshot = FirebaseFirestore.getInstance()
                    .collection("notifications")
                    .orderBy("sentAt", Query.Direction.DESCENDING)
                    .limit(50)
                    .get()
                    .await()

                val firestoreList = snapshot.documents.mapNotNull { doc ->
                    val title = doc.getString("title") ?: return@mapNotNull null
                    val message = doc.getString("message") ?: return@mapNotNull null
                    val timestamp = doc.getTimestamp("sentAt")?.toDate()?.time
                        ?: doc.getLong("sentAt")
                        ?: System.currentTimeMillis()
                    StoredNotification(title, message, timestamp, true)
                }

                if (firestoreList.isNotEmpty()) {
                    adapter.submit(firestoreList)
                    binding.emptyGroup.visibility = View.GONE
                } else {
                    val local = NotificationStore.getAll(this@NotificationsActivity)
                    adapter.submit(local)
                    binding.emptyGroup.visibility = if (local.isEmpty()) View.VISIBLE else View.GONE
                }
            } catch (_: Exception) {
                val local = NotificationStore.getAll(this@NotificationsActivity)
                adapter.submit(local)
                binding.emptyGroup.visibility = if (local.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }
}
