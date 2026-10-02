package com.styl15hh1.rn301controller.data.model

/** Capability, successful API reads and current readiness are independent evidence. */
enum class DeclaredCapability { SUPPORTED, NOT_SUPPORTED, UNKNOWN }
enum class ProbeResult { RESPONDED, NOT_SUPPORTED, NOT_TESTED, ERROR }
enum class RuntimeAvailability { READY, NOT_READY, UNKNOWN;
    companion object {
        fun from(value: String?) = when (value) { "Ready" -> READY; "Not Ready" -> NOT_READY; else -> UNKNOWN }
    }
}
enum class ReportItem {
    SYSTEM, MAIN_ZONE, POWER, VOLUME, MUTE, INPUT, TUNER, FM, AM, RDS, PRESETS,
    SERVER, NET_RADIO, SPOTIFY, AIRPLAY, PANDORA, DESCRIPTION
}
data class CompatibilityEvidence(
    val item: ReportItem,
    val capability: DeclaredCapability = DeclaredCapability.UNKNOWN,
    val probe: ProbeResult = ProbeResult.NOT_TESTED,
    val availability: RuntimeAvailability = RuntimeAvailability.UNKNOWN,
    val node: String? = null,
    val error: ErrorKind? = null
)
data class CompatibilityReport(
    val model: String? = null,
    val firmware: String? = null,
    val evidence: List<CompatibilityEvidence> = emptyList(),
    val storedPresets: Int? = null,
    val currentPreset: Int? = null,
    /** For local display only; receiver-supplied free text is never exported. */
    val station: String? = null
) {
    val controlApiResponded get() = evidence.any {
        it.item in setOf(ReportItem.SYSTEM, ReportItem.MAIN_ZONE) && it.probe == ProbeResult.RESPONDED
    }
}

/** Strict export allowlist: no raw XML, addresses, exception details or metadata. */
object ReportPrivacy {
    fun model(value: String?): String? = value?.takeIf {
        it.matches(Regex("(?:R-N|R-S|RX-V|RX-A|RX-S|HTR-|CRX-|WXA-|WXC-)[0-9]{1,4}[A-Za-z]?"))
    }
    fun firmware(value: String?): String? = value?.takeIf {
        it.length <= 32 && it.matches(Regex("[0-9]{1,2}\\.[0-9]{1,2}(?:/[0-9]{1,2}\\.[0-9]{1,2})?"))
    }
}
