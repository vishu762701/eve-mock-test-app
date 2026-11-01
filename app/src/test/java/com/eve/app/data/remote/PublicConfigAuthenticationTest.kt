package com.eve.app.data.remote

import okhttp3.*
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import java.lang.reflect.Proxy

class PublicConfigAuthenticationTest {
    @Test fun publicReadNeverTouchesFirebaseEvenWithStaleAuthorizationHeader() {
        val request = Request.Builder().url("https://example.test/api/app-config")
            .header("Authorization", "Bearer stale-test-token").build()
        var proceeded: Request? = null
        val chain = Proxy.newProxyInstance(Interceptor.Chain::class.java.classLoader,
            arrayOf(Interceptor.Chain::class.java)) { _, method, args ->
            when (method.name) {
                "request" -> request
                "proceed" -> {
                    proceeded = args!![0] as Request
                    Response.Builder().request(proceeded!!).protocol(Protocol.HTTP_1_1)
                        .code(200).message("OK").body("{}".toResponseBody()).build()
                }
                else -> error("Unexpected chain call: ${method.name}")
            }
        } as Interceptor.Chain
        // No Firebase app exists in this JVM: any token lookup fails this test.
        AuthInterceptor().intercept(chain).close()
        assertNull(proceeded!!.header("Authorization"))
        assertEquals("no-cache", proceeded!!.header("Cache-Control"))
    }
}
