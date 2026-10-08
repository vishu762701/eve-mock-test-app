package com.eve.app.data.remote

import com.eve.app.BuildConfig
import com.eve.app.util.Constants
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    private val loggingInterceptor by lazy {
        HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
    }

    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("X-Eve-Client", "2")
                    .build()
                val configured = if (request.url.encodedPath.endsWith("generate-now") || request.url.encodedPath.endsWith("/retry")) {
                    chain.withReadTimeout(150, TimeUnit.SECONDS)
                } else chain
                configured.proceed(request)
            }
            .addInterceptor(AuthInterceptor())
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    private val gson by lazy {
        GsonBuilder()
            .setLenient()
            .create()
    }

    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(Constants.WORKER_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    val api: EveApiService by lazy {
        retrofit.create(EveApiService::class.java)
    }

    val apiService: EveApiService get() = api
}
