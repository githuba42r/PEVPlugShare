package com.a42r.pevplugshare.data.remote

import com.a42r.pevplugshare.data.api.ServerApiService
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class ServerApiClient(private val tokenManager: TokenManager) {

    private var cachedRetrofit: Retrofit? = null
    private var cachedBaseUrl: String? = null

    private fun buildOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request()
                val path = request.url.encodedPath
                val jwt = tokenManager.getServerJwt()
                val newRequest = if (jwt != null && path.contains("/api/")) {
                    request.newBuilder()
                        .addHeader("Authorization", "Bearer $jwt")
                        .build()
                } else {
                    request
                }
                chain.proceed(newRequest)
            }
            .addInterceptor(loggingInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    fun getService(): ServerApiService {
        val baseUrl = tokenManager.getServerUrl()
            ?: throw IllegalStateException("Server URL is not configured")

        val existing = cachedRetrofit
        if (existing != null && cachedBaseUrl == baseUrl) {
            return existing.create(ServerApiService::class.java)
        }

        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(buildOkHttpClient())
            .addConverterFactory(GsonConverterFactory.create(GsonBuilder().create()))
            .build()

        cachedRetrofit = retrofit
        cachedBaseUrl = baseUrl

        return retrofit.create(ServerApiService::class.java)
    }

    fun clearService() {
        cachedRetrofit = null
        cachedBaseUrl = null
    }
}
