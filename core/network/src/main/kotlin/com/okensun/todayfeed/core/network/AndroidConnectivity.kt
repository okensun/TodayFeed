package com.okensun.todayfeed.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.okensun.todayfeed.core.freshness.Connection
import com.okensun.todayfeed.core.freshness.Connectivity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn

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
 * Which network the answers are about, kept apart from the platform read for the same reason
 * [connectionOf] is. The order that matters here happens on a device only when the network really
 * changes, and a plain test can drive it directly.
 *
 * A network is held as [Any] because only its identity is used, so a test needs no platform type.
 * One registration delivers every callback on one thread, so a plain field is enough.
 */
internal class DefaultNetwork {
    private var reporting: Any? = null

    fun changed(
        network: Any,
        connection: Connection,
    ): Connection {
        reporting = network
        return connection
    }

    /**
     * Null when the network going away is not the one being reported, and null means there is
     * nothing to say. Turning wifi off while mobile data is on hands over the new network's
     * capabilities before the old network's loss, so answering every loss would leave a phone
     * with a working connection reading as offline until the next capability change.
     */
    fun lost(network: Any): Connection? =
        if (network == reporting) {
            reporting = null
            Connection.Offline
        } else {
            null
        }
}

/**
 * Reads the platform. A network that exists but has not been validated is treated as offline,
 * because a captive portal answers requests without serving the internet.
 */
internal class AndroidConnectivity(
    private val context: Context,
    private val io: CoroutineDispatcher,
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
            val default = DefaultNetwork()
            val callback =
                object : ConnectivityManager.NetworkCallback() {
                    // Reported from what the callback carries, never by reading back. Inside
                    // `onLost` the platform can still hand out the network that is going away,
                    // still marked validated, and the loss would be missed.
                    override fun onCapabilitiesChanged(
                        network: Network,
                        capabilities: NetworkCapabilities,
                    ) {
                        trySend(default.changed(network, connectionOf(capabilities)))
                    }

                    override fun onLost(network: Network) {
                        default.lost(network)?.let { trySend(it) }
                    }
                }
            // The first value, so a collector is told where it stands before anything changes.
            // Registering answers with the current network too, but only when there is one, and
            // the reader who opened the app with no network is the one who needs telling.
            send(current())
            connectivity.registerDefaultNetworkCallback(callback)
            awaitClose { connectivity.unregisterNetworkCallback(callback) }
            // Never conflated. Losing the network is one shot, so collapsing it into the value
            // that follows would leave the screen believing the connection never went, and then
            // nothing asks again when it comes back. The buffer a callbackFlow already has is
            // what keeps both values.
        }.distinctUntilChanged().flowOn(io)

    private fun connectionOf(network: Network?): Connection = connectionOf(network?.let { manager?.getNetworkCapabilities(it) })

    private fun connectionOf(capabilities: NetworkCapabilities?): Connection =
        connectionOf(
            validated = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true,
            notMetered = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) == true
        )
}
