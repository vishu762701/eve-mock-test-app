package com.eve.app.data.repository

import com.eve.app.data.remote.ApiClient

class UserStatsRepository {

    private val api = ApiClient.api

    suspend fun getTotalUserCount(): Long = try {
        val res = api.getUserStats()
        res.data?.totalUsers ?: 0L
    } catch (_: Exception) {
        0L
    }

    suspend fun getOnlineUserCount(): Long = try {
        val res = api.getUserStats()
        res.data?.onlineUsers ?: 0L
    } catch (_: Exception) {
        0L
    }
}
