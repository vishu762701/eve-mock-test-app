/*
 * Derived from Telegram Android DialogsActivity.java.
 * Copyright Nikolai Kudashov, 2013-2018. GNU GPL v2 or later.
 */
package com.eve.app.ui.home

import android.view.View
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * DialogsActivity.java list behavior: 200ms item animator and
 * scroll-to-top on ActionBar title tap.
 */
internal object TelegramDialogsListHelper {
    fun install(recyclerView: RecyclerView, titleView: View?) {
        recyclerView.itemAnimator = DefaultItemAnimator().apply {
            addDuration = 200L
            removeDuration = 200L
            moveDuration = 200L
            changeDuration = 200L
            supportsChangeAnimations = false
        }
        titleView?.setOnClickListener {
            val lm = recyclerView.layoutManager as? LinearLayoutManager ?: return@setOnClickListener
            if (lm.findFirstVisibleItemPosition() > 10) {
                recyclerView.scrollToPosition(10)
            }
            recyclerView.smoothScrollToPosition(0)
        }
    }
}
