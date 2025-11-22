package com.a42r.pevplugshare.data.remote

import android.content.Context
import com.google.gson.Gson
import com.a42r.pevplugshare.data.api.OsmandApiService
import com.a42r.pevplugshare.data.api.OsmandErrorResponse
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException

class OsmandApiClient(
    private val tokenManager: TokenManager,
    private val cookieManager: CookieManager
) {
    private val gson = Gson()

    private val cookieJar = object : CookieJar {
        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            if (cookies.isNotEmpty()) {
                cookieManager.saveCookies(url.host, cookies)
            }
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            return cookieManager.loadCookies(url.host)
        }
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val authInterceptor = object : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            val token = tokenManager.getAccessToken()

            val authenticatedRequest = if (!token.isNullOrEmpty()) {
                request.newBuilder()
                    .header("Authorization", "Bearer $token")
                    .build()
            } else {
                request
            }

            return chain.proceed(authenticatedRequest)
        }
    }

    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .addInterceptor(authInterceptor)
        .cookieJar(cookieJar)
        .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(OsmandApiService.BASE_URL)
        .client(httpClient)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    val api: OsmandApiService = retrofit.create(OsmandApiService::class.java)

    fun getCookiesForUrl(url: String): List<Cookie> {
        // For OSMAnd web API, we always use osmand.net
        return cookieManager.loadCookies("osmand.net")
    }

    fun getWebApiRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(OsmandApiService.WEB_API_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    fun clearCookies() {
        cookieManager.clearCookies()
    }
}

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val code: Int, val message: String) : ApiResult<Nothing>()
    object NetworkError : ApiResult<Nothing>()
}

suspend fun <T> safeApiCall(apiCall: suspend () -> T): ApiResult<T> {
    return try {
        val startTime = System.currentTimeMillis()
        val result = apiCall()
        val duration = System.currentTimeMillis() - startTime
        android.util.Log.d("OsmAndAuth", "API call completed in ${duration}ms")
        ApiResult.Success(result)
    } catch (e: HttpException) {
        android.util.Log.e("OsmAndAuth", "❌ HTTP Error: ${e.code()} - ${e.message()}")
        try {
            val errorBody = e.response()?.errorBody()?.string()
            android.util.Log.e("OsmAndAuth", "Error body: $errorBody")

            // Handle plain string error responses (like "error_email")
            if (errorBody != null && !errorBody.startsWith("{")) {
                // Plain string error response
                val message = when (errorBody.trim()) {
                    "error_email" -> "Email address not found. Please check your email and try again."
                    "error_password" -> "Incorrect password. Please try again."
                    "error" -> "Login failed. Please check your credentials."
                    else -> errorBody.trim()
                }
                android.util.Log.e("OsmAndAuth", "Interpreted plain error: $message")
                return ApiResult.Error(e.code(), message)
            }

            // Try to parse as JSON error response
            val errorResponse = Gson().fromJson(errorBody, OsmandErrorResponse::class.java)
            val message = errorResponse.error_description
                ?: errorResponse.message
                ?: errorResponse.error
                ?: e.message()
                ?: "Unknown error"
            ApiResult.Error(e.code(), message)
        } catch (ex: Exception) {
            android.util.Log.e("OsmAndAuth", "Failed to parse error response: ${ex.message}")
            val fallbackMessage = when (e.code()) {
                400 -> "Invalid credentials. Please check your email and password."
                401 -> "Unauthorized. Please log in again."
                403 -> "Access denied. Please contact support."
                404 -> "Resource not found. Please contact support."
                500 -> "Server error. Please try again later."
                else -> e.message() ?: "Unknown error"
            }
            ApiResult.Error(e.code(), fallbackMessage)
        }
    } catch (e: java.net.SocketTimeoutException) {
        android.util.Log.e("OsmAndAuth", "❌ Timeout Error: ${e.message}")
        android.util.Log.e("OsmAndAuth", "Request took longer than 5 seconds")
        ApiResult.Error(0, "Request timeout: The server took too long to respond. Please check your internet connection and try again.")
    } catch (e: IOException) {
        android.util.Log.e("OsmAndAuth", "❌ Network Error: ${e.message}")
        android.util.Log.e("OsmAndAuth", "IOException type: ${e.javaClass.simpleName}")
        ApiResult.NetworkError
    } catch (e: Exception) {
        android.util.Log.e("OsmAndAuth", "❌ Unexpected Error: ${e.message}")
        android.util.Log.e("OsmAndAuth", "Exception type: ${e.javaClass.simpleName}")
        e.printStackTrace()
        ApiResult.Error(0, e.message ?: "Unknown error")
    }
}
