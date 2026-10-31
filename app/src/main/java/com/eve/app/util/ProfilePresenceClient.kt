package com.eve.app.util

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.eve.app.data.remote.ApiClient
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.util.UUID

/**
 * A genuine process-foreground server lease. Not derived from last_active or a UI
 * boolean. Distinct UUIDs per foreground session support multiple devices.
 * A 404 from the older production Worker fails closed until the next process.
 */
object ProfilePresenceClient : DefaultLifecycleObserver {
    private const val HEARTBEAT_MS = 30_000L
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var heartbeat: Job? = null
    private var session: String? = null
    private var foreground = false
    @Volatile private var unsupportedByServer = false

    fun register() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        foreground = true
        startWhenAuthenticated()
    }

    override fun onStop(owner: LifecycleOwner) {
        foreground = false
        stopSession()
    }

    fun onAuthChanged(authenticated: Boolean) {
        if (!authenticated) {
            stopSession()
        } else if (foreground) {
            startWhenAuthenticated()
        }
    }

    private fun startWhenAuthenticated() {
        if (unsupportedByServer ||
            FirebaseAuth.getInstance().currentUser == null) return
        if (heartbeat?.isActive == true) return
        val id = UUID.randomUUID().toString()
        session = id
        heartbeat = scope.launch {
            while (isActive && !unsupportedByServer) {
                try {
                    val res = ApiClient.apiService.startPresence(mapOf("sessionId" to id))
                    if (!res.success) break
                } catch (error: HttpException) {
                    if (error.code() == 404) {
                        unsupportedByServer = true
                        break
                    }
                } catch (_: Exception) {
                    // Network outages cannot imply an online lease; the server TTL
                    // automatically expires unless a successful renewal arrives.
                }
                delay(HEARTBEAT_MS)
            }
        }
    }

    private fun stopSession() {
        val retiringHeartbeat = heartbeat
        retiringHeartbeat?.cancel()
        heartbeat = null
        val id = session ?: return
        session = null
        if (!unsupportedByServer) {
            scope.launch {
                // Do not let a previous in-flight renewal race ahead of the
                // foreground-session revoke request on app background.
                retiringHeartbeat?.join()
                try {
                    ApiClient.apiService.endPresence(mapOf("sessionId" to id))
                } catch (_: Exception) {
                    // Server lease expiry is the authoritative offline fallback.
                }
            }
        }
    }
}
