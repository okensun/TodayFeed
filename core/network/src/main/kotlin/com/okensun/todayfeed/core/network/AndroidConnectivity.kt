package com.okensun.todayfeed.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.okensun.todayfeed.core.freshness.Connection
import com.okensun.todayfeed.core.freshness.Connectivity
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * The rules, kept apart from the platform read so a plain test can drive them.
 *
 * `NOT_METERED` is the question, not the transport. Wifi that is shared from a phone is metered
 * and reports itself as such, while the transport type would call it wifi and be wrong.
 */
internal fun connectionOf(
    validated: Boolean,
    notMetered: Boolean,
): Connection =
    when {
        !validated -> Connection.Offline
        notMetered -> Connection.Unmetered
        else -> Connection.Metered
    }

/**
 * Reads the platform. A network that exists but has not been validated is treated as offline,
 * because a captive portal answers requests without serving the internet.
 */
internal class AndroidConnectivity(
    private val context: Context,
) : Connectivity {
    private val manager: ConnectivityManager?
        get() = context.getSystemService(ConnectivityManager::class.java)

    override fun current(): Connection = connectionOf(manager?.activeNetwork)

    override fun observe(): Flow<Connection> =
        callbackFlow {
            val connectivity = manager
            if (connectivity == null) {
                send(Connection.Offline)
                awaitClose { }
                return@callbackFlow
            }
            val callback =
                object : ConnectivityManager.NetworkCallback() {
                    // Reported from what the callback carries, never by reading back. Inside
                    // `onLost` the platform can still hand out the network that is going away,
                    // still marked validated, and the loss would be missed.
                    override fun onCapabilitiesChanged(
                        network: Network,
                        capabilities: NetworkCapabilities,
                    ) {
                        trySend(connectionOf(capabilities))
                    }

                    override fun onLost(network: Network) {
                        trySend(Connection.Offline)
                    }

                    override fun onUnavailable() {
                        trySend(Connection.Offline)
                    }
                }
            // The first value, so a collector is told where it stands before anything changes.
            send(current())
            connectivity.registerDefaultNetworkCallback(callback)
            awaitClose { connectivity.unregisterNetworkCallback(callback) }
            // Conflated right here, not after another operator. A callbackFlow's channel holds
            // nothing by default, so `trySend` from a callback drops the value unless a collector
            // happens to be waiting at that instant, and losing the network is one shot.
        }.buffer(Channel.CONFLATED).distinctUntilChanged()

    private fun connectionOf(network: Network?): Connection = connectionOf(network?.let { manager?.getNetworkCapabilities(it) })

    private fun connectionOf(capabilities: NetworkCapabilities?): Connection =
        connectionOf(
            validated = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true,
            notMetered = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) == true
        )
}
