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
            val tokenTask = user.getIdToken(false)
            val result = Tasks.await(tokenTask, 15, TimeUnit.SECONDS)
            val token = result?.token
            if (!token.isNullOrBlank()) {
                requestBuilder.header("Authorization", "Bearer $token")
            }
        } catch (e: Exception) {
            Log.w("AuthInterceptor", "Failed to retrieve Firebase ID token: ${e.message}")
        }

        return chain.proceed(requestBuilder.build())
    }
}
