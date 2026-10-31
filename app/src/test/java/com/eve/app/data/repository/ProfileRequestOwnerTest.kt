package com.eve.app.data.repository

import com.eve.app.data.remote.EveApiService
import com.eve.app.data.remote.ProfileRequestOwner
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException

class ProfileRequestOwnerTest {
    @Test fun expiredSignedOutAndSwitchedAccountsCannotReuseTheInitiatingOwner() {
        val owner = ProfileRequestOwner("Alice-uid")
        owner.requireMatches("Alice-uid")
        for (other in listOf(null, "", "Bob-uid", "alice-uid")) {
            assertThrows(IOException::class.java) { owner.requireMatches(other) }
        }
        assertThrows(IllegalArgumentException::class.java) { ProfileRequestOwner("") }
    }
    @Test fun realRetrofitProfileRequestsCarryOnlyLocalOwnerMetadata() = runBlocking {
        val owner = ProfileRequestOwner("immutable-owner-uid")
        var calls = 0
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            val request = chain.request()
            assertEquals(owner, request.tag(ProfileRequestOwner::class.java))
            assertFalse(request.url.toString().contains(owner.uid))
            assertFalse(request.headers.toString().contains(owner.uid))
            request.body?.let { body ->
                val data = Buffer().also { body.writeTo(it) }.readUtf8()
                assertEquals(if (request.url.encodedPath.endsWith("presence")) "{\"sessionId\":\"fixture-session\"}" else "{\"username\":\"alice_new\"}", data)
                assertFalse(data.contains(owner.uid))
            }
            calls++
            val data = if (request.method == "GET") """{"success":true,"data":{"id":"immutable-owner-uid","email":"fixture@example.test","displayName":"Fixture","dob":"","category":"OBC","username":"alice_new"}}""" else if (request.method == "POST") """{"success":true,"data":{"expiresAt":123456}}""" else """{"success":true}"""
            Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(200).message("OK")
                .body(data.toResponseBody("application/json".toMediaType())).build()
        }.build()
        val api = Retrofit.Builder().baseUrl("https://fixture.invalid/").client(client)
            .addConverterFactory(GsonConverterFactory.create()).build().create(EveApiService::class.java)
        assertTrue(api.updateProfile(mapOf("username" to "alice_new"), owner).success)
        assertEquals(owner.uid, api.getProfile(owner).data!!.id)
        assertTrue(api.startPresence(mapOf("sessionId" to "fixture-session"), owner).success)
        assertTrue(api.endPresence(mapOf("sessionId" to "fixture-session"), owner).success)
        assertEquals(4, calls)
    }
}
