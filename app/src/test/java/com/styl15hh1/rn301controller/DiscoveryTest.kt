package com.styl15hh1.rn301controller

import com.styl15hh1.rn301controller.data.discovery.*
import com.styl15hh1.rn301controller.data.model.*
import com.styl15hh1.rn301controller.data.protocol.YamahaXmlParser
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DiscoveryTest {
    private fun response(ip: String = "192.168.1.55") =
        "HTTP/1.1 200 OK\r\nST: ${SsdpResponseParser.TARGET}\r\nLOCATION: http://$ip:8080/MediaRenderer/desc.xml\r\n\r\n"

    @Test fun parsesSsdpResponse() {
        assertEquals("http://192.168.1.55:8080/MediaRenderer/desc.xml",
            SsdpResponseParser.parse(response(), "192.168.1.55")!!.location)
    }
    @Test fun headerNamesAreCaseInsensitive() {
        assertNotNull(SsdpResponseParser.parse(response().replace("LOCATION:", "location:"), "192.168.1.55"))
    }
    @Test fun rejectsRemoteLocationAndPublicSender() {
        assertNull(SsdpResponseParser.parse(response("192.168.1.56"), "192.168.1.55"))
        assertNull(SsdpResponseParser.parse(response("8.8.8.8"), "8.8.8.8"))
    }
    @Test fun rejectsDangerousAndUnsupportedLocations() {
        listOf("https://192.168.1.55/a", "http://user@192.168.1.55/a",
            "http://192.168.1.55:22/a", "http://192.168.1.55/a#fragment", "http://192.168.1.55/a?url=other").forEach {
            val xml = response().replace("http://192.168.1.55:8080/MediaRenderer/desc.xml", it)
            assertNull(SsdpResponseParser.parse(xml, "192.168.1.55"))
        }
    }
    @Test fun rejectsMalformedAndDuplicateHeaders() {
        assertNull(SsdpResponseParser.parse("garbage", "192.168.1.55"))
        assertNull(SsdpResponseParser.parse(response().replace("200 OK", "404 Not Found"), "192.168.1.55"))
        assertNull(SsdpResponseParser.parse(response() + "LOCATION: http://192.168.1.55/other\r\n", "192.168.1.55"))
    }
    @Test fun identifiesYamahaRn301() {
        val identity = YamahaXmlParser().upnpIdentity(upnpXml)!!
        assertTrue(identity.supported)
        assertEquals("Living room", identity.name)
    }
    @Test fun rejectsOtherManufacturerAndModel() {
        assertFalse(YamahaXmlParser().upnpIdentity(upnpXml.replace("Yamaha Corporation", "Other"))!!.supported)
        assertFalse(YamahaXmlParser().upnpIdentity(upnpXml.replace("R-N301", "RX-V475"))!!.supported)
    }
    @Test fun malformedDescriptionRejected() {
        assertThrows(YamahaException::class.java) { YamahaXmlParser().upnpIdentity("<root>") }
    }
    @Test fun multipleReceiversAndDuplicates() = runTest {
        var verifications = 0
        val replies = listOf("192.168.1.55", "192.168.1.56", "192.168.1.55").map { SsdpReply(it, "http://$it:8080/MediaRenderer/desc.xml") }
        val result = SsdpYamahaDiscovery(SsdpSearch { replies }, ReceiverVerifier {
            verifications++
            DiscoveredReceiver(it.address, "R-N301", "Receiver", "uuid:${it.address}")
        }).scan()
        assertEquals(2, result.receivers.size)
        assertEquals(2, verifications)
        assertEquals(DiscoveryPhase.FOUND, result.phase)
    }
    @Test fun timeoutIsFinite() = runTest {
        val engine = SsdpYamahaDiscovery(SsdpSearch { delay(60000); emptyList() }, ReceiverVerifier { null }, 5000)
        val result = engine.scan()
        assertEquals(5000L, testScheduler.currentTime)
        assertEquals(DiscoveryPhase.EMPTY, result.phase)
        assertTrue(result.timedOut)
    }
    @Test fun timeoutRetainsAlreadyVerifiedReceivers() = runTest {
        val replies = listOf("192.168.1.55", "192.168.1.56").map { SsdpReply(it, "http://$it/a") }
        val result = SsdpYamahaDiscovery(SsdpSearch { replies }, ReceiverVerifier {
            if (it.address.endsWith("56")) delay(60000)
            DiscoveredReceiver(it.address, "R-N301", "Receiver", it.address)
        }, 1000).scan()
        assertEquals(1, result.receivers.size)
        assertTrue(result.timedOut)
    }
    @Test fun badCandidateDoesNotHideValidOne() = runTest {
        val result = SsdpYamahaDiscovery(SsdpSearch {
            listOf(SsdpReply("192.168.1.55", ""), SsdpReply("192.168.1.56", ""))
        }, ReceiverVerifier {
            if (it.address.endsWith("55")) throw YamahaException(ReceiverError(ErrorKind.INVALID_RESPONSE))
            DiscoveredReceiver(it.address, "R-N301", "Receiver", it.address)
        }).scan()
        assertEquals(DiscoveryPhase.FOUND, result.phase)
        assertEquals(1, result.receivers.size)
    }
    @Test fun unavailableNetwork() = runTest {
        val result = SsdpYamahaDiscovery(SsdpSearch {
            throw YamahaException(ReceiverError(ErrorKind.NETWORK_UNAVAILABLE))
        }, ReceiverVerifier { null }).scan()
        assertEquals(DiscoveryPhase.NETWORK_UNAVAILABLE, result.phase)
    }
    @Test fun cancellationPropagates() = runTest {
        val job = launch {
            SsdpYamahaDiscovery(SsdpSearch { delay(60000); emptyList() }, ReceiverVerifier { null }).scan()
            fail("Scan should be cancelled")
        }
        runCurrent()
        job.cancelAndJoin()
        assertTrue(job.isCancelled)
    }
}

internal val upnpXml = """<root xmlns="urn:schemas-upnp-org:device-1-0"><device>
    <deviceType>urn:schemas-upnp-org:device:MediaRenderer:1</deviceType>
    <manufacturer>Yamaha Corporation</manufacturer><modelName>R-N301</modelName>
    <friendlyName>Living room</friendlyName><UDN>uuid:test-device</UDN></device></root>"""
