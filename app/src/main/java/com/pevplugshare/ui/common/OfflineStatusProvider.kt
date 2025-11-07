package com.pevplugshare.ui.common

import com.pevplugshare.data.network.ConnectivityObserver
import com.pevplugshare.data.network.NetworkStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface OfflineStatusProvider {
    fun getNetworkStatus(): Flow<NetworkStatus>
    fun isOffline(): Flow<Boolean>
    fun isOnline(): Flow<Boolean>

    companion object {
        fun create(connectivityObserver: ConnectivityObserver): OfflineStatusProvider =
            OfflineStatusProviderImpl(connectivityObserver)
    }
}

class OfflineStatusProviderImpl(
    private val connectivityObserver: ConnectivityObserver
) : OfflineStatusProvider {

    override fun getNetworkStatus(): Flow<NetworkStatus> =
        connectivityObserver.observe()

    override fun isOffline(): Flow<Boolean> =
        connectivityObserver.observe().map { status ->
            status == NetworkStatus.OFFLINE || status == NetworkStatus.UNAVAILABLE
        }

    override fun isOnline(): Flow<Boolean> =
        connectivityObserver.observe().map { status ->
            status == NetworkStatus.ONLINE
        }
}
