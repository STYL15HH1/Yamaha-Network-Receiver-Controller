package com.styl15hh1.rn301controller.data.discovery

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import com.styl15hh1.rn301controller.data.model.*
import com.styl15hh1.rn301controller.data.network.ReceiverAddress
import com.styl15hh1.rn301controller.data.network.LocalNetwork
import kotlinx.coroutines.*
import java.net.*

class AndroidSsdpSearch(context: Context) : SsdpSearch {
    private val app = context.applicationContext
    override suspend fun search(): List<SsdpReply> = withContext(Dispatchers.IO) {
        val connectivity = app.getSystemService(ConnectivityManager::class.java)
        val network = LocalNetwork.find(app) ?: throw YamahaException(ReceiverError(ErrorKind.NETWORK_UNAVAILABLE))
        val address = connectivity.getLinkProperties(network)!!.linkAddresses.first { ReceiverAddress.isLocal(it.address) }.address
        val wifi = connectivity.getNetworkCapabilities(network)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        val multicastLock = if (wifi) app.getSystemService(WifiManager::class.java)
            .createMulticastLock("rn301-discovery").apply { setReferenceCounted(false) } else null
        try {
            multicastLock?.acquire()
            MulticastSocket(null).use { socket ->
                socket.reuseAddress = true
                socket.bind(InetSocketAddress(address, 0))
                network.bindSocket(socket)
                socket.networkInterface = NetworkInterface.getByInetAddress(address)
                socket.timeToLive = 2
                socket.soTimeout = 250
                fun sendSearch() {
                    for (target in listOf(SsdpResponseParser.TARGET, "upnp:rootdevice")) {
                        val message = "M-SEARCH * HTTP/1.1\r\nHOST: 239.255.255.250:1900\r\n" +
                            "MAN: \"ssdp:discover\"\r\nMX: 2\r\nST: $target\r\n\r\n"
                        val bytes = message.toByteArray(Charsets.US_ASCII)
                        socket.send(DatagramPacket(bytes, bytes.size, InetAddress.getByName("239.255.255.250"), 1900))
                    }
                }
                sendSearch()
                val start = System.nanoTime()
                var resent = false
                val results = linkedMapOf<String, SsdpReply>()
                var packets = 0
                while ((System.nanoTime() - start) / 1_000_000 < 4500 && packets < 256) {
                    ensureActive()
                    if (!resent && (System.nanoTime() - start) / 1_000_000 > 1000) {
                        sendSearch()
                        resent = true
                    }
                    val packet = DatagramPacket(ByteArray(8192), 8192)
                    try {
                        socket.receive(packet)
                        packets++
                        if (packet.length < 8192) SsdpResponseParser.parse(
                            String(packet.data, 0, packet.length, Charsets.US_ASCII),
                            packet.address.hostAddress.orEmpty()
                        )?.let { results[it.address] = it }
                    } catch (_: SocketTimeoutException) { /* finite receive window */ }
                }
                results.values.toList()
            }
        } catch (e: CancellationException) { throw e }
        catch (e: SecurityException) { throw YamahaException(ReceiverError(ErrorKind.NETWORK_UNAVAILABLE), e) }
        catch (e: SocketException) { throw YamahaException(ReceiverError(ErrorKind.NETWORK_UNAVAILABLE), e) }
        finally { if (multicastLock?.isHeld == true) multicastLock.release() }
    }
}
