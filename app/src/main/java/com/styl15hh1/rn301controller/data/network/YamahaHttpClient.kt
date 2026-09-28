package com.styl15hh1.rn301controller.data.network

import android.util.Log
import android.content.Context
import com.styl15hh1.rn301controller.data.discovery.SsdpReply
import com.styl15hh1.rn301controller.BuildConfig
import com.styl15hh1.rn301controller.data.model.*
import com.styl15hh1.rn301controller.data.protocol.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.delay
import java.net.*
import java.io.ByteArrayOutputStream
import java.io.IOException

interface YamahaTransport {
    suspend fun resolve(address: ReceiverAddress): String
    suspend fun description(ip: String): String
    suspend fun command(ip: String, command: YamahaCommand): String
    suspend fun liveVolume(ip: String, command: YamahaCommand.SetVolume): String = command(ip, command)
}

/** Platform HTTP client: no extra runtime HTTP dependency, proxies, redirects or cloud. */
class YamahaHttpClient(private val context: Context? = null) : YamahaTransport {
    private val requestLock = Mutex()
    private var lastRequestNanos = 0L

    override suspend fun resolve(address: ReceiverAddress): String = runInterruptible(Dispatchers.IO) {
        try {
            val addresses = context?.let { LocalNetwork.find(it) }?.getAllByName(address.host) ?: InetAddress.getAllByName(address.host)
            addresses.firstOrNull { ReceiverAddress.isLocal(it) }?.hostAddress
                ?: throw YamahaException(ReceiverError(ErrorKind.INVALID_ADDRESS, "No private IPv4 destination"))
        } catch (e: SecurityException) {
            throw YamahaException(ReceiverError(ErrorKind.NETWORK_UNAVAILABLE, "Network access denied"), e)
        } catch (e: UnknownHostException) {
            throw YamahaException(ReceiverError(ErrorKind.RECEIVER_UNAVAILABLE, "DNS lookup failed"), e)
        }
    }

    override suspend fun description(ip: String) = request(ip, "/YamahaRemoteControl/desc.xml", null, "Description")
    override suspend fun command(ip: String, command: YamahaCommand): String {
        if (command is YamahaCommand.Mute) debug("Mute PUT Main_Zone/Volume/Mute=${if (command.on) "On" else "Off"}")
        return try {
            request(ip, "/YamahaRemoteControl/ctrl", YamahaXmlBuilder.build(command), command.javaClass.simpleName).also {
                if (command is YamahaCommand.Mute) debug("Mute response bytes=${it.toByteArray(Charsets.UTF_8).size}; validating Yamaha acknowledgement")
            }
        } catch (e: YamahaException) {
            debug("${command.javaClass.simpleName} failed: ${e.error.kind} ${e.error.detail.orEmpty()}")
            throw e
        }
    }

    override suspend fun liveVolume(ip: String, command: YamahaCommand.SetVolume): String =
        request(ip, "/YamahaRemoteControl/ctrl", YamahaXmlBuilder.build(command), "LiveVolume",
            spacingMs = LiveVolumeWriter.INTERVAL_MS)

    suspend fun discoveryDescription(reply: SsdpReply): String {
        val uri = URI(reply.location)
        if (uri.scheme != "http" || uri.host != reply.address || uri.userInfo != null ||
            uri.fragment != null || uri.query != null || uri.port !in setOf(-1, 80, 8080))
            throw YamahaException(ReceiverError(ErrorKind.INVALID_ADDRESS))
        return request(reply.address, uri.rawPath, null, "UPnP description", if (uri.port == -1) 80 else uri.port)
    }
    private suspend fun request(ip: String, path: String, xml: String?, type: String, port: Int = 80, spacingMs: Long = 200L): String = requestLock.withLock {
        val elapsedMs = (System.nanoTime() - lastRequestNanos) / 1_000_000
        if (elapsedMs < spacingMs) delay(spacingMs - elapsedMs)
        try {
            runInterruptible(Dispatchers.IO) {
                // Recheck the pinned numeric address. Hostnames are resolved only at connection time.
                if (!ip.matches(Regex("[0-9.]+")) || !ReceiverAddress.isLocal(InetAddress.getByName(ip)))
                    throw YamahaException(ReceiverError(ErrorKind.INVALID_ADDRESS))
                val url = URL("http://$ip:$port$path")
                val connection = (context?.let { LocalNetwork.find(it) }?.openConnection(url, Proxy.NO_PROXY)
                    ?: url.openConnection(Proxy.NO_PROXY)) as HttpURLConnection
                try {
                    connection.connectTimeout = 3000
                    connection.readTimeout = 3000
                    connection.instanceFollowRedirects = false
                    connection.useCaches = false
                    connection.requestMethod = if (xml == null) "GET" else "POST"
                    connection.setRequestProperty("Accept", "application/xml, text/xml")
                    debug("$type $path")
                    if (xml != null) {
                        connection.doOutput = true
                        connection.setRequestProperty("Content-Type", "text/xml; charset=utf-8")
                        val bytes = xml.toByteArray(Charsets.UTF_8)
                        connection.setFixedLengthStreamingMode(bytes.size)
                        connection.outputStream.use { it.write(bytes) }
                    }
                    val code = connection.responseCode
                    debug("$type HTTP $code")
                    if (code != 200) throw YamahaException(ReceiverError(
                        HttpFailure.kind(code),
                        "HTTP $code at $path"
                    ))
                    connection.inputStream.use { stream ->
                        val out = ByteArrayOutputStream()
                        val buffer = ByteArray(4096)
                        val deadline = System.nanoTime() + 5_000_000_000
                        while (true) {
                            val count = stream.read(buffer)
                            if (count < 0) break
                            if (out.size() + count > 512 * 1024) invalid("Response too large")
                            if (System.nanoTime() > deadline) throw SocketTimeoutException("Response deadline")
                            out.write(buffer, 0, count)
                        }
                        out.toString("UTF-8")
                    }
                } catch (e: SecurityException) {
                    throw YamahaException(ReceiverError(ErrorKind.NETWORK_UNAVAILABLE, "Network access denied"), e)
                } catch (e: SocketTimeoutException) {
                    throw YamahaException(ReceiverError(ErrorKind.TIMEOUT), e)
                } catch (e: NoRouteToHostException) {
                    throw YamahaException(ReceiverError(ErrorKind.NETWORK_UNAVAILABLE), e)
                } catch (e: IOException) {
                    throw YamahaException(ReceiverError(ErrorKind.RECEIVER_UNAVAILABLE, e.javaClass.simpleName), e)
                } finally { connection.disconnect() }
            }
        } finally { lastRequestNanos = System.nanoTime() }
    }

    private fun debug(message: String) { if (BuildConfig.DEBUG) Log.d("YamahaHttp", message) }
}
