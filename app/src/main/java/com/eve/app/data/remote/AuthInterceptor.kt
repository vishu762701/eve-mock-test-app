package com.eve.app.data.remote

import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import okhttp3.Interceptor
import okhttp3.Response
import java.util.concurrent.TimeUnit

class AuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val user = FirebaseAuth.getInstance().currentUser ?: return chain.proceed(original)

        val requestBuilder = original.newBuilder()
        try {
            var tokenTask = user.getIdToken(false)
            var result = Tasks.await(tokenTask, 15, TimeUnit.SECONDS)
            var token = result?.token
            if (token.isNullOrBlank()) {
                val forceTokenTask = user.getIdToken(true)
                result = Tasks.await(forceTokenTask, 15, TimeUnit.SECONDS)
                token = result?.token
            }
            if (!token.isNullOrBlank()) {
                requestBuilder.header("Authorization", "Bearer $token")
            }
        } catch (e: Exception) {
            Log.w("AuthInterceptor", "Failed to retrieve Firebase ID token: ${e.message}")
            try {
                val forceTokenTask = user.getIdToken(true)
                val result = Tasks.await(forceTokenTask, 15, TimeUnit.SECONDS)
                val token = result?.token
                if (!token.isNullOrBlank()) {
                    requestBuilder.header("Authorization", "Bearer $token")
                }
            } catch (e2: Exception) {
                Log.w("AuthInterceptor", "Force refresh also failed: ${e2.message}")
            }
        }

        val response = chain.proceed(requestBuilder.build())
        if (response.code == 401) {
            val freshToken = try {
                Tasks.await(user.getIdToken(true), 15, TimeUnit.SECONDS)?.token
            } catch (_: Exception) {
                Log.w("AuthInterceptor", "Could not refresh Firebase authentication")
                null
            }
            if (!freshToken.isNullOrBlank()) {
                response.close()
                // Propagate network failure normally; never return the closed original response.
                return chain.proceed(original.newBuilder().header("Authorization", "Bearer $freshToken").build())
            }
        }
        return response
    }
}
