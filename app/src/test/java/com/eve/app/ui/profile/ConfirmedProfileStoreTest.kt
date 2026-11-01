package com.eve.app.ui.profile

import com.eve.app.data.remote.UserProfileResponse
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class ConfirmedProfileStoreTest {
    private fun profile(uid: String = "alice", name: String = "Alice") =
        UserProfileResponse(uid, email = "$uid@example.invalid", displayName = name,
            dob = "", category = "General", username = uid, bio = "Confirmed bio")

    @Test fun simultaneousProfileAndPresenceCoalesceAndWarmOpenUsesConfirmedData() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            var calls = 0
            val response = CompletableDeferred<UserProfileResponse>()
            val store = ConfirmedProfileStore(scope, { "alice" }, { 100L }) { calls++; response.await() }
            val a = async(start = CoroutineStart.UNDISPATCHED) { store.get("alice") }
            val b = async(start = CoroutineStart.UNDISPATCHED) { store.get("alice") }
            assertEquals(1, calls)
            response.complete(profile())
            assertEquals(a.await(), b.await())
            assertEquals(profile(), store.peek("alice"))
            assertEquals(profile(), store.get("alice"))
            assertEquals(1, calls)
        } finally { scope.cancel() }
    }

    @Test fun staleDataStaysVisibleOfflineAndRevalidationRecoversWithoutClearingFields() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            var time = 0L; var offline = false; var calls = 0
            val store = ConfirmedProfileStore(scope, { "alice" }, { time }) {
                calls++; if (offline) throw IOException("offline") else profile()
            }
            store.get("alice")
            time = 30_001L; offline = true
            try { store.get("alice"); fail() } catch (_: IOException) {}
            assertEquals("Confirmed bio", store.peek("alice")!!.bio)
            offline = false
            assertEquals(profile(), store.get("alice"))
            assertEquals(3, calls)
        } finally { scope.cancel() }
    }

    @Test fun cancelledScreenDoesNotCancelSharedRequestOrLoseReopenData() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val response = CompletableDeferred<UserProfileResponse>()
            var calls = 0
            val store = ConfirmedProfileStore(scope, { "alice" }, { 0L }) { calls++; response.await() }
            val screen = async(start = CoroutineStart.UNDISPATCHED) { store.get("alice") }
            screen.cancelAndJoin()
            val reopened = async(start = CoroutineStart.UNDISPATCHED) { store.get("alice") }
            response.complete(profile())
            assertEquals(profile(), reopened.await())
            assertEquals(1, calls)
        } finally { scope.cancel() }
    }

    @Test fun accountChangePurgesFieldsAndPositionAndRejectsOldAccountRead() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            var uid: String? = "alice"
            val store = ConfirmedProfileStore(scope, { uid }, { 0L }) { profile(it) }
            store.get("alice")
            store.rememberPosition("alice", ConfirmedProfileStore.Position(180, true))
            assertEquals(180, store.position("alice")!!.scrollY)
            uid = "bob"; store.setOwner(uid)
            assertNull(store.peek("alice")); assertNull(store.position("bob"))
            try { store.get("alice"); fail() } catch (_: CancellationException) {}
            assertEquals("bob", store.get("bob").id)
            uid = null; store.setOwner(uid)
            assertNull(store.peek("bob"))
        } finally { scope.cancel() }
    }

    @Test fun confirmedEditSupersedesEvenAnUncooperativeOlderResponse() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val response = CompletableDeferred<UserProfileResponse>()
            val store = ConfirmedProfileStore(scope, { "alice" }, { 0L }) {
                withContext(NonCancellable) { response.await() }
            }
            val old = async(start = CoroutineStart.UNDISPATCHED) { store.get("alice") }
            store.accept("alice", profile(name = "Edited"))
            response.complete(profile(name = "Old"))
            try { old.await(); fail() } catch (_: CancellationException) {}
            assertEquals("Edited", store.peek("alice")!!.displayName)
            assertEquals("Edited", store.get("alice").displayName)
        } finally { scope.cancel() }
    }

    @Test fun mismatchedServerIdentityNeverBecomesConfirmed() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val store = ConfirmedProfileStore(scope, { "alice" }, { 0L }) { profile("bob") }
            try { store.get("alice"); fail() } catch (_: IllegalStateException) {}
            assertNull(store.peek("alice"))
        } finally { scope.cancel() }
    }

    @Test fun malformedJsonCannotReplaceTheLastConfirmedFieldsOrSeedAWarmOpen() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val gson = com.google.gson.Gson()
            var response = profile()
            val store = ConfirmedProfileStore(scope, { "alice" }, { 0L }) { response }
            store.get("alice")
            for (missing in listOf("displayName", "dob", "category")) {
                val json = gson.toJsonTree(profile()).asJsonObject.apply { remove(missing) }
                response = gson.fromJson(json, UserProfileResponse::class.java)
                store.invalidate("alice")
                try { store.get("alice"); fail("Missing $missing was accepted into the cache") }
                catch (_: IllegalStateException) {}
                assertEquals(profile(), store.peek("alice"))
                try { store.accept("alice", response); fail("Malformed readback was accepted") }
                catch (_: IllegalStateException) {}
                assertEquals(profile(), store.peek("alice"))
            }
            val cold = ConfirmedProfileStore(scope, { "alice" }, { 0L }) { response }
            try { cold.get("alice"); fail("Malformed cold response was accepted") }
            catch (_: IllegalStateException) {}
            assertNull(cold.peek("alice"))
        } finally { scope.cancel() }
    }
}
