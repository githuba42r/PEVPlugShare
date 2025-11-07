package com.pevplugshare.data.remote

import com.google.gson.Gson
import com.pevplugshare.data.api.OsmandApiService
import com.pevplugshare.data.api.OsmandErrorResponse
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException

class OsmandApiClient(
    private val tokenManager: TokenManager
) {
    private val gson = Gson()

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
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(OsmandApiService.BASE_URL)
        .client(httpClient)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    val api: OsmandApiService = retrofit.create(OsmandApiService::class.java)

    fun getOAuthRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(OsmandApiService.OAUTH_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }
}

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val code: Int, val message: String) : ApiResult<Nothing>()
    object NetworkError : ApiResult<Nothing>()
}

suspend fun <T> safeApiCall(apiCall: suspend () -> T): ApiResult<T> {
    return try {
        ApiResult.Success(apiCall())
    } catch (e: HttpException) {
        try {
            val errorBody = e.response()?.errorBody()?.string()
            val errorResponse = Gson().fromJson(errorBody, OsmandErrorResponse::class.java)
            val message = errorResponse.error_description
                ?: errorResponse.message
                ?: errorResponse.error
                ?: e.message()
                ?: "Unknown error"
            ApiResult.Error(e.code(), message)
        } catch (ex: Exception) {
            ApiResult.Error(e.code(), e.message() ?: "Unknown error")
        }
    } catch (e: IOException) {
        ApiResult.NetworkError
    } catch (e: Exception) {
        ApiResult.Error(0, e.message ?: "Unknown error")
    }
}
