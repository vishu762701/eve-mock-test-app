package com.eve.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.eve.app.EveApplication
import com.eve.app.data.model.*
import com.eve.app.data.remote.ApiClient
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object PremiumRepository {

    private const val PREFS_NAME = "eve_premium_state"
    private const val KEY_IS_PREMIUM = "is_premium_"
    private const val KEY_EXPIRES_AT = "expires_at_"
    private const val KEY_IS_LIFETIME = "is_lifetime_"
    private const val KEY_PLAN_NAME = "plan_name_"

    private val _statusFlow = MutableStateFlow<PremiumStatusDto?>(null)
    val statusFlow: StateFlow<PremiumStatusDto?> = _statusFlow.asStateFlow()

    private var cachedStatus: PremiumStatusDto? = null

    private fun getPrefs(context: Context? = null): SharedPreferences? {
        val ctx = context ?: EveApplication.instance
        return ctx?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isCurrentUserPremium(context: Context? = null): Boolean {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return false
        val memoryStatus = cachedStatus
        if (memoryStatus != null && memoryStatus.isPremium) {
            if (memoryStatus.isLifetime) return true
            val expires = memoryStatus.expiresAt
            if (expires == null || expires > System.currentTimeMillis()) return true
        }

        val sp = getPrefs(context) ?: return false
        val isPrem = sp.getBoolean("$KEY_IS_PREMIUM$uid", false)
        val isLife = sp.getBoolean("$KEY_IS_LIFETIME$uid", false)
        val exp = sp.getLong("$KEY_EXPIRES_AT$uid", 0L)

        if (!isPrem) return false
        if (isLife) return true
        return exp > System.currentTimeMillis()
    }

    suspend fun getPlan(): Result<PremiumPlanDto> {
        return try {
            val response = ApiClient.api.getPremiumPlan()
            if (response.success && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.error ?: "Failed to load premium plan"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun refreshStatus(context: Context? = null): Result<PremiumStatusDto> {
        val user = FirebaseAuth.getInstance().currentUser
        val uid = user?.uid
        if (uid.isNullOrBlank()) {
            cachedStatus = null
            _statusFlow.value = null
            return Result.failure(Exception("User not authenticated"))
        }

        return try {
            val response = ApiClient.api.getPremiumStatus()
            if (response.success && response.data != null) {
                val status = response.data
                cachedStatus = status
                _statusFlow.value = status

                getPrefs(context)?.edit()?.apply {
                    putBoolean("$KEY_IS_PREMIUM$uid", status.isPremium)
                    putBoolean("$KEY_IS_LIFETIME$uid", status.isLifetime)
                    putLong("$KEY_EXPIRES_AT$uid", status.expiresAt ?: 0L)
                    putString("$KEY_PLAN_NAME$uid", status.planName)
                    apply()
                }

                Result.success(status)
            } else {
                Result.failure(Exception(response.error ?: "Failed to fetch premium status"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createOrder(paymentMethod: String): Result<CreateOrderResponse> {
        return try {
            val response = ApiClient.api.createPremiumOrder(CreateOrderRequest(paymentMethod))
            if (response.success && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.error ?: "Failed to create payment order"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun checkOrderStatus(orderId: String, context: Context? = null): Result<OrderStatusDto> {
        return try {
            val response = ApiClient.api.getOrderStatus(orderId)
            if (response.success && response.data != null) {
                val data = response.data
                if (data.isPremium) {
                    // Update cache immediately
                    refreshStatus(context)
                }
                Result.success(data)
            } else {
                Result.failure(Exception(response.error ?: "Failed to check order status"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun simulateSandboxPayment(orderId: String, context: Context? = null): Result<Unit> {
        return try {
            val response = ApiClient.api.simulateSandboxPayment(orderId)
            if (response.success) {
                refreshStatus(context)
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.error ?: "Sandbox payment simulation failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun clearCacheForLogout(uid: String, context: Context? = null) {
        cachedStatus = null
        _statusFlow.value = null
        getPrefs(context)?.edit()?.apply {
            remove("$KEY_IS_PREMIUM$uid")
            remove("$KEY_IS_LIFETIME$uid")
            remove("$KEY_EXPIRES_AT$uid")
            remove("$KEY_PLAN_NAME$uid")
            apply()
        }
    }
}
