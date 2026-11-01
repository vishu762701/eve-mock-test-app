package com.eve.app.ui.profile

import com.eve.app.data.remote.UserProfileResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async

/** One identity, confirmed responses only. No disk cache of private profile fields. */
internal class ConfirmedProfileStore(
    private val scope: CoroutineScope,
    private val currentUid: () -> String?,
    private val now: () -> Long,
    private val fetch: suspend (String) -> UserProfileResponse
) {
    private var owner: String? = null
    private var generation = 0L
    private var confirmed: UserProfileResponse? = null
    private var confirmedAt = 0L
    private var pending: Deferred<UserProfileResponse>? = null
    data class Position(val scrollY: Int, val expanded: Boolean)
    private var position: Position? = null

    @Synchronized fun setOwner(uid: String?) {
        if (owner == uid) return
        generation++
        pending?.cancel()
        pending = null
        confirmed = null
        position = null
        confirmedAt = 0L
        owner = uid
    }

    @Synchronized fun peek(uid: String): UserProfileResponse? {
        setOwner(currentUid())
        return confirmed?.takeIf { owner == uid }
    }

    @Synchronized fun position(uid: String): Position? {
        setOwner(currentUid())
        return position.takeIf { owner == uid }
    }

    @Synchronized fun rememberPosition(uid: String, value: Position) {
        setOwner(currentUid())
        if (owner == uid) position = value
    }

    /** Edits invalidate freshness, while the last confirmed display stays visible. */
    @Synchronized fun invalidate(uid: String) {
        setOwner(currentUid())
        if (owner != uid) return
        generation++
        pending?.cancel()
        pending = null
        confirmedAt = Long.MIN_VALUE
    }

    /** Successful write/readback supersedes every earlier revalidation. */
    @Synchronized fun accept(uid: String, profile: UserProfileResponse) {
        setOwner(currentUid())
        check(owner == uid) { "Profile account did not match" }
        validate(uid, profile)
        invalidate(uid)
        confirmed = profile
        confirmedAt = now()
    }

    suspend fun get(uid: String): UserProfileResponse {
        val request = synchronized(this) {
            setOwner(currentUid())
            if (owner != uid) throw CancellationException("Profile owner changed")
            val age = now() - confirmedAt
            confirmed?.takeIf { confirmedAt != Long.MIN_VALUE && age in 0 until 30_000L }
                ?.let { return it }
            pending ?: run {
                val version = generation
                scope.async(start = CoroutineStart.LAZY) {
                    try {
                        val result = fetch(uid)
                        synchronized(this@ConfirmedProfileStore) {
                            if (currentUid() != uid || owner != uid || generation != version) {
                                throw CancellationException("Superseded profile response")
                            }
                            validate(uid, result)
                            confirmed = result
                            confirmedAt = now()
                        }
                        result
                    } finally {
                        synchronized(this@ConfirmedProfileStore) {
                            if (generation == version) pending = null
                        }
                    }
                }.also { pending = it }
            }
        }
        // The process owns this request: cancelling a screen/presence waiter must
        // not cancel a shared request needed by another waiter or the next screen.
        request.start()
        return request.await()
    }

    private fun validate(uid: String, profile: UserProfileResponse) {
        check(profile.id == uid) { "Profile account did not match" }
        // Gson can produce nulls for missing JSON fields even though the Kotlin
        // DTO declares these core fields non-null. Never cache such a response:
        // warm-open rendering runs before the network error handler is entered.
        val name: String? = profile.displayName
        val birthday: String? = profile.dob
        val category: String? = profile.category
        check(name != null && birthday != null && category != null) {
            "Profile response was incomplete"
        }
    }
}
