package com.styl15hh1.rn301controller.data.repository

import com.styl15hh1.rn301controller.data.model.*
import com.styl15hh1.rn301controller.data.network.*
import com.styl15hh1.rn301controller.data.protocol.*

/** Finite, GET-only diagnostics; does not connect, save an address or mutate repository state. */
internal class CompatibilityReader(private val transport: YamahaTransport, private val parser: YamahaXmlParser) {
    suspend fun read(address: String): CompatibilityReport {
        val rows = mutableListOf<CompatibilityEvidence>()
        val target = try { transport.resolve(ReceiverAddress.parse(address)) }
        catch (e: YamahaException) {
            return CompatibilityReport(evidence = ReportItem.entries.map { CompatibilityEvidence(it,
                probe = if (it == ReportItem.SYSTEM) ProbeResult.ERROR else ProbeResult.NOT_TESTED,
                error = e.error.kind.takeIf { _ -> it == ReportItem.SYSTEM }) })
        }
        suspend fun <T> probe(item: ReportItem, block: suspend () -> T): T? = try {
            block().also { rows.add(CompatibilityEvidence(item, probe = ProbeResult.RESPONDED)) }
        } catch (e: YamahaException) {
            rows.add(CompatibilityEvidence(item, probe = if (e.error.kind == ErrorKind.UNSUPPORTED_COMMAND)
                ProbeResult.NOT_SUPPORTED else ProbeResult.ERROR, error = e.error.kind))
            null
        }
        val config = probe(ReportItem.SYSTEM) {
            val xml = transport.command(target, YamahaCommand.Config)
            parser.response(xml, "GET") // Diagnostics require RC; retain legacy connection tolerance.
            parser.config(xml)
        }
        fun declaration(name: String) = when (config?.features?.get(name)) {
            true -> DeclaredCapability.SUPPORTED; false -> DeclaredCapability.NOT_SUPPORTED
            null -> DeclaredCapability.UNKNOWN
        }
        fun decorate(item: ReportItem, name: String, availability: String? = null, node: String? = null) {
            val index = rows.indexOfLast { it.item == item }
            if (index >= 0) rows[index] = rows[index].copy(capability = declaration(name),
                availability = RuntimeAvailability.from(availability), node = node)
        }
        val basic = probe(ReportItem.MAIN_ZONE) { parser.status(transport.command(target, YamahaCommand.Status)) }
        decorate(ReportItem.MAIN_ZONE, "Main_Zone")
        listOf(ReportItem.POWER to (basic?.powerState != null), ReportItem.VOLUME to (basic?.volume != null),
            ReportItem.MUTE to (basic?.muted != null), ReportItem.INPUT to (basic?.currentSource != null)).forEach { (item, present) ->
            rows.add(CompatibilityEvidence(item, probe = if (present) ProbeResult.RESPONDED else ProbeResult.NOT_TESTED))
        }
        var count: Int? = null
        var tuner: TunerStatus? = null
        if (declaration("Tuner") != DeclaredCapability.NOT_SUPPORTED) {
            val xml = probe(ReportItem.TUNER) {
                transport.command(target, YamahaCommand.TunerConfig).also { parser.featureAvailability(it, "Tuner", "Config") }
            }
            decorate(ReportItem.TUNER, "Tuner", xml?.let { parser.featureAvailability(it, "Tuner", "Config") }, "Config")
            for ((item, band) in listOf(ReportItem.FM to TunerBand.FM, ReportItem.AM to TunerBand.AM)) {
                val range = xml?.let { probe(item) { parser.tunerConfig(it, band) } }
                if (range != null) rows[rows.lastIndex] = rows.last().copy(capability = DeclaredCapability.SUPPORTED)
                if (range == null) {
                    if (rows.none { it.item == item }) rows.add(CompatibilityEvidence(item))
                    else if (rows.last().probe == ProbeResult.RESPONDED) rows[rows.lastIndex] = rows.last().copy(probe = ProbeResult.NOT_TESTED)
                }
            }
            val rds = xml?.let { parser.rdsCapability(it) }
            rows.add(CompatibilityEvidence(ReportItem.RDS, capability = when(rds) {
                true -> DeclaredCapability.SUPPORTED; false -> DeclaredCapability.NOT_SUPPORTED; null -> DeclaredCapability.UNKNOWN
            }, probe = if (rds == null) ProbeResult.NOT_TESTED else ProbeResult.RESPONDED))
            count = probe(ReportItem.PRESETS) { parser.tunerPresets(transport.command(target, YamahaCommand.TunerPresets)).size }
            // A second Tuner row records Play_Info independently from Config readiness.
            tuner = probe(ReportItem.TUNER) { parser.tuner(transport.command(target, YamahaCommand.TunerInfo)) }
            decorate(ReportItem.TUNER, "Tuner", tuner?.availability, "Play_Info")
        } else rows.add(CompatibilityEvidence(ReportItem.TUNER, DeclaredCapability.NOT_SUPPORTED))
        for ((feature, item, command) in listOf(
            Triple(NetworkFeature.SERVER, ReportItem.SERVER, YamahaCommand.ServerInfo),
            Triple(NetworkFeature.NET_RADIO, ReportItem.NET_RADIO, YamahaCommand.NetRadioInfo),
            Triple(NetworkFeature.Spotify, ReportItem.SPOTIFY, YamahaCommand.SpotifyInfo))) {
            if (declaration(feature.name) == DeclaredCapability.NOT_SUPPORTED) {
                rows.add(CompatibilityEvidence(item, DeclaredCapability.NOT_SUPPORTED)); continue
            }
            for ((read, node) in listOf(YamahaCommand.NetworkConfig(feature) to "Config", command to "Play_Info")) {
                val availability = probe(item) {
                    parser.featureAvailability(transport.command(target, read), feature.name, node)
                }
                decorate(item, feature.name, availability, node)
            }
        }
        for ((name, item) in listOf("AirPlay" to ReportItem.AIRPLAY, "Pandora" to ReportItem.PANDORA))
            rows.add(CompatibilityEvidence(item, declaration(name)))
        probe(ReportItem.DESCRIPTION) { parser.description(transport.description(target)) }
        ReportItem.entries.filter { item -> rows.none { it.item == item } }.forEach { rows.add(CompatibilityEvidence(it)) }
        return CompatibilityReport(ReportPrivacy.model(config?.model), ReportPrivacy.firmware(config?.firmware),
            rows, count, tuner?.preset, tuner?.nowPlaying?.station)
    }
}
