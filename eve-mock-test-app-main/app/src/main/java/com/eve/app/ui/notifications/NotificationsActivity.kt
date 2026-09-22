package com.eve.app.ui.notifications

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.eve.app.databinding.ActivityNotificationsBinding
import com.eve.app.util.NotificationStore

/**
 * Bug fix: bell icon pehle sirf daily-reminder toggle tha. Ab yeh screen bell tap karne par
 * khulti hai aur asal me aayi hui notifications (naya exam alert + daily reminder) ki list
 * dikhati hai — jaisa bell icon se expect hota hai.
 */
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

        val items = NotificationStore.getAll(this)
        adapter.submit(items)
        binding.emptyGroup.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE

        // Screen khulte hi sab read mark kar do taaki Home ke bell ka red dot hat jaye
        NotificationStore.markAllRead(this)
    }
}
