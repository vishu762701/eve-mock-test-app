/*
 * Derived from Telegram Android FiltersView.java, FilterCreateActivity.java
 * Copyright Nikolai Kudashov, 2013-2023. GNU GPL v2 or later. See THIRD_PARTY_NOTICES.md.
 */

package com.eve.app.ui.home

import android.content.Context
import org.json.JSONArray

/**
 * Manages persisted custom order of exam categories/folders.
 * Upstream: FiltersView.java, FilterCreateActivity.java
 */
object CategoryOrderManager {

    private const val PREFS_NAME = "eve_category_prefs"
    private const val KEY_ORDER = "category_order"

    fun getSavedOrder(context: Context): List<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_ORDER, null) ?: return emptyList()
        return try {
            val array = JSONArray(json)
            val list = mutableListOf<String>()
            for (i in 0 until array.length()) {
                val item = array.optString(i)
                if (!item.isNullOrBlank()) list.add(item)
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveOrder(context: Context, order: List<String>) {
        val array = JSONArray()
        order.forEach { array.put(it) }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ORDER, array.toString())
            .apply()
    }

    fun resetOrder(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_ORDER)
            .apply()
    }
}
