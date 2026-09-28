package com.styl15hh1.rn301controller.data.discovery

import com.styl15hh1.rn301controller.data.model.*
import com.styl15hh1.rn301controller.data.network.ReceiverAddress
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.net.InetAddress
import java.net.URI
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

data class DiscoveredReceiver(val address: String, val model: String, val name: String, val udn: String)
enum class DiscoveryPhase { IDLE, SEARCHING, FOUND, EMPTY, NETWORK_UNAVAILABLE, ERROR }
data class DiscoveryState(
    val phase: DiscoveryPhase = DiscoveryPhase.IDLE,
    val receivers: List<DiscoveredReceiver> = emptyList(),
    val timedOut: Boolean = false
)
data class SsdpReply(val address: String, val location: String)
fun interface SsdpSearch { suspend fun search(): List<SsdpReply> }
fun interface ReceiverVerifier { suspend fun verify(reply: SsdpReply): DiscoveredReceiver? }
fun interface YamahaDiscovery { suspend fun scan(): DiscoveryState }

/** Finite scan. One bad/non-Yamaha responder cannot discard other valid receivers. */
class SsdpYamahaDiscovery(
    private val search: SsdpSearch,
    private val verifier: ReceiverVerifier,
    private val timeoutMs: Long = 12000
) : YamahaDiscovery {
    override suspend fun scan(): DiscoveryState {
        val found = ConcurrentHashMap<String, DiscoveredReceiver>()
        val failed = AtomicBoolean(false)
        var phase: DiscoveryPhase? = null
        val complete = withTimeoutOrNull(timeoutMs) {
            try {
                val replies = search.search().distinctBy { it.address }.take(16)
                val permits = Semaphore(2)
                coroutineScope {
                    replies.map { reply ->
                        async {
                            permits.withPermit {
                                try {
                                    verifier.verify(reply)?.let { found[it.udn.ifBlank { it.address }] = it }
                                } catch (e: CancellationException) { throw e }
                                catch (_: Exception) { failed.set(true) }
                            }
                        }
                    }.awaitAll()
                }
            } catch (e: CancellationException) { throw e }
            catch (e: YamahaException) {
                phase = if (e.error.kind == ErrorKind.NETWORK_UNAVAILABLE) DiscoveryPhase.NETWORK_UNAVAILABLE else DiscoveryPhase.ERROR
            } catch (_: Exception) { phase = DiscoveryPhase.ERROR }
            true
        }
        val devices = found.values.sortedWith(compareBy({ it.name }, { it.address }))
        return DiscoveryState(
            if (devices.isNotEmpty()) DiscoveryPhase.FOUND else phase ?: if (failed.get()) DiscoveryPhase.ERROR else DiscoveryPhase.EMPTY,
            devices, complete == null
        )
    }
}

object SsdpResponseParser {
    const val TARGET = "urn:schemas-upnp-org:device:MediaRenderer:1"
    fun parse(message: String, sender: String): SsdpReply? {
        if (message.length > 8192 || !sender.matches(Regex("[0-9.]+"))) return null
        if (!runCatching { ReceiverAddress.isLocal(InetAddress.getByName(sender)) }.getOrDefault(false)) return null
        val lines = message.split("\r\n")
        if (!lines.firstOrNull().orEmpty().matches(Regex("HTTP/1\\.[01] 200(?: .*|$)"))) return null
        val headers = mutableMapOf<String, String>()
        for (line in lines.drop(1).filter { it.isNotBlank() }) {
            val index = line.indexOf(':')
            if (index < 1) return null
            val key = line.substring(0, index).trim().uppercase(Locale.ROOT)
            if (headers.put(key, line.substring(index + 1).trim()) != null) return null
        }
        if (headers["ST"] != TARGET && headers["ST"] != "upnp:rootdevice") return null
        val location = headers["LOCATION"] ?: return null
        val uri = runCatching { URI(location) }.getOrNull() ?: return null
        if (uri.scheme != "http" || uri.host != sender || uri.userInfo != null ||
            uri.fragment != null || uri.query != null || uri.port !in setOf(-1, 80, 8080) ||
            uri.path.isNullOrBlank() || uri.path.length > 512) return null
        return SsdpReply(sender, location)
    }
}
