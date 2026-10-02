package com.styl15hh1.rn301controller

import com.styl15hh1.rn301controller.data.model.*
import com.styl15hh1.rn301controller.data.network.*
import com.styl15hh1.rn301controller.data.protocol.*
import com.styl15hh1.rn301controller.data.repository.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CompatibilityReportTest {
    private val parser = YamahaXmlParser()
    private val physicalConfig = envelope("System", "Config", """
        <Model_Name>R-N301</Model_Name><Version>1.13/0.05</Version><System_ID>private-system-id</System_ID>
        <Feature_Existence><Main_Zone>1</Main_Zone><Tuner>1</Tuner><Spotify>1</Spotify><Pandora>0</Pandora>
        <SERVER>1</SERVER><NET_RADIO>1</NET_RADIO><AirPlay>1</AirPlay><Unknown>unexpected</Unknown></Feature_Existence>
    """.trimIndent())
    private fun resource(name: String) = javaClass.getResource("/$name")!!.readText()
    private fun envelope(source: String, node: String, body: String) =
        """<YAMAHA_AV rsp="GET" RC="0"><$source><$node>$body</$node></$source></YAMAHA_AV>"""
    private inner class Fake : YamahaTransport {
        var config = physicalConfig
        var status = statusXml()
        var tunerConfig = resource("rn301-tuner-config.xml")
        var resolveError: ErrorKind? = null
        var failure: ErrorKind? = null
        var rc = "0"
        var descriptionAvailable = false
        val commands = mutableListOf<YamahaCommand>()
        override suspend fun resolve(address: ReceiverAddress): String {
            resolveError?.let { throw YamahaException(ReceiverError(it, "private-system-id 192.168.1.55")) }
            return "192.168.1.55"
        }
        override suspend fun description(ip: String): String {
            if (!descriptionAvailable) throw YamahaException(ReceiverError(ErrorKind.RECEIVER_UNAVAILABLE, "HTTP 404 $ip"))
            return descriptionXml
        }
        override suspend fun command(ip: String, command: YamahaCommand): String {
            commands.add(command)
            assertTrue("Diagnostic must never PUT: $command", YamahaXmlBuilder.build(command).contains("cmd=\"GET\""))
            failure?.let { throw YamahaException(ReceiverError(it, "HTTP 500 $ip token=secret")) }
            val xml = when(command) {
                YamahaCommand.Config -> config
                YamahaCommand.Status -> status
                YamahaCommand.TunerConfig -> tunerConfig
                YamahaCommand.TunerPresets -> resource("rn301-presets.xml")
                YamahaCommand.TunerInfo -> envelope("Tuner", "Play_Info", "<Feature_Availability>Ready</Feature_Availability><Preset><Preset_Sel>2</Preset_Sel></Preset><Tuning><Band>FM</Band></Tuning><Meta_Info><Program_Service>TOK FM</Program_Service></Meta_Info>")
                is YamahaCommand.NetworkConfig -> envelope(command.source.name, "Config", "<Feature_Availability>Not Ready</Feature_Availability>")
                YamahaCommand.ServerInfo -> envelope("SERVER", "Play_Info", "<Feature_Availability>Not Ready</Feature_Availability>")
                YamahaCommand.NetRadioInfo -> envelope("NET_RADIO", "Play_Info", "<Feature_Availability>Not Ready</Feature_Availability>")
                YamahaCommand.SpotifyInfo -> envelope("Spotify", "Play_Info", "<Feature_Availability>Not Ready</Feature_Availability>")
                else -> error("Unexpected diagnostic command")
            }
            return xml.replace("RC=\"0\"", "RC=\"$rc\"")
        }
    }
    private class Store : AddressStore {
        var writes = 0
        override suspend fun read() = "192.168.1.55"
        override suspend fun write(address: String) { writes++ }
    }
    @Test fun systemConfigExtractsFirmwareModelAndExplicitCapabilityMap() {
        val config = parser.config(physicalConfig)
        assertEquals("R-N301", config.model)
        assertEquals("1.13/0.05", config.firmware)
        assertEquals(true, config.features["SERVER"])
        assertEquals(true, config.features["Main_Zone"])
        assertEquals(false, config.features["Pandora"])
        assertNull(config.features["Unknown"])
        assertNull(config.features["Missing"])
    }
    @Test fun missingFeatureMapDoesNotInventCapabilities() {
        assertTrue(parser.config(configXml).features.isEmpty())
    }
    @Test fun readinessIsTypedAndIndependentOfRcSuccess() {
        for ((wire, expected) in listOf("Ready" to RuntimeAvailability.READY, "Not Ready" to RuntimeAvailability.NOT_READY,
            "Unexpected" to RuntimeAvailability.UNKNOWN)) {
            val xml = envelope("SERVER", "Config", "<Feature_Availability>$wire</Feature_Availability>")
            assertEquals(expected, RuntimeAvailability.from(parser.featureAvailability(xml, "SERVER", "Config")))
        }
        assertEquals(RuntimeAvailability.UNKNOWN, RuntimeAvailability.from(null))
    }
    @Test fun missingFeatureNodeIsInvalidRatherThanReady() {
        assertEquals(ErrorKind.INVALID_RESPONSE, assertThrows(YamahaException::class.java) {
            parser.featureAvailability(envelope("SERVER", "Other", ""), "SERVER", "Config")
        }.error.kind)
    }
    @Test fun tunerRangesAndRdsAreReadFromConfig() {
        val xml = resource("rn301-tuner-config.xml")
        assertEquals(true, parser.rdsCapability(xml))
        assertEquals(8750, parser.tunerConfig(xml, TunerBand.FM)?.min)
        assertEquals(10800, parser.tunerConfig(xml, TunerBand.FM)?.max)
        assertEquals(5, parser.tunerConfig(xml, TunerBand.FM)?.step)
        assertEquals(531, parser.tunerConfig(xml, TunerBand.AM)?.min)
        assertEquals(9, parser.tunerConfig(xml, TunerBand.AM)?.step)
    }
    @Test fun absentRdsAndBandsRemainUnknown() {
        val xml = envelope("Tuner", "Config", "")
        assertNull(parser.rdsCapability(xml)); assertNull(parser.tunerConfig(xml))
        assertEquals(false, parser.rdsCapability(envelope("Tuner", "Config", "<RDS>Not Exist</RDS>")))
    }
    @Test fun finiteReportHasSixStoredPresetsAndDoesNotChangeRepositoryOrAddress() = runTest {
        val fake = Fake(); val store = Store()
        val repo = YamahaRepository(fake, store, io = StandardTestDispatcher(testScheduler))
        val before = repo.status.value
        val report = repo.compatibilityReport("192.168.1.55")
        assertEquals(before, repo.status.value); assertEquals(0, store.writes)
        assertEquals(6, report.storedPresets); assertEquals(2, report.currentPreset)
        assertEquals("TOK FM", report.station)
        assertEquals("R-N301", report.model); assertEquals("1.13/0.05", report.firmware)
        assertEquals(11, fake.commands.size)
        assertTrue(report.controlApiResponded)
    }
    @Test fun networkCapabilityDoesNotTurnNotReadyIntoUnsupported() = runTest {
        val report = YamahaRepository(Fake(), Store(), io = StandardTestDispatcher(testScheduler)).compatibilityReport("receiver")
        val rows = report.evidence.filter { it.item in setOf(ReportItem.SERVER, ReportItem.NET_RADIO, ReportItem.SPOTIFY) }
        assertEquals(6, rows.size)
        assertTrue(rows.all { it.capability == DeclaredCapability.SUPPORTED && it.probe == ProbeResult.RESPONDED && it.availability == RuntimeAvailability.NOT_READY })
        assertEquals(setOf("Config", "Play_Info"), rows.map { it.node }.toSet())
        val airplay = report.evidence.single { it.item == ReportItem.AIRPLAY }
        assertEquals(DeclaredCapability.SUPPORTED, airplay.capability)
        assertEquals(ProbeResult.NOT_TESTED, airplay.probe)
    }
    @Test fun unavailableDescriptionDoesNotInvalidateWorkingControlApi() = runTest {
        val report = YamahaRepository(Fake(), Store(), io = StandardTestDispatcher(testScheduler)).compatibilityReport("receiver")
        assertTrue(report.controlApiResponded)
        assertEquals(ProbeResult.ERROR, report.evidence.single { it.item == ReportItem.DESCRIPTION }.probe)
    }
    @Test fun declaredUnsupportedNetworkIsNotProbed() = runTest {
        val fake = Fake().apply { config = physicalConfig.replace("<SERVER>1</SERVER>", "<SERVER>0</SERVER>") }
        val report = YamahaRepository(fake, Store(), io = StandardTestDispatcher(testScheduler)).compatibilityReport("receiver")
        assertFalse(fake.commands.contains(YamahaCommand.ServerInfo))
        assertFalse(fake.commands.contains(YamahaCommand.NetworkConfig(NetworkFeature.SERVER)))
        assertEquals(DeclaredCapability.NOT_SUPPORTED, report.evidence.single { it.item == ReportItem.SERVER }.capability)
    }
    @Test fun otherModelsCanBeDiagnosedWithoutEnablingNormalControl() = runTest {
        val fake = Fake().apply { config = physicalConfig.replace("R-N301", "RX-V6A") }
        val repo = YamahaRepository(fake, Store(), io = StandardTestDispatcher(testScheduler))
        assertEquals("RX-V6A", repo.compatibilityReport("receiver").model)
        assertEquals(ConnectionState.DISCONNECTED, repo.status.value.connectionState)
    }
    @Test fun rcErrorsAreNotSuccessfulReads() = runTest {
        val fake = Fake().apply { rc = "4" }
        val report = YamahaRepository(fake, Store(), io = StandardTestDispatcher(testScheduler)).compatibilityReport("receiver")
        assertFalse(report.controlApiResponded)
        assertEquals(ProbeResult.NOT_SUPPORTED, report.evidence.first().probe)
        assertNull(report.storedPresets)
    }
    @Test fun missingRcIsDiagnosticErrorEvenThoughLegacyConnectionToleratesIt() = runTest {
        val fake = Fake().apply { config = physicalConfig.replace(" RC=\"0\"", "") }
        val report = YamahaRepository(fake, Store(), io = StandardTestDispatcher(testScheduler)).compatibilityReport("receiver")
        assertEquals(ErrorKind.INVALID_RESPONSE, report.evidence.first().error)
    }
    @Test fun networkAndHttpErrorsAreSafeTypedDiagnostics() = runTest {
        for (error in listOf(ErrorKind.TIMEOUT, ErrorKind.RECEIVER_UNAVAILABLE, ErrorKind.NETWORK_UNAVAILABLE)) {
            val fake = Fake().apply { failure = error }
            val report = YamahaRepository(fake, Store(), io = StandardTestDispatcher(testScheduler)).compatibilityReport("receiver")
            assertEquals(error, report.evidence.first().error)
            val text = CompatibilityText.format(report, "1.2.0") { it }
            assertFalse(text.contains("192.168")); assertFalse(text.contains("secret")); assertFalse(text.contains("HTTP 500"))
        }
    }
    @Test fun resolutionFailureIsReportedWithoutProbeOrSavedAddressMutation() = runTest {
        val fake = Fake().apply { resolveError = ErrorKind.NETWORK_UNAVAILABLE }
        val report = YamahaRepository(fake, Store(), io = StandardTestDispatcher(testScheduler)).compatibilityReport("receiver")
        assertTrue(fake.commands.isEmpty()); assertEquals(ReportItem.entries.size, report.evidence.size)
        assertFalse(report.controlApiResponded)
    }
    @Test fun malformedXmlIsErrorAndReportContinues() = runTest {
        val fake = Fake().apply { config = "<broken" }
        val report = YamahaRepository(fake, Store(), io = StandardTestDispatcher(testScheduler)).compatibilityReport("receiver")
        assertEquals(ErrorKind.INVALID_RESPONSE, report.evidence.first().error)
        assertEquals(6, report.storedPresets)
    }
    @Test fun exportContainsUsefulEvidenceButNoPrivateIdentifiersOrMetadata() = runTest {
        val report = YamahaRepository(Fake(), Store(), io = StandardTestDispatcher(testScheduler)).compatibilityReport("receiver")
        val text = CompatibilityText.format(report, "1.2.0") { it }
        listOf("1.2.0", "R-N301", "1.13/0.05", "report_stored_presets: 6", "report_not_ready", "report_supported", "report_responded").forEach { assertTrue(it, text.contains(it)) }
        listOf("192.168.1.55", "System_ID", "private-system-id", "TOK FM", "SSID", "token=").forEach { assertFalse(it, text.contains(it)) }
    }
    @Test fun exportFailsClosedOnFreeTextAndInjectedIdentityFields() {
        for (private in listOf("192.168.1.55", "fe80::abcd", "AA:BB:CC:DD:EE:FF", "System_ID=secret", "ssid=mywifi", "https://host/?token=secret", "receiver.local")) {
            val report = CompatibilityReport(model = private, firmware = private, station = private,
                evidence = listOf(CompatibilityEvidence(ReportItem.SERVER, node = private)))
            assertFalse(CompatibilityText.format(report, "1.2.0") { it }.contains(private))
        }
    }
    @Test fun allNewNetworkConfigCommandsAreExactReadOnlyPaths() {
        NetworkFeature.entries.forEach {
            val xml = YamahaXmlBuilder.build(YamahaCommand.NetworkConfig(it))
            assertTrue(xml.contains("cmd=\"GET\""))
            assertTrue(xml.contains("<${it.name}><Config>GetParam</Config></${it.name}>"))
        }
    }
    @Test fun absentPresetListIsUnknownAndEmptyValidListIsZero() {
        assertTrue(parser.tunerPresets("""<YAMAHA_AV rsp="GET" RC="0"><Tuner><Play_Control><Preset><Preset_Sel_Item/></Preset></Play_Control></Tuner></YAMAHA_AV>""").isEmpty())
        assertThrows(YamahaException::class.java) { parser.tunerPresets(envelope("Tuner", "Config", "")) }
    }
}
