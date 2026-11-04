package com.eve.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.eve.app.EveApplication
import com.eve.app.data.model.*
import com.eve.app.data.remote.ApiClient
import com.google.firebase.auth.FirebaseAuth
import java.util.concurrent.atomic.AtomicLong
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

    /** One in-memory entitlement belongs to exactly one Firebase account. */
    internal data class OwnerBoundStatus(val ownerUid: String, val status: PremiumStatusDto) {
        fun activeFor(currentUid: String?, nowMillis: Long): Boolean {
            if (ownerUid.isBlank() || currentUid != ownerUid || !status.isPremium) return false
            if (status.isLifetime) return true
            return status.expiresAt?.let { it > nowMillis } ?: true
        }
    }

    @Volatile private var cachedStatus: OwnerBoundStatus? = null
    private val refreshEpoch = AtomicLong()

    internal fun acceptsResult(ownerUid: String, liveUid: String?, requestEpoch: Long, latestEpoch: Long): Boolean =
        ownerUid.isNotBlank() && ownerUid == liveUid && requestEpoch == latestEpoch

    private fun isSameOwner(uid: String): Boolean =
        uid.isNotBlank() && FirebaseAuth.getInstance().currentUser?.uid == uid

    private fun canAcceptResponse(uid: String, requestEpoch: Long): Boolean =
        acceptsResult(uid, FirebaseAuth.getInstance().currentUser?.uid, requestEpoch, refreshEpoch.get())

    private fun getPrefs(context: Context? = null): SharedPreferences? {
        val ctx = context ?: EveApplication.instance
        return ctx?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isCurrentUserPremium(context: Context? = null): Boolean {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return false
        synchronized(this) {
            if (cachedStatus?.ownerUid != null && cachedStatus?.ownerUid != uid) {
                cachedStatus = null
                _statusFlow.value = null
            }
        }
        if (cachedStatus?.activeFor(uid, System.currentTimeMillis()) == true) return true

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
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        val requestEpoch = refreshEpoch.incrementAndGet()
        synchronized(this) {
            if (uid == null || cachedStatus?.ownerUid?.let { it != uid } == true) {
                cachedStatus = null
                _statusFlow.value = null
            }
        }
        if (uid.isNullOrBlank()) {
            return Result.failure(IllegalStateException("User not authenticated"))
        }

        return try {
            val response = ApiClient.api.getPremiumStatus()
            if (!canAcceptResponse(uid, requestEpoch)) {
                Result.failure(IllegalStateException("Premium status belongs to a stale account or request"))
            } else if (response.success && response.data != null) {
                val status = response.data
                val accepted = synchronized(this) {
                    if (!canAcceptResponse(uid, requestEpoch)) {
                        false
                    } else {
                        cachedStatus = OwnerBoundStatus(uid, status)
                        _statusFlow.value = status
                        getPrefs(context)?.edit()?.apply {
                            putBoolean("$KEY_IS_PREMIUM$uid", status.isPremium)
                            putBoolean("$KEY_IS_LIFETIME$uid", status.isLifetime)
                            putLong("$KEY_EXPIRES_AT$uid", status.expiresAt ?: 0L)
                            putString("$KEY_PLAN_NAME$uid", status.planName)
                            apply()
                        }
                        true
                    }
                }
                if (accepted) Result.success(status)
                else Result.failure(IllegalStateException("Premium status request superseded"))
            } else {
                Result.failure(Exception(response.error ?: "Failed to fetch premium status"))
            }
        } catch (cancel: kotlinx.coroutines.CancellationException) {
            throw cancel
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createOrder(paymentMethod: String): Result<CreateOrderResponse> {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
            ?: return Result.failure(IllegalStateException("User not authenticated"))
        return try {
            val response = ApiClient.api.createPremiumOrder(CreateOrderRequest(paymentMethod))
            if (!isSameOwner(uid)) {
                Result.failure(IllegalStateException("Account changed during order creation"))
            } else if (response.success && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.error ?: "Failed to create payment order"))
            }
        } catch (cancel: kotlinx.coroutines.CancellationException) {
            throw cancel
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun checkOrderStatus(orderId: String, context: Context? = null): Result<OrderStatusDto> {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
            ?: return Result.failure(IllegalStateException("User not authenticated"))
        return try {
            val response = ApiClient.api.getOrderStatus(orderId)
            if (!isSameOwner(uid)) {
                Result.failure(IllegalStateException("Account changed during payment polling"))
            } else if (response.success && response.data != null) {
                val data = response.data
                if (data.orderId != orderId) {
                    Result.failure(IllegalStateException("Payment order receipt mismatch"))
                } else {
                    if (data.isPremium) refreshStatus(context)
                    if (isSameOwner(uid)) Result.success(data)
                    else Result.failure(IllegalStateException("Account changed before payment confirmation"))
                }
            } else {
                Result.failure(Exception(response.error ?: "Failed to check order status"))
            }
        } catch (cancel: kotlinx.coroutines.CancellationException) {
            throw cancel
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun simulateSandboxPayment(orderId: String, context: Context? = null): Result<Unit> {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
            ?: return Result.failure(IllegalStateException("User not authenticated"))
        return try {
            val response = ApiClient.api.simulateSandboxPayment(orderId)
            if (!isSameOwner(uid)) {
                Result.failure(IllegalStateException("Account changed during sandbox payment"))
            } else if (response.success) {
                val refreshed = refreshStatus(context)
                if (!isSameOwner(uid) || refreshed.isFailure) {
                    Result.failure(refreshed.exceptionOrNull() ?: IllegalStateException("Premium status unconfirmed"))
                } else {
                    Result.success(Unit)
                }
            } else {
                Result.failure(Exception(response.error ?: "Sandbox payment simulation failed"))
            }
        } catch (cancel: kotlinx.coroutines.CancellationException) {
            throw cancel
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun clearCacheForLogout(uid: String, context: Context? = null) {
        synchronized(this) {
            if (cachedStatus?.ownerUid == uid || isSameOwner(uid)) {
                refreshEpoch.incrementAndGet()
                cachedStatus = null
                _statusFlow.value = null
            }
        }
        getPrefs(context)?.edit()?.apply {
            remove("$KEY_IS_PREMIUM$uid")
            remove("$KEY_IS_LIFETIME$uid")
            remove("$KEY_EXPIRES_AT$uid")
            remove("$KEY_PLAN_NAME$uid")
            apply()
        }
    }
}
