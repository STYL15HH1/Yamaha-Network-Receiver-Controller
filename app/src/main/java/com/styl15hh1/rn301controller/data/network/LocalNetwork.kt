package com.styl15hh1.rn301controller.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities

/** Prefer the active LAN, otherwise a connected Wi-Fi/Ethernet LAN, not cellular. */
object LocalNetwork {
    fun find(context: Context): Network? {
        val manager = context.getSystemService(ConnectivityManager::class.java)
        val active = manager.activeNetwork
        @Suppress("DEPRECATION")
        val networks = manager.allNetworks.sortedBy { if (it == active) 0 else 1 }
        return networks.firstOrNull { network ->
            val caps = manager.getNetworkCapabilities(network)
            caps != null && (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) &&
                manager.getLinkProperties(network)?.linkAddresses?.any { ReceiverAddress.isLocal(it.address) } == true
        }
    }
}
