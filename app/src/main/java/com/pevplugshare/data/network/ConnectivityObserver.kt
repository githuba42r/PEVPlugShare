package com.pevplugshare.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

enum class NetworkStatus {
    ONLINE,
    OFFLINE,
    LOSING,
    UNAVAILABLE
}

interface ConnectivityObserver {
    fun observe(): Flow<NetworkStatus>

    companion object {
        fun create(context: Context): ConnectivityObserver =
            ConnectivityObserverImpl(context)
    }
}

class ConnectivityObserverImpl(
    private val context: Context
) : ConnectivityObserver {

    override fun observe(): Flow<NetworkStatus> = callbackFlow {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                trySend(NetworkStatus.ONLINE)
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                trySend(NetworkStatus.OFFLINE)
            }

            override fun onLosing(network: Network, maxMsToLive: Int) {
                super.onLosing(network, maxMsToLive)
                trySend(NetworkStatus.LOSING)
            }

            override fun onUnavailable() {
                super.onUnavailable()
                trySend(NetworkStatus.UNAVAILABLE)
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, callback)

        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }
}
