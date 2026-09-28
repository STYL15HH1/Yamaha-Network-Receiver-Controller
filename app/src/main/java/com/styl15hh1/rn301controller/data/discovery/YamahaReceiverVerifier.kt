package com.styl15hh1.rn301controller.data.discovery

import com.styl15hh1.rn301controller.data.network.YamahaHttpClient
import com.styl15hh1.rn301controller.data.protocol.*

class YamahaReceiverVerifier(private val http: YamahaHttpClient) : ReceiverVerifier {
    private val parser = YamahaXmlParser()
    override suspend fun verify(reply: SsdpReply): DiscoveredReceiver? {
        val identity = parser.upnpIdentity(http.discoveryDescription(reply)) ?: return null
        if (!identity.supported) return null
        // The reachable description identifies the vendor/model; also prove the legacy API.
        val config = parser.config(http.command(reply.address, YamahaCommand.Config))
        if (!config.model.equals("R-N301", true)) return null
        parser.status(http.command(reply.address, YamahaCommand.Status))
        return DiscoveredReceiver(reply.address, "R-N301", identity.name.ifBlank { "R-N301" }, identity.udn)
    }
}
