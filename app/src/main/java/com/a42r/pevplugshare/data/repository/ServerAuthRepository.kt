package com.a42r.pevplugshare.data.repository

import com.a42r.pevplugshare.data.api.GoogleAuthRequest
import com.a42r.pevplugshare.data.api.ServerUser
import com.a42r.pevplugshare.data.remote.ServerApiClient
import com.a42r.pevplugshare.data.remote.TokenManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServerAuthRepository @Inject constructor(
    private val serverApiClient: ServerApiClient,
    private val tokenManager: TokenManager
) {
    suspend fun authenticateWithGoogle(googleIdToken: String): Result<ServerUser> {
        return try {
            val response = serverApiClient.getService()
                .authenticateWithGoogle(GoogleAuthRequest(googleIdToken))
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                tokenManager.saveServerJwt(body.token)
                tokenManager.saveServerUserInfo(body.user.id, body.user.email, body.user.displayName)
                Result.success(body.user)
            } else {
                val error = response.errorBody()?.string() ?: "Authentication failed"
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCurrentUser(): Result<ServerUser> {
        return try {
            val response = serverApiClient.getService().getCurrentUser()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to get user: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun isConnected(): Boolean = tokenManager.isServerConnected()
    fun getServerEmail(): String? = tokenManager.getServerEmail()
    fun getServerDisplayName(): String? = tokenManager.getServerDisplayName()
    fun getServerUrl(): String? = tokenManager.getServerUrl()

    fun disconnect() {
        tokenManager.disconnectServer()
        serverApiClient.clearService()
    }
}
